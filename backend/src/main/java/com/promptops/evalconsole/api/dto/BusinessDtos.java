package com.promptops.evalconsole.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class BusinessDtos {
    private BusinessDtos() {}

    public record Actor(String id, String role) {}

    public record CreateOrderRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "CNY|USD") String currency,
            @NotBlank @Size(max = 160) String itemName) {}

    public record PaymentRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) {}

    public record RefundRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotBlank @Size(max = 256) String reason) {}

    public record OrderView(
            String orderId,
            String customerId,
            String itemName,
            String currency,
            BigDecimal totalAmount,
            BigDecimal refundedAmount,
            String status,
            String createdAt,
            PaymentView payment,
            List<RefundView> refunds) {}

    public record PaymentView(String paymentId, String orderId, BigDecimal amount, String status, String createdAt) {}

    public record RefundView(String refundId, String orderId, BigDecimal amount, String reason, String status, String createdAt) {}

    public record PaymentOperation(OrderView order, PaymentView payment) {}

    public record RefundOperation(OrderView order, RefundView refund) {}

    public record Mutation<T>(T body, boolean replayed) {}
}
