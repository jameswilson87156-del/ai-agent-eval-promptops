package com.promptops.evalconsole.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private final List<String> requestBodies = new ArrayList<>();
    private final List<String> authorizationHeaders = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void usesToolCallsAndOnlyLocalToolStubs() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        startServer((body, requestIndex) -> requestIndex == 1
                ? jsonResponse(toolCall("weather_lookup", "{\"city\":\"东京\"}"), "tool_calls")
                : jsonResponse(decision("completed", "东京天气查询完成", true, false), "stop"));

        var provider = provider(5, 0, 2);
        JsonNode input = mapper.readTree("""
                {"prompt":"查询东京天气。","allowed_tools":["weather_lookup"],"permissions":[]}
                """);
        List<Map<String, Object>> trace = new ArrayList<>();
        Map<String, Object> result = provider.execute(input, trace);

        assertThat(requestBodies).hasSize(2);
        assertThat(authorizationHeaders).containsOnly("Bearer unit-test-key");
        assertThat(requestBodies.get(0)).contains("openai-compatible-test-model", "查询东京天气", "weather_lookup");
        assertThat(requestBodies.get(0)).doesNotContain("unit-test-key");
        assertThat(result).containsEntry("provider_type", "openai_compatible")
                .containsEntry("model_name", "openai-compatible-test-model")
                .containsEntry("tool_runtime", "deterministic_local_stub")
                .containsEntry("task_completed", true);
        assertThat(result.get("final_output")).isEqualTo("东京天气查询完成");
        assertThat(((List<Map<String, Object>>) result.get("tool_calls"))).singleElement()
                .satisfies(call -> assertThat(call).containsEntry("name", "weather_lookup").containsEntry("status", "success"));
        assertThat(trace).extracting(event -> event.get("event_type")).contains("provider_request", "tool_selected", "tool_result");
        assertThat(mapper.writeValueAsString(result)).doesNotContain("unit-test-key");
    }

    @Test
    void retries429OnceThenAcceptsSuccessfulResponse() throws Exception {
        startServer((body, requestIndex) -> requestIndex == 1
                ? new StubResponse(429, "{\"error\":\"rate limited; secret=do-not-store\"}")
                : jsonResponse(decision("completed", "完成", true, false), "stop"));

        var provider = provider(5, 1, 1);
        Map<String, Object> result = provider.execute(mapper.readTree("{\"prompt\":\"你好\"}"), new ArrayList<>());

        assertThat(requestBodies).hasSize(2);
        assertThat(result).containsEntry("status", "completed");
    }

    @Test
    void classifiesServerErrorAfterFiniteRetryWithoutKeepingBody() throws Exception {
        startServer((body, requestIndex) -> new StubResponse(500, "{\"message\":\"private token=do-not-store\"}"));
        var provider = provider(5, 1, 1);

        assertThatThrownBy(() -> provider.execute(mapper.readTree("{\"prompt\":\"你好\"}"), new ArrayList<>()))
                .isInstanceOf(ProviderCallException.class)
                .satisfies(error -> {
                    ProviderCallException providerError = (ProviderCallException) error;
                    assertThat(providerError.code()).isEqualTo("provider_server_error");
                    assertThat(providerError.getMessage()).doesNotContain("do-not-store", "private token");
                });
        assertThat(requestBodies).hasSize(2);
    }

    @Test
    void classifiesMalformedEnvelopeAndEmptyResponseSeparately() throws Exception {
        startServer((body, requestIndex) -> new StubResponse(200, "not-json secret=do-not-store"));
        var malformedProvider = provider(5, 0, 1);
        assertThatThrownBy(() -> malformedProvider.execute(mapper.readTree("{\"prompt\":\"test\"}"), new ArrayList<>()))
                .isInstanceOfSatisfying(ProviderCallException.class, error -> assertThat(error.code()).isEqualTo("provider_invalid_json"));
        stopServer();

        startServer((body, requestIndex) -> new StubResponse(200, ""));
        var emptyProvider = provider(5, 0, 1);
        assertThatThrownBy(() -> emptyProvider.execute(mapper.readTree("{\"prompt\":\"test\"}"), new ArrayList<>()))
                .isInstanceOfSatisfying(ProviderCallException.class, error -> assertThat(error.code()).isEqualTo("provider_empty_response"));
    }

    @Test
    void classifiesMissingCompletionSchemaAndInvalidAgentJsonDifferently() throws Exception {
        startServer((body, requestIndex) -> new StubResponse(200, "{\"id\":\"x\",\"choices\":[]}"));
        var malformedProvider = provider(5, 0, 1);
        assertThatThrownBy(() -> malformedProvider.execute(mapper.readTree("{\"prompt\":\"test\"}"), new ArrayList<>()))
                .isInstanceOfSatisfying(ProviderCallException.class, error -> assertThat(error.code()).isEqualTo("provider_invalid_response"));
        stopServer();

        startServer((body, requestIndex) -> jsonResponse("not a decision json", "stop"));
        var outputProvider = provider(5, 0, 1);
        Map<String, Object> result = outputProvider.execute(mapper.readTree("{\"prompt\":\"test\"}"), new ArrayList<>());
        assertThat(result).containsEntry("status", "invalid_output").containsEntry("task_completed", false);
        assertThat(result).containsKey("contract_errors");
    }

    @Test
    void classifiesRequestTimeout() throws Exception {
        startServer((body, requestIndex) -> {
            try { Thread.sleep(1300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return jsonResponse(decision("completed", "late", true, false), "stop");
        });
        var provider = provider(1, 0, 1);
        assertThatThrownBy(() -> provider.execute(mapper.readTree("{\"prompt\":\"test\"}"), new ArrayList<>()))
                .isInstanceOfSatisfying(ProviderCallException.class, error -> assertThat(error.code()).isEqualTo("provider_timeout"));
    }

    @Test
    void deniedModelToolProposalIsRecordedAndNeverExecutedExternally() throws Exception {
        startServer((body, requestIndex) -> requestIndex == 1
                ? jsonResponse(toolCall("email_send", "{\"recipient\":\"alice@example.test\",\"body\":\"hello\"}"), "tool_calls")
                : jsonResponse(decision("denied", "权限不足，未发送邮件。", true, true), "stop"));
        var provider = provider(5, 0, 1);
        JsonNode input = mapper.readTree("""
                {"prompt":"发送邮件。","allowed_tools":["email_send"],"permissions":[]}
                """);

        Map<String, Object> result = provider.execute(input, new ArrayList<>());
        Map<String, Object> call = ((List<Map<String, Object>>) result.get("tool_calls")).get(0);
        assertThat(call).containsEntry("status", "denied").doesNotContainKey("result");
        assertThat(result).containsEntry("status", "denied").containsEntry("refused", false);
        assertThat(requestBodies.get(1)).contains("Email permission scope is not granted.");
        assertThat(requestBodies.get(0)).doesNotContain("unit-test-key");
    }

    @Test
    void doesNotCallWhenProviderHasNoCredentialOrModel() throws Exception {
        var provider = new OpenAiCompatibleProvider(mapper, "http://127.0.0.1:1/v1", "", "", "v1", 2, 1, 1,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build());
        assertThat(provider.isConfigured()).isFalse();
        assertThat(provider.publicStatus()).doesNotContainKey("api_key").containsEntry("credentials_configured", false);
        assertThatThrownBy(() -> provider.execute(mapper.readTree("{\"prompt\":\"test\"}"), new ArrayList<>()))
                .isInstanceOfSatisfying(ProviderCallException.class, error -> assertThat(error.code()).isEqualTo("provider_not_configured"));
    }

    private OpenAiCompatibleProvider provider(int timeoutSeconds, int retries, int toolRounds) {
        return new OpenAiCompatibleProvider(mapper, endpoint(), "openai-compatible-test-model", "unit-test-key",
                "stub-config-v1", timeoutSeconds, retries, toolRounds,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build());
    }

    private String endpoint() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"; }

    private void startServer(BiFunction<String, Integer, StubResponse> responder) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger count = new AtomicInteger();
        server.createContext("/v1/chat/completions", exchange -> {
            int requestIndex = count.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestBodies.add(body);
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            StubResponse response = responder.apply(body, requestIndex);
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(response.status(), bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception ignored) {
                // A timeout test intentionally closes the client before the loopback server responds.
            } finally { exchange.close(); }
        });
        server.start();
    }

    private StubResponse jsonResponse(String content, String finishReason) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", content);
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0); choice.put("message", message); choice.put("finish_reason", finishReason);
        try { return new StubResponse(200, mapper.writeValueAsString(Map.of("choices", List.of(choice)))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private StubResponse jsonResponse(Map<String, Object> message, String finishReason) {
        Map<String, Object> responseMessage = message;
        if (!message.containsKey("tool_calls")) {
            responseMessage = new LinkedHashMap<>();
            responseMessage.put("role", "assistant");
            try { responseMessage.put("content", mapper.writeValueAsString(message)); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        }
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0); choice.put("message", responseMessage); choice.put("finish_reason", finishReason);
        try { return new StubResponse(200, mapper.writeValueAsString(Map.of("choices", List.of(choice)))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private Map<String, Object> decision(String status, String output, boolean completed, boolean refused) {
        return Map.of("status", status, "final_output", output, "task_completed", completed, "refused", refused);
    }

    private Map<String, Object> toolCall(String name, String arguments) {
        Map<String, Object> function = Map.of("name", name, "arguments", arguments);
        Map<String, Object> call = Map.of("id", "call-local-1", "type", "function", "function", function);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant"); message.put("content", null); message.put("tool_calls", List.of(call));
        return message;
    }

    private record StubResponse(int status, String body) { }
}
