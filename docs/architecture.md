# 架构说明

## 目标

项目围绕“可解释、可追溯、可本地复现”构建 PromptOps 评测闭环。当前阶段不接真实 LLM，以确定性 Mock 输出和规则评分验证领域模型、API 与展示流程。

## 分层

- Vue 前端：读取摘要、Prompt、当前 Run、版本对比和 Trace；消费后端给出的 `riskLevel` / `reviewRequired`，负责搜索、筛选、Case 导航与非持久化 Review 结论演示。
- Spring MVC API：暴露 Prompt、Dataset、Case、Rule、Eval Run 和 Trace 接口。
- `GlobalExceptionHandler`：API 层使用 `@RestControllerAdvice` 统一错误响应；`ResponseStatusException`、参数校验异常和兜底异常统一返回 `code` / `message` / `timestamp`，参数校验错误额外返回字段级 `errors`。这是后端工程化能力，不是 AI 能力。
- `PromptOpsService`：协调版本选择、Run 执行、Case 结果落库、Trace 写入和版本对比。
- `MockOutputGenerator`：根据版本与 Case 生成确定性输出，不进行模型调用。
- `RuleEvaluator`：执行 `MUST_CONTAIN`、`FORBIDDEN_WORD`、`RISK_NOTICE_REQUIRED`、`FORMAT` 四类规则并计算加权分数；JSON 规则使用 Jackson 做确定性语法解析。
- Service 层可观测性：使用 SLF4J 记录 Eval Run 开始 / 完成、demo 数据初始化、规则评分失败、`variables_json` 序列化 / 解析异常等关键事件。日志只记录 ID、数量、状态和截断摘要，不记录密码、Token、完整 prompt 或完整大字段；这是后端工程化和可观测性能力，不是 AI 能力。
- `EvaluationPolicy`：统一判断 Review 队列与版本回归风险。规则失败或 high/critical 风险 Case 需要 Review；该判断不等同于持久化审批。
- MyBatis-Plus Mapper：持久化 PromptOps 领域实体。

## 数据链路

```text
prompt_template ──< prompt_version
       │
       └── scenario ──> eval_dataset ──< eval_case ──< score_rule
                                      │
prompt_version + eval_dataset ──> eval_run ──< eval_case_result
                                      └──────< generation_trace
```

`Eval Run` 保存一次版本与评测集组合的聚合结果；`Eval Case Result` 保存逐 Case 状态、评分、模拟耗时、输出与解释；`Generation Trace` 保存输入/输出摘要和失败原因，支持按 Run 与 Case 查询。

`prompt_version.variables_json` 存储标准 JSON 数组，例如 `[{"name":"customerType","description":"客户类型","required":true}]`。Java 侧由 Jackson 负责序列化 / 反序列化为 `PromptVariableDto` 列表；H2 demo/test schema 使用 CLOB 承载 JSON 字符串以保持兼容。前端接口仍返回 `variables` 数组，不感知底层存储格式。

## 运行模式

- 默认：MySQL，配置来自环境变量。
- `demo`：内存 H2，读取 `schema-h2.sql` 并写入虚构种子数据，适合本地演示和截图。
- `test`：内存 H2，运行 Spring Boot 集成测试。

Demo、测试和默认模式共用同一服务与规则评测代码；差异仅在数据源和种子数据生命周期。

## 边界

真实 LLM Provider、真实 AI Agent、Judge、任务队列、权限、审计和生产监控均未实现。后端仅返回是否需要 Review，人工 Review 结论仍只保存在当前页面，不能视为审批系统。页面中的延迟 / 耗时来自 MockOutputGenerator 的本地确定性计算，不是真实模型延迟。
