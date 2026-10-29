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

## 2026-10-29 Agent Eval 数据覆盖与模型评测协议

### agent-eval-mock-coverage-v1.1.0 Mock 用例覆盖审计与增补

- 状态：已完成。
- 审计基线：数据集 1.0.2 有 26 条；单轮普通任务、输入边界、工具路由/参数、故障注入、安全拒绝和成功 Trace 已覆盖。缺少授权工具正向执行、allow list 失败关闭、参数枚举/空白/额外字段、JSON 字段类型、Unicode/长输入 Trace 上限和 Canary 不回显。
- 变更：新增 agent-027–agent-038 共 12 条非重复场景；版本提升到 1.1.0；原 26 条断言保留。新增 `json_schema` 和 `max_length` 断言操作；工具 Mock 与 Provider 本地 stub 对额外参数 fail closed。
- 实测：隔离 Mock 运行 `1cf0c464-137c-4974-8b88-33eca6981052`，38/38 执行、36 行为通过、2 行为失败（agent-014/015）、0 执行错误、0 跳过，质量门禁 failed。详情 API 与 JSON 报告 API 内容相同。
- 验收：`backend/` 下 `mvn test` 37 通过；`PROMPTOPS_API_URL=http://127.0.0.1:28084 py -3.12 -m pytest backend/tests/test_agent_eval_api.py -q` 7 通过；generator 完全一致；`npm run check:agent-eval:live` 在 5181 端口通过，38 条运行可重复、报告持久化一致、桌面/移动无溢出、浏览器错误 0。
- 证据：`outputs/agent-eval-v1.1.0-verification-20261029-01/`，包含 Mock JSON 报告、摘要、pytest 日志及浏览器截图。人工评分过的模型结果仍为 0；XEM 探索运行见下节。

### model-eval-protocol-v0.1.0 真实模型独立评分与复核协议

- 状态：协议格式完成。
- 新增：`docs/model-evaluation-protocol.md`、`evals/model-eval/dataset.v0.1.0.json`、Case Schema 和 Review Schema。定义 0–4 分维度、不可被平均分抵消的安全硬门禁、盲审、双人复核、分歧仲裁、来源与模型配置 provenance。
- 划分：规划 40 条，development/validation/sealed_test 目标 24/8/8；按 scenario_family 分组分层，holdout 不参与提示词/阈值调优。首批 3 条模型草案另记在 `model-eval-xem-budgeted-pilot-v0.1.0`，不属于已审核 Benchmark。
- 校验：Draft 2020-12 Case/Review JSON Schema 校验通过；未安装 DeepEval。

### model-eval-xem-budgeted-pilot-v0.1.0 XEM 有界模型评测试跑

- 状态：首次鉴权尝试曾被 HTTP 401 阻塞；之后用户配置国模组 Key 并完成 2 个模型、6 次真实推理请求。账单金额未知；v0.1.0 的原因码断言存在提示未定义问题，修订版 v0.1.1 待复跑。
- 范围：`evals/model-eval/` 独立于 Mock Harness；3 条草案来源于本地合成退款规则，未批准、需业务复核；目标为 GLM、DeepSeek、Gemini 三个截图中列出的 Flash 模型；不修改 Agent Eval UI/API/H2，不改固定 Mock 集。
- 安全边界：首轮预算上限 ¥5；最多 3 个模型、9 次请求、每次最多 96 输出 Token、无重试、无工具；Key 只从环境变量读取。该本地限额不是提供商账单硬上限，费用只能以账号账单核对为准。
- 结果：新增标准库 OpenAI-compatible CLI、只读模型目录、分组 Key 路由、用量字段捕获、确定性检查与 JSON 报告。用户本机 `--list-models` 返回国模组 15 个可见模型。模型运行 `model-pilot-5f1f25db-b032-4e4b-938b-e169c074acf0` 完成 6/6 请求、0 网关错误，Provider 报告 843 输入 / 294 输出 / 1,137 总 Token，未返回费用字段。原始 v0.1.0 汇总为 2 pass/4 fail，但 4 项全因未在提示中声明的原因码枚举差异，不形成可用的模型通过率。新增 v0.1.1 显式规定原因码，保持旧数据与报告不变，等待复跑与人工评分。
- 安全处理：初始版本误将唯一配置的 `OPENAI_API_KEY` 回退用于 XEM Authorization；密钥值已发往 XEM，但没有打印或写入报告。现已修复为只读取 `PROMPTOPS_PROVIDER_API_KEY`。若该环境变量保存的是官方 OpenAI Key，应在官方控制台撤销并轮换。
- 证据：首次 401 报告 `outputs/model-eval-pilot-20261029/20261029T081928Z-model-pilot-56666912-51c9-44b9-9743-c9f6cbe4a007.json`；6-call 真实运行报告 `outputs/model-eval-pilot-20261029/20261029T085741Z-model-pilot-5f1f25db-b032-4e4b-938b-e169c074acf0.json`。错误响应体和密钥均未保存。
- 继续条件：复跑 v0.1.1 前先核对 XEM 账单及 Key 额度；用户无需提供密钥。成功复跑后仍须人工复核样本/输出，才能报告初步模型表现。

### model-eval-v0.2.0-data-matrix-20261029

- 状态：数据与 Runner 本地验收完成；已确认 glm-5.1 在 XEM 返回 `model_not_found`，从后续运行排除；下一步对其余 14 个目录模型逐个做单请求路由扫描。
- 当前基线：分支 `chore/update-actions-runtime-20261029`，开始 HEAD `2f89849a1f268e978e1f8583bc537dcbdd2013d6`；工作区已有多项未提交改动，本轮保留全部现有文件，不提交、不推送。
- 数据审计：40 条本地合成退款策略样例，unique case ID=40、scenario family=40、完整对话提示=40，development/validation/sealed_test candidate=24/8/8，approved=0；15 个目录可见国模 ID 映射 guomodel。测试范围仅文本 JSON 退款规则，不是通用 Agent 工具调用 Benchmark；sealed_test 仍可见。
- 改进：同步数据生成器与已保存 JSON 的执行计划；将 model-037 改为带 assistant 先前判断和 user 更正的消息序列；新增 `--case-ids` 代表样本选择及 split/ID 校验；严格 JSON 解析拒绝 NaN/Infinity 和重复对象键；报告区分执行状态与质量门禁，断言失败保留报告并返回非零；输出预算字段改为 `declared_budget_yuan`，避免误称授权或硬上限。
- 运行计划更新：目录列出 15 个模型，但 glm-5.1 已在 XEM 返回 `model_not_found`，不再重试。先对其余 14 个逐模型单 case 扫描；若全部可用，再跑 3 case × 14 模型 = 42 次数据契约预检；开发集重复批次最高为 24 × 14 × 3 = 1,008 次。重复只测同条件稳定性，不能当成额外独立样本。
- 本地验收：`py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q`：退出码 0，28 passed/0 failed/0 skipped；Python 编译退出码 0；数据生成器退出码 0 且 `dataset_unchanged=true`；Draft 2020-12 Case Schema：40 条、0 错误；CLI `--help` 退出码 0，含 `--case-ids`；`git diff --check` 退出码 0，仅有 Windows 行尾转换提示。
- 外部模型调用：用户运行 v0.2.0 路由预检 Run ID `model-eval-dc8eb2a3-7889-419f-8cc1-ff73b9936318`，请求计划 45 次、实际发送 3 次；前两次成功并通过断言，第三次 glm-5.1 返回 HTTP 400，Runner 停止，剩余 42 次未发出。`quality_gate=failed` 是执行不完整/网关错误，不是模型断言失败。随后单请求复验 Run ID `model-eval-b3d7ec3e-1b49-4435-9961-b01ce778fe87` 确认 `code=model_not_found; message=unknown provider for model glm-5.1`，无模型输出/usage，glm-5.1 从后续运行排除。两份报告费用均未返回，实付金额未知。
- 失败归因改进：推理 Runner 对非鉴权 HTTP 错误最多读取 4 KiB，并只从 JSON 提取 `type` / `code` / `message`，做密钥脱敏和 300 字符截断后写入可选 `error_detail`；401/403 响应体仍不读取，原始错误体不保存。单请求 glm-5.1 复验命令见 `docs/model-evaluation-protocol.md`。
- 本地诊断/route scan 回归：`py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q` 退出码 0，32 passed、0 failed、0 skipped；`py_compile` 退出码 0；生成器 `dataset_unchanged=true`、40 条按 24/8/8；Draft 2020-12 Schema 40 条、0 错误；CLI help 显示 `--continue-on-model-not-found`；`git diff --check` 退出码 0（仅 LF-to-CRLF 提示）。证据在 `../../../outputs/model-eval-v0.2.0-route-scan-readiness-20261029/`。
- 证据目录：`outputs/model-eval-v0.2.0-preflight-20261029/`，包含数据更新前备份、候选数据、pytest/py_compile/schema/generator/CLI/git 检查日志与覆盖审计；验收汇总见同目录 `acceptance.md`。
- 未完成：用户尚未执行其余 14 模型路由扫描；多场景预检与批量评测尚未运行。若 14 个模型均可用，计划是 42 次代表集预检及 1,008 次 development 重复；XEM 账单核对、40 条 gold 人工审批、盲测集私有托管、Agent 工具调用模型评估和前端模型历史集成也未完成。当前不能给 v0.2.0 通过率或模型排名。

### model-eval-incomplete-output-attribution-v1 截断输出失败归因

- 状态：已完成
- 目标：让 finish_reason=length 的调用显示为“请求成功、评测未完成”，不把被截断的空输出计入确定性业务失败或模型通过率。
- 变更：报告新增 evaluation_status、deterministic_evaluated_count 与 incomplete_output_count；质量门禁对不完整输出继续失败；完整模型调用记录有限响应形状诊断字段，绝不保留 reasoning_content 文本。CLI 摘要单列未完成输出。
- 根因证据：hy4-preview Run ID model-eval-0b02103f-5886-4911-9e4b-c37425b0a973 收到同名模型响应，无网关错误，Provider usage 报告 2,048 completion tokens，finish_reason=length，raw_output 为空。4,096 Token 命令以中文顿号加 py 开头而未执行，不能作为复测结果。
- 验收：先新增两个回归预期并观察到 2 项失败；完成实现后 py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q 为 36 passed、0 failed。真实 XEM 复测未由 Codex 发起；现有 Key 仅在用户 PowerShell 会话中。
- 后续校准：用户实际执行 hy4-preview × model-001 × 4,096 Token 一次，1/1 通过，finish_reason=stop；Provider usage 为 2,108 completion tokens，报告观测到分离的 reasoning_content 字段（只保存长度）。该结果只证明本条样例完成，不等于 Benchmark。

### model-eval-batch-timeout-continuation-v1 多用例超时后继续

- 状态：已完成
- 目标：避免单个网关超时让同一有界批次中后续用例都不运行，同时不自动重试超时请求。
- 变更：`--continue-on-gateway-timeout` 现可用于一个重复轮次的多用例/模型运行；超时记录为 error 后继续下一个计划组合。`--continue-on-model-not-found` 仍限制在单用例路由扫描。
- TDD 验收：新增的多用例 timeout continuation 测试先按预期失败；实现后定向测试 4 passed，全量 `py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q` 为 37 passed；`py_compile` 退出码 0；CLI help 退出码 0。未发起真实模型请求。
- 边界：超时结果没有 Provider usage/费用信息；continue 只影响是否发送后续计划请求，不代表超时请求未计费。

### model-eval-hy4-reasoning-budget-v1 HY4 长推理与输出上限校准

- 状态：HY4 默认推理模式的输出预算校准完成；模型对比与人工复核仍未完成
- 结果：model-004/model-022 在 4,096 上限下都截断；将上限提高到 8,192 后，两条各执行一次均完成并通过确定性检查，Provider 报告 completion token 分别为 4,440/6,007，延迟约 150.9/161.6 秒。总计 2/2 个样例、10 项确定性检查通过；费用未报告。
- 限制：这是两条 exploratory draft development case 的单次观察，人工复核 pending、`benchmark_eligible=false`。禁止将其写成模型准确率或通用 Benchmark。
- 已完成首轮交叉预检：DeepSeek V4 Pro / V4.1 Flash 在 model-004 与 model-022 上 4/4 deterministic pass、20 项断言通过；Provider 未返回费用。HY4 同两条在 8192 上限下各 pass 一次。
- 限制：总计只有两条 draft development 样例、各模型各一次；人工复核 pending、`benchmark_eligible=false`。HY4 与 DeepSeek 的 token/延迟差异仅为这四次请求的观测，不是模型排名。
- 已完成 7 条分层开发样例 × 2 个 DeepSeek 模型：14/14 请求完整且通过，70 项 deterministic checks 通过，0 gateway errors/incomplete；覆盖 7/24 development cases。报告见 `outputs/model-eval-deepseek-7case-pilot-20261029/20261029T132339Z-model-eval-e3b01b6a-f51a-41a9-8d8e-b1d1149b5918.json`。
- 限制：数据仍为 40 条 synthetic exploratory draft golden、人工审批 0 条；每个模型每个样例只跑 1 次，`benchmark_eligible=false`，不可宣称总体准确率/排名。
- 已准备人工审核清单：`evals/model-eval/human-review-v0.2.0.csv`（40 行唯一 case，含输入、draft expected、规则、source refs、dataset hash 和审核字段）；`evals/model-eval/HUMAN_REVIEW_GUIDE.md` 给出审核与数据版本更新步骤。校验数据完整，源 dataset 仍原样且 0 项 approved。
- 来源审查发现需人工定范围：model-022 的分类 JSON `unauthorized` 不等于当前 API 隐藏所有者信息时返回的 404；model-017 缺失状态和 model-020 双金额来源冲突属于合成分类输入，不是当前 API 存储层直接状态。复核指南已记录，不能混称 API contract coverage。
- 只读 Gold 数值复核：36 个可计算余额与预期一致；4 个缺失/冲突 Gold 合理地为 null。model-037 的最新轮次金额须由人工按完整 transcript 确认；此数值核对不能代替业务审批。
- 下一步：使用清单完成 40 条 case 的独立人工规则复核；有批准/修订记录后再扩大剩余 17 个 development case 和重复次数，核对 XEM 账单后才发布模型表现指标。

### model-eval-dataset-audit-v1 真实模型调用前的数据合同与复核清单审计

- 状态：已完成（离线审计工具）；真实模型全量 development 批次因当前 Codex 进程没有继承用户 PowerShell 的 Key，未代发。
- 变更：新增 `evals/model-eval/audit_dataset.py` 与针对性测试；运行前核验 Draft 2020-12 Schema、ID/scenario family 唯一性、split 数量、Gold/断言一致、check ID、多轮 role、synthetic/tool boundary、Canary 反回显和人工审核 CSV 的 dataset ID/version/hash/case IDs/approval snapshot。
- 结果：v0.2.0 的 12 项审计检查通过、0 失败、0 跳过；40 条、24/8/8、40 个唯一 case/family、人工批准 0、待审 40、工具使用用例 0；`ready_for_exploratory_run=true`、`benchmark_eligible=false`。结果只表示可开展探索性执行，不等于 Gold 正确或 Benchmark 合格。
- 证据：`outputs/model-eval-v0.2.0-dataset-audit-20261029/dataset-audit-v0.2.0.json`。
- 验收：`py -3.12 -m pytest evals/model-eval/test_audit_dataset.py -q` 7 passed；完整 model-eval 测试和 `py_compile` 结果见 `HANDOFF.md` 本轮记录。
- 后续：继续全 development 24 条 × 2 个 DeepSeek 模型的一轮对照前，先重新配置本机进程级 Key；此轮只记录探索性断言/Token/延迟，不报模型排名或通用准确率。人工复核与账单核对仍是独立门槛。

### model-eval-network-error-continuation-v1 网关网络错误后续跑

- 状态：Runner 修复和本地测试完成；真实 development 批次部分执行，剩余请求待续跑。
- 真实运行：用户启动 24 条 × 2 个 DeepSeek 路由的 48 请求批次，Run ID `model-eval-0221cb3c-b094-485b-8a3a-1c6132f5089e`，实际 9 次；8 条评测完成，7 通过、1 失败，1 次网关网络错误。发生在 model-005 Pro 的网络错误触发默认停止，因此后续计划未执行。
- 失败归因：model-003 的 deepseek-v4.1-flash 将 `allowed` 判为 false，但同一输出的 `reason_code` 为 `within_limit`；请求 326.57 元，剩余 326.58 元。其余输出字段与 Gold 一致，表现为字段内部矛盾；用例 Gold 仍待人工批准。
- 变更：新增仅针对 `gateway_network_error` 的 `--continue-on-gateway-network-error`，无重试；只续跑 model-005 至 model-024（40 个请求），不重复已完成的前 8 个组合。网络错误仍导致质量门禁失败。HTTP/配置错误仍按默认保护策略停止。
- 下一步：在 `docs/model-evaluation-protocol.md` 中的安全命令块运行剩余用例。合并分析保留两份报告及 model-005 Pro 的原始错误和后续尝试；不得静默覆盖尝试历史或报告 Benchmark 结论。

## 2026-10-29 更新：development 全批次与双项目浏览器验收

- 当前状态：development 24 条 × 2 DeepSeek 路由已完成 48 个唯一组合；51 次请求尝试，43 组匹配草案断言、5 组未匹配，质量门禁 failed。完整证据见 `docs/model-eval-development-summary-20261029.md`。
- PromptOps：Maven 37/37；model-eval pytest 48/48；本地 API pytest 26/26；数据审计 12/12；Vue 类型检查、生产构建、npm audit 0 vulnerabilities；8 次并发稳定性采样全部成功（延迟只标为 observed-only）；Playwright 全流程与桌面/手机截图通过。Web Provider 仍单独未配置。
- Gitea Quality Lab：最新本地 Gitea 28.2.0 验收 Run `20261029T150955Z-4437d857`：18/18；Harness pytest 27/27；项目 HTML 报告、JSON/JUnit 交叉校验和 1440/390px 报告页检查通过。证据见 `work/gitea-quality-lab/reports/runs/20261029T150955Z-4437d857/` 和该仓库 `reports/evidence.md`。报告页面的单独桌面/手机截图来自前一轮同 18-case 输出，已在 evidence log 标明。
- 汇总验收：`docs/phase-4-delivery-acceptance-20261029.md`。生产系统、生产监控值班、大规模生产压测均未被这些本地/合成结果证明。
- 下一步：先完成人工审核 CSV 中 40 条 v0.2.0 规则预期，批准或修订后创建新数据集版本；再决定重复性与盲测策略。CI 推送状态以 GitHub Actions 实际运行结果为准。
