# PromptOps Studio 前端 Moodboard

本 moodboard 基于 `docs/frontend_reference_research.md` 的真实公开来源。它用于指导下一阶段前端方向，不代表已经开始前端开发。

## 1. 最终推荐的 5 个参考方向

| 方向 | 主要参考 | 适合页面 | 为什么适合 |
| --- | --- | --- | --- |
| AI Engineering Console | Langfuse、LangSmith、Helicone | Eval Dashboard、Prompt Studio、Trace / Run Detail | 与 Prompt、Eval、Trace、Dataset、Review 闭环最贴近 |
| EvalOps / Test Lab | promptfoo、OpenAI Evals、W&B Weave | Batch Evaluation、Output Compare、Dataset / Test Cases | 强调测试集、断言、baseline、失败归因，适合简历表达 |
| Observability Trace | Grafana Explore、Datadog Trace View、Phoenix | Trace / Run Detail | 让一次评测像一条可解释执行链，而不是普通详情页 |
| Issue Triage / Failure Review | Sentry Issue Details、Linear | Failure Cases、Human Review | 失败样本应像 issue 一样可定位、可归因、可处理 |
| Developer SaaS Console | Supabase、Vercel、Raycast | 全局导航、README 截图、操作入口 | 让项目有开发者工具质感，而不是 Element Plus 默认后台 |

## 2. 页面对应参考

| 页面 | 应参考谁 | 视觉重点 |
| --- | --- | --- |
| Eval Dashboard | Langfuse + W&B Weave + Supabase | 一屏看清 Prompt 数、Run 数、Pass Rate、Regression Alerts、Failure Cases、Recent Runs |
| Prompt Studio / Prompt Compare | Langfuse Prompt Management + Humanloop Prompt Registry + Linear | 左侧 Prompt 列表，中间 Prompt 版本与变量，右侧评分规则/最近运行；Prompt Diff 有代码审查感 |
| Batch Evaluation | promptfoo + LangSmith Evaluation + OpenAI Evals | Dataset、Evaluator、Prompt Version、Run Status、Result Matrix、Progress |
| Output Compare | W&B Weave + promptfoo | v1/v2 输出并排、规则得分、diff 高亮、推荐版本，但不写成真实模型效果 |
| Trace / Run Detail | Grafana Trace Explore + Phoenix + Datadog Trace View | Input -> Prompt Build -> Mock/Provider Call -> Output -> Scoring -> Review Timeline |
| Failure Cases | Sentry Issue Details + Linear | 失败原因、影响等级、关联 Prompt 版本、期望输出、修复建议、回归状态 |
| Dataset / Test Cases | Supabase + Retool + promptfoo | 测试集列表、Case 输入/期望输出、标签、难度、评价维度、最近命中情况 |
| README 截图展示 | Langfuse GitHub + Supabase GitHub + Vercel Docs | 截图顺序服务叙事：总览 -> 批量评测 -> Trace -> Prompt Compare -> Failure Cases |

## 3. 推荐视觉关键词

- 中文为主，技术名词保留英文：Prompt、Eval Run、Trace、Dataset、Provider、Review、Baseline、local-rule。
- 克制的开发者工具质感，不做花哨 AI 大屏。
- 色彩可以选择高级浅色，也可以选择暗色；第一阶段建议用“浅色开发者控制台 + 深色代码/Trace 区块”混合方向。
- 指标卡片简洁，避免堆砌过多 KPI。
- Trace 和 Failure Cases 应有“问题定位”感。
- Prompt Diff 应有“代码审查”感。
- Dataset 页面应有“实验室样本管理”感。

## 4. 每个页面要做成什么视觉重点

### Eval Dashboard

- 首屏不是传统四张 KPI + 大表格。
- 以“本次评测是否可用”为核心：Pass Rate、Avg Score、Regression Alerts、Failure Cases、Review Required。
- Recent Eval Runs 显示版本、数据集、通过率、失败数、模拟耗时。
- Score Trend 第一阶段可以用本地数据静态趋势，但必须标注 Demo/Mock。

### Prompt Studio / Prompt Compare

- 左侧 Prompt Registry，支持状态和场景标签。
- 中间是 Prompt 版本、变量、输出格式、最近运行。
- 右侧是规则、版本对比、迭代建议。
- Prompt Diff 用代码块、行内高亮、规则标签，不做普通表单。

### Batch Evaluation

- 展示一次批量评测的“配置 -> 执行 -> 结果”。
- Dataset、Prompt Version、Evaluator / local-rule baseline、Run Status 放在同一流程带里。
- 第一阶段不要出现真实 Provider 名称，除非明确是“未接入 / planned”。

### Output Compare

- v1/v2 输出并排，显示得分差异和失败项。
- 格式错误、风险提示缺失、必含词缺失等规则失败要可视化。
- 推荐采用版本只能写成“基于规则建议”，不能写成上线结论。

### Trace / Run Detail

- 用纵向 timeline 或横向 stage strip 展示执行链。
- 当前阶段的 Provider Call 应写为 MockOutputGenerator。
- latency 写成 simulated latency。
- 每个阶段有状态、耗时、摘要、失败原因。

### Failure Cases

- 像 Sentry issue 一样突出：标题、影响等级、失败原因、关联版本、最近一次运行、下一步建议。
- 右侧显示输入、期望输出、Mock 输出、规则失败详情。
- 可以增加“是否已回归通过”字段，但第一阶段只做本地展示。

### Dataset / Test Cases

- 测试集作为“实验室资产”展示，不是普通数据表。
- Case 需要显示输入、期望输出、标签、难度/风险、评价维度、最近命中情况。
- 30 条样本规划应在 metrics 文档落地后再进入页面。

## 5. 必须保留原创的视觉元素

- PromptOps Studio 的产品名、信息架构、页面文案。
- 本项目的 local demo / mock / rule-based 边界提示。
- 评测维度、失败原因、规则标签。
- README 截图必须来自本仓库真实页面。
- 数据样本必须来自本项目 Demo 或未来本地脚本输出。

## 6. 只能学习信息架构，不能复制视觉的参考

- Linear：只能学习任务/问题定位层级，不复制其细腻渐变、动效和品牌视觉。
- Raycast：只能学习命令式工具感，不复制深色 launcher 风格。
- Supabase / Vercel：只能学习开发者控制台清爽层级，不复制品牌色和页面结构。
- Grafana / Datadog / Sentry：只能学习 trace/issue 结构，不复制图表和产品截图。
- Langfuse / LangSmith / Helicone：只能学习 AI engineering workflow，不复制 Logo、截图和营销文案。

## 7. 需要用户确认的点

请用户确认：

1. 是否采用“浅色开发者控制台 + 深色代码/Trace 区块”的视觉方向。
2. 第一阶段是否只做最小页面范围：Eval Dashboard、Prompt Studio、Batch Evaluation、Trace Detail、Failure Cases、Dataset/Test Cases。
3. 是否接受继续保持“无真实 LLM、无真实 Agent”的诚实边界。
4. 是否同意下一阶段先补 metrics snapshot，再改前端。

在用户明确回复“确认这个前端方向，可以开工”之前，不进入前端代码改造。
