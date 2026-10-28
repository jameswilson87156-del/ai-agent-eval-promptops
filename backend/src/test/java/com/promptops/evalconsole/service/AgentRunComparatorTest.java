package com.promptops.evalconsole.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunComparatorTest {
    private final AgentRunComparator comparator = new AgentRunComparator();

    @Test
    void classifiesCaseTransitionsAndReportsAddedAndMissingCases() {
        Map<String, Object> baseline = run("mock", "deterministic-mock", "fixed-prompt", "mock-agent-v1", "1.0", "same", 40.0,
                List.of(caseResult("a", "security", "passed"), caseResult("b", "tool", "failed"),
                        caseResult("c", "tool", "failed"), caseResult("d", "normal", "passed"), caseResult("e", "boundary", "failed")));
        Map<String, Object> candidate = run("mock", "deterministic-mock", "fixed-prompt", "mock-agent-v1", "1.0", "same", 50.0,
                List.of(caseResult("a", "security", "failed"), caseResult("b", "tool", "passed"),
                        caseResult("c", "tool", "failed"), caseResult("d", "normal", "passed"), caseResult("f", "boundary", "passed")));

        Map<String, Object> comparison = comparator.compare(baseline, candidate);
        Map<String, Object> summary = (Map<String, Object>) comparison.get("summary");
        assertThat(comparison).containsEntry("comparison_semantics", "same_configuration_run_comparison")
                .containsEntry("is_version_regression", false);
        assertThat(summary).containsEntry("new_failures", 1).containsEntry("fixed_successes", 1)
                .containsEntry("persistent_failures", 1).containsEntry("persistent_passes", 1)
                .containsEntry("added_cases", 1).containsEntry("missing_cases", 1).containsEntry("pass_rate_delta", 10.0);
        assertThat((List<Map<String, Object>>) comparison.get("case_changes"))
                .extracting(row -> row.get("change"))
                .contains("new_failure", "fixed_success", "persistent_failure", "persistent_pass", "added_case", "missing_case");
    }

    @Test
    void versionLabelsAloneAreNotReportedAsRuntimeRegression() {
        Map<String, Object> baseline = run("openai_compatible", "model-a", "prompt-v1", "agent-v1", "1.0", "hash-a", 80.0,
                List.of(caseResult("a", "normal", "passed")));
        Map<String, Object> candidate = run("openai_compatible", "model-a", "prompt-v2", "agent-v1", "1.0", "hash-b", 100.0,
                List.of(caseResult("a", "normal", "passed")));
        Map<String, Object> labelComparison = comparator.compare(baseline, candidate);
        assertThat(labelComparison).containsEntry("comparison_semantics", "configuration_label_only_comparison")
                .containsEntry("is_version_regression", false)
                .containsEntry("configuration_changed", false)
                .containsEntry("configuration_labels_changed", true);

        Map<String, Object> changedModel = run("openai_compatible", "model-b", "prompt-v2", "agent-v1", "1.0", "hash-c", 100.0,
                List.of(caseResult("a", "normal", "passed")));
        Map<String, Object> providerComparison = comparator.compare(baseline, changedModel);
        assertThat(providerComparison).containsEntry("comparison_semantics", "provider_configuration_comparison")
                .containsEntry("is_version_regression", true)
                .containsEntry("configuration_changed", true);

        Map<String, Object> mockCandidate = run("mock", "deterministic-mock", "mock-tag-2", "mock-agent-v2", "1.0", "hash-c", 80.0,
                List.of(caseResult("a", "normal", "passed")));
        Map<String, Object> mockComparison = comparator.compare(
                run("mock", "deterministic-mock", "mock-tag-1", "mock-agent-v1", "1.0", "hash-d", 80.0,
                        List.of(caseResult("a", "normal", "passed"))), mockCandidate);
        assertThat(mockComparison).containsEntry("comparison_semantics", "mock_metadata_only_comparison")
                .containsEntry("is_version_regression", false);
    }

    @Test
    void labelsDatasetChangeAndLegacyMetadataWithoutVersionClaim() {
        Map<String, Object> oldRun = run("mock", "deterministic-mock", "fixed", "mock-v1", "1.0", "hash-a", 100.0,
                List.of(caseResult("old", "legacy", "passed")));
        Map<String, Object> newRun = run("mock", "deterministic-mock", "fixed", "mock-v1", "2.0", "hash-b", 100.0,
                List.of(caseResult("new", "new-category", "passed")));
        Map<String, Object> datasetCompare = comparator.compare(oldRun, newRun);
        assertThat(datasetCompare).containsEntry("comparison_semantics", "dataset_change_comparison")
                .containsEntry("dataset_changed", true).containsEntry("is_version_regression", false);

        oldRun.remove("config_fingerprint");
        newRun.remove("config_fingerprint");
        oldRun.remove("runtime_config_fingerprint");
        newRun.remove("runtime_config_fingerprint");
        Map<String, Object> legacyCompare = comparator.compare(oldRun, newRun);
        assertThat(legacyCompare).containsEntry("comparison_semantics", "dataset_change_comparison")
                .containsEntry("configuration_metadata_complete", false);
    }

    private Map<String, Object> run(String provider, String model, String promptVersion, String agentVersion,
                                    String datasetVersion, String fingerprint, double passRate,
                                    List<Map<String, Object>> cases) {
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("run_id", "run-" + fingerprint);
        run.put("provider_type", provider); run.put("model_name", model); run.put("prompt_version", promptVersion);
        run.put("agent_config_version", agentVersion); run.put("dataset_version", datasetVersion);
        if (fingerprint != null) run.put("config_fingerprint", fingerprint);
        run.put("runtime_config_fingerprint", provider + "|" + model + "|" + datasetVersion);
        run.put("started_at", "2026-10-28T12:00:00"); run.put("completed_at", "2026-10-28T12:00:01"); run.put("duration_ms", 1000);
        run.put("summary", Map.of("pass_rate", passRate)); run.put("cases", new ArrayList<>(cases));
        return run;
    }

    private Map<String, Object> caseResult(String id, String category, String status) {
        return Map.of("case_id", id, "title", id, "category", category, "case_status", status,
                "failure_reason", "passed".equals(status) ? List.of() : List.of("expected behavior mismatch"));
    }
}
