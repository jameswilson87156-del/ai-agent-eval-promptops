package com.promptops.evalconsole;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptTemplateRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptVersionRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptProfileDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptVariableDto;
import com.promptops.evalconsole.api.dto.AgentEvalRunRequest;
import com.promptops.evalconsole.persistence.mapper.PromptVersionMapper;
import com.promptops.evalconsole.service.PromptOpsService;
import com.promptops.evalconsole.service.AgentEvalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EvalConsoleApplicationTests {

    @Autowired
    private PromptOpsService promptOpsService;

    @Autowired
    private PromptVersionMapper promptVersionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentEvalService agentEvalService;

    @Test
    void contextLoadsAndSeedsDemoData() {
        assertThat(promptOpsService.listPromptProfiles()).isNotEmpty();
    }

    @Test
    void runCanBeRetrievedAndReproduced() throws Exception {
        var first = promptOpsService.runEval(1L, 1L);
        var second = promptOpsService.runEval(1L, 1L);
        var a = promptOpsService.getRun(first.getId());
        var b = promptOpsService.getRun(second.getId());
        assertThat(a.runKey()).isNotEqualTo(b.runKey());
        assertThat(a.cases()).extracting(c -> c.result()).containsExactlyElementsOf(b.cases().stream().map(c -> c.result()).toList());
        assertThat(a.cases()).anyMatch(c -> !c.result().equals("pass"));
        mockMvc.perform(get("/api/eval-runs/{id}", first.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.runKey").value(first.getRunKey()));
        mockMvc.perform(get("/api/eval-runs/{id}", 999999999L)).andExpect(status().isNotFound());
    }

    @Test
    void agentDatasetHasThirtyEightUniqueAssertableCasesAcrossCoverageCategories() {
        var dataset=agentEvalService.getDataset();
        var cases=(List<?>)dataset.get("cases");
        assertThat(cases).hasSize(38);
        var ids=cases.stream().map(value->((java.util.Map<?,?>)value).get("case_id")).toList();
        assertThat(ids).doesNotHaveDuplicates();
        var categoryCounts=cases.stream().map(value->String.valueOf(((java.util.Map<?,?>)value).get("category")))
                .collect(java.util.stream.Collectors.groupingBy(value->value,java.util.stream.Collectors.counting()));
        assertThat(categoryCounts).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                "normal_output",9L,"input_boundary",6L,"tool_selection_arguments",8L,
                "tool_failures",4L,"security",7L,"completion_trace_rules",4L));
    }

    @Test
    void agentRunPersistsImmutableReportWithSeparateExecutorAndBehaviorStatuses() throws Exception {
        var first=agentEvalService.execute();
        var second=agentEvalService.execute();
        String firstId=(String)first.get("run_id");
        String secondId=(String)second.get("run_id");
        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(first.get("evaluator_mode")).isEqualTo("mock");
        assertThat(first.get("run_mode")).isEqualTo("mock");
        assertThat(first.get("provider_type")).isEqualTo("mock");
        assertThat(first.get("model_name")).isEqualTo("deterministic-mock-agent");
        assertThat(first.get("config_fingerprint").toString()).hasSize(64);
        assertThat(first.get("runtime_config_fingerprint").toString()).hasSize(64);
        assertThat(first.get("version_labels_semantics")).isEqualTo("metadata_only");
        assertThat(first).containsKey("traces");
        assertThat(((java.util.Map<?,?>)first.get("summary")).get("total_cases")).isEqualTo(38);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(agentEvalService.getRun(firstId))))
                .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(first)));
        assertThat(agentEvalService.listRuns()).extracting(run->run.get("run_id")).contains(firstId,secondId);
        var caseResults=(List<java.util.Map<String,Object>>)first.get("cases");
        assertThat(caseResults).allSatisfy(result->assertThat(result.get("execution_status")).isEqualTo("success"));
        assertThat(caseResults).anySatisfy(result->assertThat(result.get("case_status")).isEqualTo("failed"));
        var weather=caseResults.stream().filter(result->"agent-002".equals(result.get("case_id"))).findFirst().orElseThrow();
        assertThat(weather.get("case_status")).isEqualTo("passed");
        assertThat(weather.get("run_id")).isEqualTo(firstId);
        assertThat(weather).containsKey("assertion_results");
        var caseById=caseResults.stream().collect(java.util.stream.Collectors.toMap(result->result.get("case_id"),result->result));
        assertThat(caseById.get("agent-027").get("case_status")).isEqualTo("passed");
        assertThat(caseById.get("agent-031").get("case_status")).isEqualTo("passed");
        assertThat(caseById.get("agent-032").get("case_status")).isEqualTo("passed");
        assertThat(caseById.get("agent-036").get("case_status")).isEqualTo("passed");
        assertThat(caseById.get("agent-038").get("case_status")).isEqualTo("passed");
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(agentEvalService.getRun(firstId))))
                .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(first)));
    }

    @Test
    void exposesAgentDatasetRunHistoryAndReportApis() throws Exception {
        mockMvc.perform(get("/api/agent-eval/dataset")).andExpect(status().isOk()).andExpect(jsonPath("$.cases.length()").value(38));
        String body=mockMvc.perform(post("/api/agent-eval/runs"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evaluator_mode").value("mock"))
                .andReturn().getResponse().getContentAsString();
        String id=objectMapper.readTree(body).get("run_id").asText();
        mockMvc.perform(get("/api/agent-eval/runs/{id}/report",id)).andExpect(status().isOk()).andExpect(jsonPath("$.run_id").value(id));
        mockMvc.perform(get("/api/agent-eval/runs")).andExpect(status().isOk()).andExpect(jsonPath("$[0].run_id").exists());
        mockMvc.perform(get("/api/agent-eval/provider/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.default_provider_type").value("mock"))
                .andExpect(jsonPath("$.model_credentials_returned").value(false))
                .andExpect(jsonPath("$.providers[?(@.provider_type == 'openai_compatible')].credentials_configured").exists());
    }

    @Test
    void comparesPersistedRunsAndDoesNotCallMockMetadataAVersionRegression() {
        var baseline = agentEvalService.execute(new AgentEvalRunRequest("mock", "mock-prompt-a", "mock-agent-v1"));
        var candidate = agentEvalService.execute(new AgentEvalRunRequest("mock", "mock-prompt-b", "mock-agent-v1"));
        var comparison = agentEvalService.compareRuns((String) baseline.get("run_id"), (String) candidate.get("run_id"));

        assertThat(comparison.get("comparison_semantics")).isEqualTo("mock_metadata_only_comparison");
        assertThat(comparison.get("is_version_regression")).isEqualTo(false);
        assertThat(comparison.get("configuration_changed")).isEqualTo(false);
        assertThat(comparison.get("configuration_labels_changed")).isEqualTo(true);
        assertThat(comparison.get("run_fingerprint_changed")).isEqualTo(true);
        var summary = (java.util.Map<?, ?>) comparison.get("summary");
        assertThat(summary.get("persistent_passes")).isEqualTo(36);
        assertThat(summary.get("persistent_failures")).isEqualTo(2);
        assertThat((List<?>) comparison.get("case_changes")).hasSize(38);
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

    @Test
    void storesPromptVariablesAsStandardJsonArray() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        var template = promptOpsService.createPromptTemplate(new CreatePromptTemplateRequest(
                "json-vars-" + suffix, "JSON 变量测试", "变量序列化",
                "验证 variables_json 使用真实 JSON 数组存储。", "Backend", "Draft"));
        var variables = List.of(
                new PromptVariableDto("customerType", "客户类型", true),
                new PromptVariableDto("riskLevel", "风险等级", false)
        );

        var version = promptOpsService.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v-json", "Return JSON with customerType and riskLevel.",
                variables, "JSON", "Candidate", "codex-test"));

        String stored = promptVersionMapper.selectById(version.getId()).getVariablesJson();
        var json = objectMapper.readTree(stored);
        assertThat(json.isArray()).isTrue();
        assertThat(json.size()).isEqualTo(2);
        assertThat(json.get(0).get("name").asText()).isEqualTo("customerType");
        assertThat(json.get(0).get("description").asText()).isEqualTo("客户类型");
        assertThat(json.get(0).get("required").asBoolean()).isTrue();
        assertThat(stored).doesNotContain("::");
        assertThat(profile(template.getId()).variables()).containsExactlyElementsOf(variables);
    }

    @Test
    void readsLegacyPipeVariablesForExistingData() {
        String suffix = Long.toString(System.nanoTime());
        var template = promptOpsService.createPromptTemplate(new CreatePromptTemplateRequest(
                "legacy-vars-" + suffix, "旧变量格式测试", "变量兼容",
                "验证旧管道格式仍能被读取。", "Backend", "Draft"));
        var version = promptOpsService.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v-legacy", "Return text.",
                List.of(new PromptVariableDto("placeholder", "占位变量", true)),
                "TEXT", "Candidate", "codex-test"));
        version.setVariablesJson("[legacyName::历史字段::true|riskLevel::风险等级::false]");
        promptVersionMapper.updateById(version);

        assertThat(profile(template.getId()).variables()).containsExactly(
                new PromptVariableDto("legacyName", "历史字段", true),
                new PromptVariableDto("riskLevel", "风险等级", false)
        );
    }

    @Test
    void returnsUnifiedErrorForMissingPromptTemplate() throws Exception {
        mockMvc.perform(get("/api/prompts/{promptId}/eval-runs/current", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returnsFieldErrorsForInvalidPromptTemplateRequest() throws Exception {
        mockMvc.perform(post("/api/prompt-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("scenario")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("templateKey")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("must not be blank")));
    }

    private PromptProfileDto profile(Long templateId) {
        return promptOpsService.listPromptProfiles().stream()
                .filter(profile -> profile.id().equals(templateId))
                .findFirst()
                .orElseThrow();
    }
}
