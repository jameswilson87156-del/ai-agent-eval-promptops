package com.promptops.evalconsole.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ConsoleDtos {

    private ConsoleDtos() {
    }

    public record SummaryDto(
            int promptVersions,
            int evalRuns,
            int testCases,
            int latestPassRate,
            int averageLatencyMs,
            int pendingReviews,
            int highRiskCases
    ) {
    }

    public record PromptProfileDto(
            Long id,
            String key,
            String name,
            List<String> tags,
            List<String> versionLabels,
            Long currentVersionId,
            String currentVersion,
            String status,
            String owner,
            String updatedAt,
            String template,
            List<PromptVariableDto> variables,
            List<String> scoringRules,
            List<String> outputRequirements,
            Long datasetId
    ) {
    }

    public record PromptVariableDto(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 240) String description,
            boolean required
    ) {
    }

    public record EvalRunDto(
            Long id,
            String runKey,
            String name,
            Long promptId,
            Long promptVersionId,
            Long datasetId,
            String promptSnapshot,
            String status,
            int totalCases,
            int passedCases,
            int failedCases,
            int riskCaseCount,
            BigDecimal passRate,
            int averageLatencyMs,
            List<CaseResultDto> cases
    ) {
    }

    public record CaseResultDto(
            Long resultId,
            Long caseId,
            String caseKey,
            String name,
            String dataset,
            String input,
            String expected,
            String result,
            String riskLevel,
            boolean reviewRequired,
            int score,
            int latencyMs,
            String reason,
            String outputSummary,
            String mockOutput,
            List<String> trace,
            List<RuleCheckDto> ruleChecks
    ) {
    }

    public record RuleCheckDto(
            String ruleType,
            String expected,
            boolean passed,
            String explanation
    ) {
    }

    public record ComparisonDto(
            Long promptId,
            String fromVersion,
            String toVersion,
            int passRateBefore,
            int passRateAfter,
            int latencyBeforeMs,
            int latencyAfterMs,
            String regressionRisk,
            List<String> topFailureReasons
    ) {
    }

    public record GenerationTraceDto(
            Long id,
            Long evalRunId,
            Long evalCaseId,
            Long promptVersionId,
            String inputSummary,
            String outputSummary,
            int latencyMs,
            String status,
            String errorReason,
            LocalDateTime createdAt
    ) {
    }

    public record ErrorResponse(
            int code,
            String message,
            LocalDateTime timestamp,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorItem> errors
    ) {
    }

    public record FieldErrorItem(
            String field,
            String message
    ) {
    }

    public record CreatePromptTemplateRequest(
            @NotBlank @Size(max = 80) String templateKey,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 128) String scenario,
            @Size(max = 512) String description,
            @Size(max = 128) String ownerTeam,
            @Pattern(regexp = "Draft|Candidate|Approved") String status
    ) {
    }

    public record CreatePromptVersionRequest(
            @NotNull @Positive Long templateId,
            @NotBlank @Size(max = 32) String versionLabel,
            @NotBlank String templateText,
            @NotEmpty List<@Valid PromptVariableDto> variables,
            @NotBlank @Pattern(regexp = "JSON|MARKDOWN|TEXT", flags = Pattern.Flag.CASE_INSENSITIVE) String outputFormat,
            @Pattern(regexp = "Draft|Candidate|Approved") String status,
            @Size(max = 128) String author
    ) {
    }

    public record CreateDatasetRequest(
            @NotBlank @Size(max = 80) String datasetKey,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 128) String scenario,
            @Size(max = 512) String description
    ) {
    }

    public record CreateCaseRequest(
            @NotNull @Positive Long datasetId,
            @NotBlank @Size(max = 80) String caseKey,
            @NotBlank @Size(max = 180) String title,
            @NotBlank String inputText,
            @NotBlank String expectedText,
            @Pattern(regexp = "normal|medium|high|critical", flags = Pattern.Flag.CASE_INSENSITIVE) String riskLevel,
            List<String> tags
    ) {
    }

    public record CreateRuleRequest(
            @NotNull @Positive Long caseId,
            @NotBlank @Pattern(regexp = "MUST_CONTAIN|FORBIDDEN_WORD|RISK_NOTICE_REQUIRED|FORMAT", flags = Pattern.Flag.CASE_INSENSITIVE) String ruleType,
            @Size(max = 512) String ruleValue,
            @Pattern(regexp = "JSON|MARKDOWN|TEXT", flags = Pattern.Flag.CASE_INSENSITIVE) String expectedFormat,
            @DecimalMin(value = "0.01") BigDecimal weight,
            @Size(max = 512) String description
    ) {
    }

    public record RunEvalRequest(@NotNull @Positive Long promptVersionId, @NotNull @Positive Long datasetId) {
    }
}
