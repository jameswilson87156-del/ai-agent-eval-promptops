# PromptOps Studio 项目审查报告

审查时间：2026-07-04
审查范围：只读审查 README、前后端结构、接口、测试、脚本、Demo 数据、截图资产和现有文档；本轮未修改前端或后端业务代码。

## 1. 项目当前状态总结

| 项目 | 当前事实 |
| --- | --- |
| 当前分支 | `main` |
| 初始 Git 状态 | `git status --short` 无输出，工作区干净 |
| 项目结构 | Maven 聚合项目 + `backend/` Spring Boot 模块 + `frontend/` Vue/Vite 前端 + `docs/` 文档与截图 |
| 前端技术栈 | Vue 3、TypeScript、Vite、lucide-vue-next、Playwright 截图脚本 |
| 后端技术栈 | Java 17、Spring Boot 3.3.6、MyBatis-Plus 3.5.7、MySQL、H2、Springdoc OpenAPI、JUnit 5 |
| 截图资产 | 已存在 `docs/images/*.png` 和 `docs/images/large/*.png`，共 8 张 |
| Mock 数据 | 已存在，由 `DemoDataInitializer` 写入内存 H2 或本地库 |
| Prompt 模板 | 已存在 3 个 Demo 模板：`ticket-router`、`log-analyzer`、`jd-screen` |
| Eval 数据 | 已存在 3 个 Dataset、7 个 Case、26 条左右规则、6 次初始 Eval Run |
| 测试 | 已存在后端集成测试与领域单元测试，共 13 个 |
| API 文档 | 已接入 Springdoc，后端启动后可访问 `/swagger-ui.html` 和 `/api-docs` |
| metrics / benchmark / eval snapshot | 未发现 `docs/metrics` 或可追溯的指标快照文档 |
| 是否适合直接放简历 | 可以放，但必须写成本地 PromptOps / 规则评测项目，不能写成真实 LLM/Agent 线上平台 |
| 当前最大短板 | 缺少可追溯的 Eval 指标快照、Baseline 对比文档和更完整的前端信息架构规划 |

## 2. 前端页面现状

当前前端入口为 `frontend/src/App.vue`，是一个单页 Vue 控制台，包含 4 个视图：

- Overview：Prompt Profile 列表、最新 Eval Run 摘要、Review 队列、版本对比摘要。
- Eval Run：Case Result 列表、状态筛选、搜索、Rule Audit 面板。
- Prompt Versions：版本链路、Prompt 模板、变量、输出格式、评分规则、版本对比。
- Trace Review：Case 列表、输入/输出摘要、规则检查、Generation Trace、前端本地 Review 状态。

已有优点：

- 前端真实 fetch 后端 API，不是纯静态页面。
- 已有搜索、筛选、Prompt 切换、Case 切换、Run 触发、本地 Review 操作。
- 页面明确写出 `No real LLM connected`、`MockOutputGenerator`、`RuleEvaluator` 和 `Review state is frontend-only demo state`。
- 已有 `MetricCard.vue`、`StatusBadge.vue` 两个展示组件。

当前不足：

- 还不是完整的 7 页面 PromptOps Studio 信息架构：缺少独立 Batch Evaluation、Output Compare、Failure Cases、Dataset / Test Cases 页面。
- 当前视觉已比普通 CRUD 好，但仍需要基于真实参考进一步定义稳定的设计系统和截图优先级。
- Review 结论只存在浏览器状态，后端未持久化。

## 3. 后端接口现状

后端控制器为 `PromptOpsController`，当前共有 11 个 REST 接口：

- `GET /api/dashboard/summary`
- `GET /api/prompts`
- `POST /api/prompt-templates`
- `POST /api/prompt-versions`
- `POST /api/eval-datasets`
- `POST /api/eval-cases`
- `POST /api/score-rules`
- `POST /api/eval-runs`
- `GET /api/prompts/{promptId}/eval-runs/current`
- `GET /api/prompts/{promptId}/comparisons/latest`
- `GET /api/generation-traces`

已有能力：

- Prompt 模板、版本、Dataset、Case、Rule 创建接口。
- Eval Run 执行接口，执行后写入 Eval Run、Case Result 和 Generation Trace。
- 当前 Run、版本对比和 Trace 查询接口。
- `GlobalExceptionHandler` 统一错误响应。
- Springdoc `@Tag` / `@Operation` 支持接口文档展示。

边界：

- 当前没有分页接口，列表返回仍是小样本 Demo 规模。
- 当前没有认证、权限、多租户、审计日志、审批状态机。
- 当前没有真实 Provider 调用、异步队列、重试、超时、成本统计。

## 4. 数据模型现状

`schema.sql` 中已经建模：

- `prompt_template`
- `prompt_version`
- `eval_dataset`
- `eval_case`
- `score_rule`
- `eval_run`
- `eval_case_result`
- `generation_trace`

数据链路清晰：

```text
prompt_template -> prompt_version
prompt_template.scenario -> eval_dataset -> eval_case -> score_rule
prompt_version + eval_dataset -> eval_run -> eval_case_result
eval_run + eval_case + prompt_version -> generation_trace
```

当前可写进简历的点：

- 有明确领域模型，不是简单 CRUD。
- `variables_json` 已改为标准 JSON 数组存储，并由 Jackson 序列化/反序列化。
- Demo/Test 使用 H2，默认配置支持 MySQL。

不能夸大的点：

- 数据来自本地种子和 Mock，不是线上真实数据。
- latency 是本地确定性模拟耗时，不是真实模型 latency。
- Trace 是本地评测 Trace，不是完整生产可观测平台。

## 5. Prompt / Eval 相关能力现状

已实现：

- Prompt 模板和多版本管理。
- Demo 版本链路：每个 Prompt 有 `v1` / `v2`。
- 评测集、评测 Case、评分规则建模。
- RuleEvaluator 支持 `MUST_CONTAIN`、`FORBIDDEN_WORD`、`RISK_NOTICE_REQUIRED`、`FORMAT`。
- FORMAT 规则使用 Jackson 做 JSON 解析，不是简单括号判断。
- MockOutputGenerator 按版本和 Case 生成确定性输出。
- EvaluationPolicy 判断 Review 队列和回归风险。
- 版本对比展示通过率、模拟耗时、回归风险和失败归因。

未实现：

- 真实 LLM Provider。
- LLM-as-Judge。
- 多模型对比。
- Agent Runtime。
- 向量数据库 / RAG。
- 数据集版本锁定、置信区间、统计显著性。
- 可追溯的 `docs/metrics/eval_snapshot.md`、`baseline_compare.md`、`failure_cases.md`。

## 6. README 现状

README 当前质量较好：

- 顶部有静态技术徽章。
- 一句话定位明确为本地 PromptOps 实验台。
- 明确写出诚实边界：无真实 LLM、无真实 AI Agent、无多模型对比、无 LLM-as-Judge。
- 已展示真实仓库截图。
- 有快速启动、测试命令、项目结构、简历可写亮点和面试可讲点。

仍可增强：

- 可以新增更强的首屏价值结构：一句话定位、流程图、核心截图、指标快照、Baseline 表。
- 可以补 `docs/metrics` 的指标来源后，再把可追溯数据写进 README。
- README 截图顺序可按“Dashboard -> Eval Run -> Trace -> Prompt Compare”重新组织，让读者更快看懂 AI Eval / PromptOps 主题。

## 7. 测试与构建现状

本轮实际执行：

```powershell
mvn -pl backend test
npm run build
npm run
```

实际结果：

- `mvn -pl backend test`：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- `npm run build`：通过，`vue-tsc --noEmit && vite build` 成功。
- 当前 npm 脚本：`dev`、`typecheck`、`build`、`preview`、`screenshots`。

未运行：

- 未运行 `npm run screenshots`，因为脚本会启动后端和 Vite，并覆盖 `docs/images` 截图资产。
- 未启动后端、前端、数据库或 Docker 做人工浏览器验收。
- 未运行 `mvn package`。

## 8. 可写进简历的已有能力

可以写，但要保持边界：

- 基于 Spring Boot 3 + MyBatis-Plus 建模 Prompt 模板、版本、评测集、Case、规则、Run、Result、Trace。
- 实现本地规则评测闭环：Prompt Version -> Eval Dataset -> Eval Run -> Rule Scoring -> Case Result -> Trace -> Review -> Version Compare。
- 使用 RuleEvaluator 做可解释规则评分，覆盖关键词、禁用词、风险提示、JSON/Markdown/Text 格式校验。
- 使用 Vue 3 + TypeScript 构建可搜索、可筛选、可交互的 PromptOps 控制台。
- 使用 H2 demo profile 支持本地零配置演示。
- 使用 JUnit / Spring Boot Test 验证核心流程，当前 13 个测试通过。
- 使用 Springdoc 生成本地 OpenAPI 文档。

## 9. 暂时不能写进简历的能力

以下内容现在不能写成已实现：

- 接入真实 LLM。
- 实现真实 AI Agent Runtime。
- 接入多模型对比。
- 实现 LLM-as-Judge。
- 支持真实 token、成本、供应商 latency 统计。
- 支持生产级权限、审批流、审计日志。
- 支持 RAG / 向量数据库。
- 已有真实用户、线上流量、商业收益、生产稳定性。
- 已有大规模 benchmark 或统计显著性结论。

## 10. 最大风险点

1. 名称中含有 AI Agent / LLM Eval 相关表达，容易被误解为真实模型或真实 Agent 平台。
2. Demo 指标目前来自本地 Mock，不能直接包装成模型质量提升。
3. 前端虽然可用，但还缺少面向作品集的完整页面规划和更强截图叙事。
4. 缺少 `docs/metrics` 下可追溯的 eval snapshot / baseline compare / failure cases。
5. Review 只有前端状态，不能写成审批系统。

## 11. 下一阶段最小可落地优化路线

建议下一阶段仍然小步走：

1. 先确认本轮 `frontend_moodboard.md` 和 `frontend_showcase_design.md` 的视觉方向。
2. 补 `docs/metrics/eval_snapshot.md`、`baseline_compare.md`、`failure_cases.md`，只使用本地 Mock Eval 可追溯数据。
3. 在不接真实模型的前提下，扩充第一轮 30 条 Prompt 测试样本和 5 个规则维度。
4. 用户明确回复“确认这个前端方向，可以开工”后，再进入前端页面改造。
5. 前端第一阶段只落地 Dashboard、Prompt Studio、Batch Eval、Trace Detail、Failure Cases、Dataset/Test Cases 的最小可演示信息架构，不碰真实模型接入。
