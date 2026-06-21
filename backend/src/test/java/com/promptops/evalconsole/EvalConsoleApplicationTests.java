package com.promptops.evalconsole;

import com.promptops.evalconsole.service.PromptOpsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class EvalConsoleApplicationTests {

    @Autowired
    private PromptOpsService promptOpsService;

    @Test
    void contextLoadsAndSeedsDemoData() {
        assertThat(promptOpsService.listPromptProfiles()).isNotEmpty();
    }

    @Test
    void exposesThePromptOpsEvaluationLoop() {
        var prompt = promptOpsService.listPromptProfiles().get(0);
        var currentRun = promptOpsService.latestRunForTemplate(prompt.id());
        var selectedCase = currentRun.cases().get(0);
        var comparison = promptOpsService.latestComparison(prompt.id());

        assertThat(currentRun.cases()).isNotEmpty();
        assertThat(currentRun.totalCases()).isEqualTo(currentRun.passedCases() + currentRun.failedCases());
        assertThat(currentRun.cases()).hasSize(currentRun.totalCases());
        assertThat(currentRun.cases().stream().filter(caseResult -> caseResult.reviewRequired()).count())
                .isEqualTo(2);
        assertThat(selectedCase.ruleChecks()).isNotEmpty();
        assertThat(promptOpsService.traces(currentRun.id(), selectedCase.caseId())).isNotEmpty();
        assertThat(comparison.fromVersion()).isNotEqualTo(comparison.toVersion());
        assertThat(comparison.passRateAfter()).isGreaterThanOrEqualTo(comparison.passRateBefore());
        assertThat(comparison.topFailureReasons()).isEmpty();

        var summary = promptOpsService.summary();
        assertThat(summary.evalRuns()).isPositive();
        assertThat(summary.promptVersions()).isGreaterThanOrEqualTo(2);
        assertThat(summary.testCases()).isPositive();
    }
}
