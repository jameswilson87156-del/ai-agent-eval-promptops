<script setup lang="ts">
import { ref } from 'vue'
import { AlertTriangle, ArrowRight, BarChart3, CheckCircle2, CircleDashed, Code2, Database, FileText, GitCompare, Layers3, Search, ShieldCheck, SlidersHorizontal, UserRound } from 'lucide-vue-next'
import { systemBoundaryMockData } from '../data/systemBoundaryMockData'

type BoundaryView = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'run' | 'versions' | 'review'

defineProps<{ runningEval: boolean }>()
const emit = defineEmits<{ openView: [view: BoundaryView]; runEval: [] }>()

const data = systemBoundaryMockData
const boundarySearch = ref('')
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
  <div class="boundary-shell">
    <aside class="boundary-sidebar">
      <div class="boundary-brand"><button type="button" aria-label="展开导航"><Layers3 :size="18" /></button><strong>PromptOps Studio</strong></div>
      <nav class="boundary-nav"><button v-for="item in navItems" :key="item.id" type="button" :aria-current="item.id === 'boundary' ? 'page' : undefined" :data-view="item.view" @click="emit('openView', item.view)"><component :is="item.icon" :size="17" /><span>{{ item.label }}</span></button></nav>
      <section class="boundary-local-card"><strong>本地演示环境</strong><p><CheckCircle2 :size="14" />能力边界显式说明</p><p><CheckCircle2 :size="14" />无真实业务数据</p><small>非生产环境</small></section>
    </aside>
    <section class="boundary-workspace">
      <header class="boundary-topbar">
        <label class="boundary-search"><Search :size="17" /><span class="sr-only">搜索 Prompt、Run 或 Dataset</span><input v-model="boundarySearch" type="search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索能力边界 / 模块 / 风险" /></label>
        <div class="boundary-pills"><span>Local Demo</span><span>Mock Output</span><span>Rule-based Eval</span></div>
        <div class="boundary-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <button class="boundary-primary" type="button" :disabled="runningEval" @click="emit('openView', 'overview')">回到总览</button>
      </header>
      <main id="main" class="boundary-main" tabindex="-1">
        <header class="boundary-title"><span>{{ data.page.eyebrow }}</span><h1>{{ data.page.title }}</h1><p>{{ data.page.subtitle }}</p></header>
        <section class="boundary-summary">
          <article v-for="[label, value, helper] in data.summary" :key="label" class="boundary-card"><span>{{ label }}</span><strong>{{ value }}</strong><small>{{ helper }}</small></article>
        </section>
        <section class="boundary-grid">
          <section class="boundary-scope boundary-card">
            <div class="boundary-heading"><h2>当前能力范围</h2><span>In Scope</span></div>
            <div v-for="[title, desc] in data.inScope" :key="title"><CheckCircle2 :size="15" /><strong>{{ title }}</strong><span>{{ desc }}</span></div>
          </section>
          <section class="boundary-scope boundary-card" data-out>
            <div class="boundary-heading"><h2>明确不在范围</h2><span>Out of Scope</span></div>
            <div v-for="[title, desc] in data.outOfScope" :key="title"><AlertTriangle :size="15" /><strong>{{ title }}</strong><span>{{ desc }}</span></div>
          </section>
          <section class="boundary-security boundary-card">
            <div class="boundary-heading"><h2>数据与安全边界</h2><span>Safety</span></div>
            <div v-for="[title, desc] in data.security" :key="title"><span>{{ title }}</span><p>{{ desc }}</p></div>
          </section>
          <section class="boundary-matrix boundary-card">
            <div class="boundary-heading"><h2>功能边界矩阵</h2><span>{{ data.matrix.length }} 项</span></div>
            <div class="boundary-matrix-head"><span>模块</span><span>状态</span><span>说明</span></div>
            <div v-for="row in data.matrix" :key="row[0]" class="boundary-matrix-row"><strong>{{ row[0] }}</strong><b :data-missing="row[1] === '未接入'">{{ row[1] }}</b><span>{{ row[2] }}</span></div>
          </section>
          <section class="boundary-next boundary-card">
            <div class="boundary-heading"><h2>下一阶段路线</h2><span>Roadmap</span></div>
            <div v-for="[title, desc] in data.next" :key="title"><strong>{{ title }}</strong><span>{{ desc }}</span></div>
          </section>
          <section class="boundary-flow boundary-card">
            <h2>边界说明链路</h2>
            <div><button type="button" data-current="true"><span>Local Demo</span><strong>本地演示</strong><ArrowRight :size="14" /></button><button type="button"><span>Mock Output</span><strong>模拟输出</strong><ArrowRight :size="14" /></button><button type="button"><span>Rule-based Eval</span><strong>规则评测</strong><ArrowRight :size="14" /></button><button type="button"><span>Human Review</span><strong>人工复核</strong><ArrowRight :size="14" /></button><button type="button"><span>Production</span><strong>未接入</strong></button></div>
          </section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.boundary-shell{display:grid;grid-template-columns:190px minmax(0,1fr);height:100vh;overflow:hidden;color:#111827;background:#f6f8fb;--line:#d7e0ec;--blue:#2563eb;--green:#16a34a;--red:#ef4444}.boundary-sidebar{display:grid;grid-template-rows:auto 1fr auto;height:100vh;border-right:1px solid var(--line);background:#fff}.boundary-brand{height:50px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--line);font-size:14px}.boundary-brand button{display:grid;place-items:center;width:28px;height:28px;border:0;background:transparent;color:var(--blue)}.boundary-nav{display:flex;flex-direction:column;gap:3px;padding:8px}.boundary-nav button{position:relative;display:flex;align-items:center;gap:9px;height:34px;padding:0 9px;color:#344054;border:0;border-radius:6px;background:transparent;font-size:12px;cursor:pointer}.boundary-nav button[aria-current=page]{color:#175cd3;background:#eaf1fd;font-weight:700}.boundary-nav button[aria-current=page]:before{position:absolute;left:0;width:3px;height:22px;background:#2563eb;content:""}.boundary-local-card{margin:8px;padding:10px;border:1px solid #cfdcf1;border-radius:8px;background:#f7faff}.boundary-local-card strong{color:#175cd3;font-size:12px}.boundary-local-card p{display:flex;gap:6px;margin:5px 0;color:#475467;font-size:10px}.boundary-local-card small{font-size:9px;color:#7b8797}
.boundary-topbar{height:50px;display:grid;grid-template-columns:minmax(260px,1fr) auto auto auto;align-items:center;gap:10px;padding:0 15px;border-bottom:1px solid var(--line);background:#fff}.boundary-search{display:flex;align-items:center;gap:8px;height:32px;padding:0 12px;color:#667085;border:1px solid #ccd7e5;border-radius:8px;background:#f8fafc}.boundary-search input{width:100%;border:0;outline:0;background:transparent}.boundary-pills,.boundary-user{display:flex;align-items:center;gap:7px;white-space:nowrap}.boundary-pills span{padding:5px 8px;border-radius:6px;color:#175cd3;background:#eaf1fd;font-size:11px;font-weight:700}.boundary-pills span:first-child{color:#087a42;background:#e8f8ef}.boundary-pills span:last-child{color:#6941c6;background:#f2ebff}.boundary-user{color:#667085;font-size:11px}.boundary-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}.boundary-primary{height:32px;padding:0 12px;color:#fff;border:1px solid #1d4ed8;border-radius:7px;background:#2563eb;font-size:11px;font-weight:700}
.boundary-main{height:calc(100vh - 50px);padding:10px 15px 12px;overflow:hidden}.boundary-title{height:50px;display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}.boundary-title span{color:#667085;font-size:11px;font-weight:700;text-transform:uppercase}.boundary-title h1{margin:0;font-size:25px}.boundary-title p{width:100%;margin:-2px 0 0;color:#667085;font-size:10px}.boundary-card{min-width:0;overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#fff}.boundary-summary{display:grid;grid-template-columns:repeat(5,1fr);gap:8px;height:82px;margin-bottom:8px}.boundary-summary article{padding:11px 12px}.boundary-summary span{color:#667085;font-size:10px}.boundary-summary strong{display:block;margin-top:7px;font-size:18px}.boundary-summary small{color:#667085;font-size:9px}.boundary-grid{display:grid;grid-template-columns:1fr 1fr 1fr;grid-template-rows:198px 292px 66px;grid-template-areas:"in out security" "matrix matrix next" "flow flow flow";gap:8px}.boundary-heading{height:30px;display:flex;align-items:center;justify-content:space-between;padding:0 10px;border-bottom:1px solid #edf1f6}.boundary-heading h2{margin:0;font-size:13px}.boundary-heading span{color:#718096;font-size:9px}.boundary-scope:first-child{grid-area:in}.boundary-scope[data-out]{grid-area:out}.boundary-scope>div:not(.boundary-heading){display:grid;grid-template-columns:24px 1fr;gap:2px 7px;padding:8px 10px;border-bottom:1px solid #edf1f5}.boundary-scope svg{grid-row:1/3;color:#087a42}.boundary-scope[data-out] svg{color:#ef4444}.boundary-scope strong{font-size:10px}.boundary-scope span{color:#667085;font-size:9px}.boundary-security{grid-area:security}.boundary-security>div:not(.boundary-heading){display:grid;grid-template-columns:90px 1fr;gap:8px;padding:8px 10px;border-bottom:1px solid #edf1f5;font-size:9px}.boundary-security span{font-weight:700}.boundary-security p{margin:0;color:#667085;line-height:1.35}.boundary-matrix{grid-area:matrix}.boundary-matrix-head,.boundary-matrix-row{display:grid;grid-template-columns:1.25fr .55fr 2fr;align-items:center;min-height:24px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.boundary-matrix-head{color:#667085;font-weight:700;background:#f8fafc}.boundary-matrix-row b{color:#087a42}.boundary-matrix-row b[data-missing=true]{color:#ef4444}.boundary-matrix-row span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.boundary-next{grid-area:next}.boundary-next>div:not(.boundary-heading){display:grid;grid-template-columns:100px 1fr;gap:8px;padding:12px 10px;border-bottom:1px solid #edf1f5;font-size:9px}.boundary-next strong{font-size:10px}.boundary-next span{color:#667085}.boundary-flow{grid-area:flow;display:grid;grid-template-columns:180px 1fr;align-items:center}.boundary-flow h2{margin:0;padding-left:12px;font-size:12px}.boundary-flow>div{display:grid;grid-template-columns:repeat(5,1fr);height:100%}.boundary-flow button{position:relative;display:flex;flex-direction:column;justify-content:center;padding:0 10px;border:0;border-left:1px solid #e5ebf2;background:#fff}.boundary-flow button[data-current=true]{color:#087a42;background:#f0fdf4;box-shadow:inset 0 0 0 1px #b7e4c7}.boundary-flow span{color:#7b8797;font-size:7px}.boundary-flow strong{margin-top:2px;font-size:9px}.boundary-flow svg{position:absolute;right:-8px;background:#fff;color:#8ba0b9}
@media(max-width:1180px){.boundary-shell{display:block;height:auto;overflow:visible}.boundary-sidebar{height:auto}.boundary-nav{display:grid;grid-template-columns:repeat(3,1fr)}.boundary-local-card{display:none}.boundary-topbar{height:auto;grid-template-columns:1fr auto;padding:10px 14px}.boundary-pills,.boundary-user{display:none}.boundary-main{height:auto;overflow:visible}.boundary-summary,.boundary-grid{display:block}.boundary-card{margin-bottom:10px}}
</style>
