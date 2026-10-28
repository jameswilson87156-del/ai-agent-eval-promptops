package com.promptops.evalconsole.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compares two persisted run snapshots and keeps run comparison separate from configuration regression claims. */
@Component
public class AgentRunComparator {
    public Map<String, Object> compare(Map<String, Object> baseline, Map<String, Object> candidate) {
        Map<String, Object> baselineMetadata = metadata(baseline);
        Map<String, Object> candidateMetadata = metadata(candidate);
        String baseDataset = string(baseline, "dataset_version", "unknown");
        String candidateDataset = string(candidate, "dataset_version", "unknown");
        String baseFingerprint = string(baseline, "config_fingerprint", "legacy-unknown");
        String candidateFingerprint = string(candidate, "config_fingerprint", "legacy-unknown");
        String baseRuntimeFingerprint = string(baseline, "runtime_config_fingerprint", "legacy-unknown");
        String candidateRuntimeFingerprint = string(candidate, "runtime_config_fingerprint", "legacy-unknown");
        boolean sameDataset = baseDataset.equals(candidateDataset);
        boolean configurationMetadataComplete = !"legacy-unknown".equals(baseFingerprint)
                && !"legacy-unknown".equals(candidateFingerprint)
                && !"legacy-unknown".equals(baseRuntimeFingerprint)
                && !"legacy-unknown".equals(candidateRuntimeFingerprint);
        boolean sameConfiguration = configurationMetadataComplete && baseFingerprint.equals(candidateFingerprint);
        boolean sameRuntimeConfiguration = configurationMetadataComplete
                && baseRuntimeFingerprint.equals(candidateRuntimeFingerprint);
        boolean bothMock = "mock".equals(baselineMetadata.get("provider_type"))
                && "mock".equals(candidateMetadata.get("provider_type"));
        boolean providerIdentityChanged = !same(baselineMetadata.get("provider_type"), candidateMetadata.get("provider_type"))
                || !same(baselineMetadata.get("model_name"), candidateMetadata.get("model_name"));
        boolean versionLabelChanged = !same(baselineMetadata.get("prompt_version"), candidateMetadata.get("prompt_version"))
                || !same(baselineMetadata.get("agent_config_version"), candidateMetadata.get("agent_config_version"))
                || !same(baselineMetadata.get("provider_config_version"), candidateMetadata.get("provider_config_version"));
        boolean runtimeConfigurationChanged = configurationMetadataComplete
                && (!sameRuntimeConfiguration || providerIdentityChanged);

        String semantics;
        boolean versionRegression = false;
        if (!sameDataset) {
            semantics = "dataset_change_comparison";
        } else if (!configurationMetadataComplete) {
            semantics = "legacy_metadata_missing_run_comparison";
        } else if (bothMock && runtimeConfigurationChanged) {
            semantics = "mock_runtime_configuration_comparison";
        } else if (bothMock) {
            semantics = sameConfiguration ? "same_configuration_run_comparison" : "mock_metadata_only_comparison";
        } else if (runtimeConfigurationChanged) {
            semantics = "provider_configuration_comparison";
            versionRegression = true;
        } else if (versionLabelChanged || !sameConfiguration) {
            semantics = "configuration_label_only_comparison";
        } else {
            semantics = "same_configuration_run_comparison";
        }

        List<Map<String, Object>> baselineCases = caseList(baseline);
        List<Map<String, Object>> candidateCases = caseList(candidate);
        Map<String, Map<String, Object>> before = index(baselineCases);
        Map<String, Map<String, Object>> after = index(candidateCases);
        Set<String> orderedIds = new LinkedHashSet<>();
        candidateCases.forEach(value -> orderedIds.add(string(value, "case_id", "")));
        baselineCases.forEach(value -> orderedIds.add(string(value, "case_id", "")));

        List<Map<String, Object>> changes = new ArrayList<>();
        int added = 0, missing = 0, newFailures = 0, fixed = 0, persistentFailures = 0, persistentPasses = 0;
        for (String caseId : orderedIds) {
            Map<String, Object> oldCase = before.get(caseId);
            Map<String, Object> newCase = after.get(caseId);
            String oldStatus = oldCase == null ? "missing" : string(oldCase, "case_status", "error");
            String newStatus = newCase == null ? "missing" : string(newCase, "case_status", "error");
            String change;
            if (oldCase == null) { change = "added_case"; added++; }
            else if (newCase == null) { change = "missing_case"; missing++; }
            else if (isPassed(oldStatus) && !isPassed(newStatus)) { change = "new_failure"; newFailures++; }
            else if (!isPassed(oldStatus) && isPassed(newStatus)) { change = "fixed_success"; fixed++; }
            else if (isPassed(oldStatus)) { change = "persistent_pass"; persistentPasses++; }
            else { change = "persistent_failure"; persistentFailures++; }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("case_id", caseId);
            row.put("title", newCase != null ? newCase.get("title") : oldCase == null ? caseId : oldCase.get("title"));
            row.put("category", newCase != null ? newCase.get("category") : oldCase == null ? null : oldCase.get("category"));
            row.put("baseline_status", oldStatus);
            row.put("candidate_status", newStatus);
            row.put("change", change);
            row.put("baseline_failure_reason", oldCase == null ? null : oldCase.get("failure_reason"));
            row.put("candidate_failure_reason", newCase == null ? null : newCase.get("failure_reason"));
            changes.add(row);
        }

        Map<String, Map<String, Integer>> beforeCategories = categoryStats(baselineCases);
        Map<String, Map<String, Integer>> afterCategories = categoryStats(candidateCases);
        Set<String> categories = new LinkedHashSet<>();
        categories.addAll(beforeCategories.keySet());
        categories.addAll(afterCategories.keySet());
        List<Map<String, Object>> categoryChanges = new ArrayList<>();
        for (String category : categories) {
            Map<String, Integer> oldStats = beforeCategories.getOrDefault(category, zeroStats());
            Map<String, Integer> newStats = afterCategories.getOrDefault(category, zeroStats());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", category);
            row.put("baseline", oldStats);
            row.put("candidate", newStats);
            row.put("failed_delta", newStats.get("failed") - oldStats.get("failed"));
            row.put("case_count_delta", newStats.get("total") - oldStats.get("total"));
            categoryChanges.add(row);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("new_failures", newFailures);
        summary.put("fixed_successes", fixed);
        summary.put("persistent_failures", persistentFailures);
        summary.put("persistent_passes", persistentPasses);
        summary.put("added_cases", added);
        summary.put("missing_cases", missing);
        summary.put("pass_rate_delta", round(number(summary(candidate).get("pass_rate"))
                - number(summary(baseline).get("pass_rate"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("comparison_id", string(baseline, "run_id", "") + ".." + string(candidate, "run_id", ""));
        result.put("comparison_semantics", semantics);
        result.put("is_version_regression", versionRegression);
        result.put("dataset_changed", !sameDataset);
        result.put("configuration_metadata_complete", configurationMetadataComplete);
        result.put("configuration_changed", runtimeConfigurationChanged);
        result.put("configuration_labels_changed", versionLabelChanged);
        result.put("run_fingerprint_changed", configurationMetadataComplete && !sameConfiguration);
        result.put("baseline", baselineMetadata);
        result.put("candidate", candidateMetadata);
        result.put("baseline_summary", summary(baseline));
        result.put("candidate_summary", summary(candidate));
        result.put("summary", summary);
        result.put("case_changes", changes);
        result.put("category_changes", categoryChanges);
        result.put("metadata_changes", metadataChanges(baselineMetadata, candidateMetadata));
        return result;
    }

    private Map<String, Object> metadata(Map<String, Object> run) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("run_id", run.get("run_id"));
        value.put("dataset_version", run.get("dataset_version"));
        value.put("provider_type", run.getOrDefault("provider_type", run.getOrDefault("evaluator_mode", "unknown")));
        value.put("model_name", run.getOrDefault("model_name", "unknown"));
        value.put("prompt_version", run.getOrDefault("prompt_version", "unknown"));
        value.put("agent_config_version", run.getOrDefault("agent_config_version", "unknown"));
        value.put("config_fingerprint", run.getOrDefault("config_fingerprint", "legacy-unknown"));
        value.put("runtime_config_fingerprint", run.getOrDefault("runtime_config_fingerprint", "legacy-unknown"));
        value.put("provider_config_version", providerConfigVersion(run));
        value.put("started_at", run.get("started_at"));
        value.put("completed_at", run.get("completed_at"));
        value.put("duration_ms", run.getOrDefault("duration_ms", 0));
        return value;
    }

    private List<Map<String, Object>> caseList(Map<String, Object> report) {
        Object value = report.get("cases");
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) if (item instanceof Map<?, ?> map) result.add(castMap(map));
        return result;
    }

    private Map<String, Map<String, Object>> index(List<Map<String, Object>> cases) {
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        for (Map<String, Object> item : cases) values.put(string(item, "case_id", ""), item);
        return values;
    }

    private Map<String, Map<String, Integer>> categoryStats(List<Map<String, Object>> cases) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        for (Map<String, Object> item : cases) {
            String category = string(item, "category", "uncategorized");
            Map<String, Integer> stats = result.computeIfAbsent(category, ignored -> zeroStats());
            stats.compute("total", (key, value) -> value + 1);
            if (isPassed(string(item, "case_status", "error"))) stats.compute("passed", (key, value) -> value + 1);
            else stats.compute("failed", (key, value) -> value + 1);
        }
        return result;
    }

    private Map<String, Integer> zeroStats() {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("total", 0);
        values.put("passed", 0);
        values.put("failed", 0);
        return values;
    }

    private List<Map<String, Object>> metadataChanges(Map<String, Object> before, Map<String, Object> after) {
        List<Map<String, Object>> changes = new ArrayList<>();
        for (String key : List.of("dataset_version", "provider_type", "model_name", "prompt_version", "agent_config_version",
                "provider_config_version", "config_fingerprint", "runtime_config_fingerprint")) {
            if (!same(before.get(key), after.get(key))) changes.add(Map.of("field", key,
                    "baseline", String.valueOf(before.get(key)), "candidate", String.valueOf(after.get(key))));
        }
        return changes;
    }

    private Map<String, Object> summary(Map<String, Object> run) {
        Object value = run.get("summary");
        return value instanceof Map<?, ?> map ? castMap(map) : Map.of();
    }

    private Object providerConfigVersion(Map<String, Object> run) {
        Object config = run.get("provider_config");
        if (config instanceof Map<?, ?> map) {
            Object version = map.get("provider_config_version");
            return version == null ? "unknown" : version;
        }
        return "unknown";
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return copy;
    }

    private boolean isPassed(String status) { return "passed".equals(status); }
    private boolean same(Object left, Object right) { return java.util.Objects.equals(left, right); }
    private String string(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
    private double number(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ex) { return 0.0; }
    }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
