package com.promptops.evalconsole.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A deterministic, inspectable agent harness. It is deliberately not an LLM. */
@Component
public class MockAgent implements AgentProvider {
    private final ObjectMapper mapper;
    private static final Map<String, List<String>> REQUIRED = Map.of(
            "weather_lookup", List.of("city"), "web_search", List.of("query"),
            "create_ticket", List.of("title", "priority"), "email_send", List.of("recipient", "body"));

    public MockAgent(ObjectMapper mapper) { this.mapper = mapper; }

    @Override public String providerType() { return "mock"; }
    @Override public String modelName() { return "deterministic-mock-agent"; }
    @Override public String defaultAgentConfigVersion() { return "mock-agent-v1"; }
    @Override public String defaultPromptVersion() { return "fixed-mock-prompt-v1"; }
    @Override public boolean isConfigured() { return true; }
    @Override public Map<String, Object> fingerprintSettings() { return Map.of("harness_version", "deterministic-mock-v1", "tool_runtime", "local_stub"); }
    @Override public Map<String, Object> publicStatus() {
        return Map.of("provider_type", providerType(), "configured", true, "credentials_configured", false,
                "model_name", modelName(), "config_version", defaultAgentConfigVersion(), "tool_runtime", "deterministic_local_stub");
    }

    public Map<String, Object> execute(JsonNode input, List<Map<String, Object>> trace) throws com.fasterxml.jackson.core.JsonProcessingException {
        List<Map<String, Object>> calls = new ArrayList<>();
        long stepStarted = System.nanoTime();
        String prompt = input.path("prompt").asText("");
        event(trace, "input_received", "case input", Map.of("prompt", truncate(prompt)), "success", null, stepStarted);

        stepStarted = System.nanoTime();
        if (input.path("omit_prompt").asBoolean(false) || prompt.isBlank()) {
            event(trace, "tool_selected", prompt, Map.of("tool", "none/invalid-input"), "skipped", null, stepStarted);
            event(trace, "arguments_validated", "no tool selected", Map.of(), "error", "prompt is required", System.nanoTime());
            skippedInvocationEvents(trace, "no tool executed", "prompt is required");
            return finish(trace, "invalid_input", "prompt is required", false, false, calls);
        }

        String tool = selectTool(prompt, input);
        event(trace, "tool_selected", truncate(prompt), Map.of("tool", tool == null ? "none" : tool), tool == null ? "skipped" : "success", null, stepStarted);

        if (tool == null) {
            String text = prompt.toLowerCase();
            if (input.path("no_tool").asBoolean(false) || text.contains("月球") || text.contains("database_backup")) {
                skippedToolEvents(trace, "no tool selected", "unsupported task");
                return finish(trace, "unsupported", "No registered capability can perform this task; no external action was taken.", false, false, calls);
            }
            if ("malformed".equals(input.path("output_fault").asText())) {
                skippedToolEvents(trace, "no tool selected", "no tool call expected");
                return finish(trace, "invalid_output", "{invalid-json", false, false, calls);
            }
            if ("json".equalsIgnoreCase(input.path("output_format").asText())) {
                Map<String, Object> structured = new LinkedHashMap<>();
                structured.put("response", "Deterministic mock response");
                structured.put("task_completed", true);
                skippedToolEvents(trace, "no tool selected", "no tool call expected");
                return finish(trace, "completed", mapper.writeValueAsString(structured), true, false, calls);
            }
            skippedToolEvents(trace, "no tool selected", "no tool call expected");
            return finish(trace, "completed", "Deterministic mock response", true, false, calls);
        }

        if (!REQUIRED.containsKey(tool)) {
            event(trace, "arguments_validated", "unregistered tool: " + tool, Map.of("tool", tool), "skipped", "Tool is not registered: " + tool, System.nanoTime());
            skippedInvocationEvents(trace, "unregistered tool", "Tool is not registered: " + tool);
            return finish(trace, "unsupported", "Tool is not registered: " + tool, false, false, calls);
        }

        JsonNode argsNode = input.path("tool_arguments");
        Map<String, Object> args = mapper.convertValue(argsNode.isObject() ? argsNode : mapper.createObjectNode(), Map.class);
        stepStarted = System.nanoTime();
        boolean allowedByList = contains(input.path("allowed_tools"), tool);
        boolean scopeGranted = "email_send".equals(tool) && contains(input.path("permissions"), "email:send");
        if (!allowedByList || ("email_send".equals(tool) && !scopeGranted)) {
            String denial = "权限不足或未授权，未执行该工具操作。";
            event(trace, "arguments_validated", "selected tool: " + tool, map("tool", tool, "arguments", args), "denied", denial, stepStarted);
            skippedInvocationEvents(trace, "authorization denied", denial);
            return finish(trace, "denied", denial, true, true, calls);
        }

        String argumentError = validateArguments(tool, argsNode);
        if (argumentError == null && "create_ticket".equals(tool)
                && !List.of("low", "medium", "high", "critical").contains(String.valueOf(args.get("priority")))) {
            argumentError = "priority must be low, medium, high, or critical";
        }
        if (argumentError != null) {
            Map<String, Object> invalidCall = call(tool, args);
            invalidCall.put("status", "invalid_arguments");
            invalidCall.put("error", argumentError);
            calls.add(invalidCall);
            event(trace, "arguments_validated", "selected tool: " + tool, map("tool", tool, "arguments", args), "error", argumentError, stepStarted);
            event(trace, "tool_executed", "validated arguments for " + tool, Map.of("tool", tool, "arguments", args), "skipped", null, System.nanoTime());
            event(trace, "tool_result", "tool invocation: " + tool, Map.of("tool_call", invalidCall), "error", argumentError, System.nanoTime());
            return finish(trace, "invalid_arguments", argumentError, false, false, calls);
        }
        event(trace, "arguments_validated", "selected tool: " + tool, map("tool", tool, "arguments", args), "success", null, stepStarted);

        String behavior = input.path("tool_behavior").asText("success");
        if (List.of("timeout", "exception", "invalid_output", "reject").contains(behavior)) {
            stepStarted = System.nanoTime();
            String callStatus = switch (behavior) { case "timeout" -> "timeout"; case "exception" -> "error"; case "invalid_output" -> "invalid_output"; default -> "rejected"; };
            String runStatus = switch (behavior) { case "timeout" -> "tool_timeout"; case "exception" -> "tool_error"; case "invalid_output" -> "tool_invalid_output"; default -> "tool_rejected"; };
            String error = "Mock tool behavior: " + behavior;
            Map<String, Object> failedCall = call(tool, args);
            failedCall.put("status", callStatus);
            failedCall.put("error", error);
            calls.add(failedCall);
            event(trace, "tool_executed", "validated arguments for " + tool, Map.of("tool", tool, "arguments", args), "error", error, stepStarted);
            event(trace, "tool_result", "tool invocation: " + tool, Map.of("tool_call", failedCall), "error", error, System.nanoTime());
            return finish(trace, runStatus, "Tool did not complete successfully: " + behavior, false, false, calls);
        }

        int repetitions = input.path("repeat_tool").asBoolean(false) ? 2 : 1;
        stepStarted = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            Map<String, Object> executed = call(tool, args);
            executed.put("status", "success");
            executed.put("result", Map.of("ok", true, "tool", tool, "message", "deterministic mock tool result"));
            calls.add(executed);
        }
        event(trace, "tool_executed", "validated arguments for " + tool,
                map("tool", tool, "arguments", args, "invocation_count", repetitions), "success", null, stepStarted);
        event(trace, "tool_result", "tool invocation: " + tool, map("tool_calls", calls), "success", null, System.nanoTime());

        boolean complete = !input.has("complete_task") || input.path("complete_task").asBoolean(true);
        String status = complete ? "completed" : "incomplete";
        String finalOutput = complete ? "Task completed using " + tool : "Tool returned, but task completion was not confirmed.";
        return finish(trace, status, finalOutput, complete, false, calls);
    }

    private Map<String, Object> finish(List<Map<String, Object>> trace, String status, String output,
                                       boolean complete, boolean refused, List<Map<String, Object>> calls) {
        long stepStarted = System.nanoTime();
        Map<String, Object> result = result(status, output, complete, refused, calls);
        String traceStatus = status.startsWith("tool_") || status.contains("invalid") ? "error" : "success";
        event(trace, "output_generated", "agent outcome", map("status", status, "output", output, "tool_call_count", calls.size()),
                traceStatus, traceStatus.equals("error") ? status : null, stepStarted);
        return result;
    }

    private void skippedToolEvents(List<Map<String, Object>> trace, String input, String reason) {
        event(trace, "arguments_validated", input, Map.of(), "skipped", reason, System.nanoTime());
        skippedInvocationEvents(trace, input, reason);
    }

    private void skippedInvocationEvents(List<Map<String, Object>> trace, String input, String reason) {
        event(trace, "tool_executed", input, Map.of(), "skipped", reason, System.nanoTime());
        event(trace, "tool_result", input, Map.of(), "skipped", reason, System.nanoTime());
    }

    private String validateArguments(String tool, JsonNode argsNode) {
        for (String required : REQUIRED.get(tool)) {
            JsonNode value = argsNode.path(required);
            if (value.isMissingNode() || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
                return "Required string argument is missing or invalid: " + required;
            }
        }
        var fields = argsNode.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!REQUIRED.get(tool).contains(field)) return "Unexpected argument: " + field;
        }
        return null;
    }

    private Map<String, Object> call(String tool, Map<String, Object> args) {
        Map<String, Object> call = new LinkedHashMap<>();
        call.put("name", tool);
        call.put("arguments", args);
        return call;
    }

    private String selectTool(String prompt, JsonNode input) {
        if (input.path("no_tool").asBoolean(false)) return null;
        String text = prompt.toLowerCase();
        if (text.contains("database_backup")) return "database_backup";
        if (text.contains("email") || text.contains("邮件")) return "email_send";
        if (text.contains("支付失败") || text.contains("退款申请") || text.contains("创建工单")) return "create_ticket";
        // Deliberately simple routing exposes ambiguous intent as a testable failure.
        if (text.contains("search") || text.contains("搜索网页") || text.contains("搜索天气")) return "web_search";
        if (text.contains("weather") || text.contains("天气")) return "weather_lookup";
        return null;
    }

    private boolean contains(JsonNode list, String value) {
        if (list.isArray()) for (JsonNode item : list) if (value.equals(item.asText())) return true;
        return false;
    }

    private Map<String, Object> result(String status, String output, boolean complete, boolean refused, List<Map<String, Object>> calls) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", status);
        value.put("final_output", output);
        value.put("task_completed", complete);
        value.put("refused", refused);
        value.put("tool_calls", calls);
        if (status.contains("error") || status.contains("timeout") || status.contains("invalid")) value.put("error", output);
        return value;
    }

    private Map<String, Object> map(Object... entries) {
        Map<String, Object> value = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) value.put((String) entries[i], entries[i + 1]);
        return value;
    }

    private void event(List<Map<String, Object>> trace, String type, String input, Map<String, Object> output,
                       String status, String error, long startedNanos) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("event_id", java.util.UUID.randomUUID().toString());
        e.put("sequence", trace.size());
        e.put("event_type", type);
        e.put("timestamp", java.time.LocalDateTime.now().toString());
        e.put("duration_ms", elapsedMillis(startedNanos));
        e.put("input_summary", truncate(input));
        e.put("output_summary", output);
        e.put("status", status);
        e.put("error", error);
        trace.add(e);
    }

    private double elapsedMillis(long startedNanos) {
        return Math.round((System.nanoTime() - startedNanos) / 1_000_000.0 * 1000.0) / 1000.0;
    }

    private String truncate(String value) { return value.length() > 240 ? value.substring(0, 240) + "…" : value; }
}
