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
