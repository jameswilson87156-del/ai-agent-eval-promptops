# 验收清单

## 状态口径

- 已验证：仓库已有证据或历史验收记录支持的能力。
- 未验证：本轮未重新运行的前端构建、截图、安装或启动验证。
- 当前不支持：源码和文档均明确没有实现的能力。
- 后续可扩展：适合未来任务补充，但不能当作当前能力宣传的方向。

## 已验证：Git 与安全

- [x] `.gitignore` 覆盖 Node、Maven、环境变量、日志、本地数据库、浏览器 profile、agent/cache 目录。
- [x] 仓库不包含真实 API Key、数据库密码或用户数据。
- [x] Demo/Mock 与生产边界在页面和 README 中明确标注。

## 已验证：产品闭环

- [x] Prompt 版本与当前状态可见。
- [x] Eval Run 展示数据集、Case 数、通过率、规则分、模拟耗时与风险数。
- [x] Case Result 支持搜索、筛选和详情切换。
- [x] Trace 展示输入/输出摘要、规则检查与失败原因。
- [x] 人工 Review 可在页面内变更，并明确说明不持久化。
- [x] 规则失败或 high/critical 风险 Case 进入 Review 队列，结论变更会联动当前计数与筛选。
- [x] 版本对比展示通过率、模拟耗时、风险和迭代建议。

## 已验证：工程验收历史基线

- [x] `npm install` 成功，审计结果为 0 个已知漏洞。
- [x] `npm run typecheck` 通过。
- [x] `npm run build` 通过。
- [x] `mvn test` 通过，13 个测试、0 失败（6 个集成测试、7 个领域单元测试）。
- [x] `npm run screenshots` 通过搜索、筛选、Prompt/Case/Trace/Review 交互断言，并生成 8 张真实浏览器截图。
- [x] 1440/1920 截图、1366/390 页面无横向溢出，浏览器无 console/page error。
- [x] 截图脚本使用独立端口，只关闭自身启动的后端、Vite 与浏览器，并确认端口释放。

## 已验证：截图资产

- [x] `docs/images/dashboard.png`
- [x] `docs/images/eval-run.png`
- [x] `docs/images/prompt-versions.png`
- [x] `docs/images/trace-review.png`
- [x] `docs/images/large/` 下包含对应 1920px 版本。

## 已验证：README / GitHub 展示

- [x] README 顶部使用静态 Shields.io 技术栈徽章，不依赖 CI 状态、License 或公网演示地址。
- [x] README 的技术亮点速览区分已实现能力、Mock 输出和前端演示状态。
- [x] README 引用的截图路径均来自已有 `docs/images/` 文件，未新增或覆盖截图。
- [x] README 明确 Swagger UI 是本地接口文档，不是线上部署能力。

## 未验证：本轮 P0-2

- [ ] 本轮未运行 `npm install`。
- [ ] 本轮未运行 `npm run typecheck` / `npm run build` / `npm run screenshots`。
- [ ] 本轮未运行 `mvn package`。
- [ ] 本轮未启动后端、前端、数据库或 Docker。

## 当前不支持

- [ ] 真实 LLM 调用。
- [ ] 真实 AI Agent Runtime。
- [ ] 多模型对比。
- [ ] LLM-as-Judge。
- [ ] 真实模型 latency 统计。
- [ ] 向量数据库 / RAG。
- [ ] 持久化 Review 审批流、用户权限与完整审计日志。

## 后续可扩展

- [ ] 抽象 LLM Provider，并补充预算、超时、重试、脱敏日志和成本统计。
- [ ] 增加 LLM-as-Judge，同时保留确定性规则评分基线。
- [ ] 将 Eval Run 改为异步任务，补充幂等键、队列状态和失败重放。
- [ ] 为 Review 增加持久化审批、角色权限、状态机和审计记录。
