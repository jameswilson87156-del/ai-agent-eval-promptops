# 通用测试开发实习生 JD 补强验收

日期：2026-10-28
分支：`feat/agent-evaluation-platform`
更新：2026-10-29。功能分支已推送；实现提交 `8f99b7d0429923d3f11b864171c5c30b75be34f1`，导航竞态修复提交 `ea33d4610a0dc7ddad172c10c8519fc715c578df`。分支尚未合并 main。

## 补强目标

公开的通用测试开发实习岗位常见要求包括测试用例设计、Web/服务端/API 测试、性能与稳定性测试、自动化平台/工具、CI/CD、缺陷定位和监控。项目原有 Agent 评测链路已覆盖测试集、API 自动化、逐条失败原因、Trace 和报告；本轮补上一个有边界、可重复的本地 Mock API 稳定性采样器，并接入 GitHub Actions。

采样器位于 `backend/tools/check_agent_eval_stability.py`，只接受 localhost/loopback；默认 12 次正式请求、并发 3、预热 1 次，上限 100 次/并发 16。它校验固定数据集版本、运行模式、Run ID、逐条执行状态和断言汇总，比较多次运行的 Agent 行为输出。签名会忽略 Trace 事件 UUID、时间戳和耗时这三个易变字段，保留事件类型、顺序、输入/输出摘要、状态及错误。报告不保存 HTTP 响应正文，使用独占创建避免覆盖已有文件。

## 验收结果

| 检查 | 命令 | 结果 | 证据 |
| --- | --- | --- | --- |
| Python 语法编译 | `py -3.12 -m py_compile backend/tools/check_agent_eval_stability.py backend/tests/test_agent_eval_stability.py` | 退出码 0 | 本轮命令记录；单测日志见下方 |
| 后端 Maven 测试 | `mvn -pl backend test` | 退出码 0；29 通过、0 失败、0 错误、0 跳过 | `outputs/testdev-stability-20261028/maven-test-final.log` |
| Python/API 全套 | `PROMPTOPS_API_URL=http://127.0.0.1:18092 py -3.12 -m pytest backend/tests -q --junitxml=...` | 退出码 0；18 通过、0 失败、0 错误、0 跳过（7 个既有 API 测试 + 11 个采样器单测） | `outputs/testdev-stability-20261028/python-tests-final.log`、`python-tests-junit.xml` |
| 稳定性采样 | `py -3.12 backend/tools/check_agent_eval_stability.py --base-url http://127.0.0.1:18092 --iterations 12 --concurrency 3 --warmup-runs 1 --output .../agent-eval-stability-sample-r3.json` | 退出码 0；12/12 正式请求成功，1/1 预热成功，13 个唯一 Run ID，输出确定 | `outputs/testdev-stability-20261028/agent-eval-stability-sample-r3.json`、`sampler-execution-r3.log` |
| JSON 与运行库记录核对 | 依据 r3 JSON 的全部 Run ID 查询 `/api/agent-eval/runs`，比较模式、数据集、用例数、行为通过/失败数、执行错误和签名 | 退出码 0；13/13 ID 匹配；每次 26 条、24 个 Agent 行为通过、2 个行为失败、0 个 Provider/评测器错误 | `outputs/testdev-stability-20261028/report-db-consistency-r3.json` |
| 重启持久化 | 停止独立 Spring Boot 进程，再用同一文件 H2 URL 启动 `18092`，读取采样 Run ID | 通过；重启后历史共 51 条，13/13 个采样 ID 仍可读 | `outputs/testdev-stability-20261028/restart-persistence-r3.json`；H2 数据文件 `outputs/testdev-stability-20261028/agent-eval-stability.mv.db` |
| 失败路径保护 | 公网目标 `https://example.com`；尝试覆盖已有 r1 JSON | 两项均按预期以退出码 2 拒绝；没有创建远程输出文件，原报告 SHA-256 不变 | `outputs/testdev-stability-20261028/sampler-negative-checks.log` |
| CI 配置语法 | 使用已安装 PyYAML 解析 `.github/workflows/quality.yml` 并检查 job steps | 退出码 0；YAML 有效，完整 Python 测试和 8 次稳定性采样已接入 | `outputs/testdev-stability-20261028/workflow-yaml-check.log` |
| Git 空白检查 | `git diff --check` | 退出码 0；无空白错误（Git 显示既有工作区文件的 LF/CRLF 提示） | 本轮命令记录 |

Node 的 YAML 检查曾尝试使用仓库未安装的 `js-yaml`，因模块缺失没有执行解析；随后使用机器上已有的 PyYAML 完成解析并通过，未安装依赖。

## 采样结果的正确解释

正式采样运行 12 次，每次 26 条用例，共检查 312 条。每次 `execution_status` 都成功；Agent 行为结果为 24 通过、2 失败。两个既有失败仍是 agent-014 工具误选和 agent-015 重复调用，因此每次质量门禁为 `failed`。采样器的 `status=passed` 只表示本地 API/执行器响应稳定、输出可重复且运行记录完整，不能解释为 Agent 全部通过。失败用例没有被删除或改成无条件通过。

本机 Mock HTTP + 评测器请求耗时：最小值 6.567 ms、中位数 8.713 ms、nearest-rank p95/最大值 22.648 ms。报告 `latency_status=observed_only`，没有设定延迟预算；这些值不是模型延迟、生产性能、容量或 SLO 结论。观测吞吐约 204.381 run/s，只代表这次本机 26-case Mock 任务及并发 3 的计时方式，不应作为简历性能指标。

调试中 r2 曾因把 agent-024 的 Trace 事件 UUID/时间/耗时纳入确定性签名而判为漂移；检查数据库实际结果后，确认 Agent 输出一致，仅 Trace 元数据变化。采样器已按 Trace assertion 语义屏蔽这三个易变字段，保留事件内容，r3 重跑通过。r1、r2 文件保留为迭代记录，正式验收引用 r3。

## 尚未覆盖的外部/企业范围

- 没有调用真实模型，不含 Token、费用或真实模型延迟数据。
- 没有连接公司生产业务 API、真实外部工具、企业监控/告警或生产环境；本地合成订单 API、告警演练、Soak 和有界负载结果见下节。
- 没有设定并验证生产 p95/SLO 预算；本机延迟只是观察值。
- GitHub Actions run [40723393664](https://github.com/jameswilson87156-del/ai-agent-eval-promptops/actions/runs/40723393664) 在 Ubuntu runner 通过全部步骤；详见本文“本轮远端质量门禁”。
- 本轮已改进前端运行期间的导航状态，并用 Playwright 验证桌面/移动宽度、真实运行请求、失败详情、JSON 下载、刷新持久化和服务错误态。

岗位对齐和简历草稿见 [job-delivery.md](job-delivery.md)。

## 本地订单/退款、监控告警、Soak 与负载扩展（2026-10-28）

### 被测对象与边界

新增 `/api/business/orders` 本地仿真 API：订单、支付与退款状态保存在隔离 H2。它仅用于功能/集成/观测/稳定性/负载演练，不连接真实公司系统、客户数据或支付服务。`X-Demo-Actor-Id` / `X-Demo-Role` 是测试身份头，不是真实鉴权。后端、Prometheus、Alertmanager 和负载执行器通过内部 Docker 网络通信；本机通过固定上游代理访问 `127.0.0.1:18083`、`:9090`、`:9093`。外部告警接收器没有配置。

### 业务 API 集成覆盖

| 场景 | 预期行为 |
| --- | --- |
| 创建订单与相同幂等键重放 | 相同请求返回同一个订单；键被不同订单内容重用时返回冲突 |
| 精确金额支付与重放 | 支付金额必须等于订单总额；相同操作不产生重复支付 |
| 部分退款与全额退款 | 退款累计额不能超过订单额；状态按 PAID → PARTIALLY_REFUNDED → REFUNDED 更新 |
| 未支付订单取消 | 取消可幂等重放；取消后支付被拒绝 |
| 客户隔离与演示角色 | 客户不能读取他人订单；不支持的角色被拒；支持人员可读取演示记录 |
| 非法金额、缺少幂等键、非法 JSON | 以 400/422 等 4xx 明确拒绝，不伪报成功 |
| Prometheus 导出 | 可抓取 Agent Eval 运行/用例、业务操作和 Spring HTTP 计时指标 |

### 监控与负载实现

- Spring Boot Actuator + Micrometer Prometheus Registry 在 `demo` / `test` profile 提供 scrape endpoint；默认 profile 不暴露 Prometheus endpoint。
- Prometheus 收集 `http.server.requests` 和低基数业务/评测指标；Alertmanager 配置后端不可达、5xx 比例和 Agent Provider 错误规则。
- 本地 webhook sink 只写入脱敏的告警名、状态、级别、摘要和时间，不记录请求输入或用户数据。
- `backend/tools/run_business_soak.py` 仅允许 loopback，或显式启用 Compose 的固定 `backend:18083`；默认 30 分钟、30 秒一轮，独占创建 JSON 并在 SIGINT/SIGTERM 后收尾。
- `performance/k6/business-order-workflow.js` 使用 constant-arrival-rate，目标到达率 5/10/20/100 个完整工作流/秒，各 60 秒；每个工作流包含 5 个 HTTP 步骤。HTTP URL 标签使用固定操作名，避免订单 ID 形成高基数。流程耗时引用 k6 内置 `group_duration`，不依赖墙钟相减。新增逐场景迭代数、检查、HTTP/业务错误和 dropped-iteration 门禁；`backend/tools/summarize_k6_report.py` 校验门禁并输出摘要 JSON/Markdown。报告单独列出 `vus` 活跃峰值和 `vus_max` 最大可分配资源；`100/s` 是工作流目标到达率，不是已测压力阶段 HTTP RPS。所有延迟分位数只是本机观察量，没有生产 SLO。

实现参考 Spring Boot Actuator/Micrometer、Prometheus 告警规则、Alertmanager webhook 和 k6 constant-arrival-rate 的公开文档；项目接口、业务规则、sink 与断言为本项目实现，没有复制其它产品源码。详细命令、退出码、计数和证据文件见本轮交付报告。

### 本轮验收结果

以下结果来自本地隔离 Docker Compose 栈与 H2；用例成功表示仿真 API 按断言执行，不代表真实生产系统质量。

| 检查 | 实际结果 | 证据 |
| --- | --- | --- |
| 后端 Maven 全量测试 | `mvn -pl backend test` 退出码 0；36 通过、0 失败、0 错误、0 跳过 | `outputs/testdev-production-like-20261028/backend-maven-test-final.log` |
| 后端打包 | `mvn -pl backend -DskipTests package` 退出码 0 | `outputs/testdev-production-like-20261028/backend-package-final.log` |
| Soak 脚本单测 | Python 3.12 容器、无网络、只读挂载；4 通过、0 失败，退出码 0 | `outputs/testdev-production-like-20261028/python-business-soak-unit-test.log` |
| k6 脚本加载 | `docker compose -f compose.production-like.yml run --rm k6 inspect /scripts/business-order-workflow.js` | 退出码 0；解析出 5/10/20/100 四档 constant-arrival-rate 场景，压力档上限 80 VU | `outputs/testdev-production-like-20261028/k6-script-inspect.log` |
| Compose / Prometheus / Alertmanager 配置 | `docker compose config --quiet`、`promtool check config`、`amtool check-config` 全部退出码 0；3 条 Prometheus 规则有效 | `compose-config-check.log`、`promtool-check.log`、`amtool-check.log` |
| 业务流程负向与状态机 | JUnit HTTP 集成测试覆盖幂等、金额、部分/全额/超额退款、取消、身份隔离、非法请求和指标导出；错误请求返回 4xx | Maven 日志；`final-api-contract-smoke.json` |
| Agent Eval + 业务流程观测 | Mock Agent 运行 26/26 条执行，行为 24 通过、2 失败、0 错误；订单支付并全额退款后为 `REFUNDED`；Prometheus scrape 含业务和 Agent 指标 | `agent-eval-monitoring-smoke-eadda683-9513-4170-809b-65bf9a5a5453.json`、`local-business-monitoring-smoke.json`、`prometheus-metric-sample.txt` |
| Soak 执行期间监控目标 | Prometheus runtime API 显示 `promptops-backend` scrape target 为 `up`、无 lastError | `monitoring-runtime-snapshot.json` |
| 报告与业务数据持久化 | 只重启本地后端后，同一 Agent Eval 报告仍含 26 条；同一订单仍为 `REFUNDED`，支付 `COMPLETED`，退款记录 1 条 | `restart-persistence.json` |
| 告警恢复演练 | 停止隔离后端触发 `PromptOpsBackendUnavailable`；本地 sink 收到 1 条 firing、1 条 resolved；后端恢复为 `UP` | `alert-drill.json`、`alert-events.jsonl` |
| Soak 完整运行 | 默认 30 分钟、30 秒间隔；退出码 0，实际耗时 1,799.211 秒；60/60 轮通过、0 失败；300 个业务请求 + 60 次健康检查，共 360 个请求 | `outputs/testdev-production-like-20261028/business-soak-final-execution.log`、`business-soak-20261028T105206352718Z.json` |
| k6 四档业务负载（首次最终运行） | 退出码 0；8,103 个流程、40,515 个 HTTP 请求、81,030 项检查通过；HTTP 失败率 0、工作流失败 0、掉队 0；`vus_max=120` 是最大可分配 VU 资源，不能解释为实测同时用户 | `business-load-final-clean.log`、`business-load-final-summary.json`、`business-load-2026-10-28T10-51-48-081Z-3af9b40b.json` |
| k6 四档业务负载（前一轮独立复跑） | 命令 `docker compose -f compose.production-like.yml run --rm k6`，退出码 0；8,102 个完整流程、40,510 个 HTTP 请求、81,020 项检查通过；HTTP 错误率 0、流程失败 0、dropped iterations 0；`vus` 峰值 2，`vus_max=120`；`group_duration` p95 7.382 ms、HTTP p95 1.830 ms，均为本机观察值 | [原始 k6 JSON](reports/business-load-rerun-20261028T141643Z-e68a5f0c.json)；控制台完整运行时长约 4m29.7s |
| 压测期间 Prometheus 采样 | 在覆盖复跑的 5 分钟窗口查询，后端 `up` 最小值 1，Spring `process_cpu_usage` 最大原始值 0.066935；Docker Desktop/WSL2 报告 4 vCPU、约 3.82 GiB 内存 | [Prometheus 查询 JSON](reports/business-load-rerun-monitoring-20261028T141700Z.json)。该 CPU 值是进程级本机观测，不等同于主机 CPU 使用率或容量结论 |
| 仓库状态 | 分支仍为 `feat/agent-evaluation-platform`，HEAD 未变化；`git diff --check` 退出码 0 | `git-diff-check-final.log` |

独立 30 分钟 Soak 报告副本见 [Soak JSON](reports/business-soak-20261028T105206352718Z.json)；12 次 Agent Eval 重复性采样报告见 [稳定性 JSON](reports/agent-eval-stability-r3.json)。隔离 H2 文件位于 Docker 命名卷 `promptops-quality-local-20261028-data` 内的 `/data/promptops-quality.mv.db`；验证时文件大小约 8.5 MiB。卷元数据与容器内文件清单见 `h2-data-volume-inspect.json`、`h2-data-volume-files.log`。

首次 Soak 在 Docker Desktop 停止后中断：最后快照为 49 轮通过、0 失败、294 请求，状态仍是 `RUNNING`，没有最终退出码；见 `business-soak-interrupted-20261028.json`。第二次独立运行已完成配置的 30 分钟窗口：JSON 状态 `COMPLETED`、实际耗时 1,799.211 秒，60/60 轮通过、0 失败；300 个业务请求加 60 次健康检查，共 360 个请求。两次运行没有合并。最终报告本地链接检查证据：`outputs/testdev-production-like-20261028/output-link-check-final.json`。

## 通用测试开发 JD 技术对照与分场景负载证据（2026-10-28）

### JD 样本

- [上海人工智能实验室：测试开发实习生-技术平台中心](https://www.shlab.org.cn/joinus/detail/7685717497777785094?mode=campus)，2026-09-15 发布：大模型系统质量评估、自动化流水线与测试工具、质量把关，要求熟练至少一门编程语言。
- [清程极智测试/测试开发实习生](https://www.shixiseng.com/intern/inn_k83obyoosubt?pcm=pc_SearchList)，2026-10-26 更新：API/Web 功能与接口测试、维护自动化脚本、问题定位复现、压力和稳定性；Python/Java/Go、Linux、HTTP、MySQL/Redis。
- [Calix 自动化测试开发实习生（Linux/Python）](https://www.nowcoder.com/jobs/detail/283410)：自动化脚本和测试环境，要求 Linux、TCP/IP 与 Python。

这些是岗位样本，不代表所有公司统一要求；投递前应按具体 JD 调整项目重点。

### 项目覆盖矩阵

| 常见要求 | PromptOps Studio | Gitea Quality Lab | 证据边界 |
| --- | --- | --- | --- |
| 编程与自动化 | Java/Spring Boot、Python/pytest；26 条 Agent 合成用例和确定性断言 | Python 3.12、pytest；24 个测试框架单测及 16 个 Gitea 集成用例 | 都是本地/CI 项目结果 |
| API 与业务测试 | Spring 集成测试覆盖订单、支付、退款状态机、幂等和权限 | HTTPX 覆盖 Gitea REST 身份、权限、仓库、Issue、输入错误 | Gitea 28.2.0 本地容器，不是公司系统 |
| Web/UI 自动化 | Playwright 验收 Agent 运行、失败详情、刷新、错误态及窄屏 | Playwright Chromium 覆盖登录、建仓、Issue、评论和关闭 | 浏览器运行是本地环境 |
| 数据库/持久化 | 文件型 H2 保存运行报告和仿真订单 | SQLite 数据持久化，验证容器重启后 API 数据仍存在 | 没有 MySQL/Redis 生产集成测试 |
| 稳定性/性能 | 30 分钟低频 Soak；k6 四档本地阶梯负载；Prometheus/Alertmanager 本地告警演练 | CI 每次启动隔离 Gitea 服务并执行 API/UI/重启测试 | 本机 Docker 负载不等于生产容量或大规模生产压测 |
| CI/CD 与报告 | GitHub Actions run [40723393664](https://github.com/jameswilson87156-del/ai-agent-eval-promptops/actions/runs/40723393664) 在 Ubuntu runner 通过并上传证据 artifact | GitHub Actions run [40710451708](https://github.com/jameswilson87156-del/gitea-quality-lab/actions/runs/40710451708) 在 Ubuntu runner 通过并上传 JSON/JUnit/HTML/截图 | 两者都是隔离测试实例 / CI 临时环境，不代表生产系统 |
| 缺陷定位与可复现 | 单条断言、失败原因、Trace、固定输入、Run ID | 用例 ID、失败详情、浏览器截图和脱敏 Trace | Trace 是可观察数据，不包含模型思维链 |
| AI 质量评估 | 固定数据集、工具选择/参数/顺序/安全规则、运行比较 | 不面向 LLM，专注通用系统黑盒测试 | PromptOps 当前是 Mock/规则评测，不代表真实模型效果 |

### 新增分场景 k6 实测

- 命令：`docker compose -f compose.production-like.yml run --rm k6`；退出码 0。执行固定 `grafana/k6:1.8.1`，目标仅为本地隔离合成订单/退款 API。
- 运行 ID：`k6-2026-10-28T14-41-32-869Z-b09f9fc3`；k6 内部执行时长约 4 分 30 秒。
- 总计：8,103 个工作流、40,515 个 HTTP 请求、81,030 项检查通过；0 HTTP 错误、0 业务失败、0 掉队迭代。活跃 VU 峰值 1，最大可分配 VU 资源 120。
- 分场景：5 工作流/秒档 301/至少 300；10/s 档 601/至少 600；20/s 档 1,200/至少 1,200；100/s 档 6,001/至少 6,000。各档检查失败、HTTP 失败、业务失败和掉队迭代均为 0。
- 全阶段合并 HTTP p95 为 1.361 ms、工作流 p95 为 5.913 ms；两者是该 Docker Desktop/WSL2 本机运行的观察值，不是 SLA/SLO，不用于声称生产容量。没有独立测量压力档 HTTP RPS。
- 原始及结构化证据：[`原始 k6 JSON`](reports/business-load-k6-2026-10-28T14-41-32-869Z-b09f9fc3.json)、[`摘要 JSON`](reports/business-load-k6-2026-10-28T14-41-32-869Z-b09f9fc3-summary.json)、[`摘要 Markdown`](reports/business-load-k6-2026-10-28T14-41-32-869Z-b09f9fc3-summary.md)。汇总器单测 4 条通过；原始 k6 阈值结果也包含在 JSON 中。

## 本轮远端质量门禁（2026-10-29）

### 本地回归

| 检查 | 命令 | 结果 | 证据 |
| --- | --- | --- | --- |
| 后端 | `mvn -pl backend test` | 退出码 0；36 通过、0 失败、0 错误、0 跳过 | `outputs/testdev-career-update/promptops-local-acceptance-20261028/maven-test.log` |
| Python API / 工具单测 | `py -3.12 -m pytest backend/tests/test_agent_eval_stability.py backend/tests/test_business_soak.py backend/tests/test_k6_report_summary.py -q` | 退出码 0；19 通过、0 失败、0 跳过 | `outputs/testdev-career-update/promptops-local-acceptance-20261028/python-tests.log` |
| 数据集校验 | `py -3.12 -m pytest backend/tests/test_agent_eval_api.py -q -k dataset` | 退出码 0；1 通过、1 跳过、5 deselected | `outputs/testdev-career-update/promptops-local-acceptance-20261028/dataset-validation.log` |
| 前端类型检查与生产构建 | `npm run build` | 退出码 0；`vue-tsc --noEmit` 与 Vite production build 通过 | `outputs/testdev-career-update/promptops-local-acceptance-20261028/frontend-build.log` |
| npm 依赖审计 | `npm audit --json`（在 `frontend/` 执行） | 退出码 0；0 项 info / low / moderate / high / critical 漏洞 | `outputs/testdev-career-update/promptops-local-acceptance-20261028/npm-audit-frontend.json` |
| Workflow YAML | 使用 PyYAML 解析 `.github/workflows/quality.yml` | 退出码 0；YAML 有效，含 `verify` job | `outputs/testdev-career-update/promptops-local-acceptance-20261028/workflow-yaml-check.log` |
| 浏览器端到端 | `npm run check:agent-eval:live` | 退出码 0；运行、失败复现、JSON 导出一致、同数据集比较、刷新持久化、错误态和无页面级水平溢出断言通过；浏览器 JS 错误 0 | `outputs/testdev-career-update/promptops-local-acceptance-20261028/browser-acceptance-r5.log` 与 `live-browser-r5/` |

本地浏览器运行 ID：`00f9a964-b602-4c83-8d3c-2b701c4ad476` 与 `6ab56736-e112-440f-b7f0-7435b0731c01`。每次运行 26 条合成用例，保留 2 条 Agent 行为失败；`agent-014` 的错误工具选择可复现。浏览器下载 JSON 与保存运行内容一致。测试覆盖 1440px / 1366px 桌面与 390px 手机视口；结果来自本地 Mock 服务，不是模型质量或生产系统验收。

首次远端运行 [40721964915](https://github.com/jameswilson87156-del/ai-agent-eval-promptops/actions/runs/40721964915) 失败在浏览器导航步骤。根因是执行结果刷新历史期间，异步完成回调覆盖用户刚切换的导航。修复已提交为 `ea33d4610a0dc7ddad172c10c8519fc715c578df`，前端通过导航版本号避免覆盖用户操作；本地 Playwright 全流程回归通过。

### 通过的 GitHub Actions

[GitHub Actions run 40723393664](https://github.com/jameswilson87156-del/ai-agent-eval-promptops/actions/runs/40723393664) 在 `feat/agent-evaluation-platform`、Ubuntu runner 完成，结论 `success`：

- Maven：36 通过，0 失败、0 错误、0 跳过。
- Python API / 工具：26 通过。
- 固定数据集校验：1 通过、1 跳过、5 deselected。
- Mock 稳定性采样：8/8 正式运行、1/1 预热成功，0 请求失败、输出确定；Runner HTTP p95 观测值 33.155 ms，仅为观察值。
- Playwright：真实点击运行、26 条用例、失败归因、JSON 与持久化记录一致、重复 Mock 输出一致、刷新后历史存在、前端 503 错误显示、1440px / 1366px / 390px 视口无页面级横向溢出；通过。
- CI 上传的 `agent-eval-evidence` artifact 为 1,332,468 bytes，保留至 2027-01-26；包含运行报告与浏览器截图：[查看 artifact](https://github.com/jameswilson87156-del/ai-agent-eval-promptops/actions/runs/40723393664/artifacts/12829964846)。

远端 Actions 只验证 GitHub runner 中的自动化用例与临时 Mock 服务，不构成真实模型、企业生产系统或大规模并发容量证明。
