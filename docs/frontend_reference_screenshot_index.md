# 前端真实参考截图索引

## 1. 本轮任务说明

本轮只做三件事：

1. 只读审查 PromptOps Studio 当前项目现状。
2. 联网访问真实公开页面并采集本地参考截图。
3. 整理前端视觉升级前的参考索引文档。

本轮明确没有做的事：

1. 本轮没有修改 `frontend/` 代码。
2. 本轮没有修改 `backend/` 代码。
3. 本轮没有修改 `README.md`。
4. 本轮没有接入真实 API。
5. 本轮没有写入 API Key。

只读审查结论摘要：

- 当前前端更像“本地 PromptOps 演示控制台”，而不是普通企业后台。
- 但如果后续视觉升级做得不好，它仍然很容易滑回“有几张指标卡的普通后台”。
- 当前最需要补强参考的页面是：`Eval Dashboard`、`Batch Evaluation`、`Output Compare`、`Trace / Run Detail`、`Failure Cases`、`Dataset / Test Cases`。
- 当前最适合作为 README 截图的页面仍然是：`Eval Dashboard`、`Batch Evaluation`、`Trace / Run Detail`。
- 当前项目已经具备开始视觉升级的基础：已有真实前后端、Mock Eval 流程、Prompt 版本、Case、Trace、Review 和截图脚本。
- 当前项目仍然没有真实 LLM、真实 Agent Runtime、多模型对比和持久化审批流，后续视觉表达必须持续保留这条边界。

## 2. 项目视觉目标

PromptOps Studio 的目标不是做成普通后台管理系统。

目标方向应当是：

1. AI Eval / PromptOps / Developer Tool 控制台。
2. 面向 AI 应用开发者的工作台，而不是传统增删改查后台。
3. 适合 GitHub README、Boss 直聘、简历和面试展示。
4. 页面里必须清楚体现：Prompt 版本、批量评测、Trace、失败案例、Dataset、指标快照。
5. 推荐中文为主，必要技术词保留英文，例如 `Prompt`、`Eval Run`、`Trace`、`Dataset`、`Review`、`local-rule baseline`。

当前页面结构已经有 Prompt、Run、Trace、Review 的骨架，但还缺更强的“评测控制台”视觉叙事。后续视觉升级时，重点不是“更花哨”，而是让人一眼看出：

- 这是 AI Eval 项目。
- 这不是普通后台。
- 这项目有 PromptOps 工程闭环。

## 3. 截图目录与 Git 边界

本轮截图目录：

- `.local/reference_screenshots/`

Git 边界：

1. 该目录已通过 `.gitignore` 中的 `.local/` 规则忽略。
2. 第三方截图不会提交 GitHub。
3. 第三方截图不会放入 README。
4. 第三方截图不会放入 `docs/images/`。
5. README 最终只能使用本项目本地运行后的真实截图。
6. 本轮只允许新增 `.local/reference_screenshots/` 中的本地参考截图，不会污染项目展示资产。

## 4. 参考截图索引表

| 编号 | 来源名称 | 来源类型 | URL | 截图文件名 | 本地截图路径 | 访问结果 | 失败原因 | 适合参考的本项目页面 | 可借鉴的信息架构 | 可借鉴的视觉风格 | 可借鉴的交互方式 | 不能照搬的部分 | 对 PromptOps Studio 的具体启发 | 优先级 | 第一阶段落地 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Langfuse Docs Overview | 产品官网 / 文档 | [langfuse.com/docs](https://langfuse.com/docs) | `eval_dashboard_langfuse_docs_overview.png` | `.local/reference_screenshots/eval_dashboard_langfuse_docs_overview.png` | 成功 | - | Eval Dashboard | Prompt、Trace、Dataset、Evaluation 同平台组织 | 开发者工具感、清晰模块分区 | 文档导航和模块跳转 | Logo、品牌色、文案、截图 | 让首页一眼看出是 AI engineering console | P0 | 是 |
| 2 | Langfuse GitHub | GitHub | [github.com/langfuse/langfuse](https://github.com/langfuse/langfuse) | - | - | 失败 | 自动化判定页面含强登录提示，未纳入本轮可靠截图 | Prompt Studio / README | 开源项目首页的能力组织 | 开源可信感 | README 结构 | GitHub 页面布局、品牌元素 | README 可继续学习“能力 + 边界 + 截图”组织法 | P1 | 否 |
| 3 | LangSmith Evaluation Docs | 文档 | [docs.langchain.com/langsmith/evaluation](https://docs.langchain.com/langsmith/evaluation) | `batch_eval_langsmith_evaluation_docs.png` | `.local/reference_screenshots/batch_eval_langsmith_evaluation_docs.png` | 成功 | - | Batch Evaluation | Dataset -> Evaluator -> Experiment -> Result | 文档型、流程清晰 | 流程式信息排列 | LangChain 品牌、文案、截图 | 批量评测页应突出“配置 -> 运行 -> 结果” | P0 | 是 |
| 4 | Helicone Replay Session Docs | 文档 | [docs.helicone.ai/guides/cookbooks/replay-session](https://docs.helicone.ai/guides/cookbooks/replay-session) | `failure_cases_helicone_session_replay_docs.png` | `.local/reference_screenshots/failure_cases_helicone_session_replay_docs.png` | 成功 | - | Failure Cases | 失败案例回放、对比、问题定位 | 调试工具感 | 重放和问题定位路径 | 品牌、截图、产品术语 | 失败案例不该只是红色状态，而要能回放和解释 | P0 | 是 |
| 5 | Promptfoo GitHub | GitHub | [github.com/promptfoo/promptfoo](https://github.com/promptfoo/promptfoo) | `output_compare_promptfoo_github_readme.png` | `.local/reference_screenshots/output_compare_promptfoo_github_readme.png` | 成功 | - | Output Compare | prompts、tests、assertions、compare | 工程工具、结果驱动 | README 信息模块化 | README 原文、截图、品牌 | 输出对比要强调“规则、断言、结果差异” | P0 | 是 |
| 6 | Promptfoo Docs Intro | 文档 | [promptfoo.dev/docs/intro](https://www.promptfoo.dev/docs/intro/) | `batch_eval_promptfoo_docs_intro.png` | `.local/reference_screenshots/batch_eval_promptfoo_docs_intro.png` | 成功 | - | Batch Evaluation | 配置、断言、批量评测 | 清爽、开发者语境 | 文档分块和配置导向 | 文案、截图、品牌 | 批量评测页需要显式展示维度和断言 | P0 | 是 |
| 7 | OpenAI Evals GitHub | GitHub | [github.com/openai/evals](https://github.com/openai/evals) | `output_compare_openai_evals_github.png` | `.local/reference_screenshots/output_compare_openai_evals_github.png` | 成功 | - | Output Compare / Batch Evaluation | eval registry、自定义 eval、benchmark | 工程框架感 | 项目结构导向 | OpenAI 品牌、仓库内容、截图 | 可以借鉴“评测框架”叙事，但不能假装已接入 | P0 | 是 |
| 8 | Ragas Docs | 文档 | [docs.ragas.io/en/stable](https://docs.ragas.io/en/stable/) | `metrics_ragas_docs_home.png` | `.local/reference_screenshots/metrics_ragas_docs_home.png` | 成功 | - | Eval Dashboard / Metrics | metrics、evaluation loop、quality dimensions | 研究和指标感 | 文档导航与指标分组 | 指标名直接照搬为已实现能力 | Dashboard 可以预留未来语义评测维度，但当前不能写已实现 | P1 | 否 |
| 9 | Arize Phoenix Docs | 文档 | [arize.com/docs/phoenix](https://arize.com/docs/phoenix) | `trace_detail_arize_phoenix_docs.png` | `.local/reference_screenshots/trace_detail_arize_phoenix_docs.png` | 成功 | - | Trace / Run Detail | tracing、evaluation、troubleshooting | Observability 控制台感 | Trace 到分析的联动 | 品牌、产品截图、术语 | Trace 页要像执行链，而不是普通详情页 | P0 | 是 |
| 10 | W&B Weave Docs | 文档 | [docs.wandb.ai/weave](https://docs.wandb.ai/weave) | - | - | 失败 | 自动化判定页面含强登录提示，未纳入本轮可靠截图 | Eval Dashboard / Metrics | Track / Evaluate / Improve 闭环 | 实验评估平台感 | 文档入口组织 | 品牌、文案、截图 | 未来可继续参考“实验追踪 + 评估”结构 | P1 | 否 |
| 11 | Humanloop Create Prompt Docs | 文档 | [humanloop.com/docs/guides/prompts/create-prompt](https://humanloop.com/docs/guides/prompts/create-prompt) | - | - | 失败 | 自动化判定页面含强登录提示，未纳入本轮可靠截图 | Prompt Studio | Prompt registry、Prompt 版本、Prompt 日志 | Prompt 产品化管理 | Prompt 配置层级 | 品牌、文案、截图 | 可以继续作为 Prompt 工作台结构参考，但本轮未保留截图 | P1 | 否 |
| 12 | Supabase GitHub | GitHub / 开源项目 | [github.com/supabase/supabase](https://github.com/supabase/supabase) | `dataset_supabase_github_repo.png` | `.local/reference_screenshots/dataset_supabase_github_repo.png` | 成功 | - | Dataset / Test Cases | 资源对象列表和开发者平台分区 | 高级浅色开发者控制台 | 左侧导航 + 内容区组织 | 品牌、GitHub 页面、截图 | Dataset 页可以学“资产平台”感，而不是普通数据表 | P0 | 是 |
| 13 | Linear UI Redesign Article | 产品博客 | [linear.app/now/how-we-redesigned-the-linear-ui](https://linear.app/now/how-we-redesigned-the-linear-ui) | `failure_cases_linear_ui_redesign_article.png` | `.local/reference_screenshots/failure_cases_linear_ui_redesign_article.png` | 成功 | - | Failure Cases | 问题定位、信息收束、页面层级 | 克制、高质感、层级强 | 详情页聚焦逻辑 | Linear 品牌、动效、视觉细节 | 失败案例页需要有“问题定位台”的感觉 | P0 | 是 |
| 14 | Grafana Trace Explore Docs | 文档 | [grafana.com/docs/grafana/latest/visualizations/explore/trace-integration](https://grafana.com/docs/grafana/latest/visualizations/explore/trace-integration/) | `trace_detail_grafana_trace_integration_docs.png` | `.local/reference_screenshots/trace_detail_grafana_trace_integration_docs.png` | 成功 | - | Trace / Run Detail | timeline、trace detail、span drilldown | 可观测性时间线 | 层层钻取的时间线逻辑 | 图表样式、品牌、截图 | Trace Detail 应强调阶段、耗时、状态、失败原因 | P0 | 是 |
| 15 | Sentry Issue Details Docs | 文档 | [docs.sentry.io/product/issues/issue-details](https://docs.sentry.io/product/issues/issue-details/) | `failure_cases_sentry_issue_details_docs.png` | `.local/reference_screenshots/failure_cases_sentry_issue_details_docs.png` | 成功 | - | Failure Cases | issue header、impact、actions、events | 问题定位和 triage 感 | 详情页中的下一步动作提示 | 品牌、截图、问题模型 | 失败页应该让人知道“哪里错、影响什么、怎么修” | P0 | 是 |
| 16 | Datadog Trace View Docs | 文档 | [docs.datadoghq.com/tracing/trace_explorer/trace_view](https://docs.datadoghq.com/tracing/trace_explorer/trace_view/) | - | - | 失败 | 自动化判定页面含强登录提示，未纳入本轮可靠截图 | Trace / Run Detail | waterfall、flame graph、trace map | APM 分析感 | trace 视图切换 | 品牌、图表、截图 | 后续若需要更强 Trace 视觉，可再补这个方向 | P1 | 否 |
| 17 | Vercel Projects Docs | 文档 | [vercel.com/docs/projects](https://vercel.com/docs/projects) | - | - | 失败 | 自动化判定页面含强登录提示，未纳入本轮可靠截图 | Eval Dashboard | 项目、状态、部署对象概览 | 清爽开发者平台 | 项目总览页结构 | 品牌、截图、产品文案 | 首页可继续参考其干净分层，但本轮未保留截图 | P1 | 否 |
| 18 | LangSmith Manage Datasets Docs | 文档 | [docs.langchain.com/langsmith/manage-datasets](https://docs.langchain.com/langsmith/manage-datasets) | `dataset_langsmith_manage_datasets_docs.png` | `.local/reference_screenshots/dataset_langsmith_manage_datasets_docs.png` | 成功 | - | Dataset / Test Cases | dataset 资产管理、样本组织、数据集操作 | 文档型、结构清晰 | 数据集层级导航 | LangChain 品牌、截图、文案 | Dataset 页应体现“测试资产管理”而不是“表数据浏览” | P0 | 是 |
| 19 | Promptfoo Test Cases Docs | 文档 | [promptfoo.dev/docs/configuration/test-cases](https://www.promptfoo.dev/docs/configuration/test-cases/) | `dataset_promptfoo_test_cases_docs.png` | `.local/reference_screenshots/dataset_promptfoo_test_cases_docs.png` | 成功 | - | Dataset / Test Cases / Batch Evaluation | test case、variables、assertions、data-driven eval | 工具化、配置驱动 | 测试样本配置思路 | 文案、截图、品牌 | Dataset 与 Batch Eval 应该围绕测试样本和断言来设计 | P0 | 是 |

## 5. 按本项目页面分组的参考建议

### 5.1 Eval Dashboard / 评测总览

推荐参考来源：

- Langfuse Docs Overview
- Ragas Docs
- Vercel Projects Docs（本轮未保留截图，仅作结构参考）

推荐截图文件：

- `eval_dashboard_langfuse_docs_overview.png`
- `metrics_ragas_docs_home.png`

应该学习什么：

- 一屏就看出这是 AI Eval / PromptOps。
- 总览要围绕评测健康度、回归风险、失败样本、最近运行。
- 不是只堆 KPI，而是让人知道“这个系统在评测什么”。

不能照搬什么：

- Langfuse / Vercel 的品牌视觉。
- 第三方图标、Logo、截图排版。

本项目应该如何原创化：

- 用本项目自己的 Prompt、Run、Trace、Review、Mock 边界文案。
- 让总览服务于“本地 PromptOps 评测控制台”的定位。

README 截图时该页面负责证明什么能力：

- 证明项目有总览视角，不是零散功能页。
- 证明这是 AI Eval / PromptOps 项目。

### 5.2 Prompt Studio / Prompt 工作台

推荐参考来源：

- Langfuse GitHub
- Humanloop Create Prompt Docs
- Langfuse Docs Overview

推荐截图文件：

- 本轮无可靠本地截图保留，建议以 `Langfuse GitHub` 与 `Humanloop Create Prompt Docs` 继续做结构参考，不直接作为最终视觉照搬对象。

应该学习什么：

- Prompt 列表、当前版本、变量、规则、最近运行要放在一条工作流里。
- Prompt Compare 需要有“代码审查感”。

不能照搬什么：

- 第三方 Prompt 编辑器布局和品牌文案。

本项目应该如何原创化：

- 使用本项目已有的 `variables_json`、规则、版本对比、recent run 数据组织页面。

README 截图时该页面负责证明什么能力：

- 证明项目不是只跑评测，也有 Prompt 资产管理能力。

### 5.3 Batch Evaluation / 批量评测

推荐参考来源：

- LangSmith Evaluation Docs
- Promptfoo Docs Intro
- Promptfoo Test Cases Docs

推荐截图文件：

- `batch_eval_langsmith_evaluation_docs.png`
- `batch_eval_promptfoo_docs_intro.png`
- `dataset_promptfoo_test_cases_docs.png`

应该学习什么：

- `配置 -> 运行 -> 结果` 的评测流程感。
- 测试集、版本、规则/断言、结果矩阵的清晰关系。

不能照搬什么：

- 第三方命名、配置文案、图示结构。

本项目应该如何原创化：

- 明确展示 `local-rule baseline`。
- 不显示未接入的真实 Provider 结果。

README 截图时该页面负责证明什么能力：

- 证明项目具备批量评测和回归分析能力。

### 5.4 Output Compare / 输出对比

推荐参考来源：

- Promptfoo GitHub
- OpenAI Evals GitHub

推荐截图文件：

- `output_compare_promptfoo_github_readme.png`
- `output_compare_openai_evals_github.png`

应该学习什么：

- 对比要有明确结论，不是两段文本并排。
- 要把版本差异、规则失败、推荐结果放到一起。

不能照搬什么：

- GitHub README 视觉、品牌元素、原始文案。

本项目应该如何原创化：

- 对比的是本项目的 Prompt v1 / v2 和本地规则结果。
- 推荐结论必须写成“基于规则建议”，不能写成上线结论。

README 截图时该页面负责证明什么能力：

- 证明项目支持 Prompt 版本对比与回归判断。

### 5.5 Trace / Run Detail / 运行追踪详情

推荐参考来源：

- Arize Phoenix Docs
- Grafana Trace Explore Docs
- Datadog Trace View Docs（本轮未保留截图，仅作后续增强方向）

推荐截图文件：

- `trace_detail_arize_phoenix_docs.png`
- `trace_detail_grafana_trace_integration_docs.png`

应该学习什么：

- Trace 要像执行链。
- 阶段、状态、耗时、失败原因要一眼能读。

不能照搬什么：

- 第三方 trace 图表、品牌样式、span 视图。

本项目应该如何原创化：

- 当前阶段中心仍然是 `MockOutputGenerator`、`RuleEvaluator`、`Generation Trace`。
- latency 必须明确写成 simulated latency。

README 截图时该页面负责证明什么能力：

- 证明项目可追溯、可解释、能定位失败原因。

### 5.6 Failure Cases / 失败案例

推荐参考来源：

- Helicone Replay Session Docs
- Linear UI Redesign Article
- Sentry Issue Details Docs

推荐截图文件：

- `failure_cases_helicone_session_replay_docs.png`
- `failure_cases_linear_ui_redesign_article.png`
- `failure_cases_sentry_issue_details_docs.png`

应该学习什么：

- 失败页应该像问题定位台，不只是错误列表。
- 需要有失败原因、影响、相关版本、建议动作。

不能照搬什么：

- 第三方 issue 模型、品牌用语、页面风格。

本项目应该如何原创化：

- 失败原因必须围绕本项目已有规则：格式错误、风险提示缺失、关键词缺失、回归失败。

README 截图时该页面负责证明什么能力：

- 证明项目能沉淀失败样本并辅助修复。

### 5.7 Dataset / Test Cases / 测试集

推荐参考来源：

- Supabase GitHub
- LangSmith Manage Datasets Docs
- Promptfoo Test Cases Docs

推荐截图文件：

- `dataset_supabase_github_repo.png`
- `dataset_langsmith_manage_datasets_docs.png`
- `dataset_promptfoo_test_cases_docs.png`

应该学习什么：

- 测试集是资产，不是普通数据表。
- 样本、标签、规则、期望输出应该形成“实验室感”。

不能照搬什么：

- 第三方资源管理 UI、品牌语言、项目结构。

本项目应该如何原创化：

- 展示本项目自己的 dataset、case、risk、rule 和 recent hit。

README 截图时该页面负责证明什么能力：

- 证明项目有测试集和样本资产，不是临时试 Prompt。

## 6. 第一阶段推荐参考组合

### 6.1 README 首图参考方向

推荐来源：

- Langfuse Docs Overview

推荐截图：

- `eval_dashboard_langfuse_docs_overview.png`

适合本项目的原因：

- 它最接近 AI engineering 总览页的感觉。

不能复制的部分：

- 品牌、导航、页面样式、文案。

如何转化成本项目原创设计：

- 用本项目自己的 Prompt、Run、Review、Mock 边界做首页信息架构。

### 6.2 Dashboard 参考方向

推荐来源：

- Langfuse Docs Overview
- Ragas Docs

推荐截图：

- `eval_dashboard_langfuse_docs_overview.png`
- `metrics_ragas_docs_home.png`

适合本项目的原因：

- 一个强调平台总览，一个强调评测维度。

不能复制的部分：

- 指标视觉和品牌系统。

如何转化成本项目原创设计：

- 把维度落回本项目的 local-rule、regression、failure case、review queue。

### 6.3 Prompt Compare 参考方向

推荐来源：

- Promptfoo GitHub
- OpenAI Evals GitHub

推荐截图：

- `output_compare_promptfoo_github_readme.png`
- `output_compare_openai_evals_github.png`

适合本项目的原因：

- 一个强调比较，一个强调评测框架思维。

不能复制的部分：

- GitHub 视觉和开源文案。

如何转化成本项目原创设计：

- 做成 Prompt v1/v2 输出、规则差异、风险差异、建议版本的原创对比页。

### 6.4 Trace Detail 参考方向

推荐来源：

- Arize Phoenix Docs
- Grafana Trace Explore Docs

推荐截图：

- `trace_detail_arize_phoenix_docs.png`
- `trace_detail_grafana_trace_integration_docs.png`

适合本项目的原因：

- 都强调执行链、状态和问题定位。

不能复制的部分：

- 可观测性图表和产品符号。

如何转化成本项目原创设计：

- 改造成 `Input -> Prompt Build -> MockOutputGenerator -> RuleEvaluator -> Review` 的本项目 Trace。

### 6.5 Failure Cases / Dataset 参考方向

推荐来源：

- Sentry Issue Details Docs
- LangSmith Manage Datasets Docs
- Promptfoo Test Cases Docs

推荐截图：

- `failure_cases_sentry_issue_details_docs.png`
- `dataset_langsmith_manage_datasets_docs.png`
- `dataset_promptfoo_test_cases_docs.png`

适合本项目的原因：

- 一个负责失败定位，一个负责测试集结构。

不能复制的部分：

- issue 页面品牌风格、dataset 页面原始结构。

如何转化成本项目原创设计：

- 失败案例和测试集都围绕本项目已有规则、样本、Trace、Review 来组织。

## 7. 风险与边界

必须明确：

1. 第三方截图只作为本地参考。
2. 不复制 Logo。
3. 不复制商标。
4. 不复制官网文案。
5. 不复制截图。
6. 不复制代码。
7. 不把第三方截图放 README。
8. 不把第三方截图提交 GitHub。
9. 不把 AI 生成图伪装成真实产品参考。
10. 不把 mock 数据包装成真实线上能力。

本轮失败原因主要分为两类：

1. 自动化判定页面存在强登录提示，因此未纳入本轮可靠截图。
2. 某些页面更适合作结构参考，不适合作最终本地截图样本。

## 8. 下一步建议

建议：

1. 可以基于这批参考进入前端设计阶段，但还不能直接开始改代码。
2. 当前不缺核心参考，已经足够支撑第一阶段视觉升级。
3. 应优先改的页面是：`Eval Dashboard`、`Batch Evaluation`、`Trace / Run Detail`、`Output Compare`。
4. 第一阶段最小可落地页面范围建议为：
   `Eval Dashboard`、`Prompt Studio`、`Batch Evaluation`、`Output Compare`、`Trace / Run Detail`、`Failure Cases`、`Dataset / Test Cases`。
5. 其中如果要继续控制范围，最小最小优先集可以先做：
   `Eval Dashboard`、`Batch Evaluation`、`Trace / Run Detail`、`Output Compare`。

必须等待用户确认。

在用户没有明确回复“确认这个前端方向，可以开工”之前，不允许修改 `frontend` 代码、CSS、README 截图或截图脚本。
