# 简历表达草案

本文件用于规划简历表述，不代表所有内容已可直接投递。所有量化数据必须先落到 `docs/metrics` 或脚本输出后再写进简历。

## 1. 是否适合放简历

适合，但必须定位准确：

- 可以作为 AI 全栈 / AI 应用开发 / Java 后端方向的作品集项目。
- 不能写成真实 LLMOps 平台或真实 Agent 评测平台。
- 当前最稳妥定位是：本地 PromptOps 评测控制台 / local-rule Prompt evaluation lab。

## 2. 适合投递岗位

- AI 全栈开发实习。
- AI 应用开发实习。
- Java 后端实习。
- PromptOps / LLMOps / AI Coding 工具相关实习。
- RAG / Agent 工程方向的初级岗位，但需要说明当前未实现 RAG/Agent Runtime。

## 3. 相比普通 Java 后端项目的优势

- 领域模型更贴近 AI 应用工程：Prompt、Version、Dataset、Eval Run、Trace、Review。
- 不只是 CRUD，有规则评分、版本对比和失败归因。
- 前端真实消费 API，可展示完整用户工作流。
- 文档和截图可支持 GitHub 作品集展示。

## 4. 相比普通 Prompt demo 的优势

- 普通 Prompt demo 只展示一次输出；本项目展示 Prompt 版本迭代和回归评测。
- 有评测集、规则、Case Result、Trace，而不是只调用模型。
- 有失败样本和 Review 流程意识。
- 有后端持久化和测试，而不是单页静态样例。

## 5. 目前还虚的点

- 未接真实 LLM Provider。
- 未实现 LLM-as-Judge。
- 未实现多模型对比。
- 未实现真实 Agent Runtime。
- Review 未后端持久化。
- 指标快照和 baseline compare 文档还未沉淀。
- 当前样本规模仍是 Demo 级别。

## 6. 必须补实的数据

写进简历前建议补：

- 30 条本地 Prompt 测试样本。
- Prompt v1/v2 同 Dataset 对比。
- Pass Rate、Avg Score、Failure Case Count。
- 5 个评测维度和对应规则说明。
- 后端测试数量与通过结果。
- 前端构建结果。
- README 截图数量和生成命令。

## 7. 简历项目名称建议

推荐：

- PromptOps Studio：AI Prompt 评测与回归分析控制台。
- PromptOps Evaluation Lab：本地 Prompt 版本评测工作台。

不建议：

- AI Agent 生产评测平台。
- LLMOps 商用监控平台。
- 多模型智能评测系统。

## 8. 简历项目一句话定位

推荐：

> 搭建 PromptOps Studio 本地评测控制台，围绕 Prompt 模板管理、批量规则评测、版本对比、失败案例回放和 Generation Trace，实现可复现的 Prompt 评测闭环。

更保守版本：

> 基于 Spring Boot 3 + Vue 3 实现 PromptOps 本地实验台，通过 Mock 输出与 local-rule baseline 验证 Prompt 版本评测、Case 归因和 Trace 展示流程。

## 9. 简历 3 条 bullet 初稿

当前可用初稿：

1. 基于 Spring Boot 3、MyBatis-Plus、MySQL/H2 建模 Prompt Template、Prompt Version、Eval Dataset、Eval Case、Score Rule、Eval Run、Case Result 和 Generation Trace，实现本地可复现的 Prompt 评测数据链路。
2. 实现 RuleEvaluator 规则评分流程，支持关键词、禁用词、风险提示和输出格式校验，并通过 Eval Run 聚合 Pass Rate、Case Score、Failure Reason 和 simulated latency，辅助定位 Prompt 回归问题。
3. 使用 Vue 3 + TypeScript 构建 PromptOps 控制台，支持 Prompt 切换、Case 搜索/筛选、Trace 查看、版本对比和前端本地 Review 演示；通过 Maven/JUnit 与前端构建验证基础质量。

采集 metrics 后可增强：

1. 基于 30 条本地测试样本和 5 类评测维度输出 eval snapshot、baseline compare 和 failure case 文档，形成可追溯的 Prompt v1/v2 local-rule 评测报告。

## 10. 面试官可能追问的问题

### 这个项目有没有真实 LLM？

回答：

> 当前没有。第一阶段刻意使用 MockOutputGenerator 和 local-rule baseline，目的是先把 Prompt 版本、Dataset、Eval Run、Case Result、Trace 和 Review 的工程闭环做扎实。真实 Provider 接入需要补超时、重试、脱敏、token/cost 和审计，我没有把未实现能力写成已完成。

### 为什么不用 LLM-as-Judge？

回答：

> LLM-as-Judge 有价值，但第一阶段我先保留确定性规则基线，因为它可复现、成本低、容易解释失败原因。后续可以把 LLM-as-Judge 作为补充维度，而不是替代规则评分。

### 这个项目和普通 CRUD 有什么区别？

回答：

> 数据模型围绕 PromptOps 工作流设计，不是普通增删改查。核心链路是 Prompt Version -> Dataset -> Eval Run -> Case Result -> Rule Scoring -> Trace -> Review -> Version Compare，并且后端有测试覆盖评分和评测主流程。

### Review 是真实审批吗？

回答：

> 不是。当前 Review 结论是前端本地演示状态，后端只判断哪些 Case 应进入 Review 队列。真实审批需要用户、权限、状态机、审计日志和持久化意见，这是后续方向。

### 指标是真实模型指标吗？

回答：

> 不是。当前 Pass Rate、Score、latency 都来自本地规则和 Mock 输出。latency 是 simulated latency。后续如果接真实模型，会记录 provider、model、prompt tokens、completion tokens、成本估算和真实响应耗时。

## 11. 哪些话不能说

- “已经接入真实大模型。”
- “实现真实 AI Agent 评测。”
- “生产级 LLMOps 平台。”
- “线上稳定运行。”
- “真实用户验证。”
- “模型效果提升 X%。”
- “支持多模型对比和 LLM-as-Judge。”除非后续真正实现并有证据。

## 12. 哪些数据必须等采集后再写

- 30 条样本数量。
- Pass Rate、Avg Score、Failure Case Count。
- v1/v2 提升或下降。
- 前端 Lighthouse 分数。
- 页面首屏加载时间。
- token 成本。
- 真实模型 latency。
- Review Pass Rate。

## 13. 推荐基础句式

推荐最终口径：

> 搭建 PromptOps Studio AI 评测控制台，围绕 Prompt 模板管理、批量评测、版本对比和失败案例回放实现完整评测流程；设计相关性、完整性、格式稳定性、可执行性、风险项等评测维度，基于自建本地测试集统计 Pass Rate、Avg Score、Failure Case 等指标，辅助定位 Prompt 输出不稳定、格式错误和回归失败问题。当前阶段使用 local-rule baseline 与 Mock 输出，未接入真实 LLM。

注意：

- 上面句式中的“自建本地测试集”“Pass Rate”“Avg Score”必须在 `docs/metrics` 快照生成后再用于正式简历。
- 如果未完成 30 条样本，不要写 30。
