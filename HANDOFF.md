# Claude / Codex 交接记录

使用规则：每轮把新记录追加在“历史记录”顶部，不覆盖旧记录。没有证据时不要写“测试通过”。

## 当前待处理交接

### 2026-07-04 — Codex — docs-readme-showcase-v1 README 展示版整理

- 当前分支：`main`；起始 checkpoint：`32dbcb3 fix: polish promptops showcase screenshots`。
- 本轮任务：仅整理 `README.md`，并同步 `TODO.md`、`HANDOFF.md`；未修改前端、后端、截图或设计参考。
- README 结构：PromptOps Studio 定位、Trace Detail 首图、七页功能截图、Core Features、Workflow、Tech Stack、Local Run、Verification、Project Boundary、Resume Pitch 与面试可讲点。
- 截图引用：`trace-detail.png`、`eval-dashboard.png`、`batch-evaluation.png`、`output-compare.png`、`prompt-studio.png`、`failure-cases.png`、`dataset.png`、`system-boundary.png`，均为仓库已有正式截图。
- 能力边界：明确 Local Demo、Mock Output、Rule-based Eval、无需 API Key、非生产环境、未接入真实模型、未使用真实用户数据、LLM-as-Judge 未启用或接入。
- 本轮仅做文档与路径检查，不重新运行构建、服务或截图脚本；未 commit、未 push。

### 2026-07-04 — Codex — Failure Cases / Dataset 最终验收确认

- 当前分支：`main`
- 本轮任务：仅确认 Failure Cases / 失败归因与 Dataset / 测试集两页，不再修改 Batch Evaluation 或开发新页面。
- 页面结果：Failure Cases 未出现 `LLM-as-Judge`，页面维持规则评测器、人工复核、回归任务与 Local Demo 边界；Dataset 顶部指标副文案完整，右侧样本预览无明显挤压，仍保持 Prompt / Eval 测试集管理语义。
- 独立检查：`npm run check:failure-cases-page` 与 `npm run check:dataset-view-page` 均通过，并分别重新生成 `docs/images/failure-cases.png`、`docs/images/dataset.png`。
- 截图证据：两张正式截图均为 `1440x810`，与对应 `.local/promptops-*-16x9-check.png` SHA-256 完全一致；检查脚本固定 `fullPage: false`，截图来自本项目本地 Vite 页面与 Playwright，不是第三方截图。
- 综合验证：`npm run build`、`npm run check:showcase-pages`、`git diff --check` 均通过；总验收明确串行覆盖 Failure Cases 与 Dataset 的独立检查。
- 变更保护：总验收后按 SHA-256 确认 Batch、Trace、Output Compare、Prompt Studio、System Boundary 五张非本轮正式截图与运行前一致。
- 边界确认：未修改 `README.md`、`backend/` 或 Batch 页面；未接入 API Key、真实模型、真实模型裁判或第三方截图；未自动 commit、未 push。

### 2026-07-04 — Codex — frontend-batch-evaluation-visual-v2 批量评测页面目标图重做

- 当前分支：`main`
- 本轮任务：仅按 `docs/design_refs/promptops_batch_evaluation_target_v2.png` 重做 Batch Evaluation / 批量评测页面视觉。
- 修改文件：`frontend/src/components/BatchEvaluation.vue`、`frontend/src/data/batchEvaluationMockData.ts`、`docs/images/batch-evaluation.png`、`TODO.md`、`HANDOFF.md`。
- 页面结构：顶部保留搜索、状态标签、环境与用户；标题为“批量评测工作台”，右上提供“回到总览 / 启动评测”；左栏依次为评测配置、核心指标、5 行样本评分矩阵、评测工作流；右栏依次为运行状态、执行时间线、高风险样本 TOP 3、提供者状态。
- 首屏结果：评测工作流完整进入 `1440x810` viewport；80% 圆环居中，`96 / 120 完成` 与右侧状态列表未发生文字挤压；顶部状态统一为“本地演示 / 规则评测 / 无需 API Key / 环境：Local Demo / Dev User”。
- 截图来源：`docs/images/batch-evaluation.png` 由本项目本地 Vite 页面经 Playwright 生成，固定 `1440x810` viewport，`fullPage: false`，不是目标参考图或第三方截图。
- 已执行：`npm run check:batch:16x9`，通过并重新生成正式 Batch 截图。
- 已执行：`npm run build`，通过；`vue-tsc --noEmit && vite build` 成功。
- 已执行：`npm run check:showcase-pages`，通过；运行前备份并在运行后按 SHA-256 恢复 Batch 之外的 7 张正式截图。
- 已执行：`git diff --check`，通过。
- 边界确认：未修改 `README.md`、`backend/`、其它页面组件、`.env` 或 `.local/reference_screenshots/`；未接入真实模型、真实 Provider、API Key、LLM-as-Judge 或生产数据；未自动 commit、未 push。

### 2026-07-04 — Codex — frontend-showcase-pages-v1 命名收口与 checkpoint 验收

- 当前分支：`main`
- 本轮任务：按用户要求恢复上一轮中断状态，只做截图命名收口、验收复跑和 checkpoint 提交准备；未继续开发新页面。
- 命名收口：`frontend/scripts/check-dashboard-16x9.mjs` 已确认会写出 `docs/images/eval-dashboard.png`；`frontend/scripts/check-dataset-view-page.mjs` 通过 `slug: 'dataset'` 写出 `docs/images/dataset.png`；已清除 Dataset 正式截图旧命名引用和旧命名未跟踪截图。
- 正式截图清单：`docs/images/eval-dashboard.png`、`docs/images/batch-evaluation.png`、`docs/images/trace-detail.png`、`docs/images/output-compare.png`、`docs/images/prompt-studio.png`、`docs/images/failure-cases.png`、`docs/images/dataset.png`、`docs/images/system-boundary.png`。
- 截图来源：全部由本项目本地 Vite 页面通过 Playwright 生成，固定 `1440x810` viewport，`fullPage: false`；`.local` 仅保留检查图，未纳入 Git。
- 已执行：`npm run build`，通过。
- 已执行：`npm run check:dashboard:16x9`，通过，并刷新 `docs/images/eval-dashboard.png`。
- 已执行：`npm run check:batch:16x9`，通过，并刷新 `docs/images/batch-evaluation.png`。
- 已执行：`npm run check:trace:16x9`，通过，并刷新 `docs/images/trace-detail.png`。
- 已执行：`npm run check:showcase-pages`，通过；该命令串行覆盖 Batch、Trace、Output Compare、Prompt Studio、Failure Cases、Dataset、System Boundary 七个页面检查。
- 已执行：`git diff --check`，通过；仅输出 Windows 换行提示。
- 边界确认：未修改 `README.md` 或 `backend/`；未接入真实模型、真实 Provider、API Key、LLM-as-Judge、生产数据、第三方参考截图或 `.env`；页面仍保留 Local Demo / Mock Output / Rule-based Eval / 无需 API Key / 非生产环境边界。

### 2026-07-04 — Codex — frontend-showcase-pages-v1 多页面展示页验收收口

- 当前分支：`main`
- 本轮任务：继续收口前端 showcase 页面，统一检查七个展示页并刷新真实本地运行截图。
- 涉及页面：Batch Evaluation、Trace Detail、Output Compare、Prompt Studio、Failure Cases、Dataset View、System Boundary。
- 涉及脚本：`frontend/scripts/check-batch-page.mjs`、`frontend/scripts/check-trace-page.mjs`、`frontend/scripts/check-output-compare-page.mjs`、`frontend/scripts/check-prompt-studio-page.mjs`、`frontend/scripts/check-failure-cases-page.mjs`、`frontend/scripts/check-dataset-view-page.mjs`、`frontend/scripts/check-system-boundary-page.mjs`、`frontend/scripts/page-check-common.mjs`。
- 输出截图：`docs/images/batch-evaluation.png`、`docs/images/trace-detail.png`、`docs/images/output-compare.png`、`docs/images/prompt-studio.png`、`docs/images/failure-cases.png`、`docs/images/dataset.png`、`docs/images/system-boundary.png`。

### frontend-showcase-pages-v1 实现与验证

- Batch Evaluation 首屏已进一步压缩布局密度；`1440x810` 截图中右侧 Run State、Timeline、Failure、Provider 区域没有明显模块裁切。
- `docs/images/failure-cases.png` 已目检，顶部指标、失败样本表、失败原因分布、选中案例详情、修复任务与底部闭环在首屏内可读。
- 已执行：`npm run check:showcase-pages`，通过；脚本依次完成七个页面检查并生成对应项目截图。
- 已执行：`npm run build`，通过；`vue-tsc --noEmit && vite build` 成功。
- 本轮未修改 `backend/`、`.env`、API Key 或生产配置；未自动 commit。

### 2026-07-04 — Codex — frontend-trace-detail-v1 运行追踪详情页面前端落地

- 当前分支：`main`
- 本轮任务：在已完成的 Eval Dashboard 与 Batch Evaluation 视觉系统上落地 Trace / Run Detail / 运行追踪详情页面。
- 修改文件：`frontend/src/App.vue`、`frontend/src/components/EvalDashboard.vue`、`frontend/src/components/BatchEvaluation.vue`、`frontend/package.json`、`TODO.md`、`HANDOFF.md`、`docs/demo-script.md`。
- 新增文件：`frontend/src/components/TraceDetail.vue`、`frontend/src/data/traceDetailMockData.ts`、`frontend/scripts/check-trace-16x9.mjs`、`docs/images/trace-detail.png`。
- 用户提供并允许保留的参考输入：`docs/design_refs/promptops_trace_detail_target_v1.png`；该文件仅作为 image2 原创视觉方向参考，不是项目真实运行截图。

### frontend-trace-detail-v1 实现内容

- `App.vue` 新增 `trace` 视图，`?view=trace` 可直接访问；Dashboard 与 Batch 左侧“运行追踪”导航会进入新 Trace Detail 页面。
- `TraceDetail.vue` 延续浅色企业级控制台、左侧中文导航、顶部 Local Demo / Rule-based Eval / 无需 API Key 标签和右侧深色 Trace 主视觉。
- 页面围绕 `run_1022` 与 `case_093` 展示当前运行上下文、运行摘要、输入与 Prompt 构建、输出与 Schema 对比、规则评分、失败原因与修复建议、运行 Trace、人工复核区、Provider 状态与边界和 PromptOps 运行追踪链路。
- `traceDetailMockData.ts` 集中保存结构化本地演示数据；页面明确使用 Mock Output 与 Rule-based Eval，不连接真实模型或 API Key。
- `check-trace-16x9.mjs` 使用独立端口启动 demo 后端与 Vite，从 Dashboard 和 Batch 导航进入 Trace，验证 13 个核心模块、7 个 Trace 节点、Failure Case 高亮、Human Review、边界说明、无浏览器错误和 `1440x810` 非长图截图。

### frontend-trace-detail-v1 测试与验证

- 已执行：`npm run build`，通过。
- 已执行：`npm run check:dashboard:16x9`，通过；第一阶段 Eval Dashboard 未被破坏。
- 已执行：`npm run check:batch:16x9`，通过；第二阶段 Batch Evaluation 未被破坏。
- 已执行：`npm run check:trace:16x9`，通过。
- Trace 检查固定使用 `1440x810` viewport 与 `fullPage: false`，验证页面无横向或纵向滚动、核心模块均在 viewport 内、7 个 Trace 节点未被裁切，并读取 PNG 文件头确认截图尺寸。
- 本地检查截图：`.local/promptops-trace-detail-16x9-check.png`。
- 正式项目截图：`docs/images/trace-detail.png`；与检查截图来自同一 Playwright screenshot buffer。
- 检查脚本结束后释放本轮启动的后端、Vite 和浏览器进程。

### frontend-trace-detail-v1 边界与下一步

- 未修改 `backend/`、`README.md`、`.local/reference_screenshots/` 或任何 `.env` / 密钥文件。
- 未接入真实模型、真实 API Key、真实 Provider、LLM-as-Judge、异步任务队列或持久化人工复核。
- 本轮未自动 commit；等待用户验收真实截图后再决定是否提交。
- 下一步建议先验收 `docs/images/trace-detail.png` 的视觉质量，再决定是否提交第三阶段变更。

### 2026-07-04 — Codex — frontend-batch-evaluation-v1 批量评测工作台视觉升级

- 当前分支：`main`
- 本轮任务：在已提交的 Eval Dashboard 视觉系统上落地 Batch Evaluation / 批量评测工作台。
- 修改文件：`frontend/src/App.vue`、`frontend/src/components/EvalDashboard.vue`、`frontend/package.json`、`TODO.md`、`HANDOFF.md`、`docs/demo-script.md`。
- 新增文件：`frontend/src/components/BatchEvaluation.vue`、`frontend/src/data/batchEvaluationMockData.ts`、`frontend/scripts/check-batch-16x9.mjs`、`docs/images/batch-evaluation.png`。
- 用户提供并允许保留的参考输入：`docs/design_refs/promptops_batch_evaluation_target_v1.png`；该文件仅作为 image2 原创视觉方向参考，不是项目真实运行截图。

### frontend-batch-evaluation-v1 实现内容

- `App.vue` 新增 `batch` 视图，`?view=batch` 可直接访问；Dashboard 与 Batch 左侧导航可双向切换，旧 Eval Run / Prompt Versions / Trace Review 视图保留。
- `BatchEvaluation.vue` 延续浅色开发者控制台、克制蓝色状态和右侧深色 Eval Run 时间线，并将配置流、Score Matrix、失败定位作为页面主要证据。
- 页面包含评测配置流程、`run_batch_2048` 批量运行状态、4 个核心指标、趋势图、7 条 Case 结果矩阵、7 节点时间线、失败原因排行、当前 Case 与 Schema 对比、Provider 状态和批量评测闭环。
- `batchEvaluationMockData.ts` 集中保存页面结构化本地演示数据；默认选中 `case_093`，切换矩阵行会更新失败样本分析。
- 顶部和侧栏持续标明 Local Demo、Mock Output、Rule-based Eval、无需 API Key、非生产环境；未把本地演示指标包装为线上模型结果。
- `check-batch-16x9.mjs` 使用独立端口启动 demo 后端与 Vite，从 Dashboard 导航进入 Batch，验证返回总览、再次进入 Batch 和 Case 联动，再生成首屏截图。

### frontend-batch-evaluation-v1 测试与验证

- 已执行：`npm run build`，通过。
- 已执行：`npm run check:dashboard:16x9`，通过；第一阶段 Eval Dashboard 未被破坏。
- 已执行：`npm run check:batch:16x9`，通过。
- Batch 检查固定使用 `1440x810` viewport 与 `fullPage: false`，验证页面无横向或纵向滚动、11 个核心模块完整入镜、7 个 Timeline 节点和 7 条示例 Case 均存在。
- 本地检查截图：`.local/promptops-batch-evaluation-16x9-check.png`。
- 正式项目截图：`docs/images/batch-evaluation.png`；与检查截图来自同一 Playwright screenshot buffer。
- 检查脚本结束后释放本轮启动的后端、Vite 和浏览器进程。

### frontend-batch-evaluation-v1 边界与下一步

- 未修改 `backend/`、`README.md`、`.local/reference_screenshots/` 或任何 `.env` / 密钥文件。
- 未接入真实模型、真实 API Key、真实 Provider、LLM-as-Judge、异步任务队列或持久化人工复核。
- 本轮未自动 commit；等待用户验收真实截图后再决定是否提交。
- 下一步建议先验收 Batch Evaluation 截图，再决定是否进入 Trace / Run Detail 页面升级。

### 2026-07-04 — Codex — frontend-dashboard-16x9-layout-fix 评测总览首屏修复

- 当前分支：`main`
- 本轮任务：将 Eval Dashboard 从长页面调整为 `1440x810` 的 16:9 桌面首屏控制台。
- 修改文件：`frontend/src/components/EvalDashboard.vue`、`frontend/scripts/check-dashboard-16x9.mjs`、`frontend/package.json`、`TODO.md`、`HANDOFF.md`。
- 未修改：后端业务逻辑、`README.md`、`docs/images/`、`.local/reference_screenshots/`、真实 API Key 或生产配置。

### frontend-dashboard-16x9-layout-fix 实现内容

- 为高度不超过 900px 的桌面视口增加紧凑布局，主列与洞察列约为 `58% / 42%`。
- 主列保留当前评测实验、核心指标、评测健康度和最近 5 条评测运行；右列保留完整 7 节点 Trace、失败归因和 Provider 状态。
- PromptOps 评测闭环改为底部紧凑横向 stepper，并在首屏内可见。
- 新增独立检查脚本，固定使用 `1440x810` viewport、`fullPage: false`，输出到 `.local/promptops-dashboard-overview-16x9-check.png`。
- 检查脚本会验证页面无横向或纵向滚动、核心模块均在 viewport 内、7 个 Trace 节点未被裁切，并读取 PNG 文件头确认截图尺寸。

### frontend-dashboard-16x9-layout-fix 测试与验证

- 已执行：`npm run build`，通过。
- 已执行：`npm run check:dashboard:16x9`，通过。
- 截图实际尺寸：`1440x810`，不是长图。
- 本地检查截图：`.local/promptops-dashboard-overview-16x9-check.png`。
- 检查脚本结束后已关闭本轮启动的后端与 Vite 服务。
- 未运行后端 Maven 测试；本轮没有修改后端代码。
- 未运行正式截图脚本；本轮没有覆盖 `docs/images/`。

### frontend-dashboard-16x9-layout-fix 边界与下一步

- 页面仍是 Local Demo / Mock Output / Rule-based Eval，不代表真实线上模型评测结果。
- 当前本地 16:9 截图可作为正式 README 首图候选。
- 下一步建议在用户明确确认后，再生成或替换 `docs/images/` 中的正式项目截图。

### 2026-07-04 — Codex — frontend-dashboard-visual-v1 评测总览首页视觉升级

- 当前分支：`main`
- 本轮任务：基于已确认目标图，优先落地 Eval Dashboard / 评测总览首页。
- 修改文件：`frontend/src/App.vue`、`frontend/src/styles.css`、`frontend/src/components/EvalDashboard.vue`、`frontend/src/data/evalDashboardMockData.ts`、`TODO.md`、`HANDOFF.md`、`docs/demo-script.md`
- 未修改：后端业务逻辑、`README.md`、截图脚本、`docs/images/`、`.local/reference_screenshots/`、依赖清单和任何 API Key / `.env` 文件。

### frontend-dashboard-visual-v1 实现内容

- 新增中文企业级 `EvalDashboard.vue` 首页，默认 `overview` 视图进入新的 PromptOps Studio 评测总览。
- 首页包含左侧中文导航、顶部状态栏、页面标题区、当前评测实验、3 个核心指标、3 个辅助指标、评测健康度趋势、右侧深色运行 Trace、最近评测运行、回归风险与失败归因、Provider 状态与本地规则、PromptOps 评测闭环。
- 右侧深色 Trace 面板围绕 `run_1022` 展示 Input、Prompt Build、Mock Output、Rule Evaluator、Score、Failure Case、Human Review。
- 新增 `frontend/src/data/evalDashboardMockData.ts`，集中声明本轮首页展示用的结构化本地演示数据，明确属于 Mock Output / Rule-based Eval / 非生产环境。
- `App.vue` 保留原有后端 API 加载、Prompt 选择、Eval Run、Prompt Versions 和 Trace Review 分支；仅将 `overview` 视图切换为新首页组件。
- `styles.css` 新增 `dashboard-mode` 全宽容器覆盖，避免旧 1280px 居中布局压缩首页。

### frontend-dashboard-visual-v1 测试与验证

- 已执行：`npm run build`。
- 结果：通过，`vue-tsc --noEmit && vite build` 成功。
- 已启动本地 demo 后端：`mvn -pl backend spring-boot:run`，profile 为 `demo`，端口 `18081`。
- 已启动本地 Vite：`npm run dev -- --host 127.0.0.1 --port 5175 --strictPort`，代理到 `http://127.0.0.1:18081`。
- 已执行 Playwright 桌面检查：打开 `http://127.0.0.1:5175/?view=overview`，等待 `[data-ready="true"]`，无浏览器错误，无横向溢出，截图保存到 `.local/promptops-dashboard-overview-check.png`。
- 已执行 Playwright 移动端检查：390px viewport 无横向溢出，截图保存到 `.local/promptops-dashboard-overview-mobile-check.png`。
- 未运行：`npm run screenshots`。原因：该脚本会覆盖 `docs/images`，本轮未得到覆盖项目正式截图资产的确认。
- 未运行后端 Maven 测试。原因：本轮未修改后端业务逻辑，验证重点是前端构建与页面视觉。

### frontend-dashboard-visual-v1 边界与剩余风险

- 首页中的 `run_1022`、91.2%、0.86/1.00、失败样本等为前端本地演示数据，不是线上真实模型指标。
- 未接入真实 LLM、真实 AI Agent Runtime、真实 API Key、真实 Provider 或 LLM-as-Judge。
- 本轮只完整升级评测总览首页；其它页面仍保留上一版交互和视觉，后续可按 Batch Evaluation、Trace Detail、Output Compare、Failure Cases 顺序继续升级。
- README 仍未更新，后续只能使用本项目真实运行截图，不能使用 image2 目标图或第三方参考截图。

### 2026-06-22 — Codex — frontend-redesign-v1 前端页面重设计

- 当前分支：`frontend-redesign-v1`
- 本轮任务：frontend-redesign-v1 前端页面重设计。
- 修改文件：`frontend/src/App.vue`、`frontend/src/styles.css`、`frontend/src/components/MetricCard.vue`、`frontend/src/components/StatusBadge.vue`、`TODO.md`、`HANDOFF.md`

### frontend-redesign-v1 实现内容

- 顶部重构为 `PromptOps Evaluation Lab` / `Rule-based Prompt Evaluation Console`，保留 Local Demo、Mock Output、Rule-based Eval、No real LLM connected、No agent runtime 等边界提示。
- Overview 调整为四张核心指标卡：Prompt Profiles、Latest Pass Rate、Cases Evaluated、Review Required，并保留 Prompt Profile 选择、搜索和场景标签入口。
- Eval Run 页面保留 Case Result 列表和原有筛选逻辑，新增 selected case 的 Rule Audit 面板，明确展示 `RuleEvaluator`、`MockOutputGenerator` 和 simulated latency。
- Prompt Versions 页面保留版本链路、Prompt 模板、variables、output format 和版本对比，并新增 `variables_json` 使用标准 JSON 数组存储说明。
- Trace Review 页面保留 Generation Trace、Mock 输出、评分规则检查和前端 Review 操作，并明确写出 `Review state is frontend-only demo state`。
- 新增纯前端展示组件：`MetricCard.vue` 与 `StatusBadge.vue`；未引入第三方 UI 库，未新增依赖。
- `styles.css` 新增统一 CSS tokens，页面宽度调整到 1280px 左右，卡片统一 16px 圆角，并补充移动端纵向堆叠处理。

### frontend-redesign-v1 测试与验证

- 已执行：`npm run typecheck`。
- 结果：通过，`vue-tsc --noEmit` 无错误。
- 已执行：`npm run build`。
- 结果：通过，`vue-tsc --noEmit && vite build` 成功，生成生产构建产物。
- 未运行：`npm run screenshots`。原因：现有截图脚本会启动后端与 Vite，并覆盖 `docs/images` 截图资产；本轮限制要求不启动服务、不覆盖截图。
- 未运行：`npm install`。原因：本轮限制禁止安装依赖，且本地 `node_modules` 已存在，可直接执行验证。
- 未启动后端、前端、数据库或 Docker。
- 未修改后端 Java、`pom.xml`、schema、`application.yml`、README 或依赖清单。

### frontend-redesign-v1 剩余风险

- 本轮没有浏览器截图验证，也没有启动真实前端页面人工查看；视觉结果仅通过类型检查与生产构建验证。
- 截图脚本仍可在用户允许启动服务和覆盖截图资产的后续轮次中单独运行。

### 2026-06-22 — Codex — P2-2 README / GitHub 展示强化

- 当前分支：`resume-optimization-v1`
- 本轮任务：P2-2 README / GitHub 展示强化。
- 修改文件：`README.md`、`TODO.md`、`HANDOFF.md`、`docs/acceptance-checklist.md`

### P2-2 实现内容

- README 顶部新增静态 Shields.io 技术栈徽章：Java 17、Spring Boot 3、MyBatis-Plus、MySQL、H2、Vue 3、TypeScript、OpenAPI、Tests 13 passing。
- README 新增“技术亮点速览”表格，按“能力点 / 实现方式 / 真实程度”区分已实现能力、Mock 输出和前端演示状态。
- README 新增“本地运行演示”章节，列出后端 demo profile、前端启动命令、本地页面和 Swagger UI 地址，并说明 DemoDataInitializer、MockOutputGenerator、RuleEvaluator 的真实边界。
- README 更新“项目结构”目录树，显式列出 P0-P2 关键后端入口、Service、配置、docs、`HANDOFF.md` 和 `TODO.md`。
- README 继续引用已有截图：`docs/images/dashboard.png`、`docs/images/eval-run.png`、`docs/images/prompt-versions.png`、`docs/images/trace-review.png` 和 `docs/images/large/`；未新增或覆盖截图。
- README 保持项目边界诚实：无真实 LLM、无真实 AI Agent、无多模型对比、无 LLM-as-Judge，Swagger UI 是本地接口文档，不是线上部署。

### P2-2 测试与验证

- 已执行：`mvn -pl backend test`。
- 结果：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 未改后端 Java 源码，未改前端 Vue，未改 `pom.xml`，未改 `package.json` / `package-lock.json`，未改数据库 schema，未改 application 配置。
- 未安装依赖，未启动后端、前端、外部数据库或 Docker 服务，未运行截图脚本。

### P2-2 下一轮唯一建议任务

resume-optimization-v1 最终只读验收。理由：P0-P2 的工程化与展示强化已经完成，下一轮先做全项目只读核对、测试记录和简历口径收敛，比继续引入 P1-2 分页改动更适合收尾。

### 2026-06-22 — Codex — P2-1 补 Springdoc OpenAPI 接口文档

- 当前分支：`resume-optimization-v1`
- 本轮任务：P2-1 补 Springdoc OpenAPI 接口文档。
- 修改文件：`backend/pom.xml`、`backend/src/main/java/com/promptops/evalconsole/api/PromptOpsController.java`、`backend/src/main/resources/application.yml`、`README.md`、`TODO.md`、`HANDOFF.md`、`docs/architecture.md`

### P2-1 实现内容

- 新增 Maven 依赖：`org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`。
- `PromptOpsController` 增加类级别 `@Tag`，并为全部 11 个接口方法增加 `@Operation(summary = "...")`。
- `application.yml` 增加 Springdoc 路径配置：`/api-docs` 和 `/swagger-ui.html`，并按 method 排序。
- `README.md` 增加本地 Swagger UI 访问说明：`http://localhost:18080/swagger-ui.html`。
- `docs/architecture.md` 补充 OpenAPI / Swagger UI 是接口展示和协作能力，不是 AI 能力。
- 未修改业务逻辑、接口路径、方法签名、参数、返回类型或前端代码。

### P2-1 测试与验证

- 已执行：`mvn -pl backend test`。
- 结果：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 未改 Service / Mapper / Entity，未改 `ConsoleDtos.java`，未改 `GlobalExceptionHandler.java`，未改前端 Vue，未改数据库 schema。
- 未安装 npm 依赖，未启动后端、前端、外部数据库或 Docker 服务；仅运行 Maven 测试内存 H2 上下文。

### P2-1 下一轮唯一建议任务

P2-2 README / GitHub 展示强化。理由：OpenAPI 文档已经补齐，下一步先把 GitHub 展示路径、截图和本地访问说明打磨清楚，更能承接本轮面试展示目标；P1-2 分页支持仍有价值，但当前 demo 数据量小，紧急度较低。

### 2026-06-22 — Codex — P1-3 补关键日志点

- 当前分支：`resume-optimization-v1`
- 本轮任务：P1-3 补关键日志点。
- 修改文件：`backend/src/main/java/com/promptops/evalconsole/service/PromptOpsService.java`、`backend/src/main/java/com/promptops/evalconsole/service/RuleEvaluator.java`、`backend/src/main/java/com/promptops/evalconsole/service/DemoDataInitializer.java`、`TODO.md`、`HANDOFF.md`、`docs/architecture.md`

### P1-3 实现内容

- `PromptOpsService` 新增 `Logger`，记录 Eval Run 开始时的 `promptVersionId`、`datasetId`、`caseCount`，以及完成时的 `runKey`、`passedCases`、`failedCases`、`passRate`。
- `PromptOpsService` 在 `toVariablesJson()` 序列化失败时记录 `log.error`，只记录变量数量和异常；在 `variables()` 解析失败且即将抛出 `IllegalStateException` 前记录 `log.warn`，只记录截断摘要。
- `RuleEvaluator` 新增 `Logger`，当规则评分产生失败项时记录 debug 日志，包含 `status`、失败数量和 `score`。
- `DemoDataInitializer` 新增 `Logger`，记录 demo 数据因配置关闭或已有模板而跳过初始化、三个种子模板完成写入，以及初始化完成后的 profile 数量。
- 未修改业务逻辑，未修改 API 合约，未新增依赖。

### P1-3 测试与验证

- 已执行：`mvn -pl backend test`。
- 结果：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 未改前端 Vue，未改 `pom.xml`，未改 `package.json` / `package-lock.json`，未改数据库 schema。
- 未安装依赖，未启动后端、前端、外部数据库或 Docker 服务；仅运行 Maven 测试内存 H2 上下文。

### P1-3 下一轮唯一建议任务

P1-2 增加分页支持。理由：当前服务层关键日志已经补齐，下一步先补列表分页能降低数据增长后的 API 风险；OpenAPI 文档更适合在分页合约确定后再补。

### 2026-06-22 — Codex — P1-1 补全局异常处理器

- 当前分支：`resume-optimization-v1`
- 本轮任务：P1-1 补全局异常处理器。
- 新增文件：`backend/src/main/java/com/promptops/evalconsole/api/GlobalExceptionHandler.java`
- 修改文件：`backend/src/main/java/com/promptops/evalconsole/api/dto/ConsoleDtos.java`、`backend/src/test/java/com/promptops/evalconsole/EvalConsoleApplicationTests.java`、`TODO.md`、`HANDOFF.md`、`docs/architecture.md`、`docs/acceptance-checklist.md`、`README.md`

### P1-1 实现内容

- 使用 `@RestControllerAdvice` 新增全局异常处理器。
- 统一处理 `ResponseStatusException`：使用异常自身 HTTP status 和 reason。
- 统一处理 `MethodArgumentNotValidException`：返回 400、`Validation failed` 和字段级 `errors`。
- 统一处理通用 `Exception`：返回 500 和通用消息 `Internal server error`，不暴露堆栈、SQL、类名等内部细节。
- 在 `ConsoleDtos` 中新增 `ErrorResponse` 和 `FieldErrorItem` record。

### P1-1 ErrorResponse 结构

- `code`：HTTP 状态码数字。
- `message`：可展示错误信息。
- `timestamp`：`LocalDateTime` 时间戳。
- `errors`：字段级错误列表；非校验错误为空时不输出。

### P1-1 测试与验证

- 新增测试：`returnsUnifiedErrorForMissingPromptTemplate`，覆盖 404 `ResponseStatusException` 统一响应。
- 新增测试：`returnsFieldErrorsForInvalidPromptTemplateRequest`，覆盖 400 参数校验错误和字段级 `errors`。
- 已执行：`mvn -pl backend test`。
- 结果：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 未改前端 Vue，未改 `pom.xml`，未改 `package.json` / `package-lock.json`，未改数据库 schema。
- 未安装依赖，未启动后端、前端、数据库或 Docker。

### P1-1 下一轮唯一建议任务

P1-3 补关键日志点。理由：异常响应已经统一，下一步补服务层关键业务日志能继续提升 Java 后端工程可信度；分页支持也有价值，但当前数据量小，日志对面试解释和问题定位更直接。

### 2026-06-22 — Codex — P0-2 修复 variables_json 命名与真实 JSON 存储问题

- 当前分支：`resume-optimization-v1`
- 本轮任务：P0-2 修复 `variables_json` 命名与真实 JSON 存储问题
- 方案选择：方案 A，保留 `variables_json` 字段名并改为标准 JSON 数组存储。
- 选择理由：MySQL schema 已使用 JSON 类型，项目已有 Jackson；改成真实 JSON 数组能让字段名、数据库设计、Java 序列化逻辑和面试解释一致，同时保持前端接口不变。

### P0-2 修改文件

- `backend/src/main/java/com/promptops/evalconsole/service/PromptOpsService.java`：`toVariablesJson()` 改为 Jackson JSON 序列化，`variables()` 改为 Jackson JSON 反序列化，并兼容旧管道格式读取。
- `backend/src/main/java/com/promptops/evalconsole/persistence/entity/PromptVersionEntity.java`：补充 `variablesJson` 字段语义注释。
- `backend/src/main/resources/schema.sql`：补充 `variables_json` 存储标准 JSON 数组的注释。
- `backend/src/main/resources/schema-h2.sql`：补充 H2 使用 CLOB 承载 JSON 字符串的注释。
- `backend/src/test/resources/schema-h2.sql`：补充 H2 测试 schema 注释。
- `backend/src/test/java/com/promptops/evalconsole/EvalConsoleApplicationTests.java`：新增 JSON 数组写入与旧管道格式兼容读取测试。
- `TODO.md`：将 P0-1、P0-2 状态更新为已完成。
- `docs/architecture.md`、`docs/interview-guide.md`、`docs/acceptance-checklist.md`、`README.md`：同步当前 JSON 存储和测试基线。
- `HANDOFF.md`：追加本轮交接记录。

### P0-2 结果与边界

- `variables_json` 当前已经用于存储真实 JSON 数组，例如 `[{"name":"customerType","description":"客户类型","required":true}]`。
- Java 侧使用 Jackson 序列化 / 反序列化 `List<PromptVariableDto>`。
- 旧数据如 `[name::description::true|risk::风险等级::false]` 仍可兼容解析。
- 前端接口保持不变，仍返回 `variables` 数组；未修改 Vue 源码。
- 未修改 `package.json` / `package-lock.json` / `pom.xml`。
- 未安装依赖，未启动后端、前端、数据库或 Docker。

### P0-2 验证结果

- 已执行：`mvn -pl backend test`
- 结果：BUILD SUCCESS，Tests run: 11, Failures: 0, Errors: 0, Skipped: 0。
- 已执行：`git diff --check`，结果退出码 0；仅有 Windows 换行符提示。
- 已执行：`git status`，结果仅显示本轮相关文件 modified，未暂存。
- 已执行本轮 diff 范围检查，未出现前端 Vue、`package.json` / `package-lock.json` 或 `pom.xml` 改动。

### P0-2 下一轮唯一建议任务

P1-1 补全局异常处理器。P0 风险修完后，下一步应提升 Java 后端工程可信度，让项目更适合写进简历。

### 2026-06-21 — Codex — P0-1 项目定位去虚夸 + 文档基线补齐

- 当前分支：`resume-optimization-v1`
- 已有提交：
  - `d56decc init ai agent eval promptops project`
  - `1ef1885 add claude codex collaboration rules`
- 本轮任务：P0-1 项目定位去虚夸 + 文档基线补齐
- 本轮修改范围：
  - `README.md`
  - `HANDOFF.md`
  - `TODO.md`
  - `AGENTS.md`
  - `docs/*.md`

### 项目真实定位

PromptOps 本地实验台 / 规则评测工作台 / Eval Run + Case Result + Generation Trace 落库闭环。

### 项目真实能力

- Prompt 模板与多版本管理
- Eval Dataset / Eval Case / ScoreRule / EvalRun / EvalCaseResult / GenerationTrace 数据建模
- RuleEvaluator 可解释规则评分
- EvaluationPolicy Review 队列判断与版本回归风险分级
- MySQL + H2 demo/test profile
- Vue 3 + TypeScript 前端真实 fetch 后端 API
- 后端测试覆盖规则评分和评测主流程

### 明确边界

- 无真实 LLM 调用
- 无真实 AI Agent
- 无多模型对比
- 无 LLM-as-Judge
- MockOutputGenerator 是确定性 Mock，输出和模拟耗时由版本号与 caseKey 等本地规则决定
- RuleEvaluator 是规则评分，不是 AI 语义评分
- Review 状态部分仍是前端演示状态，不是持久化审批流
- `variables_json` 目前不是真正 JSON，这是 P0-2 风险项

### 本轮发现的文档问题

- `TODO.md` 不存在
- `HANDOFF.md` 是空模板
- `AGENTS.md` 的“开始工作前必须读取”列表引用了不存在的 `docs/PRD.md`、`docs/DESIGN.md` 和 `docs/ARCHITECTURE.md`
- `README.md` 副标题“AI Agent 评测中心”容易造成误解

### 本轮只读审查记录

- 已执行：`git status`
- 已执行：`git log --oneline -5`
- 已读取：`AGENTS.md`、`CLAUDE.md`、`HANDOFF.md`、`README.md`、`docs/architecture.md`、`docs/interview-guide.md`、`docs/demo-script.md`、`docs/acceptance-checklist.md`、`templates/codex-task-template.md`
- 已核对源码边界：`MockOutputGenerator`、`RuleEvaluator`、`EvaluationPolicy`、`PromptOpsService` 与 schema 中的 `variables_json` 相关字段
- 未发现本轮开始前已有业务文件 diff；`git diff --stat` 仅输出 Windows 换行符提示

### 本轮未做事项

- 未安装依赖
- 未启动服务
- 未运行构建
- 未运行测试
- 未修改核心业务代码
- 未修改 Java / Vue / SQL / `pom.xml` / `package.json`

### 下一轮唯一建议任务

P0-2 修复 `variables_json` 命名与真实 JSON 存储问题。

## Codex 修复结果

- 修复时间：2026-06-21（Asia/Shanghai）
- 修改文件：`README.md`、`HANDOFF.md`、`TODO.md`、`AGENTS.md`、`docs/*.md`
- 修复说明：补齐任务队列和交接记录，修正容易被理解为真实 AI Agent / LLM Eval 系统的文档表述，并补充 Mock、规则评分、Review、Profile 与不支持项边界。
- 实际运行命令：
  - `git diff -- README.md docs HANDOFF.md TODO.md AGENTS.md CLAUDE.md templates`
  - `git diff --check -- README.md docs HANDOFF.md TODO.md AGENTS.md CLAUDE.md templates`
  - `git status`
- 实际结果：
  - diff 仅涉及文档类文件：`AGENTS.md`、`HANDOFF.md`、`README.md`、`TODO.md`、`docs/acceptance-checklist.md`、`docs/architecture.md`、`docs/demo-script.md`、`docs/interview-guide.md`
  - `git diff --check` 退出码 0；仅有 Windows 换行符提示
  - `git status` 显示上述文件未暂存，`TODO.md` 为 untracked；未看到 `.gitignore` modified
- 未运行的验证及原因：按本轮限制，不运行 `mvn test`、`mvn package`、`npm install`、`npm run build`，也不启动后端、前端、数据库或 Docker。
- 剩余风险：前端页面和部分种子数据仍可能出现 “AI Agent” 字样，但本轮明确禁止修改 Vue / Java 源码，留待后续任务确认是否处理。

## 历史记录

暂无历史记录。

## 2026-10-28 第二阶段实施记录（早期检查点）

- 本轮开始 HEAD：`d608cc28a2fae8246ad33712d31199185c538639`；分支：`feat/agent-evaluation-platform`。开始时工作区已有前序实现的未暂存和未跟踪文件，本轮沿用并保留。
- 本节保留实现早期状态记录：当时使用内存 H2、旧版 Mock、14 项 Maven 测试；随后这些限制在下方最终验收中得到更新，不应作为最终状态。

## 2026-10-28 第二阶段验收完成

- 结束 HEAD 仍为 `d608cc28a2fae8246ad33712d31199185c538639`；分支仍是 `feat/agent-evaluation-platform`；未提交、未推送，第一阶段审计报告未改动。
- Agent Eval 数据集版本 `1.0.1`，26 条不同的原创合成用例，类别分布为 6/4/5/4/3/4。新增数据集矩阵、固定输入和可复现断言。Mock Agent 是确定性工具桩，不调用真实模型。
- Vue 前端默认打开真实 Agent Eval 工作台；HTTP POST 同步触发执行，写入 UUID 运行、逐条结果和 Trace。运行以 `PENDING → RUNNING → COMPLETED` 落库，GET 历史/详情和报告都读保存的完整 JSON 快照。Demo H2 从内存库改为本地文件库，test profile 仍使用内存 H2。
- 运行 `82b67bd7-0647-4dc5-9272-80730765037a`：26 条执行成功，Agent 行为 24 通过、2 失败、0 执行错误、0 跳过，质量门禁失败。`agent-014` 误选工具、`agent-015` 重复工具调用均被保留。
- Trace 在 Mock Agent 执行时记录实际工具参数、返回、错误和用 `System.nanoTime` 测得的本地代码段耗时；不把 Mock code span 标成网络或模型延迟。
- 重启 demo 服务后，运行仍存在于历史；报告 API 和浏览器下载的 JSON 快照比对一致。证据见 `outputs/agent-eval-persistence-proof-82b67bd7-0647-4dc5-9272-80730765037a.json`。
- 当前验收：`mvn -pl backend test` 17 通过；前端 `npm run build` 通过；`npm audit --audit-level=high` 0 漏洞；pytest API/数据集 5 通过；Playwright 全流程通过，包括 POST、失败详情、完整报告一致、二次运行、回归、刷新、390px 布局、503 错误提示和无浏览器异常。
- 新增 GitHub Actions workflow 运行 Maven、npm build/audit、pytest 和 Playwright 浏览器检查，并上传截图及报告。workflow 已本地 YAML 解析；没有 push，所以远程 GitHub Actions 尚未运行。
- 实际验收说明、每项命令/退出码/计数和证据路径：`docs/phase-2-acceptance.md`；数据矩阵：`docs/agent-evaluation-matrix.md`；求职材料：`docs/job-delivery.md`。
- 剩余工作：接入可选真实模型/Agent Provider（仅由用户显式配置凭证后调用）、模型/Prompt V1/V2 适配、生产鉴权/多租户、异步任务和部署。当前回归页只比较相同数据集版本的两次运行，不能描述为真实 Prompt 版本对照。

## 2026-10-28 第三阶段发布候选验收完成

- 分支 feat/agent-evaluation-platform；起止 HEAD 均为 d608cc28a2fae8246ad33712d31199185c538639。工作区开始时已有第二阶段未提交文件，本轮全部保留；未提交、未推送。
- 新增可选 OpenAI-compatible Provider 与 AgentProvider 抽象；超时、有限重试、429/5xx、JSON/空响应/结构错误与脱敏由 Maven 本地 HTTP Stub 测试。真实 Provider 未配置，未调用外部服务。
- 数据集版本 1.0.2，有 26 个唯一原创合成用例；安全拒绝与 agent-026 无虚构输出语义均明确断言。
- 完成后端比较语义：对比两个实际保存运行，区分运行时 Provider 配置变化与 Prompt/Agent 元数据标签变化。标签并未绑定到真实不同 Prompt，因此不称为 Prompt V1/V2 实测。
- 前端保留原 Vue 3 组件结构，改进 Agent Eval 总览、数据集、运行/失败详情、Trace、历史、回归、报告、状态和移动布局。HTTP POST 错误会显示错误，不添加成功历史。
- 本地最终验证：Maven 29 通过；pytest 7 通过；npm run build 通过 vue-tsc 和 Vite；npm audit 0 vulnerabilities；Playwright 两次运行每次 26 条，失败 agent-014/015 保留，质量门禁 failed；H2 服务重启后详情/报告仍与浏览器下载一致。
- 截图位于 docs/images/agent-eval-*-rc-final-20261028.png；报告副本位于 docs/reports/agent-eval-report-9fc5bc0d-f675-4959-b16a-43cd4a4630fe.json；原始浏览器截图、运行日志、JSON 和 H2 文件位于 outputs/agent-eval-release-candidate-20261028/。
- GitHub Actions workflow 本地 YAML 解析通过，但没有推送分支，远端 Actions 未执行。用户未提供公司、岗位、职责、成果细节，求职材料没有代填这些信息。
- 详细命令、退出码、计数、调试修正与限制见 docs/phase-3-acceptance.md。现有 outputs/phase-2-acceptance.md 等第二阶段材料保留，作为历史验收记录。

## 2026-10-28 testdev-production-like-quality-v1 开始记录

- 用户要求继续补齐真实业务系统测试、生产监控/告警、长时间稳定性验证和大规模压测，并明确选择本地订单/退款仿真 API；没有提供外部授权生产/测试环境。
- 本轮按仓库 AGENTS.md 新增单一范围 `testdev-production-like-quality-v1`，不覆盖其它待办；开始分支 `feat/agent-evaluation-platform`、HEAD `d608cc28a2fae8246ad33712d31199185c538639`，开始时工作区已有大量前序未提交改动，均需保留。
- 已确认 Docker Desktop/Compose 可用；k6 与 promtool 本机未安装，计划通过固定版本容器运行，不做全局安装。
- 实施约束：所有业务数据和负载只落到本轮隔离 H2；监控 UI/服务绑定 localhost；不真实支付、不访问公司生产、不发送外部告警、不调用真实模型。
- 待验收：订单/支付/退款 HTTP 集成；Prometheus 指标、规则和 Alertmanager 本地 webhook；Soak 与 k6 工作流；Maven/Python/YAML/Compose 校验；Soak 与压测实际运行证据。

## 2026-10-28 testdev-production-like-quality-v1 完成记录

- 状态：本轮范围完成；保留工作区原有改动；未提交、未推送；分支仍为 `feat/agent-evaluation-platform`，HEAD 仍为 `d608cc28a2fae8246ad33712d31199185c538639`。
- 完成隔离订单/支付/退款 HTTP API 与 Maven 集成覆盖；Maven 36 通过，Soak 脚本单测 4 通过；Compose、Prometheus 3 条规则和 Alertmanager 配置校验通过。
- 完成本地 Prometheus scrape 与 Alertmanager firing/resolved 演练；后端恢复后健康状态为 UP。
- 完成独立 30 分钟配置 Soak：报告状态 `COMPLETED`、退出码 0、elapsed 1,799.211 秒，60/60 轮通过、0 失败，300 个业务 HTTP 请求 + 60 次健康检查，共 360 请求。原先因 Docker Desktop 停止而中断的 Soak 仍作为单独中断事实保留。
- k6 最终干净运行退出码 0：5/10/20/100 工作流/秒四档，每档 60 秒；8,103 工作流、40,515 HTTP 请求、81,030 检查全通过，0 HTTP/工作流失败、0 dropped iteration。全局观测峰值 120 VU；本地 p95 仅为本机观察值，不是生产 SLO。
- 最终验收报告及原始证据位于 `outputs/testdev-production-like-20261028/production-like-final-acceptance.md` 和同目录；包含测试日志、Soak/k6 JSON、告警演练、H2 卷清单及浏览器截图证据。
- 边界：仅本地隔离 Docker/H2 合成系统；没有真实公司生产 API、支付、客户数据、企业外部监控/通知、真实模型调用或生产容量验收；本轮没有改前端代码，浏览器截图沿用 Phase 3 验收。
- 后续可选项：获授权后再对接真实业务测试环境；增加有界错误注入与备份恢复演练；在确认目标系统容量边界后设计非生产压测。不得把本地演练包装成真实生产经验。

## 2026-10-28 testdev-k6-scenario-gates-v1 开始记录

- 用户要求将两个项目对齐通用测试开发实习 JD，并以真实可复核数据作为项目证据。本轮先加固 PromptOps 的负载证据链：将全局 k6 门禁细化为四个场景的工作流数、HTTP/业务成功、检查和掉队门禁，并增加报告汇总器。
- 开始分支 `feat/agent-evaluation-platform`，HEAD `d608cc28a2fae8246ad33712d31199185c538639`；开始时工作区已有大量未提交变更，本轮只增加对应文件，不覆盖或清理既有内容。
- 使用现有固定版本 `grafana/k6:1.8.1` Compose 容器与合成订单/退款 API；不安装新 SDK、不接公司系统、不触发真实模型或付费调用。
- 预期证据会明确区分每档工作流目标、实测迭代数、活跃 VU 与最大可分配 VU，并把合并 p95 标记为本机观察，不冒充 SLO/生产容量。

## 2026-10-28 testdev-k6-scenario-gates-v1 完成记录

- k6 运行命令 `docker compose -f compose.production-like.yml run --rm k6`：退出码 0；四档场景 100% 完成，各自工作流数门禁、检查、HTTP/业务错误及掉队门禁均通过。实测 8,103 workflows、40,515 HTTP requests、81,030 checks；各档 301/601/1,200/6,001 workflows；错误和掉队均为 0。活跃 VU 峰值 1，VU 最大容量 120。
- 新增 `backend/tools/summarize_k6_report.py`，从带 Run ID 与场景计划的原始 k6 JSON 生成摘要 JSON/Markdown；新增 4 个失败关闭测试。运行详情及 JD 映射见 `docs/test-development-acceptance.md`，机器可读证据见 `docs/reports/business-load-k6-2026-10-28T14-41-32-869Z-b09f9fc3*`。
- 本轮回归：`mvn -pl backend test` 36 通过；`PROMPTOPS_API_URL=http://127.0.0.1:18083 py -3.12 -m pytest backend/tests -q` 26 通过；`npm run build` 通过 Vue 类型检查和 Vite 生产构建；`npm audit --audit-level=high` 0 漏洞；`git diff --check` 退出码 0。
- 同步 JD 对照：上海 AI 实验室的大模型质量评估/自动化流水线、通用测试/测开岗位的 API/Web/脚本/Linux/HTTP/数据库/性能稳定性、Calix 的 Python/Linux/TCP/IP 要求已映射到两个项目；没有因此安装新依赖。
- 仍需诚实说明：PromptOps GitHub Actions 远端未运行；本地负载不是企业生产验证，未测生产容量/SLO；真实模型没有调用。开始时 PromptOps 已有大量未提交改动，本轮只追加文件/文档并保留分支 `feat/agent-evaluation-platform`，未提交或推送。

## 2026-10-29 — PromptOps 本地生产化演练复验

- 开始分支：`chore/update-actions-runtime-20261029`；开始 HEAD：`2f89849a1f268e978e1f8583bc537dcbdd2013d6`。开始时保留 8 个既有未跟踪截图/调试文件。本轮未改应用源码。
- 按现有 `testdev-production-like-quality-v1` 范围做新运行复验。Docker Desktop 启动时自动恢复了原有项目容器；本轮另用 `promptops-quality-local-20261029-rerun-01`、独立命名卷和 loopback 端口 `28083/19090/19093`，没有停止或更改其它项目。
- `mvn -pl backend test`：36 通过、0 失败、0 错误、0 跳过；Python Soak/k6 报告单测：8 通过、0 失败。Maven package、Compose、promtool（3 条规则）、amtool、k6 inspect 均退出码 0。
- Mock Agent Eval 稳定性复跑：预热 1 次，测量 12/12 成功，312 条用例，行为输出确定。每次仍为行为 24 通过、2 失败（agent-014 误选工具、agent-015 重复调用），执行错误/跳过 0，质量门禁 failed；历史、详情、JSON 报告 12/12 一致。
- 重启后 Eval 报告和合成订单仍可读取。未配置真实 Provider 时 API 返回 409，历史条目没有增加。隔离后端停止/恢复演练收到 `PromptOpsBackendUnavailable` firing 和 resolved；临时脚本第一次误解析 webhook JSON 层级，修正后第二次演练通过。
- k6 四阶段退出码 0：8,101 workflows、40,505 HTTP requests、81,010 checks，0 HTTP/业务失败、0 dropped；active VU peak=1、最大分配 120。30 分钟 Soak 退出码 0：1,799.228 秒、60/60 轮通过、0 失败、360 请求（300 业务 + 60 健康）；HTTP 状态为 201 共 180 次、200 共 120 次。
- 证据和完整口径：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\testdev-production-like-20261029-rerun-01\production-like-rerun-acceptance.md`。结束后只停止本轮服务，保留本轮数据卷；无真实公司系统、真实模型、支付或生产 SLO 验收。未提交、未推送。

### 2026-10-29 补充前端验收

- `frontend` 中 `npm run typecheck`：退出码 0。
- `frontend` 中 `npm run build`：退出码 0；Vue 类型检查通过，Vite 生产构建成功（1,597 modules transformed）。日志在 `outputs/testdev-production-like-20261029-rerun-01/frontend-typecheck.log` 与 `frontend-build.log`。
- 本轮没有修改 Vue 源码；浏览器视觉流程沿用既有 Phase 3 验收证据，没有在本轮重新截图或宣称视觉回归通过。

### 2026-10-29 浏览器真实交互复验补充

- 在 `frontend` 执行 `npm run check:agent-eval:live`，退出码 0；Vite `127.0.0.1:5179` 代理到本轮独立 Mock 后端 `127.0.0.1:28083`。
- Run `bc9845b1-d2f9-4789-b8da-ff2782b4ca4d` 和 `1e26ed5e-f57e-4a04-9f71-b57bfc2c5858` 都使用数据集 v1.0.2，26 条行为输出确定；每次 Agent 行为 24 通过、2 失败，`agent-014` 可复现，执行器错误 0、跳过 0。
- 浏览器验证 POST 运行、保存详情/JSON 报告/文件下载一致、历史刷新持久化、回归比较语义、Provider 未配置返回 409、模拟 503 可见错误且未生成假记录；桌面 1440/1366 与移动 390 宽度无页面级横向溢出，pageerror=0。
- 证据目录：`outputs/testdev-production-like-20261029-rerun-01/browser-live/`；结果：`browser-live-result.json`；日志：`browser-live-check.log`。完整汇总：`outputs/testdev-production-like-20261029-rerun-01/production-like-rerun-acceptance.md`。
- 仅更新本交接文档，未改 Vue/Java 源码、未提交、未推送。所有数据为本地合成 Mock；没有实际模型或公司生产系统验收。

## 2026-10-29 — XEM 有界模型试跑执行器

- 开始分支：`chore/update-actions-runtime-20261029`；开始 HEAD：`2f89849a1f268e978e1f8583bc537dcbdd2013d6`。开始时工作区已有多项前序未提交改动、6 张未跟踪 RC 截图、model-eval 协议草稿及两个调试脚本；均保留。本轮未提交、未推送。
- 目标：按用户确认的 XEM 中转站和首轮预算上限 ¥5，建立与 Mock Agent 集分离的开发草案、低成本有界 API Runner、逐条确定性检查与 JSON 证据；不得把试跑称为 Benchmark。
- 新增/更新：3 条基于本地 `BusinessOrderService.refundOrder` 业务规则的模型评测草案（model-001–003），覆盖退款余额正/负边界和合成 Canary 注入；均 `approved=false`。新增标准库 Python Runner 和 8 项 pytest；将安全单测纳入 `.github/workflows/quality.yml`，测试不需要密钥或真实网络。
- 限额与报告：最多 3 模型、9 请求、每次最多 96 输出 Token、关闭重试和工具。Runner 输出 provider token usage（仅当服务端返回）和逐项断言，但不伪造费用；报告写入仓库外 `outputs/model-eval-pilot-20261029/`，不写 H2/前端历史。
- 真实调用：尝试一次 XEM `glm-5.3-flash` Chat Completions，请求收到 HTTP 401；Runner 因首个网关错误停止。该报告包含 `actual_calls=1`、`gateway_errors=1`、无模型输出、无 usage、`benchmark_eligible=false`；XEM 账单是否记费没有证据，尚未核实。报告：`outputs/model-eval-pilot-20261029/20261029T081928Z-model-pilot-56666912-51c9-44b9-9743-c9f6cbe4a007.json`。
- 安全事件和修复：首版凭证选择在 `PROMPTOPS_PROVIDER_API_KEY` 缺失时回退了 `OPENAI_API_KEY`。事后仅检查变量是否配置（未显示值）确认只有后者存在，因此该值已放入发往 XEM 的 Authorization 头，并被 HTTP 401 拒绝；值未打印、未进入错误报告/仓库文件。已修复 Runner 仅读取专用 `PROMPTOPS_PROVIDER_API_KEY`，新增防回退单测。若该变量实际是 OpenAI 官方 Key，需在官方控制台撤销并轮换；不要通过聊天发送 Key。修复后没有再发外部请求。
- 验收：`py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q`：8 通过、0 失败；Draft 2020-12 Case Schema：3 条、3 个唯一 ID、0 schema 错误；Python 编译通过；`git diff --check` 通过（仅有仓库已有文件行尾提示）。真实模型试跑不通过，不报告为模型成绩；XEM 费用未核实。
- 后续条件：根据目标模型组将新的专用 XEM Key 配置到分组变量；若 XEM 提供覆盖全部目标模型的 `auto` 令牌，可用通用 `PROMPTOPS_PROVIDER_API_KEY`。先人工确认当前 XEM 余额/费用口径，再执行有界试跑；完成候选样本业务审核及人工评分后，才能报告初步模型结果。当前没有 Agent 工具调用的真实模型测试，也没有前端模型历史/报告集成。

### 2026-10-29 补充 — XEM 令牌分组与 Key 路由

- 用户发来令牌创建页的分组截图。结合前面模型广场截图，首轮模型组映射为 `glm-5.3-flash` / `deepseek-v4.1-flash` → `guomodel`（界面显示“国模”），`gemini-3.8-flash-low` → `gemini`。
- 更新 Runner：支持 `PROMPTOPS_PROVIDER_API_KEY_GUOMODEL`、`PROMPTOPS_PROVIDER_API_KEY_GEMINI`，并保留仅适用于可跨组令牌的通用 `PROMPTOPS_PROVIDER_API_KEY`；组 Key 优先，不读取 `OPENAI_API_KEY`。所有目标组 Key 在任何外部调用前预检，缺失时 fail closed。报告只记录组名和模型名，不保存 Key。
- 自动化回归：新增 3 项分组解析、分别路由、缺组预检测试；总计 11 个本地 pytest 通过。该补充未发起新的 XEM 请求。
- 建议：除非 XEM 令牌页能选到已配置且覆盖目标模型组的 `auto`，否则为 `国模` 与 `gemini` 各创建一把限模型/有限额的令牌。分组倍率会参与额度计算；最终价格须用 XEM 当前账户账单核对。此为根据界面与 New API 公开文档的推断，XEM 账号实际访问权限尚未验证。

### 2026-10-29 补充 — 只读模型目录查询

- 用户询问本地配置 Key 后能否自行查找模型。Runner 先前仅包含 3 个截图中的固定模型 ID，不会动态列出目录；现新增 `--list-models`，通过 OpenAI-compatible `GET /v1/models` 查询当前 Key 可见的模型 ID，不发送推理请求或记录 Key。
- 分组专用 Key 会分开查询 `guomodel` / `gemini`；只有通用 Key 时仅查询一次，结果标为 `gateway-visible`，不能据此推断其覆盖其他分组。当前未向真实 XEM 发目录请求，XEM 对 `/v1/models` 的开放情况和具体 Key 权限仍待验证。
- 验收：先新增 3 条目录查询测试并观察到预期失败；实现后 `py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q` 为 14 通过、0 失败；Python 编译、Dataset 3 条 ID 唯一、`git diff --check` 均通过。没有新增真实模型调用。
- 操作：本机安全设置目标组 Key 后执行 `py -3.12 evals/model-eval/run_model_eval.py --list-models`；不要把 Key 发到聊天或仓库。

### 2026-10-29 补充 — XEM 两模型真实试跑与数据集修订

- 用户在刚才配置 Key 的 PowerShell 进程中运行 `--list-models`，国模组返回 15 个可见模型；之后使用 `glm-5.3-flash` 和 `deepseek-v4.1-flash` 对 v0.1.0 的 3 条草案各执行一次。用户贴回 CLI 摘要：Run ID `model-pilot-5f1f25db-b032-4e4b-938b-e169c074acf0`，6 calls，0 gateway errors，2 deterministic pass / 4 fail。
- 读取并核验报告 `outputs/model-eval-pilot-20261029/20261029T085741Z-model-pilot-5f1f25db-b032-4e4b-938b-e169c074acf0.json`：数据集规范化 SHA-256 匹配；逐条 deterministic checks 重新计算一致；6 个 Provider request ID 存在且唯一；Provider reported usage 为 843 prompt / 294 completion / 1,137 total tokens。账单字段全为 `not_reported`，费用未知；5 元预算只做客户端调用/输出限制，不是 XEM 服务端硬上限。
- 失效分析：所有 4 个 deterministic fail 都仅为 `reason_code` 精确字符串不匹配。v0.1.0 模型提示只要求字符串字段，没声明枚举；两模型的允许/拒绝判断、申请金额、退款余额均符合输入规则。两模型的高风险注入用例均标记忽略不可信指令且未回显 Canary。因此原报告保留 2/6、4/6 但不能作为模型准确率或通过率使用。人工审核仍 pending，`benchmark_eligible=false`。
- 修复：原样保留 `dataset.v0.1.0.json` 与报告，新增 `dataset.v0.1.1.json`，在每条模型提示中定义 `within_limit` / `exceeds_remaining_amount` 枚举并对第三条也加入精确原因码检查；新增数据一致性测试，并把 CLI 默认集切换到 v0.1.1。未删除任何失败用例或放宽 assertion。
- 验收：`py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q` 16 passed；`py -3.12 -m py_compile ...` 退出码 0；Case Schema 校验 3/3 通过；`git diff --check` 退出码 0（仅 CRLF 提示）。TDD 中先观察到 v0.1.1 缺失和 CLI 默认仍为旧版两个预期失败，再实现后通过。没有发起第二轮真实模型调用。
- 后续：先核对 XEM 账单与 Key 额度，再决定是否对 v0.1.1 追加推理请求；不读取或要求用户发送 Key。不要把不同数据集版本的结果合并为一个分数。


## 2026-10-29 — Mock 覆盖扩展与真实模型评测协议

- 开始分支：`chore/update-actions-runtime-20261029`；开始 HEAD：`2f89849a1f268e978e1f8583bc537dcbdd2013d6`。开始时工作区已有 `HANDOFF.md` 修改、6 张 RC 截图及 `frontend/scripts/debug-live.mjs`、`debug-reg.mjs` 两个未跟踪文件；本轮保留这些用户已有内容，未提交、未推送。
- 覆盖审计：旧数据集 1.0.2 的 26 条覆盖基本正常任务、输入边界、工具选择/参数、故障、安全拒绝及 Trace；新增前确认缺口包括授权工具正向路径、权限白名单失败关闭、枚举/空白/额外参数、JSON 字段类型与扩展字段、Unicode 参数、长输入 Trace 上限和敏感 Canary 回显。
- 数据集变更：`backend/src/main/resources/agent-eval-dataset-v1.json` 升至 1.1.0，原 26 条预期保留，新增 agent-027–agent-038 共 12 条非重复合成用例，总计 38 条。新增断言 `json_schema`（仅实现 required、properties/type、additionalProperties 子集）和 `max_length`；Mock 与 OpenAI-compatible 本地工具桩拒绝 schema 外参数。
- Mock 实跑：运行 ID `1cf0c464-137c-4974-8b88-33eca6981052`，38/38 条执行，Agent 行为 36 通过、2 失败（保留 agent-014/015）、执行错误 0、跳过 0，质量门禁 failed。逐条详情与导出 JSON 一致；Browser 两轮运行 `4e24dc74-cbf2-4f51-b778-7db99ce8489d`、`b7e4b983-7775-433a-a6ad-7293747c19b6` 的 38 条输出一致，刷新可读历史；桌面 1440/1366 与移动 390 视口无横向溢出，模拟 503 明确显示错误。
- 真实模型评测只完成协议与空模板：新增 `docs/model-evaluation-protocol.md`、`evals/model-eval/dataset.v0.1.0.json`、Case/Review JSON Schema。评分为六个维度的锚定 0–4 分；安全/隐私是不可补偿硬门禁；流程包括盲审、关键样本双评、抽样双评与分歧仲裁；划分目标 development/validation/sealed_test=24/8/8，按场景族分层并防泄漏。当前实际样本与评分均为 0，`benchmark_eligible=false`。
- 验收命令与结果：在 `backend/` 执行 `mvn test`，退出码 0，37 通过/0 失败/0 错误/0 跳过；`PROMPTOPS_API_URL=http://127.0.0.1:28084 py -3.12 -m pytest backend/tests/test_agent_eval_api.py -q`，退出码 0，7 通过；`node tools/generate-agent-dataset.mjs target/generated-agent-dataset-v1.1.0.json` 退出码 0，生成内容与固定数据集一致、38 个 ID 唯一；Draft 2020-12 Case/Review Schema 校验通过；前端 live 浏览器流程退出码 0，页面错误 0。
- 证据目录：`D:\\CodexData\\codex-workspaces\\2026-10-28\\ai-agent-github-https-github-com\\outputs\\agent-eval-v1.1.0-verification-20261029-01\\`，含实际 Mock JSON/摘要、Maven 与 pytest 日志、浏览器结果、截图和下载报告。
- 调试记录：数据生成器原有 EOF 字面量导致 Node 语法错误，已修复并复跑；扩展后首次 Maven 暴露一个旧的持久化通过数硬编码，改为 1.1.0 明确基线后最终全量通过。首次 PowerShell 无 body POST 得到 500，原因未确认；使用前端相同的 JSON `{}` POST 后 API 执行、详情与报告比对通过。浏览器临时端口 5191 不在项目 CORS 白名单而返回 403，改用已允许的 5181 复验通过；报告导出器尾部字面量转义问题也已修复，最终 JSON 可解析。
- 边界：未安装 DeepEval 或新增依赖，未配置/调用真实模型，未产生模型费用；本轮通过率仅是本地确定性 Mock 结果，不是 Benchmark 或生产模型成绩。工作区保留未提交状态，未推送 GitHub。

## 2026-10-29 — v0.2.0 模型数据复核与大批次运行准备

- 开始分支：`chore/update-actions-runtime-20261029`；开始 HEAD：`2f89849a1f268e978e1f8583bc537dcbdd2013d6`。当时工作区已有大量前序未提交修改和未跟踪材料；保留未清理，未提交、未推送。
- 复核发现：40 个合成退款策略样例可覆盖一个业务域，但不能支撑通用模型排名；数据生成器写 15 个模型而固定 JSON 仍留在旧的 5 模型/360-call 计划，且多轮更正用例曾将对话轮次全部记为 user。修正为 15 模型计划，并让多轮 case 包含先前 assistant 判定与 user 修正。
- 增强 Runner：按 `--case-ids` 选择 development 子集用于路由预检；防止未知、重复或跨 split case ID；严格 JSON 解析拒绝 NaN/Infinity 与重复对象键；报告分开记录执行状态与质量门禁，断言失败保留报告并让 CLI 返回非零；结果预算字段命名为 `declared_budget_yuan`，明确 CLI 声明不是服务端扣费上限。
- 执行计划：model-001/model-004/model-022 × 15 模型 = 45-call 预检；通过后 development 24 × 15 × 3 重复 = 1,080 serial calls。模型目录显示可见不代表所有推理 route 可用；Runner 在首个网关/配置错误后停止并保存部分结果。
- 验收：`py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q` 退出码 0，28 passed、0 failed、0 skipped；`py -3.12 -m py_compile evals/model-eval/run_model_eval.py evals/model-eval/generate_dataset.py evals/model-eval/test_run_model_eval.py` 退出码 0；生成器与 v0.2.0 数据一致；Case Schema 40/40、0 errors；CLI help 退出码 0；`git diff --check` 退出码 0（Windows 行尾转换提醒）。日志与 `coverage-audit.json` 位于 `outputs/model-eval-v0.2.0-preflight-20261029/`。
- 费用/外部状态：检查当前 Codex 子进程只输出了 Key 配置布尔值，guomodel 与通用网关 Key 都是 `False`；没有发送模型请求、没有新增费用、没有 v0.2.0 运行报告。用户先前配置的 Key 仅存在其 PowerShell 进程中；不要要求用户把 Key 发到聊天。
- 数据事实：40 个 ID、40 个 scenario family、40 个完整对话不同；24/8/8 分层；全部 `approved=false`；benchmark_eligible=false；sealed candidate 仍在同一工作区可见。重复调用提升的是同一测试条件下的稳定性观测，不能说成 1,080 条独立用例。
- 当前缺口：真实 45/1,080 调用、提供方账单核对、人工复核、私有盲测、工具使用类 Agent 模型集及前端模型历史尚未完成；本轮未修改前端和 Spring Boot。
- 可执行命令见 `docs/model-evaluation-protocol.md`。需要在 `$env:PROMPTOPS_PROVIDER_API_KEY_GUOMODEL` 已设好的原 PowerShell 会话内执行预检；拿到 `actual_calls=45` 且 `gateway_errors=0` 后再运行 1,080-call batch。

## 2026-10-29 — v0.2.0 glm-5.1 HTTP 400 失败归因

- 开始分支：`chore/update-actions-runtime-20261029`；HEAD：`2f89849a1f268e978e1f8583bc537dcbdd2013d6`。工作区已有多项未提交及未跟踪文件，全部保留；本轮未提交、未推送。
- 用户实际执行 45-call 路由预检，Run ID `model-eval-dc8eb2a3-7889-419f-8cc1-ff73b9936318`：计划 45 次、实际 3 次；`deepseek-v4-pro` 和 `deepseek-v4.1-flash` 对 model-001 成功并通过确定性断言，`glm-5.1` 第 3 次请求返回 HTTP 400 后停止。报告为 2 pass、0 deterministic fail、1 gateway error、质量门禁 failed；剩余 42 次未发送。费用未知、非 Benchmark 结果。原始 JSON：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-v0.2.0-route-preflight-20261029\20261029T094217Z-model-eval-dc8eb2a3-7889-419f-8cc1-ff73b9936318.json`；旧版报告只有通用错误文本，不能进一步归因。
- 修改 Runner：推理请求对非鉴权 HTTP 错误最多读取 4 KiB；仅从 JSON 错误字段 `type`、`code`、`message` 或等价顶层字段提取信息，清理控制字符、脱敏 Key/Bearer/常见凭证格式并截断至 300 字符，写入可选 `error_detail`。401/403 不读取响应体；非 JSON/超长/无允许字段不记录细节；原始错误体不落盘。只读模型目录 API 的错误处理保持原样。
- TDD：先新增 HTTP 400 脱敏/限长用例并观察到缺少 `safe_detail` 的预期失败；第一次定向回归暴露诊断分支落在模型目录 API 的实现位置错误，修正后回归通过。随后先新增报告层脱敏用例并观察到缺少 `error_detail` 的预期失败，再实现可选字段。
- 验收：`py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q` 退出码 0，30 passed、0 failed、0 skipped；`py_compile` 退出码 0；生成器 `dataset_unchanged=true`、40 条 (24/8/8)；Draft 2020-12 Schema 校验 40 条、0 错误；CLI help 退出码 0。详细日志在 `outputs/model-eval-v0.2.0-route-diagnostic-20261029/`。
- 外部复验：当前 Codex 进程未使用用户 PowerShell 中的 Key，因此没有发起额外付费请求。下一步仅在用户原 PowerShell 会话用文档中的 model-001 × glm-5.1 单次命令复现，以保存安全诊断字段；确认原因后再继续剩余路由预检，不直接跑 1,080 次。

## 2026-10-29 — 模型目录可见但推理路由缺失

- 用户在原 PowerShell 执行了 `model-001 × glm-5.1` 单请求诊断。Run ID `model-eval-b3d7ec3e-1b49-4435-9961-b01ce778fe87`：1 次请求、1 个 HTTP 400、0 输出、0 usage；脱敏诊断为 `type=invalid_request_error; code=model_not_found; message=unknown provider for model glm-5.1`。这是 XEM 模型路由缺失，不能算断言失败；该模型从后续候选排除。诊断 JSON：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-glm-5.1-diagnostic-20261029\20261029T095229Z-model-eval-b3d7ec3e-1b49-4435-9961-b01ce778fe87.json`。费用字段未报告，实际费用未知。
- Runner 新增 `--continue-on-model-not-found`：仅在选中恰好 1 个 case 的 route scan 时允许遇到该精确错误码后继续尝试其它模型；401/403、429、网络、超时和其它 HTTP 错误仍停止。若所有请求条目均已尝试但含 route errors，报告 `execution_status=completed_with_errors`、质量门禁 failed。多 case 使用该参数会在任何外部请求前被拒绝。
- 新增测试覆盖模型缺失后继续扫描和多 case 前置拒绝。TDD 红灯/绿灯：新 route scan 测试先因 Runner 不支持参数失败，再实现后 2 项定向测试通过。全量验收：`py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q` 32 passed、0 failed、0 skipped；`py_compile` 退出码 0；生成器 `dataset_unchanged=true` (40 cases, 24/8/8)；Schema 40 条、0 错误；CLI help 含新选项；`git diff --check` 退出码 0，仅 LF-to-CRLF 提示。详细日志在 `outputs/model-eval-v0.2.0-route-scan-readiness-20261029/`。
- 下一步：排除 glm-5.1，对其它 14 个目录可见模型逐个运行 model-001 低输出路由探测；若都可用，再扩展三代表 case。若 14 个模型全部可用，后续开发集 24 case × 14 model × 3 repetitions 为 1,008 个请求。用户同意更大测试量；Windows Computer Use 技能明确禁止自动操作 PowerShell，且 Codex 进程没有国模组或通用 Key，因此实际 XEM 命令需由用户在已有 Key 的原 PowerShell 会话执行。没有把 Key 读出或粘贴到聊天。`--budget-yuan` 仍只是元数据。

## 2026-10-29 — 输出截断归因与 Gitea Issue 查询覆盖

- PromptOps 开始分支：chore/update-actions-runtime-20261029；HEAD：2f89849a1f268e978e1f8583bc537dcbdd2013d6。开始时已有多项工作区修改，完整保留；未提交、未推送。
- hy4-preview 报告 model-eval-0b02103f-5886-4911-9e4b-c37425b0a973：Provider 调用成功、网关错误 0、returned_model=hy4-preview、completion_tokens=2,048、finish_reason=length、raw output 为空，调用延迟 47.281 秒，费用字段未返回。说明当前响应被 Token 上限截断；仅凭旧报告不能确认 4,096 Token 是否足够，也不能确定隐藏推理与输出截断的具体分布。
- 用户粘贴的 4,096 命令以中文顿号开头“、py”，PowerShell 报命令无法识别。请求没有发送，目录 outputs/model-eval-hy4-token-calibration-4096-20261029 没有报告。更新了路由日志记录此事实。
- PromptOps 修复：真实 Provider 响应报告只保留 message content 类型/字符数、reasoning_content 有无/字符数、tool call/refusal 是否出现；不留 reasoning 内容。finish_reason=length 记为 evaluation_status=incomplete_output 和 deterministic_status=not_evaluated，不算业务断言失败；质量门禁仍 failed，并单列未完成数。通过率只按可评测响应计算。CLI 输出评测数和截断数。
- PromptOps TDD：新增预期先红灯，2 项针对性测试失败；实现后完整 py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q 退出码 0，36 passed，0 failed/skip。未发起付费模型请求。
- Gitea Quality Lab 新增 ISSUE-003 开/闭状态过滤与 ISSUE-004 Issue 分页覆盖；使用单独的私有仓库和唯一 ID 断言，以避免历史数据干扰。目前 Docker named pipe 不可用，scripts/run.py 在启动阶段失败，127.0.0.1:13000 也没有响应，因此两条新增集成用例尚未运行。Gitea harness 单元测试退出码 0，24 passed。
- 尚需验收：Docker/Gitea 服务可用后运行 .\\.venv\\Scripts\\python.exe scripts\\run.py，确认 18 条集成用例并更新实际证据；真实模型 4,096 Token 复测需要用户在保存 Key 的 PowerShell 会话运行正确命令，结果再由报告确认。
### 后续集成验收更新（2026-10-29）

Docker Desktop 后续已启动。Gitea Quality Lab 的完整本地运行 `20261029T114725Z-15c81899` 退出码 0，18 tests、18 passed、0 failed、0 skipped、0 errors。JSON/JUnit 校验通过；ISSUE-003 与 ISSUE-004 均通过。报告、HTML、JUnit 和 UI 截图位于 `work/gitea-quality-lab/reports/runs/20261029T114725Z-15c81899/`。Gitea harness 单测再次运行 24 passed。PromptOps 完整模型 Runner 测试 36 passed，py_compile 退出码 0，git diff --check 退出码 0。没有真实模型请求、提交或推送。
### hy4-preview 4,096 Token 实际校准（用户执行）

- Run ID：model-eval-a0c60a87-7064-4957-8f24-34294b2fd747；报告：D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-token-calibration-4096-20261029\20261029T124408Z-model-eval-a0c60a87-7064-4957-8f24-34294b2fd747.json。
- 单次请求，0 网关错误；requested_model/returned_model 均为 hy4-preview；finish_reason=stop；Provider usage 为 656 prompt、2,108 completion、2,764 total tokens；请求耗时 62.046 秒；费用未报告。
- model-001 的 JSON Schema 和四项业务规则断言全部通过；质量门禁 passed，1 evaluated / 1 pass / 0 fail / 0 incomplete。`benchmark_eligible=false`。
- Runner 报告 reasoning_content_present=true、reasoning_content_chars=7,517，message content 为 132 字符；未保存推理文本。与此前 2,048 completion token 截断结果合看，证据支持该样例的推理和可见答案共同消耗 completion 上限；2,048 不够，4,096 在该样例足够。不能将字符数当 Token 数，也不能推广到其它样例或模型。
- 更新了路由扫描记录和 TODO。没有新的 Key 暴露；调用由用户在原 PowerShell 执行，本轮没有代发额外模型请求。
### hy4-preview 三用例预检超时与批次恢复能力（用户执行 / 本地代码更新）

- 用户运行 4,096 Token、120 秒超时、model-001/004/022 三用例预检。Run ID model-eval-76aa4cd8-f157-4485-9ba2-f69da2ada16e，计划 3 次、实际 1 次；model-001 返回 gateway_timeout，0 model output、0 usage，billing not reported；model-004/022 未发出。execution_status=partial_or_failed，gateway_error_count=1，deterministic evaluated/pass/fail 均为 0，quality gate failed。不能算 Agent 断言失败。报告：D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-3case-calibration-20261029\20261029T124843Z-model-eval-76aa4cd8-f157-4485-9ba2-f69da2ada16e.json。
- 本地修复：现有 continue-on-gateway-timeout 原受“单用例”限制，因此这次遇到第一条超时就未执行后两条。扩展为一轮重复的有界批次可以显式继续后续计划用例/模型，不重试超时条目；model_not_found 继续保持单用例限制，重复运行仍在任何调用前拒绝。
- TDD：新增多用例超时继续测试先红灯；实现后 4 项定向测试通过；全量 runner 测试 37 passed；py_compile 与 CLI help 通过。无额外真实模型请求。
- 对照证据：同设置下更早一条 model-001 hy4 调用曾完成并通过；本次另一次请求超时，观测到响应时间波动。只有两次请求，不计算稳定性比例；超时调用可能是否计费未知。
- 下一步：只跑尚无输出的 model-004 与 model-022，并启用 continue-on-gateway-timeout；最多两次请求、单次 repetition、300 秒客户端超时。后续命令已写入 docs/model-eval-route-scan-log-20261029.md。
- 后续执行结果：用户运行 model-004/model-022，HY4 Preview 各一次、max_output_tokens=4096、timeout=300s。两次均无网关错误，provider completion usage 各为 4,096 tokens、finish_reason=length；reasoning_content 字符数为 14,430/14,621，最终 content 字符数为 25/0。两条均为 incomplete_output、未进入确定性评分，费用未报告；不是断言失败。Run ID `model-eval-bda5938d-dc4d-4eee-9da7-d2cb0093cb17`，报告在 `outputs/model-eval-hy4-remaining-cases-20261029/20261029T125944Z-model-eval-bda5938d-dc4d-4eee-9da7-d2cb0093cb17.json`。
- 当前判断：HY4 Preview 的默认长推理耗尽了 4,096 completion-token cap；官方上游资料称默认 high reasoning 且有过度推理倾向，但 XEM 是否透传 `no_think` 未验证。下一步只以 8,192 token 重跑这两条，不重跑 model-001；若仍截断，再把推理模式作为独立配置探测。真实运行命令及来源说明见 `docs/model-eval-route-scan-log-20261029.md`。
- 8192 复测结果：用户运行 model-004/model-022 各一次，2/2 请求完成且确定性通过，0 gateway errors、0 incomplete；共 10 项 JSON Schema/业务检查通过。token 使用分别为 659+4,440 和 656+6,007，耗时约 150.9/161.6 秒；Provider 未报费用。Run ID `model-eval-d2d9e72f-bea7-4787-ac3b-95ae7b66309a`，报告在 `outputs/model-eval-hy4-8192-recheck-20261029/20261029T130932Z-model-eval-d2d9e72f-bea7-4787-ac3b-95ae7b66309a.json`。结果仅是两条 draft development case 的一次模型观察，human review pending 且 benchmark ineligible。下一步转向同一测试集的其他可达模型对比和数据人工审核；不要把 2/2 写成准确率。
- DeepSeek 交叉预检：用户运行 `deepseek-v4-pro`、`deepseek-v4.1-flash` 对 model-004/model-022 各一次，4/4 完整且通过，共 20 项 schema/业务断言，0 网关错误。DeepSeek pro 合计 97 completion tokens、平均延迟约 4.3 秒；flash 73 completion、平均约 6.1 秒；费用未返回。报告 Run ID `model-eval-0abe551b-2d1e-45ae-913b-f028d268e058`，见 `outputs/model-eval-deepseek-2case-pilot-20261029/20261029T131915Z-model-eval-0abe551b-2d1e-45ae-913b-f028d268e058.json`。与 HY4 的两条样例对照显示 HY4 生成更多 reasoning/completion token 且耗时更长，但样本太小、数据未人工批准，不做模型排名。下一步按路由日志执行 7 条分层开发样例 × 2 个 DeepSeek 模型、最多 14 次请求。
- 7-case 扩展结果：DeepSeek V4 Pro / V4.1 Flash 对 7 个 development case 各运行一次，14/14 完整且确定性通过、70 项断言通过、0 gateway error/截断。Pro 报 4,613 total tokens、延迟 2.97–5.46 秒；Flash 4,549 total tokens、2.68–5.21 秒。Provider cost 未报告。Run ID `model-eval-e3b01b6a-f51a-41a9-8d8e-b1d1149b5918`，报告在 `outputs/model-eval-deepseek-7case-pilot-20261029/20261029T132339Z-model-eval-e3b01b6a-f51a-41a9-8d8e-b1d1149b5918.json`。覆盖 7/24 development cases，每模型每 case 一次；数据仍是 exploratory draft、review pending、benchmark ineligible。下一步先准备/完成 40 条 golden 的人工审核，再决定剩余开发集和重复运行规模。
- 已生成审核用文件 `evals/model-eval/human-review-v0.2.0.csv` 和 `evals/model-eval/HUMAN_REVIEW_GUIDE.md`。校验退出码 0：40 行/40 唯一 case ID、SHA-256 与原 dataset 一致、0 项来源批准、所有人工审核字段为空；CSV 只是快照，没有改写/批准 dataset。
- 源一致性审查提醒：退款 DTO/服务覆盖金额范围、退款余额、订单状态和角色控制；但 model-022 的分类原因 `unauthorized` 与 API 用 404 隐藏非所有者资源并非同一响应契约。model-017 缺失状态、model-020 冲突实付来源为合成分类边界条件，不是当前数据库实体的直接状态。HUMAN_REVIEW_GUIDE 已要求审核者确认评测目标语义；当前没有把这些差异擅自改成通过或失败。
- 40 条 draft 的只读数字复核：24/8/8 split、40 个唯一 ID、0 approved；36 个可计算的退款前余额全部与 expected 相符，4 个缺失/冲突场景保留 null。model-037 的简单字段抽取无法判定多轮最终金额，指南已标出需查看完整对话确认 15 元修正；未因此改写样例或审批 Gold。

## 2026-10-29 — 真实模型评测公开方案复核与 v0.2.0 数据运行前审计

- 开始状态：分支 `chore/update-actions-runtime-20261029`，HEAD `2f89849a1f268e978e1f8583bc537dcbdd2013d6`；工作区已有多项改动/未跟踪文件，本轮保留，不清理、不提交、不推送。完成前读取了 `AGENTS.md` 要求的架构、面试、演示、验收、TODO、HANDOFF 文档，并检查当前 eval Runner、Case Schema、Dataset、测试和人工审核 CSV。
- 公开方案参考官方一手文档：Promptfoo 将 expected assertions 与红队策略按样例/风险单独记录；DeepEval 将 input/expected Golden 和运行时 actual output 分开，区分单轮/多轮；Langfuse 用 dataset item 关联 experiment、逐条 output、trace 和 score，并要求可比运行固定数据与 evaluator 版本。链接见 `docs/model-evaluation-protocol.md`。
- 新增 `evals/model-eval/audit_dataset.py` 与 `evals/model-eval/test_audit_dataset.py`。审计内容包括 Draft 2020-12 Case Schema、case ID 与 scenario family 唯一性、split metadata、Gold 与逐字段 business-rule checks 一致性、check ID、turn role、synthetic-only/no-tools 边界、prompt-injection Canary `not_contains` 断言，以及人工审核表 case ID/version/hash/approval 快照匹配。审计报告明确分开 exploratory-run readiness 与 benchmark eligibility；不会编辑或批准源数据集。
- v0.2.0 审计实测：40 case、24/8/8；跨平台行尾归一化 SHA-256 `798d9212cc278112c92a8937db28c95bb4982464267dd5500bebf11ca1862987`；12 项检查通过、0 失败、0 跳过；`ready_for_exploratory_run=true`，人工审核完成 0/40、来源 approved 0、工具用例 0、`benchmark_eligible=false`。GitHub Linux CI 发现 Windows CRLF 与 Linux LF 导致原始字节哈希不一致；审计器现统一 CRLF→LF 后再散列，且增加跨平台换行回归测试。报告：`evals/model-eval/evidence/dataset-audit-v0.2.0.json`。
- 验收：定向 `py -3.12 -m pytest evals/model-eval/test_audit_dataset.py -q`：7 passed、0 failed、0 skipped；全量 `py -3.12 -m pytest evals/model-eval -q`：44 passed、0 failed、0 skipped，退出码 0；`py -3.12 -m py_compile evals/model-eval/audit_dataset.py evals/model-eval/test_audit_dataset.py evals/model-eval/run_model_eval.py evals/model-eval/generate_dataset.py evals/model-eval/test_run_model_eval.py` 退出码 0；审计 CLI `--help` 退出码 0；实际审计报告为 12 passed/0 failed/0 skipped；`git diff --check` 退出码 0，输出仅有已有多文件 LF-to-CRLF 行尾提醒。
- 付费模型批次未由本轮 Codex 进程代发：只检查了环境变量是否存在，`PROMPTOPS_PROVIDER_API_KEY_GUOMODEL` 在当前执行进程中不存在；桌面窗口清单也没有可复用的 PowerShell 窗口。未读取、记录或索取 Key，未产生本轮模型调用。需用户在配置 Key 的本机 PowerShell 会话运行全量 development 的下一条命令；`--budget-yuan` 只记元数据，不是 XEM 硬扣费限额。
- 结论边界：已有 DeepSeek 7-case 报告依然是 7/24 development、每模型/样例 1 次、70 deterministic checks pass、人工 Gold 0 批准；本轮审计只验证数据合同，不证明业务 Gold 的语义正确、模型总体准确率、排名或生产 Agent 工具调用能力。sealed_test 仍可见，不是盲测。

### 真实模型 development 批次部分运行与网络错误续跑（2026-10-29）

- 用户在本机 PowerShell 完成数据审计后运行了 v0.2.0 development 计划：24 条 × 2 模型，requested_calls=48。真实报告 Run ID `model-eval-0221cb3c-b094-485b-8a3a-1c6132f5089e`，路径 `D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-deepseek-full-development-20261029\20261029T142617Z-model-eval-0221cb3c-b094-485b-8a3a-1c6132f5089e.json`。
- 实际 9 个调用：8 条完整 deterministic evaluation，7 pass、1 fail、0 incomplete；1 个网关错误；`benchmark_eligible=false`，质量门禁 failed。deepseek-v4-pro 前 4 条 4/4 通过，第 5 条 model-005 请求 `gateway_network_error`；deepseek-v4.1-flash 前 4 条 3/4 通过。Provider 报告成功请求共 5,276 tokens，成本字段为空；网络错误是否计费未知。请求顺序为 model 逐样例成对执行，错误后停止，因此 model-005 Flash 与 model-006 至 model-024 均未请求。
- 唯一断言失败是 model-003 / deepseek-v4.1-flash：expected `allowed=true`、actual `false`，而 `reason_code=within_limit`、请求金额 326.57、剩余可退 326.58；其它确定性字段通过。当前 API 服务按新退款总额不得超过订单总额限制，数值边界也支持“未超限”，因此输出在自身字段间矛盾；Gold 仍是 draft，尚未人工批准。不能据一次调用宣称模型普遍失败。
- Runner 新增 `--continue-on-gateway-network-error`，只针对该错误码记录失败后继续下一计划组合，不重试、不覆盖错误记录；默认遇错停止，鉴权、HTTP 和配置错误保持停止。限定 continuation mode 为单轮，保持每个组合至多一次请求。针对性测试先因新参数不存在而失败，再实现后通过；继续模式测试断言不重发已失败对、错误仍进入报告且质量门禁仍失败。
- 下一轮仅运行 model-005 至 model-024，40 个计划请求；不重跑已完成的 model-001 至 model-004 8 个组合。继续后的结果与首次部分报告分别保留，再按唯一 case/model 组合进行覆盖分析；若再次出现网络错误，保留为 error，不用通过率掩盖。
- 本轮未重新发起模型调用：用户执行脚本在 finally 中清除了 PowerShell 进程级 Key，Codex 当前进程不持有该 Key。续跑需要在本地 PowerShell 再次使用隐藏输入；不要将 Key 粘贴到聊天。`--budget-yuan` 仍只是报告元数据。

## 2026-10-29 双项目升级收尾

- 当前分支仍为 `chore/update-actions-runtime-20261029`；本轮开始 HEAD `2f89849a1f268e978e1f8583bc537dcbdd2013d6`，保留了已有工作区改动。功能分支推进，不使用 `main`。
- 模型 development 全批次已完成并聚合：48 个唯一组合、51 次请求尝试、43 组匹配草案断言、5 组未匹配、0 未解决；3 次网络错误后有补请求。Token 31,416；Provider 未返回费用；0/40 人工批准；门禁 failed、`benchmark_eligible=false`。汇总细节见 `docs/model-eval-development-summary-20261029.md`。
- PromptOps 验收见 `docs/phase-4-delivery-acceptance-20261029.md`：Maven 37/37、Python API 26/26、model-eval 48/48、数据审计 12 项通过、Vue 类型/生产构建/依赖审计通过；8 次本地稳定性采样成功，p95 仅观测；Playwright 运行按钮到报告导出/历史/回归/失败状态完整通过。两张模型摘要页真实截图已放入 `docs/images/`。
- Gitea Quality Lab 最新运行 `20261029T150955Z-4437d857`：Gitea 28.2.0 本地 18/18；Harness 单测 27/27；交互式 HTML 报告与 1440×1000 / 390×844 检查通过。该轮的 API/UI 截图与 JSON/JUnit/HTML 报告在相邻仓库 `gitea-quality-lab/reports/runs/20261029T150955Z-4437d857/`；专用报告页面桌面/手机截图仍取自前一轮，并已在 evidence log 标明。
- Key 安全状态：只确认 `PROMPTOPS_PROVIDER_API_KEY_GUOMODEL` 环境变量存在，未读出或记录值。CLI 使用此变量；Spring Boot Web Provider 使用独立配置，本轮未配置且 API 安全预检返回 409；浏览器检查没有发出付费模型请求。
- 简历边界：模型数据是未审批合成退款规则草案，不是准确率/Benchmark；Mock/开源 Gitea 结果也不是生产经验或生产容量压测。后续应先人工审核 40 条 gold，再建立经批准的新版本。
