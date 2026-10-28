# PromptOps Studio 第三阶段发布候选验收

日期：2026-10-28（Asia/Shanghai）

仓库：D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\work\ai-agent-eval-promptops

本轮证据目录：D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\agent-eval-release-candidate-20261028

## Git 与安全边界

- 分支：feat/agent-evaluation-platform
- 起始与结束 HEAD：d608cc28a2fae8246ad33712d31199185c538639
- 起始工作区已有上一阶段的未提交变更；本轮保留，不重置、不清理、不覆盖。
- 未提交、未推送、未合并，没有切换 main。
- 本轮没有发起外部模型请求。最终验收服务的模型名和 Provider API Key 环境变量显式为空；Provider status 显示未配置。
- Provider 测试只使用本地 loopback HTTP Stub。安全模式扫描 211 个源码、配置、报告及文本日志文件，未发现符合 OpenAI/GitHub/AWS/JWT 模式的凭证字符串；扫描只输出命中文件路径，不读取或输出用户密钥值。
- 独立 Demo H2 运行文件保存在本轮 outputs 目录；不覆盖仓库中已有数据库。

## 最终状态矩阵

| 功能 | 本轮状态 | 证据与边界 |
| --- | --- | --- |
| 固定测试集 1.0.2 | 已重新验证 | 26 个唯一 ID；生成器输出与固定 JSON 完全相同。 |
| Mock 批量执行、断言和质量门禁 | 已重新验证 | Playwright 通过前端触发两轮 26 条运行；agent-014、agent-015 的 Agent 行为失败保留，门禁 failed。 |
| 运行历史、JSON 报告、Trace 与失败归因 | 已重新验证 | 保存快照、下载 JSON、详情和报告接口匹配；同一 H2 重启后 Run 仍存在。 |
| OpenAI-compatible Provider | 已实现并用 Stub 测试 | Maven 覆盖超时、有限重试、429/5xx、非法 JSON、空/结构错误与脱敏；未请求真实 Provider。 |
| 两个已保存运行比较 | 已重新验证 | 后端比较逐用例和类别变化；本轮比较正确显示 mock_metadata_only_comparison。 |
| Prompt/Agent 真实版本适配 | 部分实现 | 当前版本字段仅是元数据标签，没有绑定到不同 Prompt 内容或 Agent 实现；标签差异不声称真实 V1/V2 回归。 |
| Vue 主流程和窄屏 | 已重新验证 | 首页、数据集、运行、失败、Trace、历史、比较、报告、刷新、HTTP 503、1440×920、1366×768、390×844。 |
| 依赖安全 | 已重新验证 | npm audit 返回 0 vulnerabilities。 |
| GitHub Actions | 本地配置检查通过 | workflow 可解析且包含 push / pull_request、Java、Node、pytest、Playwright；本轮未推送，远端 Actions 未执行。 |
| 生产环境能力 | 未实现/未验证 | 未运行 MySQL 生产模式；无队列、权限、多租户、生产审计和部署。 |

## 验收命令和结果

| 检查 | 实际命令与环境 | 结果 | 证据 |
| --- | --- | --- | --- |
| Maven 后端单元/集成测试 | 从仓库根目录运行 mvn -pl backend test | 退出码 0；29 通过，0 失败，0 错误，0 跳过 | outputs/agent-eval-release-candidate-20261028/maven-release-candidate-r2.log |
| Vue 类型检查与生产构建 | frontend 下运行 npm run build；脚本先执行 vue-tsc --noEmit，再执行 vite build | 退出码 0；类型检查通过；Vite 生产产物成功 | outputs/agent-eval-release-candidate-20261028/frontend-build-release-candidate-final.log |
| npm 依赖安全审计 | frontend 下运行 npm audit --audit-level=high | 退出码 0；0 vulnerabilities | outputs/agent-eval-release-candidate-20261028/npm-audit-release-candidate-r2.log |
| Python API 验收 | PROMPTOPS_API_URL=http://127.0.0.1:18091，py -3.12 -m pytest backend/tests/test_agent_eval_api.py -q --junitxml=... | 退出码 0；7 通过，0 失败，0 错误，0 跳过 | outputs/agent-eval-release-candidate-20261028/pytest-release-candidate-r3.log 和 pytest-release-candidate-r3.xml |
| 数据集生成 | backend 下运行 node tools/generate-agent-dataset.mjs，生成到 outputs/agent-eval-release-candidate-20261028/dataset-generated-v1.0.2-r2.json | 退出码 0；生成 26 条唯一用例 | outputs/agent-eval-release-candidate-20261028/dataset-generator-release-candidate-r2.log |
| 数据集一致性 | Python 解析生成 JSON 与仓库固定 JSON，比较完整对象和 26 个唯一 case_id | 退出码 0；EXACT_JSON_MATCH=true，26/26 唯一 | outputs/agent-eval-release-candidate-20261028/dataset-equality-release-candidate-r2.log |
| Playwright 浏览器 | AGENT_EVAL_BASE_URL=http://127.0.0.1:5181，npm run check:agent-eval:live；Vite 代理到本轮后端 18091 | 退出码 0；2 次完整运行各 26 条，0 pageerror，错误态验证通过，桌面/移动端无文档横向溢出 | outputs/agent-eval-release-candidate-20261028/playwright-release-candidate-r4.log；截图位于 final-browser-r4/ |
| H2 跨进程重启与 JSON 一致 | 以文件型 H2 启动、保存报告、停止 Spring Boot、使用同一数据库重新启动后核对历史/详情/报告 | PASS；Run 9fc5bc0d-f675-4959-b16a-43cd4a4630fe 重启后仍在历史，下载、详情与报告接口相同，26 条 case 均带同一 run_id | outputs/agent-eval-release-candidate-20261028/persistence-restart-proof-final.json、persistence-restart-final.log、server-backend-restart-final.log |
| GitHub Actions YAML | Python PyYAML BaseLoader 解析 .github/workflows/quality.yml | 退出码 0；1 个 verify job，push 和 pull_request 触发 | outputs/agent-eval-release-candidate-20261028/workflow-yaml-validation.log |
| 凭证模式扫描 | Python 扫描仓库与本轮 outputs 的可读文本文件 | 退出码 0；211 个文件，0 项符合扫描模式的凭证候选 | outputs/agent-eval-release-candidate-20261028/secret-pattern-scan-final.log |
| Git 与未跟踪文件空白检查 | git diff --check，并额外检查工作区内已修改/未跟踪的文本文件 | 两项退出码均为 0；tracked diff 无空白错误，42 个文本文件无尾随空白 | outputs/agent-eval-release-candidate-20261028/git-diff-check-final.log、worktree-whitespace-scan-final.log |

npm ci 未在本机重复运行；项目依赖已安装，本轮使用当前 lockfile 对应的 node_modules。workflow 中配置了 npm ci，但因为分支没有推送，没有声称远端 Workflow 验收通过。workflow YAML 本地解析通过不等于 GitHub Actions 实际通过。

## 真实浏览器运行结果

最终脚本在 1440×920 初始视口运行，调整到 1366×768 和 390×844 检查窄屏。脚本真实点击 UI 并校验：

- 两次 POST 均返回成功、各自获得不同 Run ID；每次 26 条用例均保存。
- agent-014 可复现：执行器 success，Agent 行为 failed，实际工具为 web_search，预期工具为 weather_lookup。
- 两次 Mock 行为结果一致；两个运行各 24 通过、2 失败、0 错误、0 跳过。
- 下载报告 JSON 与运行详情及报告接口逐对象相等。
- 回归 API 使用选中的两个已保存运行，比较模式为 mock_metadata_only_comparison，未宣称模型版本差异。
- 刷新后历史仍能查询；模拟后端 503 时 UI 展示错误，不新增成功运行。
- 桌面和移动端 document 宽度不超过 viewport；浏览器 pageerror 数为 0。
- Provider 未配置时选择 Provider 得到 409 拒绝；没有发送外部模型请求。

两条被保留的失败说明当前 Mock Agent 仍有路由与重复调用问题，因此质量门禁是 failed。这是正确的评测结果，不是 Harness 执行错误。

## 报告与持久化证据

最终代表性报告：9fc5bc0d-f675-4959-b16a-43cd4a4630fe；浏览器二次运行：49e05fb0-15c6-4f2d-9d45-d239e88472cf。前端第一轮开始时后端历史有 8 条，完成两轮后为 10 条；独立 H2 文件为：

D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\agent-eval-release-candidate-20261028\agent-eval-release-candidate-r2.mv.db

运行 JSON 报告：[agent-eval-report-9fc5bc0d-f675-4959-b16a-43cd4a4630fe.json](reports/agent-eval-report-9fc5bc0d-f675-4959-b16a-43cd4a4630fe.json)

四张 README 展示图位于 docs/images，其他最终浏览器截图、JSON 下载和本地 H2 数据库保存在 outputs 独立证据目录。

## 调试修正记录

- 最初用 5182 运行 Vite 时，后端按精确来源限制拒绝了该端口的跨域 POST；没有改宽 CORS 策略。改用项目已有放行端口 5181 代理到隔离后端后，完整浏览器流程通过。
- 数据集生成器第一次误从仓库根目录查找，报 MODULE_NOT_FOUND；确认实际脚本在 backend/tools 后重新执行成功并完成逐对象比对。
- 浏览器检查脚本的视口高度最初为 1440×1000 和 1366×900；按任务验收目标改为 1440×920 与 1366×768，随后完整流程再次通过。

## 已知限制与未完成项

- 本轮没有真实模型运行或真实 Provider 指标。OpenAI-compatible Provider 仅由本地 Stub 覆盖；使用者需自行配置凭证并明确发起可能计费的请求。
- Prompt/Agent 版本目前是展示标签。要称为真正版本回归，需要把不可变 Prompt/Agent 内容与运行执行器绑定。
- 工具模拟为本地 Stub，工具超时/异常用 fault 输入确定性模拟。
- 同步执行适合当前 26 条本地测试集；尚无异步队列、任务取消、并发调度或分布式重试。
- 旧 PromptOps 页面依旧展示前端本地演示内容。
- 本机只验收了 demo H2；未对 MySQL 部署或生产并发做验证。
- workflow 没有远端 GitHub Actions 执行证据。

简历草稿与面试讲解见 [docs/job-delivery.md](job-delivery.md) 与 [docs/interview-guide.md](interview-guide.md)。没有把公司、岗位或工作成果补写为用户未提供的信息。
