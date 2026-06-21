package com.promptops.evalconsole.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.evalconsole.persistence.entity.ScoreRuleEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator(new ObjectMapper());

    @Test
    void appliesWeightedRulesAndExplainsFailures() {
        RuleEvaluation evaluation = evaluator.evaluate(
                "{\"decision\":\"pass_candidate\"}",
                List.of(
                        rule("MUST_CONTAIN", "pass_candidate", null, "3"),
                        rule("MUST_CONTAIN", "risk_notice", null, "1")
                )
        );

        assertThat(evaluation.score()).isEqualTo(75);
        assertThat(evaluation.status()).isEqualTo("fail");
        assertThat(evaluation.failures()).containsExactly("输出缺少必需关键词: risk_notice");
    }

    @Test
    void rejectsMalformedJsonInsteadOfCheckingBracesOnly() {
        RuleEvaluation evaluation = evaluator.evaluate(
                "{not-valid-json}",
                List.of(rule("FORMAT", null, "JSON", "1"))
        );

        assertThat(evaluation.score()).isZero();
        assertThat(evaluation.status()).isEqualTo("format-error");
        assertThat(evaluation.checks().get(0).passed()).isFalse();
    }

    @Test
    void requiresANonBlankRiskNoticeFieldOrExplicitChineseLabel() {
        RuleEvaluation blankNotice = evaluator.evaluate(
                "{\"risk_notice\":\"\"}",
                List.of(rule("RISK_NOTICE_REQUIRED", null, null, "1"))
        );
        RuleEvaluation explicitNotice = evaluator.evaluate(
                "风险提示：需要人工复核高影响场景。",
                List.of(rule("RISK_NOTICE_REQUIRED", null, null, "1"))
        );

        assertThat(blankNotice.status()).isEqualTo("risk-missing");
        assertThat(explicitNotice.status()).isEqualTo("pass");
    }

    private ScoreRuleEntity rule(String type, String value, String format, String weight) {
        ScoreRuleEntity rule = new ScoreRuleEntity();
        rule.setRuleType(type);
        rule.setRuleValue(value);
        rule.setExpectedFormat(format);
        rule.setWeight(new BigDecimal(weight));
        rule.setEnabled(true);
        return rule;
    }
}
