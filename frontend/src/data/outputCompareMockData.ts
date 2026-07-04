export const outputCompareMockData = {
  page: {
    eyebrow: 'Output Compare',
    title: '输出对比',
    subtitle: '对比 Prompt 版本输出差异、评分变化与结构化质量。',
  },
  context: [
    ['Dataset', '客服意图分类'],
    ['Case ID', 'case_093'],
    ['Prompt A', 'refund_v3.0.j2'],
    ['Prompt B', 'refund_v3.1.j2'],
    ['模式', 'Local Demo / Rule-based Eval'],
    ['对比时间', '2025-05-20 10:15:32'],
  ],
  metrics: [
    { label: '综合评分对比', a: '0.78', b: '0.90', delta: '+0.12' },
    { label: '格式得分', a: '0.80', b: '1.00', delta: '+0.20' },
    { label: '完整性', a: '0.85', b: '0.95', delta: '+0.10' },
    { label: '相关性', a: '0.88', b: '0.94', delta: '+0.06' },
    { label: '推荐版本', a: 'v3.0', b: 'refund_v3.1.j2', delta: '综合表现更优' },
  ],
  outputs: {
    promptA: `{
  "answer": "可以退款，请提供订单号。",
  "type": "refund",
  "required": ["order_id"],
  "confidence": null
}`,
    promptB: `{
  "answer": "可以为您办理退款，请提供订单号或退款申请编号。",
  "type": "refund",
  "required": ["order_id", "refund_no"],
  "confidence": 0.86
}`,
  },
  diff: [
    { field: 'answer', before: '可以退款，请提供订单号。', after: '可以为您办理退款，请提供订单号或退款申请编号。', type: '修改' },
    { field: 'required', before: '["order_id"]', after: '["order_id", "refund_no"]', type: '新增' },
    { field: 'confidence', before: 'null', after: '0.86', type: '新增' },
  ],
  schemaRows: [
    ['answer', '通过', '通过', '一致'],
    ['type', '通过', '通过', '一致'],
    ['required', '通过', '通过', '一致'],
    ['confidence', '缺失', '通过', '新增字段'],
  ],
  risks: [
    ['缺少 confidence 字段', '高风险', '无', '风险降低'],
    ['answer 文案不完整', '中风险', '无', '风险降低'],
    ['required 字段不足', '中风险', '无', '风险降低'],
  ],
  decision: {
    result: 'Prompt v3.1 通过',
    recommended: 'refund_v3.1.j2',
    reason: '结构完整，信息充分，风险项已降低。',
  },
  recent: [
    ['cmp_20250520_001', '客服意图分类', 'case_093', 'refund_v3.0.j2', 'refund_v3.1.j2', '0.78 vs 0.90', '+0.12', 'refund_v3.1.j2', '通过', '2025-05-20 10:15:32'],
    ['cmp_20250519_017', '工单分类', 'case_051', 'ticket_v2.1.j2', 'ticket_v2.2.j2', '0.72 vs 0.74', '+0.02', 'ticket_v2.2.j2', '通过', '2025-05-19 16:42:11'],
    ['cmp_20250518_009', '退款意图识别', 'case_087', 'refund_v3.0.j2', 'refund_v3.1.j2', '0.65 vs 0.60', '-0.05', 'refund_v3.0.j2', '不通过', '2025-05-18 11:03:45'],
  ],
  flow: [
    ['Dataset', '测试集'],
    ['Prompt A', '版本 A'],
    ['Prompt B', '版本 B'],
    ['Score Compare', '评分对比'],
    ['Diff Insight', '差异洞察'],
    ['Recommended Version', '推荐版本'],
  ],
}
