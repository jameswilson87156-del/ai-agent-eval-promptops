<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  BarChart3,
  Braces,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleDashed,
  Clock3,
  Code2,
  FileSearch,
  FileText,
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
  Timer,
  XCircle,
} from 'lucide-vue-next'
import type { CaseResultStatus, EvalCase, EvalRun, GenerationTrace, PromptProfile, Summary, VersionComparison } from './types'

type ViewKey = 'overview' | 'run' | 'versions' | 'review'
type FilterKey = 'all' | 'running' | 'review' | 'pass' | 'risk'
type ReviewState = 'pending' | 'approved' | 'revision'

const viewOptions: Array<{ key: ViewKey; label: string }> = [
  { key: 'overview', label: '实验室概览' },
  { key: 'run', label: '评测运行' },
  { key: 'versions', label: 'Prompt 版本' },
  { key: 'review', label: 'Trace 与 Review' },
]
const filterOptions: Array<{ key: FilterKey; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'running', label: '运行中' },
  { key: 'review', label: '待 Review' },
  { key: 'pass', label: '已通过' },
  { key: 'risk', label: '高风险' },
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
  { label: 'Prompt 版本', value: summary.value.promptVersions, unit: '', icon: GitBranch, tone: 'accent' },
  { label: 'Eval Runs', value: summary.value.evalRuns, unit: '', icon: Activity, tone: 'neutral' },
  { label: '测试用例', value: summary.value.testCases, unit: '', icon: ListChecks, tone: 'neutral' },
  { label: '平均规则分', value: averageScore.value, unit: '/100', icon: BarChart3, tone: 'accent' },
  { label: '通过率', value: summary.value.latestPassRate, unit: '%', icon: CheckCircle2, tone: 'success' },
  { label: '当前待 Review', value: pendingReviewCount.value, unit: '', icon: FileSearch, tone: 'warning' },
  { label: '当前高风险 Case', value: highImpactCaseCount.value, unit: '', icon: AlertTriangle, tone: 'danger' },
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
  return { pass: '通过', fail: '失败', 'format-error': '格式错误', 'risk-missing': '风险缺失' }[status]
}

function promptStatusLabel(status: PromptProfile['status']) {
  return { Draft: '草稿', Candidate: '候选', Approved: '已批准' }[status]
}

function runStatusLabel(status: EvalRun['status']) {
  return { queued: '等待中', running: '运行中', finished: '已完成' }[status]
}

function riskLabel(risk: VersionComparison['regressionRisk']) {
  return { low: '低风险', medium: '需复核', high: '高风险' }[risk]
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
  <div class="app-shell" :data-ready="!loading && activePrompt && activeRun ? 'true' : 'false'">
    <a class="skip-link" href="#main">跳到主要内容</a>
    <header class="hero-panel">
      <div class="brand-lockup">
        <div class="brand-mark" aria-hidden="true"><Activity :size="20" /></div>
        <div>
          <p class="eyebrow">PROMPTOPS EVALUATION LAB</p>
          <h1>AI Agent 评测中心</h1>
          <p class="hero-subtitle">围绕 Prompt 版本、评测集、Case 结果、Trace 与人工 Review，演示可解释的本地迭代闭环。</p>
        </div>
      </div>
      <div class="hero-actions">
        <div class="demo-badges" aria-label="演示数据说明">
          <span><i></i>Local Demo</span>
          <span>Mock Evaluation Data</span>
        </div>
        <button class="primary-button" type="button" :disabled="runningEval || !activePrompt" @click="runMockBatch">
          <RefreshCw v-if="runningEval" class="spin" :size="16" />
          <Play v-else :size="16" />
          {{ runningEval ? '正在执行规则评测' : '运行 Mock Eval' }}
        </button>
      </div>
    </header>

    <div class="trust-note" role="note">
      <ShieldCheck :size="16" />
      <span>评测结果来自本地 Mock 输出与确定性规则评分，不代表真实 LLM 或生产数据；候选版本上线前必须人工 Review。</span>
    </div>

    <nav class="view-tabs" aria-label="实验室视图">
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

    <section class="toolbar" aria-label="搜索与筛选">
      <label class="search-box">
        <Search :size="18" />
        <span class="sr-only">搜索 Prompt、Eval Run、测试用例或 Trace</span>
        <input v-model="searchQuery" data-testid="global-search" type="search" aria-label="搜索 Prompt、Eval Run、测试用例或 Trace" placeholder="搜索 Prompt、Eval Run、测试用例、Trace" />
      </label>
      <div class="filter-group" aria-label="Case 状态筛选">
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

    <section v-if="apiError" class="state-panel error-state" role="alert" aria-live="assertive">
      <AlertTriangle :size="20" />
      <div><strong>本地数据服务暂不可用</strong><p>{{ apiError }}</p></div>
      <button type="button" @click="loadConsole">重新连接</button>
    </section>

    <section v-if="loading" class="loading-grid" role="status" aria-live="polite" aria-label="正在加载">
      <div v-for="item in 7" :key="item" class="skeleton"></div>
    </section>

    <template v-else-if="activePrompt && activeRun && activeComparison">
      <section id="main" class="metrics-grid" tabindex="-1" aria-label="评测摘要指标">
        <article v-for="metric in topMetrics" :key="metric.label" class="metric-card" :data-tone="metric.tone" :data-metric="metric.label">
          <span class="metric-icon"><component :is="metric.icon" :size="17" /></span>
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}<small>{{ metric.unit }}</small></strong>
        </article>
      </section>

      <main v-if="activeView === 'overview'" class="overview-layout">
        <section class="panel prompt-panel">
          <div class="panel-heading">
            <div><p class="eyebrow">PROMPT REGISTRY</p><h2>Prompt 版本</h2></div>
            <GitBranch :size="18" />
          </div>
          <div v-if="filteredPrompts.length" class="prompt-list">
            <button
              v-for="prompt in filteredPrompts"
              :key="prompt.id"
              type="button"
              class="prompt-row"
              :class="{ active: prompt.id === activePrompt.id }"
              :data-prompt-id="prompt.id"
              :aria-pressed="prompt.id === activePrompt.id"
              @click="selectPrompt(prompt)"
            >
              <span class="status-dot" :data-status="prompt.status"></span>
              <span class="prompt-copy"><strong>{{ prompt.name }}</strong><small>{{ prompt.tags[0] }} · {{ prompt.owner }}</small></span>
              <span class="prompt-version"><b>{{ prompt.currentVersion }}</b><small>{{ promptStatusLabel(prompt.status) }}</small></span>
            </button>
          </div>
          <div v-else class="compact-empty">没有匹配的 Prompt。</div>
          <button class="text-button" type="button" @click="setView('versions')">查看版本详情 <ArrowRight :size="15" /></button>
        </section>

        <section class="panel run-panel">
          <div class="panel-heading">
            <div><p class="eyebrow">CURRENT EVAL RUN</p><h2>当前评测运行</h2></div>
            <span class="status-pill success">{{ runStatusLabel(activeRun.status) }}</span>
          </div>
          <div class="run-identity">
            <div><span>Run ID</span><strong>{{ activeRun.runKey }}</strong></div>
            <div><span>评测集</span><strong>{{ activeRun.name }}</strong></div>
            <div><span>Prompt 快照</span><strong>{{ activeRun.promptSnapshot }}</strong></div>
          </div>
          <div class="run-scoreboard">
            <div class="score-ring" :style="{ '--score': `${activeRun.passRate}%` }">
              <strong>{{ activeRun.passRate }}%</strong><span>通过率</span>
            </div>
            <div class="run-bars">
              <div><span>通过</span><b>{{ statusCounts.pass }}</b><i><em :style="{ width: `${activeRun.passRate}%` }"></em></i></div>
              <div><span>待 Review</span><b>{{ pendingReviewCount }}</b><i><em class="warning-bar" :style="{ width: `${pendingReviewRate}%` }"></em></i></div>
              <p>{{ activeRun.totalCases }} 个 Case · 平均 {{ activeRun.averageLatencyMs }}ms · {{ activeRun.riskCaseCount }} 个风险用例</p>
            </div>
          </div>
          <div class="case-preview-list">
            <button
              v-for="testCase in filteredCases.slice(0, 4)"
              :key="testCase.resultId"
              type="button"
              :class="{ active: testCase.caseId === activeCase?.caseId }"
              :data-case-id="testCase.caseId"
              :aria-pressed="testCase.caseId === activeCase?.caseId"
              @click="selectCase(testCase); setView('review')"
            >
              <span class="case-status" :data-status="testCase.result"><component :is="statusIcon(testCase.result)" :size="14" /></span>
              <span><strong>{{ testCase.name }}</strong><small>{{ testCase.caseKey }} · {{ statusLabel(testCase.result) }}</small></span>
              <b>{{ testCase.score }}</b>
              <ChevronRight :size="15" />
            </button>
          </div>
          <div v-if="!filteredCases.length" class="compact-empty">当前筛选下没有 Case 结果。</div>
        </section>

        <aside class="overview-side">
          <section class="panel review-card">
            <div class="panel-heading compact"><div><p class="eyebrow">REVIEW GATE</p><h2>上线判断</h2></div><ShieldCheck :size="18" /></div>
            <div class="risk-summary" :data-risk="activeComparison.regressionRisk">
              <span>回归风险</span><strong>{{ riskLabel(activeComparison.regressionRisk) }}</strong>
              <p>{{ activeComparison.regressionRisk === 'low' ? '可进入人工复核，不代表已批准上线。' : '需要先处理失败 Case，再决定是否进入人工复核。' }}</p>
            </div>
            <dl class="review-facts">
              <div><dt>当前版本</dt><dd>{{ activePrompt.currentVersion }}</dd></div>
              <div><dt>待 Review</dt><dd>{{ pendingReviewCount }}</dd></div>
              <div><dt>本地确认</dt><dd>{{ reviewState(activeCase) === 'pending' ? '未记录' : '已记录' }}</dd></div>
            </dl>
            <button class="secondary-button" type="button" @click="setView('review')">进入人工 Review <ArrowRight :size="15" /></button>
          </section>

          <section class="panel trace-card">
            <div class="panel-heading compact"><div><p class="eyebrow">TRACE SNAPSHOT</p><h2>当前 Trace</h2></div><CircleDashed :size="18" /></div>
            <template v-if="activeCase">
              <span class="mono-label">{{ activeCase.caseKey }}</span>
              <strong>{{ activeCase.name }}</strong>
              <p>{{ activeCase.outputSummary }}</p>
              <div class="trace-meta"><span>{{ traces[0]?.status ?? '—' }}</span><span>{{ traces[0]?.latencyMs ?? activeCase.latencyMs }}ms</span></div>
            </template>
          </section>
        </aside>

        <section class="panel compare-panel">
          <div class="panel-heading">
            <div><p class="eyebrow">VERSION COMPARISON</p><h2>{{ activeComparison.fromVersion }} → {{ activeComparison.toVersion }} 版本对比</h2></div>
            <GitCompare :size="18" />
          </div>
          <div class="comparison-grid">
            <div><span>通过率变化</span><strong :class="passRateDelta >= 0 ? 'positive' : 'negative'">{{ passRateDelta > 0 ? '+' : '' }}{{ passRateDelta }}%</strong><small>{{ activeComparison.passRateBefore }}% → {{ activeComparison.passRateAfter }}%</small></div>
            <div><span>平均耗时变化</span><strong :class="latencyDelta <= 0 ? 'positive' : 'negative'">{{ latencyDelta > 0 ? '+' : '' }}{{ latencyDelta }}ms</strong><small>{{ activeComparison.latencyBeforeMs }}ms → {{ activeComparison.latencyAfterMs }}ms</small></div>
            <div class="failure-reasons"><span>失败 Case 归因</span><ul><li v-for="reason in activeComparison.topFailureReasons" :key="reason">{{ reason }}</li></ul></div>
            <div class="suggestion-block"><Sparkles :size="17" /><span>Prompt 迭代建议</span><p>{{ iterationSuggestion }}</p></div>
          </div>
        </section>
      </main>

      <main v-else-if="activeView === 'run'" class="run-layout">
        <aside class="panel prompt-panel sticky-panel">
          <div class="panel-heading"><div><p class="eyebrow">PROMPT REGISTRY</p><h2>选择 Prompt</h2></div><SlidersHorizontal :size="18" /></div>
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
            <span class="status-pill success">{{ runStatusLabel(activeRun.status) }}</span>
          </div>
          <div class="run-summary-strip">
            <div><span>Case 数量</span><strong>{{ activeRun.totalCases }}</strong></div>
            <div><span>通过率</span><strong>{{ activeRun.passRate }}%</strong></div>
            <div><span>平均规则分</span><strong>{{ averageScore }}</strong></div>
            <div><span>平均延迟</span><strong>{{ activeRun.averageLatencyMs }}ms</strong></div>
            <div><span>风险 Case</span><strong>{{ activeRun.riskCaseCount }}</strong></div>
          </div>
          <div class="result-legend" aria-label="结果分布">
            <span><i data-status="pass"></i>通过 {{ statusCounts.pass }}</span>
            <span><i data-status="fail"></i>失败 {{ statusCounts.fail }}</span>
            <span><i data-status="format-error"></i>格式错误 {{ statusCounts['format-error'] }}</span>
            <span><i data-status="risk-missing"></i>风险缺失 {{ statusCounts['risk-missing'] }}</span>
          </div>
          <div class="case-table" aria-label="测试用例结果">
            <div class="case-table-head"><span>Case / 数据集</span><span>风险</span><span>状态</span><span>评分</span><span>延迟</span></div>
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
        <section class="panel attribution-panel">
          <div class="panel-heading"><div><p class="eyebrow">FAILURE ATTRIBUTION</p><h2>失败 Case 归因</h2></div><AlertTriangle :size="18" /></div>
          <div class="attribution-list">
            <div v-for="(reason, index) in activeComparison.topFailureReasons" :key="reason"><span>0{{ index + 1 }}</span><p>{{ reason }}</p></div>
            <div v-if="!activeComparison.topFailureReasons.length"><span>00</span><p>当前版本没有规则失败项。</p></div>
          </div>
          <div class="suggestion-callout"><Sparkles :size="18" /><div><strong>规则归纳建议</strong><p>{{ iterationSuggestion }}</p></div></div>
        </section>
      </main>

      <main v-else-if="activeView === 'versions'" class="versions-layout">
        <section class="panel version-lineage-panel">
          <div class="panel-heading"><div><p class="eyebrow">VERSION LINEAGE</p><h2>{{ activePrompt.name }}</h2></div><GitBranch :size="18" /></div>
          <p class="panel-intro">演示数据保留版本标签、当前状态和 Prompt 快照；未虚构逐版本提交时间与变更说明。</p>
          <div class="version-lineage">
            <article v-for="(version, index) in activePrompt.versionLabels" :key="version" :class="{ current: version === activePrompt.currentVersion }">
              <span class="version-index">0{{ index + 1 }}</span>
              <span><strong>{{ version }}</strong><small>{{ version === activePrompt.currentVersion ? '当前版本 · ' + promptStatusLabel(activePrompt.status) : '历史评测基线' }}</small></span>
              <CheckCircle2 v-if="version === activePrompt.currentVersion" :size="17" />
              <CircleDashed v-else :size="17" />
            </article>
          </div>
          <dl class="metadata-list">
            <div><dt>Owner</dt><dd>{{ activePrompt.owner }}</dd></div>
            <div><dt>最近更新</dt><dd>{{ activePrompt.updatedAt || '演示数据未记录' }}</dd></div>
            <div><dt>推荐上线</dt><dd>{{ activePrompt.status === 'Approved' ? '仍需人工复核' : '否' }}</dd></div>
          </dl>
        </section>
        <section class="panel prompt-spec-panel">
          <div class="panel-heading"><div><p class="eyebrow">PROMPT SPEC</p><h2>模板与变量</h2></div><Code2 :size="18" /></div>
          <div class="code-preview"><span>{{ activePrompt.currentVersion }} · {{ activePrompt.outputRequirements[0] }}</span><pre>{{ activePrompt.template }}</pre></div>
          <div class="spec-grid">
            <section><h3>变量列表</h3><div class="variable-list"><div v-for="variable in activePrompt.variables" :key="variable.name"><code>{{ variable.name }}</code><span>{{ variable.description }}</span><b>{{ variable.required ? '必填' : '可选' }}</b></div></div></section>
            <section><h3>输出要求</h3><ul class="plain-list"><li v-for="field in activePrompt.outputRequirements" :key="field"><Check :size="14" />{{ field }}</li></ul></section>
          </div>
        </section>
        <section class="panel rules-panel">
          <div class="panel-heading"><div><p class="eyebrow">SCORING RULES</p><h2>评分规则</h2></div><ListChecks :size="18" /></div>
          <div class="rules-list"><div v-for="(rule, index) in activePrompt.scoringRules" :key="rule"><span>R{{ String(index + 1).padStart(2, '0') }}</span><p>{{ rule }}</p><b>启用</b></div></div>
        </section>
        <section class="panel version-compare-panel">
          <div class="panel-heading"><div><p class="eyebrow">COMPARE</p><h2>{{ activeComparison.fromVersion }} 与 {{ activeComparison.toVersion }}</h2></div><GitCompare :size="18" /></div>
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
          <div class="panel-heading"><div><p class="eyebrow">CASE RESULTS</p><h2>测试用例结果</h2></div><ListChecks :size="18" /></div>
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
            <div><span>规则评分</span><strong>{{ activeCase.score }}</strong></div>
            <div><span>执行状态</span><strong>{{ statusLabel(activeCase.result) }}</strong></div>
            <div><span>延迟</span><strong>{{ activeCase.latencyMs }}ms</strong></div>
            <div><span>风险等级</span><strong>{{ caseRisk(activeCase) }}</strong></div>
          </div>
          <section class="trace-section"><h3>模型输入摘要</h3><p>{{ activeCase.input }}</p><small>Expected：{{ activeCase.expected }}</small></section>
          <section class="trace-section"><h3>Mock 输出摘要</h3><p>{{ activeCase.outputSummary }}</p><pre>{{ activeCase.mockOutput }}</pre></section>
          <section class="trace-section"><h3>评分规则检查</h3><div class="rule-checks"><div v-for="check in activeCase.ruleChecks" :key="`${check.ruleType}-${check.expected}`" :data-passed="check.passed"><component :is="check.passed ? CheckCircle2 : XCircle" :size="16" /><span><strong>{{ check.ruleType }}</strong><small>{{ check.explanation }}</small></span><b>{{ check.passed ? 'PASS' : 'FAIL' }}</b></div></div></section>
          <section class="trace-section"><h3>Generation Trace</h3><div class="generation-list"><div v-for="trace in traces" :key="trace.id"><span>{{ trace.status }}</span><p>{{ trace.errorReason || trace.outputSummary }}</p><time>{{ trace.latencyMs }}ms · {{ trace.createdAt }}</time></div></div></section>
        </section>
        <aside v-if="activeCase" class="review-side">
          <section class="panel review-decision-panel">
            <div class="panel-heading compact"><div><p class="eyebrow">HUMAN REVIEW</p><h2>人工复核</h2></div><ShieldCheck :size="18" /></div>
            <span class="review-state" data-testid="review-state" role="status" aria-live="polite" :data-state="reviewState(activeCase)">{{ reviewState(activeCase) === 'pending' ? '等待人工结论' : reviewState(activeCase) === 'approved' ? '本地标记：通过' : '本地标记：退回修订' }}</span>
            <p>{{ activeCase.reviewRequired ? '该 Case 因规则失败或 high/critical 风险进入 Review 队列。' : '该 Case 不在强制 Review 队列中，仍可进行本地抽检。' }}</p>
            <p>结论只保存在当前页面，不写入数据库，也不等同于真实上线审批。</p>
            <div class="review-actions"><button type="button" data-review-action="approved" :aria-pressed="reviewState(activeCase) === 'approved'" @click="setReview('approved')"><Check :size="15" />标记通过</button><button type="button" data-review-action="revision" :aria-pressed="reviewState(activeCase) === 'revision'" @click="setReview('revision')"><RefreshCw :size="15" />退回修订</button></div>
            <span class="sr-only" role="status" aria-live="polite">{{ reviewFeedback }}</span>
          </section>
          <section class="panel risk-panel">
            <div class="panel-heading compact"><div><p class="eyebrow">RISK & NEXT STEP</p><h2>风险与建议</h2></div><AlertTriangle :size="18" /></div>
            <span class="risk-banner" :data-risk="caseRisk(activeCase)">{{ caseRisk(activeCase) }} risk</span>
            <p>{{ activeCase.reason }}</p>
            <div class="suggestion-callout"><Sparkles :size="17" /><div><strong>Prompt 迭代建议</strong><p>{{ iterationSuggestion }}</p></div></div>
          </section>
        </aside>
      </main>

      <footer class="app-footer">
        <span>PromptOps Evaluation Lab · Local Demo</span>
        <span>Mock Output + Rule Scoring + Human Review</span>
      </footer>
    </template>

    <section v-else-if="!apiError" class="state-panel empty-state">
      <Layers3 :size="28" /><strong>暂无评测数据</strong><p>启用 Demo 数据或创建 Prompt 与评测集后再试。</p>
    </section>

    <div v-if="switchingPrompt" class="switching-indicator" aria-live="polite">正在切换 Prompt…</div>
  </div>
</template>
