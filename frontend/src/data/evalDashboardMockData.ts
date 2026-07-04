export const evalDashboardMockData = {
  experiment: {
    dataset: '客服意图分类',
    prompt: 'refund_v3.1.j2',
    ruleSet: 'refund_rules_v1.6',
    runId: 'run_1022',
    sampleCount: '120',
    mode: 'Local Demo / Rule-based Eval',
  },
  metrics: [
    { label: '评测通过率', value: '91.2%', helper: '相比上一轮 +3.6%' },
    { label: '输出质量分', value: '0.86', helper: '本地规则加权结果' },
    { label: '回归风险', value: '高风险 3/10', helper: '需要复核失败样本' },
    { label: '待人工复核', value: '15', helper: 'Review state is frontend-only' },
  ],
  health: [
    { label: '格式有效率', value: 94 },
    { label: 'Schema 完整性', value: 88 },
    { label: '相关性命中', value: 86 },
    { label: '稳定性', value: 84 },
  ],
  recentRuns: [
    { runId: 'run_1024', dataset: '客服意图分类', prompt: 'v3.2', passRate: '93%', status: '已完成', duration: '02:11' },
    { runId: 'run_1023', dataset: '工单分类', prompt: 'v1.8', passRate: '94%', status: '已完成', duration: '03:02' },
    { runId: 'run_1022', dataset: '客服意图分类', prompt: 'v3.1', passRate: '91%', status: '待复核', duration: '02:14' },
    { runId: 'run_1021', dataset: '退款规则识别', prompt: 'v2.0', passRate: '79%', status: '需关注', duration: '01:48' },
    { runId: 'run_1020', dataset: '纠纷升级原因', prompt: 'v1.9', passRate: '93%', status: '已完成', duration: '03:37' },
  ],
  failureReasons: [
    { reason: '缺少必填字段', count: 12, ratio: '32%' },
    { reason: '格式无效', count: 8, ratio: '22%' },
    { reason: '相关性不足', count: 6, ratio: '16%' },
    { reason: '风险提示缺失', count: 5, ratio: '14%' },
  ],
  providers: [
    { name: 'local-rule baseline', status: '正常' },
    { name: 'mock output generator', status: '运行中' },
    { name: 'LLM-as-Judge', status: '未配置' },
    { name: 'API Key', status: '非必需' },
  ],
}
