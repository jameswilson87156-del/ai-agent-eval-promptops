package com.promptops.evalconsole.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EvaluationPolicy {

    public boolean requiresReview(String resultStatus, String riskLevel) {
        return !"pass".equalsIgnoreCase(resultStatus) || isHighImpactRisk(riskLevel);
    }

    public boolean isHighImpactRisk(String riskLevel) {
        return "high".equalsIgnoreCase(riskLevel) || "critical".equalsIgnoreCase(riskLevel);
    }

    public String regressionRisk(
            BigDecimal passRateBefore,
            BigDecimal passRateAfter,
            int failedCasesBefore,
            int failedCasesAfter,
            int latencyBeforeMs,
            int latencyAfterMs
    ) {
        BigDecimal passDelta = passRateAfter.subtract(passRateBefore);
        int latencyDelta = latencyAfterMs - latencyBeforeMs;
        if (passDelta.compareTo(BigDecimal.valueOf(-5)) < 0 || failedCasesAfter > failedCasesBefore) {
            return "high";
        }
        if (passDelta.compareTo(BigDecimal.ZERO) < 0 || latencyDelta > 180) {
            return "medium";
        }
        return "low";
    }
}
