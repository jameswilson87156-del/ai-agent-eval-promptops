export const traceDetailMockData = {
  page: {
    eyebrow: 'Trace / Run Detail',
    title: '运行追踪详情',
    subtitle: '追踪单次 Eval Run 的输入、Prompt 构建、规则评分、失败归因与人工复核过程。',
  },
  context: [
    { label: 'Run ID', value: 'run_1022' },
    { label: 'Case ID', value: 'case_093' },
    { label: 'Dataset', value: '客服意图分类' },
    { label: 'Prompt', value: 'refund_v3.1.j2' },
    { label: '规则集', value: 'refund_rules_v1.6' },
    { label: '状态', value: '已完成 / 待复核', tone: 'warning' },
    { label: '模式', value: 'Local Demo / Rule-based Eval' },
  ],
  summary: {
    identity: [
      ['Run ID', 'run_1022'],
      ['Case ID', 'case_093'],
      ['测试集', '客服意图分类'],
      ['Prompt 版本', 'refund_v3.1.j2'],
      ['规则集', 'refund_rules_v1.6'],
    ],
    score: '0.86 / 1.00',
    failureReason: '缺少 confidence 字段',
    reviewStatus: '待人工复核',
  },
  promptBuild: {
    rawInput: '用户问题：如何申请退款？需要哪些材料？',
    variables: ['user_query', 'locale', 'channel'],
    values: [
      ['locale', 'zh-CN'],
      ['channel', 'web'],
    ],
    template: 'refund_v3.1.j2',
    preview: `请根据用户问题生成结构化回答，输出 JSON 且包含以下字段：
answer: string
confidence: number`,
  },
  schemaCompare: {
    mockOutput: `{
  "answer": "通过客服渠道提交退款申请，准备相关材料。"
}`,
    expectedSchema: `{
  "type": "object",
  "properties": {
    "answer": { "type": "string" },
    "confidence": { "type": "number" }
  },
  "required": ["answer", "confidence"]
}`,
    actualOutput: `{
  "answer": "通过客服渠道提交退款申请，准备相关材料。"
}`,
    checks: [
      { label: 'answer 字段存在', state: 'pass' },
      { label: 'confidence 字段缺失', state: 'fail' },
      { label: '缺少 confidence 字段', state: 'fail' },
      { label: 'Schema 校验失败', state: 'fail' },
    ],
  },
  scoring: {
    dimensions: [
      ['格式', '0.90'],
      ['完整性', '0.88'],
      ['相关性', '0.85'],
      ['合规性', '0.95'],
      ['稳定性', '0.84'],
    ],
    totalScore: '0.86 / 1.00',
    meta: [
      ['触发规则', '3 / 22'],
      ['失败规则', 'required_confidence'],
      ['风险类型', '退款保证'],
      ['回归影响', '-0.12'],
    ],
  },
  failureFix: {
    problem: '缺少 confidence 字段',
    impact: 'Schema 校验失败，输出质量分下降 -0.12',
    suggestions: [
      '在 Prompt 中明确要求输出 confidence',
      '增加 JSON Schema 示例',
      '将 case_093 加入回归测试集',
      '复跑 Batch Evaluation',
    ],
  },
  trace: {
    runId: 'run_1022',
    status: '已完成',
    completedAt: '2025-05-20 10:12:52',
    selectedNodeId: 'failure-case',
    nodes: [
      { id: 'input', label: 'Input', time: '10:12:45', status: '已完成', duration: '120ms', summary: '用户问题“如何申请退款？需要哪些材料？”', action: '查看详情', tone: 'success' },
      { id: 'prompt-build', label: 'Prompt Build', time: '10:12:45', status: '已完成', duration: '188ms', summary: '模板 refund_v3.1.j2，变量 user_query、locale、channel', action: '查看详情', tone: 'success' },
      { id: 'mock-output', label: 'Mock Output', time: '10:12:46', status: '已完成', duration: '422ms', summary: '生成模拟回答', action: '查看详情', tone: 'success' },
      { id: 'rule-evaluator', label: 'Rule Evaluator', time: '10:12:47', status: '已完成', duration: '316ms', summary: '触发规则 3 / 22', action: '查看规则', tone: 'success' },
      {
        id: 'score',
        label: 'Score',
        time: '10:12:48',
        status: '已完成',
        duration: '284ms',
        summary: '格式 0.90、完整性 0.88、相关性 0.85、合规性 0.95、稳定性 0.84、总分 0.86',
        action: '查看评分',
        tone: 'success',
        scores: [
          ['格式', '0.90'],
          ['完整性', '0.88'],
          ['相关性', '0.85'],
          ['合规性', '0.95'],
          ['稳定性', '0.84'],
          ['总分', '0.86'],
        ],
      },
      { id: 'failure-case', label: 'Failure Case', time: '10:12:48', status: '失败', duration: '95ms', summary: '缺少必填字段 confidence', action: '查看详情', tone: 'danger' },
      { id: 'human-review', label: 'Human Review', time: '10:12:52', status: '待复核', duration: '-', summary: '进入人工复核队列', action: '去复核', tone: 'warning' },
    ],
  },
  review: {
    status: '待复核',
    suggestedAction: '确认失败原因、更新 Prompt 示例、重新运行评测',
    actions: ['标记已确认', '创建回归任务'],
  },
  providers: {
    items: [
      { name: 'local-rule baseline', status: '正常', tone: 'green' },
      { name: 'mock output generator', status: '运行中', tone: 'blue' },
      { name: 'LLM-as-Judge', status: '可选 / 未配置', tone: 'gray' },
      { name: 'API Key', status: '非必需', tone: 'gray' },
    ],
    boundary: '本地演示环境，使用 Mock Output 与 Rule-based Eval，非生产环境。',
  },
  flow: [
    ['Input', '输入'],
    ['Prompt Build', 'Prompt 构建'],
    ['Mock Output', '模拟输出'],
    ['Rule Evaluator', '规则评分'],
    ['Score', '质量分'],
    ['Failure Case', '失败案例'],
    ['Human Review', '人工复核'],
  ],
}
