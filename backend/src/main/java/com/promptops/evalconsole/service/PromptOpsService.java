package com.promptops.evalconsole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.promptops.evalconsole.api.dto.ConsoleDtos;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CaseResultDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.ComparisonDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateCaseRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateDatasetRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptTemplateRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptVersionRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateRuleRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.EvalRunDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.GenerationTraceDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptProfileDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptVariableDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.SummaryDto;
import com.promptops.evalconsole.persistence.entity.EvalCaseEntity;
import com.promptops.evalconsole.persistence.entity.EvalCaseResultEntity;
import com.promptops.evalconsole.persistence.entity.EvalDatasetEntity;
import com.promptops.evalconsole.persistence.entity.EvalRunEntity;
import com.promptops.evalconsole.persistence.entity.GenerationTraceEntity;
import com.promptops.evalconsole.persistence.entity.PromptTemplateEntity;
import com.promptops.evalconsole.persistence.entity.PromptVersionEntity;
import com.promptops.evalconsole.persistence.entity.ScoreRuleEntity;
import com.promptops.evalconsole.persistence.mapper.EvalCaseMapper;
import com.promptops.evalconsole.persistence.mapper.EvalCaseResultMapper;
import com.promptops.evalconsole.persistence.mapper.EvalDatasetMapper;
import com.promptops.evalconsole.persistence.mapper.EvalRunMapper;
import com.promptops.evalconsole.persistence.mapper.GenerationTraceMapper;
import com.promptops.evalconsole.persistence.mapper.PromptTemplateMapper;
import com.promptops.evalconsole.persistence.mapper.PromptVersionMapper;
import com.promptops.evalconsole.persistence.mapper.ScoreRuleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PromptOpsService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final PromptTemplateMapper promptTemplateMapper;
    private final PromptVersionMapper promptVersionMapper;
    private final EvalDatasetMapper evalDatasetMapper;
    private final EvalCaseMapper evalCaseMapper;
    private final ScoreRuleMapper scoreRuleMapper;
    private final EvalRunMapper evalRunMapper;
    private final EvalCaseResultMapper evalCaseResultMapper;
    private final GenerationTraceMapper generationTraceMapper;
    private final RuleEvaluator ruleEvaluator;
    private final MockOutputGenerator mockOutputGenerator;
    private final EvaluationPolicy evaluationPolicy;

    public PromptOpsService(
            PromptTemplateMapper promptTemplateMapper,
            PromptVersionMapper promptVersionMapper,
            EvalDatasetMapper evalDatasetMapper,
            EvalCaseMapper evalCaseMapper,
            ScoreRuleMapper scoreRuleMapper,
            EvalRunMapper evalRunMapper,
            EvalCaseResultMapper evalCaseResultMapper,
            GenerationTraceMapper generationTraceMapper,
            RuleEvaluator ruleEvaluator,
            MockOutputGenerator mockOutputGenerator,
            EvaluationPolicy evaluationPolicy
    ) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.promptVersionMapper = promptVersionMapper;
        this.evalDatasetMapper = evalDatasetMapper;
        this.evalCaseMapper = evalCaseMapper;
        this.scoreRuleMapper = scoreRuleMapper;
        this.evalRunMapper = evalRunMapper;
        this.evalCaseResultMapper = evalCaseResultMapper;
        this.generationTraceMapper = generationTraceMapper;
        this.ruleEvaluator = ruleEvaluator;
        this.mockOutputGenerator = mockOutputGenerator;
        this.evaluationPolicy = evaluationPolicy;
    }

    public SummaryDto summary() {
        int promptVersions = Math.toIntExact(promptVersionMapper.selectCount(null));
        int evalRuns = Math.toIntExact(evalRunMapper.selectCount(null));
        int testCases = Math.toIntExact(evalCaseMapper.selectCount(null));
        EvalRunEntity latestRun = latestRun();
        int latestPassRate = latestRun == null ? 0 : latestRun.getPassRate().setScale(0, RoundingMode.HALF_UP).intValue();
        int averageLatencyMs = latestRun == null ? 0 : latestRun.getAverageLatencyMs();
        int pendingReviews = latestRun == null ? 0 : reviewQueueCount(latestRun.getId());
        int highRiskCases = latestRun == null ? 0 : highImpactCaseCount(latestRun.getId());
        return new SummaryDto(promptVersions, evalRuns, testCases, latestPassRate, averageLatencyMs, pendingReviews, highRiskCases);
    }

    public List<PromptProfileDto> listPromptProfiles() {
        List<PromptTemplateEntity> templates = promptTemplateMapper.selectList(null);
        return templates.stream()
                .sorted(Comparator.comparing(PromptTemplateEntity::getId))
                .map(this::toPromptProfile)
                .toList();
    }

    public EvalRunDto latestRunForTemplate(Long templateId) {
        PromptVersionEntity version = currentVersion(template(templateId));
        EvalDatasetEntity dataset = datasetForScenario(template(templateId).getScenario());
        EvalRunEntity run = evalRunMapper.selectList(new LambdaQueryWrapper<EvalRunEntity>()
                        .eq(EvalRunEntity::getPromptVersionId, version.getId())
                        .eq(EvalRunEntity::getDatasetId, dataset.getId())
                        .orderByDesc(EvalRunEntity::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElseGet(() -> runEval(version.getId(), dataset.getId()));
        return toEvalRunDto(run);
    }

    public ComparisonDto latestComparison(Long templateId) {
        PromptTemplateEntity template = template(templateId);
        List<PromptVersionEntity> versions = versions(templateId);
        if (versions.size() < 2) {
            EvalRunDto current = latestRunForTemplate(templateId);
            return new ComparisonDto(templateId, current.promptSnapshot(), current.promptSnapshot(),
                    current.passRate().intValue(), current.passRate().intValue(),
                    current.averageLatencyMs(), current.averageLatencyMs(), "low", List.of("暂无可比较版本"));
        }
        PromptVersionEntity to = versions.get(versions.size() - 1);
        PromptVersionEntity from = versions.get(versions.size() - 2);
        EvalDatasetEntity dataset = datasetForScenario(template.getScenario());
        EvalRunEntity fromRun = latestRunOrCreate(from.getId(), dataset.getId());
        EvalRunEntity toRun = latestRunOrCreate(to.getId(), dataset.getId());
        List<String> failures = topFailureReasons(toRun.getId());
        String risk = deriveRegressionRisk(fromRun, toRun);
        return new ComparisonDto(
                templateId,
                from.getVersionLabel(),
                to.getVersionLabel(),
                fromRun.getPassRate().setScale(0, RoundingMode.HALF_UP).intValue(),
                toRun.getPassRate().setScale(0, RoundingMode.HALF_UP).intValue(),
                fromRun.getAverageLatencyMs(),
                toRun.getAverageLatencyMs(),
                risk,
                failures
        );
    }

    public List<GenerationTraceDto> traces(Long runId, Long caseId) {
        LambdaQueryWrapper<GenerationTraceEntity> wrapper = new LambdaQueryWrapper<GenerationTraceEntity>()
                .orderByDesc(GenerationTraceEntity::getId);
        if (runId != null) {
            wrapper.eq(GenerationTraceEntity::getEvalRunId, runId);
        }
        if (caseId != null) {
            wrapper.eq(GenerationTraceEntity::getEvalCaseId, caseId);
        }
        wrapper.last("LIMIT 20");
        return generationTraceMapper.selectList(wrapper).stream().map(this::toTraceDto).toList();
    }

    @Transactional
    public PromptTemplateEntity createPromptTemplate(CreatePromptTemplateRequest request) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setTemplateKey(request.templateKey());
        entity.setName(request.name());
        entity.setScenario(request.scenario());
        entity.setDescription(request.description());
        entity.setOwnerTeam(request.ownerTeam());
        entity.setStatus(blankToDefault(request.status(), "Draft"));
        promptTemplateMapper.insert(entity);
        return entity;
    }

    @Transactional
    public PromptVersionEntity createPromptVersion(CreatePromptVersionRequest request) {
        PromptTemplateEntity template = template(request.templateId());
        PromptVersionEntity entity = new PromptVersionEntity();
        entity.setTemplateId(template.getId());
        entity.setVersionLabel(request.versionLabel());
        entity.setTemplateText(request.templateText());
        entity.setVariablesJson(toVariablesJson(request.variables()));
        entity.setOutputFormat(request.outputFormat());
        entity.setStatus(blankToDefault(request.status(), "Draft"));
        entity.setAuthor(blankToDefault(request.author(), "system"));
        promptVersionMapper.insert(entity);
        template.setCurrentVersionId(entity.getId());
        template.setStatus(entity.getStatus());
        promptTemplateMapper.updateById(template);
        return entity;
    }

    @Transactional
    public EvalDatasetEntity createDataset(CreateDatasetRequest request) {
        EvalDatasetEntity entity = new EvalDatasetEntity();
        entity.setDatasetKey(request.datasetKey());
        entity.setName(request.name());
        entity.setScenario(request.scenario());
        entity.setDescription(request.description());
        evalDatasetMapper.insert(entity);
        return entity;
    }

    @Transactional
    public EvalCaseEntity createCase(CreateCaseRequest request) {
        EvalDatasetEntity dataset = dataset(request.datasetId());
        EvalCaseEntity entity = new EvalCaseEntity();
        entity.setDatasetId(dataset.getId());
        entity.setCaseKey(request.caseKey());
        entity.setTitle(request.title());
        entity.setInputText(request.inputText());
        entity.setExpectedText(request.expectedText());
        entity.setRiskLevel(blankToDefault(request.riskLevel(), "normal"));
        entity.setTagsJson(toJsonArray(request.tags() == null ? List.of() : request.tags()));
        entity.setEnabled(true);
        evalCaseMapper.insert(entity);
        return entity;
    }

    @Transactional
    public ScoreRuleEntity createRule(CreateRuleRequest request) {
        evalCase(request.caseId());
        String ruleType = request.ruleType().toUpperCase();
        if (("MUST_CONTAIN".equals(ruleType) || "FORBIDDEN_WORD".equals(ruleType))
                && (request.ruleValue() == null || request.ruleValue().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ruleType + " requires ruleValue");
        }
        if ("FORMAT".equals(ruleType) && (request.expectedFormat() == null || request.expectedFormat().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FORMAT requires expectedFormat");
        }
        ScoreRuleEntity entity = new ScoreRuleEntity();
        entity.setCaseId(request.caseId());
        entity.setRuleType(ruleType);
        entity.setRuleValue(request.ruleValue());
        entity.setExpectedFormat(request.expectedFormat());
        entity.setWeight(request.weight() == null ? BigDecimal.ONE : request.weight());
        entity.setDescription(request.description());
        entity.setEnabled(true);
        scoreRuleMapper.insert(entity);
        return entity;
    }

    @Transactional
    public EvalRunEntity runEval(Long promptVersionId, Long datasetId) {
        PromptVersionEntity version = version(promptVersionId);
        EvalDatasetEntity dataset = dataset(datasetId);
        List<EvalCaseEntity> cases = enabledCases(dataset.getId());
        LocalDateTime now = LocalDateTime.now();

        EvalRunEntity run = new EvalRunEntity();
        run.setRunKey("run-" + promptVersionId + "-" + datasetId + "-" + System.currentTimeMillis());
        run.setPromptVersionId(promptVersionId);
        run.setDatasetId(datasetId);
        run.setStatus("running");
        run.setTotalCases(cases.size());
        run.setPassedCases(0);
        run.setFailedCases(0);
        run.setRiskCaseCount(0);
        run.setPassRate(BigDecimal.ZERO);
        run.setAverageLatencyMs(0);
        run.setStartedAt(now);
        evalRunMapper.insert(run);

        int passed = 0;
        int failed = 0;
        int riskCases = 0;
        int latencySum = 0;
        for (EvalCaseEntity evalCase : cases) {
            List<ScoreRuleEntity> rules = rules(evalCase.getId());
            MockGeneration generation = mockOutputGenerator.generate(version, evalCase);
            RuleEvaluation evaluation = ruleEvaluator.evaluate(generation.output(), rules);
            boolean pass = "pass".equals(evaluation.status());
            passed += pass ? 1 : 0;
            failed += pass ? 0 : 1;
            riskCases += "normal".equalsIgnoreCase(evalCase.getRiskLevel()) ? 0 : 1;
            latencySum += generation.latencyMs();
            insertCaseResult(run, evalCase, generation, evaluation);
            insertTrace(run, version, evalCase, generation, evaluation);
        }

        run.setStatus("finished");
        run.setPassedCases(passed);
        run.setFailedCases(failed);
        run.setRiskCaseCount(riskCases);
        run.setAverageLatencyMs(cases.isEmpty() ? 0 : latencySum / cases.size());
        run.setPassRate(cases.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(passed * 100.0 / cases.size()).setScale(2, RoundingMode.HALF_UP));
        run.setFinishedAt(LocalDateTime.now());
        evalRunMapper.updateById(run);
        return run;
    }

    public EvalRunDto toEvalRunDto(EvalRunEntity run) {
        PromptVersionEntity version = version(run.getPromptVersionId());
        PromptTemplateEntity template = template(version.getTemplateId());
        EvalDatasetEntity dataset = dataset(run.getDatasetId());
        List<EvalCaseResultEntity> results = evalCaseResultMapper.selectList(new LambdaQueryWrapper<EvalCaseResultEntity>()
                .eq(EvalCaseResultEntity::getEvalRunId, run.getId())
                .orderByAsc(EvalCaseResultEntity::getId));
        List<CaseResultDto> caseDtos = results.stream().map(result -> toCaseResultDto(result, dataset)).toList();
        return new EvalRunDto(
                run.getId(),
                run.getRunKey(),
                dataset.getName(),
                template.getId(),
                version.getId(),
                dataset.getId(),
                version.getVersionLabel() + "-" + version.getStatus().toLowerCase(),
                run.getStatus(),
                run.getTotalCases(),
                run.getPassedCases(),
                run.getFailedCases(),
                run.getRiskCaseCount(),
                run.getPassRate(),
                run.getAverageLatencyMs(),
                caseDtos
        );
    }

    private void insertCaseResult(EvalRunEntity run, EvalCaseEntity evalCase, MockGeneration generation, RuleEvaluation evaluation) {
        EvalCaseResultEntity result = new EvalCaseResultEntity();
        result.setEvalRunId(run.getId());
        result.setEvalCaseId(evalCase.getId());
        result.setResultStatus(evaluation.status());
        result.setScore(BigDecimal.valueOf(evaluation.score()));
        result.setLatencyMs(generation.latencyMs());
        result.setFailureReasonsJson(toJsonArray(evaluation.failures()));
        result.setOutputSummary(generation.summary());
        result.setMockOutput(generation.output());
        result.setExplanation(evaluation.failures().isEmpty()
                ? "全部规则通过，mock 输出满足该用例的可解释检查。"
                : "规则失败: " + String.join("; ", evaluation.failures()));
        evalCaseResultMapper.insert(result);
    }

    private void insertTrace(EvalRunEntity run, PromptVersionEntity version, EvalCaseEntity evalCase, MockGeneration generation, RuleEvaluation evaluation) {
        GenerationTraceEntity trace = new GenerationTraceEntity();
        trace.setEvalRunId(run.getId());
        trace.setEvalCaseId(evalCase.getId());
        trace.setPromptVersionId(version.getId());
        trace.setInputSummary(summarize(evalCase.getInputText(), 180));
        trace.setOutputSummary(generation.summary());
        trace.setLatencyMs(generation.latencyMs());
        trace.setStatus(evaluation.status().equals("pass") ? "success" : "eval_failed");
        trace.setErrorReason(evaluation.failures().isEmpty() ? null : String.join("; ", evaluation.failures()));
        generationTraceMapper.insert(trace);
    }

    private CaseResultDto toCaseResultDto(EvalCaseResultEntity result, EvalDatasetEntity dataset) {
        EvalCaseEntity evalCase = evalCaseMapper.selectById(result.getEvalCaseId());
        List<ScoreRuleEntity> rules = rules(evalCase.getId());
        RuleEvaluation evaluation = ruleEvaluator.evaluate(result.getMockOutput(), rules);
        List<String> traceLines = new ArrayList<>();
        traceLines.add("case=" + evalCase.getCaseKey());
        traceLines.add("risk=" + evalCase.getRiskLevel());
        traceLines.add("rules=" + rules.size());
        traceLines.add("status=" + result.getResultStatus());
        if (result.getExplanation() != null) {
            traceLines.add(summarize(result.getExplanation(), 96));
        }
        return new CaseResultDto(
                result.getId(),
                evalCase.getId(),
                evalCase.getCaseKey(),
                evalCase.getTitle(),
                dataset.getName(),
                evalCase.getInputText(),
                evalCase.getExpectedText(),
                result.getResultStatus(),
                evalCase.getRiskLevel(),
                evaluationPolicy.requiresReview(result.getResultStatus(), evalCase.getRiskLevel()),
                result.getScore().setScale(0, RoundingMode.HALF_UP).intValue(),
                result.getLatencyMs(),
                result.getExplanation(),
                result.getOutputSummary(),
                result.getMockOutput(),
                traceLines,
                evaluation.checks()
        );
    }

    private PromptProfileDto toPromptProfile(PromptTemplateEntity template) {
        List<PromptVersionEntity> versions = versions(template.getId());
        PromptVersionEntity current = currentVersion(template);
        EvalDatasetEntity dataset = datasetForScenario(template.getScenario());
        List<String> scoringRules = dataset == null ? List.of() : enabledCases(dataset.getId()).stream()
                .flatMap(evalCase -> rules(evalCase.getId()).stream())
                .map(rule -> rule.getRuleType() + ": " + blankToDefault(rule.getRuleValue(), rule.getExpectedFormat()))
                .distinct()
                .limit(6)
                .toList();
        return new PromptProfileDto(
                template.getId(),
                template.getTemplateKey(),
                template.getName(),
                List.of(template.getScenario(), template.getOwnerTeam() == null ? "PromptOps" : template.getOwnerTeam(), template.getStatus()),
                versions.stream().map(PromptVersionEntity::getVersionLabel).toList(),
                current == null ? null : current.getId(),
                current == null ? "v0" : current.getVersionLabel(),
                template.getStatus(),
                template.getOwnerTeam(),
                template.getUpdatedAt() == null ? "" : template.getUpdatedAt().format(DISPLAY_TIME),
                current == null ? "" : current.getTemplateText(),
                current == null ? List.of() : variables(current.getVariablesJson()),
                scoringRules,
                current == null ? List.of() : List.of(current.getOutputFormat(), "仅 Mock 输出", "规则评分可解释"),
                dataset == null ? null : dataset.getId()
        );
    }

    private EvalRunEntity latestRunOrCreate(Long promptVersionId, Long datasetId) {
        return evalRunMapper.selectList(new LambdaQueryWrapper<EvalRunEntity>()
                        .eq(EvalRunEntity::getPromptVersionId, promptVersionId)
                        .eq(EvalRunEntity::getDatasetId, datasetId)
                        .orderByDesc(EvalRunEntity::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElseGet(() -> runEval(promptVersionId, datasetId));
    }

    private EvalRunEntity latestRun() {
        return evalRunMapper.selectList(new LambdaQueryWrapper<EvalRunEntity>()
                        .orderByDesc(EvalRunEntity::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<String> topFailureReasons(Long runId) {
        return evalCaseResultMapper.selectList(new LambdaQueryWrapper<EvalCaseResultEntity>()
                        .eq(EvalCaseResultEntity::getEvalRunId, runId))
                .stream()
                .filter(result -> !"pass".equals(result.getResultStatus()))
                .map(EvalCaseResultEntity::getExplanation)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(reason -> summarize(reason, 36), LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String deriveRegressionRisk(EvalRunEntity fromRun, EvalRunEntity toRun) {
        return evaluationPolicy.regressionRisk(
                fromRun.getPassRate(),
                toRun.getPassRate(),
                fromRun.getFailedCases(),
                toRun.getFailedCases(),
                fromRun.getAverageLatencyMs(),
                toRun.getAverageLatencyMs()
        );
    }

    private GenerationTraceDto toTraceDto(GenerationTraceEntity trace) {
        return new GenerationTraceDto(
                trace.getId(),
                trace.getEvalRunId(),
                trace.getEvalCaseId(),
                trace.getPromptVersionId(),
                trace.getInputSummary(),
                trace.getOutputSummary(),
                trace.getLatencyMs(),
                trace.getStatus(),
                trace.getErrorReason(),
                trace.getCreatedAt()
        );
    }

    private PromptTemplateEntity template(Long id) {
        PromptTemplateEntity entity = promptTemplateMapper.selectById(id);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt template not found: " + id);
        }
        return entity;
    }

    private PromptVersionEntity version(Long id) {
        PromptVersionEntity entity = promptVersionMapper.selectById(id);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt version not found: " + id);
        }
        return entity;
    }

    private EvalDatasetEntity dataset(Long id) {
        EvalDatasetEntity entity = evalDatasetMapper.selectById(id);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Eval dataset not found: " + id);
        }
        return entity;
    }

    private EvalCaseEntity evalCase(Long id) {
        EvalCaseEntity entity = evalCaseMapper.selectById(id);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Eval case not found: " + id);
        }
        return entity;
    }

    private int reviewQueueCount(Long runId) {
        return (int) resultsForRun(runId).stream()
                .filter(result -> {
                    EvalCaseEntity evalCase = evalCase(result.getEvalCaseId());
                    return evaluationPolicy.requiresReview(result.getResultStatus(), evalCase.getRiskLevel());
                })
                .count();
    }

    private int highImpactCaseCount(Long runId) {
        return (int) resultsForRun(runId).stream()
                .map(result -> evalCase(result.getEvalCaseId()))
                .filter(evalCase -> evaluationPolicy.isHighImpactRisk(evalCase.getRiskLevel()))
                .count();
    }

    private List<EvalCaseResultEntity> resultsForRun(Long runId) {
        return evalCaseResultMapper.selectList(new LambdaQueryWrapper<EvalCaseResultEntity>()
                .eq(EvalCaseResultEntity::getEvalRunId, runId));
    }

    private EvalDatasetEntity datasetForScenario(String scenario) {
        return evalDatasetMapper.selectList(new LambdaQueryWrapper<EvalDatasetEntity>()
                        .eq(EvalDatasetEntity::getScenario, scenario)
                        .orderByAsc(EvalDatasetEntity::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private PromptVersionEntity currentVersion(PromptTemplateEntity template) {
        if (template.getCurrentVersionId() != null) {
            PromptVersionEntity current = promptVersionMapper.selectById(template.getCurrentVersionId());
            if (current != null) {
                return current;
            }
        }
        return versions(template.getId()).stream().reduce((first, second) -> second).orElse(null);
    }

    private List<PromptVersionEntity> versions(Long templateId) {
        return promptVersionMapper.selectList(new LambdaQueryWrapper<PromptVersionEntity>()
                .eq(PromptVersionEntity::getTemplateId, templateId)
                .orderByAsc(PromptVersionEntity::getId));
    }

    private List<EvalCaseEntity> enabledCases(Long datasetId) {
        return evalCaseMapper.selectList(new LambdaQueryWrapper<EvalCaseEntity>()
                .eq(EvalCaseEntity::getDatasetId, datasetId)
                .eq(EvalCaseEntity::getEnabled, true)
                .orderByAsc(EvalCaseEntity::getId));
    }

    private List<ScoreRuleEntity> rules(Long caseId) {
        return scoreRuleMapper.selectList(new LambdaQueryWrapper<ScoreRuleEntity>()
                .eq(ScoreRuleEntity::getCaseId, caseId)
                .eq(ScoreRuleEntity::getEnabled, true)
                .orderByAsc(ScoreRuleEntity::getId));
    }

    private List<PromptVariableDto> variables(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) {
            return List.of();
        }
        String normalized = json.replace("[", "").replace("]", "").replace("{", "").replace("}", "");
        List<PromptVariableDto> variables = new ArrayList<>();
        for (String item : normalized.split("\\|")) {
            String[] parts = item.split("::");
            if (parts.length >= 3) {
                variables.add(new PromptVariableDto(parts[0], parts[1], Boolean.parseBoolean(parts[2])));
            }
        }
        return variables;
    }

    private String toVariablesJson(List<PromptVariableDto> variables) {
        return variables.stream()
                .map(variable -> variable.name() + "::" + variable.description() + "::" + variable.required())
                .collect(Collectors.joining("|", "[", "]"));
    }

    private String toJsonArray(List<String> values) {
        return values.stream()
                .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String summarize(String value, int max) {
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= max ? compact : compact.substring(0, max) + "...";
    }
}
