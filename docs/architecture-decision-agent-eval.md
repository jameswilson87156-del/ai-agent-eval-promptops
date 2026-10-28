# 设计决策：本地 Agent 评测闭环与 Provider

日期：2026-10-28

## 决策

沿用仓库现有 Vue 3 / TypeScript 与 Spring Boot，不再复制建设第二套项目。Agent Eval 使用固定 JSON 数据集、同步 HTTP 执行、确定性规则断言和文件型 H2 报告快照。小型本地评测先不引入队列或新的存储服务。

Harness 默认注入 MockAgent，先验证工具选择、参数、顺序、权限、异常、结束状态与 Trace。接口 AgentProvider 可注入 OpenAiCompatibleProvider；真实 Provider 需要运行者在服务端显式配置并在 UI 选择。API Key 从环境变量读取，不写入前端、日志、Trace、报告或配置指纹。

执行状态和 Agent 行为断言分开保存。每次运行的完整报告 JSON 作为不可变快照写入 promptops_agent_eval_run；历史、详情和下载都读该快照。Trace 记录可观察的工具和断言事件，不伪造思维链。

## 比较语义

回归 API 对两个已存在的 Run 做比较。它检查相同/不同数据集、用例新增或缺失、逐条通过状态变化、失败原因变化和类别统计。Prompt 与 Agent 配置输入目前仅为标签；运行时配置指纹单独记录 Provider、模型和可执行设置。仅标签不同不标记为真实版本回归。真正的 Prompt V1/V2 评测需进一步保存不可变 Prompt 模板/内容版本，并由运行适配器实际加载该版本。

## 可选 Provider 保护

OpenAiCompatibleProvider 调用 Chat Completions 风格接口。其配置支持 Base URL、模型名、超时、有限重试及工具调用轮数上限；处理超时、传输错误、429、服务器错误、非法 JSON、空输出和结构异常。敏感值在输出和 Trace 边界脱敏。自动化覆盖使用本地 loopback HTTP server，不调用互联网、不触发付费服务。默认 Mock 能在无凭证环境运行；缺模型或凭证时 Provider 运行在外发请求前失败。

## 公开设计参考

- Promptfoo：参考测试输入、预期输出、确定性断言和 Agent trajectory 检查思路。
- DeepEval：参考 Agent 工具调用正确性检查和 pytest / CI 的测试组织。
- Langfuse：参考 Dataset / Run / Trace / Experiment 的信息关联。

本项目有自己的数据格式、执行器与组件。没有复制上述项目的代码或完整实现。公开项目的许可证与参考页面见历史审计材料 outputs/agent-eval-architecture-decision.md。

## 取舍与限制

同步执行便于本地复现；以后用例量变大时再增加任务队列。报告快照让历史结果不随当前数据定义变化而重算；后续仍需数据保留与隐私管理。Mock 结果只能验证工程链路，不能代表真实模型效果。真实模型统计、动态 Prompt 版本绑定、异步调度、鉴权和生产部署尚未完成。
