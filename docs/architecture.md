# PromptOps Studio 架构说明

## 项目定位

PromptOps Studio 保留原有 PromptOps 演示页面，同时增加独立的 Agent Eval 执行工作台。Agent Eval 用固定合成数据、确定性 Mock 或显式选择的 OpenAI-compatible Provider 执行同一组断言，并保存可复核的运行快照。

Agent Eval 指标来自实际 HTTP 执行，不来自静态页面；默认运行结果只描述 Mock Agent。原有 PromptOps 页面仍有本地展示数据，不能与新工作台的后端运行统计混用。

## 模块边界

- Vue 3 + TypeScript：展示总览、数据集、批量运行、用例详情、历史、比较和 JSON 报告。工作台读取 /api/agent-eval API；等待状态来自实际 HTTP 请求生命周期。
- AgentEvalController：提供数据集、Provider 状态、运行创建、运行历史、详情、报告和运行比较端点。
- AgentEvalService：校验固定用例定义和运行参数，执行被测 Provider、断言与 Trace，生成 summary 和质量门禁，并持久化完整报告快照。
- AgentProvider：抽象被测 Agent 执行接口。
- MockAgent：确定性关键词路由和本地工具 Stub。测试天气/网页搜索/工单、错误注入、权限拒绝、重复调用和任务完成行为，不调用网络或外部工具。
- OpenAiCompatibleProvider：可选调用配置的 OpenAI-compatible Chat Completions 接口。Provider 返回的工具请求由本地工具注册表执行，再由同一套断言器判定。它不是通用 Agent Runtime，也不会调用真实业务工具。
- AgentRunComparator：比较两个已保存的报告快照，输出逐用例变化、类别汇总、运行元数据和语义标识。
- Python / pytest：独立校验数据集和 HTTP API 契约；不复制 Java 评测实现。
- 既有 PromptOps 服务：PromptOpsService、MyBatis-Plus 和原有实体继续服务旧展示工作流。

## 执行与保存链路

1. GET 数据集与 Provider 状态，前端显示当前 Mock 默认模式及可选 Provider 的配置情况。
2. POST /api/agent-eval/runs 启动同步批量评测。只有明确选择 Provider 且服务端配置完整时才创建真实 Provider 运行；没有凭证时请求在发往外部之前被拒绝。
3. 每条用例记录执行器结果与 Agent 行为结果。execution_status=success 表示 Harness 得到了可供判断的输出；case_status=passed/failed 表示行为是否符合断言。Provider 请求失败、评测器错误和 Agent 行为失败独立归类。
4. Trace 记录输入接收、工具选择、参数校验、工具执行、工具结果、输出生成、断言和结束事件。Trace 展示可观察事件、参数、结果、错误和实际本地代码段耗时，不表示模型思维链。
5. 报告包括用例定义快照、实际输出、每条断言、失败原因、Trace、运行模式、配置元数据、质量门禁及时间信息；保存后历史、详情与 JSON 下载读取同一份快照，不重新评分。

Agent Eval 运行快照保存在 promptops_agent_eval_run.report_json。Demo profile 使用文件型 H2，默认文件为 backend/agent-eval-demo.mv.db，可跨本机进程重启读取。测试 profile 使用隔离的内存 H2。已有数据库不作为源码数据提交。

## Provider 与配置指纹

环境变量：

| 变量 | 用途 | 默认行为 |
| --- | --- | --- |
| PROMPTOPS_PROVIDER_BASE_URL | OpenAI-compatible API 根地址 | https://api.openai.com/v1 |
| PROMPTOPS_PROVIDER_MODEL | 模型名称 | 空；Provider 未配置 |
| PROMPTOPS_PROVIDER_API_KEY | 服务端凭证 | 空；密钥不回传 |
| PROMPTOPS_PROVIDER_CONFIG_VERSION | 运维配置标签 | provider-config-v1 |
| PROMPTOPS_PROVIDER_TIMEOUT_SECONDS | 请求超时 | 30 秒 |
| PROMPTOPS_PROVIDER_MAX_RETRIES | 有限重试次数 | 2 |
| PROMPTOPS_PROVIDER_MAX_TOOL_ROUNDS | 工具往返上限 | 2 |

Provider 对超时、传输异常、HTTP 429、服务端错误、非法 JSON、空响应和结构错误分类并执行有限重试。Trace、日志和指纹不会保存 API Key；Provider 响应与请求摘要经过脱敏。自动化 Provider 测试使用本地 loopback HTTP Stub，没有外部付费请求。

报告同时保存完整配置指纹和运行时配置指纹。Prompt / Agent 版本输入框是元数据标签；它们当前不会选择不同的 Prompt 文件或 Agent 实现。运行时指纹用于识别 Provider/模型/运行设置变化，标签变化不作为实际版本回归证据。

## API

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| GET | /api/agent-eval/dataset | 固定数据集定义和版本 |
| GET | /api/agent-eval/provider/status | Provider 是否可用；不返回密钥 |
| POST | /api/agent-eval/runs | 同步执行一整套启用用例 |
| GET | /api/agent-eval/runs | 持久化运行历史 |
| GET | /api/agent-eval/runs/{runId} | 读取完整运行快照 |
| GET | /api/agent-eval/runs/{runId}/report | 下载该运行 JSON 快照 |
| GET | /api/agent-eval/comparisons?baseline_run_id={id}&candidate_run_id={id} | 比较两个实际存在的运行 |

## 比较口径

比较结果覆盖新增失败、修复成功、持续失败、持续通过、新增/缺失用例和类别变化。语义根据数据集版本、Provider/运行时指纹和版本标签区分：

- 同配置重复运行：检查 Mock 可重复性或同配置运行差异。
- Mock 元数据变化：标签变了，但执行配置没有变；不称为模型版本回归。
- Provider 配置变化：运行时指纹发生变化时标记配置比较；只有实际 Provider 的两个运行才能作为真实 Provider 运行对照。
- 数据集变化：作为不同数据集对比，新增/缺失用例单独列出。
- 历史报告缺少版本元数据：保守标记为旧运行比较。

## 本地模式

- demo：文件 H2、本地虚构种子数据和 Agent Eval API。
- test：内存 H2，用于 Spring Boot 测试。
- 默认 profile：沿用项目 MySQL 环境变量配置。

## 本地生产化测试演练栈

为练习 API/业务集成测试、指标抓取、告警和负载验证，另提供仅供本地使用的订单/退款仿真 API。它不是外部公司的真实业务系统，也不连接支付网关。

- `BusinessOrderController` 只在 `demo` / `test` profile 注册，提供 `POST /api/business/orders`、`GET /api/business/orders/{id}`、订单支付、退款和取消端点。
- `BusinessOrderService` 使用事务和 H2/MySQL 兼容表 `promptops_business_order`、`promptops_business_payment`、`promptops_business_refund`；覆盖金额规则、订单状态、部分/全额退款、幂等键、客户隔离和演示角色校验。
- `X-Demo-Actor-Id` / `X-Demo-Role` 是集成测试所需的演示身份头，不是身份认证或授权系统。支付/退款只是数据库状态变更，不能移动真实资金。
- `ProductionMetrics` 记录低基数的评测运行/用例、Provider/评测器错误、业务操作及运行耗时。不会把 Run ID、订单 ID、客户 ID、输入或模型响应放进 metric label。
- Demo profile 暴露 `/actuator/prometheus`；默认运行只暴露 health/info。Prometheus 使用 5 秒抓取，Alertmanager 通过内部 webhook 投递到只保留脱敏告警名/状态的本地 evidence sink。
- `compose.production-like.yml` 的 backend、Prometheus、Alertmanager、k6、soak runner 和 sink 在内部 Docker 网络中；固定上游本地代理仅绑定宿主机 loopback 的 `18083`、`9090`、`9093`，供 API / 监控界面访问。告警不会发送到 Slack、邮件或其它外部地址。
- H2 数据使用专属命名卷 `promptops-quality-local-20261028-data`，和已有的 demo 文件 H2、运行中的服务分离。运行负载/稳定性命令不会连接线上系统。
- Python soak runner 默认 30 分钟、每 30 秒执行完整订单流程；支持 SIGINT/SIGTERM 收尾并写出 JSON。k6 使用 constant-arrival-rate，目标为 5、10、20、100 个完整工作流/秒，各 60 秒；一个工作流包含 5 个 HTTP 步骤。上一轮独立复跑为 8,102 个工作流、40,510 个请求和 81,020 项检查；原始数据仍保留用于历史对照。
- k6 每档分别门禁工作流数、全量检查、HTTP/业务错误和 dropped iterations；`backend/tools/summarize_k6_report.py` 从原始 k6 JSON 生成带 Run ID 和本地边界说明的 JSON/Markdown 摘要。最近一轮完成 8,103 个工作流、40,515 个请求和 81,030 项检查；活跃 VU 峰值为 1，`vus_max=120` 表示最大可分配 VU 资源。报告未单独测量 100/s 阶段的 HTTP RPS；合并延迟分位数只代表该轮本机观察值。
- 本机 Docker Desktop/WSL2 资源报告为 4 vCPU、约 3.82 GiB 内存。Prometheus 在覆盖这轮负载的 5 分钟窗口中记录后端 `up` 最小值为 1，`process_cpu_usage` 最大原始值为 0.0669。它是 Spring 进程指标，不是主机 CPU 饱和度。上述 H2/Docker 观测没有生产代表性；没有配置生产 p95/SLO，也不能据此推导生产容量。原始 k6 JSON 和 Prometheus 采样分别位于 `docs/reports/business-load-rerun-20261028T141643Z-e68a5f0c.json` 与 `docs/reports/business-load-rerun-monitoring-20261028T141700Z.json`。

设计参考（采用公开架构思路，不复制实现）：[Spring Boot Actuator/Micrometer 指标](https://docs.spring.io/spring-boot/3.3/reference/actuator/metrics.html)、[Prometheus 告警规则](https://prometheus.io/docs/prometheus/latest/configuration/recording_rules/)、[Alertmanager webhook 配置](https://prometheus.io/docs/alerting/latest/configuration/) 与 [k6 constant-arrival-rate executor](https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/constant-arrival-rate/)。

复现实例命令和本轮实测结果见 [通用测试开发验收](test-development-acceptance.md) 及本轮验收报告。

详情、本地启动、报告示例和实际边界见 [README](../README.md)；当前测试集见 [Agent Eval 测试矩阵](agent-evaluation-matrix.md)；本轮真实验收见 [Phase 3 发布候选验收](phase-3-acceptance.md)。

## 已知限制

- Mock Agent 是确定性测试替身，覆盖的是可观察行为，不代表模型推理能力。
- Provider 尚无本轮真实模型运行样本和线上统计。外部请求会发送所选整套测试输入，需由运行者自行确认服务商、计费和数据策略。
- Provider 的工具由本地 Stub 实现，不会对真实业务系统执行操作。
- 订单/退款 API 是本地合成业务系统；演示头不是真实鉴权，业务状态不代表生产订单或资金。
- Prometheus/Alertmanager 配置只适配本地 Compose 服务；本轮没有接入真实公司的生产监控、值班通知或告警平台。
- 同步执行当前面向 26 条本地用例；没有队列、并发调度、多租户、用户权限、持久化审批、生产审计或生产部署。
- Prompt / Agent 版本标签尚未连接动态 Prompt 配置或版本化 Agent 实现。
- Trace 仅保存可观察轨迹，不生成模型思维链。
- 旧 PromptOps 展示页仍含本地演示数据。
