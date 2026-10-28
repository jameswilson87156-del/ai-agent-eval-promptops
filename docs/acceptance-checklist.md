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

## 部分实现或尚未实现

- Prompt/Agent 版本输入框保存元数据标签；没有绑定动态 Prompt 配置或不同 Agent Runtime。
- Provider 工具调用落到确定性本地工具 Stub，不操作真实业务系统。
- 同步执行当前适合本地 26 条数据；无队列、并发调度、取消和分布式重试。
- 无用户权限、多租户、生产审计、持久化人工审批、备份/恢复策略或生产部署。
- 未运行生产模式 MySQL 集成验收；本地验收用 demo H2。
- 未进行真实模型效果、Token、成本、外部延迟或性能基准测量。
- 原有 PromptOps 静态展示页仍包含本地演示数据。
- [x] k6 报告增加逐场景工作流/错误/检查/掉队门禁与 JSON/Markdown 汇总；最近完整轮 4/4 场景通过，8,103 工作流、40,515 请求、81,030 检查、0 失败、0 掉队；报告标明活跃 VU 峰值 1 与 VU 容量 120 的区别。
