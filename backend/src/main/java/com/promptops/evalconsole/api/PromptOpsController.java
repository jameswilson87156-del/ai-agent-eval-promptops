package com.promptops.evalconsole.api;

import com.promptops.evalconsole.api.dto.ConsoleDtos.ComparisonDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateCaseRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateDatasetRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptTemplateRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptVersionRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateRuleRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.EvalRunDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.GenerationTraceDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptProfileDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.RunEvalRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.SummaryDto;
import com.promptops.evalconsole.persistence.entity.EvalCaseEntity;
import com.promptops.evalconsole.persistence.entity.EvalDatasetEntity;
import com.promptops.evalconsole.persistence.entity.EvalRunEntity;
import com.promptops.evalconsole.persistence.entity.PromptTemplateEntity;
import com.promptops.evalconsole.persistence.entity.PromptVersionEntity;
import com.promptops.evalconsole.persistence.entity.ScoreRuleEntity;
import com.promptops.evalconsole.service.PromptOpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5174", "http://127.0.0.1:5174", "http://localhost:5173", "http://127.0.0.1:5173"})
@Tag(name = "PromptOps", description = "Prompt templates, versions, eval runs, case results, and generation traces")
public class PromptOpsController {

    private final PromptOpsService promptOpsService;

    public PromptOpsController(PromptOpsService promptOpsService) {
        this.promptOpsService = promptOpsService;
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get dashboard summary")
    public SummaryDto summary() {
        return promptOpsService.summary();
    }

    @GetMapping("/prompts")
    @Operation(summary = "List prompt profiles")
    public List<PromptProfileDto> prompts() {
        return promptOpsService.listPromptProfiles();
    }

    @PostMapping("/prompt-templates")
    @Operation(summary = "Create prompt template")
    public PromptTemplateEntity createPromptTemplate(@Valid @RequestBody CreatePromptTemplateRequest request) {
        return promptOpsService.createPromptTemplate(request);
    }

    @PostMapping("/prompt-versions")
    @Operation(summary = "Create prompt version")
    public PromptVersionEntity createPromptVersion(@Valid @RequestBody CreatePromptVersionRequest request) {
        return promptOpsService.createPromptVersion(request);
    }

    @PostMapping("/eval-datasets")
    @Operation(summary = "Create eval dataset")
    public EvalDatasetEntity createDataset(@Valid @RequestBody CreateDatasetRequest request) {
        return promptOpsService.createDataset(request);
    }

    @PostMapping("/eval-cases")
    @Operation(summary = "Create eval case")
    public EvalCaseEntity createCase(@Valid @RequestBody CreateCaseRequest request) {
        return promptOpsService.createCase(request);
    }

    @PostMapping("/score-rules")
    @Operation(summary = "Create score rule")
    public ScoreRuleEntity createRule(@Valid @RequestBody CreateRuleRequest request) {
        return promptOpsService.createRule(request);
    }

    @PostMapping("/eval-runs")
    @Operation(summary = "Run rule-based evaluation")
    public EvalRunDto runEval(@Valid @RequestBody RunEvalRequest request) {
        EvalRunEntity run = promptOpsService.runEval(request.promptVersionId(), request.datasetId());
        return promptOpsService.toEvalRunDto(run);
    }

    @GetMapping("/prompts/{promptId}/eval-runs/current")
    @Operation(summary = "Get current eval run")
    public EvalRunDto latestRun(@PathVariable Long promptId) {
        return promptOpsService.latestRunForTemplate(promptId);
    }

    @GetMapping("/prompts/{promptId}/comparisons/latest")
    @Operation(summary = "Get latest version comparison")
    public ComparisonDto comparison(@PathVariable Long promptId) {
        return promptOpsService.latestComparison(promptId);
    }

    @GetMapping("/generation-traces")
    @Operation(summary = "List generation traces")
    public List<GenerationTraceDto> traces(@RequestParam(required = false) Long runId, @RequestParam(required = false) Long caseId) {
        return promptOpsService.traces(runId, caseId);
    }
}
