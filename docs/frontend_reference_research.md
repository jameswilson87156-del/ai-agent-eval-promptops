# 前端参考调研

调研时间：2026-07-04
联网状态：成功。本文件只记录可打开的公开链接，不使用 AI 生成图作为参考图，不复制第三方 Logo、品牌文案、官网截图或 UI 代码。

## 调研结论摘要

PromptOps Studio 不应该做成普通后台管理系统。更适合的方向是：

- AI Developer Tool Console：像 Langfuse / LangSmith / Helicone 一样围绕 trace、eval、prompt、dataset 组织。
- EvalOps Dashboard：像 promptfoo / OpenAI Evals / Ragas 一样强调测试集、断言、得分、失败归因。
- Developer SaaS Console：像 Supabase / Vercel / Linear 一样保持信息密度、层级清晰、交互克制。
- Observability / Issue Triage：像 Grafana、Sentry、Datadog 一样让 Trace 和 Failure Case 有定位问题的感觉。

## 候选参考来源

| # | 名称 | 链接 | 来源类型 | 适合参考的页面类型 | 可借鉴的信息架构 | 可借鉴的视觉风格 | 不允许复制的部分 | 对 PromptOps Studio 的具体启发 | 优先级 | 第一阶段落地 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Langfuse Docs / Platform | [langfuse.com/docs](https://langfuse.com/docs) | 文档 / 产品官网 | Dashboard、Trace、Prompt Management、Experiments | Trace、Prompt、Dataset、Evaluation 放在同一 AI engineering 平台内 | 开发者工具感、清晰标签、trace/eval 联动 | Logo、品牌色、官网截图、文案 | Dashboard 应突出 Prompt、Trace、Eval、Review 的闭环，而不是只放 KPI | P0 | 是 | 最贴近本项目定位 |
| 2 | Langfuse GitHub | [github.com/langfuse/langfuse](https://github.com/langfuse/langfuse) | GitHub | README 展示、开源项目结构 | README 同时说明 observability、evals、prompt management、datasets | 开源项目可信感 | README 原文、徽章组合、截图 | README 可用“能力 + 边界 + 本地截图 + 工作流”组织 | P0 | 是 | 适合 README 参考 |
| 3 | LangSmith Evaluation | [docs.langchain.com/langsmith/evaluation](https://docs.langchain.com/langsmith/evaluation) | 文档 | Evaluation、Dataset、Experiment | Dataset -> Evaluator -> Experiment -> Result | 文档型清晰流程 | LangChain 品牌与截图 | Batch Evaluation 页面可按 dataset、evaluator、experiment 拆分 | P0 | 是 | 借鉴评测流程，不复制视觉 |
| 4 | Helicone Docs: Session Replay | [docs.helicone.ai/guides/cookbooks/replay-session](https://docs.helicone.ai/guides/cookbooks/replay-session) | 文档 | Session Replay、Trace Replay、Prompt 调试 | 用 replay 思路对比修改前后的输出 | 工具型、偏调试 | 产品截图、品牌文案 | Failure Cases 可以设计“回放一次失败 Case -> 查看输出 -> 修复建议” | P0 | 是 | 与失败案例回放高度相关 |
| 5 | Helicone GitHub | [github.com/helicone/helicone](https://github.com/helicone/helicone) | GitHub | LLM Observability、Prompt Management | Requests、Sessions、Prompt、Metrics | 数据密集但清晰 | UI 代码、Logo、商标 | Provider Status 和成本/延迟未来可参考其信息层级 | P1 | 否 | 本轮不能写已接真实 Provider |
| 6 | Promptfoo GitHub | [github.com/promptfoo/promptfoo](https://github.com/promptfoo/promptfoo) | GitHub | Eval Result、CLI/CI、README | prompts、providers、tests、assertions、outputs | 测试工具感、结果表格 | README 原文、示例图、品牌 | 本项目 metrics 可借鉴“local-rule baseline + pass/fail matrix” | P0 | 是 | 很适合规则评测基线 |
| 7 | Promptfoo Docs | [promptfoo.dev/docs/intro](https://www.promptfoo.dev/docs/intro/) | 文档 | Eval 配置、断言、CI | Benchmark、assertion、cache、CI/CD | 文档清爽、开发者语境 | UI 代码和品牌 | Batch Evaluation 可突出规则维度和断言结果 | P0 | 是 | 与 local-rule 评测最接近 |
| 8 | OpenAI Evals | [github.com/openai/evals](https://github.com/openai/evals) | GitHub | Eval Framework、Registry、Custom Evals | eval registry、自定义 eval、私有数据集 | 偏工程框架，不重视觉 | OpenAI 品牌、README 原文 | README 可解释“当前是本地规则 eval，未来可扩展 LLM eval registry” | P0 | 是 | 只能借鉴概念，不声称接入 |
| 9 | Ragas Docs | [docs.ragas.io](https://docs.ragas.io/en/stable/) | 文档 | RAG/LLM Evaluation Metrics | 从 vibe check 到 systematic evaluation loops | 研究/指标感 | 指标名直接套用为已实现能力 | Metrics 规划可区分 relevance、faithfulness 等未来维度 | P1 | 否 | 当前未实现 RAG 和 LLM-as-Judge |
| 10 | Arize Phoenix Docs | [arize.com/docs/phoenix](https://arize.com/docs/phoenix) | 文档 | Trace、Evaluation、Troubleshooting | Trace 捕获模型调用、检索、工具和自定义逻辑 | Observability 控制台感 | 视觉细节、截图 | Trace Detail 可按 Input、Prompt Build、Provider Call、Output、Scoring 组织 | P0 | 是 | 适合 Trace 页面结构 |
| 11 | Arize Phoenix GitHub | [github.com/Arize-ai/phoenix](https://github.com/Arize-ai/phoenix) | GitHub | AI Observability / Evaluation | tracing、evaluation、experimentation | 开源 AI observability | UI 代码和图标 | 为项目后续 OpenTelemetry/Trace 叙事提供参考 | P1 | 否 | 当前项目不是 OTEL trace |
| 12 | W&B Weave Docs | [docs.wandb.ai/weave](https://docs.wandb.ai/weave) | 文档 | LLM Observability、Evaluation、Datasets | Track、Evaluate、Improve | 实验追踪与评估结合 | 品牌、截图、产品文案 | Eval Dashboard 可把 runs、scores、feedback 组织为连续改进闭环 | P1 | 否 | 当前没有真实生产 agent |
| 13 | W&B Weave GitHub | [github.com/wandb/weave](https://github.com/wandb/weave) | GitHub | README、Trace、Eval | inputs、outputs、traces、apples-to-apples evals | 工程严谨感 | README 原文、商标 | Baseline Compare 文档可强调 apples-to-apples 对比 | P0 | 是 | 适合指标对比口径 |
| 14 | Humanloop Eval UI Docs | [humanloop.com/docs/guides/evals/run-evaluation-ui](https://humanloop.com/docs/guides/evals/run-evaluation-ui) | 文档 | Prompt Evaluation、Human Feedback | Dataset + Evaluators + Prompt Versions | 人工评审/评估流程感 | 品牌、截图、文案 | Prompt v1/v2 + Dataset + Evaluator 的页面结构可参考 | P1 | 否 | 其文档提示平台计划 2025-09-08 sunset；2026 年应作为历史参考 |
| 15 | Humanloop Prompt Management | [humanloop.com/docs/guides/prompts/create-prompt](https://humanloop.com/docs/guides/prompts/create-prompt) | 文档 | Prompt Registry、Prompt Versions | Prompt 作为 registry 管理版本和 logs | 产品化 Prompt 管理 | 品牌、截图、账号流程 | Prompt Studio 可突出版本、变量、最近运行、关联评测 | P0 | 是 | 只学信息架构 |
| 16 | Dify GitHub | [github.com/langgenius/dify](https://github.com/langgenius/dify) | GitHub | AI App Platform、Workflow、LLMOps | Workflow、RAG、Agent、Model、Observability | 开源 AI 应用平台 | 复杂工作流 UI、Logo、文案 | 本项目不要假装有 Dify 级 workflow，只借鉴 AI 应用工作台的导航层级 | P2 | 否 | 当前项目无真实 workflow/agent |
| 17 | Vercel AI SDK Telemetry | [ai-sdk.dev/docs/ai-sdk-core/telemetry](https://ai-sdk.dev/docs/ai-sdk-core/telemetry) | 文档 | Provider Call、Telemetry、Trace | generation lifecycle telemetry | 开发者文档风格 | SDK 文案、示例代码直接照搬 | Trace / Run Detail 可预留 Provider Call 和 token/cost 字段 | P1 | 否 | 当前不接真实模型 |
| 18 | Supabase | [github.com/supabase/supabase](https://github.com/supabase/supabase) | GitHub / 产品 | Developer Dashboard、Database Console | 项目、数据库、Auth、Storage、Logs 分区 | 高级浅色开发者控制台 | 品牌、具体组件、截图 | Dataset 页面可学习“表格 + 详情抽屉 + 状态标签” | P0 | 是 | 适合布局密度 |
| 19 | Linear UI Redesign | [linear.app/now/how-we-redesigned-the-linear-ui](https://linear.app/now/how-we-redesigned-the-linear-ui) | 产品博客 | Navigation、Issue Detail、Workflow | 快速定位、紧凑任务流 | 克制、高质感、层级强 | Linear 视觉、动效、品牌 | Failure Cases 可以借鉴问题定位、状态转换和详情聚焦 | P0 | 是 | 只学 polish 和信息层级 |
| 20 | Raycast | [raycast.com](https://www.raycast.com/) | 产品官网 | Developer Command Console | 命令式、快捷入口、插件生态 | 高级深色、快速工具感 | 视觉风格照搬、Logo、文案 | 可用于 PromptOps 的“操作台”气质：快速运行、筛选、定位 | P1 | 否 | 适合局部交互氛围 |
| 21 | PostHog GitHub | [github.com/posthog/posthog](https://github.com/posthog/posthog) | GitHub | Product Analytics、Session Replay | analytics、replay、experiments、feature flags | 开发者平台，信息密度高 | 品牌、插画、UI 代码 | Failure Cases 可参考 replay + event context 的调试路径 | P1 | 否 | 当前没有用户行为数据 |
| 22 | Grafana Trace Explore | [grafana.com/docs/grafana/latest/visualizations/explore/trace-integration](https://grafana.com/docs/grafana/latest/visualizations/explore/trace-integration/) | 文档 | Trace Timeline、Span Detail | minimap、timeline、span detail | 可观测性、时间轴 | Grafana UI 和图形样式 | Trace Detail 可做分段 timeline：Input -> Prompt Build -> Output -> Scoring | P0 | 是 | 最适合 Trace 结构 |
| 23 | Sentry Issue Details | [docs.sentry.io/product/issues/issue-details](https://docs.sentry.io/product/issues/issue-details/) | 文档 | Failure Case、Issue Detail、Triage | issue header、impact、actions、events | 问题定位和 triage 感 | 品牌、截图、文案 | Failure Cases 页面可像 issue triage：原因、影响、状态、下一步 | P0 | 是 | 很适合失败样本页面 |
| 24 | Datadog Trace View | [docs.datadoghq.com/tracing/trace_explorer/trace_view](https://docs.datadoghq.com/tracing/trace_explorer/trace_view/) | 文档 | Trace Waterfall、Span List | flame graph、span list、waterfall、map | APM 分析感 | Datadog 视觉和图表 | Provider Call / Scoring 未来可用 waterfall 表达耗时分布 | P1 | 否 | 当前 latency 是模拟值 |
| 25 | Retool Templates | [retool.com/templates](https://retool.com/templates) | Demo / 模板 | Internal Tool、Dashboard | 快速搭建管理/运营工具 | 实用、组件化 | 模板 UI、组件布局照搬 | 可参考“左侧对象列表 + 右侧详情/操作”的高效工具布局 | P2 | 否 | 避免变成低质 CRUD |
| 26 | Vercel Projects / Deployments | [vercel.com/docs/projects](https://vercel.com/docs/projects) | 文档 / 产品 | Project Dashboard、Deployments | 项目 -> 部署 -> 指标 -> 日志 | 极简开发者平台 | 品牌、页面截图 | README 首屏和状态面板可借鉴清晰分层 | P1 | 否 | 当前无线上部署，不可写 deployment |

## 调研中形成的设计约束

- 第一阶段不复制任何参考产品的截图、Logo、商标、品牌文案、UI 代码。
- README 中只能使用本仓库真实页面生成的截图。
- 调研参考只用于信息架构、交互层级、页面叙事和视觉气质。
- Humanloop 相关链接仅作为历史产品参考；当前日期为 2026-07-04，其文档提到的 sunset 日期已经过去，不能写成当前活跃平台能力背书。
- 本项目仍然是 local demo + mock output + rule-based eval，不能写成真实 LLMOps 平台。
