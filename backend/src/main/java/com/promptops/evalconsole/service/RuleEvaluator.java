package com.promptops.evalconsole.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.evalconsole.api.dto.ConsoleDtos.RuleCheckDto;
import com.promptops.evalconsole.persistence.entity.ScoreRuleEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RuleEvaluator {

    private final ObjectMapper objectMapper;

    public RuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RuleEvaluation evaluate(String output, List<ScoreRuleEntity> rules) {
        List<RuleCheckDto> checks = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal passedWeight = BigDecimal.ZERO;
        List<String> failures = new ArrayList<>();

        for (ScoreRuleEntity rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }
            BigDecimal weight = rule.getWeight() == null ? BigDecimal.ONE : rule.getWeight();
            totalWeight = totalWeight.add(weight);
            RuleCheckDto check = checkRule(output, rule);
            checks.add(check);
            if (check.passed()) {
                passedWeight = passedWeight.add(weight);
            } else {
                failures.add(check.explanation());
            }
        }

        int score = totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? 100
                : passedWeight.multiply(BigDecimal.valueOf(100)).divide(totalWeight, 0, java.math.RoundingMode.HALF_UP).intValue();
        String status = failures.isEmpty() ? "pass" : deriveFailureStatus(failures);
        return new RuleEvaluation(score, status, failures, checks);
    }

    private RuleCheckDto checkRule(String output, ScoreRuleEntity rule) {
        String type = rule.getRuleType().toUpperCase(Locale.ROOT);
        String expected = rule.getRuleValue() == null ? "" : rule.getRuleValue();
        return switch (type) {
            case "MUST_CONTAIN" -> containsRule(output, expected, true);
            case "FORBIDDEN_WORD" -> containsRule(output, expected, false);
            case "RISK_NOTICE_REQUIRED" -> riskRule(output);
            case "FORMAT" -> formatRule(output, rule.getExpectedFormat());
            default -> new RuleCheckDto(type, expected, false, "未知规则类型: " + rule.getRuleType());
        };
    }

    private RuleCheckDto containsRule(String output, String expected, boolean mustContain) {
        boolean contains = output != null && output.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
        boolean passed = mustContain == contains;
        String type = mustContain ? "MUST_CONTAIN" : "FORBIDDEN_WORD";
        String explanation = passed
                ? (mustContain ? "输出包含必需关键词: " : "输出未出现禁用词: ") + expected
                : (mustContain ? "输出缺少必需关键词: " : "输出出现禁用词: ") + expected;
        return new RuleCheckDto(type, expected, passed, explanation);
    }

    private RuleCheckDto riskRule(String output) {
        boolean passed = hasJsonRiskNotice(output) || (output != null && output.contains("风险提示"));
        return new RuleCheckDto(
                "RISK_NOTICE_REQUIRED",
                "风险提示",
                passed,
                passed ? "输出包含风险提示" : "输出缺少风险提示"
        );
    }

    private RuleCheckDto formatRule(String output, String expectedFormat) {
        String format = expectedFormat == null ? "TEXT" : expectedFormat.toUpperCase(Locale.ROOT);
        boolean passed = switch (format) {
            case "JSON" -> looksLikeJson(output);
            case "MARKDOWN" -> output != null && (output.contains("#") || output.contains("- ") || output.contains("```"));
            case "TEXT" -> output != null && !output.isBlank() && !looksLikeJson(output);
            default -> false;
        };
        return new RuleCheckDto(
                "FORMAT",
                format,
                passed,
                passed ? "输出符合 " + format + " 格式要求" : "输出不符合 " + format + " 格式要求"
        );
    }

    private boolean looksLikeJson(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(output);
            return node != null && (node.isObject() || node.isArray());
        } catch (JsonProcessingException ignored) {
            return false;
        }
    }

    private boolean hasJsonRiskNotice(String output) {
        if (!looksLikeJson(output)) {
            return false;
        }
        try {
            JsonNode notice = objectMapper.readTree(output).path("risk_notice");
            return notice.isTextual() && !notice.asText().isBlank();
        } catch (JsonProcessingException ignored) {
            return false;
        }
    }

    private String deriveFailureStatus(List<String> failures) {
        if (failures.stream().anyMatch(reason -> reason.contains("格式"))) {
            return "format-error";
        }
        if (failures.stream().anyMatch(reason -> reason.contains("风险"))) {
            return "risk-missing";
        }
        return "fail";
    }
}
