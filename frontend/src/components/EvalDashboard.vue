<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  BarChart3,
  CheckCircle2,
  CircleDashed,
  Code2,
  Database,
  FileText,
  GitCompare,
  KeyRound,
  Layers3,
  ListChecks,
  Play,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  UserRound,
} from 'lucide-vue-next'
import { evalDashboardMockData } from '../data/evalDashboardMockData'
import type { PromptProfile } from '../types'

type DashboardView = 'overview' | 'run' | 'versions' | 'review'

const props = defineProps<{
  activePromptId: number | null
  prompts: PromptProfile[]
  runningEval: boolean
}>()

const emit = defineEmits<{
  openView: [view: DashboardView]
  runEval: []
  selectPrompt: [prompt: PromptProfile]
}>()

const dashboardSearch = ref('')
const data = evalDashboardMockData

const navItems = [
  { id: 'dashboard', label: '评测总览', icon: BarChart3, view: 'overview' as const },
  { id: 'prompt', label: 'Prompt 工作台', icon: Code2, view: 'versions' as const },
  { id: 'batch', label: '批量评测', icon: FileText, view: 'run' as const },
  { id: 'compare', label: '输出对比', icon: GitCompare, view: 'versions' as const },
  { id: 'trace', label: '运行追踪', icon: CircleDashed, view: 'review' as const },
  { id: 'failure', label: '失败归因', icon: AlertTriangle, view: 'run' as const },
  { id: 'dataset', label: '测试集', icon: Database, view: 'run' as const },
  { id: 'metrics', label: '指标快照', icon: SlidersHorizontal, view: 'overview' as const },
  { id: 'boundary', label: '系统边界', icon: ShieldCheck, view: 'overview' as const },
]

const experimentFields = computed(() => [
  ['Dataset', data.experiment.dataset],
  ['Prompt', data.experiment.prompt],
  ['规则集', data.experiment.ruleSet],
  ['Eval Run', data.experiment.runId],
  ['样本数', data.experiment.sampleCount],
  ['模式', data.experiment.mode],
])

const promptOptions = computed(() => props.prompts.slice(0, 3))

function chartPoints(values: number[]) {
  if (!values.length) return ''
  const step = values.length === 1 ? 0 : 100 / (values.length - 1)
  return values.map((value, index) => `${index * step},${100 - value}`).join(' ')
}
</script>

<template>
  <div class="eval-dashboard-shell">
    <aside class="eval-sidebar" aria-label="PromptOps Studio 导航">
      <div class="eval-brand">
        <button class="sidebar-menu-button" type="button" aria-label="展开导航">
          <Layers3 :size="18" />
        </button>
        <strong>PromptOps Studio</strong>
      </div>

      <nav class="eval-nav">
        <button
          v-for="item in navItems"
          :key="item.id"
          type="button"
          :aria-current="item.id === 'dashboard' ? 'page' : undefined"
          :data-view="item.view"
          @click="emit('openView', item.view)"
        >
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <section class="local-demo-card" aria-label="本地演示环境说明">
        <strong>本地演示环境</strong>
        <p><CheckCircle2 :size="14" />使用 Mock Output 与 Rule-based Eval</p>
        <p><CheckCircle2 :size="14" />无需网络与 API Key</p>
        <small>版本 0.3.0-beta</small>
        <small>非生产环境</small>
      </section>
    </aside>

    <section class="eval-workspace">
      <header class="eval-topbar">
        <label class="dashboard-search">
          <Search :size="17" />
          <span class="sr-only">搜索 Prompt、Run 或 Dataset</span>
          <input
            v-model="dashboardSearch"
            data-testid="global-search"
            type="search"
            aria-label="搜索 Prompt、Run 或 Dataset"
            placeholder="搜索 Prompt / Run / Dataset"
          />
        </label>

        <div class="status-cluster" aria-label="当前环境状态">
          <span data-tone="green">本地演示</span>
          <span data-tone="blue">规则评测</span>
          <span data-tone="purple">无需 API Key</span>
        </div>

        <div class="topbar-user">
          <span>环境：<strong>Local Demo</strong></span>
          <span class="dev-user"><UserRound :size="15" />Dev User</span>
        </div>

        <div class="topbar-actions">
          <button class="outline-action" type="button" @click="emit('openView', 'run')">
            <Database :size="16" />查看测试集
          </button>
          <button class="primary-action" type="button" :disabled="runningEval" @click="emit('runEval')">
            <Play :size="16" />{{ runningEval ? '评测运行中' : '新建评测运行' }}
          </button>
        </div>
      </header>

      <main id="main" class="eval-dashboard-main" tabindex="-1">
        <section class="dashboard-title-row">
          <div>
            <h1>PromptOps Studio</h1>
            <p class="dashboard-subtitle">AI 输出质量评测工作台</p>
            <p class="dashboard-intro">基于本地测试集、Mock Output 与 Rule-based Eval，分析 Prompt 输出质量、回归风险与失败原因。</p>
          </div>
        </section>

        <section class="dashboard-grid">
          <section class="experiment-card panel-card">
            <div class="section-heading">
              <h2>当前评测实验</h2>
              <span>{{ data.experiment.runId }}</span>
            </div>
            <div class="experiment-fields">
              <div v-for="[label, value] in experimentFields" :key="label">
                <span>{{ label }}</span>
                <strong>{{ value }}</strong>
              </div>
            </div>
            <div v-if="promptOptions.length" class="prompt-switch-row" aria-label="Prompt 快速切换">
              <button
                v-for="prompt in promptOptions"
                :key="prompt.id"
                type="button"
                :data-prompt-id="prompt.id"
                :aria-pressed="prompt.id === activePromptId"
                @click="emit('selectPrompt', prompt)"
              >
                {{ prompt.currentVersion }} · {{ prompt.name }}
              </button>
            </div>
            <p class="mock-boundary">本页指标为本地演示数据，围绕 run_1022 展示 Mock Output 与 Rule-based Eval 闭环。</p>
          </section>

          <section class="metrics-block">
            <div class="core-metrics">
              <article
                v-for="metric in data.coreMetrics"
                :key="metric.label"
                class="dashboard-metric"
                :data-tone="metric.tone"
              >
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
                <p>{{ metric.helper }} <b>{{ metric.delta }}</b></p>
                <div class="mini-trend" aria-hidden="true">
                  <i v-for="(point, index) in metric.trend" :key="index" :style="{ height: `${point}%` }"></i>
                </div>
              </article>
            </div>

            <aside class="support-metrics panel-card">
              <div v-for="item in data.supportMetrics" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <small>{{ item.helper }}</small>
              </div>
            </aside>
          </section>

          <section class="health-card panel-card">
            <div class="section-heading">
              <h2>评测健康度</h2>
              <div class="chart-legend">
                <span><i data-color="blue"></i>Avg Score</span>
                <span><i data-color="green"></i>Pass Rate</span>
              </div>
            </div>
            <div class="trend-chart">
              <svg viewBox="0 0 100 100" preserveAspectRatio="none" role="img" aria-label="最近 8 次评测运行趋势">
                <line x1="0" y1="25" x2="100" y2="25" />
                <line x1="0" y1="50" x2="100" y2="50" />
                <line x1="0" y1="75" x2="100" y2="75" />
                <polyline :points="chartPoints(data.healthTrend.avgScore)" data-line="blue" />
                <polyline :points="chartPoints(data.healthTrend.passRate)" data-line="green" />
              </svg>
              <div class="chart-axis">
                <span v-for="label in data.healthTrend.labels" :key="label">{{ label }}</span>
              </div>
            </div>
          </section>

          <section class="trace-master-panel">
            <div class="trace-header">
              <div>
                <h2>运行 Trace</h2>
                <span>{{ data.trace.runId }}</span>
              </div>
              <div>
                <strong>{{ data.trace.status }}</strong>
                <time>{{ data.trace.completedAt }}</time>
              </div>
            </div>
            <div class="trace-timeline">
              <article
                v-for="node in data.trace.nodes"
                :key="node.id"
                class="trace-node"
                :data-tone="node.tone ?? 'normal'"
              >
                <span class="trace-marker"><Activity :size="14" /></span>
                <time>{{ node.time }}</time>
                <div>
                  <strong>{{ node.label }}</strong>
                  <small>{{ node.status }}</small>
                  <p>{{ node.summary }}</p>
                  <div v-if="node.id === 'score'" class="score-dimensions">
                    <span v-for="[label, value] in data.trace.scoreDimensions" :key="label">{{ label }} <b>{{ value }}</b></span>
                    <mark>总分 0.86 / 1.00</mark>
                  </div>
                </div>
                <button type="button">{{ node.action }}</button>
              </article>
            </div>
          </section>

          <section class="recent-runs-card panel-card">
            <div class="section-heading">
              <h2>最近评测运行</h2>
              <span>当前选中 {{ data.experiment.runId }}</span>
            </div>
            <div class="run-table">
              <div class="run-table-head">
                <span>运行 ID</span><span>测试集</span><span>Prompt 版本</span><span>样本数</span><span>通过率</span><span>质量分</span><span>状态</span><span>耗时</span>
              </div>
              <div
                v-for="run in data.recentRuns.slice(0, 5)"
                :key="run[0]"
                class="run-table-row"
                :data-selected="run[0] === data.experiment.runId"
              >
                <span>{{ run[0] }}</span><span>{{ run[1] }}</span><span>{{ run[2] }}</span><span>{{ run[3] }}</span><span>{{ run[4] }}</span><span>{{ run[5] }}</span><span>{{ run[6] }}</span><span>{{ run[7] }}</span>
              </div>
            </div>
          </section>

          <section class="failure-card panel-card">
            <div class="section-heading">
              <h2>回归风险与失败归因</h2>
              <span>关联 Trace：{{ data.failureAttribution.trace }}</span>
            </div>
            <div class="failure-layout">
              <div class="failure-ranking">
                <h3>失败原因排行</h3>
                <div v-for="(item, index) in data.failureAttribution.ranking" :key="item[0]">
                  <span>{{ index + 1 }}</span>
                  <p>{{ item[0] }}</p>
                  <b>{{ item[1] }} ({{ item[2] }})</b>
                </div>
              </div>
              <div class="risk-diagnosis">
                <span>当前主要风险</span>
                <strong>{{ data.failureAttribution.mainRisk }}</strong>
                <p>影响评分 <b>{{ data.failureAttribution.impact }}</b></p>
                <p>修复建议：{{ data.failureAttribution.suggestion }}</p>
                <a href="#main">run_1022</a>
              </div>
              <div class="schema-compare">
                <h3>Schema 对比</h3>
                <div>
                  <pre>{
  "answer": "...",
  "confidence": "..."
}</pre>
                  <pre>{
  "answer": "...",
  "confidence": null
}</pre>
                </div>
                <p>检测到 1 处关键字段缺失</p>
              </div>
            </div>
          </section>

          <aside class="provider-card panel-card">
            <div class="section-heading">
              <h2>Provider 状态与本地规则</h2>
            </div>
            <div class="provider-list">
              <div v-for="[name, state, tone] in data.providerStatus" :key="name">
                <span><KeyRound :size="14" />{{ name }}</span>
                <b :data-tone="tone">{{ state }}</b>
              </div>
            </div>
            <p class="provider-note">本地演示环境，使用 Mock Output 与 Rule-based Eval，非生产环境。</p>
          </aside>

          <section class="loop-card panel-card">
            <div class="section-heading">
              <h2>PromptOps 评测闭环</h2>
            </div>
            <div class="eval-loop">
              <button v-for="(item, index) in data.loop" :key="item[0]" type="button">
                <span>{{ item[0] }}</span>
                <strong>{{ item[1] }}</strong>
                <ArrowRight v-if="index < data.loop.length - 1" :size="16" />
              </button>
            </div>
          </section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.eval-dashboard-shell {
  display: grid;
  grid-template-columns: 216px minmax(0, 1fr);
  min-height: 100vh;
  color: #111827;
  background: #f6f8fb;
}

.eval-sidebar {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 100vh;
  border-right: 1px solid #d9e2ec;
  background: #fbfcfe;
}

.eval-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 52px;
  padding: 0 18px;
  border-bottom: 1px solid #d9e2ec;
}

.sidebar-menu-button {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  color: #2563eb;
  background: transparent;
}

.eval-brand strong {
  font-size: 16px;
  font-weight: 760;
  white-space: nowrap;
}

.eval-nav {
  display: grid;
  align-content: start;
  gap: 4px;
  padding: 22px 12px;
}

.eval-nav button {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 44px;
  padding: 0 14px;
  border: 0;
  border-radius: 8px;
  color: #334155;
  background: transparent;
  font-size: 13px;
  font-weight: 640;
  cursor: pointer;
}

.eval-nav button[aria-current="page"] {
  color: #1d4ed8;
  background: #eef4ff;
}

.eval-nav button[aria-current="page"]::before {
  position: absolute;
  left: 0;
  width: 3px;
  height: 26px;
  border-radius: 999px;
  background: #2563eb;
  content: "";
}

.local-demo-card {
  margin: 14px;
  padding: 16px;
  border: 1px solid #d6e2ef;
  border-radius: 10px;
  background: #f8fbff;
}

.local-demo-card strong {
  display: block;
  margin-bottom: 12px;
  color: #1d4ed8;
  font-size: 13px;
}

.local-demo-card p {
  display: flex;
  gap: 6px;
  margin: 0 0 8px;
  color: #334155;
  font-size: 12px;
  line-height: 1.55;
}

.local-demo-card svg {
  flex: 0 0 auto;
  color: #16a34a;
}

.local-demo-card small {
  display: block;
  margin-top: 12px;
  color: #64748b;
  font-size: 11px;
}

.eval-workspace {
  min-width: 0;
}

.eval-topbar {
  display: grid;
  grid-template-columns: minmax(260px, 500px) auto auto auto;
  align-items: center;
  gap: 18px;
  min-height: 52px;
  padding: 0 24px;
  border-bottom: 1px solid #d9e2ec;
  background: rgba(255, 255, 255, 0.92);
}

.dashboard-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #d8e0ea;
  border-radius: 8px;
  color: #64748b;
  background: #f8fafc;
}

.dashboard-search input {
  width: 100%;
  border: 0;
  outline: 0;
  color: #0f172a;
  background: transparent;
  font-size: 12px;
}

.status-cluster,
.topbar-user,
.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-cluster span {
  min-height: 30px;
  padding: 6px 11px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 720;
}

.status-cluster span[data-tone="green"] {
  color: #047857;
  background: #dcfce7;
}

.status-cluster span[data-tone="blue"] {
  color: #1d4ed8;
  background: #eaf1ff;
}

.status-cluster span[data-tone="purple"] {
  color: #6d28d9;
  background: #f0e8ff;
}

.topbar-user {
  color: #475569;
  font-size: 12px;
  white-space: nowrap;
}

.dev-user {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #0f172a;
  font-weight: 650;
}

.outline-action,
.primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 720;
  cursor: pointer;
}

.outline-action {
  border: 1px solid #cbd5e1;
  color: #0f172a;
  background: #ffffff;
}

.primary-action {
  border: 1px solid #1d4ed8;
  color: #ffffff;
  background: #2563eb;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.14);
}

.eval-dashboard-main {
  padding: 22px 24px 24px;
}

.dashboard-title-row h1 {
  margin: 0 0 4px;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.12;
}

.dashboard-subtitle {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 18px;
  font-weight: 650;
}

.dashboard-intro {
  max-width: 720px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.65;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 0.96fr);
  gap: 16px;
  margin-top: 14px;
}

.panel-card {
  min-width: 0;
  border: 1px solid #d9e2ec;
  border-radius: 10px;
  background: #ffffff;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 15px 16px 12px;
}

.section-heading h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 760;
}

.section-heading span {
  color: #64748b;
  font-size: 12px;
}

.experiment-card {
  grid-column: 1;
}

.experiment-fields {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 1px;
  margin: 0 16px 10px;
  overflow: hidden;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #d9e2ec;
}

.experiment-fields div {
  display: grid;
  gap: 5px;
  min-height: 58px;
  padding: 10px;
  background: #f8fafc;
}

.experiment-fields span {
  color: #64748b;
  font-size: 11px;
}

.experiment-fields strong {
  overflow: hidden;
  color: #0f172a;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prompt-switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 16px 10px;
}

.prompt-switch-row button {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  color: #475569;
  background: #ffffff;
  font-size: 11px;
  cursor: pointer;
}

.prompt-switch-row button[aria-pressed="true"] {
  color: #1d4ed8;
  border-color: #bfdbfe;
  background: #eff6ff;
}

.mock-boundary {
  margin: 0 16px 16px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

.metrics-block {
  display: grid;
  gap: 10px;
  grid-column: 1;
}

.core-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.dashboard-metric {
  display: grid;
  min-height: 152px;
  padding: 17px;
  border: 1px solid #d9e2ec;
  border-radius: 10px;
  background: #ffffff;
}

.dashboard-metric span {
  color: #334155;
  font-size: 13px;
  font-weight: 720;
}

.dashboard-metric strong {
  margin-top: 8px;
  color: #0f172a;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 25px;
  line-height: 1.1;
  white-space: nowrap;
}

.dashboard-metric p {
  margin: 8px 0 10px;
  color: #64748b;
  font-size: 12px;
}

.dashboard-metric b {
  color: #16a34a;
}

.dashboard-metric[data-tone="red"] b {
  color: #ef4444;
}

.mini-trend {
  display: flex;
  align-items: end;
  gap: 5px;
  height: 32px;
  margin-top: auto;
}

.mini-trend i {
  flex: 1;
  min-width: 4px;
  border-radius: 999px 999px 0 0;
  background: #2563eb;
}

.dashboard-metric[data-tone="green"] .mini-trend i {
  background: #16a34a;
}

.dashboard-metric[data-tone="red"] .mini-trend i {
  background: #ef4444;
}

.support-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.support-metrics div {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 10px;
  padding: 14px 16px;
  border-left: 1px solid #e2e8f0;
}

.support-metrics div:first-child {
  border-left: 0;
}

.support-metrics span,
.support-metrics small {
  color: #64748b;
  font-size: 12px;
}

.support-metrics strong {
  grid-row: span 2;
  color: #0f172a;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 20px;
}

.health-card,
.recent-runs-card,
.failure-card,
.loop-card {
  grid-column: 1;
}

.chart-legend {
  display: flex;
  gap: 14px;
}

.chart-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.chart-legend i {
  width: 18px;
  height: 3px;
  border-radius: 999px;
  background: #2563eb;
}

.chart-legend i[data-color="green"] {
  background: #16a34a;
}

.trend-chart {
  padding: 0 16px 16px;
}

.trend-chart svg {
  width: 100%;
  height: 138px;
  overflow: visible;
}

.trend-chart line {
  stroke: #e2e8f0;
  stroke-width: 0.6;
}

.trend-chart polyline {
  fill: none;
  stroke-width: 2.2;
  vector-effect: non-scaling-stroke;
}

.trend-chart polyline[data-line="blue"] {
  stroke: #2563eb;
}

.trend-chart polyline[data-line="green"] {
  stroke: #16a34a;
}

.chart-axis {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  color: #64748b;
  font-size: 11px;
  text-align: center;
}

.trace-master-panel {
  grid-column: 2;
  grid-row: 1 / span 4;
  overflow: hidden;
  border: 1px solid #23364f;
  border-radius: 10px;
  color: #e5eefb;
  background: #0f1f33;
  box-shadow: 0 18px 36px rgba(15, 31, 51, 0.22);
}

.trace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px 12px;
}

.trace-header h2 {
  margin: 0 0 4px;
  font-size: 15px;
}

.trace-header span,
.trace-header time {
  color: #94a3b8;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
}

.trace-header strong {
  display: inline-block;
  margin-right: 12px;
  padding: 4px 10px;
  border-radius: 7px;
  color: #86efac;
  background: rgba(22, 163, 74, 0.16);
  font-size: 11px;
}

.trace-timeline {
  position: relative;
  display: grid;
  gap: 10px;
  padding: 0 16px 16px 46px;
}

.trace-timeline::before {
  position: absolute;
  top: 8px;
  bottom: 24px;
  left: 29px;
  width: 1px;
  background: #49647f;
  content: "";
}

.trace-node {
  position: relative;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
  min-height: 50px;
  padding: 10px;
  border: 1px solid #324965;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.trace-marker {
  position: absolute;
  left: -34px;
  top: 10px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 1px solid #7890aa;
  border-radius: 50%;
  color: #bfdbfe;
  background: #122944;
}

.trace-node time {
  color: #9fb3ca;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
}

.trace-node strong {
  display: inline-block;
  margin-right: 8px;
  color: #f8fafc;
  font-size: 12px;
}

.trace-node small {
  color: #9fb3ca;
  font-size: 11px;
}

.trace-node p {
  margin: 6px 0 0;
  color: #cbd5e1;
  font-size: 12px;
  line-height: 1.5;
}

.trace-node button {
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid #415b78;
  border-radius: 6px;
  color: #dbeafe;
  background: #172b46;
  font-size: 11px;
  cursor: pointer;
}

.trace-node[data-tone="danger"] {
  border-color: rgba(239, 68, 68, 0.6);
  background: rgba(127, 29, 29, 0.28);
}

.trace-node[data-tone="danger"] strong,
.trace-node[data-tone="danger"] p {
  color: #fecaca;
}

.trace-node[data-tone="warning"] {
  border-color: rgba(251, 191, 36, 0.34);
}

.score-dimensions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.score-dimensions span,
.score-dimensions mark {
  padding: 4px 7px;
  border: 1px solid #415b78;
  border-radius: 6px;
  color: #dbeafe;
  background: #172b46;
  font-size: 11px;
}

.score-dimensions mark {
  color: #ffffff;
  background: transparent;
}

.run-table {
  padding: 0 16px 16px;
}

.run-table-head,
.run-table-row {
  display: grid;
  grid-template-columns: 0.72fr 1fr 0.74fr 0.58fr 0.62fr 0.62fr 0.66fr 0.55fr;
  align-items: center;
  min-height: 42px;
  gap: 10px;
  padding: 0 10px;
  border-bottom: 1px solid #e2e8f0;
  font-size: 12px;
}

.run-table-head {
  min-height: 38px;
  color: #64748b;
  background: #f8fafc;
  font-size: 11px;
  font-weight: 720;
}

.run-table-row[data-selected="true"] {
  color: #1d4ed8;
  background: #eff6ff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.failure-layout {
  display: grid;
  grid-template-columns: 1fr 0.8fr 1.1fr;
  gap: 14px;
  padding: 0 16px 16px;
}

.failure-ranking h3,
.schema-compare h3 {
  margin: 0 0 10px;
  font-size: 12px;
}

.failure-ranking div {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  gap: 9px;
  align-items: center;
  min-height: 28px;
  font-size: 12px;
}

.failure-ranking span {
  color: #64748b;
  font-family: "Cascadia Code", Consolas, monospace;
}

.failure-ranking p {
  margin: 0;
}

.failure-ranking b {
  color: #64748b;
  font-size: 11px;
}

.risk-diagnosis {
  padding: 14px;
  border-left: 2px solid #ef4444;
  background: #fff7f7;
}

.risk-diagnosis span {
  color: #64748b;
  font-size: 11px;
}

.risk-diagnosis strong {
  display: block;
  margin: 6px 0 8px;
  color: #dc2626;
  font-size: 18px;
}

.risk-diagnosis p,
.risk-diagnosis a,
.schema-compare p {
  margin: 0 0 8px;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
}

.risk-diagnosis a {
  color: #2563eb;
  text-decoration: none;
}

.schema-compare > div {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.schema-compare pre {
  min-height: 112px;
  margin: 0;
  padding: 10px;
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #334155;
  background: #f8fafc;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
  line-height: 1.45;
}

.schema-compare p {
  margin-top: 8px;
  color: #ef4444;
}

.provider-card {
  grid-column: 2;
}

.provider-list {
  display: grid;
  gap: 0;
  padding: 0 16px 12px;
}

.provider-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  border-top: 1px solid #e2e8f0;
  font-size: 12px;
}

.provider-list span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #334155;
}

.provider-list b {
  color: #64748b;
  font-size: 12px;
}

.provider-list b[data-tone="green"] {
  color: #16a34a;
}

.provider-list b[data-tone="blue"] {
  color: #2563eb;
}

.provider-note {
  margin: 0 16px 16px;
  padding: 12px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  color: #1e40af;
  background: #eff6ff;
  font-size: 12px;
  line-height: 1.55;
}

.eval-loop {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  padding: 0 16px 16px;
}

.eval-loop button {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 5px;
  min-height: 58px;
  padding: 8px 6px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  color: #0f172a;
  background: #ffffff;
  font-size: 11px;
  cursor: pointer;
}

.eval-loop span {
  color: #334155;
  font-family: "Cascadia Code", Consolas, monospace;
}

.eval-loop strong {
  font-size: 12px;
}

.eval-loop svg {
  position: absolute;
  right: -13px;
  top: 20px;
  color: #64748b;
  z-index: 1;
}

@media (min-width: 1181px) and (max-height: 900px) {
  .eval-dashboard-shell {
    grid-template-columns: 190px minmax(0, 1fr);
    min-height: 100vh;
  }

  .eval-brand {
    height: 50px;
    padding: 0 12px;
  }

  .eval-brand strong {
    font-size: 14px;
  }

  .eval-nav {
    gap: 2px;
    padding: 10px 8px;
  }

  .eval-nav button {
    min-height: 34px;
    gap: 8px;
    padding: 0 10px;
    font-size: 12px;
  }

  .eval-nav button[aria-current="page"]::before {
    height: 22px;
  }

  .local-demo-card {
    margin: 8px;
    padding: 10px;
  }

  .local-demo-card strong {
    margin-bottom: 6px;
    font-size: 11px;
  }

  .local-demo-card p {
    margin-bottom: 4px;
    font-size: 10px;
    line-height: 1.35;
  }

  .local-demo-card small {
    display: inline-block;
    margin: 6px 8px 0 0;
    font-size: 9px;
  }

  .eval-topbar {
    grid-template-columns: minmax(250px, 1fr) auto auto auto;
    gap: 10px;
    min-height: 50px;
    padding: 0 16px;
  }

  .dashboard-search {
    min-height: 32px;
  }

  .status-cluster,
  .topbar-user,
  .topbar-actions {
    gap: 7px;
  }

  .status-cluster span {
    min-height: 26px;
    padding: 4px 8px;
    font-size: 10px;
  }

  .topbar-user {
    font-size: 10px;
  }

  .outline-action,
  .primary-action {
    min-height: 34px;
    padding: 0 11px;
    font-size: 11px;
  }

  .eval-dashboard-main {
    padding: 10px 16px 12px;
  }

  .dashboard-title-row > div {
    display: grid;
    grid-template-columns: auto auto minmax(0, 1fr);
    align-items: center;
    gap: 12px;
    min-height: 40px;
  }

  .dashboard-title-row h1 {
    margin: 0;
    font-size: 24px;
  }

  .dashboard-subtitle {
    margin: 0;
    font-size: 14px;
  }

  .dashboard-intro {
    overflow: hidden;
    font-size: 10px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .dashboard-grid {
    grid-template-areas:
      "experiment trace"
      "metrics trace"
      "health trace"
      "recent failure"
      "recent provider"
      "loop loop";
    grid-template-columns: minmax(0, 58fr) minmax(0, 42fr);
    grid-template-rows: 66px 126px 118px 150px 78px 58px;
    gap: 8px;
    margin-top: 8px;
  }

  .experiment-card {
    grid-area: experiment;
    overflow: hidden;
  }

  .metrics-block {
    grid-area: metrics;
    gap: 6px;
  }

  .health-card {
    grid-area: health;
    overflow: hidden;
  }

  .trace-master-panel {
    grid-area: trace;
  }

  .recent-runs-card {
    grid-area: recent;
    overflow: hidden;
  }

  .failure-card {
    grid-area: failure;
    overflow: hidden;
  }

  .provider-card {
    grid-area: provider;
    overflow: hidden;
  }

  .loop-card {
    grid-area: loop;
    display: grid;
    grid-template-columns: 150px minmax(0, 1fr);
    align-items: center;
    overflow: hidden;
  }

  .section-heading {
    min-height: 26px;
    padding: 6px 10px;
  }

  .section-heading h2 {
    font-size: 12px;
  }

  .section-heading span {
    font-size: 9px;
  }

  .experiment-fields {
    margin: 0 8px;
    border-radius: 6px;
  }

  .experiment-fields div {
    align-content: center;
    gap: 2px;
    min-height: 32px;
    padding: 3px 6px;
  }

  .experiment-fields span {
    font-size: 8px;
  }

  .experiment-fields strong {
    font-size: 9px;
  }

  .prompt-switch-row,
  .mock-boundary {
    display: none;
  }

  .core-metrics {
    gap: 6px;
  }

  .dashboard-metric {
    min-height: 85px;
    padding: 8px 10px;
  }

  .dashboard-metric span {
    font-size: 10px;
  }

  .dashboard-metric strong {
    margin-top: 4px;
    font-size: 20px;
  }

  .dashboard-metric p {
    margin: 3px 0 4px;
    font-size: 9px;
  }

  .mini-trend {
    gap: 3px;
    height: 17px;
  }

  .support-metrics div {
    align-items: center;
    min-height: 35px;
    padding: 4px 9px;
  }

  .support-metrics span {
    font-size: 9px;
  }

  .support-metrics strong {
    grid-row: auto;
    font-size: 15px;
  }

  .support-metrics small {
    grid-column: 1 / -1;
    font-size: 8px;
  }

  .chart-legend {
    gap: 8px;
  }

  .chart-legend span {
    font-size: 8px;
  }

  .chart-legend i {
    width: 12px;
  }

  .trend-chart {
    padding: 0 10px 6px;
  }

  .trend-chart svg {
    height: 68px;
  }

  .chart-axis {
    font-size: 8px;
  }

  .trace-header {
    min-height: 34px;
    padding: 7px 10px 5px;
  }

  .trace-header h2 {
    margin-bottom: 1px;
    font-size: 12px;
  }

  .trace-header span,
  .trace-header time,
  .trace-header strong {
    font-size: 8px;
  }

  .trace-header strong {
    margin-right: 6px;
    padding: 3px 6px;
  }

  .trace-timeline {
    gap: 4px;
    padding: 0 8px 8px 35px;
  }

  .trace-timeline::before {
    top: 5px;
    bottom: 13px;
    left: 21px;
  }

  .trace-node {
    grid-template-columns: 45px minmax(0, 1fr) auto;
    gap: 6px;
    align-items: center;
    min-height: 29px;
    padding: 3px 6px;
    border-radius: 6px;
  }

  .trace-marker {
    left: -25px;
    top: 50%;
    width: 18px;
    height: 18px;
    transform: translateY(-50%);
  }

  .trace-marker svg {
    width: 10px;
    height: 10px;
  }

  .trace-node time {
    font-size: 8px;
  }

  .trace-node strong {
    margin-right: 4px;
    font-size: 10px;
  }

  .trace-node small {
    font-size: 8px;
  }

  .trace-node > div {
    display: grid;
    grid-template-columns: auto auto minmax(0, 1fr);
    align-items: center;
    gap: 4px;
    min-width: 0;
  }

  .trace-node p {
    overflow: hidden;
    margin: 0;
    font-size: 9px;
    line-height: 1.25;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .trace-node button {
    min-height: 22px;
    padding: 0 7px;
    font-size: 8px;
  }

  .score-dimensions {
    grid-column: 1 / -1;
    flex-wrap: nowrap;
    gap: 3px;
    margin-top: 1px;
    overflow: hidden;
  }

  .score-dimensions span,
  .score-dimensions mark {
    padding: 2px 4px;
    font-size: 8px;
    white-space: nowrap;
  }

  .run-table {
    padding: 0 10px 6px;
  }

  .run-table-head,
  .run-table-row {
    grid-template-columns: 0.72fr 1fr 0.7fr 0.52fr 0.58fr 0.55fr 0.62fr 0.5fr;
    gap: 5px;
    min-height: 20px;
    padding: 0 6px;
    font-size: 8px;
  }

  .run-table-head {
    min-height: 20px;
    font-size: 7px;
  }

  .failure-layout {
    grid-template-columns: 0.9fr 0.9fr 1.1fr;
    gap: 6px;
    padding: 0 8px 7px;
  }

  .failure-ranking h3,
  .schema-compare h3 {
    margin-bottom: 4px;
    font-size: 9px;
  }

  .failure-ranking div {
    grid-template-columns: 12px minmax(0, 1fr) auto;
    gap: 4px;
    min-height: 18px;
    font-size: 8px;
  }

  .failure-ranking > div:nth-of-type(n + 4) {
    display: none;
  }

  .failure-ranking b {
    font-size: 7px;
  }

  .risk-diagnosis {
    padding: 7px;
  }

  .risk-diagnosis span,
  .risk-diagnosis p,
  .risk-diagnosis a,
  .schema-compare p {
    margin-bottom: 3px;
    font-size: 8px;
    line-height: 1.3;
  }

  .risk-diagnosis strong {
    margin: 3px 0 4px;
    font-size: 13px;
  }

  .schema-compare > div {
    gap: 4px;
  }

  .schema-compare pre {
    min-height: 0;
    height: 58px;
    padding: 5px;
    font-size: 7px;
    line-height: 1.25;
  }

  .schema-compare p {
    margin-top: 3px;
  }

  .provider-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    column-gap: 10px;
    padding: 0 10px 5px;
  }

  .provider-list div {
    min-height: 20px;
    font-size: 8px;
  }

  .provider-list b {
    font-size: 8px;
  }

  .provider-note {
    display: none;
  }

  .loop-card .section-heading {
    height: 100%;
    border-right: 1px solid #e2e8f0;
  }

  .eval-loop {
    gap: 6px;
    padding: 6px 10px;
  }

  .eval-loop button {
    gap: 1px;
    min-height: 36px;
    padding: 3px;
  }

  .eval-loop span {
    font-size: 7px;
  }

  .eval-loop strong {
    font-size: 9px;
  }

  .eval-loop svg {
    right: -11px;
    top: 11px;
    width: 13px;
    height: 13px;
  }
}

@media (max-width: 1180px) {
  .eval-topbar {
    grid-template-columns: minmax(220px, 1fr);
    padding: 12px 18px;
  }

  .status-cluster,
  .topbar-user,
  .topbar-actions {
    flex-wrap: wrap;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .trace-master-panel,
  .provider-card {
    grid-column: 1;
    grid-row: auto;
  }
}

@media (max-width: 760px) {
  .eval-dashboard-shell {
    grid-template-columns: 1fr;
  }

  .eval-sidebar {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid #d9e2ec;
  }

  .eval-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 12px;
  }

  .local-demo-card {
    display: none;
  }

  .eval-dashboard-main {
    padding: 18px 14px;
  }

  .experiment-fields,
  .core-metrics,
  .support-metrics,
  .failure-layout,
  .schema-compare > div,
  .eval-loop {
    grid-template-columns: 1fr;
  }

  .run-table {
    overflow-x: auto;
  }

  .run-table-head,
  .run-table-row {
    min-width: 820px;
  }

  .trace-node {
    grid-template-columns: 1fr;
  }
}
</style>
