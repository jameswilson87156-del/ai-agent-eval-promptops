# TODO

本文件用于 Claude / Codex 本地接力。每轮只处理一个名称明确、范围可验收的任务；未被用户确认前，不自动进入下一项。

## Frontend

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
