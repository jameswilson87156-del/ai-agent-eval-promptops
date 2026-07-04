# PromptOps Studio 前端 Showcase Design

本文件是下一阶段前端改造的设计规格，不是代码实现记录。本轮没有修改 `frontend/`。

## 1. 视觉定位

目标定位：AI Developer Tool Console / Prompt Evaluation Lab / EvalOps Dashboard。

建议方向：高级浅色开发者控制台，混合深色代码块和 Trace 区块。整体要比普通管理后台更像开发者工具，比纯 AI 大屏更克制可信。

关键词：

- local-rule baseline
- Prompt version diff
- Eval run matrix
- Trace timeline
- Failure triage
- Human Review queue
- Dataset lab

## 2. 参考来源

主要参考来自：

- Langfuse：AI engineering 平台的信息架构。
- LangSmith：Dataset、Evaluator、Experiment 的评测流程。
- promptfoo：规则/断言/结果矩阵。
- Grafana / Datadog：Trace timeline 和 span detail。
- Sentry / Linear：Failure Case 的 issue triage。
- Supabase / Vercel：开发者控制台的清晰层级。

所有参考只用于方向，不复制第三方视觉资产、截图、Logo、文案或代码。

## 3. 色彩系统

建议第一阶段使用浅色为主：

| 角色 | 建议 | 用途 |
| --- | --- | --- |
| Background | `#f7f8fb` 附近 | 页面背景 |
| Surface | `#ffffff` | 面板、表格、列表 |
| Surface Soft | `#f1f4f8` | 次级区块、筛选条 |
| Text | 深灰接近 `#111827` | 正文 |
| Muted | 灰蓝 `#64748b` | 辅助文本 |
| Border | 低饱和灰 `#d9e0ea` | 分割线 |
| Primary | 蓝绿或青蓝，不用廉价亮蓝 | 主按钮、当前状态 |
| Success | 绿色 | pass |
| Warning | 琥珀 | review / regression |
| Danger | 红色 | fail / critical |
| Code Surface | `#101828` 附近 | Prompt diff、Mock output、Trace payload |

约束：

- 避免整站蓝白后台感。
- 避免紫蓝霓虹玻璃拟态。
- 避免大面积渐变球、装饰 blob。
- 状态色必须语义清晰。

## 4. 字体层级

| 层级 | 用途 | 建议 |
| --- | --- | --- |
| Page title | 页面主标题 | 28-34px，紧凑，不做夸张 hero |
| Section title | 面板标题 | 16-20px |
| Table / list primary | Case、Prompt、Run 名称 | 14-15px |
| Metadata | Run key、Case key、status | 12-13px |
| Code / prompt | Prompt、JSON、Mock output | 等宽字体，13px 左右 |

要求：

- 中文和英文混排要留足行高。
- 按钮内文字不能挤压。
- 代码块保持可读，不使用过小字号。

## 5. 卡片样式

- 卡片半径建议 8px 或更小，避免过度圆角。
- 页面 section 不做层层卡片嵌套。
- 卡片只用于独立信息单元：指标卡、Case 卡、Trace stage、Modal。
- 表格和列表优先使用清晰行分隔，而不是每行一张厚卡。
- hover 只做轻微背景/边框变化，不做夸张浮动。

## 6. 页面布局

全局结构：

- 顶部：产品名、Demo 边界、运行 Mock Eval 主操作。
- 左侧或顶部导航：Dashboard、Prompt Studio、Batch Evaluation、Output Compare、Trace Detail、Failure Cases、Dataset/Test Cases。
- 主内容：根据当前工作流分两到三栏。
- 右侧详情：当前 Case、Rule Audit、Trace Summary、Review Decision。

响应式：

- 桌面：优先 12 栅格，两栏/三栏。
- 1366px：不横向溢出，关键表格可压缩列。
- 390px：纵向堆叠，表格变为列表卡片。

## 7. 导航结构

第一阶段建议导航：

1. Dashboard
2. Prompt Studio
3. Batch Evaluation
4. Output Compare
5. Trace Detail
6. Failure Cases
7. Dataset Lab

导航文案保留英文核心词，减少翻译误差。页面内说明使用中文。

## 8. 核心页面草图说明

### Eval Dashboard

布局：

- 顶部一行：项目边界提示 + Run Mock Eval。
- 左侧大区：Evaluation Health，包括 Total Evaluations、Pass Rate、Avg Score、Regression Alerts。
- 中间：Recent Eval Runs 表格。
- 右侧：Review Queue 和 Failure Reason Top 3。
- 底部：Score Trend + Prompt Version Snapshot。

截图重点：

- 一眼看出这是 Prompt/Eval/Trace 项目。
- 不要只像普通运营后台。

### Prompt Studio

布局：

- 左：Prompt 模板列表，含状态、场景、最新分数。
- 中：Prompt 编辑/只读展示区，含变量、输出格式。
- 右：Version Timeline、Scoring Rules、Recent Runs。
- 底：Prompt Diff。

截图重点：

- Prompt 管理能力和版本对比能力。
- 代码审查感。

### Batch Evaluation

布局：

- 顶：Dataset、Prompt Version、Evaluator、Run Mode 配置条。
- 中：批量运行状态、进度条、Pass Rate、失败样本。
- 下：结果矩阵，行是 Case，列是规则/维度。

截图重点：

- 批量评测是本项目核心，不是单条 Prompt demo。
- 第一阶段必须标注 local-rule baseline。

### Output Compare

布局：

- 左右两栏：Prompt v1 输出 / Prompt v2 输出。
- 中间或顶部：score delta、format status、risk notice、recommended version。
- 底部：差异高亮和失败项说明。

截图重点：

- 版本对比可解释。
- 推荐结果来自规则，不是 AI 判断。

### Trace / Run Detail

布局：

- 顶：Run key、Case key、状态、风险等级、模拟耗时。
- 主：Timeline，阶段为 Input、Prompt Build、MockOutputGenerator、Output、RuleEvaluator、Human Review。
- 右：Stage Detail，展示摘要、payload、失败原因。

截图重点：

- 让面试官看到“可追溯”和“可解释”。

### Failure Cases

布局：

- 左：失败样本列表，按原因、风险、Prompt 版本筛选。
- 中：失败详情，含输入、期望、Mock 输出。
- 右：归因、修复建议、回归状态。

截图重点：

- 像问题定位工具，不像普通表格。

### Dataset / Test Cases

布局：

- 左：Dataset 列表。
- 中：Case 表格/列表，含标签、难度、风险、维度。
- 右：Case Detail，含输入、期望输出、规则。

截图重点：

- 展示测试集资产和评测维度。

## 9. 页面信息模块

| 页面 | 必须模块 | 可选模块 |
| --- | --- | --- |
| Dashboard | Total Evaluations、Pass Rate、Avg Score、Regression Alerts、Recent Runs | Score Trend、Provider Status 占位 |
| Prompt Studio | Prompt 列表、版本、变量、输出格式、Prompt Diff | Author、Change note |
| Batch Evaluation | Dataset、Prompt Version、local-rule baseline、进度、结果矩阵 | 模型 Provider 占位但标注未接入 |
| Output Compare | v1/v2 输出、得分差异、规则失败、推荐版本 | Diff filter |
| Trace Detail | Input、Prompt Build、Mock Output、Scoring、Review | Error/Fallback 占位 |
| Failure Cases | 失败列表、原因分类、期望/实际、修复建议 | 回归通过状态 |
| Dataset Lab | Dataset、Case、标签、风险、规则 | 采样统计 |

## 10. README 截图展示顺序

建议顺序：

1. Eval Dashboard：总览项目价值。
2. Batch Evaluation：说明批量评测闭环。
3. Trace / Run Detail：说明可解释与可追溯。
4. Prompt Studio / Prompt Compare：说明版本管理。
5. Failure Cases：说明失败定位与回归思路。
6. Dataset / Test Cases：说明数据集和样本资产。

## 11. 借鉴与原创边界

可借鉴：

- Langfuse 的 AI engineering 结构。
- promptfoo 的测试/断言结果思路。
- Grafana / Datadog 的 Trace 时间轴信息结构。
- Sentry / Linear 的 failure triage 层级。
- Supabase / Vercel 的开发者控制台清晰度。

必须原创：

- 产品文案、页面命名、指标口径。
- PromptOps Studio 的本地 Demo 边界提示。
- 数据样本、规则、失败原因、截图。
- Vue 组件实现和 CSS。

绝对不能照搬：

- 第三方 Logo、商标、截图、官网文案、UI 源码。
- 第三方产品的具体品牌色和页面布局。
- 未实现的 Provider、真实用户、生产监控能力。

## 12. 第一阶段最小可实现页面范围

用户确认后，第一阶段建议只实现：

1. Eval Dashboard
2. Prompt Studio
3. Batch Evaluation
4. Trace / Run Detail
5. Failure Cases
6. Dataset / Test Cases

Output Compare 可以作为 Prompt Studio 或 Batch Evaluation 内的重点模块，不必单独做重页面。

## 13. 第二阶段可增强范围

- 真实 Provider 抽象，但不硬编码 API Key。
- LLM-as-Judge 可选开关。
- token / cost / latency 采集。
- Eval Run 异步队列。
- Review 持久化、状态机、审计日志。
- 数据集版本锁定。
- 前端 Playwright 截图和 Lighthouse 记录。

## 14. 前端验收标准

第一阶段前端验收应包含：

- `npm run typecheck` 通过。
- `npm run build` 通过。
- 浏览器打开无 console/page error。
- 1366px 和 390px 无横向溢出。
- 页面明确显示 Local Demo、Mock Output、Rule-based Eval、No real LLM。
- 不出现真实 Provider 调用或 API Key。
- README 截图只能由真实本地页面生成。
- 所有指标来自后端 API、Demo 数据或明确标注为规划占位。
