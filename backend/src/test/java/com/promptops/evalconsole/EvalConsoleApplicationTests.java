package com.promptops.evalconsole;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptTemplateRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptVersionRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptProfileDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptVariableDto;
import com.promptops.evalconsole.persistence.mapper.PromptVersionMapper;
import com.promptops.evalconsole.service.PromptOpsService;
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
