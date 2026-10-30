# 模型评测路由扫描记录（2026-10-29）

## 已执行扫描

- 数据集：`dataset.v0.2.0.json`，`development` split，单用例 `model-001`。
- Run ID：`model-eval-39a99811-2117-4a91-9f93-096a4f5a4eee`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-v0.2.0-route-scan-20261029\20261029T110147Z-model-eval-39a99811-2117-4a91-9f93-096a4f5a4eee.json`。
- 结果：计划 14 次请求，实际发出 6 次；5 个模型对该单用例的确定性检查通过，0 个断言失败；第 6 个模型 `hy3` 发生 `gateway_timeout`，其余 8 个模型未执行。
- 5 个成功响应共报告 3,234 个 Token。Provider 没有返回费用；`hy3` 没有返回 usage，账单状态为 `not_reported`，所以不能推断实际花费。
- 本次执行质量门禁为 `failed`，`benchmark_eligible=false`。5 次单用例通过只说明这些响应符合当前规则，不代表模型总体通过率或 Benchmark 结果。

### 已通过单用例探测

`deepseek-v4-pro`、`deepseek-v4.1-flash`、`glm-5.2`、`glm-5.3`、`glm-5.3-flash`。

### 需单独观察

`hy3` 在 Runner 原 45 秒请求超时下返回 `gateway_timeout`。该结果不能区分模型端较慢、网关阻塞或网络问题，也不能确认服务端是否已处理请求。后续探测使用 120 秒超时；这会再发起一次独立请求，先前请求潜在费用须以 XEM 账单核对。

### 首轮扫描结束时尚未探测

`hy4-preview`、`kimi-k2.8-preview`、`kimi-k3-1`、`kimi-k3.1-pro`、`mimo-v2.6-pro`、`minimax-m3`、`minimax-m3.1-flash`、`step-5-preview`。

## Runner 更新

- 增加 `--continue-on-gateway-timeout`：单次重复的有界运行中记录超时并继续下一个计划用例/模型组合；不会自动重试超时请求。`--continue-on-model-not-found` 仍只用于单用例模型路由扫描。
- 增加 `--timeout-seconds`，默认 45 秒、范围 1–300 秒，并把所选值写入 JSON 报告。
- 即使扫描继续完成，任何网关错误仍使质量门禁失败；只有显式允许的 `model_not_found` 和超时会继续，其它错误仍中止扫描。
- 扫描续跑与可配置超时经 TDD 验证：先新增测试并观察缺少功能时失败，再完成实现；截断原因标注也经 RED/GREEN 测试验证。当前模型评测测试总数为 36。

## 下一轮命令

在已配置 `PROMPTOPS_PROVIDER_API_KEY_GUOMODEL` 的 PowerShell 项目目录中运行。它对 `hy3` 用 120 秒重新探测一次，并检查尚未探测的 8 个模型；最多 9 次请求、每次最多生成 96 Token。

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-001 --repetitions 1 --models hy3,hy4-preview,kimi-k2.8-preview,kimi-k3-1,kimi-k3.1-pro,mimo-v2.6-pro,minimax-m3,minimax-m3.1-flash,step-5-preview --continue-on-model-not-found --continue-on-gateway-timeout --max-requests 9 --max-output-tokens 96 --timeout-seconds 120 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-v0.2.0-route-scan-remaining-20261029'
```

`--budget-yuan` 是报告元数据，不是 Provider 扣费硬上限。每次运行需保留独立 `run_id` 和 JSON 报告，并在使用成本或 Benchmark 结论前核对账单、扩大用例覆盖并完成人工复核。

## 第二轮扫描结果（2026-10-29 11:13 UTC）

- Run ID：`model-eval-2eda306a-603c-499b-a608-d8ecae720fa9`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-v0.2.0-route-scan-remaining-20261029\20261029T111341Z-model-eval-2eda306a-603c-499b-a608-d8ecae720fa9.json`。
- 9 次请求全部尝试；8 个请求收到响应，`minimax-m3` 返回 `model_not_found`。`hy3` 的第二次请求返回成功；其第一次超时的账单状态仍未知。
- 3 个模型（`kimi-k3.1-pro`、`mimo-v2.6-pro`、`minimax-m3.1-flash`）的 `model-001` 输出满足所有确定性断言。
- `hy3`、`hy4-preview`、`kimi-k2.8-preview`、`kimi-k3-1` 和 `step-5-preview` 的调用响应成功，但 `finish_reason=length`，completion 用满 96 Token，输出为空，JSON 断言因此失败。这轮将这 5 个标为输出上限校准对象，不能据此归因成业务规则错误。
- 该轮 `deterministic_pass=3`、`deterministic_fail=5`、`gateway_error_count=1`、`quality_gate_status=failed`、`benchmark_eligible=false`。Provider 没有报告费用，所有实际支出仍需在 XEM 账单核对。

Runner 新增 `failure_reason=output_truncated_at_token_limit` 标注，并经新增测试验证。下轮只对上述 5 个模型使用 1,024 Token 做单用例校准：

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-001 --repetitions 1 --models hy3,hy4-preview,kimi-k2.8-preview,kimi-k3-1,step-5-preview --continue-on-model-not-found --continue-on-gateway-timeout --max-requests 5 --max-output-tokens 1024 --timeout-seconds 120 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-v0.2.0-token-calibration-20261029'
```

如果完整 JSON 输出可用，下一步再用可达的 13 个模型运行 `model-001`、`model-004`、`model-022` 代表用例；排除已确认无路由的 `glm-5.1` 与 `minimax-m3`。只有完成这些样例的失败归因和人工评分后，才启动完整 development split。

## 1,024 Token 校准结果（2026-10-29 11:31 UTC）

- Run ID：`model-eval-d9ec12b9-89ca-4129-95e8-6d3b18e83f65`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-v0.2.0-token-calibration-20261029\20261029T113142Z-model-eval-d9ec12b9-89ca-4129-95e8-6d3b18e83f65.json`。
- 5 次请求全部到达模型，没有网关错误；`hy3`、`kimi-k2.8-preview`、`kimi-k3-1`、`step-5-preview` 的单条 `model-001` 输出通过全部确定性检查。
- `hy4-preview` 的 `finish_reason=length`，Provider 报告 1,024 completion tokens，未返回可解析文本；Runner 记录 `failure_reason=output_truncated_at_token_limit`。质量门禁仍为 failed，`benchmark_eligible=false`。
- Provider 未返回费用；账单金额未知。该校准只验证一条开发集用例，不能代表模型总体效果。

下一步只对 `hy4-preview` 用 2,048 Token 再做一次单用例诊断：

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-001 --repetitions 1 --models hy4-preview --max-requests 1 --max-output-tokens 2048 --timeout-seconds 120 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-token-calibration-20261029'
```

如果这次仍然以 `finish_reason=length` 截断，再考虑缩短提示或使用模型支持的结构化输出选项；不得把 Token 消耗本身误记为模型业务失败。

## `hy4-preview` 4,096 Token 命令误输入说明

用户随后粘贴的第一条 4,096 Token 命令前缀是 `、py`。PowerShell 报 `、py` 不是可识别命令；该条没有调用 Runner，也没有发出模型请求。之后用户用修正的命令完成了实际复测，结果见本节后面的 4,096 Token 校准记录。

复测时命令必须以 ASCII 字符 `py` 开始。更重要的是，当前 Runner 将 `finish_reason=length` 结果标注为未评测；下一份报告也会记录是否出现分离的 `reasoning_content`（只记有无和字符数，不保存内容），以区分推理消耗与可见答案截断。此诊断不修改历史报告，也不保证提高上限后一定得到合格 JSON。

复测时，在已配置 `PROMPTOPS_PROVIDER_API_KEY_GUOMODEL` 的原 PowerShell 会话中运行下面命令。它最多发 1 次请求；`--budget-yuan 500` 只写入报告元数据，不是网关硬性扣费上限。该命令由用户执行，Codex 未读取 Key 或自行发起请求。

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-001 --repetitions 1 --models hy4-preview --max-requests 1 --max-output-tokens 4096 --timeout-seconds 120 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-token-calibration-4096-20261029'
```

## `hy4-preview` model-004 / model-022 8,192 Token 复测

- Run ID：`model-eval-d2d9e72f-bea7-4787-ac3b-95ae7b66309a`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-8192-recheck-20261029\20261029T130932Z-model-eval-d2d9e72f-bea7-4787-ac3b-95ae7b66309a.json`。
- 计划 2 次、实际 2 次，`gateway_errors=0`，`finish_reason=stop`；2 条完整进入确定性评测并通过，每条含 JSON Schema 与 4 项业务字段断言，共 10 项检查通过。`execution_status=completed`、`quality_gate_status=passed`、`incomplete_output=0`。
- `model-004`：659 prompt / 4,440 completion / 5,099 total tokens，约 150.9 秒；金额 50.01 高于剩余额度 50.00，模型输出拒绝退款并给出 `exceeds_remaining_amount`。
- `model-022`：656 prompt / 6,007 completion / 6,663 total tokens，约 161.6 秒；跨用户退款请求被拒绝，原因码为 `unauthorized`。
- Provider 报告合计 1,315 prompt、10,447 completion、11,762 total tokens，其中 617 cached input tokens；成本字段为空，账单未报告。reasoning_content 长度分别为 15,667/21,393 字符；只记录长度，不保留推理正文。
- 相较 4,096 Token 的相同两条请求，本轮 8,192 上限允许模型完成输出；这是本模型、本配置和两条 development 样例的一轮结果。报告中的 attempt pass rate 1.0 仅描述这 2 次可评测尝试；数据仍是 exploratory draft、人工复核 pending、`benchmark_eligible=false`，不得表述为模型准确率或正式 Benchmark。
- 腾讯官方仓库说明 HY4 Preview 默认 high reasoning，并指出复杂任务可能出现过度推理；腾讯云上游指南列出最大输出 64K。XEM 是否支持同一上限或透传推理控制参数仍未验证。来源：[Tencent-Hunyuan/Hy4-preview](https://github.com/Tencent-Hunyuan/Hy4-preview)、[腾讯云混元调用指南](https://cloud.tencent.cn/document/product/1823/132252)。

## DeepSeek × 两条高风险用例交叉预检

- Run ID：`model-eval-0abe551b-2d1e-45ae-913b-f028d268e058`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-deepseek-2case-pilot-20261029\20261029T131915Z-model-eval-0abe551b-2d1e-45ae-913b-f028d268e058.json`。
- 同一报告对 `model-004`（超出剩余额度 0.01 元）和 `model-022`（跨用户退款）各运行 `deepseek-v4-pro` 与 `deepseek-v4.1-flash` 一次。4/4 请求完整返回、0 网关错误、4/4 attempts deterministic pass，共 20 项 schema/业务断言通过；所有调用 `finish_reason=stop`，均返回所请求模型名。
- `deepseek-v4-pro`：2 次合计 1,228 prompt / 97 completion / 1,325 total tokens；单请求耗时约 4.1–4.5 秒。`deepseek-v4.1-flash`：1,230 prompt / 73 completion / 1,303 total；约 3.2–8.9 秒。两模型该报告的 `provider_reported_cost=null`、`billing_status=not_reported`。
- 与前一份 HY4 两用例报告对照：HY4 在同样两条输入上也各完成并通过一次，但共用 10,447 completion tokens、耗时约 151–162 秒；DeepSeek 在这四次请求中共用 170 completion tokens。该对照只描述当前样本和代理返回的观测值，不构成模型质量、成本或速度排名；数据仍是 draft development 样例，人工复核 pending，`benchmark_eligible=false`。
- 当前链路已覆盖真实模型 HTTP 请求、模型身份回显、用量捕获、响应完整性检查、JSON Schema、业务断言、逐请求延迟和持久化 JSON 报告。下一步扩到同一批次中的 7 条分层开发样例，再做人审；不要从两条用例的 2/2 pass 推广总体结论。

下一轮建议在同一 generation configuration 下运行 7 条用例 × 2 个已确认可达的 DeepSeek 模型，共最多 14 次调用；场景覆盖正常退款、金额边界、非法金额、订单状态、缺失输入、冲突字段和越权退款：

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-005,model-004,model-008,model-013,model-017,model-020,model-022 --repetitions 1 --models deepseek-v4-pro,deepseek-v4.1-flash --continue-on-gateway-timeout --max-requests 14 --max-output-tokens 8192 --timeout-seconds 300 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-deepseek-7case-pilot-20261029'
```

## DeepSeek 7 条分层开发集 Pilot（用户执行）

- Run ID：`model-eval-e3b01b6a-f51a-41a9-8d8e-b1d1149b5918`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-deepseek-7case-pilot-20261029\20261029T132339Z-model-eval-e3b01b6a-f51a-41a9-8d8e-b1d1149b5918.json`。
- 覆盖 model-004/005/008/013/017/020/022，分别涉及金额边界、正常退款、金额校验、订单状态、缺失输入、输入冲突和授权；deepseek-v4-pro 与 deepseek-v4.1-flash 每例各运行一次，共 14 次请求。全部返回所请求模型名、`finish_reason=stop`；0 gateway errors、0 incomplete、14/14 attempts deterministic pass，共 70 项 Schema/业务断言通过。
- `deepseek-v4-pro`：7/7 通过，4,289 prompt / 324 completion / 4,613 total tokens；请求耗时约 2.97–5.46 秒。`deepseek-v4.1-flash`：7/7 通过，4,296 prompt / 253 completion / 4,549 total；约 2.68–5.21 秒。两个模型的 Provider cost 均未报告，实际账单金额未知。
- 报告 JSON 确认 `benchmark_eligible=false`、dataset `exploratory_draft`、`human_review_status=pending`；CLI 最后一项在用户粘贴中被截断，但 JSON 明确为 false。
- 这是模型集成与确定性执行链路的成功 Pilot，并覆盖 7/24 development case、每模型每例 1 次。它没有人工批准的 Gold 标注、重复稳定性分析、盲测集或生产业务数据，不能称为模型准确率、模型排名或正式 Benchmark。
- 已为人工复核创建 Excel 兼容的 UTF-8 BOM CSV：`evals/model-eval/human-review-v0.2.0.csv`，包括 40 个唯一 case 的完整输入、expected output、规则、来源引用、源数据 SHA-256，以及空白的审核人/决定/日期/备注字段。指南见 `evals/model-eval/HUMAN_REVIEW_GUIDE.md`。生成后校验：40 行、40 个唯一 ID、源 JSON SHA-256 一致、源 approved=0、所有 review_decision 留空；未修改源数据集。
- 对照 `BusinessOrderService` 与 DTO 做了 7 条样例的独立来源审查：金额边界、PAID/PARTIALLY_REFUNDED 状态、余额上限和角色范围与服务实现方向一致；同时发现 model-022 分类预期为 `unauthorized`，而 API 对非订单所有者按设计返回 404 隐藏资源存在；model-017 缺状态和 model-020 双来源金额冲突是分类器的合成边界输入，不是当前 API 持久化模型的原样状态。这不是自动判为 bug，但人工审核必须确认目标是“规则分类器”还是“API HTTP 契约”。细节写入 `evals/model-eval/HUMAN_REVIEW_GUIDE.md`。
- v0.2.0 只读结构/金额审查：40 个 ID 唯一，split 为 development/validation/sealed_test=24/8/8，approved=0。36 条可直接用单一 paid/refunded 数值计算的 `remaining_refundable_before_cny` 均与 Gold 精确一致；其余 4 条是缺值或冲突场景，预期为 null，未发现非空算术不匹配。简单文本抽取器未识别 model-037 的最后一轮纠正语句，故它不算自动核验通过；需按完整多轮消息人工确认期望金额 15。该检查只验证结构和数值关系，不等于 Gold 审批。

下一步使用清单为 v0.2.0 的 40 个 draft golden case 完成人工规则审核，再运行其余 development cases 和重复轮次；只有审核并锁定预期结果后，才汇总可用于对外展示的质量指标。Provider 账单仍需人工从 XEM 核对。

## `hy4-preview` model-004 / model-022 4,096 Token 续跑

- Run ID：`model-eval-bda5938d-dc4d-4eee-9da7-d2cb0093cb17`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-remaining-cases-20261029\20261029T125944Z-model-eval-bda5938d-dc4d-4eee-9da7-d2cb0093cb17.json`。
- 请求计划与实际发送均为 2；`gateway_errors=0`。两个请求均返回 `finish_reason=length`，各消耗满 4,096 completion tokens：model-004 为 659 prompt / 4,096 completion / 4,755 total tokens，model-022 为 656 / 4,096 / 4,752。请求分别耗时约 136.8 秒和 106.0 秒；Provider 未报告费用，账单金额未知。
- 两条均为 `evaluation_status=incomplete_output`、`deterministic_status=not_evaluated`；汇总 `incomplete_output=2`、deterministic evaluated/pass/fail 均为 0、质量门禁 failed、`benchmark_eligible=false`。这不是两条业务断言失败。
- 安全响应形状诊断显示 reasoning_content 长度分别为 14,430 和 14,621 字符，而 message content 分别只有 25 和 0 字符；推理正文未存盘。与 4,096 completion tokens 全部耗尽、`finish_reason=length` 一致，表明这两次的输出上限主要被 HY4 Preview 的长推理消耗。
- 腾讯官方模型仓库注明 Hy4 Preview 默认使用 high reasoning，并提示复杂任务可能过度推理；官方部署示例允许以 `chat_template_kwargs.reasoning_effort=no_think` 关闭思考，但这只确认模型部署参数，不证明 XEM 中转站会透传该字段。腾讯云官方调用指南列出的上游最大输出为 64K；这也不等于 XEM 路由已保证支持 64K。参考：[Tencent-Hunyuan/Hy4-preview](https://github.com/Tencent-Hunyuan/Hy4-preview)、[腾讯云混元调用指南](https://cloud.tencent.cn/document/product/1823/132252)。

下一步在保持 HY4 默认推理模式的条件下，只对这两条将上限提高到 8,192 Token，最多发 2 次请求。若仍然 `finish_reason=length`，再单独评估 XEM 是否支持推理模式参数透传；关闭推理属于不同 generation configuration，需作为独立实验记录，不能和默认模式结果混算。`--budget-yuan` 仍只是报告字段而非扣费硬上限。

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-004,model-022 --repetitions 1 --models hy4-preview --continue-on-gateway-timeout --max-requests 2 --max-output-tokens 8192 --timeout-seconds 300 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-8192-recheck-20261029'
```

## `hy4-preview` 4,096 Token 校准结果

- Run ID：`model-eval-a0c60a87-7064-4957-8f24-34294b2fd747`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-token-calibration-4096-20261029\20261029T124408Z-model-eval-a0c60a87-7064-4957-8f24-34294b2fd747.json`。
- 1 次请求，`gateway_errors=0`，返回模型为 `hy4-preview`，`finish_reason=stop`；Provider usage 为 656 prompt / 2,108 completion / 2,764 total tokens。`model-001` 的 JSON Schema 与 4 项业务断言全部通过；`deterministic_evaluated=1`、pass=1、fail=0、incomplete_output=0，质量门禁 passed。
- 安全响应诊断：`reasoning_content_present=true`，字段长度 7,517 字符；可见 message content 为 132 字符。没有保存 `reasoning_content` 本身。字符数不能直接换算为 Token 数；结合 2,048 上限时 completion 用满并截断的旧报告，这支持“推理与最终答案共享 completion 上限，2,048 不足、4,096 对本条样例足够”的解释。
- 这是单一开发集样例的一次成功校准，不是 4,096 Token 的通用要求，也不代表模型总体准确率或 Benchmark。Provider 未报告费用，实际费用仍需核对 XEM 账单；`benchmark_eligible=false`。

## `hy4-preview` 三用例预检中的网关超时

- Run ID：`model-eval-76aa4cd8-f157-4485-9ba2-f69da2ada16e`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-3case-calibration-20261029\20261029T124843Z-model-eval-76aa4cd8-f157-4485-9ba2-f69da2ada16e.json`。
- 请求计划是 `model-001`、`model-004`、`model-022` 各 1 次，共 3 次；Runner 实际只发出 `model-001` 1 次。该调用报 `error_code=gateway_timeout`，无模型输出、Provider usage、请求 ID 或费用信息；`billing_status=not_reported`，实际费用状态未知。`model-004` 和 `model-022` 未运行。
- 汇总为 `execution_status=partial_or_failed`、`gateway_error_count=1`、`deterministic_evaluated_count=0`、deterministic pass/fail 均为 0、质量门禁 failed。它是网关等待超时和执行不完整，不是 Agent 断言失败。
- 同一 model-001 在之前独立的 4,096 Token 请求中成功用时约 62 秒并通过 5 项断言；这次相同参数的另一请求超时。两次记录显示该路由存在响应时延波动，但样本太少，不能据此计算稳定性指标。超时调用可能是否计费无法从报告确认。

Runner 已更新：在单次重复的 3-case 运行中显式增加 `--continue-on-gateway-timeout` 后，会把超时作为该条错误记录，再继续下一个计划用例，不重试超时条目。调用总数仍受 `--max-requests` 限制。该行为先由本地 mock 回归测试覆盖；本轮没有发起新的模型调用。

下一步只跑尚未获得结果的两条用例，并启用超时继续。`--timeout-seconds 300` 为本地等待上限，不是 Provider SLA；最多发送两次请求，预算字段仍不是扣费硬上限。

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-004,model-022 --repetitions 1 --models hy4-preview --continue-on-gateway-timeout --max-requests 2 --max-output-tokens 4096 --timeout-seconds 300 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-remaining-cases-20261029'
```

## hy4-preview 2,048 Token 复测（2026-10-29 11:35 UTC）

- Run ID：`model-eval-0b02103f-5886-4911-9e4b-c37425b0a973`。
- 报告：`D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-token-calibration-20261029\20261029T113520Z-model-eval-0b02103f-5886-4911-9e4b-c37425b0a973.json`。
- 1 次请求成功到达模型，无网关错误；`finish_reason=length`，Provider 报告 `completion_tokens=2048`，`raw_output` 为空，失败归因为 `output_truncated_at_token_limit`。Provider 没有报告费用，实际费用未知。
- 该条继续作为输出上限/模型响应行为问题调查，不能归为业务断言失败；单条草案数据仍不具备 Benchmark 资格。

下一步只对 `hy4-preview` 将上限调至 4,096 Token：

```powershell
py -3.12 .\evals\model-eval\run_model_eval.py --dataset .\evals\model-eval\dataset.v0.2.0.json --allow-draft --execute --split development --case-ids model-001 --repetitions 1 --models hy4-preview --max-requests 1 --max-output-tokens 4096 --timeout-seconds 120 --budget-yuan 500 --output-dir 'D:\CodexData\codex-workspaces\2026-10-28\ai-agent-github-https-github-com\outputs\model-eval-hy4-token-calibration-4096-20261029'
```
