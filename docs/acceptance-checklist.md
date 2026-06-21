# 验收清单

## Git 与安全

- [x] `.gitignore` 覆盖 Node、Maven、环境变量、日志、本地数据库、浏览器 profile、agent/cache 目录。
- [x] 仓库不包含真实 API Key、数据库密码或用户数据。
- [x] Demo/Mock 与生产边界在页面和 README 中明确标注。

## 产品闭环

- [x] Prompt 版本与当前状态可见。
- [x] Eval Run 展示数据集、Case 数、通过率、规则分、延迟与风险数。
- [x] Case Result 支持搜索、筛选和详情切换。
- [x] Trace 展示输入/输出摘要、规则检查与失败原因。
- [x] 人工 Review 可在页面内变更，并明确说明不持久化。
- [x] 规则失败或 high/critical 风险 Case 进入 Review 队列，结论变更会联动当前计数与筛选。
- [x] 版本对比展示通过率、延迟、风险和迭代建议。

## 工程验收

- [x] `npm install` 成功，审计结果为 0 个已知漏洞。
- [x] `npm run typecheck` 通过。
- [x] `npm run build` 通过。
- [x] `mvn test` 通过，9 个测试、0 失败（2 个集成测试、7 个领域单元测试）。
- [x] `npm run screenshots` 通过搜索、筛选、Prompt/Case/Trace/Review 交互断言，并生成 8 张真实浏览器截图。
- [x] 1440/1920 截图、1366/390 页面无横向溢出，浏览器无 console/page error。
- [x] 截图脚本使用独立端口，只关闭自身启动的后端、Vite 与浏览器，并确认端口释放。

## 截图资产

- [x] `docs/images/dashboard.png`
- [x] `docs/images/eval-run.png`
- [x] `docs/images/prompt-versions.png`
- [x] `docs/images/trace-review.png`
- [x] `docs/images/large/` 下包含对应 1920px 版本。
