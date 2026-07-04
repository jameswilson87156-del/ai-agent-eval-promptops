<script setup lang="ts">
import { computed, ref } from 'vue'
import { batchEvaluationMockData } from '../data/batchEvaluationMockData'

type ViewKey = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary'

defineProps<{ runningEval?: boolean }>()

const emit = defineEmits<{
  (event: 'open-view', view: ViewKey): void
  (event: 'run-eval'): void
}>()

const data = batchEvaluationMockData
const selectedCaseId = ref('case_093')
const selectedCase = computed(() => data.matrix.find((row) => row.caseId === selectedCaseId.value) ?? data.matrix[0])

function openView(view: ViewKey) {
  emit('open-view', view)
}
</script>

<template>
  <section class="batch-page-shell">
    <aside class="batch-sidebar">
      <div class="brand-mark">E</div>
      <nav aria-label="Eval navigation">
        <button type="button" @click="openView('overview')">总览</button>
        <button type="button" class="active">批量评测</button>
        <button type="button" @click="openView('compare')">输出对比</button>
        <button type="button" @click="openView('trace')">运行追踪</button>
        <button type="button" @click="openView('failure')">失败样本</button>
        <button type="button" @click="openView('dataset')">测试集</button>
        <button type="button" @click="openView('boundary')">系统边界</button>
      </nav>
      <div class="batch-sidebar-note">
        <span>Local Demo</span>
        <strong>Rule-based Eval</strong>
      </div>
    </aside>

    <main class="batch-main">
      <header class="batch-topbar">
        <div class="batch-title-row">
          <div>
            <p>{{ data.page.eyebrow }}</p>
            <h1>{{ data.page.title }}</h1>
            <span>{{ data.page.subtitle }}</span>
          </div>
          <div class="batch-actions">
            <button type="button" class="ghost" @click="openView('overview')">回到总览</button>
            <button type="button" class="primary" :disabled="runningEval" @click="emit('run-eval')">
              {{ runningEval ? '评测中' : '启动评测' }}
            </button>
          </div>
        </div>
      </header>

      <section class="batch-layout">
        <div class="batch-left">
          <section class="batch-config-card">
            <div class="section-heading">
              <p>Configuration</p>
              <h2>评测配置</h2>
            </div>
            <div class="config-grid">
              <article v-for="item in data.configuration" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
            <ol class="configuration-steps">
              <li v-for="step in data.configurationSteps" :key="step">{{ step }}</li>
            </ol>
          </section>

          <section class="batch-metrics-card">
            <div class="section-heading">
              <p>Metrics</p>
              <h2>核心指标</h2>
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
              <div>
                <p>Score Matrix</p>
                <h2>样本评分矩阵</h2>
              </div>
              <span>选中 {{ selectedCase.caseId }}</span>
            </div>
            <div class="batch-matrix" role="table" aria-label="Batch score matrix">
              <div class="matrix-header" role="row">
                <span role="columnheader">Case</span>
                <span role="columnheader">输入类型</span>
                <span role="columnheader">格式</span>
                <span role="columnheader">完整性</span>
                <span role="columnheader">相关性</span>
                <span role="columnheader">结果</span>
              </div>
              <button
                v-for="row in data.matrix"
                :key="row.caseId"
                type="button"
                role="row"
                :class="{ selected: row.caseId === selectedCaseId }"
                @click="selectedCaseId = row.caseId"
              >
                <span role="cell">{{ row.caseId }}</span>
                <span role="cell">{{ row.inputType }}</span>
                <span role="cell">{{ row.format }}</span>
                <span role="cell">{{ row.completeness }}</span>
                <span role="cell">{{ row.relevance }}</span>
                <strong role="cell" :class="row.result === '通过' ? 'ok' : 'fail'">{{ row.result }}</strong>
              </button>
            </div>
          </section>
        </div>

        <div class="batch-right">
          <section class="batch-run-card">
            <div class="section-heading">
              <p>Run State</p>
              <h2>{{ data.run.id }}</h2>
            </div>
            <div class="run-progress" :style="{ '--progress': `${data.run.progress}%` }">
              <strong>{{ data.run.progress }}%</strong>
              <span>{{ data.run.completed }} / {{ data.run.sampleCount }} completed</span>
            </div>
            <dl>
              <div>
                <dt>状态</dt>
                <dd>{{ data.run.status }}</dd>
              </div>
              <div>
                <dt>失败样本</dt>
                <dd>{{ data.run.failed }}</dd>
              </div>
              <div>
                <dt>预计耗时</dt>
                <dd>{{ data.run.estimatedDuration }}</dd>
              </div>
            </dl>
          </section>

          <section class="batch-timeline-card">
            <div class="section-heading">
              <p>Timeline</p>
              <h2>执行时间线</h2>
            </div>
            <div class="batch-timeline">
              <article v-for="item in data.timeline" :key="item.label">
                <span>{{ item.time }}</span>
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
              <p>Selected Failure</p>
              <h2>{{ selectedCase.caseId }}</h2>
            </div>
            <dl class="failure-detail">
              <div>
                <dt>失败原因</dt>
                <dd>{{ selectedCase.failureReason }}</dd>
              </div>
              <div>
                <dt>修复建议</dt>
                <dd>{{ selectedCase.suggestion }}</dd>
              </div>
            </dl>
            <div class="code-compare">
              <pre>{{ selectedCase.expectedOutput }}</pre>
              <pre>{{ selectedCase.actualOutput }}</pre>
            </div>
          </section>

          <section class="batch-provider-card">
            <div class="section-heading">
              <p>Providers</p>
              <h2>依赖状态</h2>
            </div>
            <ul>
              <li v-for="provider in data.providers" :key="provider.name">
                <span>{{ provider.name }}</span>
                <strong :class="`tone-${provider.tone}`">{{ provider.status }}</strong>
              </li>
            </ul>
          </section>

          <section class="batch-loop-card">
            <div class="section-heading">
              <p>Workflow</p>
              <h2>评测闭环</h2>
            </div>
            <div class="loop-track">
              <span v-for="step in data.loop" :key="step[0]">{{ step[0] }}</span>
            </div>
          </section>
        </div>
      </section>
    </main>
  </section>
</template>

<style scoped>
.batch-page-shell {
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  background:
    linear-gradient(135deg, rgba(239, 247, 243, 0.92), rgba(250, 252, 255, 0.98) 42%, rgba(246, 241, 231, 0.78)),
    #f8faf8;
  color: #18201c;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.batch-sidebar {
  height: 100vh;
  min-height: 0;
  padding: 12px 10px;
  border-right: 1px solid rgba(36, 48, 42, 0.12);
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(16px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #16201b;
  color: #fff;
  font-weight: 900;
}

.batch-sidebar nav {
  display: grid;
  gap: 3px;
}

.batch-sidebar button {
  border: 0;
  border-radius: 7px;
  padding: 8px 9px;
  text-align: left;
  background: transparent;
  color: #53625a;
  font-size: 12px;
  cursor: pointer;
}

.batch-sidebar button.active,
.batch-sidebar button:hover {
  background: #e8f3ee;
  color: #173728;
  font-weight: 800;
}

.batch-sidebar-note {
  margin-top: auto;
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid rgba(36, 48, 42, 0.12);
  border-radius: 8px;
  background: rgba(248, 250, 248, 0.84);
}

.batch-sidebar-note span,
.section-heading p,
.batch-title-row p {
  margin: 0;
  color: #6a7b72;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0;
  font-weight: 800;
}

.batch-main {
  height: 100vh;
  padding: 10px 15px 12px;
  overflow: hidden;
  min-width: 0;
}

.batch-topbar {
  height: 78px;
  margin-bottom: 8px;
}

.batch-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.batch-title-row h1 {
  margin: 2px 0;
  color: #121915;
  font-size: 25px;
  line-height: 1.1;
}

.batch-title-row span {
  display: block;
  max-width: 780px;
  color: #56655e;
  line-height: 1.45;
  font-size: 10px;
}

.batch-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.batch-actions button {
  border: 1px solid rgba(27, 40, 33, 0.14);
  border-radius: 7px;
  min-height: 32px;
  padding: 0 10px;
  cursor: pointer;
  font-weight: 800;
}

.batch-actions .ghost {
  background: #fff;
  color: #203129;
}

.batch-actions .primary {
  background: #1c6b4b;
  color: #fff;
  border-color: #1c6b4b;
}

.batch-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(330px, 0.75fr);
  height: calc(100vh - 108px);
  min-height: 0;
  gap: 8px;
  align-items: start;
}

.batch-left,
.batch-right {
  display: grid;
  gap: 8px;
  min-height: 0;
}

.batch-left {
  grid-template-rows: 170px 140px minmax(0, 1fr);
}

.batch-right {
  grid-template-rows: 126px 230px 126px 74px 52px;
}

.batch-config-card,
.batch-run-card,
.batch-metrics-card,
.batch-matrix-card,
.batch-timeline-card,
.batch-failure-card,
.batch-provider-card,
.batch-loop-card {
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(36, 48, 42, 0.12);
  border-radius: 8px;
  box-shadow: 0 8px 20px rgba(37, 45, 42, 0.06);
  padding: 10px;
  min-height: 0;
  overflow: hidden;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.section-heading h2 {
  margin: 2px 0 0;
  font-size: 13px;
  color: #16201b;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.config-grid article {
  border-radius: 7px;
  background: #f4f8f5;
  border: 1px solid rgba(36, 48, 42, 0.09);
  padding: 7px 8px;
  min-height: 45px;
}

.config-grid span,
.metric-grid span,
.failure-detail dt,
.batch-run-card dt {
  display: block;
  color: #68756f;
  font-size: 12px;
}

.config-grid strong {
  display: block;
  margin-top: 4px;
  color: #17231d;
  font-size: 10px;
  line-height: 1.3;
}

.configuration-steps {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 5px;
}

.configuration-steps li {
  border-radius: 999px;
  background: #18201b;
  color: #fff;
  min-height: 22px;
  display: grid;
  place-items: center;
  font-size: 9px;
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.metric-grid article {
  position: relative;
  overflow: hidden;
  min-height: 96px;
  border-radius: 7px;
  padding: 10px;
  background: #f7faf7;
  border: 1px solid rgba(36, 48, 42, 0.1);
}

.metric-grid strong {
  display: block;
  margin: 7px 0 4px;
  color: #14201a;
  font-size: 18px;
}

.metric-grid em {
  display: block;
  color: #63746c;
  font-size: 12px;
  font-style: normal;
}

.metric-grid i {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 5px;
  background: #1c6b4b;
}

.metric-grid .tone-red i {
  background: #bd4b45;
}

.metric-grid .tone-blue i {
  background: #2468a8;
}

.matrix-heading > span {
  align-self: center;
  border-radius: 999px;
  background: #ecf5f1;
  color: #1e6248;
  padding: 5px 8px;
  font-size: 10px;
  font-weight: 800;
}

.batch-matrix {
  overflow: hidden;
  border: 1px solid rgba(36, 48, 42, 0.12);
  border-radius: 7px;
}

.matrix-header,
.batch-matrix button {
  display: grid;
  grid-template-columns: 1fr 1.1fr 0.82fr 0.86fr 0.86fr 0.7fr;
  gap: 10px;
  align-items: center;
}

.matrix-header {
  min-height: 28px;
  padding: 0 12px;
  background: #e7eee9;
  color: #5d6c65;
  font-size: 12px;
  font-weight: 900;
}

.batch-matrix button {
  width: 100%;
  min-height: 32px;
  border: 0;
  border-top: 1px solid rgba(36, 48, 42, 0.09);
  padding: 0 12px;
  background: #fff;
  color: #27352f;
  font-size: 10px;
  text-align: left;
  cursor: pointer;
}

.batch-matrix button.selected {
  background: #fff8eb;
  box-shadow: inset 4px 0 0 #c78321;
}

.batch-matrix strong.ok {
  color: #28714c;
}

.batch-matrix strong.fail {
  color: #b24743;
}

.run-progress {
  --progress: 80%;
  min-height: 70px;
  border-radius: 8px;
  background:
    radial-gradient(circle closest-side, #fff 78%, transparent 80%),
    conic-gradient(#1c6b4b var(--progress), #e2ebe6 0);
  display: grid;
  place-items: center;
  text-align: center;
}

.run-progress strong {
  font-size: 22px;
}

.run-progress span {
  display: block;
  color: #6d7b74;
  font-size: 12px;
}

.batch-run-card dl,
.failure-detail {
  display: grid;
  gap: 5px;
  margin: 8px 0 0;
}

.batch-run-card dl div,
.failure-detail div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid rgba(36, 48, 42, 0.1);
  padding-top: 5px;
}

.batch-run-card dd,
.failure-detail dd {
  margin: 0;
  color: #17231d;
  font-weight: 800;
  text-align: right;
}

.batch-timeline {
  display: grid;
  gap: 5px;
}

.batch-timeline article {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
}

.batch-timeline article > span {
  color: #6d7b74;
  font-size: 9px;
  font-weight: 800;
  padding-top: 3px;
}

.batch-timeline article div {
  border-left: 2px solid rgba(28, 107, 75, 0.22);
  padding-left: 8px;
}

.batch-timeline strong {
  color: #17231d;
  font-size: 10px;
}

.batch-timeline em {
  margin-left: 8px;
  color: #1c6b4b;
  font-size: 9px;
  font-style: normal;
  font-weight: 800;
}

.batch-timeline p {
  margin: 2px 0 0;
  color: #65746d;
  font-size: 8px;
  line-height: 1.25;
}

.code-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 7px;
}

.code-compare pre {
  min-height: 46px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  border-radius: 7px;
  padding: 7px;
  background: #17201c;
  color: #e8f4ee;
  font-size: 8px;
  line-height: 1.35;
}

.batch-provider-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 5px;
}

.batch-provider-card li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #4f5f58;
  font-size: 10px;
}

.batch-provider-card strong {
  color: #1c6b4b;
}

.batch-provider-card .tone-gray {
  color: #7a847e;
}

.loop-track {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.loop-track span {
  min-height: 22px;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #eef5f1;
  color: #244636;
  padding: 0 8px;
  font-size: 9px;
  font-weight: 800;
}

@media (max-width: 1100px) {
  .batch-page-shell {
    grid-template-columns: 1fr;
  }

  .batch-sidebar {
    min-height: auto;
  }

  .batch-layout,
  .config-grid,
  .metric-grid,
  .configuration-steps {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .batch-main {
    padding: 18px;
  }

  .batch-title-row {
    display: grid;
  }

  .matrix-header,
  .batch-matrix button {
    grid-template-columns: 1fr 1fr 0.7fr;
  }

  .matrix-header span:nth-child(4),
  .matrix-header span:nth-child(5),
  .matrix-header span:nth-child(6),
  .batch-matrix button span:nth-child(4),
  .batch-matrix button span:nth-child(5),
  .batch-matrix button strong {
    display: none;
  }

  .code-compare {
    grid-template-columns: 1fr;
  }
}
</style>
