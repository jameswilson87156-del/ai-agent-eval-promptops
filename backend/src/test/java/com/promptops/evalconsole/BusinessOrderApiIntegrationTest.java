package com.promptops.evalconsole;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class BusinessOrderApiIntegrationTest {
    private static final String CUSTOMER_ID = "customer-business-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsOrdersAndReplaysCreationIdempotently() throws Exception {
        String key = key("create");
        JsonNode first = createOrder(key, "125.50", 201);
        JsonNode replay = createOrder(key, "125.50", 200);

        assertThat(first.path("orderId").asText()).isNotBlank();
        assertThat(first.path("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(replay.path("orderId").asText()).isEqualTo(first.path("orderId").asText());

        mockMvc.perform(post("/api/business/orders")
                        .header("X-Demo-Actor-Id", CUSTOMER_ID)
                        .header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody("130.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void paysExactOrderTotalAndPreventsDuplicatePayment() throws Exception {
        String orderId = createOrder(key("create")).path("orderId").asText();
        String paymentKey = key("payment");
        String body = "{\"amount\": \"99.00\"}";

        MvcResult first = mockMvc.perform(post("/api/business/orders/{id}/payments", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", paymentKey).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order.status").value("PAID"))
                .andReturn();
        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());

        MvcResult replay = mockMvc.perform(post("/api/business/orders/{id}/payments", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", paymentKey).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("PAID"))
                .andReturn();

        assertThat(objectMapper.readTree(replay.getResponse().getContentAsString()).path("payment").path("paymentId").asText())
                .isEqualTo(firstJson.path("payment").path("paymentId").asText());
        String unpaidOrderId = createOrder(key("wrong-amount-order")).path("orderId").asText();
        mockMvc.perform(post("/api/business/orders/{id}/payments", unpaidOrderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", key("wrong-payment")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 10.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void partialRefundsTrackRemainingBalanceAndAreIdempotent() throws Exception {
        String orderId = createAndPay();
        String firstKey = key("refund-partial");
        String firstBody = "{\"amount\": 25.00, \"reason\": \"Partial return\"}";
        MvcResult first = refund(orderId, firstKey, firstBody, 201);
        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());
        assertThat(firstJson.path("order").path("status").asText()).isEqualTo("PARTIALLY_REFUNDED");
        assertThat(firstJson.path("order").path("refundedAmount").decimalValue()).isEqualByComparingTo("25.00");

        MvcResult replay = refund(orderId, firstKey, firstBody, 200);
        assertThat(objectMapper.readTree(replay.getResponse().getContentAsString()).path("refund").path("refundId").asText())
                .isEqualTo(firstJson.path("refund").path("refundId").asText());

        JsonNode finalRefund = objectMapper.readTree(refund(orderId, key("refund-final"),
                "{\"amount\": 74.00, \"reason\": \"Remaining return\"}", 201)
                .getResponse().getContentAsString());
        assertThat(finalRefund.path("order").path("status").asText()).isEqualTo("REFUNDED");
        assertThat(finalRefund.path("order").path("refundedAmount").decimalValue()).isEqualByComparingTo("99.00");
        assertThat(finalRefund.path("order").path("refunds").size()).isEqualTo(2);

        mockMvc.perform(post("/api/business/orders/{id}/refunds", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", key("refund-over"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0.01, \"reason\": \"Too much\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unpaidOrderCanBeCancelledButCannotBePaidAfterward() throws Exception {
        String orderId = createOrder(key("create")).path("orderId").asText();
        String cancelKey = key("cancel");
        mockMvc.perform(post("/api/business/orders/{id}/cancel", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", cancelKey))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/business/orders/{id}/cancel", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", cancelKey))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/business/orders/{id}/payments", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", key("late-payment"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 99.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void customerIsolationAndDemoRoleRestrictionsAreEnforced() throws Exception {
        String orderId = createOrder(key("create")).path("orderId").asText();
        mockMvc.perform(get("/api/business/orders/{id}", orderId)
                        .header("X-Demo-Actor-Id", "different-customer").header("X-Demo-Role", "customer"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/business/orders")
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "viewer")
                        .header("Idempotency-Key", key("bad-role"))
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody("99.00")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/business/orders/{id}", orderId)
                        .header("X-Demo-Actor-Id", "support-demo").header("X-Demo-Role", "support_agent"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value(orderId));
        mockMvc.perform(get("/api/business/orders/{id}", orderId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestValidationRejectsInvalidMoneyAndMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/business/orders")
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", key("invalid"))
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody("0.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").isArray());
        mockMvc.perform(post("/api/business/orders")
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody("10.00")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/business/orders")
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", key("malformed"))
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable JSON request body"));
    }

    @Test
    void exportsBusinessAndAgentEvaluationMetricsForPrometheus() throws Exception {
        createOrder(key("metrics-order"));
        mockMvc.perform(post("/api/agent-eval/runs"))
                .andExpect(status().isOk());

        String metrics = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(metrics).contains("promptops_business_mutations_total{operation=\"order_create\",outcome=\"success\"}");
        assertThat(metrics).contains("promptops_agent_eval_runs_total");
        assertThat(metrics).contains("promptops_agent_eval_cases_total");
        assertThat(metrics).contains("http_server_requests_seconds_count");
    }

    private String createAndPay() throws Exception {
        String orderId = createOrder(key("create")).path("orderId").asText();
        mockMvc.perform(post("/api/business/orders/{id}/payments", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", key("payment")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 99.00}"))
                .andExpect(status().isCreated());
        return orderId;
    }

    private MvcResult refund(String orderId, String idempotencyKey, String body, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/business/orders/{id}/refunds", orderId)
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", idempotencyKey).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    private JsonNode createOrder(String idempotencyKey) throws Exception {
        return createOrder(idempotencyKey, "99.00", 201);
    }

    private JsonNode createOrder(String idempotencyKey, String amount, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/business/orders")
                        .header("X-Demo-Actor-Id", CUSTOMER_ID).header("X-Demo-Role", "customer")
                        .header("Idempotency-Key", idempotencyKey).contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(amount)))
                .andExpect(status().is(expectedStatus)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String orderBody(String amount) throws Exception {
        return objectMapper.writeValueAsString(new CreateOrderPayload(new BigDecimal(amount), "CNY", "Synthetic subscription"));
    }

    private String key(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record CreateOrderPayload(BigDecimal amount, String currency, String itemName) {}
}
