package com.promptops.evalconsole.api;

import com.promptops.evalconsole.api.dto.BusinessDtos.Actor;
import com.promptops.evalconsole.api.dto.BusinessDtos.CreateOrderRequest;
import com.promptops.evalconsole.api.dto.BusinessDtos.Mutation;
import com.promptops.evalconsole.api.dto.BusinessDtos.OrderView;
import com.promptops.evalconsole.api.dto.BusinessDtos.PaymentOperation;
import com.promptops.evalconsole.api.dto.BusinessDtos.PaymentRequest;
import com.promptops.evalconsole.api.dto.BusinessDtos.RefundOperation;
import com.promptops.evalconsole.api.dto.BusinessDtos.RefundRequest;
import com.promptops.evalconsole.service.BusinessOrderService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business/orders")
@Profile({"demo", "test"})
public class BusinessOrderController {
    private final BusinessOrderService service;

    public BusinessOrderController(BusinessOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderView> create(
            @RequestHeader(value = "X-Demo-Actor-Id", required = false) String actorId,
            @RequestHeader(value = "X-Demo-Role", required = false) String role,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        Mutation<OrderView> result = service.createOrder(actor(actorId, role), idempotencyKey, request);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result.body());
    }

    @GetMapping("/{orderId}")
    public OrderView get(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Demo-Actor-Id", required = false) String actorId,
            @RequestHeader(value = "X-Demo-Role", required = false) String role) {
        return service.getOrder(actor(actorId, role), orderId);
    }

    @PostMapping("/{orderId}/payments")
    public ResponseEntity<PaymentOperation> pay(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Demo-Actor-Id", required = false) String actorId,
            @RequestHeader(value = "X-Demo-Role", required = false) String role,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        Mutation<PaymentOperation> result = service.payOrder(actor(actorId, role), orderId, idempotencyKey, request);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result.body());
    }

    @PostMapping("/{orderId}/refunds")
    public ResponseEntity<RefundOperation> refund(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Demo-Actor-Id", required = false) String actorId,
            @RequestHeader(value = "X-Demo-Role", required = false) String role,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RefundRequest request) {
        Mutation<RefundOperation> result = service.refundOrder(actor(actorId, role), orderId, idempotencyKey, request);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result.body());
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderView> cancel(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Demo-Actor-Id", required = false) String actorId,
            @RequestHeader(value = "X-Demo-Role", required = false) String role,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Mutation<OrderView> result = service.cancelOrder(actor(actorId, role), orderId, idempotencyKey);
        return ResponseEntity.ok(result.body());
    }

    private Actor actor(String id, String role) {
        return new Actor(id, role);
    }
}
