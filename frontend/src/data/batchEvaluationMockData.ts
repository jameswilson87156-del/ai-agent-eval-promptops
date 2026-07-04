export interface BatchMatrixRow {
  caseId: string
  inputType: string
  promptVersion: string
  format: string
  completeness: string
  relevance: string
  compliance: string
  stability: string
  result: '通过' | '失败'
  failureReason: string
  expectedOutput: string
  actualOutput: string
  suggestion: string
}

export const batchEvaluationMockData = {
  page: {
    eyebrow: 'Batch Evaluation',
    title: '批量评测工作台',
    subtitle: '基于本地测试集、Prompt 版本与 Rule-based Eval，批量验证 AI 输出质量、格式稳定性与回归风险。',
  },
  configuration: [
    { label: '测试集', value: '客服意图分类 / 120 条样本', icon: 'dataset' },
    { label: 'Prompt 版本', value: 'refund_v3.1.j2', icon: 'prompt' },
    { label: '规则集', value: 'refund_rules_v1.6', icon: 'rules' },
    { label: '评测模式', value: 'Local Demo / Rule-based Eval', icon: 'mode' },
    { label: 'Provider', value: 'mock output generator', icon: 'provider' },
    { label: '评测维度', value: '格式、完整性、相关性、合规性、稳定性', icon: 'dimensions' },
  ],
  configurationSteps: ['选择测试集', '选择 Prompt 版本', '选择规则集', '启动批量评测'],
  run: {
    id: 'run_batch_2048',
    status: '运行中',
    sampleCount: 120,
    completed: 96,
    failed: 9,
    estimatedDuration: '02:14',
    progress: 80,
    startedAt: '2025-05-20 10:18:32',
  },
  metrics: [
    { label: '评测通过率', value: '91.2%', helper: '通过 109 / 120', tone: 'green', progress: 91.2 },
    { label: '输出质量分', value: '0.86 / 1.00', helper: 'local-rule 加权结果', tone: 'blue', progress: 86 },
    { label: '格式有效率', value: '94.5%', helper: 'JSON / Schema 规则', tone: 'green', progress: 94.5 },
    { label: '回归风险等级', value: '高风险 3 / 10', helper: '需要人工复核', tone: 'red', progress: 70 },
  ],
  trend: {
    labels: ['09:30', '09:40', '09:50', '10:00', '10:10', '10:18'],
    avgScore: [48, 58, 66, 72, 82, 86],
    passRate: [36, 48, 55, 71, 84, 91],
  },
  matrix: [
    { caseId: 'case_089', inputType: '账户查询', promptVersion: 'v3.1', format: '通过', completeness: '通过', relevance: '通过', compliance: '通过', stability: '通过', result: '通过', failureReason: '-', expectedOutput: '{ "answer": "账户状态", "confidence": 0.92 }', actualOutput: '{ "answer": "账户状态", "confidence": 0.91 }', suggestion: '当前规则全部通过，无需修复。' },
    { caseId: 'case_090', inputType: '退款政策', promptVersion: 'v3.1', format: '通过', completeness: '通过', relevance: '通过', compliance: '通过', stability: '通过', result: '通过', failureReason: '-', expectedOutput: '{ "answer": "退款说明", "confidence": 0.90 }', actualOutput: '{ "answer": "退款说明", "confidence": 0.88 }', suggestion: '当前规则全部通过，无需修复。' },
    { caseId: 'case_093', inputType: '退款咨询', promptVersion: 'v3.1', format: '通过', completeness: '缺失', relevance: '通过', compliance: '通过', stability: '通过', result: '失败', failureReason: '缺少 confidence 字段', expectedOutput: '{ "answer": "示例", "confidence": 0.86 }', actualOutput: '{ "answer": "示例" }', suggestion: '补充 Schema 约束，并在 Prompt 示例中明确 confidence 字段。' },
    { caseId: 'case_094', inputType: '账户解冻', promptVersion: 'v3.1', format: '通过', completeness: '通过', relevance: '不足', compliance: '通过', stability: '通过', result: '失败', failureReason: '相关性不足', expectedOutput: '{ "answer": "解冻步骤", "confidence": 0.84 }', actualOutput: '{ "answer": "请联系支持", "confidence": 0.62 }', suggestion: '补充账户解冻步骤的必含关键词规则。' },
    { caseId: 'case_101', inputType: '风险提示', promptVersion: 'v3.1', format: '无效', completeness: '缺失', relevance: '不足', compliance: '通过', stability: '通过', result: '失败', failureReason: '格式无效，缺少 answer', expectedOutput: '{ "answer": "风险提示", "confidence": 0.90 }', actualOutput: '{ "message": "风险提示" }', suggestion: '收紧输出字段白名单，并增加 FORMAT 规则。' },
    { caseId: 'case_107', inputType: '投诉提交', promptVersion: 'v3.1', format: '通过', completeness: '通过', relevance: '通过', compliance: '缺失', stability: '通过', result: '失败', failureReason: '合规性不足', expectedOutput: '{ "answer": "投诉流程", "risk_notice": "隐私提醒" }', actualOutput: '{ "answer": "请提交个人证明" }', suggestion: '为敏感信息场景增加 risk_notice 必填规则。' },
    { caseId: 'case_115', inputType: '活动咨询', promptVersion: 'v3.1', format: '通过', completeness: '通过', relevance: '通过', compliance: '通过', stability: '通过', result: '通过', failureReason: '-', expectedOutput: '{ "answer": "活动说明", "confidence": 0.88 }', actualOutput: '{ "answer": "活动说明", "confidence": 0.90 }', suggestion: '当前规则全部通过，无需修复。' },
  ] satisfies BatchMatrixRow[],
  timeline: [
    { label: 'Load Dataset', time: '10:18:32', state: '已完成', summary: '加载测试集“客服意图分类”，120 条样本' },
    { label: 'Build Prompt', time: '10:18:34', state: '已完成', summary: '应用 Prompt 版本 refund_v3.1.j2' },
    { label: 'Generate Mock Output', time: '10:18:36', state: '已完成', summary: '使用 mock output generator 生成输出' },
    { label: 'Run Rule Evaluator', time: '10:18:41', state: '已完成', summary: '规则集 refund_rules_v1.6，并发 6 / 22' },
    { label: 'Compute Score', time: '10:18:47', state: '运行中', summary: '计算评分与通过率' },
    { label: 'Collect Failure Cases', time: '--:--:--', state: '等待中', summary: '收集失败样本与原因归类' },
    { label: 'Ready for Human Review', time: '--:--:--', state: '等待中', summary: '生成复核清单，待人工复核' },
  ],
  failureRanking: [
    { label: '缺少 confidence 字段', count: 5, percent: 31 },
    { label: '格式无效', count: 4, percent: 25 },
    { label: '相关性不足', count: 3, percent: 19 },
    { label: '合规性不足', count: 2, percent: 12 },
    { label: '相似度过低', count: 2, percent: 12 },
  ],
  providers: [
    { name: 'local-rule baseline', status: '正常', tone: 'green' },
    { name: 'mock output generator', status: '运行中', tone: 'blue' },
    { name: 'LLM-as-Judge', status: '可选 / 未配置', tone: 'gray' },
    { name: 'API Key', status: '非必需', tone: 'gray' },
  ],
  loop: [
    ['Dataset', '测试集'],
    ['Prompt Version', 'Prompt 版本'],
    ['Rule Set', '规则集'],
    ['Batch Run', '批量运行'],
    ['Score Matrix', '评分矩阵'],
    ['Failure Cases', '失败样本'],
    ['Human Review', '人工复核'],
  ],
}
