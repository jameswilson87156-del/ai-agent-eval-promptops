package com.promptops.evalconsole.service;

import com.promptops.evalconsole.persistence.entity.EvalCaseEntity;
import com.promptops.evalconsole.persistence.entity.PromptVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class MockOutputGenerator {

    public MockGeneration generate(PromptVersionEntity version, EvalCaseEntity evalCase) {
        int latency = 420 + Math.abs((version.getVersionLabel() + evalCase.getCaseKey()).hashCode() % 520);
        boolean improved = version.getVersionLabel().matches(".*[23]$|.*[4-9]$");
        boolean riskyCase = !"normal".equalsIgnoreCase(evalCase.getRiskLevel());

        String output = switch (version.getOutputFormat().toUpperCase()) {
            case "MARKDOWN" -> markdownOutput(evalCase, improved, riskyCase);
            case "TEXT" -> textOutput(evalCase, improved, riskyCase);
            default -> jsonOutput(evalCase, improved, riskyCase);
        };
        return new MockGeneration(output, summarize(output), latency);
    }

    private String jsonOutput(EvalCaseEntity evalCase, boolean improved, boolean riskyCase) {
        String risk = improved || riskyCase ? "\"risk_notice\":\"风险提示: 需要人工复核高影响场景\"," : "";
        String category = improved ? "target_route" : "general_question";
        String status = improved ? "pass_candidate" : "needs_review";
        return "{"
                + "\"case_key\":\"" + escape(evalCase.getCaseKey()) + "\","
                + "\"category\":\"" + category + "\","
                + risk
                + "\"status\":\"" + status + "\","
                + "\"summary\":\"基于规则 mock 输出: " + escape(evalCase.getTitle()) + "\""
                + "}";
    }

    private String markdownOutput(EvalCaseEntity evalCase, boolean improved, boolean riskyCase) {
        String risk = improved || riskyCase ? "\n- 风险提示: 需要人工复核高影响场景" : "";
        return "## Eval Output\n- Case: " + evalCase.getCaseKey()
                + "\n- Decision: " + (improved ? "pass_candidate" : "needs_review")
                + risk
                + "\n- Summary: 基于规则 mock 输出: " + evalCase.getTitle();
    }

    private String textOutput(EvalCaseEntity evalCase, boolean improved, boolean riskyCase) {
        String risk = improved || riskyCase ? " 风险提示: 需要人工复核高影响场景。" : "";
        return "Case " + evalCase.getCaseKey() + " -> " + (improved ? "pass_candidate" : "needs_review")
                + "。基于规则 mock 输出: " + evalCase.getTitle() + "。" + risk;
    }

    private String summarize(String output) {
        if (output == null) {
            return "";
        }
        String compact = output.replaceAll("\\s+", " ").trim();
        return compact.length() > 180 ? compact.substring(0, 180) + "..." : compact;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
