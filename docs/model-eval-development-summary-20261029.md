# v0.2.0 DeepSeek development 探索性评测记录

日期：2026-10-29（Asia/Shanghai）

数据集：`promptops-synthetic-refund-policy-model-eval` v0.2.0

Runner 规范化 JSON SHA-256：`f87f51dd6178780e12b620c6df293801160d45612e24d63c38b3448263bf26b2`
评测范围：development split 的 24 条合成退款规则用例；`deepseek-v4-pro` 和 `deepseek-v4.1-flash`，每个 case/model 组合计划执行 1 次。

## 结果

| 项目 | 观测值 |
| --- | ---: |
| 计划唯一组合 | 48 |
| 完成唯一组合 | 48 |
| 匹配当前草案断言的组合 | 43 |
| 未匹配当前草案断言的组合 | 5 |
| 未解决的组合 | 0 |
| HTTP 请求尝试 | 51 |
| 网关网络错误尝试 | 3 |
| 错误后额外请求 | 3 |
| Provider 报告 Token | 31,416 |
| Provider 报告费用 | 未提供，实际账单未知 |

模型分项：

| 模型 | 完成组合 | 匹配草案断言 | 未匹配 | Provider 报告 Token | 延迟 p50 / p95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `deepseek-v4-pro` | 24/24 | 22 | 2 | 15,794 | 3,962.428 / 7,704.575 ms |
| `deepseek-v4.1-flash` | 24/24 | 21 | 3 | 15,622 | 3,228.333 / 6,218.106 ms |

这里的“匹配”只表示输出字段符合当前未审批草案中的确定性检查，不代表经人工确认的正确性。每组只运行一次，p50/p95 仅是本批次完成请求的描述性延迟，不是服务 SLA、容量或模型排名。

## 保留的未匹配项

- `model-003` / `deepseek-v4.1-flash`：`allowed` 预期 `true`，实际 `false`。
- `model-009` / 两个模型：`requested_refund_cny` 预期 `1.001`，实际字段缺失（`null`）。
- `model-011` / `deepseek-v4-pro`：`reason_code` 预期 `invalid_amount`，实际 `exceeds_remaining_amount`。
- `model-012` / `deepseek-v4.1-flash`：`allowed` 预期 `null`，实际 `false`。

数据集人工审核仍为 `pending`（0/40 已批准），质量门禁为 `failed`，`benchmark_eligible=false`。未删减失败用例，也未修改断言来提高结果。

## 请求尝试与计数方法

每份运行报告保留实际 attempt。聚合器以 dataset hash、split、case ID、model ID 和 repetition 构造唯一组合；网络错误后的补请求不会重复计入该组合的确定性结果，但会保留在请求尝试数和错误历史中。合并后的 JSON 不保存原始模型回答，逐项未匹配只保留被断言字段的 expected/actual，便于复核而不把整段用户输入或回答复制到前端静态数据。

合并运行记录在本机仓库旁的 `../../outputs/model-eval-deepseek-development-20261029/model-eval-summary.json`。Runner 使用排序后的 UTF-8 JSON 计算规范化 SHA；数据审计报告对原始文件字节计算 SHA-256 `180d4db8a803502b624614e704382b29c889e1902af63aeed1a61b393e062118`，两种摘要算法不同，不能直接混比。源运行 ID：

- `model-eval-0221cb3c-b094-485b-8a3a-1c6132f5089e`
- `model-eval-93477432-541e-48b3-8f50-1d29599e9483`
- `model-eval-8d1950b8-0edc-418d-ac8a-458b6da04f47`
- `model-eval-d7836205-456d-4de0-99b0-571bfa875b5b`
- `model-eval-89759973-2517-4b03-843e-9a6e94c44d04`

当前仓库内的脱敏展示数据：[`frontend/src/data/modelEvalDevelopmentSummary.json`](../frontend/src/data/modelEvalDevelopmentSummary.json)。页面标题为“真实模型试跑”，数据仅为固定运行快照；该页不重新调用模型，真实评测仍通过独立 Runner 发起。

## 配置状态与边界

- 本机 XEM CLI 的 `PROMPTOPS_PROVIDER_API_KEY_GUOMODEL` 环境变量存在；密钥值不读取、不写入报告或仓库。
- Spring Boot Agent Eval API 的浏览器安全预检返回 HTTP 409：该应用 Provider 自己的配置没有设置。因此本轮浏览器验证没有从 UI 发出任何模型请求。CLI Runner 的 Key 配置与 Web Provider 配置是两套入口。
- 所有模型输入均为合成退款规则样例；请求发往用户选择的 XEM 路由。这里不包含公司生产系统、真实客户数据、大规模并发压测或生产监控证据。
- `--budget-yuan` 是报告元数据，不是代理网关的硬扣费上限；网关未返回费用，本记录不声称花费金额。

## 下一步

先让领域审核者逐条确认或修订 v0.2.0 的 40 条 gold 与断言，并创建新的数据集版本，不回写这轮结果。之后再对批准后的 development 集做重复性运行，并保留独立 validation；sealed-test candidate 目前仍与代码一起可见，不能称为盲测集。核对 XEM 账单后再讨论费用。通过这些门槛之前，不发布模型准确率或排名。
