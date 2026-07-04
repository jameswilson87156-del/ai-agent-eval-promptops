<script setup lang="ts">
import { computed, ref } from 'vue'
import { Activity, AlertTriangle, ArrowRight, BarChart3, CheckCircle2, CircleDashed, Code2, Database, FileText, GitCompare, Layers3, Search, ShieldCheck, SlidersHorizontal, UserRound } from 'lucide-vue-next'
import { evalDashboardMockData } from '../data/evalDashboardMockData'
import type { PromptProfile } from '../types'

type DashboardView = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'run' | 'versions' | 'review'

const props = defineProps<{ activePromptId: number | null; prompts: PromptProfile[]; runningEval: boolean }>()
const emit = defineEmits<{ openView: [view: DashboardView]; runEval: []; selectPrompt: [prompt: PromptProfile] }>()

const data = evalDashboardMockData
const dashboardSearch = ref('')
const promptOptions = computed(() => props.prompts.slice(0, 3))
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

const traceStages = ['Input', 'Prompt Build', 'Mock Output', 'Rule Evaluator', 'Score', 'Failure Case', 'Human Review']
</script>

<template>
  <div class="eval-dashboard-shell">
    <aside class="eval-sidebar">
      <div class="eval-brand"><button type="button" aria-label="展开导航"><Layers3 :size="18" /></button><strong>PromptOps Studio</strong></div>
      <nav class="eval-nav"><button v-for="item in navItems" :key="item.id" type="button" :aria-current="item.id === 'dashboard' ? 'page' : undefined" :data-view="item.view" @click="emit('openView', item.view)"><component :is="item.icon" :size="17" /><span>{{ item.label }}</span></button></nav>
      <section class="local-demo-card"><strong>本地演示环境</strong><p><CheckCircle2 :size="14" />Mock Output + Rule-based Eval</p><p><CheckCircle2 :size="14" />无需 API Key</p><small>非生产环境</small></section>
    </aside>
    <section class="eval-workspace">
      <header class="eval-topbar">
        <label class="dashboard-search"><Search :size="17" /><span class="sr-only">搜索 Prompt、Run 或 Dataset</span><input v-model="dashboardSearch" type="search" data-testid="global-search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索 Prompt / Run / Dataset" /></label>
        <div class="status-cluster"><span data-tone="green">本地演示</span><span data-tone="blue">规则评测</span><span data-tone="purple">无需 API Key</span></div>
        <div class="topbar-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <div class="topbar-actions"><button class="outline-action" type="button" @click="emit('openView', 'dataset')"><Database :size="16" />查看测试集</button><button class="primary-action" type="button" :disabled="runningEval" @click="emit('runEval')">运行评测</button></div>
      </header>
      <main id="main" class="eval-dashboard-main" tabindex="-1">
        <section class="dashboard-title-row"><div><h1>PromptOps Studio</h1><p>基于本地测试集、Mock Output 与 Rule-based Eval 的 Prompt 质量工作台。</p></div><div class="prompt-switch-row"><button v-for="prompt in promptOptions" :key="prompt.id" type="button" :aria-pressed="prompt.id === activePromptId" @click="emit('selectPrompt', prompt)">{{ prompt.name }}</button></div></section>
        <section class="dashboard-grid">
          <section class="experiment-card panel"><h2>当前实验</h2><div><span>Dataset</span><strong>{{ data.experiment.dataset }}</strong></div><div><span>Prompt</span><strong>{{ data.experiment.prompt }}</strong></div><div><span>规则集</span><strong>{{ data.experiment.ruleSet }}</strong></div><div><span>样本数</span><strong>{{ data.experiment.sampleCount }}</strong></div></section>
          <section class="metrics-block panel"><h2>核心指标</h2><article v-for="metric in data.metrics" :key="metric.label"><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.helper }}</small></article></section>
          <section class="health-card panel"><h2>运行健康度</h2><div v-for="item in data.health" :key="item.label"><span>{{ item.label }}</span><meter min="0" max="100" :value="item.value"></meter><strong>{{ item.value }}%</strong></div></section>
          <section class="recent-runs-card panel"><h2>最近 Run</h2><div class="runs-head"><span>Run</span><span>Dataset</span><span>Prompt</span><span>通过率</span><span>状态</span><span>耗时</span></div><div v-for="run in data.recentRuns" :key="run.runId"><span>{{ run.runId }}</span><span>{{ run.dataset }}</span><span>{{ run.prompt }}</span><strong>{{ run.passRate }}</strong><b>{{ run.status }}</b><span>{{ run.duration }}</span></div></section>
          <section class="trace-master-panel panel"><h2>PromptOps 评测闭环</h2><div class="trace-node" v-for="stage in traceStages" :key="stage" :data-current="stage === 'Failure Case'"><span>{{ stage }}</span><ArrowRight :size="14" /></div></section>
          <section class="failure-card panel"><h2>失败热点</h2><p v-for="item in data.failureReasons" :key="item.reason"><strong>{{ item.count }}</strong><span>{{ item.reason }}</span><b>{{ item.ratio }}</b></p></section>
          <section class="provider-card panel"><h2>Provider 状态</h2><p v-for="provider in data.providers" :key="provider.name"><span>{{ provider.name }}</span><strong>{{ provider.status }}</strong></p></section>
          <section class="loop-card panel"><h2>下一步</h2><p>从失败样本进入 Trace Detail，修复 Prompt 后重新运行批量评测。</p><button type="button" @click="emit('openView', 'failure')">查看失败归因</button></section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.eval-dashboard-shell{display:grid;grid-template-columns:190px minmax(0,1fr);height:100vh;overflow:hidden;color:#111827;background:#f6f8fb;--line:#d7e0ec;--blue:#2563eb;--green:#16a34a;--red:#ef4444}.eval-sidebar{display:grid;grid-template-rows:auto 1fr auto;height:100vh;border-right:1px solid var(--line);background:#fff}.eval-brand{height:50px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--line);font-size:14px}.eval-brand button{display:grid;place-items:center;width:28px;height:28px;border:0;background:transparent;color:var(--blue)}.eval-nav{display:flex;flex-direction:column;gap:3px;padding:8px}.eval-nav button{position:relative;display:flex;align-items:center;gap:9px;height:34px;padding:0 9px;color:#344054;border:0;border-radius:6px;background:transparent;font-size:12px;cursor:pointer}.eval-nav button[aria-current=page]{color:#175cd3;background:#eaf1fd;font-weight:700}.eval-nav button[aria-current=page]:before{position:absolute;left:0;width:3px;height:22px;background:#2563eb;content:""}.local-demo-card{margin:8px;padding:10px;border:1px solid #cfdcf1;border-radius:8px;background:#f7faff}.local-demo-card strong{color:#175cd3;font-size:12px}.local-demo-card p{display:flex;gap:6px;margin:5px 0;color:#475467;font-size:10px}.local-demo-card small{font-size:9px;color:#7b8797}
.eval-topbar{height:50px;display:grid;grid-template-columns:minmax(260px,1fr) auto auto auto;align-items:center;gap:10px;padding:0 15px;border-bottom:1px solid var(--line);background:#fff}.dashboard-search{display:flex;align-items:center;gap:8px;height:32px;padding:0 12px;color:#667085;border:1px solid #ccd7e5;border-radius:8px;background:#f8fafc}.dashboard-search input{width:100%;border:0;outline:0;background:transparent}.status-cluster,.topbar-user,.topbar-actions{display:flex;align-items:center;gap:7px;white-space:nowrap}.status-cluster span{padding:5px 8px;border-radius:6px;color:#175cd3;background:#eaf1fd;font-size:11px;font-weight:700}.status-cluster [data-tone=green]{color:#087a42;background:#e8f8ef}.status-cluster [data-tone=purple]{color:#6941c6;background:#f2ebff}.topbar-user{color:#667085;font-size:11px}.topbar-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}.topbar-actions button{display:flex;align-items:center;gap:6px;height:32px;padding:0 10px;border-radius:7px;font-size:11px;font-weight:700}.outline-action{color:#344054;border:1px solid #cbd7e6;background:#fff}.primary-action{color:#fff;border:1px solid #1d4ed8;background:#2563eb}
.eval-dashboard-main{height:calc(100vh - 50px);padding:10px 15px 12px;overflow:hidden}.dashboard-title-row{height:58px;display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.dashboard-title-row h1{margin:0;font-size:25px}.dashboard-title-row p{margin:2px 0 0;color:#667085;font-size:10px}.prompt-switch-row{display:flex;gap:6px}.prompt-switch-row button{height:30px;padding:0 10px;border:1px solid #cbd7e6;border-radius:7px;background:#fff;font-size:10px}.prompt-switch-row button[aria-pressed=true]{color:#175cd3;border-color:#8fb5fb;background:#eaf1fd}.dashboard-grid{display:grid;grid-template-columns:.85fr 1.15fr 1fr;grid-template-rows:148px 198px 156px 66px;gap:8px;grid-template-areas:"experiment metrics health" "runs runs trace" "failure provider trace" "loop loop loop"}.panel{min-width:0;overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#fff}.panel h2{height:30px;margin:0;padding:0 10px;border-bottom:1px solid #edf1f6;font-size:13px;line-height:30px}.experiment-card{grid-area:experiment}.experiment-card div{display:grid;grid-template-columns:68px 1fr;height:28px;align-items:center;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:10px}.experiment-card span{color:#667085}.metrics-block{grid-area:metrics;display:grid;grid-template-columns:repeat(4,1fr)}.metrics-block h2{grid-column:1/-1}.metrics-block article{padding:13px 10px;border-right:1px solid #edf1f5}.metrics-block span{color:#667085;font-size:10px}.metrics-block strong{display:block;margin:8px 0;font-family:"Cascadia Code",Consolas,monospace;font-size:22px}.metrics-block small{color:#667085}.health-card{grid-area:health}.health-card div{display:grid;grid-template-columns:1fr 1.2fr 46px;gap:8px;align-items:center;height:28px;padding:0 10px;font-size:10px}.health-card meter{width:100%;height:8px}.recent-runs-card{grid-area:runs}.recent-runs-card>div{display:grid;grid-template-columns:.9fr 1fr 1fr .65fr .65fr .65fr;align-items:center;height:27px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.runs-head{color:#667085;font-weight:700;background:#f8fafc}.trace-master-panel{grid-area:trace;padding-bottom:8px}.trace-master-panel h2{margin-bottom:8px}.trace-node{display:grid;grid-template-columns:1fr 22px;align-items:center;height:37px;margin:0 10px 6px;padding:0 10px;border:1px solid #edf1f6;border-radius:7px;font-size:10px}.trace-node[data-current=true]{color:#ef4444;border-color:#f5b8b8;background:#fff7f7}.failure-card{grid-area:failure}.failure-card p{display:grid;grid-template-columns:50px 1fr 54px;align-items:center;height:28px;margin:0;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:10px}.failure-card strong{color:#ef4444}.failure-card b{color:#667085}.provider-card{grid-area:provider}.provider-card p{display:grid;grid-template-columns:1fr 90px;align-items:center;height:28px;margin:0;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:10px}.provider-card strong{color:#087a42}.loop-card{grid-area:loop;display:grid;grid-template-columns:180px 1fr auto;align-items:center}.loop-card h2{border-bottom:0}.loop-card p{margin:0;color:#526174;font-size:10px}.loop-card button{height:30px;margin-right:12px;padding:0 10px;color:#175cd3;border:1px solid #8fb5fb;border-radius:7px;background:#fff;font-size:10px;font-weight:700}
@media(max-width:1180px){.eval-dashboard-shell{display:block;height:auto;overflow:visible}.eval-sidebar{height:auto}.eval-nav{display:grid;grid-template-columns:repeat(3,1fr)}.local-demo-card{display:none}.eval-topbar{height:auto;grid-template-columns:1fr auto;padding:10px 14px}.status-cluster,.topbar-user{display:none}.eval-dashboard-main{height:auto;overflow:visible}.dashboard-grid{display:block}.panel{margin-bottom:10px}}
</style>
