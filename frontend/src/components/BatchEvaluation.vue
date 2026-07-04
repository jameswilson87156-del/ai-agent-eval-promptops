<script setup lang="ts">
import { ref } from 'vue'
import {
  AlertTriangle,
  ArrowRight,
  BarChart3,
  CheckCircle2,
  CircleDashed,
  Clock3,
  Database,
  FileText,
  GitCompare,
  LayoutGrid,
  Layers3,
  LoaderCircle,
  Play,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  UserRound,
  XCircle,
} from 'lucide-vue-next'
import { batchEvaluationMockData } from '../data/batchEvaluationMockData'

type ViewKey = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary'

defineProps<{ runningEval?: boolean }>()

const emit = defineEmits<{
  (event: 'open-view', view: ViewKey): void
  (event: 'run-eval'): void
}>()

const data = batchEvaluationMockData
const selectedCaseId = ref('case_093')
const batchSearch = ref('')

const navItems = [
  { label: '评测总览', icon: BarChart3, view: 'overview' as const },
  { label: '批量评测', icon: FileText, view: 'batch' as const },
  { label: '输出对比', icon: GitCompare, view: 'compare' as const },
  { label: '运行追踪', icon: CircleDashed, view: 'trace' as const },
  { label: '失败归因', icon: AlertTriangle, view: 'failure' as const },
  { label: '测试集', icon: Database, view: 'dataset' as const },
  { label: '指标快照', icon: SlidersHorizontal, view: 'overview' as const },
  { label: '系统边界', icon: ShieldCheck, view: 'boundary' as const },
]

const workflowIcons = [Database, FileText, ShieldCheck, Play, LayoutGrid, AlertTriangle, UserRound]

function openView(view: ViewKey) {
  emit('open-view', view)
}
</script>

<template>
  <section class="batch-page-shell">
    <aside class="batch-sidebar">
      <div class="batch-brand">
        <button type="button" aria-label="PromptOps Studio">
          <Layers3 :size="18" />
        </button>
        <strong>PromptOps Studio</strong>
      </div>

      <nav aria-label="Eval navigation">
        <button
          v-for="item in navItems"
          :key="item.label"
          type="button"
          :class="{ active: item.view === 'batch' }"
          :aria-current="item.view === 'batch' ? 'page' : undefined"
          @click="openView(item.view)"
        >
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="batch-sidebar-note">
        <span>LOCAL DEMO</span>
        <strong>Rule-based Eval</strong>
        <small>Mock Output · 非生产环境</small>
      </div>
    </aside>

    <section class="batch-workspace">
      <header class="batch-topbar">
        <label class="batch-search">
          <Search :size="17" />
          <span class="sr-only">搜索 Dataset、Case 或标签</span>
          <input v-model="batchSearch" type="search" placeholder="搜索 Dataset / Case / 标签" />
        </label>

        <div class="batch-status" aria-label="Demo status">
          <span data-tone="green">本地演示</span>
          <span data-tone="blue">规则评测</span>
          <span data-tone="purple">无需 API Key</span>
        </div>

        <div class="batch-user">
          <span>环境：<strong>Local Demo</strong></span>
          <span><UserRound :size="15" />Dev User</span>
        </div>
      </header>

      <main class="batch-main">
        <section class="batch-title-row">
          <div>
            <h1>{{ data.page.title }}</h1>
            <p>{{ data.page.subtitle }}</p>
          </div>
          <div class="batch-action-buttons">
            <button type="button" class="ghost" @click="openView('overview')">回到总览</button>
            <button type="button" class="primary" :disabled="runningEval" @click="emit('run-eval')">
              <Play :size="15" />
              {{ runningEval ? '评测中' : '启动评测' }}
            </button>
          </div>
        </section>

        <section class="batch-layout">
          <div class="batch-left">
            <section class="batch-config-card">
              <div class="section-heading">
                <h2>评测配置</h2>
                <span>Local Demo / Mock Output</span>
              </div>
              <div class="config-grid">
                <article v-for="item in data.configuration" :key="item.label">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </article>
              </div>
              <ol class="configuration-steps">
                <li v-for="(step, index) in data.configurationSteps" :key="step">
                  <b>{{ index + 1 }}</b>
                  <span>{{ step }}</span>
                  <ArrowRight v-if="index < data.configurationSteps.length - 1" :size="13" />
                </li>
              </ol>
            </section>

            <section class="batch-metrics-card">
              <div class="section-heading">
                <h2>核心指标 <small>（已完成样本）</small></h2>
              </div>
              <div class="metric-grid">
                <article v-for="metric in data.metrics" :key="metric.label" :class="`tone-${metric.tone}`">
                  <span>{{ metric.label }}</span>
                  <strong>{{ metric.value }}</strong>
                  <em>{{ metric.helper }}</em>
                  <i :style="{ width: `${metric.progress}%` }" />
                </article>
              </div>
            </section>

            <section class="batch-matrix-card">
              <div class="section-heading matrix-heading">
                <h2>样本评分矩阵 <small>（部分）</small></h2>
                <span>当前选择 {{ selectedCaseId }}</span>
              </div>
              <div class="batch-matrix" role="table" aria-label="Batch score matrix">
                <div class="matrix-header" role="row">
                  <span role="columnheader">Case ID</span>
                  <span role="columnheader">输入类型</span>
                  <span role="columnheader">格式</span>
                  <span role="columnheader">完整性</span>
                  <span role="columnheader">相关性</span>
                  <span role="columnheader">合规性</span>
                  <span role="columnheader">稳定性</span>
                  <span role="columnheader">综合评分</span>
                  <span role="columnheader">结果</span>
                </div>
                <button
                  v-for="(row, index) in data.matrix"
                  :key="row.caseId"
                  type="button"
                  role="row"
                  :class="{ selected: row.caseId === selectedCaseId, 'matrix-overflow-row': index > 4 }"
                  @click="selectedCaseId = row.caseId"
                >
                  <span role="cell">{{ row.caseId }}</span>
                  <span role="cell">{{ row.inputType }}</span>
                  <span role="cell">{{ row.format }}</span>
                  <span role="cell">{{ row.completeness }}</span>
                  <span role="cell">{{ row.relevance }}</span>
                  <span role="cell">{{ row.compliance }}</span>
                  <span role="cell">{{ row.stability }}</span>
                  <strong role="cell">{{ row.score }}</strong>
                  <b role="cell" :class="row.result === '通过' ? 'ok' : 'fail'">{{ row.result }}</b>
                </button>
              </div>
            </section>

            <section class="batch-loop-card">
              <div class="section-heading">
                <h2>评测工作流</h2>
                <span>Dataset → Review</span>
              </div>
              <div class="loop-track">
                <article v-for="(step, index) in data.loop" :key="step[0]">
                  <div><component :is="workflowIcons[index]" :size="19" /></div>
                  <strong>{{ step[0] }}</strong>
                  <span>{{ step[1] }}</span>
                  <ArrowRight v-if="index < data.loop.length - 1" :size="14" />
                </article>
              </div>
            </section>
          </div>

          <aside class="batch-right">
            <section class="batch-run-card">
              <div class="section-heading">
                <h2>运行状态</h2>
                <strong>{{ data.run.id }}</strong>
              </div>
              <div class="run-state-body">
                <div class="run-progress" :style="{ '--progress': `${data.run.progress}%` }">
                  <div>
                    <strong>{{ data.run.progress }}%</strong>
                    <span>{{ data.run.completed }} / {{ data.run.sampleCount }} 完成</span>
                  </div>
                </div>
                <dl>
                  <div><dt><LoaderCircle :size="13" />状态</dt><dd class="running">{{ data.run.status }}</dd></div>
                  <div><dt><CheckCircle2 :size="13" />已完成</dt><dd>{{ data.run.completed }}</dd></div>
                  <div><dt><Clock3 :size="13" />处理中</dt><dd>{{ data.run.processing }}</dd></div>
                  <div><dt><XCircle :size="13" />失败样本</dt><dd>{{ data.run.failed }}</dd></div>
                  <div><dt>开始时间</dt><dd>{{ data.run.startedAt }}</dd></div>
                  <div><dt>预计完成</dt><dd>{{ data.run.estimatedFinish }}</dd></div>
                </dl>
              </div>
            </section>

            <section class="batch-timeline-card">
              <div class="section-heading">
                <h2>执行时间线</h2>
              </div>
              <div class="batch-timeline">
                <article
                  v-for="(item, index) in data.timeline"
                  :key="item.label"
                  :class="{ active: item.state === '运行中', 'timeline-overflow-step': index > 4 }"
                >
                  <time>{{ item.time }}</time>
                  <i><CheckCircle2 v-if="item.state === '已完成'" :size="13" /><LoaderCircle v-else :size="13" /></i>
                  <div>
                    <strong>{{ item.label }}</strong>
                    <em>{{ item.state }}</em>
                    <p>{{ item.summary }}</p>
                  </div>
                </article>
              </div>
            </section>

            <section class="batch-failure-card">
              <div class="section-heading">
                <h2>高风险样本 TOP 3</h2>
              </div>
              <div class="risk-list">
                <article v-for="item in data.highRiskSamples" :key="item.caseId">
                  <strong>{{ item.caseId }}</strong>
                  <b>{{ item.score }}</b>
                  <span>{{ item.reason }}</span>
                </article>
              </div>
            </section>

            <section class="batch-provider-card">
              <div class="section-heading">
                <h2>提供者状态</h2>
              </div>
              <ul>
                <li v-for="provider in data.providers.slice(0, 2)" :key="provider.name">
                  <span>{{ provider.name }}</span>
                  <strong :class="`tone-${provider.tone}`">{{ provider.status }}</strong>
                </li>
              </ul>
            </section>
          </aside>
        </section>
      </main>
    </section>
  </section>
</template>

<style scoped>
.batch-page-shell {
  --blue: #2563eb;
  --green: #07864f;
  --red: #dc4c4c;
  --line: #d9e1eb;
  --muted: #667085;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  color: #111827;
  background: #f6f8fb;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.batch-sidebar {
  display: grid;
  grid-template-rows: 50px minmax(0, 1fr) auto;
  height: 100vh;
  min-height: 0;
  border-right: 1px solid var(--line);
  background: #fff;
}

.batch-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 12px;
  border-bottom: 1px solid var(--line);
  font-size: 14px;
}

.batch-brand button {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--blue);
  border: 0;
  background: transparent;
}

.batch-sidebar nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px;
}

.batch-sidebar nav button {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  height: 34px;
  padding: 0 9px;
  color: #344054;
  border: 0;
  border-radius: 6px;
  background: transparent;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}

.batch-sidebar nav button.active,
.batch-sidebar nav button:hover {
  color: #087a42;
  background: #eaf7f0;
  font-weight: 700;
}

.batch-sidebar nav button.active::before {
  position: absolute;
  left: 0;
  width: 3px;
  height: 22px;
  background: var(--green);
  content: "";
}

.batch-sidebar-note {
  display: grid;
  gap: 5px;
  margin: 8px;
  padding: 10px;
  border: 1px solid #d4dce6;
  border-radius: 8px;
  background: #f9fbfd;
}

.batch-sidebar-note span {
  color: #60736a;
  font-size: 10px;
  font-weight: 800;
}

.batch-sidebar-note strong {
  font-size: 14px;
}

.batch-sidebar-note small {
  color: var(--muted);
  font-size: 9px;
}

.batch-workspace {
  height: 100vh;
  min-width: 0;
  overflow: hidden;
}

.batch-topbar {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) auto auto;
  align-items: center;
  gap: 16px;
  height: 50px;
  padding: 0 15px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}

.batch-search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: min(100%, 630px);
  height: 32px;
  padding: 0 12px;
  color: var(--muted);
  border: 1px solid #ccd7e5;
  border-radius: 8px;
  background: #f8fafc;
}

.batch-search input {
  width: 100%;
  border: 0;
  outline: 0;
  color: #344054;
  background: transparent;
  font-size: 12px;
}

.batch-status,
.batch-user {
  display: flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.batch-status span {
  padding: 5px 8px;
  border-radius: 6px;
  color: #175cd3;
  background: #eaf1fd;
  font-size: 11px;
  font-weight: 700;
}

.batch-status [data-tone="green"] {
  color: #087a42;
  background: #e8f8ef;
}

.batch-status [data-tone="purple"] {
  color: #6941c6;
  background: #f2ebff;
}

.batch-user {
  color: var(--muted);
  font-size: 11px;
}

.batch-user span:last-child {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #25324a;
  font-weight: 700;
}

.batch-main {
  height: calc(100vh - 50px);
  min-width: 0;
  padding: 8px 14px 10px;
  overflow: hidden;
}

.batch-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  height: 62px;
}

.batch-title-row h1 {
  margin: 0;
  font-size: 25px;
  line-height: 1.15;
}

.batch-title-row p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.45;
}

.batch-action-buttons {
  display: flex;
  gap: 8px;
}

.batch-action-buttons button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 34px;
  padding: 0 13px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.batch-action-buttons .ghost {
  color: #344054;
  border: 1px solid #b9c5d3;
  background: #fff;
}

.batch-action-buttons .primary {
  color: #fff;
  border: 1px solid #07864f;
  background: #07864f;
}

.batch-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.72fr) minmax(350px, 1fr);
  gap: 9px;
  height: calc(100% - 62px);
  min-height: 0;
}

.batch-left,
.batch-right {
  display: grid;
  gap: 8px;
  min-height: 0;
}

.batch-left {
  grid-template-rows: 164px 122px 246px minmax(0, 1fr);
}

.batch-right {
  grid-template-rows: 190px 260px 112px minmax(0, 1fr);
}

.batch-config-card,
.batch-run-card,
.batch-metrics-card,
.batch-matrix-card,
.batch-timeline-card,
.batch-failure-card,
.batch-provider-card,
.batch-loop-card {
  min-height: 0;
  padding: 9px 10px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(35, 50, 72, 0.035);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 20px;
  margin-bottom: 7px;
}

.section-heading h2 {
  margin: 0;
  font-size: 13px;
  line-height: 1.2;
}

.section-heading h2 small,
.section-heading > span {
  color: var(--muted);
  font-size: 9px;
  font-weight: 500;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 5px 6px;
}

.config-grid article {
  display: grid;
  align-content: center;
  min-height: 42px;
  padding: 5px 8px;
  border: 1px solid #dce3ec;
  border-radius: 6px;
  background: #fbfcfe;
}

.config-grid span,
.metric-grid span {
  color: var(--muted);
  font-size: 9px;
}

.config-grid strong {
  margin-top: 3px;
  overflow: hidden;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.configuration-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  margin: 6px 0 0;
  padding: 0;
  overflow: hidden;
  border-radius: 6px;
  background: #f4f7f9;
  list-style: none;
}

.configuration-steps li {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  height: 24px;
  color: #344054;
  font-size: 8px;
  font-weight: 700;
}

.configuration-steps b {
  display: grid;
  place-items: center;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  color: #fff;
  background: var(--green);
  font-size: 8px;
}

.configuration-steps svg {
  position: absolute;
  right: -6px;
  z-index: 1;
  color: #89bda5;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.metric-grid article {
  position: relative;
  min-width: 0;
  height: 78px;
  padding: 8px 9px;
  overflow: hidden;
  border: 1px solid #dde4ed;
  border-radius: 7px;
  background: #fbfcfe;
}

.metric-grid strong {
  display: block;
  margin: 5px 0 3px;
  font-size: 19px;
  line-height: 1;
}

.metric-grid em {
  display: block;
  overflow: hidden;
  color: #526174;
  font-size: 9px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-grid i {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 4px;
  background: var(--green);
}

.metric-grid .tone-blue i {
  background: var(--blue);
}

.metric-grid .tone-red i {
  background: var(--red);
}

.matrix-heading > span {
  padding: 4px 7px;
  border-radius: 999px;
  color: #087a42;
  background: #eaf7f0;
  font-weight: 700;
}

.batch-matrix {
  overflow: hidden;
  border: 1px solid #dde4ed;
  border-radius: 7px;
}

.matrix-header,
.batch-matrix button {
  display: grid;
  grid-template-columns: 0.9fr 1fr repeat(5, 0.62fr) 0.72fr 0.62fr;
  align-items: center;
  gap: 6px;
}

.matrix-header {
  height: 29px;
  padding: 0 10px;
  color: #526174;
  background: #f2f5f8;
  font-size: 8px;
  font-weight: 700;
}

.batch-matrix button {
  width: 100%;
  height: 35px;
  padding: 0 10px;
  color: #344054;
  border: 0;
  border-top: 1px solid #e7ecf2;
  background: #fff;
  font-size: 8px;
  text-align: left;
  cursor: pointer;
}

.batch-matrix button.selected {
  background: #fff8ed;
  box-shadow: inset 3px 0 0 #f5a623;
}

.batch-matrix button b,
.batch-matrix button strong {
  font-size: 8px;
}

.batch-matrix .ok {
  color: #07864f;
}

.batch-matrix .fail {
  color: var(--red);
}

.matrix-overflow-row {
  display: none !important;
}

.loop-track {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  align-items: start;
  gap: 4px;
}

.loop-track article {
  position: relative;
  display: grid;
  justify-items: center;
  min-width: 0;
  text-align: center;
}

.loop-track article > div {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  margin-bottom: 5px;
  color: var(--blue);
  border: 1px solid #bfd2f9;
  border-radius: 50%;
  background: #f4f7ff;
}

.loop-track article strong {
  color: #175cd3;
  font-size: 8px;
}

.loop-track article span {
  margin-top: 2px;
  color: var(--muted);
  font-size: 7px;
  white-space: nowrap;
}

.loop-track article > svg {
  position: absolute;
  top: 12px;
  right: -9px;
  color: #b8c4d4;
}

.batch-run-card .section-heading {
  padding-bottom: 5px;
  border-bottom: 1px solid #edf1f5;
}

.batch-run-card .section-heading > strong {
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 10px;
}

.run-state-body {
  display: grid;
  grid-template-columns: 142px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.run-progress {
  --progress: 80%;
  display: grid;
  place-items: center;
  width: 122px;
  height: 122px;
  margin: 2px auto 0;
  border-radius: 50%;
  background:
    radial-gradient(circle closest-side, #fff 70%, transparent 72%),
    conic-gradient(var(--green) var(--progress), #e3e8e6 0);
}

.run-progress > div {
  display: grid;
  gap: 5px;
  text-align: center;
}

.run-progress strong {
  font-size: 24px;
  line-height: 1;
}

.run-progress span {
  color: #526174;
  font-size: 10px;
  white-space: nowrap;
}

.batch-run-card dl {
  display: grid;
  gap: 0;
  margin: 0;
}

.batch-run-card dl div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 22px;
  border-bottom: 1px solid #edf1f5;
  font-size: 9px;
}

.batch-run-card dt {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #526174;
  white-space: nowrap;
}

.batch-run-card dd {
  margin: 0;
  font-weight: 700;
  text-align: right;
  white-space: nowrap;
}

.batch-run-card dd.running {
  color: var(--green);
}

.batch-timeline {
  display: grid;
}

.batch-timeline article {
  display: grid;
  grid-template-columns: 54px 18px minmax(0, 1fr);
  align-items: start;
  min-height: 43px;
}

.batch-timeline time {
  padding-top: 2px;
  color: #526174;
  font-size: 8px;
  font-weight: 700;
}

.batch-timeline i {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 15px;
  height: 15px;
  color: var(--green);
  background: #fff;
  font-style: normal;
}

.batch-timeline i::after {
  position: absolute;
  top: 15px;
  bottom: -28px;
  left: 7px;
  width: 1px;
  background: #a8d3bf;
  content: "";
}

.batch-timeline article:last-of-type i::after {
  display: none;
}

.batch-timeline article > div {
  min-width: 0;
  padding-left: 5px;
}

.batch-timeline strong {
  font-size: 9px;
}

.batch-timeline em {
  margin-left: 8px;
  color: var(--green);
  font-size: 8px;
  font-style: normal;
  font-weight: 700;
}

.batch-timeline article.active i,
.batch-timeline article.active em {
  color: var(--blue);
}

.batch-timeline p {
  margin: 2px 0 0;
  overflow: hidden;
  color: var(--muted);
  font-size: 8px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-overflow-step {
  display: none !important;
}

.risk-list {
  display: grid;
}

.risk-list article {
  display: grid;
  grid-template-columns: 64px 34px minmax(0, 1fr);
  align-items: center;
  min-height: 23px;
  border-top: 1px solid #edf1f5;
  font-size: 8px;
}

.risk-list article:first-child {
  border-top: 0;
}

.risk-list strong {
  font-family: "Cascadia Code", Consolas, monospace;
}

.risk-list b {
  color: #344054;
}

.risk-list span {
  overflow: hidden;
  color: var(--muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-provider-card ul {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.batch-provider-card li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 24px;
  border-top: 1px solid #edf1f5;
  color: #344054;
  font-size: 9px;
}

.batch-provider-card li:first-child {
  border-top: 0;
}

.batch-provider-card strong {
  color: var(--green);
  white-space: nowrap;
}

.batch-provider-card .tone-blue {
  color: var(--blue);
}

@media (max-width: 1120px) {
  .batch-page-shell {
    display: block;
    height: auto;
    overflow: visible;
  }

  .batch-sidebar,
  .batch-workspace,
  .batch-main {
    height: auto;
    overflow: visible;
  }

  .batch-sidebar {
    grid-template-rows: auto;
  }

  .batch-sidebar nav {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
  }

  .batch-sidebar-note,
  .batch-status,
  .batch-user {
    display: none;
  }

  .batch-topbar {
    grid-template-columns: 1fr;
    height: auto;
    padding: 9px 14px;
  }

  .batch-layout,
  .batch-left,
  .batch-right {
    display: block;
    height: auto;
  }

  .batch-config-card,
  .batch-run-card,
  .batch-metrics-card,
  .batch-matrix-card,
  .batch-timeline-card,
  .batch-failure-card,
  .batch-provider-card,
  .batch-loop-card {
    margin-bottom: 9px;
  }
}
</style>
