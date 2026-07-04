# Eval 与 Metrics 数据规划

本文件只做规划。本轮未调用真实模型，未新增采集脚本，未生成真实 benchmark 结论。当前项目已有 local-rule 评测闭环，可以作为第一轮指标采集基础。

## 1. 指标口径总原则

- 只写可追溯到源码、测试、脚本输出或 `docs/metrics` 快照的数据。
- Mock 数据必须标注 Mock / Local Demo。
- simulated latency 不能写成真实模型 latency。
- 未接真实模型前，不写 token、成本、Provider SLA。
- 指标进入简历前，必须有保存文件或命令输出作为证据。

## 2. 功能规模类指标

| 指标 | 含义 | 如何采集 | 脚本建议 | 保存位置 | 是否可写简历 / 写法 | 不能写的情况 | 接真实模型后的 token/成本记录 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Prompt 模板数量 | 已建模的 Prompt Template 数 | 查询 `GET /api/prompts` 或数据库 `prompt_template` | 下一阶段 `scripts/collect-metrics` | `docs/metrics/eval_snapshot.md` | 可写“建模并管理 N 个 Prompt 模板” | N 未采集或来自手猜 | 与 token 无关 |
| Prompt 版本数量 | 所有 Prompt Version 数 | `GET /api/dashboard/summary.promptVersions` | 复用后端 API | `eval_snapshot.md` | 可写“跟踪 N 个 Prompt 版本” | 只看 README 未跑 API | 记录每版本 provider/model 配置 |
| 测试集数量 | Eval Dataset 数 | 数据库或新增 API | 新增 metrics collector | `eval_snapshot.md` | 可写“构建 N 个本地评测集” | 未落库、未快照 | 记录每测试集是否触发真实调用 |
| 测试样本数量 | Eval Case 数 | `summary.testCases` 或 DB | 复用后端 API | `eval_snapshot.md` | 可写“自建 N 条测试样本” | 样本没落库或只是计划 | 每条 Case 记录 prompt/completion token |
| 评测维度数量 | 规则/评分维度数量 | 统计 `score_rule.rule_type` 去重 | metrics collector | `eval_snapshot.md` | 可写“设计 N 类规则维度” | 把未来 LLM 维度写成已实现 | 记录 judge 模型和费用 |
| Eval Run 数量 | 已执行评测次数 | `summary.evalRuns` 或 DB `eval_run` | 复用 API | `eval_snapshot.md` | 可写“记录 N 次本地 Eval Run” | 把 demo run 写成线上运行 | 每个 run 汇总 total tokens/cost |
| Failure Case 数量 | 失败样本数 | `eval_case_result.result_status != pass` | metrics collector | `failure_cases.md` | 可写“沉淀失败样本归因” | 未保存失败样本明细 | 保存失败调用 token/cost |
| Trace 节点数量 | Generation Trace 数或每 Case Trace stage 数 | `GET /api/generation-traces` 或 DB | metrics collector | `eval_snapshot.md` | 可写“落库 Generation Trace” | 把本地 trace 写成 OTEL trace | 记录 span 级 token/cost 元数据 |
| 页面数量 | 前端可访问视图/页面数 | 手工审查 + Playwright | screenshot script | `eval_snapshot.md` | 可写“实现 N 个核心页面/视图” | 未截图或不可访问 | 无关 |
| API 数量 | 后端 REST 接口数量 | Controller 审查或 OpenAPI | OpenAPI 导出 | `eval_snapshot.md` | 可写“提供 N 个 PromptOps API” | 接口未启动验证 | 无关 |

## 3. Prompt 评测类指标

| 指标 | 含义 | 如何采集 | 脚本建议 | 保存位置 | 是否可写简历 / 写法 | 不能写的情况 | 接真实模型后的 token/成本记录 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Pass Rate | 通过 Case / 总 Case | `eval_run.pass_rate` | 复用 API | `eval_snapshot.md` | 可写“基于本地规则统计 Pass Rate” | 不标注 local-rule / mock | 记录 run total cost |
| Avg Score | Case 规则分平均值 | 汇总 `eval_case_result.score` | metrics collector | `eval_snapshot.md` | 可写“统计 Avg Score” | 规则权重未说明 | 记录 judge 成本 |
| Format Valid Rate | FORMAT 规则通过率 | 统计 FORMAT `RuleCheck.passed` | API 或 DB | `baseline_compare.md` | 可写“监控结构化输出稳定性” | 把 JSON 校验写成语义质量 | 无直接 token |
| Relevance Score | 输出相关性 | 当前未实现；规划维度 | 后续 LLM-as-Judge 或人工标注 | 待未来 `eval_snapshot.md` | 当前不能写已实现 | 未接 judge 却写分数 | 记录 judge prompt/completion token |
| Completeness Score | 输出完整性 | 当前未实现；规划维度 | 后续规则+judge | 待未来 | 当前不能写已实现 | 没有规则/样本证据 | 同上 |
| Faithfulness Score | 是否忠实输入/证据 | 当前未实现；规划维度 | 后续 RAG/LLM Judge | 待未来 | 当前不能写已实现 | 当前无 RAG/证据链 | 同上 |
| Safety / Risk Score | 风险提示覆盖和高风险处理 | 当前可用 `RISK_NOTICE_REQUIRED` + riskLevel | metrics collector | `eval_snapshot.md` | 可写“基于规则识别风险提示缺失” | 写成安全合规系统 | 记录风险 judge 成本 |
| Regression Count | 版本对比中的回归数量 | `latestComparison` 或 run 差异 | metrics collector | `baseline_compare.md` | 可写“对比 Prompt v1/v2 回归风险” | 未使用同一 Dataset | 记录两版本总成本 |
| Failure Case Count | 失败 Case 总量 | `failedCases` + failure detail | metrics collector | `failure_cases.md` | 可写“归档失败样本并归因” | 没有失败详情 | 记录失败调用成本 |
| Human Review Pass Rate | 人工 Review 通过率 | 当前仅前端状态，未持久化 | 后续 Review API | 未来 `eval_snapshot.md` | 当前不能写 | 前端状态未落库 | 记录 review 后复跑成本 |

## 4. Baseline 对比类指标

| 指标 | 含义 | 如何采集 | 脚本建议 | 保存位置 | 是否可写简历 / 写法 | 不能写的情况 | 接真实模型后的 token/成本记录 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Prompt v1 vs Prompt v2 | 同一 Dataset 上版本效果对比 | `GET /api/prompts/{id}/comparisons/latest` | metrics collector | `baseline_compare.md` | 可写“基于同一评测集做版本回归对比” | 不同样本集直接比较 | 记录各版本 run cost |
| 无结构化输出约束 vs 有 JSON Schema 约束 | 验证输出格式约束收益 | 当前未实现 JSON Schema，只能规划 | 后续新增规则/样本 | 未来 | 当前不能写 | 未实现 schema 却写收益 | 记录两组调用成本 |
| 无 Few-shot vs 有 Few-shot | 验证示例对质量影响 | 当前未实现 | 后续新增 Prompt 版本 | 未来 | 当前不能写 | 没有对应版本 | 记录版本调用成本 |
| 无评测回归 vs 有评测回归 | 说明引入 Eval 后的定位能力 | 可用流程对比，但不能写量化收益 | 文档对比 | `baseline_compare.md` | 可写“建立评测回归流程” | 写成提升百分比 | 无关或记录复跑成本 |
| local-rule vs LLM-as-Judge | 规则评分和模型评分对比 | 当前只有 local-rule | 后续可选真实模型 | 未来 | 当前只能写规划 | 未调用 judge 却写结果 | 记录 judge model、tokens、cost |
| 单模型输出 vs 多模型输出 | 多 Provider 对比 | 当前未实现 | 后续 Provider 抽象 | 未来 | 当前不能写 | 未接多模型 | 每 provider 记录 tokens/cost |

## 5. 工程质量类指标

| 指标 | 含义 | 如何采集 | 脚本建议 | 保存位置 | 是否可写简历 / 写法 | 不能写的情况 | 接真实模型后的 token/成本记录 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 自动化测试数量 | JUnit / 前端验证数量 | Maven 输出、测试报告 | CI 或本地脚本 | `eval_snapshot.md` | 可写“13 个后端测试通过” | 未重新运行或失败 | 无关 |
| 前端路由/视图数量 | 可访问页面/视图 | App 审查 + Playwright | screenshot script | `eval_snapshot.md` | 可写“实现 N 个核心视图” | 视图不可访问 | 无关 |
| 后端接口数量 | REST API 数 | Controller / OpenAPI | OpenAPI 导出 | `eval_snapshot.md` | 可写“提供 N 个 REST API” | 未验证启动 | 无关 |
| npm build 是否通过 | 前端类型检查+构建 | `npm run build` | npm script | `eval_snapshot.md` | 可写“构建通过” | 本轮未运行 | 无关 |
| mvn test 是否通过 | 后端测试 | `mvn -pl backend test` | Maven | `eval_snapshot.md` | 可写“测试通过” | 本轮未运行或失败 | 无关 |
| README 截图数量 | 真实页面截图资产 | 文件检查 + screenshots script | `npm run screenshots` | `eval_snapshot.md` | 可写“生成 N 张本地截图” | 图片非真实页面或未生成 | 无关 |
| Docker 是否可运行 | 容器化能力 | 当前未验证/未规划 | 后续 docker compose | 未来 | 当前不能写 | 未提供 Docker 文件 | 真实模型时避免镜像含 API Key |
| 无 API Key fallback 是否可演示 | 无 Key 本地 Demo 能否运行 | demo profile 启动验证 | 后续 smoke script | `eval_snapshot.md` | 可写“支持无 API Key 本地演示” | 未启动验证 | 无真实调用成本 |

## 6. 性能体验类指标

| 指标 | 含义 | 如何采集 | 脚本建议 | 保存位置 | 是否可写简历 / 写法 | 不能写的情况 | 接真实模型后的 token/成本记录 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 单次 Eval 平均耗时 | 当前为 simulated latency 平均值 | `eval_run.average_latency_ms` | metrics collector | `eval_snapshot.md` | 可写“统计模拟耗时” | 写成真实模型 latency | 记录真实 provider latency 与 tokens |
| 批量评测耗时 | Eval Run 总耗时 | 当前未保存 wall-clock | 后续脚本记录 start/end | 未来 | 当前不能写 | 没有采集脚本 | 记录总 tokens/cost |
| Provider 调用耗时 | 真实模型耗时 | 当前未实现 | Provider wrapper | 未来 | 当前不能写 | 未接模型 | 记录 prompt/completion/cost |
| 页面首屏加载时间 | 前端加载体验 | Playwright performance API | 后续 `npm run perf` | 未来 | 采集后可写 | 未采集 | 无关 |
| 前端 bundle 大小 | 产物大小 | `npm run build` 输出 | build parser | `eval_snapshot.md` | 可写构建产物大小 | 未保存输出 | 无关 |
| Lighthouse 分数 | 页面质量 | Lighthouse CLI | 后续脚本 | 未来 | 采集后可写 | 未运行 Lighthouse | 无关 |

## 7. 第一轮可落地评测规划

目标：只使用本地规则评分，不调用真实模型。

范围：

- 30 条 Prompt 测试样本。
- 至少 2 个 Prompt 版本：v1 baseline、v2 constrained。
- 5 个评测维度：
  1. 格式稳定性：JSON/Markdown/Text 是否符合要求。
  2. 相关性：是否包含关键业务字段或关键词。
  3. 完整性：是否覆盖期望输出字段。
  4. 可执行性：是否给出明确分类/路由/结论。
  5. 风险项：高风险 Case 是否输出风险提示。
- local-rule baseline。
- 可选规划 LLM-as-Judge，但本轮不调用。

建议输出文件：

- `docs/metrics/eval_snapshot.md`：记录样本规模、Run 数、Pass Rate、Avg Score、失败数、命令输出。
- `docs/metrics/baseline_compare.md`：记录 v1/v2 同 Dataset 对比。
- `docs/metrics/failure_cases.md`：记录失败样本、原因分类、相关规则、修复建议。

建议下一阶段新增脚本：

- `backend` 或 `scripts` 下新增 metrics collector，只读取后端 API 或 H2/MySQL 数据。
- `frontend` 保留 `npm run screenshots`，另加只读 `npm run audit:ui` 时再考虑。
- 不在脚本中写入 API Key，不调用真实 Provider。

## 8. 简历使用规则

可以写：

> 基于自建本地评测集统计 local-rule Pass Rate、Avg Score、Failure Case Count，并沉淀 baseline compare 和 failure case 文档，用于定位 Prompt 输出格式错误、风险提示缺失和版本回归问题。

不能写：

- “真实模型准确率提升 X%”。
- “线上用户使用后效果提升”。
- “生产级 LLMOps 指标平台”。
- “已完成多模型/LLM-as-Judge/token 成本统计”。
