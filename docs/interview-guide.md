# PromptOps Studio 面试讲解指南

## 一句话定位

用固定合成数据驱动的本地 Agent 评测控制台：可重复执行 Mock Harness，也提供可选 OpenAI-compatible Provider；保存逐条断言、失败原因、Trace 和 JSON 快照。本轮实际通过的是 Mock 与本地 Stub 验收，没有真实模型评测结果。

## 重点问题与回答

### 这是一个真实 AI Agent 产品吗？

它是一个 Agent 行为评测 Harness 与本地控制台，不是具备通用规划、长期记忆和外部业务工具的生产 Agent。Mock Agent 用确定性规则验证工具选择、安全边界和任务完成；OpenAI-compatible Provider 已实现，但它仍将工具请求交给本地 Stub。

### 它有没有真实模型运行数据？

没有。本轮浏览器验收在 Provider 状态未配置时运行；Provider API 测试使用 loopback HTTP Stub，没有请求外部服务。报告中的通过率只描述本地确定性 Mock。

### 如何定义测试预期？

测试集 1.0.2 有 26 条原创合成场景，每条含输入、预期行为和可执行断言。类别包括正常任务、输入边界、工具路由/参数、工具错误、安全和完成/Trace。安全用例要求拒绝越权；当模型正确拒绝，断言应通过。

### 如何区分执行器故障和 Agent 行为失败？

execution_status 描述测试执行链路是否成功得到可检查结果；case_status 描述 Agent 是否满足预期。agent-014 的 Mock Agent 成功执行并返回了工具调用，但调用了 web_search 而不是 weather_lookup，因此 execution_status=success 且 case_status=failed。Provider 请求失败会单独记录，不把它当作 Agent 行为失败。

### 如何发现不可重复或静态结果？

运行接口每次生成新的 Run ID，按固定输入再次运行；对比相同 case 的 actual_output 与 case_status。数据库历史、详情接口、报告接口和浏览器下载可相互核对。H2 服务重启后继续读取相同报告快照。

### Trace 为什么有用？

Trace 记录可观察步骤、顺序、工具名、参数、结果、耗时和错误；可定位从输入、路由到断言失败的位置。Trace 不是模型内部推理，也不作为 Chain-of-Thought 展示。

### Provider 怎么工作？

AgentProvider 提供 Mock 和 OpenAI-compatible 实现。OpenAI-compatible Provider 使用服务端环境变量，具有超时、有限重试、对 429/5xx/非法 JSON/空响应/结构错误分类和脱敏；真实 Provider 错误与断言失败分离。测试使用本地 Stub，不需要 Key，也不产生外部调用。

### 为什么当前比较不叫 Prompt V1/V2？

界面上 Prompt 与 Agent 版本值目前是标签元数据。比较 API 基于实际保存的两次报告、数据集与运行时配置指纹区分比较语义；改变标签而未改变实际运行配置时，UI 明确显示 metadata-only comparison。要做真正版本比较，需要把标签绑定到不可变 Prompt/Agent 配置快照，再执行同一数据集。

### 通过率有什么限制？

本轮两次完整浏览器 Mock 运行都保留 agent-014 和 agent-015 两个失败：各 24/26 行为通过、2 失败、0 执行错误、0 跳过，门禁为 failed。该比例只是一轮固定 Mock 执行的描述统计，不能代表真实模型或外部任务成功率。

### 为什么使用 H2 快照？

本地演示无需安装独立数据库，同时文件可跨本地进程重启保存运行快照。生产多人使用还需迁移、备份、并发、权限和保留策略；当前版本没有这些运维能力。

### CI 验收边界是什么？

仓库有 push / pull_request workflow，涵盖 Java 17 Maven、Node 生产构建、npm audit、pytest 和 Playwright。当前只在本机运行各项检查并本地解析 YAML；分支未推送，所以没有 GitHub 远端 Actions 结果。

### 如何证明监控告警链路不是静态配置？

订单/退款仿真 API 在隔离 Docker 环境运行，Actuator/Micrometer 暴露低基数指标给 Prometheus。演练时仅停止该项目自己的 backend 容器，等待 `PromptOpsBackendUnavailable` 触发，再启动原容器，检查 Alertmanager webhook sink 收到 firing 和 resolved，并核对业务 H2 记录仍可读取。这个证据说明本地规则和通知链路可用；不代表接入了真实公司的生产指标、值班系统或告警策略。

### 业务压测的结果可以代表线上容量吗？

不可以。k6 对本地 H2 订单/退款模拟 API 以 5、10、20、100 个完整工作流/秒的目标到达率分阶段运行。最新复跑实际完成 8,102 个工作流、40,510 个 HTTP 请求，81,020 项检查通过，HTTP 错误、业务失败和 dropped iterations 均为 0。`vus` 活跃峰值是 2；`vus_max=120` 是最大可分配 VU 资源，不是并发用户数。报告的 HTTP RPS 是跨阶段总体值，没有独立给出 100/s 压力阶段 RPS。30 分钟 Soak 也只在本地隔离环境执行；这些数据证明本地脚本和仿真 API 可重复，不能证明企业生产容量或生产 SLO。

## 深挖方向

- 如何拆分测试定义、运行报告与 Trace？
- 为什么断言工具顺序比只检查工具是否存在更有用？
- Provider 的超时/重试如何防止将请求错误算成 Agent 行为错误？
- 如何安全缓存敏感输入、模型原始响应和日志？
- 怎样把配置指纹与真实不可变 Prompt/Agent 版本绑定？
- 当用例扩到数千条时，如何加入异步队列、取消、并发和预算门禁？
- Mock 能覆盖哪些危险场景，哪些必须用隔离环境的真实 Agent 集成测试？
