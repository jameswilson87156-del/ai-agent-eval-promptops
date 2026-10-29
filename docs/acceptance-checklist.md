# 发布候选验收清单

本清单反映 2026-10-28 本地实测状态。完整命令、计数与证据路径见 [Phase 3 发布候选验收](phase-3-acceptance.md)。

## 已重新验证

- [x] 当前分支为 feat/agent-evaluation-platform，HEAD 保持 d608cc28a2fae8246ad33712d31199185c538639；未提交、未推送。
- [x] 固定数据集 1.0.2 有 26 个唯一用例，生成器输出与仓库 JSON 完全一致。
- [x] Maven 全量测试：29 通过，0 失败、0 错误、0 跳过；包括本地 Provider HTTP Stub 测试。
- [x] Python API 测试：7 通过，0 失败、0 错误、0 跳过。
- [x] Vue 类型检查和 Vite 生产构建通过。
- [x] npm audit --audit-level=high：0 vulnerabilities。
- [x] 两次 26 条 Mock 批量运行有不同 Run ID，行为结果可重复，agent-014 和 agent-015 失败保留。
- [x] JSON 下载与详情接口、报告接口一致；文件 H2 跨服务重启仍保留报告与历史。
- [x] 后端比较两个已保存运行，显示 Mock metadata-only 比较，未声称是真实 Prompt/模型版本比较。
- [x] 浏览器验证首页、批量运行、失败详情、Trace、历史、比较、报告下载、刷新、HTTP 503 错误显示。
- [x] 浏览器视口验证 1440×920、1366×768、390×844，桌面和移动端文档宽度无横向溢出；无 pageerror。
- [x] GitHub Actions YAML 本地解析通过；workflow 包含 push / pull_request 触发、Maven、npm、完整 pytest 套件、Mock 稳定性采样和 Playwright。
- [x] git diff --check 通过；报告、运行状态和输出日志未发现符合扫描规则的真实密钥格式。

## 已实现，本轮未由外部环境验证

- OpenAI-compatible Provider 请求超时、有限重试、限流/服务端错误、响应 JSON/结构校验、脱敏：通过 Java 单元测试和本地 loopback HTTP Stub 覆盖；未连接真实远端 Provider。
- CI workflow 本地 YAML 解析与相同步骤的本地任务通过；GitHub 托管 Actions 尚未运行。

## 通用测试开发岗位补强（2026-10-28）

- [x] 增加仅访问 loopback 的 Mock API 稳定性采样器：有界并发、预热、唯一 Run ID、固定数据集版本、逐条执行状态、断言计数与确定性输出校验。
- [x] 新增 11 项 Python 采样器单测；全量 Python/API 测试 18 通过，0 失败、0 错误、0 跳过。
- [x] 最终采样 12/12 请求成功，另有 1 次预热成功；12 个正式运行各执行 26 条用例，Mock Agent 行为 24 通过、2 失败，执行器错误 0，质量门禁按预期失败。
- [x] 13 个采样 Run ID 与 API 历史一致；使用同一独立文件 H2 重启服务后，13 条采样结果仍可读取。
- [x] 验证公网目标被拒绝、覆盖已有 JSON 被拒绝且原报告 SHA-256 未变化；workflow YAML 本地解析成功。
- [x] `mvn -pl backend test` 复跑 29 通过，0 失败、0 错误、0 跳过。
- [x] 增加仅用于隔离 H2 的订单/支付/退款仿真 API 与 HTTP 集成测试；覆盖幂等、金额、状态机、权限和非法输入。
- [x] Micrometer/Prometheus 导出业务、Agent Eval 和 HTTP 指标；Prometheus 三条规则和 Alertmanager 配置通过工具校验。
- [x] 后端不可达告警完成本地 firing/resolved 演练；后端恢复后健康状态为 UP。
- [x] k6 本地四档到达率运行与复跑：两次独立运行分别完成 8,103 和 8,102 个工作流，均无 HTTP/流程失败、检查失败或掉队；最新运行 40,510 个请求、81,020 项检查通过。最新 `vus` 活跃峰值为 2；`vus_max=120` 是最大可分配 VU 资源，不是实测并发数。延迟只作本机观察，证据见 `docs/reports/business-load-rerun-20261028T141643Z-e68a5f0c.json`。
- [x] 首次 30 分钟 Soak 因 Docker Desktop 停止而中断并单独保留；第二次按配置完成 30 分钟窗口，退出码 0，耗时 1,799.211 秒，60/60 轮通过、0 失败、360 个请求（300 个业务请求 + 60 次健康检查）。
- [ ] 未做真实模型性能测试、公司生产业务验证、外部监控/通知、生产容量压测或 SLO 验收。
- [ ] GitHub 托管 Actions 未执行；未推送分支。

完整命令、数据口径与证据路径见 [通用测试开发 JD 补强验收](test-development-acceptance.md)。

本地业务仿真扩展的 36 项 Maven 测试、Soak 单测、配置校验、告警演练和 k6 证据见 `outputs/testdev-production-like-20261028/production-like-final-acceptance.md`；此为隔离本地演练，不构成生产验收。

## 2026-10-29 真实模型试跑准备与网关鉴权结果

- [x] 新增与 38 条 Mock Agent 集分离的 3 条退款/安全评测草案；均有唯一 ID、来源、输入、预期行为和确定性检查；`approved=false`，不作为基准集。
- [x] 独立 OpenAI-compatible CLI 覆盖用量解析、JSON/业务规则断言、调用上限、草案显式确认、HTTPS 校验、401 错误体丢弃、凭证脱敏、分组 Key 预检/路由及只读模型目录；16 个本地 pytest 通过。
- [x] Draft 2020-12 Case Schema 校验：v0.1.0 与 v0.1.1 各 3 条通过，0 schema 错误；ID 唯一。
- [x] 本机 XEM 目录查询显示国模 Key 可见 15 个模型；用户随后用 `glm-5.3-flash` 与 `deepseek-v4.1-flash` 执行 6 条推理请求，6 成功、0 网关错误。
- [ ] 模型效果判定不通过有效性审查：v0.1.0 的 4 个“失败”全是未在输入中定义的精确原因码不匹配。关键业务数值/决策断言均通过；高风险注入用例两模型通过。该 2/6 汇总不能作为准确率。
- [ ] 账单未返回费用字段，实际人民币费用未知；人工评分 pending；Benchmark 不符合条件。首次独立试跑收到 HTTP 401 属于更早的鉴权尝试，不是本次成功运行。
- 安全记录：首轮 CLI 错误地将唯一配置的 `OPENAI_API_KEY` 回退用于 XEM Authorization；该值已发送到 XEM，未进入控制台输出或报告。CLI 已修复为只读取专用 `PROMPTOPS_PROVIDER_API_KEY`。若该变量保存的是 OpenAI 官方 Key，应在 OpenAI 控制台撤销并轮换。
- 证据：仓库外 `outputs/model-eval-pilot-20261029/20261029T081928Z-model-pilot-56666912-51c9-44b9-9743-c9f6cbe4a007.json`；未保存 HTTP 错误响应体或密钥。

### 2026-10-29 只读模型目录查询

- [x] 新增 `--list-models`：仅发送 OpenAI-compatible `GET /v1/models`，不发推理请求；分组 Key 分开查询，通用 Key 只查询一次；终端不回显 Key。
- [x] `py -3.12 -m pytest evals/model-eval/test_run_model_eval.py -q`：16 通过、0 失败；含本地 HTTP Stub 验证目录路径、Authorization、可见模型 ID、组 Key 分路及单个通用 Key 行为。
- [x] 用户本机实际查询成功，国模 Key 返回 15 个模型 ID。目录输出来自用户粘贴的 PowerShell 结果；尚未另存终端原始日志。

### 2026-10-29 首轮模型运行核验与数据修订

- [x] 用户在 PowerShell 执行 2 模型 × 3 用例，Run ID `model-pilot-5f1f25db-b032-4e4b-938b-e169c074acf0`：6 请求、6 成功、0 网关错误；模型返回 ID 与请求 ID 匹配。
- [x] 报告 Dataset v0.1.0 规范化 SHA-256 与当前 v0.1.0 数据匹配；6 份结果的确定性断言全部可复算；6 个 Provider Request ID 存在且唯一。
- [x] Provider 报告 843 输入 / 294 输出 / 1,137 总 Token。`billing_status=not_reported`，没有费用字段；账单金额与是否扣费无法从该报告确认。客户端 `budget_yuan=5` 不构成 XEM 端硬性额度。
- [x] 发现并记录评测缺陷：4 个 fail 都是 `reason_code` 精确值不同，原提示未声明允许枚举；不将 2/6 或任何由此计算的通过率写成模型准确率。
- [x] 新增 `dataset.v0.1.1.json` 并把 CLI 默认版本切到 v0.1.1：显式定义两种原因码，未删减历史失败记录；新增数据一致性测试。v0.1.1 尚无真实模型结果。
- [ ] 尚未按 v0.1.1 复跑，且未进行双人/人工评分。复跑前先核对 XEM 账单和 Key 限额；不得把两次不同数据集版本直接合并统计。
- 运行 JSON：`../../../outputs/model-eval-pilot-20261029/20261029T085741Z-model-pilot-5f1f25db-b032-4e4b-938b-e169c074acf0.json`；结果分析：`../../../outputs/model-eval-pilot-20261029/analysis-20261029-model-pilot-5f1f25db.md`；最新离线验收日志：`../../../outputs/model-eval-pilot-20261029/verification-v0.1.1-20261029/verification.md`。

## 部分实现或尚未实现

- Prompt/Agent 版本输入框保存元数据标签；没有绑定动态 Prompt 配置或不同 Agent Runtime。
- Provider 工具调用落到确定性本地工具 Stub，不操作真实业务系统。
- 同步执行当前适合本地 38 条 Mock 数据；无队列、并发调度、取消和分布式重试。
- 无用户权限、多租户、生产审计、持久化人工审批、备份/恢复策略或生产部署。
- 未运行生产模式 MySQL 集成验收；本地验收用 demo H2。
- 未进行真实模型效果、Token、成本、外部延迟或性能基准测量。
- 原有 PromptOps 静态展示页仍包含本地演示数据。
- [x] k6 报告增加逐场景工作流/错误/检查/掉队门禁与 JSON/Markdown 汇总；最近完整轮 4/4 场景通过，8,103 工作流、40,515 请求、81,030 检查、0 失败、0 掉队；报告标明活跃 VU 峰值 1 与 VU 容量 120 的区别。

## Agent Eval 覆盖审计与模型评测规程（2026-10-29）

- [x] Mock 数据集从 1.0.2 更新至 1.1.0：26 条旧用例保留，新增 12 条非重复覆盖；总计 38 个唯一 ID，类别数为 9/6/8/4/7/4。
- [x] 扩展 JSON 输出断言：验证 required 字段、字段类型和 additionalProperties 子集；增加 Trace 字符串最大长度断言；数据集校验器拒绝未知断言操作。
- [x] Mock 与 OpenAI-compatible 本地工具桩均拒绝 schema 外参数；Maven 包含额外参数拒绝回归。
- [x] 在 `backend/` 执行 `mvn test`：37 通过、0 失败、0 错误、0 跳过。
- [x] `PROMPTOPS_API_URL=http://127.0.0.1:28084 py -3.12 -m pytest backend/tests/test_agent_eval_api.py -q`：7 通过、0 失败、0 错误、0 跳过。
- [x] Generator 结果与提交数据逐字段一致；38 个 ID 唯一；当前实际 Mock 运行 `1cf0c464-137c-4974-8b88-33eca6981052` 为 36 通过、2 失败（agent-014/015）、0 错误、0 跳过，门禁 failed；详情与 JSON 报告接口内容一致。
- [x] `npm run check:agent-eval:live`：使用项目 CORS 白名单端口 5181 通过；两次运行均为 38 条，输出可重复，下载/持久化一致，浏览器错误数 0，1440/1366 桌面与 390 移动视口无横向溢出；Provider 未配置预检返回 409，模拟 503 显示错误。
- [x] 真实模型评测独立建立 0.1.0 清单、Case Schema、Review Schema 和 3 条待审核的 development 草案；40 条目标和 24/8/8 split 计划保留，`benchmark_eligible=false`。Draft 2020-12 schema 校验通过。
- [x] XEM v0.1.0 exploratory run completed for two `guomodel` models, 6 calls; its exact reason-code assertions had a dataset design defect and no valid model pass rate can be claimed. A corrected v0.1.1 dataset is ready but not run. No billing amount was returned; cost remains unknown.
- 验收证据目录：`../../../outputs/agent-eval-v1.1.0-verification-20261029-01/`，包含 Mock 运行报告、报告摘要、pytest 日志和浏览器截图。
