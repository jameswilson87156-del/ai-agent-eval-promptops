package com.promptops.evalconsole.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationPolicyTest {

    private final EvaluationPolicy policy = new EvaluationPolicy();

    @Test
    void queuesFailuresAndHighImpactPassesForReview() {
        assertThat(policy.requiresReview("fail", "normal")).isTrue();
        assertThat(policy.requiresReview("pass", "high")).isTrue();
        assertThat(policy.requiresReview("pass", "critical")).isTrue();
        assertThat(policy.requiresReview("pass", "medium")).isFalse();
    }

    @Test
    void classifiesPassRateRegressionAsHighRisk() {
        assertThat(policy.regressionRisk(
                BigDecimal.valueOf(90), BigDecimal.valueOf(80), 1, 1, 500, 520
        )).isEqualTo("high");
    }

    @Test
    void classifiesLargeLatencyIncreaseAsMediumRisk() {
        assertThat(policy.regressionRisk(
                BigDecimal.valueOf(90), BigDecimal.valueOf(90), 1, 1, 500, 720
        )).isEqualTo("medium");
    }

    @Test
    void keepsStableOrImprovedRunsAtLowRisk() {
        assertThat(policy.regressionRisk(
                BigDecimal.valueOf(80), BigDecimal.valueOf(90), 2, 1, 500, 520
        )).isEqualTo("low");
    }
}
