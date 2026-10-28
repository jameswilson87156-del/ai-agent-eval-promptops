# 第二阶段验收记录

日期：2026-10-28（Asia/Shanghai）

仓库：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\work\ai-agent-eval-promptops`

分支：`feat/agent-evaluation-platform`
起点 / 结束 HEAD：`d608cc28a2fae8246ad33712d31199185c538639`（未提交、未推送）

## 结果摘要

默认 Agent Eval 页面通过 HTTP 触发 Spring Boot 同步执行。测试定义为数据集 `1.0.1` 的 26 条原创合成场景；报告、历史、详情、失败归因和 Trace 来自真实运行与 H2 快照。当前执行器是确定性 Mock Agent，不调用真实模型。

验收报告 `82b67bd7-0647-4dc5-9272-80730765037a`：

| 统计项 | 实际结果 |
| --- | ---: |
| 用例总数 / 已执行 | 26 / 26 |
| Agent 行为通过 | 24 |
| Agent 行为失败 | 2 |
| 执行错误 / 跳过 | 0 / 0 |
| 评测器错误 | 0 |
| 通过率 | 92.31%（仅此 Mock 运行，不代表真实模型效果） |
| 质量门禁 | failed |

失败 `agent-014`（路由误选 `web_search`）和 `agent-015`（天气工具被重复调用）均可重复，断言和数据未为提高通过率而修改。

## 命令与验收证据

所有测试命令均在本地实际执行。日志与机器可读证据保存在：
`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs`

| 检查 | 实际命令 | 退出码 / 计数 | 证据 |
| --- | --- | --- | --- |
| 后端 Maven 测试 | `mvn -pl backend test` | 0；17 通过，0 失败，0 错误，0 跳过 | `agent-eval-maven-test-20261028.log`；`backend/target/surefire-reports/` |
| Vue 类型检查及生产构建 | `cd frontend; npm run build`（包含 `vue-tsc --noEmit && vite build`） | 0；类型检查通过，生产构建成功 | `agent-eval-frontend-build-20261028.log` |
| 依赖安全审计 | `cd frontend; npm audit --audit-level=high` | 0；0 vulnerabilities | `agent-eval-npm-audit-20261028.log` |
| 数据集生成一致性 | `cd backend; node tools/generate-agent-dataset.mjs target/generated-agent-dataset-check-final.json`，解析后与固定数据集比较 | 0；26 条生成结果与固定数据逐字段相同 | `agent-eval-dataset-generator-check-20261028.log`；临时文件在 `backend/target/` |
| pytest 数据集与 API 验收 | `$env:PROMPTOPS_API_URL='http://localhost:18089'; py -3.12 -m pytest backend/tests/test_agent_eval_api.py -q --junitxml=D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\agent-eval-pytest-20261028.xml` | 0；5 通过，0 失败，0 跳过 | `agent-eval-pytest-20261028.log`、`agent-eval-pytest-20261028.xml` |
| Playwright 浏览器验收 | `$env:AGENT_EVAL_OUTPUT_DIR='D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\agent-eval-browser-final-20261028'; cd frontend; npm run check:agent-eval:live` | 0；流程完整结束，0 浏览器异常，390px 无横向溢出 | `agent-eval-playwright-final-20261028.log`；`agent-eval-browser-final-20261028/` |
| GitHub Actions YAML 解析 | `py -3.12 -c "import yaml; yaml.safe_load(open('.github/workflows/quality.yml', encoding='utf-8'))"` | 0；YAML 解析成功 | `agent-eval-workflow-yaml-check-20261028.log` |
| Git 差异空白检查 | `git diff --check` | 0；无空白错误 | `agent-eval-git-diff-check-20261028.log` |

### Playwright 实际检查内容

- 真实 POST 返回 HTTP 200，报告有 26 条；再次运行得到不同运行 ID。
- 失败工作台显示 `agent-014`，输入、预期、实际输出、失败原因、断言和 8 个 Trace 事件可见。
- 搜索测试集只显示目标用例；报告 JSON 下载后与 POST 运行报告逐字段一致。
- 同数据集运行比较页加载，刷新后历史仍能显示。
- 1440×920 桌面流程完成；390×844 页面无横向溢出。模拟 POST HTTP 503 后出现错误提示，历史行数不变。
- 0 个浏览器 `pageerror`。
- 本轮浏览器运行 ID：`82b67bd7-0647-4dc5-9272-80730765037a`；二次运行 ID：`7bbb1852-4926-4132-86bf-3f00870923d1`。

证据目录 `outputs/agent-eval-browser-final-20261028/` 含真实浏览器生成的总览、失败/Trace、回归和窄屏截图，以及下载报告 `agent-eval-report-82b67bd7-0647-4dc5-9272-80730765037a.json`。

### 失败复现与报告一致性

- pytest 连续运行两次，确认 `agent-014` 和 `agent-015` 都得到相同 `actual_output` 与失败状态；`agent-015` 实际产生 2 次 `weather_lookup` 调用，断言预期 1 次。
- API 历史、Run 详情和 `/report` 返回同一数据库 JSON 快照；Python 将保存报告与 POST /report 完整对象进行比较。
- `agent-eval-report-82b67bd7-0647-4dc5-9272-80730765037a.json` 被下载后，停止验证过的本地服务，再以 `demo` profile 重启。重启后运行仍在历史，`/report` 返回内容与下载文件一致；208 条 Trace 事件同时保留。
- 跨重启核对命令结果：`report_matches_downloaded_json=true`、`history_contains_run=true`。机器可读证据：`agent-eval-persistence-proof-82b67bd7-0647-4dc5-9272-80730765037a.json`；持久化文件：`backend/agent-eval-demo.mv.db`（gitignored）。

## 截图

以下均由本地浏览器打开真实运行页面生成，不是设计稿：

- 总览：`outputs/agent-eval-browser-final-20261028/agent-eval-overview-82b67bd7-0647-4dc5-9272-80730765037a.png`
- 失败归因与 Trace：`outputs/agent-eval-browser-final-20261028/agent-eval-failure-trace-82b67bd7-0647-4dc5-9272-80730765037a.png`
- 同数据集回归：`outputs/agent-eval-browser-final-20261028/agent-eval-regression-82b67bd7-0647-4dc5-9272-80730765037a.png`
- 390px 窄屏总览：`outputs/agent-eval-browser-final-20261028/agent-eval-mobile-82b67bd7-0647-4dc5-9272-80730765037a.png`
- README 展示副本：`docs/images/agent-eval-*-v2.png`

## CI 与尚未执行的项目

- `.github/workflows/quality.yml` 已配置 Maven、`npm ci`、生产构建、npm audit、pytest 数据集/API 和 Chromium + Playwright 浏览器验收，并上传浏览器证据。
- 本地对 workflow YAML 解析成功；分支未 push，因此 GitHub 托管的远端 Actions **没有运行**，不能标为通过。
- 本机没有执行 `npm ci`；本轮生产构建和审计使用现有 `node_modules`。`npm audit fix` 使用兼容版本范围更新 lockfile，没有使用 `--force`，最终审计为 0 vulnerabilities。
- 本阶段没有安装真实模型依赖、使用 API Key 或触发付费模型调用。未实现真实模型性能指标、真实 Prompt/Agent V1/V2、异步队列、用户认证和生产数据库。
- 第一次复制截图后的 PowerShell 文件清单命令因参数类型错误退出 1；逐文件复核确认 4 张图和报告均已复制。该文件清单错误没有影响源码或评测数据。
- 为验证文件 H2 跨重启，终止已确认的本地 Spring Boot 子进程后，原 `spring-boot:run` shell 记录 `Process terminated with exit code: -1`（退出码 1）。这是主动停止服务的运行结束状态，不是 Maven 测试失败；随后服务正常重启并通过上述报告核对。
