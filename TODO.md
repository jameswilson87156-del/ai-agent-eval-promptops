# TODO

本文件用于 Claude / Codex 本地接力。每轮只处理一个名称明确、范围可验收的任务；未被用户确认前，不自动进入下一项。

## Frontend

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
