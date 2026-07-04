# 前端方向用户验收说明

这份文档是给非设计师看的前端方向确认稿。它不讲太多设计术语，只说明下一阶段前端大概会长什么样、每个页面负责证明什么、以及你需要确认哪些点。

## 最终视觉方向总结

1. 整体会像一个给 AI 应用开发者用的评测控制台，类似“Prompt 版本 + 批量评测 + Trace + 失败定位”的工作台。
2. 它不会像普通后台管理系统，也不会像低质蓝白 CRUD 页面、Element Plus 默认后台或花哨 AI 大屏。
3. 主体建议是高级浅色界面，配合深色代码块、Trace 区块和清晰状态色，看起来专业、克制、有开发者工具感。
4. 信息密度会比普通展示页高，但不会堆满表格；每屏都要让人知道“现在评测了什么、哪里失败、下一步怎么修”。
5. 这种方向适合作品集展示，因为面试官一眼能看出它不是简单 Prompt demo，而是一个有数据、有流程、有后端模型、有前端交互的 AI Eval / PromptOps 项目。

## 这不是普通后台管理系统

普通后台管理系统通常是：菜单、表格、增删改查、几张统计卡片。

PromptOps Studio 应该是：围绕 Prompt 版本、评测集、批量运行、规则评分、失败样本、Trace 回放和人工 Review 的 AI Developer Tool 控制台。

用户进入页面后，第一感觉应该是：

- 这是一个 AI Prompt / Eval 工作台。
- 它能比较 Prompt 版本好坏。
- 它能解释为什么某个 Case 失败。
- 它能把失败样本、规则、Trace 和 Review 连起来。
- 它诚实标注当前是 Local Demo / Mock Output / Rule-based Eval，不假装已经接入真实大模型。

## 7 个核心页面验收表

| 页面 | 页面一句话作用 | 第一眼最应该看到什么 | 面试官能看出什么能力 | README 截图负责证明什么 | 参考了哪些产品的信息架构 | 哪些地方保持原创 | 做差了最容易变成什么低级效果 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Eval Dashboard | 项目总览页，回答“当前 Prompt 评测整体健康吗”。 | Pass Rate、Avg Score、Regression Alerts、Failure Cases、Recent Eval Runs。 | 你能把 Prompt 评测结果数据化，而不是只展示一次模型输出。 | 证明这是 AI Eval / PromptOps 控制台，不是普通后台。 | Langfuse、W&B Weave、Supabase。 | 指标口径、Mock 边界提示、PromptOps 文案、本项目截图。 | 变成四张 KPI 卡加一张普通表格，看不出 AI Eval 主题。 |
| Prompt Studio | 管理 Prompt 模板、版本、变量、规则和最近运行。 | 左边 Prompt 列表，中间 Prompt 内容，右边版本和评分规则。 | 你有 Prompt 管理和版本迭代意识。 | 证明项目不只是跑评测，也能管理 Prompt 资产。 | Langfuse Prompt Management、Humanloop Prompt Registry、Linear。 | Prompt 模板内容、变量、规则、版本说明、local-rule 口径。 | 变成普通表单编辑页，像后台管理“新增 Prompt”。 |
| Batch Evaluation | 展示一次批量评测怎么配置、运行和看结果。 | Dataset、Prompt Version、local-rule baseline、运行状态、结果矩阵。 | 你能做批量测试和回归评测，不是手动试 Prompt。 | 证明项目有“评测流程”，不是单条 demo。 | promptfoo、LangSmith Evaluation、OpenAI Evals。 | 本地规则维度、Demo 数据、Run 结果展示、无真实 Provider 标注。 | 变成普通任务列表，或者假装有真实模型批量调用。 |
| Output Compare | 对比两个 Prompt 版本的输出和规则得分。 | v1/v2 左右对比、差异高亮、规则失败、推荐版本。 | 你能解释 Prompt 改动带来的结果变化。 | 证明项目支持版本对比和回归判断。 | W&B Weave、promptfoo、Linear diff 思路。 | 输出样本、规则解释、推荐理由、基于规则的判断口径。 | 变成两段文本并排，没有得分、没有原因、没有结论。 |
| Trace / Run Detail | 回放一次 Case 从输入到评分的完整过程。 | Input、Prompt Build、MockOutputGenerator、Output、RuleEvaluator、Review 时间线。 | 你有可解释、可追溯的工程思维。 | 证明项目能解释“为什么这个结果失败”。 | Grafana Trace Explore、Phoenix、Datadog Trace View。 | 本项目的 Trace 字段、Mock 输出、规则检查、模拟耗时说明。 | 变成普通详情页，只贴输入输出，看不出执行链路。 |
| Failure Cases | 集中定位失败样本和修复方向。 | 失败原因分类、风险等级、关联 Prompt 版本、期望输出、Mock 输出、修复建议。 | 你能把失败变成可处理的任务，而不是只看分数。 | 证明项目能沉淀失败案例和回归问题。 | Sentry Issue Details、Linear、Helicone Replay。 | 失败原因、风险标签、修复建议、本地 Review 边界。 | 变成普通错误列表，只有红色状态，没有分析价值。 |
| Dataset / Test Cases | 管理评测集和测试样本资产。 | 测试集列表、Case 输入、期望输出、标签、难度、评价维度、最近命中情况。 | 你知道 AI 评测需要稳定测试集，而不是临时问几个问题。 | 证明项目有数据支撑和测试样本资产。 | Supabase、Retool、promptfoo。 | 自建样本、评价维度、风险标签、Demo/Mock 标注。 | 变成普通数据表，像数据库管理页，缺少实验室感。 |

## 用户确认清单

下面 10 个问题，你只需要回答“满意 / 不满意”。如果有某项不满意，再补一句你想改成什么方向即可。

| # | 确认问题 | 你的回答 |
| --- | --- | --- |
| 1 | 是否接受“高级浅色界面 + 深色代码/Trace 区块”的 AI Developer Tool 风格？ | 满意 / 不满意 |
| 2 | 是否接受 Eval Dashboard 作为 README 首图？ | 满意 / 不满意 |
| 3 | 是否接受 Prompt Compare 做成左右对比 + Diff 高亮？ | 满意 / 不满意 |
| 4 | 是否接受 Trace Detail 做成时间线，展示 Input -> Mock Output -> Scoring -> Review？ | 满意 / 不满意 |
| 5 | 是否接受 Failure Cases 做成问题定位台，而不是普通错误表？ | 满意 / 不满意 |
| 6 | 是否接受 Dataset 页面做成测试实验室，而不是数据库表格？ | 满意 / 不满意 |
| 7 | 是否接受 README 截图顺序：Dashboard -> Batch Evaluation -> Trace Detail -> Prompt Studio -> Failure Cases -> Dataset？ | 满意 / 不满意 |
| 8 | 是否接受参考 Langfuse / LangSmith / Helicone / Linear / Supabase 的信息架构？ | 满意 / 不满意 |
| 9 | 是否接受不复制任何 Logo、商标、官网文案和截图，只学习结构和思路？ | 满意 / 不满意 |
| 10 | 是否确认第一阶段先做作品集展示效果，不追求生产系统复杂度？ | 满意 / 不满意 |

## 是否建议开工

### 1. 当前前端方向是否建议开工

建议开工。

原因是：方向已经比较清楚，目标不是“更好看的后台”，而是“更像 AI Eval / PromptOps / Developer Tool 的作品集控制台”。参考来源已经足够，边界也清楚：不接真实模型，不伪造数据，不复制第三方视觉资产。

### 2. 如果建议开工，第一阶段应该改哪些页面

第一阶段建议围绕 7 个核心页面改：

1. Eval Dashboard
2. Prompt Studio
3. Batch Evaluation
4. Output Compare
5. Trace / Run Detail
6. Failure Cases
7. Dataset / Test Cases

如果时间需要控制，优先级建议是：

1. Eval Dashboard：README 首图，最影响第一印象。
2. Batch Evaluation：证明批量评测能力。
3. Trace / Run Detail：证明可解释和可追溯。
4. Prompt Studio + Output Compare：证明 Prompt 版本管理和对比。
5. Failure Cases：证明失败定位能力。
6. Dataset / Test Cases：证明评测有数据支撑。

### 3. 如果不建议开工，还缺什么参考或决策

如果你不满意现在的方向，开工前需要补 3 个决策：

1. 你更想要浅色为主，还是深色为主。
2. README 首图到底选 Eval Dashboard，还是 Trace / Run Detail。
3. 第一阶段是完整做 7 页，还是先做 4 个最强展示页。

### 4. 开工前必须等待用户确认

在开始任何 `frontend/` 代码修改前，必须等待你明确回复：

```text
确认这个前端方向，可以开工
```

没有这句话之前，不修改前端代码，不修改后端业务代码，不把规划文档当成已经实现的功能。
