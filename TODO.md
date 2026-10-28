# TODO

本文件用于 Claude / Codex 本地接力。每轮只处理一个名称明确、范围可验收的任务；未被用户确认前，不自动进入下一项。

## Documentation

### docs-readme-showcase-v1 PromptOps Studio README 展示版整理

- 状态：已完成
- 目标：使用已验收的 8 张本地真实页面截图，整理适合 GitHub、简历与面试展示的 PromptOps Studio README。
- 结果：README 已按项目定位、Showcase、页面导览、Core Features、Workflow、Tech Stack、Local Run、Verification、Project Boundary 与 Resume Pitch 重组。
- 边界：保留 Local Demo、Mock Output、Rule-based Eval、无需 API Key、非生产环境、未接入真实模型与 LLM-as-Judge 未启用说明。
- 验收：已确认全部截图引用存在，并执行 `git diff --check`；本轮不重新构建、不重新截图、不修改前后端。

## Frontend

### frontend-batch-evaluation-visual-v2 批量评测工作台目标图重做

- 状态：已完成
- 目标：
  - 仅按 `docs/design_refs/promptops_batch_evaluation_target_v2.png` 重做 Batch Evaluation 页面视觉。
  - 在 `1440x810` 首屏内完整展示评测配置、核心指标、5 行样本评分矩阵、评测工作流、运行状态、执行时间线、高风险样本 TOP 3 与提供者状态。
  - 保持 Local Demo、Rule-based Eval、Mock Output、无需 API Key、非生产环境边界。
- 结果：
  - `BatchEvaluation.vue` 已重构为固定顶栏、左侧主工作区与右侧运行信息栏；底部评测工作流完整进入首屏。
  - 运行状态使用居中 80% 进度环，`96 / 120 完成` 与状态列表分栏展示；样本评分矩阵首屏固定显示 5 行。
  - 顶部状态统一为“本地演示 / 规则评测 / 无需 API Key / 环境：Local Demo / Dev User”。
  - `docs/images/batch-evaluation.png` 已由本项目本地 Vite 页面重新生成，尺寸 `1440x810`，`fullPage: false`。
  - 已执行 `npm run check:batch:16x9`、`npm run build`、`npm run check:showcase-pages` 与 `git diff --check`，均通过。
- 未做事项：
  - 未修改 `README.md`、`backend/`、其它页面组件或其它 7 张正式截图。
  - 未接入真实模型、真实 Provider、API Key、LLM-as-Judge 或生产数据；未自动 commit、未 push。

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

## 第二阶段：真实执行链路与前端升级

- 状态：已完成（Mock Agent 评测范围）
- 结果：26 条原创合成用例；前端真实 HTTP 触发同步执行；H2 文件库保存运行/逐条结果/Trace/JSON 报告；历史和报告跨服务重启可读；浏览器可查看失败并导出报告；支持同版本数据集运行比较。
- 实际演示运行：`82b67bd7-0647-4dc5-9272-80730765037a`，24/26 行为通过、2 失败、0 错误、0 跳过。失败 `agent-014` 和 `agent-015` 是当前 Mock Agent 的真实行为结果，保留用于后续修复。
- 验收：17 Maven 测试、5 pytest、Vue 类型检查/生产构建、npm audit 与 Playwright 全流程均通过；跨重启报告一致性证据和截图路径见 `docs/phase-2-acceptance.md`。
- CI：`.github/workflows/quality.yml` 已配置；未推送分支，因此远程 GitHub Actions 尚未运行。

### 后续项

- 接入可选真实 Agent / Model Provider：密钥仅通过环境变量，本地明确启用，不在默认 CI 触发付费调用。
- 建立 Prompt / Agent 版本适配后，再把当前同数据集 run-to-run 对比扩展为真正的 V1/V2 回归。
- 根据真实执行需求评估异步任务、鉴权/多租户、生产数据库、脱敏、审计与成本统计。
- 先定位并修复 `agent-014` 工具路由和 `agent-015` 重复调用问题，再保持固定断言进行回归。

## 2026-10-28 Phase 3 发布候选验收（本轮已完成）

- 状态：本轮范围已完成，工作区变更保留，未提交、未推送。
- 结果：Provider 抽象与本地 Stub 错误测试、数据集 1.0.2、运行快照/比较语义、前端 Agent Eval 控制台、报告和验收文档均已核验。
- 实际验收：Maven 29 项、pytest 7 项、前端类型检查/生产构建、npm audit 0 漏洞、Playwright 两次完整 26 条 Mock 运行、H2 跨进程重启一致性。
- 证据目录：outputs/agent-eval-release-candidate-20261028/；详细记录见 docs/phase-3-acceptance.md。
- 下一阶段候选项（不属于本轮）：真实模型样本与预算策略；不可变 Prompt/Agent 配置版本；异步队列和并发；鉴权/审计/生产部署。
- 边界：本轮未调用真实模型；Mock 通过率不是模型指标；远端 GitHub Actions 未运行。

### testdev-production-like-quality-v1 本地业务 API、监控告警、Soak 与负载验收

- 状态：已完成（用户明确要求补齐这些能力，并选择本地订单/退款业务场景）
- 目标：在现有 Spring Boot/Vue 工程中搭建隔离的本地订单支付/退款业务 API；增加业务集成测试；接入 Micrometer/Prometheus/Alertmanager；实现可配置长时间稳定性运行和可重复负载场景，并保留可核验报告。
- 验收范围：订单状态机、支付/退款幂等与权限场景有 HTTP 集成测试；Prometheus 可抓取业务/Agent Eval 技术指标；告警规则可校验，至少验证一次告警触发与恢复；Soak 默认 30 分钟且可安全中止；k6 运行渐进并发/到达率业务流程，结果有错误率、延迟分布和证据。
- 安全边界：业务 API 是本地合成系统，不能声称为外部真实生产业务；演示身份头不是真实认证；压测只针对本项目隔离服务和隔离 H2；监控端口只绑定本机，不配置外部通知或密钥。
- 不做：访问未经授权的公司系统、真实支付渠道、真实客户数据、生产环境压测或真实模型付费调用。
- 结果：Maven 36 项通过、Soak 脚本单测 4 项通过；Prometheus/Alertmanager 配置校验通过，本地告警完成 firing/resolved 演练；k6 四档负载退出码 0，8,103 个工作流/40,515 个请求/81,030 项检查全通过，0 HTTP/工作流失败、0 dropped iteration。
- Soak：首次运行因 Docker Desktop 停止而中断（49/49 轮、294 请求，无最终退出码），单独保留；第二次独立运行状态 `COMPLETED`、运行器退出码 0、耗时 1,799.211 秒，60/60 轮通过、0 失败，300 个业务请求 + 60 次健康检查，共 360 个请求。
- 验收报告：`outputs/testdev-production-like-20261028/production-like-final-acceptance.md`；该目录包含原始 Soak/k6 JSON、测试日志、告警事件、H2 卷证据、最终链接检查和前序阶段浏览器截图。
- 安全边界：仅本地隔离合成订单/支付/退款 API 与 H2；无真实生产系统、支付、客户数据、外部告警、真实模型或生产 SLO 验证；未推送、未提交。
- 结束条件：已满足。本轮命令、退出码、测试计数、告警样本、完整 Soak/负载 JSON 和 H2 数据路径均已写入本轮验收报告；未运行的外部/企业检查明确列为未验证。

## 2026-10-28 通用测试开发岗位证据加固（本轮）

### testdev-k6-scenario-gates-v1 分场景负载门禁与报告

- 状态：已完成
- 开始 HEAD：`d608cc28a2fae8246ad33712d31199185c538639`；开始分支：`feat/agent-evaluation-platform`。
- 开始时工作区：有大量前序未提交改动；本轮保留，不做 reset、覆盖或清理。
- 目标：让四档 k6 负载各自校验计划工作流数、零掉队、零 HTTP/业务失败和全量检查通过，并生成带 Run ID、逐场景结果、阈值状态和边界说明的 JSON/Markdown 报告。
- 范围：仅修改 k6 负载脚本，新增标准库报告汇总器及单测，并为本轮实际运行生成独立证据；不改业务服务、容量阈值或真实 Provider。
- 验收：4 个汇总器单测通过；完整 k6 运行退出码 0；逐场景和汇总门禁全部通过；本轮 Python API/dataset/runner 测试 26 通过；Maven 36 通过；前端类型检查与生产构建通过；npm audit 0 漏洞；`git diff --check` 退出码 0。
- 实际数据：8,103 workflows、40,515 HTTP requests、81,030 checks；四档为 301/601/1,200/6,001 workflows；0 HTTP/业务失败、0 dropped；活跃 VU 峰值 1，`vus_max=120` 仅为资源容量上限。
- 报告：`docs/reports/business-load-k6-2026-10-28T14-41-32-869Z-b09f9fc3.json` 及同前缀 `-summary.json`/`-summary.md`；完整运行证据和 JD 矩阵见 `docs/test-development-acceptance.md`。
- 边界：所有负载和报告均为本地隔离合成服务数据；合并 p95 是本机观察值，无真实生产系统、生产容量、SLO 或真实模型结论。
