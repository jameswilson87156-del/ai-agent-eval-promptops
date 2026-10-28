package com.promptops.evalconsole.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.evalconsole.api.dto.AgentEvalRunRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentEvalService {
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final List<AgentProvider> providers;
    private final AgentRunComparator comparator;
    private final ProductionMetrics metrics;
    private JsonNode dataset;

    public AgentEvalService(ObjectMapper mapper, JdbcTemplate jdbc, List<AgentProvider> providers,
                            AgentRunComparator comparator, ProductionMetrics metrics) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.providers = providers;
        this.comparator = comparator;
        this.metrics = metrics;
    }

    @PostConstruct
    public void initialize() throws Exception {
        jdbc.execute("CREATE TABLE IF NOT EXISTS promptops_agent_eval_run (run_id VARCHAR(36) PRIMARY KEY, state VARCHAR(24) NOT NULL, evaluator_mode VARCHAR(16) NOT NULL, dataset_version VARCHAR(32) NOT NULL, report_json TEXT, created_at VARCHAR(40) NOT NULL)");
        try (var input = new ClassPathResource("agent-eval-dataset-v1.json").getInputStream()) { dataset = mapper.readTree(input); }
        validateDataset(dataset);
    }

    public Map<String,Object> getDataset() {
        return Map.of("dataset_version", dataset.path("dataset_version").asText(),
                "source", dataset.path("source").asText(), "evaluator_mode", "mock",
                "cases", mapper.convertValue(dataset.path("cases"), Object.class));
    }

    public Map<String, Object> getProviderStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("default_provider_type", "mock");
        response.put("providers", providers.stream().map(AgentProvider::publicStatus).toList());
        response.put("real_provider_requires_explicit_selection", true);
        response.put("model_credentials_returned", false);
        return response;
    }

    public List<Map<String,Object>> listRuns() {
        return jdbc.query("SELECT report_json FROM promptops_agent_eval_run WHERE state IN ('COMPLETED','FAILED') ORDER BY created_at DESC", (rs, n) -> parse(rs.getString(1)));
    }

    public Map<String,Object> getRun(String id) {
        List<String> found = jdbc.query("SELECT report_json FROM promptops_agent_eval_run WHERE run_id=?", (rs,n)->rs.getString(1), id);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + id);
        return parse(found.get(0));
    }

    public Map<String, Object> compareRuns(String baselineRunId, String candidateRunId) {
        if (baselineRunId == null || baselineRunId.isBlank() || candidateRunId == null || candidateRunId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both baseline and candidate run IDs are required.");
        }
        if (baselineRunId.equals(candidateRunId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose two different saved runs to compare.");
        }
        return comparator.compare(getRun(baselineRunId), getRun(candidateRunId));
    }

    @Transactional
    public Map<String,Object> execute() { return execute(null); }

    @Transactional
    public Map<String,Object> execute(AgentEvalRunRequest request) {
        AgentProvider provider = resolveProvider(request == null ? null : request.providerType());
        if (!provider.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Selected provider is not configured; check its model and credential environment variables.");
        }
        String promptVersion = versionLabel(request == null ? null : request.promptVersion(), provider.defaultPromptVersion(), "prompt_version");
        String agentConfigVersion = versionLabel(request == null ? null : request.agentConfigVersion(), provider.defaultAgentConfigVersion(), "agent_config_version");
        String id = UUID.randomUUID().toString();
        String now = LocalDateTime.now().toString();
        long runStarted = System.nanoTime();
        String datasetVersion = dataset.path("dataset_version").asText();
        String evaluatorMode = "mock".equals(provider.providerType()) ? "mock" : "provider";
        String runMode = "mock".equals(provider.providerType()) ? "mock" : "real_model";
        String fingerprint = fingerprint(provider, datasetVersion, promptVersion, agentConfigVersion);
        String runtimeFingerprint = runtimeFingerprint(provider);
        jdbc.update("INSERT INTO promptops_agent_eval_run(run_id,state,evaluator_mode,dataset_version,report_json,created_at) VALUES(?,?,?,?,?,?)",
                id, "PENDING", evaluatorMode, datasetVersion, "{}", now);
        jdbc.update("UPDATE promptops_agent_eval_run SET state=? WHERE run_id=?", "RUNNING", id);
        List<Map<String,Object>> cases = new ArrayList<>();
        List<Map<String,Object>> errors = new ArrayList<>();
        int passed=0, failed=0, errorCount=0, skipped=0, evaluatorErrors=0, providerErrors=0;
        boolean stopProviderRun = false;
        for (JsonNode definition : dataset.path("cases")) {
            if (!definition.path("enabled").asBoolean(true)) { skipped++; continue; }
            if (stopProviderRun) {
                cases.add(skippedCase(definition, id, "Provider request stopped the remaining batch after a provider error.",
                        provider, promptVersion, agentConfigVersion, fingerprint));
                skipped++;
                continue;
            }
            List<Map<String,Object>> trace = new ArrayList<>();
            String caseStartedAt = LocalDateTime.now().toString();
            long caseStartedNanos = System.nanoTime();
            try {
                Map<String,Object> result = executeCase(definition, provider, trace);
                result.put("run_id", id);
                result.put("run_mode", runMode);
                result.put("provider_type", provider.providerType());
                result.put("model_name", provider.modelName());
                result.put("prompt_version", promptVersion);
                result.put("agent_config_version", agentConfigVersion);
                result.put("config_fingerprint", fingerprint);
                cases.add(result);
                switch (String.valueOf(result.get("case_status"))) { case "passed" -> passed++; case "failed" -> failed++; case "error" -> errorCount++; default -> skipped++; }
                if ("error".equals(result.get("execution_status"))) evaluatorErrors++;
            } catch (ProviderCallException ex) {
                errorCount++;
                providerErrors++;
                stopProviderRun = true;
                cases.add(errorCase(definition, id, ex.getMessage(), ex.code(), trace,
                        provider, promptVersion, agentConfigVersion, fingerprint, caseStartedAt, caseStartedNanos));
                errors.add(Map.of("case_id", definition.path("case_id").asText(), "type", "provider_error",
                        "code", ex.code(), "message", ex.getMessage()));
            } catch (Exception ex) {
                errorCount++;
                evaluatorErrors++;
                cases.add(errorCase(definition, id, safeMessage(ex), "evaluator_error", trace,
                        provider, promptVersion, agentConfigVersion, fingerprint, caseStartedAt, caseStartedNanos));
                errors.add(Map.of("case_id", definition.path("case_id").asText(), "type", "evaluator_error",
                        "code", ex.getClass().getSimpleName(), "message", safeMessage(ex)));
            }
        }
        int executed = passed + failed + errorCount;
        Map<String,Object> summary = new LinkedHashMap<>();
        summary.put("total_cases", dataset.path("cases").size()); summary.put("executed_cases",executed);
        summary.put("passed",passed); summary.put("failed",failed); summary.put("errors",errorCount); summary.put("skipped",skipped);
        summary.put("provider_errors", providerErrors); summary.put("evaluator_errors",evaluatorErrors);
        summary.put("pass_rate",executed==0?0.0:Math.round(passed*10000.0/executed)/100.0);
        List<Map<String,Object>> traces=cases.stream().flatMap(result->((List<Map<String,Object>>)result.get("execution_steps")).stream().map(event->{Map<String,Object> linked=new LinkedHashMap<>(event);linked.put("case_id",result.get("case_id"));return linked;})).toList();
        Map<String,Object> report = new LinkedHashMap<>();
        String completedAt = LocalDateTime.now().toString();
        report.put("report_schema_version","1.1"); report.put("run_id",id); report.put("state","COMPLETED");
        report.put("evaluator_mode",evaluatorMode); report.put("run_mode",runMode);
        report.put("provider_type",provider.providerType()); report.put("model_name",provider.modelName());
        report.put("prompt_version",promptVersion); report.put("agent_config_version",agentConfigVersion);
        report.put("config_fingerprint",fingerprint); report.put("runtime_config_fingerprint",runtimeFingerprint);
        report.put("version_labels_semantics","metadata_only"); report.put("provider_config",provider.fingerprintSettings());
        report.put("tool_runtime","deterministic_local_stub");
        report.put("dataset_version",datasetVersion); report.put("started_at",now); report.put("completed_at",completedAt);
        long durationMillis = Math.max(0,(System.nanoTime()-runStarted)/1_000_000);
        report.put("duration_ms",durationMillis);
        report.put("summary",summary); report.put("cases",cases); report.put("traces",traces); report.put("errors",errors);
        String qualityGate = failed==0 && errorCount==0 && skipped==0?"passed":"failed";
        report.put("quality_gate",qualityGate);
        report.put("report_generated_at",LocalDateTime.now().toString());
        try { jdbc.update("UPDATE promptops_agent_eval_run SET state=?,report_json=? WHERE run_id=?", "COMPLETED", mapper.writeValueAsString(report), id); }
        catch (Exception ex) { jdbc.update("UPDATE promptops_agent_eval_run SET state=? WHERE run_id=?", "FAILED", id); throw new IllegalStateException("Could not persist evaluation report",ex); }
        metrics.recordAgentEvaluation(runMode, qualityGate, passed, failed, errorCount, skipped,
                providerErrors, evaluatorErrors, durationMillis);
        return report;
    }

    private Map<String,Object> executeCase(JsonNode definition, AgentProvider provider,
                                           List<Map<String,Object>> trace) throws Exception {
        LocalDateTime started=LocalDateTime.now(); long begin=System.nanoTime();
        JsonNode input=definition.path("input");
        Map<String,Object> actual=provider.execute(input,trace);
        actual.putIfAbsent("provider_type", provider.providerType());
        actual.putIfAbsent("model_name", provider.modelName());
        addTraceEvent(trace,"assertions_evaluated","final output",Map.of("note","assertion results are evaluated against the actual output"),"pending",null);
        addTraceEvent(trace,"run_completed","assertion results",Map.of("status","pending"),"pending",null);
        Map<String,Object> assertionContext=new LinkedHashMap<>(actual);
        assertionContext.put("trace",trace);
        List<Map<String,Object>> assertions=new ArrayList<>();
        long assertionStarted=System.nanoTime();
        for (JsonNode assertion:definition.path("assertions")) assertions.add(checkAssertion(assertionContext,assertion));
        boolean pass=assertions.stream().allMatch(a->Boolean.TRUE.equals(a.get("passed")));
        Map<String,Object> assertionEvent=trace.get(trace.size()-2);
        finishTraceEvent(assertionEvent,assertionStarted,
                Map.of("total",assertions.size(),"passed",assertions.stream().filter(a->Boolean.TRUE.equals(a.get("passed"))).count(),"failed",assertions.stream().filter(a->!Boolean.TRUE.equals(a.get("passed"))).count()),
                pass?"success":"failed",null);
        Map<String,Object> completionEvent=trace.get(trace.size()-1);
        finishTraceEvent(completionEvent,System.nanoTime(),Map.of("case_status",pass?"passed":"failed"),"success",null);
        long duration=(System.nanoTime()-begin)/1_000_000;
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("run_id",null); result.put("case_id",definition.path("case_id").asText()); result.put("title",definition.path("title").asText());
        result.put("category",definition.path("category").asText()); result.put("risk_level",definition.path("risk_level").asText()); result.put("tags",mapper.convertValue(definition.path("tags"),Object.class));
        result.put("input",input); result.put("expected_behavior",definition.path("expected_behavior").asText()); result.put("assertions",assertions); result.put("assertion_results",assertions);
        result.put("actual_output",actual); result.put("execution_status","success"); result.put("case_status",pass?"passed":"failed");
        result.put("failure_reason",pass?null:assertions.stream().filter(a->!Boolean.TRUE.equals(a.get("passed"))).map(a->a.get("description")+" (expected "+a.get("expected")+", actual "+a.get("actual")+")").toList());
        result.put("started_at",started.toString()); result.put("duration_ms",duration); result.put("execution_steps",trace);
        return result;
    }

    private void addTraceEvent(List<Map<String,Object>> events,String type,String input,Map<String,Object> output,String status,String error) {
        Map<String,Object> event=new LinkedHashMap<>(); event.put("event_id",UUID.randomUUID().toString()); event.put("sequence",events.size()); event.put("event_type",type);
        event.put("timestamp",LocalDateTime.now().toString()); event.put("duration_ms",0.0); event.put("input_summary",input); event.put("output_summary",output); event.put("status",status); event.put("error",error); events.add(event);
    }

    private void finishTraceEvent(Map<String,Object> event,long startedNanos,Map<String,Object> output,String status,String error) {
        event.put("timestamp",LocalDateTime.now().toString()); event.put("duration_ms",Math.round((System.nanoTime()-startedNanos)/1_000_000.0*1000.0)/1000.0);
        event.put("output_summary",output); event.put("status",status); event.put("error",error);
    }

    private Map<String,Object> checkAssertion(Map<String,Object> actual,JsonNode assertion) throws Exception {
        String path=assertion.path("path").asText(); String op=assertion.path("op").asText("eq"); JsonNode value=path(actual,path);
        JsonNode expected=assertion.path("value"); boolean passed=switch(op) {
            case "eq" -> value!=null && (value.equals(expected) || (value.isArray() && expected.isNumber() && value.size()==expected.asInt()));
            case "contains" -> value!=null && value.isTextual() && value.asText().contains(expected.asText());
            case "not_contains" -> value!=null && value.isTextual() && !value.asText().contains(expected.asText());
            case "one_of" -> expected.isArray() && java.util.stream.StreamSupport.stream(expected.spliterator(),false).anyMatch(value::equals);
            case "count_eq" -> value!=null && value.isArray() ? value.size()==expected.asInt() : expected.asInt()==0 && (value==null||value.isNull());
            case "exists" -> value!=null && !value.isNull() && !value.isMissingNode();
            case "valid_json" -> { boolean valid; try { JsonNode parsed=mapper.readTree(actual.get("final_output").toString()); valid=parsed!=null; } catch(Exception ex) { valid=false; } yield valid==expected.asBoolean(); }
            case "ordered_events" -> {
                if (!value.isArray() || !expected.isArray()) yield false;
                int matched = 0;
                for (JsonNode event : value) {
                    if (matched < expected.size() && event.path("event_type").asText().equals(expected.path(matched).asText())) matched++;
                }
                yield matched == expected.size();
            }
            default -> false;
        };
        Map<String,Object> out=new LinkedHashMap<>(); out.put("path",path);out.put("operator",op);out.put("expected",expected);out.put("actual",value);out.put("passed",passed);out.put("description",assertion.path("description").asText(""));return out;
    }

    private JsonNode path(Map<String,Object> actual,String expression) {
        JsonNode current=mapper.valueToTree(actual);
        for(String part:expression.split("\\.")) { if(current==null)return com.fasterxml.jackson.databind.node.NullNode.getInstance(); if(part.matches("\\d+"))current=current.path(Integer.parseInt(part));else current=current.path(part); }
        return current;
    }
    private void validateDataset(JsonNode value) {
        if(!value.path("cases").isArray()||value.path("cases").size()!=26) throw new IllegalStateException("Agent evaluation dataset must contain 26 cases");
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(JsonNode c:value.path("cases")) { if(!ids.add(c.path("case_id").asText())||c.path("assertions").isEmpty()||!c.hasNonNull("expected_behavior")) throw new IllegalStateException("Invalid or duplicate case in dataset: "+c.path("case_id").asText()); }
    }
    private Map<String,Object> errorCase(JsonNode definition, String runId, String message, String errorCode,
                                         List<Map<String,Object>> trace, AgentProvider provider,
                                         String promptVersion, String agentConfigVersion, String fingerprint,
                                         String caseStartedAt, long caseStartedNanos) {
        Map<String,Object> result=new LinkedHashMap<>(); result.put("run_id",runId); result.put("case_id",definition.path("case_id").asText());
        result.put("title",definition.path("title").asText()); result.put("category",definition.path("category").asText()); result.put("risk_level",definition.path("risk_level").asText());
        result.put("tags",mapper.convertValue(definition.path("tags"),Object.class)); result.put("input",definition.path("input")); result.put("expected_behavior",definition.path("expected_behavior").asText());
        result.put("assertions",List.of()); result.put("assertion_results",List.of()); result.put("actual_output",Map.of());
        result.put("execution_status","error"); result.put("case_status","error"); result.put("error_code",errorCode);
        result.put("provider_error_code",errorCode.startsWith("provider_")?errorCode:null);
        result.put("failure_reason",List.of(message)); result.put("started_at",caseStartedAt);
        result.put("duration_ms",Math.max(0,(System.nanoTime()-caseStartedNanos)/1_000_000));
        result.put("execution_steps",trace); result.put("run_mode","mock".equals(provider.providerType())?"mock":"real_model");
        result.put("provider_type",provider.providerType()); result.put("model_name",provider.modelName());
        result.put("prompt_version",promptVersion); result.put("agent_config_version",agentConfigVersion); result.put("config_fingerprint",fingerprint);
        return result;
    }

    private Map<String,Object> skippedCase(JsonNode definition, String runId, String reason, AgentProvider provider,
                                           String promptVersion, String agentConfigVersion, String fingerprint) {
        Map<String,Object> event=new LinkedHashMap<>();
        event.put("event_id",UUID.randomUUID().toString()); event.put("sequence",0); event.put("event_type","provider_batch_stopped");
        event.put("timestamp",LocalDateTime.now().toString()); event.put("duration_ms",0); event.put("input_summary","No provider request was made for this case.");
        event.put("output_summary",Map.of("reason",reason)); event.put("status","skipped"); event.put("error",reason);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("run_id",runId); result.put("case_id",definition.path("case_id").asText()); result.put("title",definition.path("title").asText());
        result.put("category",definition.path("category").asText()); result.put("risk_level",definition.path("risk_level").asText());
        result.put("tags",mapper.convertValue(definition.path("tags"),Object.class)); result.put("input",definition.path("input"));
        result.put("expected_behavior",definition.path("expected_behavior").asText()); result.put("assertions",List.of()); result.put("assertion_results",List.of());
        result.put("actual_output",Map.of()); result.put("execution_status","skipped"); result.put("case_status","skipped");
        result.put("failure_reason",List.of(reason)); result.put("started_at",LocalDateTime.now().toString()); result.put("duration_ms",0);
        result.put("execution_steps",List.of(event)); result.put("run_mode","mock".equals(provider.providerType())?"mock":"real_model");
        result.put("provider_type",provider.providerType()); result.put("model_name",provider.modelName()); result.put("prompt_version",promptVersion);
        result.put("agent_config_version",agentConfigVersion); result.put("config_fingerprint",fingerprint);
        return result;
    }

    private AgentProvider resolveProvider(String providerType) {
        String requested=providerType==null||providerType.isBlank()?"mock":providerType.trim().toLowerCase();
        return providers.stream().filter(provider->provider.providerType().equals(requested)).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported provider_type. Use mock or openai_compatible."));
    }

    private String versionLabel(String value,String fallback,String field) {
        String result=value==null||value.isBlank()?fallback:value.trim();
        if(result.length()>80||result.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid "+field+"; maximum length is 80 characters.");
        return result;
    }

    private String fingerprint(AgentProvider provider,String datasetVersion,String promptVersion,String agentConfigVersion) {
        try {
            Map<String,Object> identity=new LinkedHashMap<>();
            identity.put("provider_type",provider.providerType()); identity.put("model_name",provider.modelName());
            identity.put("dataset_version",datasetVersion); identity.put("prompt_version",promptVersion);
            identity.put("agent_config_version",agentConfigVersion); identity.put("provider_settings",provider.fingerprintSettings());
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(identity).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch(Exception ex) { throw new IllegalStateException("Could not fingerprint provider configuration.",ex); }
    }
    private String runtimeFingerprint(AgentProvider provider) {
        try {
            Map<String,Object> identity=new LinkedHashMap<>();
            identity.put("provider_type",provider.providerType()); identity.put("model_name",provider.modelName());
            Map<String,Object> settings=new LinkedHashMap<>(provider.fingerprintSettings());
            settings.remove("provider_config_version");
            identity.put("provider_runtime_settings",settings);
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(identity).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch(Exception ex) { throw new IllegalStateException("Could not fingerprint runtime provider settings.",ex); }
    }
    private Map<String,Object> parse(String json) { try { return mapper.readValue(json,mapper.getTypeFactory().constructMapType(LinkedHashMap.class,String.class,Object.class)); } catch(Exception ex) { throw new IllegalStateException("Stored evaluation report is invalid JSON",ex); } }
    private String safeMessage(Exception ex) {
        String message=ex.getMessage();
        if(message==null||message.isBlank())return ex.getClass().getSimpleName()+" during evaluation.";
        return truncate(message.replaceAll("(?i)(api[_-]?key|authorization|token|secret|password)\\s*[:=]\\s*[^\\s,;]+","$1=[REDACTED]")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+","Bearer [REDACTED]")
                .replaceAll("\\bsk-[A-Za-z0-9_-]{16,}\\b","[REDACTED]"));
    }
    private String truncate(String value) { return value.length()>240?value.substring(0,240)+"…":value; }
}
