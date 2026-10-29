# PromptOps Studio 演示脚本

## 面试演示主线（约 3 分钟）

演示前检查后端和前端正在运行，页面右上角显示 MOCK 默认模式。演示不需要模型凭证，也不调用外部模型。

### 0:00–0:25 说明问题和运行边界

打开 Agent Eval 总览。说明这是把固定测试集、批量执行、断言、失败诊断、Trace 和报告放到一条可复现链路里的本地工程项目。Mock 数据集 1.1.0 有 38 条原创合成用例。右侧 Provider 状态显示 OpenAI-compatible 当前未配置；所有统计来自真实保存的 Mock 运行，但不是模型效果指标。真实模型评测规则已另行设计，但样本数为 0、尚无模型运行。

### 0:25–1:00 启动真实评测

点击“运行完整测试集”，展示前端发起的 HTTP 请求、独立 Run ID、用例数和质量门禁。强调执行器成功与 Agent 行为通过是两个状态：一个用例可以被成功执行，但行为断言失败。

### 1:00–1:50 解释失败和 Trace

进入运行工作台，选择 agent-014。预期要求天气查询选择 weather_lookup，实际 Mock Agent 选择了 web_search；执行器状态 success，行为状态 failed。展开输入、预期、实际输出、失败断言和 Trace 时间线。

再切换 agent-015。预期只调用一次天气工具，实际产生两次调用。两个失败均保留在数据集和质量门禁中，未通过修改预期掩盖。

### 1:50–2:30 查看历史、报告与比较

打开运行历史，查看持久化 ID 和运行模式。打开报告中心下载服务端 JSON 快照，展示 dataset_version、provider_type、运行配置指纹、逐条 cases、assertions、traces 和质量门禁。刷新页面后重新打开同一 Run，证明历史仍可查询。

打开回归比较页，选择两次真实保存的 Mock 运行。说明页面可以列出新增失败、修复、持续失败/通过和类别变化；当前 Prompt/Agent 版本输入框是元数据标签，标签变化本身不能证明 Prompt V1/V2 效果差异。

### 2:30–3:00 说明工程和限制

项目使用 Spring Boot、Vue 3、TypeScript、文件型 H2；Maven 测试包含 Provider loopback HTTP Stub，pytest 检查 API，Playwright 检查浏览器主流程和错误态。OpenAI-compatible Provider 已实现，但本轮未请求真实模型。没有 Token、成本或真实模型延迟数据；没有队列、鉴权、多租户和生产部署。

## 测试开发工程演示（约 5 分钟）

此段展示本项目隔离的本地订单/退款仿真 API，不称作真实公司业务系统。

1. 先启动专用 Compose 栈：`docker compose -f compose.production-like.yml up -d --build backend prometheus alertmanager alert-sink proxy`。打开 `http://127.0.0.1:18083/actuator/health`、`http://127.0.0.1:9090/targets` 与 `http://127.0.0.1:9093`，展示仅本机发布的端口和 UP scrape target。
2. 执行 Maven 测试，挑选订单幂等、金额不符、退款超额、取消后支付拒绝等集成测试说明状态机和 HTTP 断言；通过与失败分别说明。
3. 展示 Prometheus 指标样本及本地告警演练证据。讲解停止隔离后端后 `PromptOpsBackendUnavailable` 进入 firing，恢复后产生 resolved；通知只写入本地 sink。
4. 展示 k6 JSON：四档目标到达率为 5/10/20/100 个完整工作流/秒、各 60 秒；每个工作流走 5 个 API 步骤。最新复跑完成 8,102 个工作流、40,510 个请求，81,020 项检查通过，0 HTTP 错误、0 业务失败、0 掉队。`vus` 活跃峰值为 2；`vus_max=120` 是分配上限，不是实测并发。整体请求率不是压力档单独 RPS；数据来自本机 H2，不代表生产 SLO/容量。
5. 展示 30 分钟 Soak JSON 的每轮健康检查、创建/支付/退款/状态回读和失败原因。运行中只看 `RUNNING`；只有最终状态 `COMPLETED` 且失败数为 0 才作为通过证据。
6. 说明 API、H2、Prometheus 和 Alertmanager 都处于隔离本地栈；测试身份头并非生产鉴权，支付/退款没有真实资金，外部告警和公司系统均未连接。

## 本地启动

终端一，从仓库根目录执行：

~~~powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
mvn -pl backend spring-boot:run
~~~

终端二：

~~~powershell
Set-Location frontend
npm ci
npm run dev
~~~

浏览器打开 http://localhost:5174。若要按 Playwright 默认端口 5179 验收，可在前端终端设置代理后启动：

~~~powershell
$env:VITE_API_PROXY_TARGET = 'http://localhost:18080'
npm run dev -- --port 5179 --strictPort
$env:AGENT_EVAL_BASE_URL = 'http://localhost:5179'
npm run check:agent-eval:live
~~~

## 旧 PromptOps 展示页

仓库仍保留早期 Dashboard、Batch Evaluation、Trace Detail、Prompt Studio、Output Compare、Failure Cases 和 Dataset 视图。这些页面中的 run_1022、80% 进度、case_093 和历史指标属于前端本地演示数据。介绍它们时需标明这是原有展示页，不是 Agent Eval API 的保存结果。
