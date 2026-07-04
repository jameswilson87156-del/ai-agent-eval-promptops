<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  Braces,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleDashed,
  Code2,
  FileSearch,
  Filter,
  GitBranch,
  GitCompare,
  Layers3,
  ListChecks,
  Play,
  RefreshCw,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  XCircle,
} from 'lucide-vue-next'
import EvalDashboard from './components/EvalDashboard.vue'
import MetricCard from './components/MetricCard.vue'
import StatusBadge from './components/StatusBadge.vue'
import type { CaseResultStatus, EvalCase, EvalRun, GenerationTrace, PromptProfile, Summary, VersionComparison } from './types'

type ViewKey = 'overview' | 'run' | 'versions' | 'review'
type FilterKey = 'all' | 'running' | 'review' | 'pass' | 'risk'
type ReviewState = 'pending' | 'approved' | 'revision'

const viewOptions: Array<{ key: ViewKey; label: string }> = [
  { key: 'overview', label: 'Overview' },
  { key: 'run', label: 'Eval Run' },
  { key: 'versions', label: 'Prompt Versions' },
  { key: 'review', label: 'Trace Review' },
]
const filterOptions: Array<{ key: FilterKey; label: string }> = [
  { key: 'all', label: 'All' },
  { key: 'running', label: 'Running' },
  { key: 'review', label: 'Review' },
  { key: 'pass', label: 'Passed' },
  { key: 'risk', label: 'High Risk' },
]

const initialView = new URLSearchParams(window.location.search).get('view') as ViewKey | null
const activeView = shallowRef<ViewKey>(viewOptions.some((item) => item.key === initialView) ? initialView! : 'overview')
const statusFilter = shallowRef<FilterKey>('all')
const searchQuery = ref('')
const prompts = shallowRef<PromptProfile[]>([])
const activePromptId = shallowRef<number | null>(null)
const activeRun = shallowRef<EvalRun | null>(null)
const activeComparison = shallowRef<VersionComparison | null>(null)
const traces = shallowRef<GenerationTrace[]>([])
const summary = shallowRef<Summary>({
  promptVersions: 0,
  evalRuns: 0,
  testCases: 0,
  latestPassRate: 0,
  averageLatencyMs: 0,
  pendingReviews: 0,
  highRiskCases: 0,
})
const activeCaseId = shallowRef<number | null>(null)
const reviewStates = ref<Record<number, ReviewState>>({})
const loading = shallowRef(false)
const switchingPrompt = shallowRef(false)
const runningEval = shallowRef(false)
const apiError = shallowRef('')
const reviewFeedback = shallowRef('')

const activePrompt = computed(() => prompts.value.find((prompt) => prompt.id === activePromptId.value) ?? prompts.value[0])
const activeCase = computed(() => {
  if (!activeRun.value?.cases.length) return null
  return activeRun.value.cases.find((testCase) => testCase.caseId === activeCaseId.value) ?? activeRun.value.cases[0]
})
const normalizedQuery = computed(() => searchQuery.value.trim().toLowerCase())
const filteredPrompts = computed(() => {
  if (!normalizedQuery.value) return prompts.value
  return prompts.value.filter((prompt) =>
    [prompt.name, prompt.key, prompt.currentVersion, prompt.owner, ...prompt.tags, ...prompt.versionLabels]
      .join(' ')
      .toLowerCase()
      .includes(normalizedQuery.value),
  )
})
const filteredCases = computed(() => {
  const run = activeRun.value
  if (!run) return []
  return run.cases.filter((testCase) => {
    const matchesQuery =
      !normalizedQuery.value ||
      [run.runKey, run.name, testCase.caseKey, testCase.name, testCase.input, testCase.reason, testCase.riskLevel, ...testCase.trace]
        .join(' ')
        .toLowerCase()
        .includes(normalizedQuery.value)
    const risk = caseRisk(testCase)
    const matchesStatus =
      statusFilter.value === 'all' ||
      (statusFilter.value === 'running' && run.status === 'running') ||
      (statusFilter.value === 'review' && testCase.reviewRequired && reviewState(testCase) === 'pending') ||
      (statusFilter.value === 'pass' && testCase.result === 'pass') ||
      (statusFilter.value === 'risk' && ['high', 'critical'].includes(risk))
    return matchesQuery && matchesStatus
  })
})
const averageScore = computed(() => {
  const cases = activeRun.value?.cases ?? []
  if (!cases.length) return 0
  return Math.round(cases.reduce((total, item) => total + item.score, 0) / cases.length)
})
const pendingReviewCount = computed(() =>
  (activeRun.value?.cases ?? []).filter(
    (testCase) => testCase.reviewRequired && reviewState(testCase) === 'pending',
  ).length,
)
const pendingReviewRate = computed(() => {
  const total = activeRun.value?.totalCases ?? 0
  return total ? Math.round((pendingReviewCount.value / total) * 100) : 0
})
const highImpactCaseCount = computed(() =>
  (activeRun.value?.cases ?? []).filter((testCase) => ['high', 'critical'].includes(testCase.riskLevel)).length,
)
const topMetrics = computed(() => [
  {
    label: 'Prompt Profiles',
    value: prompts.value.length || summary.value.promptVersions,
    unit: '',
    helper: `${summary.value.promptVersions} versions tracked`,
    icon: Layers3,
    tone: 'primary' as const,
  },
  {
    label: 'Latest Pass Rate',
    value: summary.value.latestPassRate,
    unit: '%',
    helper: activeRun.value ? `${activeRun.value.runKey} latest run` : 'waiting for run',
    icon: CheckCircle2,
    tone: 'success' as const,
  },
  {
    label: 'Cases Evaluated',
    value: activeRun.value?.totalCases ?? summary.value.testCases,
    unit: '',
    helper: `${summary.value.evalRuns} eval runs recorded`,
    icon: ListChecks,
    tone: 'neutral' as const,
  },
  {
    label: 'Review Required',
    dataMetric: '当前待 Review',
    value: pendingReviewCount.value,
    unit: '',
    helper: `${highImpactCaseCount.value} high-impact cases`,
    icon: FileSearch,
    tone: 'warning' as const,
  },
])
const statusCounts = computed(() => {
  const cases = activeRun.value?.cases ?? []
  return cases.reduce<Record<CaseResultStatus, number>>(
    (counts, testCase) => {
      counts[testCase.result] += 1
      return counts
    },
    { pass: 0, fail: 0, 'format-error': 0, 'risk-missing': 0 },
  )
})
const passRateDelta = computed(() =>
  activeComparison.value ? activeComparison.value.passRateAfter - activeComparison.value.passRateBefore : 0,
)
const latencyDelta = computed(() =>
  activeComparison.value ? activeComparison.value.latencyAfterMs - activeComparison.value.latencyBeforeMs : 0,
)
const iterationSuggestion = computed(() => {
  if (!activeComparison.value) return '完成一次对比评测后再生成迭代建议。'
  if (activeComparison.value.regressionRisk === 'high') return '先阻断候选版本，补充失败 Case，再复核格式与风险提示规则。'
  if (activeCase.value?.result === 'risk-missing') return '在 Prompt 中明确高影响场景必须输出 risk_notice，并为缺失字段增加规则校验。'
  if (activeCase.value?.result === 'format-error') return '收紧输出格式约束，并补充结构化字段示例与 FORMAT 规则。'
  if (activeCase.value?.result !== 'pass') return '根据当前规则失败原因缩小 Prompt 改动范围，重新执行同一评测集。'
  return '当前 Case 规则已通过；仍需人工检查业务含义后，才能决定是否推荐上线。'
})

onMounted(loadConsole)

async function loadConsole() {
  loading.value = true
  apiError.value = ''
  try {
    const [nextSummary, nextPrompts] = await Promise.all([
      getJson<Summary>('/api/dashboard/summary'),
      getJson<PromptProfile[]>('/api/prompts'),
    ])
    summary.value = nextSummary
    prompts.value = nextPrompts
    if (nextPrompts.length) await selectPrompt(nextPrompts[0])
  } catch (error) {
    apiError.value = error instanceof Error ? error.message : '无法连接 PromptOps API'
  } finally {
    loading.value = false
  }
}

async function selectPrompt(prompt: PromptProfile) {
  switchingPrompt.value = true
  apiError.value = ''
  try {
    const [nextRun, nextComparison] = await Promise.all([
      getJson<EvalRun>(`/api/prompts/${prompt.id}/eval-runs/current`),
      getJson<VersionComparison>(`/api/prompts/${prompt.id}/comparisons/latest`),
    ])
    const nextCaseId = nextRun.cases[0]?.caseId ?? null
    const nextTraces = nextCaseId ? await getTraces(nextRun.id, nextCaseId) : []
    activePromptId.value = prompt.id
    activeRun.value = nextRun
    activeComparison.value = nextComparison
    activeCaseId.value = nextCaseId
    traces.value = nextTraces
    reviewFeedback.value = ''
  } catch (error) {
    apiError.value = error instanceof Error ? error.message : 'Prompt 数据加载失败'
  } finally {
    switchingPrompt.value = false
  }
}

async function selectCase(testCase: EvalCase) {
  if (!activeRun.value) return
  apiError.value = ''
  try {
    const nextTraces = await getTraces(activeRun.value.id, testCase.caseId)
    activeCaseId.value = testCase.caseId
    traces.value = nextTraces
    reviewFeedback.value = ''
  } catch (error) {
    apiError.value = error instanceof Error ? error.message : 'Trace 加载失败'
  }
}

async function runMockBatch() {
  if (!activePrompt.value || !activeRun.value) return
  runningEval.value = true
  apiError.value = ''
  try {
    const nextRun = await postJson<EvalRun>('/api/eval-runs', {
      promptVersionId: activePrompt.value.currentVersionId,
      datasetId: activePrompt.value.datasetId,
    })
    const nextCaseId = nextRun.cases[0]?.caseId ?? null
    const [nextSummary, nextComparison, nextTraces] = await Promise.all([
      getJson<Summary>('/api/dashboard/summary'),
      getJson<VersionComparison>(`/api/prompts/${activePrompt.value.id}/comparisons/latest`),
      nextCaseId ? getTraces(nextRun.id, nextCaseId) : Promise.resolve([]),
    ])
    activeRun.value = nextRun
    summary.value = nextSummary
    activeComparison.value = nextComparison
    activeCaseId.value = nextCaseId
    traces.value = nextTraces
    reviewFeedback.value = 'Mock Eval 已完成，Review 队列已按新结果刷新。'
  } catch (error) {
    apiError.value = error instanceof Error ? error.message : '规则评测执行失败'
  } finally {
    runningEval.value = false
  }
}

function getTraces(runId: number, caseId: number) {
  return getJson<GenerationTrace[]>(`/api/generation-traces?runId=${runId}&caseId=${caseId}`)
}

function setView(view: ViewKey) {
  activeView.value = view
  const url = new URL(window.location.href)
  url.searchParams.set('view', view)
  window.history.replaceState({}, '', url)
}

function setReview(state: ReviewState) {
  if (!activeCase.value) return
  reviewStates.value[activeCase.value.caseId] = state
  reviewFeedback.value = state === 'approved'
    ? `${activeCase.value.caseKey} 已在本地 Demo 标记通过。`
    : `${activeCase.value.caseKey} 已在本地 Demo 退回修订。`
}

function resetFilters() {
  searchQuery.value = ''
  statusFilter.value = 'all'
}

function reviewState(testCase: EvalCase | null) {
  if (!testCase) return 'pending'
  return reviewStates.value[testCase.caseId] ?? 'pending'
}

function caseRisk(testCase: EvalCase) {
  return testCase.riskLevel
}

function statusLabel(status: CaseResultStatus) {
  return { pass: 'Passed', fail: 'Failed', 'format-error': 'Format error', 'risk-missing': 'Risk notice missing' }[status]
}

function promptStatusLabel(status: PromptProfile['status']) {
  return { Draft: 'Draft', Candidate: 'Candidate', Approved: 'Approved' }[status]
}

function runStatusLabel(status: EvalRun['status']) {
  return { queued: 'Queued', running: 'Running', finished: 'Finished' }[status]
}

function riskLabel(risk: VersionComparison['regressionRisk']) {
  return { low: 'Low risk', medium: 'Needs review', high: 'High risk' }[risk]
}

function statusIcon(status: CaseResultStatus) {
  return { pass: Check, fail: XCircle, 'format-error': Braces, 'risk-missing': AlertTriangle }[status]
}

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`${url} 请求失败（${response.status}）`)
  return response.json() as Promise<T>
}

async function postJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) throw new Error(`${url} 请求失败（${response.status}）`)
  return response.json() as Promise<T>
}
</script>

<template>
  <div class="app-shell" :class="{ 'dashboard-mode': activeView === 'overview' }" :data-ready="!loading && activePrompt && activeRun ? 'true' : 'false'">
    <a class="skip-link" href="#main">跳到主要内容</a>
    <header v-if="activeView !== 'overview'" class="hero-panel">
      <div class="brand-lockup">
        <div class="brand-mark" aria-hidden="true"><Activity :size="20" /></div>
        <div>
          <p class="eyebrow">Rule-based Prompt Evaluation Console</p>
          <h1>PromptOps Evaluation Lab</h1>
          <p class="hero-subtitle">A local PromptOps workbench for prompt profiles, deterministic Mock output, rule checks, generation traces, and frontend-only review decisions.</p>
        </div>
      </div>
      <div class="hero-actions">
        <div class="demo-badges" aria-label="演示数据说明">
          <StatusBadge dot tone="neutral">Local Demo</StatusBadge>
          <StatusBadge tone="neutral">Mock Output</StatusBadge>
          <StatusBadge tone="primary">Rule-based Eval</StatusBadge>
        </div>
        <button class="primary-button" type="button" :disabled="runningEval || !activePrompt" @click="runMockBatch">
          <RefreshCw v-if="runningEval" class="spin" :size="16" />
          <Play v-else :size="16" />
          {{ runningEval ? 'Running rule eval' : 'Run Mock Eval' }}
        </button>
      </div>
    </header>

    <div v-if="activeView !== 'overview'" class="trust-note" role="note">
      <ShieldCheck :size="16" />
      <span>No real LLM connected. No agent runtime. Results come from MockOutputGenerator and RuleEvaluator, and review decisions stay in the browser demo state.</span>
    </div>

    <nav v-if="activeView !== 'overview'" class="view-tabs" aria-label="Console views">
      <button
        v-for="item in viewOptions"
        :key="item.key"
        type="button"
        :data-view="item.key"
        :aria-pressed="activeView === item.key"
        @click="setView(item.key)"
      >
        {{ item.label }}
      </button>
    </nav>

    <section v-if="activeView !== 'overview'" class="toolbar" aria-label="Search and filters">
      <label class="search-box">
        <Search :size="18" />
        <span class="sr-only">搜索 Prompt、Eval Run、测试用例或 Trace</span>
        <input v-model="searchQuery" data-testid="global-search" type="search" aria-label="搜索 Prompt、Eval Run、测试用例或 Trace" placeholder="Search prompts, runs, cases, traces" />
      </label>
      <div class="filter-group" aria-label="Case status filters">
        <Filter :size="16" aria-hidden="true" />
        <button
          v-for="item in filterOptions"
          :key="item.key"
          type="button"
          :data-filter="item.key"
          :aria-pressed="statusFilter === item.key"
          @click="statusFilter = item.key"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section v-if="activeView !== 'overview'" class="scenario-strip" aria-label="Prompt profile scenarios">
      <span>Scenario tags</span>
      <button type="button" @click="searchQuery = 'ticket-router'">ticket-router</button>
      <button type="button" @click="searchQuery = 'log-analyzer'">log-analyzer</button>
      <button type="button" @click="searchQuery = 'jd-screen'">jd-screen</button>
      <button v-if="searchQuery" type="button" @click="resetFilters">Clear</button>
    </section>

    <section v-if="apiError" class="state-panel error-state" role="alert" aria-live="assertive">
      <AlertTriangle :size="20" />
      <div><strong>本地数据服务暂不可用</strong><p>{{ apiError }}</p></div>
      <button type="button" @click="loadConsole">重新连接</button>
    </section>

    <section v-if="loading" class="loading-grid" role="status" aria-live="polite" aria-label="正在加载">
      <div v-for="item in 7" :key="item" class="skeleton"></div>
    </section>

    <template v-else-if="activePrompt && activeRun && activeComparison">
      <EvalDashboard
        v-if="activeView === 'overview'"
        :active-prompt-id="activePromptId"
        :prompts="prompts"
        :running-eval="runningEval"
        @open-view="setView"
        @run-eval="runMockBatch"
        @select-prompt="selectPrompt"
      />

      <template v-else>
        <section id="main" class="metrics-grid" tabindex="-1" aria-label="评测摘要指标">
          <MetricCard
            v-for="metric in topMetrics"
            :key="metric.label"
            :data-metric="metric.dataMetric"
            :helper="metric.helper"
            :icon="metric.icon"
            :label="metric.label"
            :tone="metric.tone"
            :unit="metric.unit"
            :value="metric.value"
          />
        </section>

      <main v-if="activeView === 'run'" class="run-layout">
        <aside class="panel prompt-panel sticky-panel">
          <div class="panel-heading"><div><p class="eyebrow">PROMPT REGISTRY</p><h2>Prompt Profiles</h2></div><SlidersHorizontal :size="18" /></div>
          <div class="prompt-list">
            <button v-for="prompt in filteredPrompts" :key="prompt.id" type="button" class="prompt-row" :class="{ active: prompt.id === activePrompt.id }" :data-prompt-id="prompt.id" :aria-pressed="prompt.id === activePrompt.id" @click="selectPrompt(prompt)">
              <span class="status-dot" :data-status="prompt.status"></span>
              <span class="prompt-copy"><strong>{{ prompt.name }}</strong><small>{{ prompt.tags[0] }} · {{ prompt.owner }}</small></span>
              <span class="prompt-version"><b>{{ prompt.currentVersion }}</b><small>{{ promptStatusLabel(prompt.status) }}</small></span>
            </button>
          </div>
        </aside>
        <section class="panel run-detail-panel">
          <div class="panel-heading">
            <div><p class="eyebrow">EVAL RUN / {{ activeRun.runKey }}</p><h2>{{ activeRun.name }}</h2></div>
            <StatusBadge tone="success">{{ runStatusLabel(activeRun.status) }}</StatusBadge>
          </div>
          <div class="run-summary-strip">
            <div><span>Cases</span><strong>{{ activeRun.totalCases }}</strong></div>
            <div><span>Pass rate</span><strong>{{ activeRun.passRate }}%</strong></div>
            <div><span>Avg score</span><strong>{{ averageScore }}</strong></div>
            <div><span>Simulated latency</span><strong>{{ activeRun.averageLatencyMs }}ms</strong></div>
            <div><span>Risk cases</span><strong>{{ activeRun.riskCaseCount }}</strong></div>
          </div>
          <div class="result-legend" aria-label="结果分布">
            <span><i data-status="pass"></i>Passed {{ statusCounts.pass }}</span>
            <span><i data-status="fail"></i>Failed {{ statusCounts.fail }}</span>
            <span><i data-status="format-error"></i>Format error {{ statusCounts['format-error'] }}</span>
            <span><i data-status="risk-missing"></i>Risk missing {{ statusCounts['risk-missing'] }}</span>
          </div>
          <div class="case-table" aria-label="测试用例结果">
            <div class="case-table-head"><span>Case / dataset</span><span>Risk</span><span>Status</span><span>Score</span><span>Latency</span></div>
            <button v-for="testCase in filteredCases" :key="testCase.resultId" type="button" :data-case-id="testCase.caseId" :aria-pressed="testCase.caseId === activeCase?.caseId" :class="{ active: testCase.caseId === activeCase?.caseId }" @click="selectCase(testCase); setView('review')">
              <span><strong>{{ testCase.name }}</strong><small>{{ testCase.caseKey }} · {{ testCase.dataset }}</small></span>
              <span><b class="risk-label" :data-risk="caseRisk(testCase)">{{ caseRisk(testCase) }}</b></span>
              <span><b class="result-label" :data-status="testCase.result">{{ statusLabel(testCase.result) }}</b></span>
              <span class="score-bar"><strong>{{ testCase.score }}</strong><i><em :style="{ width: `${testCase.score}%` }"></em></i></span>
              <span class="mono-value">{{ testCase.latencyMs }}ms</span>
            </button>
          </div>
          <div v-if="!filteredCases.length" class="empty-state"><FileSearch :size="28" /><strong>没有匹配的 Case</strong><p>清除搜索词或切换状态筛选后再试。</p><button type="button" @click="resetFilters">清除搜索与筛选</button></div>
        </section>
        <aside v-if="activeCase" class="panel rule-audit-panel">
          <div class="panel-heading">
            <div><p class="eyebrow">RULE AUDIT</p><h2>Selected Case Checks</h2></div>
            <FileSearch :size="18" />
          </div>
          <div class="engine-stack" aria-label="Evaluation engines">
            <StatusBadge tone="neutral">RuleEvaluator</StatusBadge>
            <StatusBadge tone="neutral">MockOutputGenerator</StatusBadge>
            <StatusBadge tone="warning">Simulated latency</StatusBadge>
          </div>
          <div class="audit-case-summary">
            <span>{{ activeCase.caseKey }}</span>
            <strong>{{ activeCase.name }}</strong>
            <p>{{ activeCase.reason }}</p>
          </div>
          <div class="rule-checks audit-checks">
            <div v-for="check in activeCase.ruleChecks" :key="`${check.ruleType}-${check.expected}`" :data-passed="check.passed">
              <component :is="check.passed ? CheckCircle2 : XCircle" :size="16" />
              <span><strong>{{ check.ruleType }}</strong><small>{{ check.explanation }}</small></span>
              <b>{{ check.passed ? 'PASS' : 'FAIL' }}</b>
            </div>
          </div>
          <button class="secondary-button" type="button" @click="setView('review')">Open full Trace Review <ArrowRight :size="15" /></button>
        </aside>
        <section class="panel attribution-panel">
          <div class="panel-heading"><div><p class="eyebrow">FAILURE ATTRIBUTION</p><h2>Failure Reasons</h2></div><AlertTriangle :size="18" /></div>
          <div class="attribution-list">
            <div v-for="(reason, index) in activeComparison.topFailureReasons" :key="reason"><span>0{{ index + 1 }}</span><p>{{ reason }}</p></div>
            <div v-if="!activeComparison.topFailureReasons.length"><span>00</span><p>当前版本没有规则失败项。</p></div>
          </div>
          <div class="suggestion-callout"><Sparkles :size="18" /><div><strong>Rule-based iteration note</strong><p>{{ iterationSuggestion }}</p></div></div>
        </section>
      </main>

      <main v-else-if="activeView === 'versions'" class="versions-layout">
        <section class="panel version-lineage-panel">
          <div class="panel-heading"><div><p class="eyebrow">VERSION LINEAGE</p><h2>{{ activePrompt.name }}</h2></div><GitBranch :size="18" /></div>
          <p class="panel-intro">Version labels, current status, and prompt snapshots come from the backend demo data. No production approval or online benchmark is implied.</p>
          <div class="version-lineage">
            <article v-for="(version, index) in activePrompt.versionLabels" :key="version" :class="{ current: version === activePrompt.currentVersion }">
              <span class="version-index">0{{ index + 1 }}</span>
              <span><strong>{{ version }}</strong><small>{{ version === activePrompt.currentVersion ? 'Current · ' + promptStatusLabel(activePrompt.status) : 'Historical baseline' }}</small></span>
              <CheckCircle2 v-if="version === activePrompt.currentVersion" :size="17" />
              <CircleDashed v-else :size="17" />
            </article>
          </div>
          <dl class="metadata-list">
            <div><dt>Owner</dt><dd>{{ activePrompt.owner }}</dd></div>
            <div><dt>Updated</dt><dd>{{ activePrompt.updatedAt || 'Demo data not recorded' }}</dd></div>
            <div><dt>Release decision</dt><dd>{{ activePrompt.status === 'Approved' ? 'Still needs review' : 'No' }}</dd></div>
          </dl>
        </section>
        <section class="panel prompt-spec-panel">
          <div class="panel-heading"><div><p class="eyebrow">PROMPT SPEC</p><h2>模板与变量</h2></div><Code2 :size="18" /></div>
          <div class="code-preview"><span>{{ activePrompt.currentVersion }} · {{ activePrompt.outputRequirements[0] }}</span><pre>{{ activePrompt.template }}</pre></div>
          <div class="storage-note">
            <Braces :size="16" />
            <span><strong>variables_json</strong> stores a standard JSON array and the API still returns a typed <code>variables</code> list.</span>
          </div>
          <div class="spec-grid">
            <section><h3>Variables</h3><div class="variable-list"><div v-for="variable in activePrompt.variables" :key="variable.name"><code>{{ variable.name }}</code><span>{{ variable.description }}</span><b>{{ variable.required ? 'Required' : 'Optional' }}</b></div></div></section>
            <section><h3>Output format</h3><ul class="plain-list"><li v-for="field in activePrompt.outputRequirements" :key="field"><Check :size="14" />{{ field }}</li></ul></section>
          </div>
        </section>
        <section class="panel rules-panel">
          <div class="panel-heading"><div><p class="eyebrow">SCORING RULES</p><h2>RuleEvaluator Rules</h2></div><ListChecks :size="18" /></div>
          <div class="rules-list"><div v-for="(rule, index) in activePrompt.scoringRules" :key="rule"><span>R{{ String(index + 1).padStart(2, '0') }}</span><p>{{ rule }}</p><b>Active</b></div></div>
        </section>
        <section class="panel version-compare-panel">
          <div class="panel-heading"><div><p class="eyebrow">COMPARE</p><h2>{{ activeComparison.fromVersion }} vs {{ activeComparison.toVersion }}</h2></div><GitCompare :size="18" /></div>
          <div class="version-compare-flow">
            <div><span>{{ activeComparison.fromVersion }}</span><strong>{{ activeComparison.passRateBefore }}%</strong><small>{{ activeComparison.latencyBeforeMs }}ms</small></div>
            <ArrowRight :size="20" />
            <div class="current"><span>{{ activeComparison.toVersion }}</span><strong>{{ activeComparison.passRateAfter }}%</strong><small>{{ activeComparison.latencyAfterMs }}ms</small></div>
            <div class="compare-decision" :data-risk="activeComparison.regressionRisk"><span>回归风险</span><strong>{{ riskLabel(activeComparison.regressionRisk) }}</strong><p>{{ iterationSuggestion }}</p></div>
          </div>
        </section>
      </main>

      <main v-else class="review-layout">
        <aside class="panel review-case-panel">
          <div class="panel-heading"><div><p class="eyebrow">CASE RESULTS</p><h2>Review Queue</h2></div><ListChecks :size="18" /></div>
          <div class="review-case-list">
            <button v-for="testCase in filteredCases" :key="testCase.resultId" type="button" :data-case-id="testCase.caseId" :aria-pressed="testCase.caseId === activeCase?.caseId" :class="{ active: testCase.caseId === activeCase?.caseId }" @click="selectCase(testCase)">
              <span class="case-status" :data-status="testCase.result"><component :is="statusIcon(testCase.result)" :size="14" /></span>
              <span><strong>{{ testCase.name }}</strong><small>{{ testCase.caseKey }} · {{ caseRisk(testCase) }} risk</small></span>
              <b>{{ testCase.score }}</b>
            </button>
          </div>
          <div v-if="!filteredCases.length" class="compact-empty">没有匹配的 Case。</div>
        </aside>
        <section v-if="activeCase" class="panel trace-detail-panel">
          <div class="panel-heading"><div><p class="eyebrow">EXECUTION TRACE</p><h2>{{ activeCase.caseKey }} / {{ activeCase.name }}</h2></div><CircleDashed :size="18" /></div>
          <div class="trace-summary-grid">
            <div><span>Rule score</span><strong>{{ activeCase.score }}</strong></div>
            <div><span>Status</span><strong>{{ statusLabel(activeCase.result) }}</strong></div>
            <div><span>Simulated latency</span><strong>{{ activeCase.latencyMs }}ms</strong></div>
            <div><span>Risk level</span><strong>{{ caseRisk(activeCase) }}</strong></div>
          </div>
          <section class="trace-section"><h3>Input summary</h3><p>{{ activeCase.input }}</p><small>Expected：{{ activeCase.expected }}</small></section>
          <section class="trace-section"><h3>Mock output from MockOutputGenerator</h3><p>{{ activeCase.outputSummary }}</p><pre>{{ activeCase.mockOutput }}</pre></section>
          <section class="trace-section"><h3>RuleEvaluator scoring summary</h3><div class="rule-checks"><div v-for="check in activeCase.ruleChecks" :key="`${check.ruleType}-${check.expected}`" :data-passed="check.passed"><component :is="check.passed ? CheckCircle2 : XCircle" :size="16" /><span><strong>{{ check.ruleType }}</strong><small>{{ check.explanation }}</small></span><b>{{ check.passed ? 'PASS' : 'FAIL' }}</b></div></div></section>
          <section class="trace-section"><h3>Generation Trace</h3><div class="generation-list"><div v-for="trace in traces" :key="trace.id"><span>{{ trace.status }}</span><p>{{ trace.errorReason || trace.outputSummary }}</p><time>{{ trace.latencyMs }}ms · {{ trace.createdAt }}</time></div></div></section>
        </section>
        <aside v-if="activeCase" class="review-side">
          <section class="panel review-decision-panel">
            <div class="panel-heading compact"><div><p class="eyebrow">HUMAN REVIEW</p><h2>Frontend Review</h2></div><ShieldCheck :size="18" /></div>
            <span class="review-state" data-testid="review-state" role="status" aria-live="polite" :data-state="reviewState(activeCase)">{{ reviewState(activeCase) === 'pending' ? '等待人工结论' : reviewState(activeCase) === 'approved' ? '本地标记：通过' : '本地标记：退回修订' }}</span>
            <p>{{ activeCase.reviewRequired ? 'This case enters review because a rule failed or the risk level is high/critical.' : 'This case is not forced into review, but can still be checked locally.' }}</p>
            <p><strong>Review state is frontend-only demo state.</strong> It is not persisted and is not a production approval workflow.</p>
            <div class="review-actions"><button type="button" data-review-action="approved" :aria-pressed="reviewState(activeCase) === 'approved'" @click="setReview('approved')"><Check :size="15" />标记通过</button><button type="button" data-review-action="revision" :aria-pressed="reviewState(activeCase) === 'revision'" @click="setReview('revision')"><RefreshCw :size="15" />退回修订</button></div>
            <span class="sr-only" role="status" aria-live="polite">{{ reviewFeedback }}</span>
          </section>
          <section class="panel risk-panel">
            <div class="panel-heading compact"><div><p class="eyebrow">RISK & NEXT STEP</p><h2>Risk & Prompt Note</h2></div><AlertTriangle :size="18" /></div>
            <span class="risk-banner" :data-risk="caseRisk(activeCase)">{{ caseRisk(activeCase) }} risk</span>
            <p>{{ activeCase.reason }}</p>
            <div class="suggestion-callout"><Sparkles :size="17" /><div><strong>Rule-based prompt note</strong><p>{{ iterationSuggestion }}</p></div></div>
          </section>
        </aside>
      </main>

      <footer class="app-footer">
        <span>PromptOps Evaluation Lab · Local Demo</span>
        <span>Mock Output · RuleEvaluator · Frontend-only Review</span>
      </footer>
      </template>
    </template>

    <section v-else-if="!apiError" class="state-panel empty-state">
      <Layers3 :size="28" /><strong>暂无评测数据</strong><p>启用 Demo 数据或创建 Prompt 与评测集后再试。</p>
    </section>

    <div v-if="switchingPrompt" class="switching-indicator" aria-live="polite">正在切换 Prompt…</div>
  </div>
</template>
