export const promptStudioMockData = {
  page: {
    eyebrow: 'Prompt Studio',
    title: 'Prompt 工作台',
    subtitle: '管理 Prompt 模板、版本、变量与评测结果。',
  },
  templates: [
    { name: 'refund_v3.1.j2', scenario: '客服退款流程 / JSON 输出', score: '0.86', run: '10:12', tags: ['refund', 'json'], active: true },
    { name: 'intent_router_v2.0', scenario: '意图路由 / 分类预测', score: '0.92', run: '09:58', tags: ['router', 'classification'] },
    { name: 'faq_answer_v1.4', scenario: '常见问题 / 检索增强生成', score: '0.81', run: '09:41', tags: ['faq', 'rag'] },
    { name: 'order_status_v1.2', scenario: '订单状态查询 / 结构化输出', score: '0.88', run: '09:30', tags: ['order', 'json'] },
    { name: 'escalation_email_v1.1', scenario: '升级处理 / 邮件撰写', score: '0.77', run: '昨天 18:22', tags: ['email', 'escalation'] },
  ],
  current: {
    name: 'refund_v3.1.j2',
    version: 'v3.1 当前',
    variables: [
      ['user_query', '用户输入'],
      ['locale', '地区 / 语言'],
      ['channel', '渠道'],
    ],
    prompt: `你是一名专业的客服退款助手，请根据用户输入和上下文生成结构化的退款处理结果。
要求：
- 严格按照 Schema 输出 JSON
- 识别用户意图与关键信息（订单号、金额、原因等）
- 判断是否符合退款政策，给出处理建议
- 语言：{{ locale }}，渠道：{{ channel }}

用户输入：
{{ user_query }}

输出格式（JSON）：
{{ schema }}`,
    preview: `{
  "intent": "refund_request",
  "order_id": "12345",
  "amount": 199.00,
  "reason": "quality_issue",
  "policy_match": true,
  "refund_eligible": true,
  "suggestion": "同意退款，按原支付方式退回",
  "confidence": 0.92,
  "notes": "商品存在质量问题，符合退款政策"
}`,
  },
  score: {
    total: '0.86 / 1.00',
    dimensions: [
      ['格式合规', '0.90'],
      ['相关性', '0.88'],
      ['指令遵循', '0.85'],
      ['稳定性', '0.84'],
    ],
    evalRun: '客服退款测试集 v1.2',
    updatedAt: '2025-05-20 09:58',
  },
  evalRuns: [
    ['run_1032', '通过', '0.86', '10:12'],
    ['run_1031', '通过', '0.87', '昨天 17:22'],
    ['run_1030', '失败', '0.58', '昨天 15:11'],
    ['run_1029', '通过', '0.84', '05-18'],
    ['run_1028', '通过', '0.89', '05-18'],
  ],
  diff: [
    ['新增', '变量 channel（渠道）'],
    ['新增', '输出字段 notes（备注说明）'],
    ['调整', '更强调“严格按 Schema 输出”'],
    ['移除', '冗余示例说明段落'],
  ],
  suggestions: [
    '当前版本稳定性良好，建议继续在客服退款场景使用。',
    '可补充更多边界案例，提升失败率较高的格式合规项。',
    '若扩展多语言，建议在变量中增加 language 维度。',
  ],
  history: [
    ['v3.0（上一版）', '你是一名专业的客服退款助手，请根据用户输入生成结构化的退款处理结果。'],
    ['v3.1（当前版）', '你是一名专业的客服退款助手，请根据用户输入和上下文生成结构化的退款处理结果。'],
  ],
  recentRuns: [
    ['run_1032', '客服退款测试集 v1.2', 'v3.1', '120', '0.86', '91%', '通过', '02:12', '2025-05-20 10:12:45'],
    ['run_1031', '客服退款测试集 v1.2', 'v3.1', '120', '0.87', '92%', '通过', '02:08', '2025-05-19 17:22:31'],
    ['run_1030', '客服退款测试集 v1.2', 'v3.1', '120', '0.58', '45%', '失败', '02:15', '2025-05-19 15:11:09'],
    ['run_1029', '客服退款测试集 v1.1', 'v3.0', '120', '0.84', '88%', '通过', '02:07', '2025-05-18 11:02:17'],
  ],
}
