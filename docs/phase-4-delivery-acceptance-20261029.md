# 两个测试开发项目升级与验收记录

日期：2026-10-29（Asia/Shanghai）

## 安全接手基线

| 仓库 | 分支 | 本轮开始 HEAD | 开始时状态 |
| --- | --- | --- | --- |
| PromptOps Studio | `chore/update-actions-runtime-20261029` | `2f89849a1f268e978e1f8583bc537dcbdd2013d6` | 已有多项工作区修改和未跟踪评测材料，保留并基于现有内容完成；未切换 main |
| Gitea Quality Lab | `feat/gitea-quality-lab` | `91ad1ba5022e0f814f05db6ef17460ebbd62d031` | 已有 Issue 覆盖与运行证据改动，保留；未切换 main |

两项工作都沿用已有仓库和 Git 历史。运行证据中的业务数据均为合成数据。

## PromptOps Studio

### 自动化验收

| 检查 | 命令 / 实际执行内容 | 退出码 | 结果 / 证据 |
| --- | --- | ---: | --- |
| 后端 Maven 测试 | `mvn -pl backend test` | 0 | 37 tests；37 passed、0 failed、0 errors、0 skipped |
| 前端类型检查 | `npm run typecheck`（`frontend/`） | 0 | `vue-tsc --noEmit` 成功 |
| 前端生产构建 | `npm run build`（`frontend/`） | 0 | `vue-tsc --noEmit && vite build` 成功，Vite 生成生产构建 |
| 前端依赖审计 | `npm audit --audit-level=high`（`frontend/`） | 0 | 0 vulnerabilities |
| Python 编译 | `py -3.12 -m py_compile`（model-eval 的 audit / runner / summary 与测试模块） | 0 | 全部编译成功 |
| 模型 Runner 与审计测试 | `py -3.12 -m pytest .\evals\model-eval -q` | 0 | 47 passed、0 failed、0 skipped |
| Agent Eval API pytest | `PROMPTOPS_API_URL=http://127.0.0.1:18084 py -3.12 -m pytest .\backend\tests -q --junitxml=...` | 0 | 最新 26 passed、0 failed、0 skipped；JUnit 在仓库旁 `outputs/agent-eval-live-20261029/python-api-junit-final-20261029.xml` |
| Mock 稳定性采样 | `py -3.12 .\backend\tools\check_agent_eval_stability.py --base-url http://127.0.0.1:18084 --iterations 8 --concurrency 3 --warmup-runs 1 --output ...` | 0 | 最新 8/8 成功、预热 1、失败请求 0、输出确定；p95 36.122 ms，工具标为 `observed_only`，不作为性能门槛或容量声明。报告在仓库旁 `outputs/agent-eval-live-20261029/agent-eval-stability-final-20261029.json` |
| 固定数据审计 | `py -3.12 .\evals\model-eval\audit_dataset.py ...` | 0 | 40 cases；12 checks passed、0 failed、0 skipped；人工审批 0/40；`benchmark_eligible=false`。报告：[dataset-audit-v0.2.0.json](../evals/model-eval/evidence/dataset-audit-v0.2.0.json) |
| 本地完整浏览器链路 | `npm run check:agent-eval:live`（另设 `AGENT_EVAL_BASE_URL=http://127.0.0.1:5179`，连接本地 Spring Boot demo 服务） | 0 | 最新两次 Mock Run：`e9aeb68f-1787-4f7c-b6cc-d0a2adec2f57`、`bba44f58-a17b-4a44-8c53-58c39461f564`；执行前历史 25 条；本次每次 38 条，保留 2 条 Agent 行为失败；重放一致，JSON 导出等于持久化快照，页面刷新后历史可查；`agent-014` 有失败原因与 Trace；服务 503 被 UI 显示为失败且未生成运行记录；0 浏览器错误 |
| 页面适配 | 同一 Playwright 流程：1440、1366、390 px | 0 | 总览、Mock 流程及模型摘要页均无横向溢出；桌面/移动截图见下 |

Mock 浏览器流程调用真实 HTTP API，不调用模型。Web Provider 状态显示未配置，未配置模型的请求在请求前收到 HTTP 409；因此本轮不产生浏览器付费模型调用。CLI 模型运行使用单独的 XEM Key 环境变量，Web Provider 还需独立配置。

截图：

- [模型探索页桌面版](images/model-eval-development-desktop-20261029.png)
- [模型探索页移动版](images/model-eval-development-mobile-20261029.png)
- 本机完整 Mock / 报告 / Trace 流程截图在仓库旁 `outputs/agent-eval-live-20261029/`。

### 真实模型探索批次

v0.2.0 数据集 40 条、development/validation/sealed-test candidate 为 24/8/8。已实际请求两个 XEM DeepSeek 路由：24 个 development case × 2 模型 × 每组一次，共 48 个唯一组合；3 次网关网络错误后的补请求保留为 attempts，总计 51 次请求。结果为 43 组匹配当前草案确定性断言、5 组未匹配、0 个未解决组合；质量门禁 failed。Provider 报告总计 31,416 Token，没有返回费用。Gold 人工审批 0/40，因此这不是模型准确率、排名或 Benchmark。

数据、延迟分位数、失败字段、请求尝试计数和五份源运行 ID 见[模型评测摘要](model-eval-development-summary-20261029.md)。合并输出位于仓库旁 `outputs/model-eval-deepseek-development-20261029/model-eval-summary.json`；前端读取的摘要数据已脱敏且不包含原始回答。

### Provider Key 状态

安全检查只确认了本机环境变量 `PROMPTOPS_PROVIDER_API_KEY_GUOMODEL` 存在，没有读取或打印 Key 值。模型 CLI 可通过此变量调用用户选定的 XEM 路由。Spring Boot Web Provider 不使用这个专用变量，本轮 Provider status 因其独立配置未设置而显示未配置，故 UI 的真实模型按钮不会发模型请求；HTTP 预检验证了 409 拒绝。不要把环境变量值写进报告、截图或 Git。

## Gitea Quality Lab

| 检查 | 命令 / 实际执行内容 | 退出码 | 结果 / 证据 |
| --- | --- | ---: | --- |
| Harness 单元测试 | `\.venv\Scripts\python.exe -m pytest tests\unit -q` | 0 | 27 passed、0 failed、0 skipped；包含 HTML 报告渲染和浏览器筛选/展开/移动宽度检查 |
| 本地 Gitea 集成 | `\.venv\Scripts\python.exe scripts\run.py`，Gitea 28.2.0 / Compose / SQLite | 0 | Run `20261029T150955Z-4437d857`：18 passed、0 failed、0 skipped、0 errors；JSON 与 JUnit 总数一致 |
| 报告展示 | 从已校验的 run JSON 生成项目 HTML 报告 | 0 | `report.html`；pytest-html 原报告另存为 `pytest-report.html`，不覆盖 |
| 浏览器响应式检查 | Playwright，1440×1000 和 390×844 | 0 | 27 个 Harness 单测含浏览器筛选/详情/窄屏断言；交互截图 `20261029T150955Z-4437d857-UI-001.png`；专用报告页面桌面/手机截图来自前一轮同 18-case 结果 `20261029T144534Z-ec54abd9`；证据见 [Gitea evidence log](https://github.com/jameswilson87156-del/gitea-quality-lab/blob/feat/gitea-quality-lab/reports/evidence.md) |

本项目仍是本地上游 Gitea 系统测试，不代表公司生产系统、生产监控或生产容量。

## 当前不能写入简历的内容

- 没有公司生产系统测试、生产监控值班、生产告警处置经历。
- 没有大规模生产压测、生产容量或公开互联网并发数据。
- 不能把 v0.2.0 草案断言匹配数写成模型准确率，也没有网关账单费用。
- Gitea 18/18 是本地固定版本和合成账户上的一轮结果；PromptOps 38 条是确定性 Mock Agent 行为测试。

简历可按本地测试工具、黑盒 API/UI/SQLite 自动化、可重复评测数据与失败归因来表述，并标明这是个人本地项目。
