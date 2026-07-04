# TODO

本文件用于 Claude / Codex 本地接力。每轮只处理一个名称明确、范围可验收的任务；未被用户确认前，不自动进入下一项。

## Frontend

### frontend-showcase-pages-v1 多页面展示页与截图验收收口

- 状态：已完成
- 目标：
  - 将 Batch Evaluation、Trace Detail、Output Compare、Prompt Studio、Failure Cases、Dataset View、System Boundary 七个展示页统一纳入本地页面检查。
  - 使用真实本地运行页面生成 `1440x810` 首屏截图，避免把参考图或第三方截图当作项目截图。
  - 收口 Batch Evaluation 首屏密度，保证右侧 Run State 与主要评测信息在 16:9 viewport 内可读、不重叠。
- 结果：
  - 新增统一脚本 `npm run check:showcase-pages`，串行执行七个页面检查脚本。
  - 已生成/刷新项目截图：`docs/images/eval-dashboard.png`、`docs/images/batch-evaluation.png`、`docs/images/trace-detail.png`、`docs/images/output-compare.png`、`docs/images/prompt-studio.png`、`docs/images/failure-cases.png`、`docs/images/dataset.png`、`docs/images/system-boundary.png`。
  - 已目检 `docs/images/batch-evaluation.png` 与 `docs/images/failure-cases.png`，首屏没有明显文字重叠或模块裁切。
  - 已重新执行 `npm run build`、`npm run check:dashboard:16x9`、`npm run check:batch:16x9`、`npm run check:trace:16x9`、`npm run check:showcase-pages` 与 `git diff --check`，均通过。
  - `npm run check:showcase-pages` 已覆盖 Output Compare、Prompt Studio、Failure Cases、Dataset 与 System Boundary 页面检查。
- 未做事项：
  - 未修改后端业务逻辑、`.env`、API Key 或生产配置。
  - 未修改 `README.md`；未接入真实 LLM、真实 Provider、LLM-as-Judge、生产数据或任何密钥。

### frontend-trace-detail-v1 运行追踪详情页面前端落地

- 状态：已完成
- 目标：
  - 在已有 PromptOps Studio 视觉系统内落地 Trace / Run Detail / 运行追踪详情页面。
  - 在 `1440x810` 首屏展示当前运行上下文、运行摘要、输入与 Prompt 构建、输出与 Schema 对比、规则评分、失败原因与修复建议、运行 Trace、人工复核区、Provider 状态与边界和运行追踪链路。
  - 使用结构化本地演示数据，并保持 Local Demo、Mock Output、Rule-based Eval、无需 API Key、非生产环境边界。
- 结果：
  - 新增 `TraceDetail.vue` 与 `traceDetailMockData.ts`，可通过 `?view=trace` 或左侧“运行追踪”导航进入。
  - “评测总览”“批量评测”“运行追踪”三页左侧导航可互相切换，当前页面有正确高亮。
  - Trace 页面围绕 `run_1022` / `case_093` 展示缺少 `confidence` 字段的 Schema 校验失败、规则评分、修复建议和待人工复核闭环。
  - 新增 `npm run check:trace:16x9`，验证 Dashboard / Batch 到 Trace 的导航、无首屏滚动、13 个核心模块完整入镜、7 个 Trace 节点未裁切与浏览器无错误。
  - 生成 `.local/promptops-trace-detail-16x9-check.png` 和本项目真实运行截图 `docs/images/trace-detail.png`，尺寸均为 `1440x810`，截图使用 `fullPage: false`。
  - 已执行 `npm run build`、`npm run check:dashboard:16x9`、`npm run check:batch:16x9`、`npm run check:trace:16x9`，均通过。
- 未做事项：
  - 未修改后端业务逻辑或 `README.md`。
  - 未接入真实 LLM、真实 Provider、API Key、LLM-as-Judge 或生产数据。
  - 未把 image2 目标图当作正式项目截图；正式截图来自本项目本地真实运行页面。

### frontend-batch-evaluation-v1 批量评测工作台视觉升级

- 状态：已完成
- 目标：
  - 在第一阶段 PromptOps Studio 视觉系统内落地 Batch Evaluation / 批量评测工作台。
  - 在 `1440x810` 首屏展示评测配置、批量运行、核心指标、Score Matrix、Eval Run 时间线、失败样本分析、Provider 边界和批量评测闭环。
  - 使用结构化本地演示数据，并保持 Local Demo、Mock Output、Rule-based Eval、无需 API Key 边界。
- 结果：
  - 新增 `BatchEvaluation.vue` 与 `batchEvaluationMockData.ts`，默认通过 `?view=batch` 或左侧“批量评测”导航进入。
  - Score Matrix 展示 7 条代表性 Case，默认选中 `case_093`，切换 Case 会联动失败原因、修复建议与 Schema 对比。
  - 新增 `npm run check:batch:16x9`，验证 Dashboard / Batch 双向导航、Case 联动、无首屏滚动、核心模块完整与浏览器无错误。
  - 生成 `.local/promptops-batch-evaluation-16x9-check.png` 和本项目真实运行截图 `docs/images/batch-evaluation.png`，尺寸均为 `1440x810`，截图使用 `fullPage: false`。
  - 已执行 `npm run build`、`npm run check:dashboard:16x9`、`npm run check:batch:16x9`，均通过。
- 未做事项：
  - 未修改后端业务逻辑或 `README.md`。
  - 未接入真实 LLM、真实 Provider、API Key、LLM-as-Judge 或生产数据。
  - 未把 image2 目标图或第三方参考截图作为正式项目截图。

### frontend-dashboard-16x9-layout-fix 评测总览 16:9 首屏布局修复

- 状态：已完成
- 目标：
  - 将 Eval Dashboard 调整为适合 `1440x810` README 首图的桌面端两列布局。
  - 在单个首屏内保留标题、实验信息、核心指标、趋势、最近运行、Trace、失败归因、Provider 状态和评测闭环。
  - 保持右侧运行 Trace 为主视觉，并保留 Local Demo、Rule-based Eval、无需 API Key 等能力边界。
- 结果：
  - 桌面短视口使用约 `58% / 42%` 的主列与洞察列布局，压缩卡片和表格高度但不删除核心模块。
  - 最近评测运行固定展示 5 行，Trace 的 7 个节点均完整显示，PromptOps 评测闭环改为紧凑横向 stepper。
  - 新增 `npm run check:dashboard:16x9`，使用 `1440x810` viewport 和 `fullPage: false` 生成 `.local/promptops-dashboard-overview-16x9-check.png`。
  - 已执行 `npm run build` 和 `npm run check:dashboard:16x9`，均通过。
- 未做事项：
  - 未修改后端业务逻辑、`README.md`、`docs/images/` 或第三方参考截图。
  - 未接入真实 API、API Key、模型 Provider 或生产数据。

### frontend-dashboard-visual-v1 评测总览首页视觉升级

- 状态：已完成
- 目标：
  - 基于已确认目标图，将默认首页升级为中文企业级 AI Eval / PromptOps Developer Tool 控制台。
  - 首页聚焦 Eval Dashboard / 评测总览，突出右侧深色运行 Trace 主视觉。
  - 围绕 `run_1022` 展示当前评测实验、核心指标、评测健康度、最近运行、失败归因、Provider 状态和 PromptOps 评测闭环。
  - 保留 Local Demo、Mock Output、Rule-based Eval、无需 API Key、非生产环境等边界。
- 结果：
  - 新增 `EvalDashboard.vue`，默认 `overview` 视图进入新的评测总览首页。
  - 新增 `evalDashboardMockData.ts`，集中存放本轮首页使用的结构化前端本地演示数据。
  - `App.vue` 保留原有后端 API 加载和其它视图，只在 `overview` 使用新首页组件。
  - `styles.css` 新增 `dashboard-mode` 容器覆盖，让首页可以使用全宽控制台布局。
  - 已执行 `npm run build`，通过。
  - 已启动本地 demo 后端与 Vite，用 Playwright 检查桌面与移动端首页无横向溢出、无浏览器错误，并保存本地检查截图到 `.local/`。
- 未做事项：
  - 未修改后端业务逻辑。
  - 未修改 `README.md`。
  - 未运行 `npm run screenshots`，避免覆盖 `docs/images` 现有截图资产。
  - 未接入真实模型、真实 API Key、真实 Provider 或 LLM-as-Judge。

### frontend-redesign-v1 前端页面重设计

- 状态：已完成
- 目标：
  - 将前端从普通 demo 页面重设计为简历 / GitHub 展示级 PromptOps 控制台。
  - 保留真实 fetch 后端 API、Prompt 选择、Eval Run 触发、Trace 查询、Version Comparison 和前端 Review 演示状态。
  - 保持 Local Demo、Mock Output、Rule-based Eval、No real LLM connected、No agent runtime 等诚实边界。
- 结果：
  - 顶部产品名更新为 `PromptOps Evaluation Lab`，导航改为 Overview / Eval Run / Prompt Versions / Trace Review。
  - 新增统一 CSS tokens：`--bg` / `--surface` / `--surface-soft` / `--border` / `--text` / `--muted` / `--primary` / `--success` / `--warning` / `--danger`。
  - 新增 `MetricCard` 与 `StatusBadge` 前端展示组件。
  - Eval Run 页面补充 selected case 的 Rule Audit 面板，并明确展示 `RuleEvaluator` / `MockOutputGenerator` / Simulated latency。
  - Prompt Versions 页面补充 `variables_json` 使用标准 JSON 数组存储的说明。
  - Trace Review 页面明确写出 `Review state is frontend-only demo state`。
  - 已运行 `npm run typecheck` 和 `npm run build`，均通过。
- 未做事项：
  - 未运行 `npm install`。
  - 未启动后端、前端、数据库或 Docker。
  - 未运行 `npm run screenshots`，因为现有截图脚本会启动服务并覆盖 `docs/images` 截图资产。

## P0

### P0-1 项目定位去虚夸 + 文档基线补齐

- 状态：已完成
- 目标：
  - 创建 `TODO.md`
  - 填写 `HANDOFF.md` 本轮审查记录
  - 修正 `README.md` 中可能造成误解的 AI Agent 表述
  - 修复 `AGENTS.md` 中对不存在文档的引用
  - 确保 `docs/` 中各文档边界说明与源码一致
- 涉及文件：
  - `README.md`
  - `HANDOFF.md`
  - `TODO.md`
  - `AGENTS.md`
  - `docs/*.md`
- 禁止修改：
  - Java 源码
  - Vue 源码
  - SQL
  - `pom.xml`
  - `package.json` / `package-lock.json`
  - 配置文件
  - 测试文件

### P0-2 修复 variables_json 命名与真实 JSON 存储问题

- 状态：已完成
- 目标：
  - 将 `variables_json` 字段改为存储真正的 JSON 数组，或重命名字段以匹配当前真实格式，降低面试风险。
- 结果：
  - 采用方案 A，保留字段名并改为标准 JSON 数组存储。
  - Java 侧使用 Jackson 序列化 / 反序列化。
  - 前端接口 `variables` 数组结构保持不变。

## P1

### P1-1 补全局异常处理器

- 状态：已完成
- 结果：
  - 新增 `GlobalExceptionHandler`，统一处理 `ResponseStatusException`、参数校验异常和兜底异常。
  - API 错误响应统一为 `code` / `message` / `timestamp`，参数校验错误额外返回字段级 `errors`。
  - 后端测试基线更新为 13 个。

### P1-2 增加分页支持

- 状态：待执行

### P1-3 补关键日志点

- 状态：已完成

## P2

### P2-1 补 Springdoc OpenAPI 接口文档

- 状态：已完成

### P2-2 README / GitHub 展示强化

- 状态：已完成
