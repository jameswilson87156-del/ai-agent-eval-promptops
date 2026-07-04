<script setup lang="ts">
import { ref } from 'vue'
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
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
  Search,
  ShieldCheck,
  SlidersHorizontal,
  UserCheck,
  UserRound,
} from 'lucide-vue-next'
import { traceDetailMockData } from '../data/traceDetailMockData'

type TraceView = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'run' | 'versions' | 'review'

defineProps<{
  runningEval: boolean
}>()

const emit = defineEmits<{
  openView: [view: TraceView]
  runEval: []
}>()

const data = traceDetailMockData
const traceSearch = ref('')

const navItems = [
  { id: 'dashboard', label: '评测总览', icon: BarChart3, view: 'overview' as const },
  { id: 'prompt', label: 'Prompt 工作台', icon: Code2, view: 'prompt' as const },
  { id: 'batch', label: '批量评测', icon: FileText, view: 'batch' as const },
  { id: 'compare', label: '输出对比', icon: GitCompare, view: 'compare' as const },
  { id: 'trace', label: '运行追踪', icon: CircleDashed, view: 'trace' as const },
  { id: 'failure', label: '失败归因', icon: AlertTriangle, view: 'failure' as const },
  { id: 'dataset', label: '测试集', icon: Database, view: 'dataset' as const },
  { id: 'metrics', label: '指标快照', icon: SlidersHorizontal, view: 'overview' as const },
  { id: 'boundary', label: '系统边界', icon: ShieldCheck, view: 'boundary' as const },
]
</script>

<template>
  <div class="trace-shell">
    <aside class="trace-sidebar" aria-label="PromptOps Studio 导航">
      <div class="trace-brand">
        <button type="button" aria-label="展开导航"><Layers3 :size="18" /></button>
        <strong>PromptOps Studio</strong>
      </div>
      <nav class="trace-nav">
        <button
          v-for="item in navItems"
          :key="item.id"
          type="button"
          :aria-current="item.id === 'trace' ? 'page' : undefined"
          :data-view="item.view"
          @click="emit('openView', item.view)"
        >
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </nav>
      <section class="trace-local-card" aria-label="本地演示环境说明">
        <strong>本地演示环境</strong>
        <p><CheckCircle2 :size="14" />使用 Mock Output 与 Rule-based Eval</p>
        <p><CheckCircle2 :size="14" />无需网络与 API Key</p>
        <small>版本 0.3.0-beta</small><small>非生产环境</small>
      </section>
    </aside>

    <section class="trace-workspace">
      <header class="trace-topbar">
        <label class="trace-search">
          <Search :size="17" />
          <span class="sr-only">搜索 Prompt、Run 或 Dataset</span>
          <input v-model="traceSearch" type="search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索 Prompt / Run / Dataset" />
        </label>
        <div class="trace-status-cluster" aria-label="当前环境状态">
          <span data-tone="green">本地演示</span>
          <span data-tone="blue">规则评测</span>
          <span data-tone="purple">无需 API Key</span>
        </div>
        <div class="trace-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <div class="trace-top-actions">
          <button type="button" class="trace-outline-action" @click="emit('openView', 'batch')"><ArrowLeft :size="16" />返回评测运行</button>
          <button type="button" class="trace-primary-action" :disabled="runningEval" @click="emit('openView', 'review')"><UserCheck :size="16" />进入人工复核</button>
        </div>
      </header>

      <main id="main" class="trace-main" tabindex="-1">
        <header class="trace-title-row">
          <div><span>{{ data.page.eyebrow }}</span><h1>{{ data.page.title }}</h1><p>{{ data.page.subtitle }}</p></div>
        </header>

        <section class="trace-context-strip" aria-label="当前运行上下文">
          <div v-for="item in data.context" :key="item.label" :data-tone="item.tone">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="trace-detail-grid">
          <section class="trace-summary-card trace-panel">
            <div class="trace-section-heading"><h2>运行摘要</h2><span>{{ data.summary.identity[0][1] }}</span></div>
            <div class="trace-summary-body">
              <div class="trace-summary-identity">
                <div v-for="[label, value] in data.summary.identity" :key="label"><span>{{ label }}</span><strong>{{ value }}</strong></div>
              </div>
              <div class="trace-summary-highlights">
                <div data-tone="score"><span>质量分</span><strong>{{ data.summary.score }}</strong></div>
                <div data-tone="danger"><span>失败原因</span><strong>{{ data.summary.failureReason }}</strong></div>
                <div data-tone="warning"><span>复核状态</span><strong>{{ data.summary.reviewStatus }}</strong></div>
              </div>
            </div>
          </section>

          <section class="trace-prompt-card trace-panel">
            <div class="trace-section-heading"><h2>输入与 Prompt 构建</h2><span>{{ data.promptBuild.template }}</span></div>
            <div class="trace-prompt-layout">
              <div class="trace-input-box">
                <span>原始输入</span>
                <p>{{ data.promptBuild.rawInput }}</p>
                <span>变量注入</span>
                <div class="trace-variable-chips"><b v-for="variable in data.promptBuild.variables" :key="variable">{{ variable }}</b></div>
                <div class="trace-variable-values"><p v-for="[label, value] in data.promptBuild.values" :key="label"><code>{{ label }}</code>：{{ value }}</p></div>
              </div>
              <div class="trace-prompt-preview">
                <div><span>Prompt 模板</span><strong>{{ data.promptBuild.template }}</strong></div>
                <span>Prompt Preview</span>
                <pre>{{ data.promptBuild.preview }}</pre>
              </div>
            </div>
          </section>

          <section class="trace-schema-card trace-panel">
            <div class="trace-section-heading"><h2>输出与 Schema 对比</h2><span>Schema 校验失败</span></div>
            <div class="trace-schema-grid">
              <section><span>Mock Output</span><pre>{{ data.schemaCompare.mockOutput }}</pre></section>
              <section><span>期望 Schema</span><pre>{{ data.schemaCompare.expectedSchema }}</pre></section>
              <section><span>实际输出</span><pre>{{ data.schemaCompare.actualOutput }}</pre></section>
              <section class="trace-checks">
                <span>缺失字段 / 校验结果</span>
                <p v-for="check in data.schemaCompare.checks" :key="check.label" :data-state="check.state">
                  <CheckCircle2 v-if="check.state === 'pass'" :size="13" />
                  <AlertTriangle v-else :size="13" />
                  {{ check.label }}
                </p>
              </section>
            </div>
          </section>

          <section class="trace-score-card trace-panel">
            <div class="trace-section-heading"><h2>规则评分</h2><span>总分 {{ data.scoring.totalScore }}</span></div>
            <div class="trace-score-grid">
              <div v-for="[label, value] in data.scoring.dimensions" :key="label"><span>{{ label }}</span><strong>{{ value }}</strong></div>
              <div class="trace-total-score"><span>总分</span><strong>{{ data.scoring.totalScore }}</strong></div>
            </div>
            <div class="trace-score-meta">
              <p v-for="[label, value] in data.scoring.meta" :key="label" :data-danger="value.startsWith('-') || value.includes('required')">
                <span>{{ label }}</span><strong>{{ value }}</strong>
              </p>
            </div>
          </section>

          <section class="trace-fix-card trace-panel">
            <div class="trace-section-heading"><h2>失败原因与修复建议</h2></div>
            <div class="trace-fix-body">
              <p><span>主要问题</span><strong>{{ data.failureFix.problem }}</strong></p>
              <p><span>影响</span>{{ data.failureFix.impact }}</p>
              <ol><li v-for="suggestion in data.failureFix.suggestions" :key="suggestion">{{ suggestion }}</li></ol>
            </div>
          </section>

          <section class="trace-timeline-panel">
            <div class="trace-timeline-heading">
              <div><h2>运行 Trace</h2><span>{{ data.trace.runId }}</span></div>
              <div><strong>{{ data.trace.status }}</strong><time>{{ data.trace.completedAt }}</time></div>
            </div>
            <div class="trace-timeline">
              <article
                v-for="node in data.trace.nodes"
                :key="node.id"
                :data-state="node.tone"
                :data-selected="node.id === data.trace.selectedNodeId"
              >
                <span class="trace-node-marker"><Activity :size="13" /></span>
                <time>{{ node.time }}</time>
                <div class="trace-node-main">
                  <strong>{{ node.label }}</strong><small>{{ node.status }}</small><b>{{ node.duration }}</b><p>{{ node.summary }}</p>
                  <div v-if="node.scores" class="trace-score-tags"><span v-for="[label, value] in node.scores" :key="label">{{ label }} {{ value }}</span></div>
                </div>
                <button type="button" :disabled="node.tone === 'success'">{{ node.action }}</button>
              </article>
            </div>
          </section>

          <section class="trace-review-card trace-panel">
            <div class="trace-section-heading"><h2>人工复核区</h2><span>{{ data.review.status }}</span></div>
            <div class="trace-review-body">
              <p><span>复核状态</span><strong>{{ data.review.status }}</strong></p>
              <p><span>建议动作</span>{{ data.review.suggestedAction }}</p>
              <div>
                <button type="button"><CheckCircle2 :size="16" />{{ data.review.actions[0] }}</button>
                <button type="button" @click="emit('openView', 'batch')"><ListChecks :size="16" />{{ data.review.actions[1] }}</button>
              </div>
            </div>
          </section>

          <section class="trace-provider-card trace-panel">
            <div class="trace-section-heading"><h2>Provider 状态与边界</h2></div>
            <div class="trace-provider-grid">
              <p v-for="provider in data.providers.items" :key="provider.name"><KeyRound :size="13" /><span>{{ provider.name }}</span><strong :data-tone="provider.tone">{{ provider.status }}</strong></p>
              <div class="trace-boundary-note"><strong>边界说明</strong><span>{{ data.providers.boundary }}</span></div>
            </div>
          </section>

          <section class="trace-flow-card trace-panel">
            <h2>PromptOps 运行追踪链路</h2>
            <div class="trace-flow">
              <button v-for="(item, index) in data.flow" :key="item[0]" type="button" :data-current="item[0] === 'Failure Case'">
                <span>{{ item[0] }}</span><strong>{{ item[1] }}</strong><ArrowRight v-if="index < data.flow.length - 1" :size="14" />
              </button>
            </div>
          </section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.trace-shell { --trace-blue:#2563eb; --trace-green:#16a34a; --trace-red:#ef4444; --trace-amber:#d97706; --trace-border:#d7e0ec; display:grid; grid-template-columns:190px minmax(0,1fr); height:100vh; min-height:0; overflow:hidden; color:#111827; background:#f6f8fb; }
button,input { font:inherit; }
.trace-sidebar { display:grid; grid-template-rows:auto 1fr auto; height:100vh; border-right:1px solid var(--trace-border); background:#fff; }
.trace-brand { height:50px; display:flex; align-items:center; gap:10px; padding:0 12px; border-bottom:1px solid var(--trace-border); font-size:14px; }
.trace-brand button { display:grid; place-items:center; width:28px; height:28px; padding:0; color:var(--trace-blue); border:0; background:transparent; }
.trace-nav { display:flex; flex-direction:column; gap:3px; padding:8px; }
.trace-nav button { position:relative; display:flex; align-items:center; gap:9px; min-height:34px; padding:0 9px; color:#344054; border:0; border-radius:6px; background:transparent; font-size:12px; cursor:pointer; }
.trace-nav button:hover { background:#f2f5fa; }
.trace-nav button[aria-current="page"] { color:#175cd3; background:#eaf1fd; font-weight:700; }
.trace-nav button[aria-current="page"]::before { position:absolute; left:0; width:3px; height:22px; border-radius:2px; background:#2563eb; content:""; }
.trace-local-card { margin:8px; padding:10px; border:1px solid #cfdcf1; border-radius:8px; background:#f7faff; }
.trace-local-card>strong { color:#175cd3; font-size:12px; }
.trace-local-card p { display:flex; gap:6px; margin:5px 0; color:#475467; font-size:10px; line-height:1.35; }
.trace-local-card p svg { flex:none; color:#12a150; }
.trace-local-card small { margin-right:8px; color:#7b8797; font-size:9px; }
.trace-workspace { min-width:0; }
.trace-topbar { height:50px; display:grid; grid-template-columns:minmax(260px,1fr) auto auto auto; align-items:center; gap:10px; padding:0 15px; border-bottom:1px solid var(--trace-border); background:#fff; }
.trace-search { display:flex; align-items:center; gap:8px; max-width:600px; height:32px; padding:0 12px; color:#667085; border:1px solid #ccd7e5; border-radius:8px; background:#f8fafc; }
.trace-search input { width:100%; border:0; outline:0; background:transparent; color:#344054; }
.trace-status-cluster,.trace-user,.trace-top-actions { display:flex; align-items:center; gap:7px; white-space:nowrap; }
.trace-status-cluster span { padding:5px 8px; border-radius:6px; font-size:11px; font-weight:700; }
.trace-status-cluster [data-tone="green"]{color:#087a42;background:#e8f8ef}
.trace-status-cluster [data-tone="blue"]{color:#175cd3;background:#eaf1fd}
.trace-status-cluster [data-tone="purple"]{color:#6941c6;background:#f2ebff}
.trace-user { color:#667085; font-size:11px; }
.trace-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}
.trace-top-actions button { height:32px; display:flex; align-items:center; gap:7px; padding:0 10px; border-radius:7px; font-size:11px; font-weight:700; cursor:pointer; }
.trace-outline-action{color:#344054;border:1px solid #cbd7e6;background:#fff}
.trace-primary-action{color:#fff;border:1px solid #1d4ed8;background:#2563eb;box-shadow:0 2px 5px rgb(37 99 235 / 18%)}
.trace-main { height:calc(100vh - 50px); min-width:0; padding:10px 15px 12px; overflow:hidden; }
.trace-title-row { min-height:50px; display:flex; align-items:flex-start; justify-content:space-between; gap:18px; }
.trace-title-row>div{display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}
.trace-title-row span{text-transform:uppercase;color:#667085;font-size:11px;font-weight:700}
.trace-title-row h1{margin:0;font-size:25px;letter-spacing:0}
.trace-title-row div p{width:100%;margin:1px 0 0;color:#667085;font-size:10px}
.trace-context-strip { display:grid; grid-template-columns:.7fr .7fr 1fr 1fr 1fr .95fr 1.3fr; align-items:center; min-height:44px; margin-bottom:8px; overflow:hidden; border:1px solid var(--trace-border); border-radius:8px; background:#fff; }
.trace-context-strip div { min-width:0; padding:0 12px; border-left:1px solid #e6ebf2; }
.trace-context-strip div:first-child { border-left:0; }
.trace-context-strip span { display:block; color:#667085; font-size:9px; }
.trace-context-strip strong { display:block; overflow:hidden; margin-top:4px; font-family:"Cascadia Code",Consolas,monospace; font-size:10px; white-space:nowrap; text-overflow:ellipsis; }
.trace-context-strip [data-tone="warning"] strong { color:#d97706; font-family:inherit; }
.trace-detail-grid { display:grid; grid-template-columns:minmax(0,50fr) minmax(0,50fr); grid-template-rows:112px 148px 132px 132px 66px; grid-template-areas:"summary timeline" "prompt timeline" "schema timeline" "scorefix side" "flow flow"; gap:8px; }
.trace-panel { min-width:0; overflow:hidden; border:1px solid var(--trace-border); border-radius:8px; background:#fff; }
.trace-section-heading { height:30px; display:flex; align-items:center; justify-content:space-between; gap:10px; padding:0 10px; border-bottom:1px solid #edf1f6; }
.trace-section-heading h2 { margin:0; font-size:13px; }
.trace-section-heading span { overflow:hidden; color:#718096; font-size:9px; white-space:nowrap; text-overflow:ellipsis; }
.trace-summary-card { grid-area:summary; }
.trace-summary-body { display:grid; grid-template-rows:38px 1fr; height:calc(100% - 30px); }
.trace-summary-identity { display:grid; grid-template-columns:repeat(5,1fr); border-bottom:1px solid #edf1f5; }
.trace-summary-identity div, .trace-summary-highlights div { display:grid; align-content:center; min-width:0; padding:0 10px; border-right:1px solid #edf1f5; }
.trace-summary-identity div:last-child, .trace-summary-highlights div:last-child { border-right:0; }
.trace-summary-identity span, .trace-summary-highlights span { display:block; color:#667085; font-size:8px; }
.trace-summary-identity strong, .trace-summary-highlights strong { display:block; overflow:hidden; margin-top:3px; white-space:nowrap; text-overflow:ellipsis; }
.trace-summary-identity strong { font-family:"Cascadia Code",Consolas,monospace; font-size:10px; line-height:1.2; }
.trace-summary-highlights { display:grid; grid-template-columns:1fr 1fr 1fr; }
.trace-summary-highlights strong { font-size:12px; line-height:1.2; }
.trace-summary-highlights [data-tone="score"] strong { color:#087a42; font-family:"Cascadia Code",Consolas,monospace; font-size:15px; }
.trace-summary-highlights [data-tone="danger"] strong { color:#e04444; }
.trace-summary-highlights [data-tone="warning"] strong { color:#d97706; }
.trace-prompt-card { grid-area:prompt; }
.trace-prompt-layout { display:grid; grid-template-columns:1fr 1.38fr; height:calc(100% - 30px); }
.trace-input-box, .trace-prompt-preview { min-width:0; padding:8px 10px; }
.trace-input-box { border-right:1px solid #edf1f5; }
.trace-input-box span, .trace-prompt-preview span, .trace-schema-grid span { display:block; color:#667085; font-size:8px; font-weight:700; }
.trace-input-box p { margin:3px 0 7px; color:#344054; font-size:9px; line-height:1.35; }
.trace-variable-chips { display:flex; flex-wrap:wrap; gap:5px; margin:4px 0 6px; }
.trace-variable-chips b { padding:4px 7px; color:#175cd3; border-radius:5px; background:#eaf1fd; font-size:8px; }
.trace-variable-values p { margin:2px 0; color:#344054; font-size:8px; }
.trace-prompt-preview>div { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:7px; padding-bottom:6px; border-bottom:1px solid #e6ebf2; }
.trace-prompt-preview strong { font-family:"Cascadia Code",Consolas,monospace; font-size:9px; }
.trace-prompt-preview pre, .trace-schema-grid pre { margin:4px 0 0; overflow:hidden; color:#dbeafe; border:1px solid #25496d; border-radius:6px; background:#0f223b; font-family:"Cascadia Code",Consolas,monospace; white-space:pre-wrap; }
.trace-prompt-preview pre { height:72px; padding:8px; font-size:9px; line-height:1.45; }
.trace-schema-card { grid-area:schema; }
.trace-schema-grid { display:grid; grid-template-columns:1fr 1.35fr 1fr .95fr; height:calc(100% - 30px); }
.trace-schema-grid section { min-width:0; padding:7px 8px; border-right:1px solid #edf1f5; }
.trace-schema-grid section:last-child { border-right:0; }
.trace-schema-grid pre { height:76px; padding:6px; font-size:7px; line-height:1.4; }
.trace-checks p { display:flex; align-items:center; gap:5px; margin:5px 0; color:#159447; font-size:8px; font-weight:700; }
.trace-checks p[data-state="fail"] { color:#e04444; }
.trace-score-card { grid-area:scorefix; width:58%; }
.trace-fix-card { grid-area:scorefix; justify-self:end; width:calc(42% - 4px); }
.trace-score-grid { display:grid; grid-template-columns:repeat(6,1fr); height:34px; border-bottom:1px solid #edf1f5; }
.trace-score-grid div { min-width:0; padding:5px 6px; border-right:1px solid #edf1f5; text-align:center; }
.trace-score-grid span, .trace-score-meta span, .trace-fix-body span { display:block; color:#667085; font-size:7px; }
.trace-score-grid strong { display:block; margin-top:2px; font-family:"Cascadia Code",Consolas,monospace; font-size:10px; }
.trace-total-score strong { color:#087a42; }
.trace-score-meta { display:grid; grid-template-columns:repeat(4,1fr); height:26px; }
.trace-score-meta p { min-width:0; margin:0; padding:5px 6px; border-right:1px solid #edf1f5; }
.trace-score-meta strong { display:block; overflow:hidden; margin-top:2px; font-family:"Cascadia Code",Consolas,monospace; font-size:8px; white-space:nowrap; text-overflow:ellipsis; }
.trace-score-meta [data-danger="true"] strong { color:#e04444; }
.trace-fix-body { display:grid; grid-template-columns:1fr 1.15fr; gap:0; height:calc(100% - 30px); }
.trace-fix-body p { margin:0; padding:8px 8px; color:#344054; border-right:1px solid #edf1f5; font-size:8px; line-height:1.35; }
.trace-fix-body p strong { display:block; margin-top:3px; color:#e04444; font-size:10px; }
.trace-fix-body ol { display:grid; grid-template-columns:1fr 1fr; gap:2px 12px; margin:0; padding:8px 10px 8px 24px; color:#344054; font-size:7px; line-height:1.3; }
.trace-timeline-panel { grid-area:timeline; min-width:0; padding:10px 12px; border:1px solid #16355a; border-radius:9px; background:#0f223b; color:#fff; box-shadow:0 5px 14px rgb(15 34 59 / 15%); }
.trace-timeline-heading { height:30px; display:flex; justify-content:space-between; align-items:flex-start; }
.trace-timeline-heading>div { display:flex; align-items:center; gap:10px; }
.trace-timeline-heading h2 { margin:0; font-size:14px; }
.trace-timeline-heading span,.trace-timeline-heading time { color:#a8bdd6; font-family:"Cascadia Code",Consolas,monospace; font-size:9px; }
.trace-timeline-heading strong { padding:4px 7px; color:#86efac; border-radius:5px; background:rgba(22,163,74,.18); font-size:9px; }
.trace-timeline { position:relative; display:grid; gap:4px; padding-left:28px; }
.trace-timeline::before { position:absolute; top:12px; bottom:14px; left:8px; width:1px; background:#456584; content:""; }
.trace-timeline article { position:relative; display:grid; grid-template-columns:58px minmax(0,1fr) 70px; align-items:center; gap:7px; min-height:39px; padding:4px 6px; border:1px solid #294a6d; border-radius:6px; background:rgba(255,255,255,.035); }
.trace-node-marker { position:absolute; left:-30px; z-index:1; display:grid; place-items:center; width:18px; height:18px; color:#67d69c; border:1px solid #67d69c; border-radius:50%; background:#0f223b; }
.trace-timeline article[data-state="danger"] { border-color:#ef4444; background:rgba(127,29,29,.28); }
.trace-timeline article[data-state="danger"] .trace-node-marker { color:#ef4444; border-color:#ef4444; }
.trace-timeline article[data-state="warning"] .trace-node-marker { color:#f59e0b; border-color:#f59e0b; }
.trace-timeline article[data-selected="true"] { box-shadow:inset 0 0 0 1px #ef4444; }
.trace-timeline time { color:#91aac4; font-family:"Cascadia Code",Consolas,monospace; font-size:8px; }
.trace-node-main { display:grid; grid-template-columns:100px 52px 48px minmax(0,1fr); align-items:center; gap:6px; min-width:0; }
.trace-node-main strong { font-size:10px; }
.trace-node-main small { color:#91aac4; font-size:8px; }
.trace-node-main b { color:#c2d0df; font-family:"Cascadia Code",Consolas,monospace; font-size:8px; }
.trace-node-main p { overflow:hidden; margin:0; color:#c2d0df; font-size:9px; white-space:nowrap; text-overflow:ellipsis; }
.trace-score-tags { grid-column:4; display:flex; gap:3px; overflow:hidden; }
.trace-score-tags span { flex:0 0 auto; padding:2px 4px; color:#dbeafe; border:1px solid #42617f; border-radius:4px; font-size:7px; }
.trace-timeline button { min-height:23px; padding:0 8px; color:#dbeafe; border:1px solid #42617f; border-radius:5px; background:#142b49; font-size:8px; cursor:pointer; }
.trace-timeline button:disabled { opacity:.55; }
.trace-review-card { grid-area:side; width:44%; }
.trace-review-body { display:grid; align-content:center; gap:5px; height:calc(100% - 30px); padding:6px 8px; }
.trace-review-body p { margin:0; padding:0; color:#344054; font-size:9px; line-height:1.35; }
.trace-review-body p strong { display:block; margin-top:3px; color:#d97706; }
.trace-review-body>div { display:flex; gap:8px; padding:0; }
.trace-review-body button { flex:1; display:flex; align-items:center; justify-content:center; gap:6px; height:30px; min-width:0; padding:0 8px; color:#175cd3; border:1px solid #8fb5fb; border-radius:7px; background:#fff; font-size:9px; font-weight:700; cursor:pointer; }
.trace-review-body button:last-child { color:#fff; border-color:#1d4ed8; background:#2563eb; }
.trace-provider-card { grid-area:side; justify-self:end; width:calc(56% - 4px); }
.trace-provider-grid { display:grid; grid-template-columns:1fr 1fr; align-items:center; height:calc(100% - 30px); }
.trace-provider-grid p { display:flex; align-items:center; gap:7px; min-width:0; margin:0; padding:0 10px; color:#526174; font-size:8px; }
.trace-provider-grid p span { overflow:hidden; white-space:nowrap; text-overflow:ellipsis; }
.trace-provider-grid p strong { margin-left:auto; white-space:nowrap; }
.trace-provider-grid [data-tone="green"] { color:#159447; }
.trace-provider-grid [data-tone="blue"] { color:#2563eb; }
.trace-provider-grid [data-tone="gray"] { color:#667085; }
.trace-boundary-note { grid-column:1 / -1; display:grid; align-content:center; gap:3px; align-self:stretch; margin:0 8px 7px; padding:6px 9px; color:#175cd3; border:1px solid #bed3f6; border-radius:6px; background:#eff5ff; font-size:8px; line-height:1.35; }
.trace-boundary-note strong { font-size:10px; }
.trace-flow-card { grid-area:flow; display:grid; grid-template-columns:180px 1fr; align-items:center; }
.trace-flow-card h2 { margin:0; padding-left:12px; font-size:12px; }
.trace-flow { display:grid; grid-template-columns:repeat(7,1fr); height:100%; }
.trace-flow button { position:relative; display:flex; flex-direction:column; justify-content:center; min-width:0; padding:0 9px; border:0; border-left:1px solid #e5ebf2; background:#fff; }
.trace-flow button[data-current="true"] { color:#e04444; background:#fff7f7; box-shadow:inset 0 0 0 1px #f3b8b8; }
.trace-flow span { color:#7b8797; font-size:7px; }
.trace-flow strong { margin-top:2px; font-size:9px; }
.trace-flow svg { position:absolute; right:-8px; z-index:1; color:#8ba0b9; background:#fff; }
@media (max-width:1180px) {
  .trace-shell { display:block; height:auto; overflow:visible; }
  .trace-sidebar { height:auto; }
  .trace-nav { display:grid; grid-template-columns:repeat(3,1fr); }
  .trace-local-card { display:none; }
  .trace-topbar { height:auto; grid-template-columns:1fr auto; padding:10px 14px; }
  .trace-status-cluster,.trace-user { display:none; }
  .trace-main { height:auto; overflow:visible; }
  .trace-context-strip,.trace-detail-grid,.trace-prompt-layout,.trace-schema-grid,.trace-review-body,.trace-provider-grid,.trace-flow-card { grid-template-columns:1fr; }
  .trace-detail-grid { display:block; }
  .trace-panel,.trace-timeline-panel { margin-bottom:10px; }
  .trace-score-card,.trace-fix-card,.trace-review-card,.trace-provider-card { width:100%; justify-self:stretch; }
}
</style>
