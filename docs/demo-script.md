# 3 分钟演示脚本

## 0:00–0:30 项目定位

打开“评测总览”，先看 PromptOps Studio 首页：左侧是中文控制台导航，顶部标明本地演示、规则评测、无需 API Key。说明这是本地 Demo：输出由 Mock 生成器产生，分数来自确定性规则，不是真实 LLM、真实 AI Agent 或线上模型指标。指出当前评测实验、核心指标和右侧 `run_1022` 运行 Trace 都围绕同一次本地评测展开。

## 0:30–1:10 Prompt 与 Eval Run

在首页查看“当前评测实验”、最近评测运行和回归风险归因，说明 Dataset、Prompt、规则集、Eval Run、失败样本和 Trace 指向同一个 `run_1022`。进入“批量评测 / Eval Run”，使用搜索或“高风险 / 待 Review”筛选定位 Case，说明 Run → Case Result 的聚合关系，以及规则失败或 high/critical 风险进入 Review 队列的本地策略。

## 1:10–1:50 Trace 与规则解释

点击一个 Case 进入“Trace 与 Review”。展示输入摘要、Mock 输出、逐条规则 PASS/FAIL、Generation Trace 和模拟耗时；标记通过或退回修订，观察当前待 Review 计数与队列联动。强调结论只保存在当前页面，不是生产审批记录。

## 1:50–2:30 版本对比与迭代

进入“Prompt 版本”，说明版本链路、模板变量、评分规则和 v1/v2 对比。回到概览查看通过率、模拟耗时变化、失败归因与规则归纳的迭代建议。

## 2:30–3:00 工程化与边界

点击“运行 Mock Eval”演示重新执行。说明后端使用 Spring Boot + MyBatis-Plus，Demo/Test 使用 H2，默认配置支持 MySQL；Playwright 会先验证交互、1366/390 响应式和浏览器错误，再生成截图并确认独立端口释放。最后明确真实 LLM、真实 Agent、持久化审批、队列和权限是后续方向，不是当前已实现功能。
