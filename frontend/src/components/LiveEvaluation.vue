<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import modelEvalEvidence from '../data/modelEvalDevelopmentSummary.json'

type Assertion = { path: string; operator: string; expected: unknown; actual: unknown; passed: boolean; description: string }
type TraceEvent = { event_id: string; sequence: number; event_type: string; timestamp: string; duration_ms: number; input_summary: string | null; output_summary: unknown; status: string; error: string | null }
type CaseResult = { run_id: string; case_id: string; title: string; category: string; risk_level: string; tags: string[]; input: Record<string, unknown>; expected_behavior: string; assertions: Assertion[]; assertion_results?: Assertion[]; actual_output: Record<string, unknown>; execution_status: string; case_status: string; failure_reason: string[] | null; started_at: string; duration_ms: number; execution_steps: TraceEvent[] }
type Summary = { total_cases: number; executed_cases: number; passed: number; failed: number; errors: number; skipped: number; provider_errors?: number; evaluator_errors?: number; pass_rate: number }
type Run = { run_id: string; state: string; evaluator_mode: string; run_mode: string; provider_type: string; model_name: string; prompt_version: string; agent_config_version: string; config_fingerprint: string; runtime_config_fingerprint: string; version_labels_semantics: string; dataset_version: string; started_at: string; completed_at: string; report_generated_at: string; duration_ms: number; quality_gate: string; summary: Summary; cases: CaseResult[]; errors: unknown[] }
type Definition = { case_id: string; title: string; category: string; description: string; input: Record<string, unknown>; expected_behavior: string; assertions: unknown[]; risk_level: string; tags: string[]; enabled: boolean }
type Dataset = { dataset_version: string; source: string; evaluator_mode: string; cases: Definition[] }
type Provider = { provider_type: string; configured: boolean; credentials_configured?: boolean; model_name: string; config_version?: string; base_url?: string; tool_runtime?: string }
type ProviderStatus = { default_provider_type: string; providers: Provider[]; real_provider_requires_explicit_selection: boolean; model_credentials_returned: boolean }
type Comparison = { comparison_id: string; comparison_semantics: string; is_version_regression: boolean; dataset_changed: boolean; configuration_metadata_complete: boolean; configuration_changed: boolean; configuration_labels_changed: boolean; run_fingerprint_changed: boolean; baseline: Record<string, unknown>; candidate: Record<string, unknown>; baseline_summary: Summary; candidate_summary: Summary; summary: { new_failures: number; fixed_successes: number; persistent_failures: number; persistent_passes: number; added_cases: number; missing_cases: number; pass_rate_delta: number }; case_changes: Array<{ case_id: string; title: string; category: string | null; baseline_status: string; candidate_status: string; change: string; baseline_failure_reason: string[] | null; candidate_failure_reason: string[] | null }>; category_changes: Array<{ category: string; baseline: { total: number; passed: number; failed: number }; candidate: { total: number; passed: number; failed: number }; failed_delta: number; case_count_delta: number }>; metadata_changes: Array<{ field: string; baseline: string; candidate: string }> }
type ModelPair = { case_id: string; requested_model: string; pair_status: string; deterministic_status: string | null; selected_run_id: string; attempt_count: number; gateway_error_attempts: number; failed_checks: Array<{ check_id: string; expected: unknown; actual: unknown }> }
type ModelSummary = { model: string; planned_pairs: number; evaluated_pairs: number; passed_pairs: number; failed_pairs: number; unresolved_pairs: number; gateway_error_attempts: number; provider_reported_usage: { prompt_tokens: number; completion_tokens: number; total_tokens: number }; billing_status: string; latency_ms: { n: number; p50_nearest_rank: number | null; p95_nearest_rank: number | null } }
type ModelEvaluationEvidence = { summary_schema_version: string; completion_status: string; quality_gate_status: string; dataset_id: string; dataset_version: string; dataset_sha256: string; selected_split: string; benchmark_eligible: boolean; human_review_status: string; billing_status: string; source_run_ids: string[]; requested_models: string[]; planned_unique_pairs: number; evaluated_unique_pairs: number; deterministic_pass_pairs: number; deterministic_fail_pairs: number; unresolved_unique_pairs: number; actual_request_attempts: number; gateway_error_attempts: number; duplicate_pair_attempts: number; per_model_summary: ModelSummary[]; pairs: ModelPair[] }
const modelEvaluation = modelEvalEvidence as ModelEvaluationEvidence

const navigation = [
  { id: 'overview', label: '总览', icon: '◫' },
  { id: 'model-eval', label: '真实模型试跑', icon: '◎' },
  { id: 'dataset', label: '测试集', icon: '▤' },
  { id: 'run', label: '运行工作台', icon: '⌁' },
  { id: 'history', label: '运行历史', icon: '◷' },
  { id: 'compare', label: '回归比较', icon: '↔' },
  { id: 'reports', label: '报告中心', icon: '⇩' },
] as const
type Page = typeof navigation[number]['id']

const page = ref<Page>('overview')
const dataset = ref<Dataset | null>(null)
const runs = ref<Run[]>([])
const currentRun = ref<Run | null>(null)
const selectedCaseId = ref('')
const providerStatus = ref<ProviderStatus | null>(null)
const navigationRevision = ref(0)
const selectedProvider = ref('mock')
const promptVersion = ref('fixed-mock-prompt-v1')
const agentConfigVersion = ref('mock-agent-v1')
const comparison = ref<Comparison | null>(null)
const baselineRunId = ref('')
const candidateRunId = ref('')
const loading = ref(true)
const busy = ref(false)
const errorMessage = ref('')
const query = ref('')
const categoryFilter = ref('全部')
const riskFilter = ref('全部')
const caseFilter = ref('全部')
const selectedDefinition = ref<Definition | null>(null)

const selectedCase = computed(() => currentRun.value?.cases.find(item => item.case_id === selectedCaseId.value) ?? currentRun.value?.cases[0] ?? null)
const categories = computed(() => ['全部', ...new Set((dataset.value?.cases ?? []).map(item => item.category))])
const latestCaseResults = computed(() => {
  const latest = new Map<string, CaseResult>()
  for (const run of runs.value) for (const result of run.cases) if (!latest.has(result.case_id)) latest.set(result.case_id, result)
  return latest
})
const filteredDefinitions = computed(() => (dataset.value?.cases ?? []).filter(item => {
  const status = latestCaseResults.value.get(item.case_id)?.case_status ?? '未运行'
  const matchesQuery = !query.value || (item.case_id + ' ' + item.title + ' ' + item.description + ' ' + item.tags.join(' ')).toLowerCase().includes(query.value.toLowerCase())
  return matchesQuery && (categoryFilter.value === '全部' || item.category === categoryFilter.value) && (riskFilter.value === '全部' || item.risk_level === riskFilter.value) && (caseFilter.value === '全部' || status === caseFilter.value)
}))
const totals = computed(() => {
  const all = runs.value.flatMap(run => run.cases)
  return {
    runCount: runs.value.length,
    cases: all.length,
    passed: all.filter(item => item.case_status === 'passed').length,
    failed: all.filter(item => item.case_status === 'failed').length,
    errors: all.filter(item => item.case_status === 'error').length,
    realRuns: runs.value.filter(run => run.run_mode === 'real_model').length,
  }
})
const selectedProviderInfo = computed(() => providerStatus.value?.providers.find(item => item.provider_type === selectedProvider.value) ?? null)
const sameDatasetRuns = computed(() => runs.value.filter(run => run.dataset_version === dataset.value?.dataset_version))
const canCompare = computed(() => baselineRunId.value && candidateRunId.value && baselineRunId.value !== candidateRunId.value)

async function api<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch('/api/agent-eval' + path, options)
  const payload = await response.json().catch(() => null)
  if (!response.ok) throw new Error(payload?.message ?? payload?.error ?? ('HTTP ' + response.status))
  return payload as T
}

function initializeComparisonChoices(history: Run[]) {
  const matching = history.filter(run => run.dataset_version === dataset.value?.dataset_version)
  if (!matching.length) return
  if (!candidateRunId.value || !matching.some(run => run.run_id === candidateRunId.value)) candidateRunId.value = matching[0].run_id
  if (!baselineRunId.value || !matching.some(run => run.run_id === baselineRunId.value) || baselineRunId.value === candidateRunId.value) {
    baselineRunId.value = matching.find(run => run.run_id !== candidateRunId.value)?.run_id ?? ''
  }
}

async function refresh() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [data, history, status] = await Promise.all([
      api<Dataset>('/dataset'),
      api<Run[]>('/runs'),
      api<ProviderStatus>('/provider/status'),
    ])
    dataset.value = data
    runs.value = history
    providerStatus.value = status
    if (currentRun.value) currentRun.value = history.find(run => run.run_id === currentRun.value?.run_id) ?? currentRun.value
    else if (history[0]) {
      currentRun.value = history[0]
      selectedCaseId.value = history[0].cases[0]?.case_id ?? ''
    }
    initializeComparisonChoices(history)
  } catch (reason) {
    errorMessage.value = reason instanceof Error ? reason.message : String(reason)
  } finally {
    loading.value = false
  }
}

async function selectRun(runId: string) {
  errorMessage.value = ''
  try {
    const saved = await api<Run>('/runs/' + encodeURIComponent(runId))
    currentRun.value = saved
    selectedCaseId.value = saved.cases[0]?.case_id ?? ''
    page.value = 'run'
  } catch (reason) {
    errorMessage.value = reason instanceof Error ? reason.message : String(reason)
  }
}

async function executeRun() {
  const navigationRevisionAtStart = navigationRevision.value
  errorMessage.value = ''
  const provider = selectedProviderInfo.value
  if (!provider?.configured) {
    errorMessage.value = selectedProvider.value === 'mock'
      ? 'Mock 执行器当前不可用，请刷新后重试。'
      : '真实模型 Provider 尚未配置。请检查服务端环境变量后再运行。'
    return
  }
  if (selectedProvider.value !== 'mock') {
    if (!provider.credentials_configured) {
      errorMessage.value = '未检测到 Provider 凭证。为避免意外外部请求，本次没有创建运行。'
      return
    }
    const confirmed = window.confirm('此操作会向已配置的模型 Provider 发送评测请求，并可能产生费用。是否继续？')
    if (!confirmed) return
  }
  busy.value = true
  try {
    const report = await api<Run>('/runs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        provider_type: selectedProvider.value,
        prompt_version: promptVersion.value.trim() || undefined,
        agent_config_version: agentConfigVersion.value.trim() || undefined,
      }),
    })
    currentRun.value = report
    selectedCaseId.value = report.cases[0]?.case_id ?? ''
    await refresh()
    currentRun.value = report
    if (navigationRevision.value === navigationRevisionAtStart) page.value = 'run'
  } catch (reason) {
    errorMessage.value = reason instanceof Error ? reason.message : String(reason)
  } finally {
    busy.value = false
  }
}

async function compareSavedRuns() {
  if (!canCompare.value) return
  errorMessage.value = ''
  comparison.value = null
  try {
    const queryString = '?baseline_run_id=' + encodeURIComponent(baselineRunId.value) + '&candidate_run_id=' + encodeURIComponent(candidateRunId.value)
    comparison.value = await api<Comparison>('/comparisons' + queryString)
  } catch (reason) {
    errorMessage.value = reason instanceof Error ? reason.message : String(reason)
  }
}

async function exportReport(run: Run | null = currentRun.value) {
  if (!run) return
  errorMessage.value = ''
  try {
    const saved = await api<Run>('/runs/' + encodeURIComponent(run.run_id) + '/report')
    const blob = new Blob([JSON.stringify(saved, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'promptops-' + run.run_id + '.json'
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
  } catch (reason) {
    errorMessage.value = reason instanceof Error ? reason.message : String(reason)
  }
}

function setPage(next: Page) {
  navigationRevision.value += 1
  page.value = next
  errorMessage.value = ''
}

function semanticsLabel(value: string) {
  const labels: Record<string, string> = {
    same_configuration_run_comparison: '同配置运行比较',
    mock_metadata_only_comparison: 'Mock 元数据比较',
    mock_runtime_configuration_comparison: 'Mock 运行时配置比较',
    configuration_label_only_comparison: '配置标签比较',
    provider_configuration_comparison: 'Provider 运行配置比较',
    dataset_change_comparison: '测试集版本变化比较',
    legacy_metadata_missing_run_comparison: '旧运行记录比较（配置元数据不完整）',
  }
  return labels[value] ?? value
}

onMounted(refresh)
</script>

<template>
  <div class="console" data-ready="true">
    <aside class="rail">
      <div class="brand"><span class="brand-mark">◈</span><div><strong>PromptOps</strong><small>AGENT EVAL LAB</small></div></div>
      <nav aria-label="主导航">
        <button v-for="item in navigation" :key="item.id" :aria-label="item.label" :aria-current="page === item.id ? 'page' : undefined" @click="setPage(item.id)">
          <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span><span>{{ item.label }}</span>
          <span v-if="item.id === 'history'" class="nav-count">{{ runs.length }}</span>
        </button>
      </nav>
      <div class="rail-foot">
        <span class="status-indicator"></span><span>LOCAL EVALUATOR</span>
        <small>{{ providerStatus?.default_provider_type === 'mock' ? 'Mock by default · no model call' : 'Provider status unavailable' }}</small>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="breadcrumbs"><span>WORKSPACE</span><b>/</b><span>AGENT EVALUATION</span><b>/</b><strong>{{ navigation.find(item => item.id === page)?.label }}</strong></div>
        <button class="refresh-button" aria-label="刷新运行记录" :disabled="loading" @click="refresh"><span>↻</span><span class="refresh-label">刷新</span></button>
      </header>

      <div v-if="errorMessage" class="alert" role="alert">
        <span class="alert-symbol">!</span><div><strong>请求未完成</strong><p>{{ errorMessage }}</p></div><button aria-label="关闭错误提示" @click="errorMessage = ''">关闭</button>
      </div>
      <div v-if="loading" class="loading-state" role="status"><span class="spinner"></span> 正在读取测试集、Provider 状态与已保存运行…</div>

      <template v-else>
        <section v-if="page === 'overview'" class="page">
          <div class="page-heading">
            <div><p class="eyebrow">EVALUATION / OVERVIEW</p><h1>Agent 评测总览</h1><p class="lede">追踪真实保存的本地评测运行、失败用例和数据集版本。</p></div>
            <span class="mode-chip"><i></i> MOCK 默认模式</span>
          </div>
          <div class="metric-grid">
            <div class="metric"><span>保存的运行</span><strong>{{ totals.runCount }}</strong><small>持久化历史记录</small></div>
            <div class="metric"><span>累计执行用例</span><strong>{{ totals.cases }}</strong><small>跨所有已保存运行</small></div>
            <div class="metric"><span>失败 / 错误</span><strong>{{ totals.failed }} <em>/</em> {{ totals.errors }}</strong><small>按运行结果累计</small></div>
            <div class="metric"><span>真实模型运行</span><strong>{{ totals.realRuns }}</strong><small>{{ totals.realRuns ? '报告含 Provider 元数据' : '尚无真实模型调用记录' }}</small></div>
          </div>

          <div class="section-heading">
            <div><p class="eyebrow">RECENT RUNS</p><h2>最近运行</h2></div>
            <button class="button-primary" :disabled="busy || loading" @click="executeRun">{{ busy ? '服务端执行中…' : '▶　运行完整测试集' }}</button>
          </div>
          <p class="helper-line">运行由后端同步执行并持久化；Mock Agent 使用确定性本地工具桩，不会请求外部模型。</p>
          <div v-if="runs.length" class="table-scroll">
            <table>
              <thead><tr><th>运行 ID / 完成时间</th><th>数据集</th><th>执行模式</th><th>通过率</th><th>通过 / 失败 / 错误</th><th>质量门禁</th><th></th></tr></thead>
              <tbody>
                <tr v-for="run in runs.slice(0, 8)" :key="run.run_id">
                  <td><code class="mono">{{ run.run_id.slice(0, 18) }}…</code><small>{{ run.completed_at }}</small></td>
                  <td>v{{ run.dataset_version }}</td><td><span :class="['mode-label', run.run_mode === 'real_model' ? 'real' : 'mock']">{{ run.run_mode === 'real_model' ? '真实模型' : 'MOCK' }}</span></td>
                  <td><strong>{{ Number(run.summary.pass_rate).toFixed(2) }}%</strong></td>
                  <td><span class="good-text">{{ run.summary.passed }}</span> / <span class="bad-text">{{ run.summary.failed }}</span> / {{ run.summary.errors }}</td>
                  <td><span :class="['state-pill', run.quality_gate === 'passed' ? 'pass' : 'fail']">{{ run.quality_gate }}</span></td>
                  <td><button class="button-link" @click="selectRun(run.run_id)">查看详情 →</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state"><div class="empty-icon">⌁</div><strong>还没有已保存的运行</strong><p>点击运行后，后端会对测试集逐条执行、保存 Trace 并生成报告。</p><button class="button-primary" :disabled="busy" @click="executeRun">开始首次评测</button></div>

          <div class="overview-lower">
            <div class="failure-summary">
              <div class="subsection-heading"><div><p class="eyebrow">LATEST DATASET</p><h2>测试集结构</h2></div><span class="subtle-badge">v{{ dataset?.dataset_version }}</span></div>
              <div class="category-bars">
                <div v-for="name in categories.slice(1)" :key="name" class="category-row">
                  <span>{{ name }}</span><div class="bar-track"><i :style="{ width: (dataset?.cases.filter(item => item.category === name).length ?? 0) / Math.max(dataset?.cases.length ?? 1, 1) * 100 + '%' }"></i></div><b>{{ dataset?.cases.filter(item => item.category === name).length ?? 0 }}</b>
                </div>
              </div>
              <p class="chart-caption">按当前后端测试集中的用例数量统计。</p>
            </div>
            <div class="provider-summary">
              <div class="subsection-heading"><div><p class="eyebrow">PROVIDER CONFIGURATION</p><h2>执行模式</h2></div><span class="state-pill pass">Mock 可用</span></div>
              <div class="provider-row"><span class="provider-symbol">M</span><div><strong>Deterministic Mock Agent</strong><small>本地规则逻辑 · deterministic_local_stub</small></div><span class="online">READY</span></div>
              <div class="provider-row"><span class="provider-symbol external">AI</span><div><strong>OpenAI compatible</strong><small>{{ providerStatus?.providers.find(item => item.provider_type === 'openai_compatible')?.credentials_configured ? '服务端检测到凭证（仅布尔状态）' : '凭证未配置；不会发起外部请求' }}</small></div><span :class="['state-pill', providerStatus?.providers.find(item => item.provider_type === 'openai_compatible')?.configured ? 'pass' : 'neutral']">{{ providerStatus?.providers.find(item => item.provider_type === 'openai_compatible')?.configured ? 'READY' : 'NOT CONFIGURED' }}</span></div>
              <button class="button-link" @click="setPage('run')">配置并打开运行工作台 →</button>
            </div>
          </div>
        </section>

        <section v-else-if="page === 'model-eval'" class="page model-eval-page">
          <div class="page-heading">
            <div><p class="eyebrow">MODEL EVALUATION / EXPLORATORY</p><h1>真实模型试跑</h1><p class="lede">XEM 国模组 · DeepSeek V4 · development split · 合成退款规则任务</p></div>
            <span class="model-draft-tag"><i></i>草案数据 · 非 Benchmark</span>
          </div>
          <div class="model-warning" role="note">
            <strong>结果只反映本批次的草案断言匹配</strong>
            <span>40 条数据尚未人工审批；本页显示 24 条 development 用例 × 2 个模型、每组 1 次的确定性检查。不能据此宣称模型准确率、排名或正式 Benchmark。网关未返回账单金额。</span>
          </div>
          <div class="metric-grid model-metrics">
            <div class="metric"><span>唯一模型 / 用例组合</span><strong>{{ modelEvaluation.evaluated_unique_pairs }} / {{ modelEvaluation.planned_unique_pairs }}</strong><small>24 个 development 用例 × 2 个模型</small></div>
            <div class="metric"><span>匹配当前断言</span><strong class="model-good">{{ modelEvaluation.deterministic_pass_pairs }}</strong><small>不是经人工确认的准确率</small></div>
            <div class="metric"><span>断言未匹配</span><strong class="model-bad">{{ modelEvaluation.deterministic_fail_pairs }}</strong><small>逐项 expected / actual 可复核</small></div>
            <div class="metric"><span>请求尝试</span><strong>{{ modelEvaluation.actual_request_attempts }}</strong><small>{{ modelEvaluation.gateway_error_attempts }} 次网络错误后补齐 · {{ modelEvaluation.duplicate_pair_attempts }} 次额外尝试</small></div>
          </div>
          <div class="section-heading model-section-heading"><div><p class="eyebrow">MODEL COMPARISON / ONE RUN PER CASE</p><h2>本批次观测</h2></div><span class="model-meta-chip">账单：未返回</span></div>
          <div class="model-cards">
            <article v-for="item in modelEvaluation.per_model_summary" :key="item.model" class="model-card">
              <div class="model-card-heading"><div><p class="eyebrow">{{ item.model === 'deepseek-v4-pro' ? 'DEEPSEEK V4' : 'DEEPSEEK V4.1' }}</p><h3>{{ item.model }}</h3></div><span class="model-pair-count">{{ item.evaluated_pairs }} / {{ item.planned_pairs }} 条</span></div>
              <div class="model-score-line"><strong>{{ item.passed_pairs }}</strong><span>条匹配断言</span><b>{{ item.failed_pairs }} 条未匹配</b></div>
              <div class="model-score-track" role="img" :aria-label="`${item.passed_pairs} passed assertion sets, ${item.failed_pairs} failed`"><i :style="{ width: (item.passed_pairs / Math.max(item.evaluated_pairs, 1) * 100) + '%' }"></i></div>
              <dl class="model-observations"><div><dt>Provider 报告 Token</dt><dd>{{ item.provider_reported_usage.total_tokens.toLocaleString() }}</dd></div><div><dt>响应延迟 p50 / p95</dt><dd>{{ item.latency_ms.p50_nearest_rank?.toLocaleString() ?? '—' }} / {{ item.latency_ms.p95_nearest_rank?.toLocaleString() ?? '—' }} ms</dd></div><div><dt>网络错误尝试</dt><dd>{{ item.gateway_error_attempts }} · 后续补齐</dd></div></dl>
            </article>
          </div>
          <section class="model-failures panel">
            <div class="subsection-heading"><div><p class="eyebrow">ASSERTION REVIEW</p><h2>未匹配的确定性断言</h2><p>保留失败项，便于核对规则、样本预期和模型输出字段。</p></div><span class="subtle-badge">{{ modelEvaluation.deterministic_fail_pairs }} 组</span></div>
            <div class="table-scroll"><table><thead><tr><th>用例</th><th>模型</th><th>断言字段</th><th>预期值</th><th>实际值</th></tr></thead><tbody>
              <template v-for="pair in modelEvaluation.pairs.filter(item => item.deterministic_status === 'failed')" :key="pair.case_id + pair.requested_model">
                <tr v-for="check in pair.failed_checks" :key="pair.case_id + pair.requested_model + check.check_id"><td><code class="mono">{{ pair.case_id }}</code></td><td>{{ pair.requested_model }}</td><td><code>{{ check.check_id }}</code></td><td><code>{{ JSON.stringify(check.expected) }}</code></td><td><code>{{ JSON.stringify(check.actual) }}</code></td></tr>
              </template>
            </tbody></table></div>
          </section>
          <details class="model-provenance">
            <summary>数据口径与原始运行记录</summary>
            <p>数据集 {{ modelEvaluation.dataset_id }} v{{ modelEvaluation.dataset_version }} · SHA-256 <code>{{ modelEvaluation.dataset_sha256 }}</code></p>
            <p>合并 {{ modelEvaluation.source_run_ids.length }} 份运行报告；共 {{ modelEvaluation.actual_request_attempts }} 次请求尝试、{{ modelEvaluation.evaluated_unique_pairs }} 个唯一组合。重试尝试单独保留，但结果按唯一用例 / 模型 / repetition 计数。</p>
            <p>人工复核：{{ modelEvaluation.human_review_status }} · 质量门禁：{{ modelEvaluation.quality_gate_status }} · Benchmark eligible：{{ modelEvaluation.benchmark_eligible }}</p>
            <div class="provenance-runs"><code v-for="runId in modelEvaluation.source_run_ids" :key="runId">{{ runId }}</code></div>
          </details>
        </section>

        <section v-else-if="page === 'dataset'" class="page">
          <div class="page-heading"><div><p class="eyebrow">DATASET / {{ dataset?.dataset_version }}</p><h1>合成测试集</h1><p class="lede">{{ dataset?.source }}</p></div><span class="subtle-badge">{{ dataset?.cases.length }} 条 · 固定版本</span></div>
          <div class="filters">
            <input v-model="query" aria-label="搜索测试用例" placeholder="搜索 ID、用例名称或标签">
            <select v-model="categoryFilter" aria-label="按类别筛选"><option v-for="name in categories" :key="name">{{ name }}</option></select>
            <select v-model="riskFilter" aria-label="按风险筛选"><option>全部</option><option>normal</option><option>medium</option><option>high</option><option>critical</option></select>
            <select v-model="caseFilter" aria-label="按最近运行结果筛选"><option>全部</option><option>passed</option><option>failed</option><option>error</option><option>未运行</option></select>
            <span>{{ filteredDefinitions.length }} / {{ dataset?.cases.length }}</span>
          </div>
          <div v-if="filteredDefinitions.length" class="table-scroll dataset-table">
            <table><thead><tr><th>ID</th><th>测试用例</th><th>类别</th><th>风险</th><th>预期行为</th><th>断言</th><th>最近状态</th></tr></thead>
              <tbody><tr v-for="item in filteredDefinitions" :key="item.case_id" :class="{ 'selected-row': selectedDefinition?.case_id === item.case_id }" tabindex="0" @click="selectedDefinition = item" @keydown.enter="selectedDefinition = item">
                <td><code class="mono">{{ item.case_id }}</code></td><td class="case-title"><strong>{{ item.title }}</strong><small>{{ item.description }}</small></td><td>{{ item.category }}</td><td><span class="risk-label">{{ item.risk_level }}</span></td><td class="expected-cell">{{ item.expected_behavior }}</td><td>{{ item.assertions.length }}</td><td><span :class="['state-dot-label', latestCaseResults.get(item.case_id)?.case_status || 'not-run']">{{ latestCaseResults.get(item.case_id)?.case_status ?? '未运行' }}</span></td>
              </tr></tbody>
            </table>
          </div>
          <div v-else class="empty-state compact"><strong>没有匹配的用例</strong><p>请调整搜索内容或筛选条件。</p><button class="button-link" @click="query = ''; categoryFilter = '全部'; riskFilter = '全部'; caseFilter = '全部'">清除筛选</button></div>
          <div v-if="selectedDefinition" class="definition-detail">
            <div class="subsection-heading"><div><p class="eyebrow">{{ selectedDefinition.case_id }} / {{ selectedDefinition.category }}</p><h2>{{ selectedDefinition.title }}</h2><p>{{ selectedDefinition.description }}</p></div><span class="risk-label">{{ selectedDefinition.risk_level }}</span></div>
            <div class="definition-columns"><div><h3>输入</h3><pre class="code-panel">{{ JSON.stringify(selectedDefinition.input, null, 2) }}</pre></div><div><h3>预期行为 / 可执行断言</h3><p class="expected-detail">{{ selectedDefinition.expected_behavior }}</p><pre class="code-panel">{{ JSON.stringify(selectedDefinition.assertions, null, 2) }}</pre><div class="tag-list"><span v-for="tag in selectedDefinition.tags" :key="tag" class="tag">{{ tag }}</span></div></div></div>
          </div>
        </section>

        <section v-else-if="page === 'run'" class="page">
          <div class="page-heading"><div><p class="eyebrow">RUN / CASE EXPLORER</p><h1>运行工作台</h1><p class="lede">执行状态与 Agent 行为评测分开记录，失败可通过固定输入复现。</p></div><span class="mode-chip"><i></i>{{ currentRun?.run_mode === 'real_model' ? '真实模型' : 'MOCK MODE' }}</span></div>
          <div class="launch-panel">
            <div class="launch-header"><div><p class="eyebrow">NEW EVALUATION</p><h2>选择执行 Provider</h2><p>每次请求由后端创建唯一 Run ID，并保存完整报告快照。</p></div><span class="state-pill pass">同步执行</span></div>
            <div class="launch-controls">
              <label class="field"><span>执行模式</span><select v-model="selectedProvider" aria-label="执行模式"><option value="mock">确定性 Mock（免费、本地）</option><option value="openai_compatible">OpenAI 兼容 Provider（外部调用）</option></select></label>
              <label class="field"><span>Prompt 版本</span><input v-model="promptVersion" aria-label="Prompt 版本"></label>
              <label class="field"><span>Agent 配置版本</span><input v-model="agentConfigVersion" aria-label="Agent 配置版本"></label>
              <button class="button-primary launch-button" :disabled="busy" @click="executeRun">{{ busy ? '服务端执行中…' : selectedProvider === 'mock' ? '运行完整测试集' : '运行真实模型评测' }}</button>
            </div>
            <div class="launch-note"><span class="status-indicator"></span><span v-if="selectedProvider === 'mock'">Mock 模式使用确定性本地 Agent 与工具桩，不产生模型调用。失败用例保留在结果中。</span><span v-else>真实模型模式需配置服务端密钥并由操作者确认；错误会与 Agent 断言失败分开标记，首个 Provider 错误后停止其余用例。</span><small>Prompt / Agent 版本输入目前作为运行元数据标签，不会自动选择不同的 Prompt 内容或 Agent 配置。</small></div>
          </div>

          <div v-if="!currentRun" class="empty-state compact"><strong>暂无运行记录</strong><p>先执行一次测试集，结果和 Trace 会显示在这里。</p></div>
          <template v-else>
            <div class="run-heading"><div><p class="eyebrow">SAVED RUN / {{ currentRun.dataset_version }}</p><h2>用例结果</h2><code class="mono">RUN ID　{{ currentRun.run_id }}</code></div><div class="run-actions"><span class="state-pill" :class="currentRun.quality_gate === 'passed' ? 'pass' : 'fail'">QUALITY GATE {{ currentRun.quality_gate }}</span><button class="button-outline" @click="exportReport(currentRun)">下载 JSON</button></div></div>
            <div class="run-summary"><div><strong>{{ currentRun.summary.executed_cases }}</strong><span>已执行</span></div><div><strong class="good-text">{{ currentRun.summary.passed }}</strong><span>Agent 通过</span></div><div><strong class="bad-text">{{ currentRun.summary.failed }}</strong><span>断言失败</span></div><div><strong>{{ currentRun.summary.errors }}</strong><span>执行错误</span></div><div><strong>{{ currentRun.summary.skipped }}</strong><span>跳过</span></div><div><strong>{{ Number(currentRun.summary.pass_rate).toFixed(2) }}%</strong><span>通过率</span></div><div class="run-summary-note">执行器成功 ≠ Agent 行为通过</div></div>
            <div class="case-explorer">
              <div class="case-list"><div class="case-list-head"><span>用例 / 类别</span><span>执行</span><span>评测</span></div>
                <button v-for="result in currentRun.cases" :key="result.case_id" :aria-label="result.case_id + ' ' + result.title" :class="['case-row', { selected: selectedCase?.case_id === result.case_id }]" @click="selectedCaseId = result.case_id">
                  <span><strong>{{ result.title }}</strong><small>{{ result.case_id }} · {{ result.category }}</small></span><span :class="result.execution_status === 'success' ? 'good-text' : result.execution_status === 'error' ? 'bad-text' : 'muted-text'">{{ result.execution_status }}</span><span :class="result.case_status === 'passed' ? 'good-text' : result.case_status === 'failed' || result.case_status === 'error' ? 'bad-text' : 'muted-text'">{{ result.case_status }}</span>
                </button>
              </div>
              <article v-if="selectedCase" class="case-detail">
                <div class="detail-heading"><div><p class="eyebrow">{{ selectedCase.case_id }} / {{ selectedCase.category }}</p><h3>{{ selectedCase.title }}</h3><span class="detail-meta">{{ currentRun.provider_type }} · {{ currentRun.model_name }} · {{ selectedCase.duration_ms }} ms</span></div><span :class="['state-pill', selectedCase.case_status === 'passed' ? 'pass' : selectedCase.case_status === 'failed' || selectedCase.case_status === 'error' ? 'fail' : 'neutral']">{{ selectedCase.case_status }}</span></div>
                <div class="detail-content">
                  <section><h4>预期行为</h4><p>{{ selectedCase.expected_behavior }}</p></section>
                  <section v-if="selectedCase.failure_reason?.length" class="failure-box"><h4>失败归因</h4><ul><li v-for="reason in selectedCase.failure_reason" :key="reason">{{ reason }}</li></ul><small>复现：使用测试集 v{{ currentRun.dataset_version }} 和用例 {{ selectedCase.case_id }} 重新执行。</small></section>
                  <section><h4>输入</h4><pre class="code-panel">{{ JSON.stringify(selectedCase.input, null, 2) }}</pre></section>
                  <section><h4>实际输出</h4><pre class="code-panel">{{ JSON.stringify(selectedCase.actual_output, null, 2) }}</pre><small v-if="currentRun.run_mode === 'real_model'" class="content-note">展示 Provider 返回的结构化结果；不包含隐藏推理内容。</small></section>
                  <section><h4>断言结果 <span class="muted-text">{{ (selectedCase.assertion_results ?? selectedCase.assertions).filter(item => item.passed).length }} / {{ (selectedCase.assertion_results ?? selectedCase.assertions).length }} 通过</span></h4>
                    <div v-for="(assertion, index) in (selectedCase.assertion_results ?? selectedCase.assertions)" :key="assertion.path + assertion.operator + index" class="assertion-row"><b :class="assertion.passed ? 'good-text' : 'bad-text'">{{ assertion.passed ? '✓' : '×' }}</b><code>{{ assertion.path }} {{ assertion.operator }} {{ JSON.stringify(assertion.expected) }}</code><small>{{ assertion.description }} · 实际：{{ JSON.stringify(assertion.actual) }}</small></div>
                  </section>
                  <section><h4>Trace 时间线 <span class="muted-text">{{ selectedCase.execution_steps.length }} 步</span></h4>
                    <div class="trace-panel">
                      <div v-for="event in selectedCase.execution_steps" :key="event.event_id" class="trace-event">
                        <span :class="['trace-node', event.status === 'error' || event.status === 'failed' || event.status === 'denied' ? 'error-node' : '']"></span><div class="trace-body"><div class="trace-title"><strong>{{ String(event.sequence + 1).padStart(2, '0') }}　{{ event.event_type }}</strong><span :class="event.status === 'error' || event.status === 'failed' || event.status === 'denied' ? 'trace-error' : ''">{{ event.duration_ms }} ms · {{ event.status }}</span></div><small>{{ event.timestamp }}</small><p v-if="event.input_summary">输入：{{ event.input_summary }}</p><pre v-if="event.output_summary" class="trace-output">{{ JSON.stringify(event.output_summary, null, 2) }}</pre><em v-if="event.error">{{ event.error }}</em></div>
                      </div>
                    </div>
                  </section>
                </div>
              </article>
              <div v-else class="empty-state compact">此运行没有用例结果。</div>
            </div>
          </template>
        </section>

        <section v-else-if="page === 'history'" class="page">
          <div class="page-heading"><div><p class="eyebrow">RUNS / PERSISTED HISTORY</p><h1>运行历史</h1><p class="lede">从后端持久化快照读取，可刷新后重新打开任意运行。</p></div><span class="subtle-badge">{{ runs.length }} 条记录</span></div>
          <div v-if="runs.length" class="table-scroll"><table><thead><tr><th>运行 ID</th><th>运行时间</th><th>Provider / 模型</th><th>Prompt / Agent 配置</th><th>测试集</th><th>通过 / 失败 / 错误</th><th>模式</th><th></th></tr></thead><tbody>
            <tr v-for="run in runs" :key="run.run_id"><td><code class="mono">{{ run.run_id.slice(0, 18) }}…</code><small>{{ run.completed_at }}</small></td><td>{{ run.duration_ms }} ms</td><td>{{ run.provider_type }}<small>{{ run.model_name }}</small></td><td>{{ run.prompt_version }}<small>{{ run.agent_config_version }}</small></td><td>v{{ run.dataset_version }}</td><td><span class="good-text">{{ run.summary.passed }}</span> / <span class="bad-text">{{ run.summary.failed }}</span> / {{ run.summary.errors }}</td><td>{{ run.run_mode }}</td><td><button class="button-link" @click="selectRun(run.run_id)">打开 →</button></td></tr>
          </tbody></table></div>
          <div v-else class="empty-state"><strong>运行历史为空</strong><p>评测完成后，结果会写入本地持久化存储。</p><button class="button-primary" @click="setPage('run')">打开运行工作台</button></div>
        </section>

        <section v-else-if="page === 'compare'" class="page">
          <div class="page-heading"><div><p class="eyebrow">COMPARISON / SAVED RUNS</p><h1>回归比较</h1><p class="lede">比较两个实际存在的已保存运行；报告由后端按用例和配置元数据生成。</p></div><span class="subtle-badge">{{ sameDatasetRuns.length }} 条当前数据集运行</span></div>
          <div class="compare-controls">
            <label class="field"><span>基线运行</span><select v-model="baselineRunId" aria-label="基线运行"><option value="">选择基线</option><option v-for="run in runs" :key="run.run_id" :value="run.run_id">{{ run.run_id.slice(0, 12) }} · {{ run.provider_type }} · v{{ run.dataset_version }}</option></select></label>
            <label class="field"><span>候选运行</span><select v-model="candidateRunId" aria-label="候选运行"><option value="">选择候选</option><option v-for="run in runs" :key="run.run_id" :value="run.run_id">{{ run.run_id.slice(0, 12) }} · {{ run.provider_type }} · v{{ run.dataset_version }}</option></select></label>
            <button class="button-primary" :disabled="!canCompare" @click="compareSavedRuns">比较已保存运行</button>
          </div>
          <div v-if="!runs.length" class="empty-state compact"><strong>需要至少两个运行</strong><p>先从运行工作台执行测试集，比较结果才会有数据来源。</p></div>
          <div v-else-if="runs.length < 2" class="empty-state compact"><strong>还需要第二条运行记录</strong><p>保存两个独立运行后，可以比较逐用例变化。</p><button class="button-link" @click="setPage('run')">运行测试集 →</button></div>
          <div v-else-if="comparison" class="comparison-result">
            <div class="comparison-banner"><div><span class="eyebrow">COMPARISON SEMANTICS</span><h2>{{ semanticsLabel(comparison.comparison_semantics) }}</h2><p>基线 {{ String(comparison.baseline.run_id) }}　→　候选 {{ String(comparison.candidate.run_id) }}</p></div><span :class="['mode-chip', comparison.configuration_changed ? 'regression-chip' : '']">{{ comparison.configuration_changed ? 'Provider 配置差异' : '运行比较' }}</span></div>
            <div class="notice" :class="comparison.configuration_changed ? 'notice-info' : 'notice-neutral'">{{ comparison.configuration_changed ? '两条运行的 Provider / 模型或运行时设置指纹不同。结果是配置比较，不代表已切换 Prompt 内容 V1/V2；指标只描述这两条保存运行。' : comparison.comparison_semantics === 'mock_metadata_only_comparison' ? '两次运行都来自 Mock Agent。Prompt / Agent 标签变化只作为元数据比较，不代表真实模型版本回归。' : comparison.comparison_semantics === 'configuration_label_only_comparison' ? 'Prompt / Agent 配置标签发生变化，但运行时配置指纹一致；当前系统不会按这些标签动态加载不同 Prompt 内容。' : '当前结果仅是保存运行之间的比较；运行时配置未变化时，不称为配置版本回归。' }}</div>
            <div class="metric-grid comparison-metrics"><div class="metric"><span>通过率变化</span><strong>{{ comparison.summary.pass_rate_delta > 0 ? '+' : '' }}{{ comparison.summary.pass_rate_delta }} <em>pp</em></strong><small>{{ comparison.baseline_summary.pass_rate }}% → {{ comparison.candidate_summary.pass_rate }}%</small></div><div class="metric"><span>新增失败</span><strong class="bad-text">{{ comparison.summary.new_failures }}</strong><small>基线通过 → 候选未通过</small></div><div class="metric"><span>修复成功</span><strong class="good-text">{{ comparison.summary.fixed_successes }}</strong><small>基线未通过 → 候选通过</small></div><div class="metric"><span>持续失败</span><strong>{{ comparison.summary.persistent_failures }}</strong><small>两次运行均未通过</small></div></div>
            <div class="comparison-meta"><span>新增用例 {{ comparison.summary.added_cases }}</span><span>缺失用例 {{ comparison.summary.missing_cases }}</span><span>配置元数据 {{ comparison.configuration_metadata_complete ? '完整' : '不完整' }}</span><span>运行时配置变化 {{ comparison.configuration_changed ? '是' : '否' }}</span><span>版本标签变化 {{ comparison.configuration_labels_changed ? '是' : '否' }}</span><span>运行指纹变化 {{ comparison.run_fingerprint_changed ? '是' : '否' }}</span></div>
            <div v-if="comparison.metadata_changes.length" class="metadata-changes"><strong>运行元数据差异</strong><span v-for="change in comparison.metadata_changes" :key="change.field"><code>{{ change.field }}</code>{{ change.baseline }} → {{ change.candidate }}</span></div>
            <div class="table-scroll"><table><thead><tr><th>用例 / 类别</th><th>基线</th><th>候选</th><th>变化</th><th>失败原因变化</th></tr></thead><tbody><tr v-for="change in comparison.case_changes" :key="change.case_id"><td><strong>{{ change.title }}</strong><small class="mono">{{ change.case_id }} · {{ change.category ?? '类别未知' }}</small></td><td>{{ change.baseline_status }}</td><td>{{ change.candidate_status }}</td><td><span :class="['change-label', change.change === 'new_failure' || change.change === 'missing_case' ? 'change-bad' : change.change === 'fixed_success' ? 'change-good' : '']">{{ change.change }}</span></td><td><small>{{ (change.candidate_failure_reason ?? change.baseline_failure_reason ?? []).join('；') || '—' }}</small></td></tr></tbody></table></div>
            <div class="category-comparison"><div><p class="eyebrow">CATEGORY MOVEMENT</p><h2>类别变化</h2></div><div class="table-scroll"><table><thead><tr><th>类别</th><th>基线通过 / 失败</th><th>候选通过 / 失败</th><th>失败变化</th><th>用例数变化</th></tr></thead><tbody><tr v-for="item in comparison.category_changes" :key="item.category"><td>{{ item.category }}</td><td>{{ item.baseline.passed }} / {{ item.baseline.failed }}</td><td>{{ item.candidate.passed }} / {{ item.candidate.failed }}</td><td :class="item.failed_delta > 0 ? 'bad-text' : item.failed_delta < 0 ? 'good-text' : ''">{{ item.failed_delta > 0 ? '+' : '' }}{{ item.failed_delta }}</td><td>{{ item.case_count_delta > 0 ? '+' : '' }}{{ item.case_count_delta }}</td></tr></tbody></table></div></div>
          </div>
          <div v-else-if="runs.length >= 2" class="empty-state compact"><strong>请选择两条不同运行并执行比较</strong><p>比较结果通过后端 API 读取两条保存快照，不会用前端临时数据代替。</p></div>
        </section>

        <section v-else class="page">
          <div class="page-heading"><div><p class="eyebrow">REPORTS / JSON SNAPSHOTS</p><h1>报告中心</h1><p class="lede">下载后端保存的原始运行 JSON，报告包含摘要、用例、断言、Trace 和错误分类。</p></div><span class="subtle-badge">JSON · schema 1.1</span></div>
          <div v-if="runs.length" class="table-scroll"><table><thead><tr><th>运行 ID</th><th>生成时间</th><th>Provider 模式</th><th>数据集</th><th>用例</th><th>通过 / 失败 / 错误 / 跳过</th><th>质量门禁</th><th></th></tr></thead><tbody>
            <tr v-for="run in runs" :key="run.run_id"><td><code class="mono">{{ run.run_id }}</code></td><td>{{ run.report_generated_at }}</td><td>{{ run.run_mode === 'real_model' ? '真实模型' : 'Mock' }}<small>{{ run.model_name }}</small></td><td>v{{ run.dataset_version }}</td><td>{{ run.summary.total_cases }}</td><td>{{ run.summary.passed }} / {{ run.summary.failed }} / {{ run.summary.errors }} / {{ run.summary.skipped }}</td><td><span :class="['state-pill', run.quality_gate === 'passed' ? 'pass' : 'fail']">{{ run.quality_gate }}</span></td><td><button class="button-link" @click="exportReport(run)">下载 JSON ↓</button></td></tr>
          </tbody></table></div>
          <div v-else class="empty-state"><strong>当前没有可导出的 JSON 报告</strong><p>执行一次评测后，报告中心会从持久化运行记录读取报告。</p><button class="button-primary" @click="setPage('run')">打开运行工作台</button></div>
        </section>

        <footer class="footer"><span>PromptOps Studio · Local Agent Evaluation</span><span>{{ dataset?.cases.length ?? 0 }} synthetic cases · Mock results are not model benchmarks</span></footer>
      </template>
    </main>
  </div>
</template>

<style scoped>
:global(*){box-sizing:border-box}
.console{min-height:100vh;background:#f3f6f4;color:#1b2a27;display:flex;font:14px/1.5 Inter,"Segoe UI","Microsoft YaHei",sans-serif}
.rail{width:232px;flex:0 0 232px;background:#edf2ef;border-right:1px solid #dce5df;padding:25px 15px 18px;display:flex;flex-direction:column}
.brand{height:76px;display:flex;align-items:flex-start;gap:10px;padding:2px 11px;color:#164f48}
.brand-mark{font-size:23px;color:#16816c;line-height:1}
.brand strong{display:block;font-size:17px;letter-spacing:-.035em}
.brand small{display:block;margin-top:4px;color:#899690;font-size:9px;font-weight:700;letter-spacing:.16em}
.rail nav{display:grid;gap:4px;margin-top:13px}
.rail nav button{min-width:0;height:41px;padding:0 11px;display:flex;align-items:center;gap:10px;border:0;border-radius:5px;background:transparent;color:#64746e;font:inherit;text-align:left;cursor:pointer}
.rail nav button:hover{background:#e4ece7;color:#285b4d}
.rail nav button[aria-current=page]{background:#dceae3;color:#176d59;font-weight:650}
.nav-icon{width:19px;text-align:center;color:#64867a;font-size:16px}
.nav-count{margin-left:auto;min-width:22px;padding:1px 5px;border-radius:10px;background:#f8faf9;color:#718078;font-size:10px;text-align:center}
.rail-foot{margin-top:auto;padding:14px 10px;border-top:1px solid #dbe3de;color:#447562;font-size:9px;font-weight:700;letter-spacing:.11em}
.rail-foot small{display:block;margin:8px 0 0 16px;color:#87948d;font-size:10px;font-weight:400;letter-spacing:0}
.status-indicator{display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#20a276;vertical-align:1px}
.main{width:calc(100% - 232px);max-width:1600px;min-width:0;margin:0 auto;padding:0 40px 24px}
.topbar{height:62px;border-bottom:1px solid #dce4df;display:flex;align-items:center;justify-content:space-between}
.breadcrumbs{display:flex;align-items:center;gap:9px;color:#9aa49f;font-size:9px;font-weight:650;letter-spacing:.13em}
.breadcrumbs b{color:#c5ceca}.breadcrumbs strong{color:#52645b}
.refresh-button{height:33px;padding:0 9px;display:flex;align-items:center;gap:6px;border:1px solid #d8e1dc;border-radius:4px;background:#fff;color:#63746c;cursor:pointer;font:inherit;font-size:11px}
.refresh-button:disabled{opacity:.55}.refresh-button span:first-child{font-size:17px}
.page{padding:31px 0 20px}
.page-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;margin-bottom:27px}
.eyebrow{margin:0;color:#558173;font-size:9px;font-weight:750;letter-spacing:.16em}
.page-heading h1{margin:7px 0 7px;font-size:29px;line-height:1.2;letter-spacing:-.04em;font-weight:650}
.lede{margin:0;color:#77857e;font-size:12px}
.mode-chip,.subtle-badge{display:inline-flex;align-items:center;gap:7px;padding:7px 10px;border:1px solid #d3e3db;border-radius:4px;background:#edf5f0;color:#3e7864;font-size:10px;font-weight:650;white-space:nowrap}
.mode-chip i{width:6px;height:6px;border-radius:50%;background:#219576}
.metric-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));margin:0 0 34px;border-top:1px solid #dce5df;border-bottom:1px solid #dce5df;background:#fff}
.metric{min-height:108px;padding:17px 20px;border-right:1px solid #e8eeea;display:flex;flex-direction:column;gap:5px}
.metric:last-child{border-right:0}.metric>span{color:#7d8a83;font-size:10px}.metric strong{font-size:26px;line-height:1.2;letter-spacing:-.04em;font-weight:650;color:#20342d}.metric strong em{font-size:16px;font-style:normal;color:#9ca8a2}.metric small{color:#98a29d;font-size:10px}
.section-heading,.subsection-heading,.run-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}
.section-heading{margin-bottom:8px}.section-heading h2,.subsection-heading h2,.category-comparison h2{margin:4px 0 0;font-size:17px;letter-spacing:-.02em;font-weight:650}
.button-primary{min-height:38px;padding:0 15px;border:1px solid #176d59;border-radius:4px;background:#176d59;color:#fff;font:inherit;font-size:11px;font-weight:650;cursor:pointer;white-space:nowrap;transition:background .15s ease,border-color .15s ease}
.button-primary:hover{background:#105847;border-color:#105847}.button-primary:disabled{opacity:.55;cursor:wait}
.helper-line{margin:0 0 16px;color:#87948d;font-size:10px}
.table-scroll{overflow:auto;border-top:1px solid #dbe4df;border-bottom:1px solid #dbe4df;background:#fff}
table{width:100%;border-collapse:collapse;text-align:left;white-space:nowrap}
thead{background:#f7f9f8}th{color:#85928b;font-size:9px;font-weight:700;letter-spacing:.07em;text-transform:uppercase}
th,td{padding:12px 13px;border-bottom:1px solid #e9eeeb}
tbody tr:last-child td{border-bottom:0}td{color:#40534a;font-size:11px}td small{display:block;margin-top:4px;color:#96a19a;font-size:9px}
.mono{color:#4c6d5d;font:10px ui-monospace,SFMono-Regular,Consolas,monospace}
.mode-label{display:inline-block;padding:3px 6px;font-size:9px;letter-spacing:.07em}.mode-label.mock{background:#edf5f0;color:#39745f}.mode-label.real{background:#eaf0fb;color:#526c9e}
.state-pill{display:inline-block;padding:4px 7px;border-radius:3px;font-size:9px;font-weight:700;letter-spacing:.03em;white-space:nowrap}
.state-pill.pass{background:#eaf4ed;color:#397857}.state-pill.fail{background:#fff0ee;color:#ad5047}.state-pill.neutral{background:#f0f2f0;color:#7d8982}
.good-text{color:#278060!important}.bad-text{color:#b45149!important}.muted-text{color:#929e97!important}
.button-link{padding:0;border:0;background:transparent;color:#257662;font:inherit;font-size:10px;cursor:pointer}.button-link:hover{text-decoration:underline}
.empty-state{padding:42px 20px;text-align:center;border:1px dashed #cbd8d0;color:#728179}
.empty-state strong{display:block;color:#3b5147;font-size:14px}.empty-state p{margin:6px 0 0;color:#8b9790;font-size:11px}.empty-state .button-primary{margin-top:15px}
.empty-state.compact{padding:25px 18px}.empty-icon{margin:0 auto 10px;width:34px;height:34px;border-radius:50%;display:grid;place-items:center;background:#e7f1eb;color:#37816b;font-size:20px}
.overview-lower{display:grid;grid-template-columns:1.05fr .95fr;gap:32px;margin-top:30px}
.failure-summary,.provider-summary{padding:19px 0;border-top:1px solid #dce5df;border-bottom:1px solid #dce5df}
.subsection-heading{margin-bottom:17px}.subsection-heading>div>p:not(.eyebrow){margin:5px 0 0;color:#87938d;font-size:11px}
.subtle-badge{padding:5px 8px;background:#fff;border-color:#dce5df;color:#718077;font-size:9px}
.category-bars{display:grid;gap:10px}.category-row{display:grid;grid-template-columns:minmax(100px,1.1fr) minmax(80px,2fr) 22px;align-items:center;gap:10px;font-size:10px;color:#6c7b73}.category-row b{text-align:right;color:#596b61;font-size:10px}
.bar-track{height:5px;background:#e9efeb;border-radius:5px;overflow:hidden}.bar-track i{display:block;height:100%;min-width:2px;background:#60a68d;border-radius:5px}
.chart-caption{margin:13px 0 0;color:#9aa49e;font-size:9px}
.provider-row{min-height:53px;padding:7px 0;display:flex;align-items:center;gap:10px;border-top:1px solid #edf0ee}
.provider-symbol{width:25px;height:25px;display:grid;place-items:center;border-radius:4px;background:#e7f3ec;color:#3e816b;font-size:10px;font-weight:700}.provider-symbol.external{background:#edf0f8;color:#6174a2}
.provider-row>div{min-width:0;flex:1}.provider-row strong{display:block;color:#40554a;font-size:10px}.provider-row small{display:block;color:#909b95;font-size:9px}
.online{color:#398565;font-size:9px;font-weight:700;letter-spacing:.05em}
.provider-summary>.button-link{margin-top:12px}
.filters{display:flex;align-items:center;gap:8px;margin:18px 0 13px}.filters input,.filters select,.field input,.field select{height:36px;padding:0 10px;border:1px solid #d7e0db;border-radius:4px;background:#fff;color:#3d5147;font:inherit;font-size:10px}
.filters input{min-width:200px;flex:1}.filters select{max-width:190px}.filters>span{color:#849189;font-size:10px}
.dataset-table{max-height:62vh}.dataset-table td{white-space:normal}.dataset-table .case-title{min-width:165px}.case-title strong{display:block;color:#354b40;font-size:10px}.case-title small{max-width:230px;line-height:1.4}.expected-cell{min-width:265px;max-width:420px;line-height:1.45;color:#63736b}
.risk-label{color:#7e6750;font-size:9px}.state-dot-label{font-size:9px}.state-dot-label.passed{color:#278060}.state-dot-label.failed,.state-dot-label.error{color:#b45149}.state-dot-label.not-run{color:#909b95}
.selected-row{background:#eef6f1;outline:1px solid #d8e8df;cursor:pointer}
.definition-detail{margin-top:24px;padding:19px 0;border-top:1px solid #dce5df}.definition-detail .subsection-heading{align-items:flex-start}.definition-detail h2{margin:5px 0;font-size:17px}.definition-detail .subsection-heading p:not(.eyebrow){margin:5px 0;color:#819087;font-size:11px}
.definition-columns{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-top:18px}.definition-columns h3,.detail-content h4{margin:0 0 7px;color:#64766c;font-size:10px;font-weight:700}
.expected-detail{margin:0 0 10px;color:#475c50;font-size:11px;line-height:1.55}
.tag-list{display:flex;flex-wrap:wrap;gap:5px}.tag{padding:3px 6px;background:#edf2ef;color:#687a70;font-size:9px}
.code-panel{max-height:250px;overflow:auto;margin:0;padding:13px 14px;border:1px solid #263b33;border-radius:4px;background:#172620;color:#cce1d5;white-space:pre-wrap;overflow-wrap:anywhere;font:10px/1.55 ui-monospace,SFMono-Regular,Consolas,monospace}
.launch-panel{margin-bottom:22px;padding:19px 21px;border:1px solid #dce5df;background:#fff}
.launch-header{display:flex;align-items:flex-start;justify-content:space-between;gap:15px}.launch-header h2{margin:4px 0;font-size:16px}.launch-header p:last-child{margin:5px 0 0;color:#849189;font-size:10px}
.launch-controls{display:grid;grid-template-columns:1.25fr 1fr 1fr auto;align-items:end;gap:10px;margin-top:17px}
.field{display:grid;gap:5px;color:#78867e;font-size:9px;font-weight:650}.field input,.field select{width:100%;height:37px}
.launch-button{height:37px}
.launch-note{margin-top:13px;padding-top:11px;border-top:1px solid #edf0ee;color:#738179;font-size:10px}.launch-note small{display:block;margin:5px 0 0 15px;color:#9aa49e;font-size:9px}
.run-heading{margin:20px 0 10px;align-items:flex-end}.run-heading h2{margin:4px 0;font-size:17px}.run-actions{display:flex;align-items:center;gap:8px}
.button-outline{height:30px;padding:0 9px;border:1px solid #cbd9d1;border-radius:4px;background:#fff;color:#367762;font:inherit;font-size:10px;cursor:pointer}
.run-summary{display:flex;align-items:center;gap:22px;flex-wrap:wrap;padding:12px 0;border-top:1px solid #dce5df;border-bottom:1px solid #dce5df;color:#77857d}
.run-summary>div:not(.run-summary-note){display:grid;gap:2px}.run-summary strong{color:#33483e;font-size:15px}.run-summary span{font-size:9px}
.run-summary-note{margin-left:auto;color:#9aa49e;font-size:9px}
.case-explorer{display:grid;grid-template-columns:minmax(260px,33%) minmax(0,1fr);margin-top:16px;border:1px solid #dce4df;background:#fff;min-height:520px}
.case-list{max-height:75vh;overflow:auto;border-right:1px solid #e5ebe7}.case-list-head,.case-row{display:grid;grid-template-columns:minmax(0,1fr) 59px 57px;align-items:center;gap:7px}
.case-list-head{position:sticky;top:0;z-index:1;padding:10px 12px;background:#f6f8f7;border-bottom:1px solid #e5ebe7;color:#829087;font-size:9px}
.case-row{width:100%;padding:10px 11px;border:0;border-bottom:1px solid #edf0ee;background:#fff;text-align:left;color:#7b8981;font:inherit;font-size:9px;cursor:pointer}
.case-row:hover{background:#f7faf8}.case-row.selected{background:#edf5f1;box-shadow:inset 2px 0 #21816b}
.case-row strong{display:block;color:#40544a;font-size:10px;font-weight:600}.case-row small{display:block;margin-top:3px;color:#97a19b;font:9px ui-monospace,Consolas,monospace}
.case-detail{min-width:0}.detail-heading{padding:16px 18px;border-bottom:1px solid #e7ede9;display:flex;justify-content:space-between;align-items:flex-start;gap:12px}
.detail-heading h3{margin:4px 0;font-size:15px}.detail-meta{color:#929d96;font-size:9px}
.detail-content{padding:16px 18px}.detail-content section{margin-bottom:17px}.detail-content h4{display:flex;justify-content:space-between;align-items:center}.detail-content p{margin:0;color:#4f6257;font-size:10px;line-height:1.55}
.failure-box{padding:11px 12px;border-left:3px solid #c15c52;background:#fff3f1;color:#91473f}.failure-box ul{margin:3px 0;padding-left:17px;font-size:10px;line-height:1.5}.failure-box small{color:#a47c76;font-size:9px}.content-note{display:block;margin-top:5px;color:#89968f;font-size:9px}
.assertion-row{display:grid;grid-template-columns:15px 1fr;gap:3px 7px;padding:8px 0;border-bottom:1px solid #edf0ee}.assertion-row code{color:#40574a;font:9px/1.4 ui-monospace,SFMono-Regular,Consolas,monospace;overflow-wrap:anywhere}.assertion-row small{grid-column:2;color:#89958e;font-size:9px}
.trace-panel{padding:12px 14px;border-radius:4px;background:#15251f;color:#cfdfd6}
.trace-event{position:relative;display:grid;grid-template-columns:9px minmax(0,1fr);gap:9px;padding:7px 0 8px;border-left:1px solid #405c4e}
.trace-node{width:7px;height:7px;margin:3px 0 0 -4px;border:1px solid #92baa6;border-radius:50%;background:#458d70}
.trace-node.error-node{border-color:#ef988d;background:#c46055}
.trace-body{min-width:0}.trace-title{display:flex;justify-content:space-between;gap:9px}.trace-title strong{color:#c9e0d2;font:10px ui-monospace,Consolas,monospace}.trace-title span,.trace-body>small{color:#849b8d;font-size:8px}
.trace-body>small{display:block;margin-top:3px}.trace-body p{margin:5px 0;color:#adc0b5;font-size:9px;overflow-wrap:anywhere}
.trace-output{max-height:160px;overflow:auto;margin:6px 0;padding:9px;background:#1d3028;border:1px solid #2a4137;border-radius:3px;color:#bed7c8;white-space:pre-wrap;overflow-wrap:anywhere;font:9px/1.5 ui-monospace,Consolas,monospace}
.trace-body em{display:block;margin-top:5px;color:#f0a097;font-size:9px;font-style:normal}
.compare-controls{display:grid;grid-template-columns:1fr 1fr auto;align-items:end;gap:12px;margin:15px 0 18px;padding:15px;background:#fff;border:1px solid #dce5df}
.compare-controls .field select{width:100%}
.comparison-banner{display:flex;align-items:flex-start;justify-content:space-between;gap:14px;padding:18px 0;border-top:1px solid #dce5df;border-bottom:1px solid #dce5df}
.comparison-banner h2{margin:5px 0;font-size:17px}.comparison-banner p{margin:0;color:#89958e;font:9px ui-monospace,Consolas,monospace}
.regression-chip{border-color:#d9e0f0;background:#f0f3fb;color:#5c6f9c}
.notice{margin:12px 0;padding:10px 12px;border-left:3px solid #a1b1a8;font-size:10px;line-height:1.5}
.notice-info{border-color:#6484bd;background:#eff3fb;color:#52688f}.notice-neutral{background:#f5f7f5;color:#728078}
.comparison-metrics{margin:14px 0 8px}.comparison-metrics .metric{min-height:91px;padding:13px 16px}.comparison-metrics .metric strong{font-size:22px}
.comparison-meta{display:flex;flex-wrap:wrap;gap:8px 18px;padding:11px 0;color:#78867e;font-size:9px;border-bottom:1px solid #e1e8e3}
.metadata-changes{display:flex;flex-wrap:wrap;align-items:center;gap:7px 15px;padding:11px 0;border-bottom:1px solid #e1e8e3;font-size:9px;color:#68786f}.metadata-changes>span{display:flex;gap:6px}.metadata-changes code{color:#46745e}
.change-label{font-size:9px;color:#77857d}.change-label.change-bad{color:#b45149}.change-label.change-good{color:#278060}
.category-comparison{margin-top:23px}.category-comparison h2{margin-bottom:10px}
.footer{display:flex;justify-content:space-between;gap:12px;margin-top:26px;padding-top:13px;border-top:1px solid #dce4df;color:#7e8b84;font-size:9px}.footer span:last-child{color:#9aa49f;text-align:right}
.alert{display:flex;align-items:center;gap:10px;margin-top:14px;padding:10px 12px;border-left:3px solid #bd5b51;background:#fff1ef;color:#8e3d36}
.alert-symbol{width:19px;height:19px;display:grid;place-items:center;border-radius:50%;background:#f4d6d1;font-size:11px;font-weight:700}
.alert div{min-width:0;flex:1}.alert strong{font-size:10px}.alert p{margin:2px 0 0;font-size:10px;overflow-wrap:anywhere}.alert button{border:0;background:transparent;color:#87453e;font:inherit;font-size:9px;text-decoration:underline;cursor:pointer}
.loading-state{padding:65px 15px;text-align:center;color:#728279;font-size:11px}.spinner{display:inline-block;width:13px;height:13px;margin-right:6px;border:2px solid #d8e5dd;border-top-color:#31856b;border-radius:50%;vertical-align:-3px;animation:spin .8s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:1100px){.main{padding-right:25px;padding-left:25px}.rail{width:205px;flex-basis:205px}.main{width:calc(100% - 205px)}.launch-controls{grid-template-columns:1fr 1fr}.launch-button{grid-column:2}.metric{padding-right:14px;padding-left:14px}.case-explorer{grid-template-columns:minmax(230px,35%) minmax(0,1fr)}}
@media(max-width:760px){.overview-lower{grid-template-columns:1fr;gap:18px}.metric-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.metric:nth-child(2){border-right:0}.metric:nth-child(-n+2){border-bottom:1px solid #e8eeea}.case-explorer{grid-template-columns:1fr}.case-list{max-height:280px;border-right:0;border-bottom:1px solid #e5ebe7}.run-summary{gap:12px}.run-summary-note{margin-left:0}.definition-columns{gap:13px}}
@media(max-width:560px){
  .console{display:block}.rail{position:sticky;top:0;z-index:5;width:auto;padding:8px 10px 9px;border-right:0;border-bottom:1px solid #d6e0da}.brand{height:31px;align-items:center;padding:0 5px;gap:7px}.brand-mark{font-size:18px}.brand strong{font-size:14px}.brand small{margin-top:1px;font-size:7px}
  .rail nav{grid-template-columns:repeat(3,minmax(0,1fr));gap:3px;margin-top:6px}.rail nav button{height:31px;justify-content:center;gap:5px;padding:0 3px;font-size:9px}.nav-icon{width:auto;font-size:12px}.nav-count{display:none}.rail-foot{display:none}
  .main{width:100%;padding:0 13px 18px}.topbar{height:45px}.breadcrumbs{gap:5px;font-size:7px;letter-spacing:.07em}.refresh-button{height:28px;padding:0 6px}.refresh-label{display:none}
  .page{padding-top:20px}.page-heading{display:block;margin-bottom:19px}.page-heading h1{font-size:24px}.page-heading>.mode-chip,.page-heading>.subtle-badge{margin-top:10px}
  .metric-grid{margin-bottom:24px}.metric{min-height:90px;padding:12px}.metric>span{font-size:9px}.metric strong{font-size:21px}.metric small{font-size:9px}
  .section-heading{align-items:flex-start}.section-heading h2{font-size:15px}.section-heading .button-primary{max-width:155px;white-space:normal;line-height:1.3;padding:7px 9px;font-size:9px}
  .overview-lower{gap:13px}.category-row{grid-template-columns:minmax(85px,1fr) minmax(35px,1.3fr) 17px;gap:7px;font-size:9px}
  .filters{flex-wrap:wrap}.filters input{flex-basis:100%;min-width:0}.filters select{flex:1;max-width:none;min-width:0;font-size:9px;padding:0 5px}.filters>span{margin-left:auto}
  .definition-columns{grid-template-columns:1fr}.definition-detail{padding-top:15px}.definition-detail .subsection-heading{gap:8px}.expected-cell{min-width:220px}
  .launch-panel{padding:14px 12px}.launch-header h2{font-size:14px}.launch-controls{grid-template-columns:1fr}.launch-button{grid-column:auto}.launch-note{font-size:9px}
  .run-heading{display:block}.run-actions{margin-top:10px;justify-content:space-between}.run-summary{gap:10px 15px}.run-summary>div:not(.run-summary-note){min-width:52px}.case-list-head,.case-row{grid-template-columns:minmax(0,1fr) 52px 50px;gap:4px;padding-right:8px;padding-left:8px}
  .detail-heading,.detail-content{padding-right:12px;padding-left:12px}.trace-panel{padding:9px}.trace-title{align-items:flex-start}.trace-title span{text-align:right}
  .compare-controls{grid-template-columns:1fr;padding:11px}.comparison-banner{display:block}.comparison-banner>.mode-chip{margin-top:10px}.comparison-metrics .metric{min-height:82px;padding:10px}.comparison-metrics .metric strong{font-size:19px}
  .footer{display:block;line-height:1.6}.footer span:last-child{display:block;text-align:left;margin-top:4px}
}
.model-draft-tag{display:inline-flex;align-items:center;gap:8px;padding:7px 10px;border:1px solid #ead9b5;border-radius:5px;background:#fff8e9;color:#86611b;font-size:10px;font-weight:700;white-space:nowrap}
.model-draft-tag i{width:7px;height:7px;border-radius:50%;background:#d49b35}
.model-warning{display:grid;gap:4px;margin:0 0 22px;padding:14px 16px;border:1px solid #ead9b5;border-left:3px solid #c89534;border-radius:7px;background:#fffaf0;color:#6b572d;font-size:11px}
.model-warning strong{font-size:12px;color:#755616}.model-warning span{line-height:1.6}
.model-metrics{grid-template-columns:repeat(4,minmax(0,1fr));margin-bottom:28px;border:1px solid var(--line);border-radius:9px;overflow:hidden}
.model-metrics .metric{min-width:0;border-top:0}.model-metrics .metric strong{font-variant-numeric:tabular-nums}.model-metrics .metric small{line-height:1.45}
.model-good{color:var(--success)!important}.model-bad{color:var(--danger)!important}
.model-section-heading{margin:0 0 13px}.model-meta-chip,.model-pair-count{padding:5px 8px;border:1px solid var(--line);border-radius:5px;background:#fff;color:var(--muted);font-size:10px;white-space:nowrap}
.model-cards{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:13px;margin-bottom:20px}
.model-card{min-width:0;padding:18px 20px;border:1px solid var(--line);border-radius:9px;background:#fff}
.model-card-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.model-card-heading h3{margin:5px 0 0;color:var(--ink);font:600 17px/1.3 ui-monospace,SFMono-Regular,Consolas,monospace;overflow-wrap:anywhere}
.model-score-line{display:flex;align-items:baseline;gap:8px;margin:22px 0 8px}.model-score-line strong{font-size:26px;line-height:1;color:var(--success)}.model-score-line span,.model-score-line b{font-size:10px;color:var(--muted);font-weight:500}.model-score-line b{margin-left:auto;color:var(--danger)}
.model-score-track{height:6px;overflow:hidden;border-radius:6px;background:#f4e8e7}.model-score-track i{display:block;height:100%;border-radius:6px;background:#25836c}
.model-observations{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin:17px 0 0;padding-top:14px;border-top:1px solid #edf0ed}.model-observations div{min-width:0}.model-observations dt{color:var(--muted);font-size:9px;line-height:1.4}.model-observations dd{margin:5px 0 0;color:var(--ink);font-size:11px;font-weight:650;font-variant-numeric:tabular-nums;overflow-wrap:anywhere}
.model-failures{margin-top:20px;padding:18px 20px;border:1px solid var(--line);border-radius:9px;background:#fff}.model-failures .subsection-heading>div>p:not(.eyebrow){margin:5px 0 0;color:var(--muted);font-size:10px}.model-failures .table-scroll{margin-top:14px}.model-failures td{white-space:normal;overflow-wrap:anywhere}.model-failures td code{font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:10px}
.model-provenance{margin-top:14px;padding:15px 17px;border:1px solid var(--line);border-radius:8px;background:#f9fbfa;color:var(--muted);font-size:10px}.model-provenance summary{color:var(--ink);font-size:11px;font-weight:650;cursor:pointer}.model-provenance p{line-height:1.6;overflow-wrap:anywhere}.model-provenance code{font-family:ui-monospace,SFMono-Regular,Consolas,monospace}.provenance-runs{display:grid;gap:4px;margin-top:10px}.provenance-runs code{color:#668072;font-size:9px;overflow-wrap:anywhere}
@media(max-width:900px){.model-metrics{grid-template-columns:repeat(2,minmax(0,1fr))}.model-metrics .metric:nth-child(2){border-right:0}.model-metrics .metric:nth-child(n+3){border-top:1px solid var(--line)}}
@media(max-width:680px){.model-cards{grid-template-columns:1fr}.model-card{padding:16px}.model-failures{padding:15px 12px}.model-observations{grid-template-columns:1fr 1fr}.model-observations div:last-child{grid-column:1/-1}.model-draft-tag{margin-top:10px}.model-failures .table-scroll{border:0}.model-failures table{min-width:580px}}
@media(max-width:420px){.model-metrics .metric{padding:13px}.model-metrics .metric strong{font-size:22px}.model-observations{grid-template-columns:1fr}.model-observations div:last-child{grid-column:auto}.model-score-line{gap:6px}.model-score-line b{font-size:9px}}
</style>
