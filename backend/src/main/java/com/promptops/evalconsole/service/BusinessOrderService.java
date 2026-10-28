package com.promptops.evalconsole.service;

import com.promptops.evalconsole.api.dto.BusinessDtos.Actor;
import com.promptops.evalconsole.api.dto.BusinessDtos.CreateOrderRequest;
import com.promptops.evalconsole.api.dto.BusinessDtos.Mutation;
import com.promptops.evalconsole.api.dto.BusinessDtos.OrderView;
import com.promptops.evalconsole.api.dto.BusinessDtos.PaymentOperation;
import com.promptops.evalconsole.api.dto.BusinessDtos.PaymentRequest;
import com.promptops.evalconsole.api.dto.BusinessDtos.PaymentView;
import com.promptops.evalconsole.api.dto.BusinessDtos.RefundOperation;
import com.promptops.evalconsole.api.dto.BusinessDtos.RefundRequest;
import com.promptops.evalconsole.api.dto.BusinessDtos.RefundView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessOrderService {
    private final JdbcTemplate jdbc;
    private final ProductionMetrics metrics;

    public BusinessOrderService(JdbcTemplate jdbc, ProductionMetrics metrics) {
        this.jdbc = jdbc;
        this.metrics = metrics;
    }

    @Transactional
    public Mutation<OrderView> createOrder(Actor actor, String idempotencyKey, CreateOrderRequest request) {
        requireRole(actor, "customer");
        requireIdempotencyKey(idempotencyKey);
        List<OrderRow> existing = findOrderByCreateKey(idempotencyKey);
        if (!existing.isEmpty()) {
            OrderRow row = existing.get(0);
            if (!row.customerId().equals(actor.id())
                    || row.totalAmount().compareTo(request.amount()) != 0
                    || !row.currency().equals(request.currency())
                    || !row.itemName().equals(request.itemName())) {
                throw conflict("Idempotency key was already used for a different order.");
            }
            metrics.recordBusinessMutation("order_create", true);
            return new Mutation<>(view(row.orderId()), true);
        }

        String orderId = "ord_" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO promptops_business_order
                    (order_id, create_idempotency_key, customer_id, item_name, currency,
                     total_amount, refunded_amount, order_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, 'PENDING_PAYMENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, orderId, idempotencyKey, actor.id(), request.itemName(), request.currency(), request.amount());
        metrics.recordBusinessMutation("order_create", false);
        return new Mutation<>(view(orderId), false);
    }

    @Transactional
    public Mutation<OrderView> cancelOrder(Actor actor, String orderId, String idempotencyKey) {
        requireRole(actor, "customer");
        requireIdempotencyKey(idempotencyKey);
        OrderRow order = lockOrder(orderId);
        requireOwner(order, actor);
        if (order.cancelIdempotencyKey() != null) {
            if (order.cancelIdempotencyKey().equals(idempotencyKey) && "CANCELLED".equals(order.status())) {
                metrics.recordBusinessMutation("order_cancel", true);
                return new Mutation<>(view(orderId), true);
            }
            throw conflict("Order cancellation has already been recorded.");
        }
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw conflict("Only an unpaid order can be cancelled.");
        }
        jdbc.update("UPDATE promptops_business_order SET order_status='CANCELLED', cancel_idempotency_key=?, updated_at=CURRENT_TIMESTAMP WHERE order_id=?",
                idempotencyKey, orderId);
        metrics.recordBusinessMutation("order_cancel", false);
        return new Mutation<>(view(orderId), false);
    }

    @Transactional
    public Mutation<PaymentOperation> payOrder(Actor actor, String orderId, String idempotencyKey, PaymentRequest request) {
        requireRole(actor, "customer");
        requireIdempotencyKey(idempotencyKey);
        OrderRow order = lockOrder(orderId);
        requireOwner(order, actor);
        List<PaymentView> previous = findPaymentByKey(idempotencyKey);
        if (!previous.isEmpty()) {
            PaymentView payment = previous.get(0);
            if (!payment.orderId().equals(orderId) || payment.amount().compareTo(request.amount()) != 0) {
                throw conflict("Idempotency key was already used for a different payment.");
            }
            metrics.recordBusinessMutation("payment_create", true);
            return new Mutation<>(new PaymentOperation(view(orderId), payment), true);
        }
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw conflict("Only an unpaid order can be paid.");
        }
        if (request.amount().compareTo(order.totalAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Payment amount must equal the order total.");
        }
        String paymentId = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO promptops_business_payment
                    (payment_id, order_id, idempotency_key, actor_id, amount, payment_status, created_at)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED', CURRENT_TIMESTAMP)
                """, paymentId, orderId, idempotencyKey, actor.id(), request.amount());
        jdbc.update("UPDATE promptops_business_order SET order_status='PAID', updated_at=CURRENT_TIMESTAMP WHERE order_id=?", orderId);
        metrics.recordBusinessMutation("payment_create", false);
        return new Mutation<>(new PaymentOperation(view(orderId), payment(paymentId)), false);
    }

    @Transactional
    public Mutation<RefundOperation> refundOrder(Actor actor, String orderId, String idempotencyKey, RefundRequest request) {
        requireRefundRole(actor);
        requireIdempotencyKey(idempotencyKey);
        OrderRow order = lockOrder(orderId);
        requireOrderAccess(order, actor);
        List<RefundView> previous = findRefundByKey(idempotencyKey);
        if (!previous.isEmpty()) {
            RefundView refund = previous.get(0);
            if (!refund.orderId().equals(orderId)
                    || refund.amount().compareTo(request.amount()) != 0
                    || !refund.reason().equals(request.reason())) {
                throw conflict("Idempotency key was already used for a different refund.");
            }
            metrics.recordBusinessMutation("refund_create", true);
            return new Mutation<>(new RefundOperation(view(orderId), refund), true);
        }
        if (!List.of("PAID", "PARTIALLY_REFUNDED").contains(order.status())) {
            throw conflict("Only a paid order with a remaining balance can be refunded.");
        }
        BigDecimal newRefundedTotal = order.refundedAmount().add(request.amount());
        if (newRefundedTotal.compareTo(order.totalAmount()) > 0) {
            throw conflict("Refund amount exceeds the remaining order balance.");
        }
        String refundId = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO promptops_business_refund
                    (refund_id, order_id, idempotency_key, actor_id, amount, reason, refund_status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'COMPLETED', CURRENT_TIMESTAMP)
                """, refundId, orderId, idempotencyKey, actor.id(), request.amount(), request.reason());
        String newStatus = newRefundedTotal.compareTo(order.totalAmount()) == 0 ? "REFUNDED" : "PARTIALLY_REFUNDED";
        jdbc.update("UPDATE promptops_business_order SET refunded_amount=?, order_status=?, updated_at=CURRENT_TIMESTAMP WHERE order_id=?",
                newRefundedTotal, newStatus, orderId);
        metrics.recordBusinessMutation("refund_create", false);
        return new Mutation<>(new RefundOperation(view(orderId), refund(refundId)), false);
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(Actor actor, String orderId) {
        requireActor(actor);
        OrderRow order = findOrder(orderId);
        requireOrderAccess(order, actor);
        return view(orderId);
    }

    private OrderView view(String orderId) {
        OrderRow row = findOrder(orderId);
        List<PaymentView> payments = jdbc.query("""
                SELECT payment_id, order_id, amount, payment_status, created_at
                FROM promptops_business_payment WHERE order_id=?
                """, (rs, n) -> new PaymentView(rs.getString("payment_id"), rs.getString("order_id"),
                rs.getBigDecimal("amount"), rs.getString("payment_status"), rs.getTimestamp("created_at").toLocalDateTime().toString()), orderId);
        List<RefundView> refunds = jdbc.query("""
                SELECT refund_id, order_id, amount, reason, refund_status, created_at
                FROM promptops_business_refund WHERE order_id=? ORDER BY created_at, refund_id
                """, (rs, n) -> new RefundView(rs.getString("refund_id"), rs.getString("order_id"),
                rs.getBigDecimal("amount"), rs.getString("reason"), rs.getString("refund_status"), rs.getTimestamp("created_at").toLocalDateTime().toString()), orderId);
        PaymentView payment = payments.isEmpty() ? null : payments.get(0);
        return new OrderView(row.orderId(), row.customerId(), row.itemName(), row.currency(), row.totalAmount(),
                row.refundedAmount(), row.status(), row.createdAt().toString(), payment, refunds);
    }

    private OrderRow lockOrder(String orderId) {
        List<OrderRow> rows = jdbc.query("""
                SELECT order_id, create_idempotency_key, cancel_idempotency_key, customer_id, item_name,
                       currency, total_amount, refunded_amount, order_status, created_at
                FROM promptops_business_order WHERE order_id=? FOR UPDATE
                """, (rs, n) -> mapOrder(rs.getString("order_id"), rs.getString("create_idempotency_key"),
                rs.getString("cancel_idempotency_key"), rs.getString("customer_id"), rs.getString("item_name"),
                rs.getString("currency"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("refunded_amount"),
                rs.getString("order_status"), rs.getTimestamp("created_at").toLocalDateTime()), orderId);
        if (rows.isEmpty()) throw notFound();
        return rows.get(0);
    }

    private OrderRow findOrder(String orderId) {
        List<OrderRow> rows = jdbc.query("""
                SELECT order_id, create_idempotency_key, cancel_idempotency_key, customer_id, item_name,
                       currency, total_amount, refunded_amount, order_status, created_at
                FROM promptops_business_order WHERE order_id=?
                """, (rs, n) -> mapOrder(rs.getString("order_id"), rs.getString("create_idempotency_key"),
                rs.getString("cancel_idempotency_key"), rs.getString("customer_id"), rs.getString("item_name"),
                rs.getString("currency"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("refunded_amount"),
                rs.getString("order_status"), rs.getTimestamp("created_at").toLocalDateTime()), orderId);
        if (rows.isEmpty()) throw notFound();
        return rows.get(0);
    }

    private OrderRow mapOrder(String orderId, String createKey, String cancelKey, String customerId, String itemName,
                              String currency, BigDecimal total, BigDecimal refunded, String status, LocalDateTime createdAt) {
        return new OrderRow(orderId, createKey, cancelKey, customerId, itemName, currency, total, refunded, status, createdAt);
    }

    private List<OrderRow> findOrderByCreateKey(String key) {
        return jdbc.query("""
                SELECT order_id, create_idempotency_key, cancel_idempotency_key, customer_id, item_name,
                       currency, total_amount, refunded_amount, order_status, created_at
                FROM promptops_business_order WHERE create_idempotency_key=?
                """, (rs, n) -> mapOrder(rs.getString("order_id"), rs.getString("create_idempotency_key"),
                rs.getString("cancel_idempotency_key"), rs.getString("customer_id"), rs.getString("item_name"),
                rs.getString("currency"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("refunded_amount"),
                rs.getString("order_status"), rs.getTimestamp("created_at").toLocalDateTime()), key);
    }

    private List<PaymentView> findPaymentByKey(String key) {
        return jdbc.query("""
                SELECT payment_id, order_id, amount, payment_status, created_at
                FROM promptops_business_payment WHERE idempotency_key=?
                """, (rs, n) -> new PaymentView(rs.getString("payment_id"), rs.getString("order_id"),
                rs.getBigDecimal("amount"), rs.getString("payment_status"), rs.getTimestamp("created_at").toLocalDateTime().toString()), key);
    }

    private List<RefundView> findRefundByKey(String key) {
        return jdbc.query("""
                SELECT refund_id, order_id, amount, reason, refund_status, created_at
                FROM promptops_business_refund WHERE idempotency_key=?
                """, (rs, n) -> new RefundView(rs.getString("refund_id"), rs.getString("order_id"),
                rs.getBigDecimal("amount"), rs.getString("reason"), rs.getString("refund_status"), rs.getTimestamp("created_at").toLocalDateTime().toString()), key);
    }

    private PaymentView payment(String paymentId) {
        return jdbc.queryForObject("""
                SELECT payment_id, order_id, amount, payment_status, created_at
                FROM promptops_business_payment WHERE payment_id=?
                """, (rs, n) -> new PaymentView(rs.getString("payment_id"), rs.getString("order_id"),
                rs.getBigDecimal("amount"), rs.getString("payment_status"), rs.getTimestamp("created_at").toLocalDateTime().toString()), paymentId);
    }

    private RefundView refund(String refundId) {
        return jdbc.queryForObject("""
                SELECT refund_id, order_id, amount, reason, refund_status, created_at
                FROM promptops_business_refund WHERE refund_id=?
                """, (rs, n) -> new RefundView(rs.getString("refund_id"), rs.getString("order_id"),
                rs.getBigDecimal("amount"), rs.getString("reason"), rs.getString("refund_status"), rs.getTimestamp("created_at").toLocalDateTime().toString()), refundId);
    }

    private void requireActor(Actor actor) {
        if (actor == null || actor.id() == null || actor.id().isBlank() || actor.role() == null || actor.role().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demo actor ID and role headers are required.");
        }
        if (!List.of("customer", "support_agent").contains(actor.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo actor role is not allowed.");
        }
    }

    private void requireRole(Actor actor, String role) {
        requireActor(actor);
        if (!role.equals(actor.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operation is not allowed for this demo role.");
    }

    private void requireRefundRole(Actor actor) {
        requireActor(actor);
        if (!List.of("customer", "support_agent").contains(actor.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refund is not allowed for this demo role.");
        }
    }

    private void requireOwner(OrderRow order, Actor actor) {
        if (!order.customerId().equals(actor.id())) throw notFound();
    }

    private void requireOrderAccess(OrderRow order, Actor actor) {
        if ("support_agent".equals(actor.role())) return;
        if (!"customer".equals(actor.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operation is not allowed for this demo role.");
        requireOwner(order, actor);
    }

    private void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 128 characters.");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Business order not found.");
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record OrderRow(String orderId, String createIdempotencyKey, String cancelIdempotencyKey,
                            String customerId, String itemName, String currency, BigDecimal totalAmount,
                            BigDecimal refundedAmount, String status, LocalDateTime createdAt) {}
}
