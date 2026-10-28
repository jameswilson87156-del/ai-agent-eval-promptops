package com.promptops.evalconsole.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/** Low-cardinality counters and duration for the local production-like test environment. */
@Component
public class ProductionMetrics {
    private static final Map<String, Boolean> CASE_STATUSES = Map.of(
            "passed", true, "failed", true, "error", true, "skipped", true);

    private final MeterRegistry registry;

    public ProductionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAgentEvaluation(String runMode, String qualityGate, int passed, int failed,
                                      int errors, int skipped, int providerErrors, int evaluatorErrors,
                                      long durationMillis) {
        String safeMode = "real_model".equals(runMode) ? "real_model" : "mock";
        String safeGate = "passed".equals(qualityGate) ? "passed" : "failed";
        registry.counter("promptops.agent.eval.runs", "run_mode", safeMode, "quality_gate", safeGate).increment();
        addCases(safeMode, "passed", passed);
        addCases(safeMode, "failed", failed);
        addCases(safeMode, "error", errors);
        addCases(safeMode, "skipped", skipped);
        if (providerErrors > 0) {
            registry.counter("promptops.agent.eval.provider.errors", "run_mode", safeMode).increment(providerErrors);
        }
        if (evaluatorErrors > 0) {
            registry.counter("promptops.agent.eval.evaluator.errors", "run_mode", safeMode).increment(evaluatorErrors);
        }
        registry.timer("promptops.agent.eval.run.duration", "run_mode", safeMode)
                .record(Duration.ofMillis(Math.max(0, durationMillis)));
    }

    public void recordBusinessMutation(String operation, boolean replayed) {
        String safeOperation = switch (operation) {
            case "order_create", "order_cancel", "payment_create", "refund_create" -> operation;
            default -> "other";
        };
        registry.counter("promptops.business.mutations", "operation", safeOperation,
                "outcome", replayed ? "replayed" : "success").increment();
    }

    private void addCases(String runMode, String status, int count) {
        if (count <= 0 || !CASE_STATUSES.containsKey(status)) return;
        registry.counter("promptops.agent.eval.cases", "run_mode", runMode, "status", status).increment(count);
    }
}
