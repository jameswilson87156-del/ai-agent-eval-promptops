<script setup lang="ts">
import { ref } from 'vue'
import { AlertTriangle, ArrowRight, BarChart3, CheckCircle2, CircleDashed, Code2, Database, FileText, GitCompare, Layers3, Search, ShieldCheck, SlidersHorizontal, UserRound } from 'lucide-vue-next'
import { failureCasesMockData } from '../data/failureCasesMockData'

type FailureView = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'run' | 'versions' | 'review'

defineProps<{ runningEval: boolean }>()
const emit = defineEmits<{ openView: [view: FailureView]; runEval: [] }>()

const data = failureCasesMockData
const failureSamples = data.cases.slice(0, 7)
const failureSearch = ref('')
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
  <div class="failure-shell">
    <aside class="failure-sidebar">
      <div class="failure-brand"><button type="button" aria-label="展开导航"><Layers3 :size="18" /></button><strong>PromptOps Studio</strong></div>
      <nav class="failure-nav"><button v-for="item in navItems" :key="item.id" type="button" :aria-current="item.id === 'failure' ? 'page' : undefined" :data-view="item.view" @click="emit('openView', item.view)"><component :is="item.icon" :size="17" /><span>{{ item.label }}</span></button></nav>
      <section class="failure-local-card"><strong>本地演示环境</strong><p><CheckCircle2 :size="14" />失败归因来自规则集</p><p><CheckCircle2 :size="14" />可回跳 Trace 详情</p><small>非生产环境</small></section>
    </aside>
    <section class="failure-workspace">
      <header class="failure-topbar">
        <label class="failure-search"><Search :size="17" /><span class="sr-only">搜索 Prompt、Run 或 Dataset</span><input v-model="failureSearch" type="search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索失败样本 / Prompt / Dataset" /></label>
        <div class="failure-pills"><span>本地演示</span><span>规则评测</span><span>无需 API Key</span></div>
        <div class="failure-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <button class="failure-primary" type="button" :disabled="runningEval" @click="emit('runEval')">重新运行评测</button>
      </header>
      <main id="main" class="failure-main" tabindex="-1">
        <header class="failure-title"><span>{{ data.page.eyebrow }}</span><h1>{{ data.page.title }}</h1><p>{{ data.page.subtitle }}</p></header>
        <section class="failure-grid">
          <section class="failure-metrics">
            <article v-for="metric in data.metrics" :key="metric.label" class="failure-card" :data-tone="metric.tone"><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.helper }}</small></article>
          </section>
          <section class="failure-table failure-card">
            <div class="failure-heading"><h2>失败样本列表</h2><span>{{ failureSamples.length }} 条样本</span></div>
            <div class="failure-table-head"><span>Case ID</span><span>Dataset</span><span>Prompt</span><span>根因</span><span>风险</span><span>状态</span><span>时间</span></div>
            <div v-for="row in failureSamples" :key="row[0]" class="failure-table-row"><span v-for="cell in row" :key="cell">{{ cell }}</span></div>
          </section>
          <section class="failure-distribution failure-card">
            <div class="failure-heading"><h2>失败原因分布</h2><span>Top 5</span></div>
            <div v-for="[label, count, percent] in data.distribution" :key="label"><span>{{ label }}</span><meter min="0" max="40" :value="count"></meter><strong>{{ count }}</strong><b>{{ percent }}</b></div>
          </section>
          <section class="failure-insight failure-card">
            <div class="failure-heading"><h2>选中案例根因</h2><span>{{ data.insight.caseId }}</span></div>
            <dl>
              <dt>主要问题</dt><dd>{{ data.insight.problem }}</dd>
              <dt>期望输出</dt><dd><code>{{ data.insight.expected }}</code></dd>
              <dt>修复建议</dt><dd>{{ data.insight.fix }}</dd>
              <dt>Prompt</dt><dd>{{ data.insight.prompt }}</dd>
              <dt>根因分类</dt><dd>{{ data.insight.reason }}</dd>
              <dt>已回归</dt><dd>{{ data.insight.regressed }}</dd>
            </dl>
            <button type="button" @click="emit('openView', 'trace')"><CircleDashed :size="16" />打开 Trace Detail</button>
          </section>
          <section class="failure-tasks failure-card">
            <div class="failure-heading"><h2>修复任务</h2><span>Prompt 修复闭环</span></div>
            <div class="failure-task-head"><span>任务</span><span>影响</span><span>Prompt</span><span>负责人</span><span>状态</span><span>优先级</span><span>时间</span></div>
            <div v-for="row in data.tasks" :key="row[0]" class="failure-task-row"><span v-for="cell in row" :key="cell">{{ cell }}</span></div>
          </section>
          <section class="failure-flow failure-card">
            <h2>失败归因闭环</h2>
            <div><button v-for="(item, index) in data.flow" :key="item[0]" type="button" :data-current="item[0] === 'Root Cause'"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong><ArrowRight v-if="index < data.flow.length - 1" :size="14" /></button></div>
          </section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.failure-shell{display:grid;grid-template-columns:190px minmax(0,1fr);height:100vh;overflow:hidden;color:#111827;background:#f6f8fb;--line:#d7e0ec;--blue:#2563eb;--green:#16a34a;--red:#ef4444}.failure-sidebar{display:grid;grid-template-rows:auto 1fr auto;height:100vh;border-right:1px solid var(--line);background:#fff}.failure-brand{height:50px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--line);font-size:14px}.failure-brand button{display:grid;place-items:center;width:28px;height:28px;border:0;background:transparent;color:var(--blue)}.failure-nav{display:flex;flex-direction:column;gap:3px;padding:8px}.failure-nav button{position:relative;display:flex;align-items:center;gap:9px;height:34px;padding:0 9px;color:#344054;border:0;border-radius:6px;background:transparent;font-size:12px;cursor:pointer}.failure-nav button[aria-current=page]{color:#175cd3;background:#eaf1fd;font-weight:700}.failure-nav button[aria-current=page]:before{position:absolute;left:0;width:3px;height:22px;background:#2563eb;content:""}.failure-local-card{margin:8px;padding:10px;border:1px solid #cfdcf1;border-radius:8px;background:#f7faff}.failure-local-card strong{color:#175cd3;font-size:12px}.failure-local-card p{display:flex;gap:6px;margin:5px 0;color:#475467;font-size:10px}.failure-local-card small{font-size:9px;color:#7b8797}
.failure-topbar{height:50px;display:grid;grid-template-columns:minmax(260px,1fr) auto auto auto;align-items:center;gap:10px;padding:0 15px;border-bottom:1px solid var(--line);background:#fff}.failure-search{display:flex;align-items:center;gap:8px;height:32px;padding:0 12px;color:#667085;border:1px solid #ccd7e5;border-radius:8px;background:#f8fafc}.failure-search input{width:100%;border:0;outline:0;background:transparent}.failure-pills,.failure-user{display:flex;align-items:center;gap:7px;white-space:nowrap}.failure-pills span{padding:5px 8px;border-radius:6px;color:#175cd3;background:#eaf1fd;font-size:11px;font-weight:700}.failure-pills span:first-child{color:#087a42;background:#e8f8ef}.failure-pills span:last-child{color:#6941c6;background:#f2ebff}.failure-user{color:#667085;font-size:11px}.failure-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}.failure-primary{height:32px;padding:0 12px;color:#fff;border:1px solid #1d4ed8;border-radius:7px;background:#2563eb;font-size:11px;font-weight:700}
.failure-main{height:calc(100vh - 50px);padding:10px 15px 12px;overflow:hidden}.failure-title{height:50px;display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}.failure-title span{color:#667085;font-size:11px;font-weight:700;text-transform:uppercase}.failure-title h1{margin:0;font-size:25px}.failure-title p{width:100%;margin:-2px 0 0;color:#667085;font-size:10px}.failure-grid{display:grid;grid-template-columns:1.45fr .75fr .9fr;grid-template-rows:92px 304px 186px 66px;grid-template-areas:"metrics metrics metrics" "table dist insight" "tasks tasks insight" "flow flow flow";gap:8px}.failure-card{min-width:0;overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#fff}.failure-heading{height:30px;display:flex;align-items:center;justify-content:space-between;padding:0 10px;border-bottom:1px solid #edf1f6}.failure-heading h2{margin:0;font-size:13px}.failure-heading span{color:#718096;font-size:9px}.failure-metrics{grid-area:metrics;display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.failure-metrics article{padding:12px 14px}.failure-metrics span{color:#667085;font-size:10px}.failure-metrics strong{display:block;margin-top:8px;font-family:"Cascadia Code",Consolas,monospace;font-size:24px}.failure-metrics small{color:#667085}.failure-metrics [data-tone=red] strong{color:#ef4444}.failure-metrics [data-tone=amber] strong{color:#d97706}.failure-metrics [data-tone=green] strong{color:#087a42}.failure-table{grid-area:table}.failure-table-head,.failure-table-row{display:grid;grid-template-columns:.8fr 1fr 1fr 1fr .65fr .7fr 1fr;align-items:center;height:30px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.failure-table-head{color:#667085;font-weight:700;background:#f8fafc}.failure-table-row span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.failure-table-row span:nth-child(5){color:#ef4444;font-weight:700}.failure-distribution{grid-area:dist}.failure-distribution>div:not(.failure-heading){display:grid;grid-template-columns:1fr 1.2fr 36px 46px;align-items:center;height:43px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:10px}.failure-distribution meter{width:100%;height:8px}.failure-distribution strong{color:#ef4444}.failure-insight{grid-area:insight}.failure-insight dl{display:grid;grid-template-columns:76px 1fr;gap:6px 8px;margin:10px 12px;font-size:10px}.failure-insight dt{color:#667085}.failure-insight dd{min-width:0;margin:0;color:#344054;line-height:1.35}.failure-insight code{font-family:"Cascadia Code",Consolas,monospace;font-size:9px}.failure-insight button{display:flex;align-items:center;justify-content:center;gap:6px;width:calc(100% - 24px);height:30px;margin:8px 12px;color:#175cd3;border:1px solid #8fb5fb;border-radius:7px;background:#fff;font-size:10px;font-weight:700}.failure-tasks{grid-area:tasks}.failure-task-head,.failure-task-row{display:grid;grid-template-columns:2fr .8fr 1fr .85fr .75fr .55fr 1.05fr;align-items:center;height:31px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.failure-task-head{color:#667085;font-weight:700;background:#f8fafc}.failure-task-row span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.failure-flow{grid-area:flow;display:grid;grid-template-columns:180px 1fr;align-items:center}.failure-flow h2{margin:0;padding-left:12px;font-size:12px}.failure-flow>div{display:grid;grid-template-columns:repeat(5,1fr);height:100%}.failure-flow button{position:relative;display:flex;flex-direction:column;justify-content:center;padding:0 10px;border:0;border-left:1px solid #e5ebf2;background:#fff}.failure-flow button[data-current=true]{color:#ef4444;background:#fff7f7;box-shadow:inset 0 0 0 1px #f3b8b8}.failure-flow span{color:#7b8797;font-size:7px}.failure-flow strong{margin-top:2px;font-size:9px}.failure-flow svg{position:absolute;right:-8px;background:#fff;color:#8ba0b9}
@media(max-width:1180px){.failure-shell{display:block;height:auto;overflow:visible}.failure-sidebar{height:auto}.failure-nav{display:grid;grid-template-columns:repeat(3,1fr)}.failure-local-card{display:none}.failure-topbar{height:auto;grid-template-columns:1fr auto;padding:10px 14px}.failure-pills,.failure-user{display:none}.failure-main{height:auto;overflow:visible}.failure-grid,.failure-metrics{display:block}.failure-card{margin-bottom:10px}}
</style>
