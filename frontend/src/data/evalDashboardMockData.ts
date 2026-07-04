export interface DashboardMetric {
  label: string
  value: string
  helper: string
  delta?: string
  tone: 'blue' | 'green' | 'red' | 'neutral'
  trend: number[]
}

export interface DashboardTraceNode {
  id: string
  label: string
  time: string
  status: string
  summary: string
  action: string
  tone?: 'normal' | 'success' | 'warning' | 'danger'
}

export const evalDashboardMockData = {
  experiment: {
    dataset: '客服意图分类',
    prompt: 'refund_v3.1.j2',
    ruleSet: 'refund_rules_v1.6',
    runId: 'run_1022',
    sampleCount: '120',
    mode: 'Local Demo / Rule-based Eval',
  },
  coreMetrics: [
    {
      label: '评测通过率',
      value: '91.2%',
      helper: '相比上一轮',
      delta: '+3.6%',
      tone: 'green',
      trend: [42, 45, 43, 48, 46, 58, 55, 64],
    },
    {
      label: '输出质量分',
      value: '0.86/1.00',
      helper: '基于本地规则评分',
      delta: '+0.04',
      tone: 'blue',
      trend: [62, 58, 54, 61, 66, 72, 68, 74],
    },
    {
      label: '回归风险等级',
      value: '高风险 3/10',
      helper: '需要复核失败样本',
      delta: '+1',
      tone: 'red',
      trend: [35, 32, 34, 31, 38, 43, 40, 50],
    },
  ] satisfies DashboardMetric[],
  supportMetrics: [
    { label: 'Eval Run 数', value: '28', helper: '最近 30 天' },
    { label: 'Prompt 版本', value: 'v3.1', helper: '当前版本' },
    { label: '失败样本', value: '9 条', helper: '占比 7.5%' },
  ],
  healthTrend: {
    labels: ['5/13', '5/14', '5/15', '5/16', '5/17', '5/18', '5/19', '5/20'],
    avgScore: [54, 62, 51, 61, 66, 55, 60, 63],
    passRate: [74, 79, 71, 78, 81, 73, 77, 80],
  },
  trace: {
    runId: 'run_1022',
    status: '已完成',
    completedAt: '2025-05-20 10:12:52',
    nodes: [
      {
        id: 'input',
        label: 'Input',
        time: '10:12:45',
        status: '已接收',
        summary: '用户问题：如何申请退款？需要哪些材料？',
        action: '查看详情',
      },
      {
        id: 'prompt-build',
        label: 'Prompt Build',
        time: '10:12:45',
        status: '已构建',
        summary: '模板 refund_v3.1.j2，变量 user_query、locale、channel',
        action: '查看详情',
      },
      {
        id: 'mock-output',
        label: 'Mock Output',
        time: '10:12:46',
        status: '已生成',
        summary: '输出片段：申请退款需在订单详情页发起申请...',
        action: '查看详情',
      },
      {
        id: 'rule-evaluator',
        label: 'Rule Evaluator',
        time: '10:12:47',
        status: '规则命中',
        summary: '规则集 refund_rules_v1.6，触发规则 3 / 22',
        action: '查看规则',
      },
      {
        id: 'score',
        label: 'Score',
        time: '10:12:48',
        status: '已评分',
        summary: '格式 0.90，完整性 0.88，相关性 0.85，合规性 0.95，稳定性 0.84',
        action: '查看分数',
        tone: 'success',
      },
      {
        id: 'failure-case',
        label: 'Failure Case',
        time: '10:12:48',
        status: '回归告警',
        summary: '缺少必填字段 confidence；风险类型：退款保证',
        action: '查看详情',
        tone: 'danger',
      },
      {
        id: 'human-review',
        label: 'Human Review',
        time: '10:12:52',
        status: '待人工复核',
        summary: '进入 Review 队列，结论仅保存在前端本地演示状态',
        action: '去复核',
        tone: 'warning',
      },
    ] satisfies DashboardTraceNode[],
    scoreDimensions: [
      ['格式', '0.90'],
      ['完整性', '0.88'],
      ['相关性', '0.85'],
      ['合规性', '0.95'],
      ['稳定性', '0.84'],
    ],
  },
  recentRuns: [
    ['run_1024', '客服意图分类', 'v3.2', '120', '93%', '0.89', '已完成', '02:11'],
    ['run_1023', '工单分类', 'v1.8', '150', '94%', '0.90', '已完成', '03:02'],
    ['run_1022', '客服意图分类', 'v3.1', '120', '91%', '0.86', '已完成', '02:14'],
    ['run_1021', '退换货原因识别', 'v2.0', '100', '79%', '0.72', '需关注', '01:48'],
    ['run_1020', '纠纷升级原因', 'v1.9', '180', '93%', '0.89', '已完成', '03:37'],
    ['run_1019', '智能查询', 'v2.2', '130', '85%', '0.78', '运行中', '01:05'],
  ],
  failureAttribution: {
    ranking: [
      ['缺少必填字段', '12', '32%'],
      ['格式无效', '8', '22%'],
      ['相关性不足', '6', '16%'],
      ['风险遗漏', '5', '14%'],
      ['字段值错误', '3', '8%'],
    ],
    mainRisk: '缺少必填字段',
    impact: '-0.12',
    suggestion: '补充 confidence 字段并对齐 Schema',
    trace: 'run_1022',
  },
  providerStatus: [
    ['local-rule baseline', '正常', 'green'],
    ['mock output generator', '运行中', 'blue'],
    ['LLM-as-Judge', '可选 / 未配置', 'gray'],
    ['API Key', '非必需', 'gray'],
  ],
  loop: [
    ['Dataset', '测试集'],
    ['Prompt Version', 'Prompt 版本'],
    ['Mock Output', '模拟输出'],
    ['Rule Evaluator', '规则评测'],
    ['Score', '评分计算'],
    ['Failure Case', '失败案例'],
    ['Human Review', '人工复核'],
  ],
}
