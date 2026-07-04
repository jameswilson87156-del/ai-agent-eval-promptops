# 3 分钟演示脚本

## 0:00–0:30 项目定位

打开“评测总览”，先看 PromptOps Studio 首页：左侧是中文控制台导航，顶部标明本地演示、规则评测、无需 API Key。说明这是本地 Demo：输出由 Mock 生成器产生，分数来自确定性规则，不是真实 LLM、真实 AI Agent 或线上模型指标。指出当前评测实验、核心指标和右侧 `run_1022` 运行 Trace 都围绕同一次本地评测展开。

## 0:30–1:10 Batch Evaluation 与 Score Matrix

点击左侧“批量评测”进入 Batch Evaluation 工作台。先说明测试集、Prompt 版本、规则集、Local Demo 模式和 mock output generator 的配置关系，再查看 `run_batch_2048` 的 80% 运行进度、核心指标与右侧 Eval Run 时间线。点击 Score Matrix 中的 `case_093`，说明格式、完整性、相关性、合规性和稳定性来自本地规则演示数据，不是 AI 语义评分。

## 1:10–1:50 Trace 与规则解释

在 Batch 页面查看 `case_093` 的失败样本分析、缺少 `confidence` 字段、修复建议与 Schema 对比，再进入“运行追踪 / Trace Review”查看输入摘要、Mock 输出、逐条规则 PASS/FAIL、Generation Trace 和模拟耗时。标记通过或退回修订，强调结论只保存在当前页面，不是生产审批记录。

## 1:50–2:30 版本对比与迭代

进入“Prompt 版本”，说明版本链路、模板变量、评分规则和 v1/v2 对比。回到概览查看通过率、模拟耗时变化、失败归因与规则归纳的迭代建议。

## 2:30–3:00 工程化与边界

点击“启动批量评测”演示复用当前本地 Eval Run 接口。说明后端使用 Spring Boot + MyBatis-Plus，Demo/Test 使用 H2，默认配置支持 MySQL；Playwright 会验证 Dashboard / Batch 双向导航、Case 联动、1440×810 首屏和浏览器错误，再生成真实运行截图并确认独立端口释放。最后明确真实 LLM、真实 Agent、LLM-as-Judge、持久化审批、队列和权限是后续方向，不是当前已实现功能。
