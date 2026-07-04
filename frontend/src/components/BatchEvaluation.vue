<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  BarChart3,
  Braces,
  CheckCircle2,
  CircleDashed,
  Code2,
  Database,
  FileCheck2,
  FileText,
  GitCompare,
  KeyRound,
  Layers3,
  ListChecks,
  Play,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  UserRound,
} from 'lucide-vue-next'
import { batchEvaluationMockData } from '../data/batchEvaluationMockData'

type BatchView = 'overview' | 'batch' | 'run' | 'versions' | 'review'

defineProps<{
  runningEval: boolean
}>()

const emit = defineEmits<{
  openView: [view: BatchView]
  runEval: []
}>()

const data = batchEvaluationMockData
const batchSearch = ref('')
const selectedCaseId = ref('case_093')

const selectedCase = computed(() =>
  data.matrix.find((item) => item.caseId === selectedCaseId.value) ?? data.matrix[2],
)

const navItems = [
  { id: 'dashboard', label: '评测总览', icon: BarChart3, view: 'overview' as const },
  { id: 'prompt', label: 'Prompt 工作台', icon: Code2, view: 'versions' as const },
  { id: 'batch', label: '批量评测', icon: FileText, view: 'batch' as const },
  { id: 'compare', label: '输出对比', icon: GitCompare, view: 'versions' as const },
  { id: 'trace', label: '运行追踪', icon: CircleDashed, view: 'review' as const },
  { id: 'failure', label: '失败归因', icon: AlertTriangle, view: 'run' as const },
  { id: 'dataset', label: '测试集', icon: Database, view: 'run' as const },
  { id: 'metrics', label: '指标快照', icon: SlidersHorizontal, view: 'overview' as const },
  { id: 'boundary', label: '系统边界', icon: ShieldCheck, view: 'overview' as const },
]

const configIcons = {
  dataset: Database,
  prompt: Code2,
  rules: FileText,
  mode: SlidersHorizontal,
  provider: KeyRound,
  dimensions: FileCheck2,
}

function chartPoints(values: number[]) {
  if (!values.length) return ''
  const step = values.length === 1 ? 0 : 100 / (values.length - 1)
  return values.map((value, index) => `${index * step},${100 - value}`).join(' ')
}

function stateTone(value: string) {
  if (value === '通过' || value === '-') return 'pass'
  if (value === '失败' || value === '缺失' || value === '无效') return 'fail'
  return 'warning'
}
</script>

<template>
  <div class="batch-shell">
    <aside class="batch-sidebar" aria-label="PromptOps Studio 导航">
      <div class="batch-brand">
        <button type="button" aria-label="展开导航"><Layers3 :size="18" /></button>
        <strong>PromptOps Studio</strong>
      </div>

      <nav class="batch-nav">
        <button
          v-for="item in navItems"
          :key="item.id"
          type="button"
          :aria-current="item.id === 'batch' ? 'page' : undefined"
          :data-view="item.view"
          @click="emit('openView', item.view)"
        >
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <section class="batch-local-card" aria-label="本地演示环境说明">
        <strong>本地演示环境</strong>
        <p><CheckCircle2 :size="14" />使用 Mock Output 与 Rule-based Eval</p>
        <p><CheckCircle2 :size="14" />无需网络与 API Key</p>
        <small>版本 0.3.0-beta</small><small>非生产环境</small>
      </section>
    </aside>

    <section class="batch-workspace">
      <header class="batch-topbar">
        <label class="batch-search">
          <Search :size="17" />
          <span class="sr-only">搜索 Prompt、Run 或 Dataset</span>
          <input v-model="batchSearch" type="search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索 Prompt / Run / Dataset" />
        </label>
        <div class="batch-status-cluster" aria-label="当前环境状态">
          <span data-tone="green">本地演示</span>
          <span data-tone="blue">规则评测</span>
          <span data-tone="purple">无需 API Key</span>
        </div>
        <div class="batch-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <div class="batch-top-actions">
          <button type="button" class="batch-outline-action" @click="emit('openView', 'run')"><Database :size="16" />查看测试集</button>
          <button type="button" class="batch-primary-action" :disabled="runningEval" @click="emit('runEval')"><Play :size="16" />{{ runningEval ? '批量评测运行中' : '启动批量评测' }}</button>
        </div>
      </header>

      <main id="main" class="batch-main" tabindex="-1">
        <header class="batch-title-row">
          <div><span>{{ data.page.eyebrow }}</span><h1>{{ data.page.title }}</h1><p>{{ data.page.subtitle }}</p></div>
          <p><ShieldCheck :size="14" />本页统计来自结构化本地演示数据，不代表真实模型或线上指标。</p>
        </header>

        <section class="batch-grid">
          <section class="batch-config-card batch-panel">
            <div class="batch-section-heading">
              <h2>评测配置流程</h2>
              <div class="batch-config-steps">
                <span v-for="(step, index) in data.configurationSteps" :key="step">{{ step }}<ArrowRight v-if="index < data.configurationSteps.length - 1" :size="12" /></span>
              </div>
            </div>
            <div class="batch-config-grid">
              <button v-for="field in data.configuration" :key="field.label" type="button">
                <span>{{ field.label }}</span>
                <strong><component :is="configIcons[field.icon as keyof typeof configIcons]" :size="15" />{{ field.value }}</strong>
              </button>
            </div>
          </section>

          <section class="batch-run-card batch-panel">
            <div class="batch-run-heading"><h2>批量运行状态 <small>Eval Run：{{ data.run.id }}</small></h2><span>{{ data.run.status }}</span></div>
            <div class="batch-run-stats">
              <div><span>样本数</span><strong>{{ data.run.sampleCount }}</strong></div>
              <div><span>已完成</span><strong>{{ data.run.completed }}</strong></div>
              <div data-tone="danger"><span>失败样本</span><strong>{{ data.run.failed }}</strong></div>
              <div><span>预计耗时</span><strong>{{ data.run.estimatedDuration }}</strong></div>
              <div class="batch-progress"><span>进度</span><i><b :style="{ width: `${data.run.progress}%` }"></b></i><strong>{{ data.run.progress }}%</strong></div>
            </div>
          </section>

          <section class="batch-metrics-card batch-panel">
            <div class="batch-metric-side">
              <div class="batch-section-heading"><h2>核心评测指标</h2><span>基于本地测试集与 Rule-based Eval 的演示统计</span></div>
              <div class="batch-metrics">
                <article v-for="metric in data.metrics" :key="metric.label" :data-tone="metric.tone">
                  <span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.helper }}</small>
                  <i><b :style="{ width: `${metric.progress}%` }"></b></i>
                </article>
              </div>
            </div>
            <div class="batch-trend">
              <div class="batch-section-heading"><h2>评测趋势</h2><span><i data-line="blue"></i>Avg Score <i data-line="green"></i>Pass Rate</span></div>
              <svg viewBox="0 0 100 100" preserveAspectRatio="none" role="img" aria-label="当前批量评测得分与通过率趋势">
                <line x1="0" y1="25" x2="100" y2="25" /><line x1="0" y1="50" x2="100" y2="50" /><line x1="0" y1="75" x2="100" y2="75" />
                <polyline :points="chartPoints(data.trend.avgScore)" data-line="blue" /><polyline :points="chartPoints(data.trend.passRate)" data-line="green" />
              </svg>
              <div><span v-for="label in data.trend.labels" :key="label">{{ label }}</span></div>
            </div>
          </section>

          <section class="batch-matrix-card batch-panel">
            <div class="batch-section-heading"><h2>评测结果矩阵 <small>共 120 条，已完成 96 条</small></h2><span>Score Matrix / local-rule baseline</span></div>
            <div class="batch-matrix" role="table" aria-label="批量评测结果矩阵">
              <div class="batch-matrix-head" role="row">
                <span>Case ID</span><span>输入类型</span><span>Prompt</span><span>格式</span><span>完整性</span><span>相关性</span><span>合规性</span><span>稳定性</span><span>结果</span><span>失败原因</span>
              </div>
              <button
                v-for="row in data.matrix"
                :key="row.caseId"
                type="button"
                role="row"
                :data-selected="row.caseId === selectedCaseId"
                :aria-pressed="row.caseId === selectedCaseId"
                @click="selectedCaseId = row.caseId"
              >
                <span>{{ row.caseId }}</span><span>{{ row.inputType }}</span><span>{{ row.promptVersion }}</span>
                <span :data-state="stateTone(row.format)">{{ row.format }}</span><span :data-state="stateTone(row.completeness)">{{ row.completeness }}</span>
                <span :data-state="stateTone(row.relevance)">{{ row.relevance }}</span><span :data-state="stateTone(row.compliance)">{{ row.compliance }}</span>
                <span :data-state="stateTone(row.stability)">{{ row.stability }}</span><span :data-state="stateTone(row.result)">{{ row.result }}</span><span>{{ row.failureReason }}</span>
              </button>
            </div>
          </section>

          <section class="batch-timeline-card">
            <div class="batch-timeline-heading"><div><h2>Eval Run 时间线</h2><span>{{ data.run.id }}</span></div><div><strong>{{ data.run.status }}</strong><time>{{ data.run.startedAt }}</time></div></div>
            <div class="batch-timeline">
              <article v-for="node in data.timeline" :key="node.label" :data-state="node.state">
                <span class="batch-timeline-marker"><Activity :size="13" /></span><time>{{ node.time }}</time><div><strong>{{ node.label }}</strong><small>{{ node.state }}</small><p>{{ node.summary }}</p></div>
              </article>
            </div>
          </section>

          <section class="batch-failure-card batch-panel">
            <div class="batch-section-heading"><h2>失败样本分析</h2><span>关联 Eval Run：{{ data.run.id }} · 当前 {{ selectedCase.caseId }}</span></div>
            <div class="batch-failure-layout">
              <div class="batch-failure-ranking"><h3>失败原因排行</h3><div v-for="(item, index) in data.failureRanking" :key="item.label"><b>{{ index + 1 }}</b><span>{{ item.label }}</span><i><em :style="{ width: `${item.percent * 2.5}%` }"></em></i><strong>{{ item.count }} ({{ item.percent }}%)</strong></div></div>
              <div class="batch-current-failure"><span>问题：<strong>{{ selectedCase.failureReason }}</strong></span><p>期望输出：包含 answer 与 confidence</p><p>实际输出：{{ selectedCase.failureReason === '-' ? '字段完整' : selectedCase.failureReason }}</p><p>修复建议：{{ selectedCase.suggestion }}</p></div>
              <div class="batch-schema"><h3>Schema 对比</h3><div><section><span>期望输出</span><pre>{{ selectedCase.expectedOutput }}</pre></section><section><span>实际输出</span><pre>{{ selectedCase.actualOutput }}</pre></section></div></div>
            </div>
          </section>

          <section class="batch-provider-card batch-panel">
            <div class="batch-provider-status"><h2>Provider 状态与评测边界</h2><div><span v-for="provider in data.providers" :key="provider.name"><KeyRound :size="13" />{{ provider.name }}<b :data-tone="provider.tone">{{ provider.status }}</b></span></div></div>
            <p><Sparkles :size="15" />本地演示环境，使用 Mock Output 与 Rule-based Eval，非生产环境。</p>
          </section>

          <section class="batch-loop-card batch-panel">
            <h2>PromptOps 批量评测闭环</h2>
            <div><button v-for="(item, index) in data.loop" :key="item[0]" type="button"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong><ArrowRight v-if="index < data.loop.length - 1" :size="14" /></button></div>
          </section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.batch-shell { --batch-blue:#2563eb; --batch-green:#16a34a; --batch-red:#ef4444; --batch-border:#d7e0ec; display:grid; grid-template-columns:216px minmax(0,1fr); min-height:100vh; color:#111827; background:#f6f8fb; }
button,input { font:inherit; }
.batch-sidebar { display:grid; grid-template-rows:auto 1fr auto; min-height:100vh; border-right:1px solid var(--batch-border); background:#fff; }
.batch-brand { height:58px; display:flex; align-items:center; gap:10px; padding:0 16px; border-bottom:1px solid var(--batch-border); font-size:16px; }
.batch-brand button { display:grid; place-items:center; width:28px; height:28px; padding:0; color:var(--batch-blue); border:0; background:transparent; }
.batch-nav { display:flex; flex-direction:column; gap:3px; padding:12px 9px; }
.batch-nav button { position:relative; display:flex; align-items:center; gap:10px; min-height:38px; padding:0 11px; color:#344054; border:0; border-radius:6px; background:transparent; cursor:pointer; }
.batch-nav button:hover { background:#f2f5fa; }
.batch-nav button[aria-current="page"] { color:#175cd3; background:#eaf1fd; font-weight:700; }
.batch-nav button[aria-current="page"]::before { position:absolute; left:0; width:3px; height:24px; border-radius:2px; background:#2563eb; content:""; }
.batch-local-card { margin:10px; padding:12px; border:1px solid #cfdcf1; border-radius:8px; background:#f7faff; }
.batch-local-card>strong { color:#175cd3; font-size:13px; }.batch-local-card p { display:flex; gap:6px; margin:7px 0; color:#475467; font-size:12px; line-height:1.4; }.batch-local-card p svg { flex:none; color:#12a150; }.batch-local-card small { margin-right:10px; color:#7b8797; font-size:10px; }
.batch-workspace { min-width:0; }
.batch-topbar { height:58px; display:grid; grid-template-columns:minmax(280px,1fr) auto auto auto; align-items:center; gap:14px; padding:0 18px; border-bottom:1px solid var(--batch-border); background:#fff; }
.batch-search { display:flex; align-items:center; gap:8px; max-width:600px; height:34px; padding:0 12px; color:#667085; border:1px solid #ccd7e5; border-radius:8px; background:#f8fafc; }.batch-search input { width:100%; border:0; outline:0; background:transparent; color:#344054; }
.batch-status-cluster,.batch-user,.batch-top-actions { display:flex; align-items:center; gap:7px; white-space:nowrap; }.batch-status-cluster span { padding:6px 9px; border-radius:6px; font-size:11px; font-weight:700; }.batch-status-cluster [data-tone="green"]{color:#087a42;background:#e8f8ef}.batch-status-cluster [data-tone="blue"]{color:#175cd3;background:#eaf1fd}.batch-status-cluster [data-tone="purple"]{color:#6941c6;background:#f2ebff}
.batch-user { color:#667085; font-size:11px; }.batch-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}.batch-top-actions button { height:34px; display:flex; align-items:center; gap:7px; padding:0 12px; border-radius:7px; font-weight:700; cursor:pointer; }.batch-outline-action{color:#344054;border:1px solid #cbd7e6;background:#fff}.batch-primary-action{color:#fff;border:1px solid #1d4ed8;background:#2563eb;box-shadow:0 2px 5px rgb(37 99 235 / 18%)}.batch-primary-action:disabled{opacity:.65}
.batch-main { min-width:0; padding:14px 16px 16px; }
.batch-title-row { min-height:58px; display:flex; align-items:flex-start; justify-content:space-between; gap:18px; }.batch-title-row>div{display:flex;align-items:baseline;gap:9px;flex-wrap:wrap}.batch-title-row span{text-transform:uppercase;color:#667085;font-size:11px;font-weight:700}.batch-title-row h1{margin:0;font-size:27px;letter-spacing:0}.batch-title-row div p{width:100%;margin:1px 0 0;color:#667085;font-size:12px}.batch-title-row>p{display:flex;align-items:center;gap:6px;max-width:365px;margin:5px 0 0;color:#667085;font-size:11px}
.batch-grid { display:grid; grid-template-columns:minmax(0,58fr) minmax(0,42fr); grid-template-rows:118px 64px 130px 142px 74px 58px; grid-template-areas:"config timeline" "run timeline" "metrics timeline" "matrix failure" "matrix provider" "loop loop"; gap:8px; }
.batch-panel { min-width:0; overflow:hidden; border:1px solid var(--batch-border); border-radius:8px; background:#fff; }.batch-section-heading{height:30px;display:flex;align-items:center;justify-content:space-between;gap:10px;padding:0 10px;border-bottom:1px solid #edf1f6}.batch-section-heading h2{margin:0;font-size:13px}.batch-section-heading h2 small{margin-left:6px;color:#667085;font-size:10px;font-weight:500}.batch-section-heading>span{overflow:hidden;color:#718096;font-size:9px;white-space:nowrap;text-overflow:ellipsis}
.batch-config-card{grid-area:config}.batch-config-steps{display:flex;align-items:center;gap:5px;color:#667085;font-size:9px}.batch-config-steps span{display:flex;align-items:center;gap:5px;white-space:nowrap}.batch-config-grid{display:grid;grid-template-columns:repeat(3,1fr);height:calc(100% - 30px)}.batch-config-grid button{min-width:0;padding:6px 9px;text-align:left;border:0;border-right:1px solid #e6ebf2;border-bottom:1px solid #e6ebf2;background:#fff}.batch-config-grid button:nth-child(3n){border-right:0}.batch-config-grid span{display:block;margin-bottom:4px;color:#7b8797;font-size:9px}.batch-config-grid strong{display:flex;align-items:center;gap:6px;overflow:hidden;color:#27364f;font-size:10px;white-space:nowrap;text-overflow:ellipsis}
.batch-run-card{grid-area:run;display:grid;grid-template-columns:220px 1fr;align-items:center;padding:0 10px}.batch-run-heading{display:flex;align-items:center;gap:8px}.batch-run-heading h2{margin:0;font-size:13px}.batch-run-heading small{display:block;margin-top:3px;color:#667085;font-size:9px;font-weight:500}.batch-run-heading>span{padding:4px 7px;color:#175cd3;border-radius:10px;background:#eaf1fd;font-size:9px;font-weight:700}.batch-run-stats{display:grid;grid-template-columns:repeat(4,75px) minmax(170px,1fr);align-items:center}.batch-run-stats>div{padding:0 9px;border-left:1px solid #edf1f6}.batch-run-stats span{display:block;color:#7b8797;font-size:9px}.batch-run-stats strong{font-size:13px}.batch-run-stats [data-tone="danger"] strong{color:var(--batch-red)}.batch-progress{display:grid!important;grid-template-columns:auto 1fr auto;align-items:center;gap:7px}.batch-progress span{grid-column:1/-1}.batch-progress i{height:6px;overflow:hidden;border-radius:4px;background:#e9edf3}.batch-progress b{display:block;height:100%;border-radius:4px;background:#2563eb}.batch-progress strong{font-size:10px}
.batch-metrics-card{grid-area:metrics;display:grid;grid-template-columns:minmax(0,2fr) minmax(210px,1fr)}.batch-metric-side{min-width:0;border-right:1px solid #e6ebf2}.batch-metrics{display:grid;grid-template-columns:repeat(4,1fr);height:calc(100% - 30px)}.batch-metrics article{display:flex;flex-direction:column;min-width:0;padding:8px 9px;border-right:1px solid #edf1f6}.batch-metrics article:last-child{border-right:0}.batch-metrics span{color:#667085;font-size:9px}.batch-metrics strong{margin:4px 0 1px;font-size:17px;white-space:nowrap}.batch-metrics small{overflow:hidden;color:#8994a4;font-size:8px;white-space:nowrap;text-overflow:ellipsis}.batch-metrics article>i{height:5px;margin-top:auto;overflow:hidden;border-radius:4px;background:#edf1f5}.batch-metrics article>i b{display:block;height:100%;border-radius:4px}.batch-metrics [data-tone="green"]>i b{background:var(--batch-green)}.batch-metrics [data-tone="blue"]>i b{background:var(--batch-blue)}.batch-metrics [data-tone="red"] strong{color:#dc2626}.batch-metrics [data-tone="red"]>i b{background:var(--batch-red)}
.batch-trend{min-width:0}.batch-trend .batch-section-heading span{display:flex;align-items:center;gap:5px}.batch-trend .batch-section-heading span i{width:11px;height:2px}.batch-trend [data-line="blue"]{background:#2563eb}.batch-trend [data-line="green"]{background:#16a34a}.batch-trend svg{width:calc(100% - 18px);height:72px;margin:3px 9px 0;overflow:visible}.batch-trend svg line{stroke:#e7ecf3;stroke-width:.6}.batch-trend svg polyline{fill:none;stroke-width:2;vector-effect:non-scaling-stroke}.batch-trend svg [data-line="blue"]{stroke:#2563eb}.batch-trend svg [data-line="green"]{stroke:#16a34a}.batch-trend>div:last-child{display:flex;justify-content:space-between;padding:0 9px;color:#8a94a3;font-size:7px}
.batch-matrix-card{grid-area:matrix}.batch-matrix{height:calc(100% - 30px);padding:2px 8px}.batch-matrix-head,.batch-matrix button{display:grid;grid-template-columns:70px 76px 54px repeat(5,50px) 48px minmax(120px,1fr);align-items:center;min-width:0}.batch-matrix-head{height:22px;color:#667085;border-bottom:1px solid #dfe6ef;font-size:8px;font-weight:700}.batch-matrix button{width:100%;height:23px;padding:0;color:#344054;text-align:left;border:0;border-bottom:1px solid #edf1f5;background:#fff;font-size:8px;cursor:pointer}.batch-matrix button:hover{background:#f7f9fc}.batch-matrix button[data-selected="true"]{box-shadow:inset 3px 0 #2563eb;background:#edf4ff;color:#175cd3;font-weight:700}.batch-matrix span{min-width:0;overflow:hidden;padding:0 4px;white-space:nowrap;text-overflow:ellipsis}.batch-matrix [data-state="pass"]{color:#159447;font-weight:700}.batch-matrix [data-state="fail"]{color:#e04444;font-weight:700}.batch-matrix [data-state="warning"]{color:#d97706;font-weight:700}
.batch-timeline-card{grid-area:timeline;min-width:0;padding:10px 12px;border:1px solid #16355a;border-radius:9px;background:#0f223b;color:#fff;box-shadow:0 5px 14px rgb(15 34 59 / 15%)}.batch-timeline-heading{height:30px;display:flex;justify-content:space-between;align-items:flex-start}.batch-timeline-heading>div{display:flex;align-items:center;gap:10px}.batch-timeline-heading h2{margin:0;font-size:14px}.batch-timeline-heading span,.batch-timeline-heading time{color:#a8bdd6;font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:9px}.batch-timeline-heading strong{padding:4px 7px;border-radius:5px;background:#2563eb;font-size:9px}.batch-timeline{position:relative}.batch-timeline::before{position:absolute;top:11px;bottom:11px;left:7px;width:1px;background:#456584;content:""}.batch-timeline article{position:relative;display:grid;grid-template-columns:18px 54px minmax(0,1fr);align-items:center;min-height:39px}.batch-timeline-marker{z-index:1;display:grid;place-items:center;width:16px;height:16px;color:#60a5fa;border:1px solid #60a5fa;border-radius:50%;background:#0f223b}.batch-timeline article[data-state="已完成"] .batch-timeline-marker{color:#67d69c;border-color:#67d69c}.batch-timeline article[data-state="运行中"]{margin:1px 0;padding:2px 6px 2px 0;border:1px solid #245d9b;border-radius:6px;background:#15365d}.batch-timeline article[data-state="运行中"] .batch-timeline-marker{box-shadow:0 0 0 3px rgb(37 99 235 / 25%);color:#93c5fd}.batch-timeline article[data-state="等待中"]{opacity:.68}.batch-timeline article>time{color:#91aac4;font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:8px}.batch-timeline article>div{display:grid;grid-template-columns:145px 48px minmax(0,1fr);align-items:center;gap:7px;min-width:0}.batch-timeline article strong{font-size:10px}.batch-timeline article small{color:#91aac4;font-size:8px}.batch-timeline article p{overflow:hidden;margin:0;color:#c2d0df;font-size:9px;white-space:nowrap;text-overflow:ellipsis}
.batch-failure-card{grid-area:failure}.batch-failure-layout{display:grid;grid-template-columns:1.05fr .95fr 1.1fr;height:calc(100% - 30px)}.batch-failure-layout>div{min-width:0;padding:7px 8px;border-right:1px solid #edf1f5}.batch-failure-layout>div:last-child{border-right:0}.batch-failure-layout h3{margin:0 0 5px;font-size:9px}.batch-failure-ranking>div{display:grid;grid-template-columns:12px minmax(0,1fr) 38px 42px;align-items:center;gap:4px;height:16px;font-size:7px}.batch-failure-ranking>div>b{color:#ef4444}.batch-failure-ranking>div>span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.batch-failure-ranking i{height:3px;background:#f2e4e4}.batch-failure-ranking em{display:block;height:100%;background:#ef4444}.batch-failure-ranking strong{font-size:7px}.batch-current-failure{font-size:8px;line-height:1.45}.batch-current-failure>span{display:block;margin-bottom:5px;color:#667085}.batch-current-failure span strong{color:#e04444}.batch-current-failure p{margin:3px 0}.batch-schema>div{display:grid;grid-template-columns:1fr 1fr;gap:5px}.batch-schema section{min-width:0}.batch-schema span{color:#667085;font-size:7px}.batch-schema pre{height:67px;margin:3px 0 0;padding:5px;overflow:hidden;color:#334155;border:1px solid #e2e8f0;border-radius:5px;background:#f8fafc;font-size:7px;line-height:1.4;white-space:pre-wrap;word-break:break-all}
.batch-provider-card{grid-area:provider;display:grid;grid-template-columns:1fr 220px;align-items:stretch}.batch-provider-status{min-width:0;padding:7px 9px}.batch-provider-status h2{margin:0 0 7px;font-size:11px}.batch-provider-status>div{display:grid;grid-template-columns:1fr 1fr;gap:6px 12px}.batch-provider-status span{display:flex;align-items:center;gap:5px;min-width:0;color:#526174;font-size:8px}.batch-provider-status b{margin-left:auto;white-space:nowrap}.batch-provider-status [data-tone="green"]{color:#159447}.batch-provider-status [data-tone="blue"]{color:#2563eb}.batch-provider-status [data-tone="gray"]{color:#667085}.batch-provider-card>p{display:flex;align-items:center;justify-content:center;gap:7px;margin:7px;padding:0 10px;color:#175cd3;border:1px solid #bed3f6;border-radius:6px;background:#eff5ff;text-align:center;font-size:9px;line-height:1.45}
.batch-loop-card{grid-area:loop;display:grid;grid-template-columns:145px 1fr;align-items:center}.batch-loop-card h2{margin:0;padding-left:12px;font-size:12px}.batch-loop-card>div{display:grid;grid-template-columns:repeat(7,1fr);height:100%}.batch-loop-card button{position:relative;display:flex;flex-direction:column;justify-content:center;min-width:0;padding:0 9px;border:0;border-left:1px solid #e5ebf2;background:#fff}.batch-loop-card button span{color:#7b8797;font-size:7px}.batch-loop-card button strong{margin-top:2px;font-size:9px}.batch-loop-card button svg{position:absolute;right:-8px;z-index:1;color:#8ba0b9;background:#fff}
@media (min-width:1181px) and (max-height:900px){.batch-shell{grid-template-columns:190px minmax(0,1fr);height:100vh;min-height:0;overflow:hidden}.batch-sidebar{min-height:0;height:100vh}.batch-brand{height:50px;padding:0 12px;font-size:14px}.batch-nav{padding:8px}.batch-nav button{min-height:34px;padding:0 9px;font-size:12px}.batch-local-card{margin:8px;padding:10px}.batch-local-card p{margin:5px 0;font-size:10px}.batch-topbar{height:50px;grid-template-columns:minmax(260px,1fr) auto auto auto;gap:10px;padding:0 15px}.batch-search{height:32px}.batch-status-cluster span{padding:5px 8px}.batch-top-actions button{height:32px;padding:0 10px;font-size:11px}.batch-main{height:calc(100vh - 50px);padding:10px 15px 12px;overflow:hidden}.batch-title-row{min-height:50px}.batch-title-row h1{font-size:25px}.batch-title-row div p{font-size:10px}.batch-grid{height:calc(100% - 50px)}}
@media (max-width:1180px){.batch-topbar{grid-template-columns:1fr auto}.batch-status-cluster,.batch-user{display:none}.batch-grid{grid-template-columns:1fr;grid-template-rows:auto;grid-template-areas:"config" "run" "metrics" "timeline" "matrix" "failure" "provider" "loop"}.batch-main{overflow:visible}.batch-config-card{min-height:130px}.batch-run-card{min-height:80px}.batch-metrics-card{min-height:150px}.batch-timeline-card{min-height:330px}.batch-matrix-card{min-height:230px}.batch-failure-card{min-height:160px}.batch-provider-card{min-height:80px}.batch-loop-card{min-height:64px}}
@media (max-width:760px){.batch-shell{display:block}.batch-sidebar{min-height:auto;border-right:0}.batch-brand{height:50px}.batch-nav{display:grid;grid-template-columns:1fr 1fr}.batch-local-card{display:none}.batch-topbar{height:auto;grid-template-columns:1fr;padding:8px}.batch-top-actions{justify-content:stretch}.batch-top-actions button{flex:1;justify-content:center}.batch-main{padding:10px}.batch-title-row{display:block}.batch-title-row>p{margin-bottom:10px}.batch-config-grid{grid-template-columns:1fr}.batch-run-card{display:block;padding:10px}.batch-run-stats{grid-template-columns:1fr 1fr;margin-top:10px}.batch-progress{grid-column:1/-1}.batch-metrics-card{display:block}.batch-metrics{grid-template-columns:1fr 1fr}.batch-trend{min-height:130px}.batch-matrix{overflow-x:auto}.batch-matrix-head,.batch-matrix button{min-width:770px}.batch-failure-layout{grid-template-columns:1fr}.batch-provider-card{grid-template-columns:1fr}.batch-loop-card{display:block;overflow-x:auto}.batch-loop-card h2{padding:10px}.batch-loop-card>div{min-width:720px;height:56px}}
</style>
