package com.promptops.evalconsole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateCaseRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateDatasetRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptTemplateRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreatePromptVersionRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.CreateRuleRequest;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptVariableDto;
import com.promptops.evalconsole.api.dto.ConsoleDtos.PromptProfileDto;
import com.promptops.evalconsole.persistence.entity.EvalCaseEntity;
import com.promptops.evalconsole.persistence.entity.EvalDatasetEntity;
import com.promptops.evalconsole.persistence.entity.PromptTemplateEntity;
import com.promptops.evalconsole.persistence.entity.PromptVersionEntity;
import com.promptops.evalconsole.persistence.mapper.PromptTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final PromptTemplateMapper promptTemplateMapper;
    private final PromptOpsService service;
    private final boolean enabled;

    public DemoDataInitializer(
            PromptTemplateMapper promptTemplateMapper,
            PromptOpsService service,
            @Value("${promptops.demo-data.enabled:true}") boolean enabled
    ) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.service = service;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Demo data initialization skipped: disabled by configuration");
            return;
        }
        if (promptTemplateMapper.selectCount(new LambdaQueryWrapper<PromptTemplateEntity>()
                .eq(PromptTemplateEntity::getTemplateKey, "ticket-router")) > 0) {
            log.info("Demo data initialization skipped: existing prompt templates found");
            return;
        }
        seedTicketRouter();
        log.info("Seeded prompt template: ticket-router");
        seedLogAnalyzer();
        log.info("Seeded prompt template: log-analyzer");
        seedJdReviewer();
        log.info("Seeded prompt template: jd-screen");
        List<PromptProfileDto> profiles = service.listPromptProfiles();
        profiles.forEach(profile -> service.latestRunForTemplate(profile.id()));
        log.info("Demo data initialization complete: {} profiles seeded", profiles.size());
    }

    private void seedTicketRouter() {
        PromptTemplateEntity template = service.createPromptTemplate(new CreatePromptTemplateRequest(
                "ticket-router", "客服工单分流 Agent", "工单分类",
                "按服务类型、紧急度和 SLA 风险分流客服工单。", "Ops AI", "Candidate"));
        PromptVersionEntity v1 = service.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v1", "Classify the ticket into category and urgency. Return JSON.",
                List.of(new PromptVariableDto("ticket_text", "用户原始工单内容", true), new PromptVariableDto("customer_tier", "客户等级", true)),
                "JSON", "Draft", "ops-ai"));
        PromptVersionEntity v2 = service.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v2", "Classify category, urgency, owner team, and add risk_notice for SLA or complaint scenarios. Return JSON.",
                List.of(new PromptVariableDto("ticket_text", "用户原始工单内容", true), new PromptVariableDto("customer_tier", "客户等级", true), new PromptVariableDto("recent_incidents", "近期事故摘要", false)),
                "JSON", "Candidate", "ops-ai"));
        EvalDatasetEntity dataset = service.createDataset(new CreateDatasetRequest(
                "ticket-regression", "客服分流回归集", "工单分类", "覆盖投诉、宕机与低风险反馈的本地演示用例。"));
        EvalCaseEntity c1 = service.createCase(new CreateCaseRequest(dataset.getId(), "tc-104", "退款 + 威胁投诉",
                "企业客户要求退款，并表示 30 分钟内无人处理将公开投诉。",
                "category=billing_escalation, risk_notice required", "high", List.of("SLA", "billing")));
        addCommonRules(c1.getId(), "risk_notice", "JSON", true);
        EvalCaseEntity c2 = service.createCase(new CreateCaseRequest(dataset.getId(), "tc-117", "企业客户宕机催促",
                "核心 API 从 10:12 开始不可用，企业客户正在催促恢复。",
                "owner_team=SRE, risk_notice required", "critical", List.of("outage", "SRE")));
        addCommonRules(c2.getId(), "target_route", "JSON", true);
        EvalCaseEntity c3 = service.createCase(new CreateCaseRequest(dataset.getId(), "tc-121", "轻微 UI 反馈",
                "用户反馈设置页按钮文案不够清晰，暂不影响使用。",
                "low urgency, valid JSON", "normal", List.of("ui")));
        addCommonRules(c3.getId(), "target_route", "JSON", false);
        service.runEval(v1.getId(), dataset.getId());
        service.runEval(v2.getId(), dataset.getId());
    }

    private void seedLogAnalyzer() {
        PromptTemplateEntity template = service.createPromptTemplate(new CreatePromptTemplateRequest(
                "log-analyzer", "日志异常解释 Agent", "日志分析",
                "归纳日志突发模式并给出可能受影响的组件。", "Platform", "Draft"));
        PromptVersionEntity v1 = service.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v1", "Summarize the log anomaly. Return Markdown.",
                List.of(new PromptVariableDto("log_excerpt", "日志片段", true), new PromptVariableDto("deploy_window", "发布窗口", false)),
                "MARKDOWN", "Draft", "platform"));
        PromptVersionEntity v2 = service.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v2", "Summarize logs, cite evidence, and include risk notice when signal is incomplete. Return Markdown.",
                List.of(new PromptVariableDto("log_excerpt", "日志片段", true), new PromptVariableDto("service_map", "服务拓扑", false)),
                "MARKDOWN", "Candidate", "platform"));
        EvalDatasetEntity dataset = service.createDataset(new CreateDatasetRequest(
                "sre-alert-suite", "SRE 告警评测集", "日志分析", "覆盖告警去重与故障解释的本地演示用例。"));
        EvalCaseEntity c1 = service.createCase(new CreateCaseRequest(dataset.getId(), "lg-301", "Redis 连接池耗尽",
                "connection pool exhausted; timeout waiting for idle object; checkout-api p95 rising.",
                "component=redis_pool, markdown output", "high", List.of("redis")));
        addCommonRules(c1.getId(), "pass_candidate", "MARKDOWN", true);
        EvalCaseEntity c2 = service.createCase(new CreateCaseRequest(dataset.getId(), "lg-337", "缺少 trace id 的异常",
                "NullPointerException at OrderService, no trace_id present in snippet.",
                "risk notice required for incomplete evidence", "medium", List.of("trace")));
        addCommonRules(c2.getId(), "风险提示", "MARKDOWN", true);
        service.runEval(v1.getId(), dataset.getId());
        service.runEval(v2.getId(), dataset.getId());
    }

    private void seedJdReviewer() {
        PromptTemplateEntity template = service.createPromptTemplate(new CreatePromptTemplateRequest(
                "jd-screen", "JD 匹配评审 Agent", "JD 分析",
                "仅基于可验证证据评估简历与岗位要求的匹配度。", "Growth AI", "Approved"));
        PromptVersionEntity v1 = service.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v1", "Score resume fit against job description in plain text.",
                List.of(new PromptVariableDto("job_description", "职位描述", true), new PromptVariableDto("resume_text", "候选人简历", true)),
                "TEXT", "Candidate", "growth-ai"));
        PromptVersionEntity v2 = service.createPromptVersion(new CreatePromptVersionRequest(
                template.getId(), "v2", "Score resume fit using evidence only, include risk notice for inference gaps. Plain text output.",
                List.of(new PromptVariableDto("job_description", "职位描述", true), new PromptVariableDto("resume_text", "候选人简历", true), new PromptVariableDto("must_have_weights", "硬性条件权重", false)),
                "TEXT", "Approved", "growth-ai"));
        EvalDatasetEntity dataset = service.createDataset(new CreateDatasetRequest(
                "hiring-fit-golden", "JD 匹配黄金集", "JD 分析", "用于演示简历与 JD 证据匹配的黄金用例集。"));
        EvalCaseEntity c1 = service.createCase(new CreateCaseRequest(dataset.getId(), "jd-208", "Java 后端候选人",
                "JD requires Java, Spring, MySQL; resume has 6 years Java platform work.",
                "pass_candidate and no invented facts", "normal", List.of("backend")));
        addCommonRules(c1.getId(), "pass_candidate", "TEXT", false);
        EvalCaseEntity c2 = service.createCase(new CreateCaseRequest(dataset.getId(), "jd-236", "过度推断管理能力",
                "Resume says led feature delivery, does not mention direct reports.",
                "risk notice required for leadership inference", "medium", List.of("inference")));
        addCommonRules(c2.getId(), "风险提示", "TEXT", true);
        service.runEval(v1.getId(), dataset.getId());
        service.runEval(v2.getId(), dataset.getId());
    }

    private void addCommonRules(Long caseId, String keyword, String format, boolean riskNotice) {
        service.createRule(new CreateRuleRequest(caseId, "MUST_CONTAIN", keyword, null, BigDecimal.ONE, "必须包含关键词: " + keyword));
        service.createRule(new CreateRuleRequest(caseId, "FORBIDDEN_WORD", "内部策略", null, BigDecimal.ONE, "禁止泄露内部策略"));
        if (riskNotice) {
            service.createRule(new CreateRuleRequest(caseId, "RISK_NOTICE_REQUIRED", null, null, BigDecimal.ONE, "高风险用例必须包含风险提示"));
        }
        service.createRule(new CreateRuleRequest(caseId, "FORMAT", null, format, BigDecimal.ONE, "输出必须符合 " + format + " 格式"));
    }
}
