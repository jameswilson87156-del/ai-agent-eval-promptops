<script setup lang="ts">
import { ref } from 'vue'
import { AlertTriangle, ArrowRight, BarChart3, CheckCircle2, CircleDashed, Code2, Database, FileText, GitCompare, Layers3, Search, ShieldCheck, SlidersHorizontal, UserRound } from 'lucide-vue-next'
import { datasetMockData } from '../data/datasetMockData'

type DatasetViewKey = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'run' | 'versions' | 'review'

defineProps<{ runningEval: boolean }>()
const emit = defineEmits<{ openView: [view: DatasetViewKey]; runEval: [] }>()

const data = datasetMockData
const datasetSearch = ref('')
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
  <div class="dataset-shell">
    <aside class="dataset-sidebar">
      <div class="dataset-brand"><button type="button" aria-label="展开导航"><Layers3 :size="18" /></button><strong>PromptOps Studio</strong></div>
      <nav class="dataset-nav"><button v-for="item in navItems" :key="item.id" type="button" :aria-current="item.id === 'dataset' ? 'page' : undefined" :data-view="item.view" @click="emit('openView', item.view)"><component :is="item.icon" :size="17" /><span>{{ item.label }}</span></button></nav>
      <section class="dataset-local-card"><strong>本地演示环境</strong><p><CheckCircle2 :size="14" />测试集为自造数据</p><p><CheckCircle2 :size="14" />无真实用户数据</p><small>非生产环境</small></section>
    </aside>
    <section class="dataset-workspace">
      <header class="dataset-topbar">
        <label class="dataset-search"><Search :size="17" /><span class="sr-only">搜索 Prompt、Run 或 Dataset</span><input v-model="datasetSearch" type="search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索 Dataset / Case / 标签" /></label>
        <div class="dataset-pills"><span>本地演示</span><span>自造测试集</span><span>无需 API Key</span></div>
        <div class="dataset-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <button class="dataset-primary" type="button" :disabled="runningEval" @click="emit('runEval')">运行评测</button>
      </header>
      <main id="main" class="dataset-main" tabindex="-1">
        <header class="dataset-title"><span>{{ data.page.eyebrow }}</span><h1>{{ data.page.title }}</h1><p>{{ data.page.subtitle }}</p></header>
        <section class="dataset-metrics"><article v-for="[label, value, helper] in data.metrics" :key="label" class="dataset-card"><span>{{ label }}</span><strong>{{ value }}</strong><small>{{ helper }}</small></article></section>
        <section class="dataset-grid">
          <aside class="dataset-list dataset-card">
            <div class="dataset-heading"><h2>测试集列表</h2><span>{{ data.datasets.length }} 个测试集</span></div>
            <button v-for="item in data.datasets" :key="item.name" type="button" :aria-pressed="item.active === true"><strong>{{ item.name }}</strong><span>{{ item.sampleCount }} 样本 · {{ item.tagCount }} 标签</span><time>{{ item.updatedAt }}</time></button>
          </aside>
          <section class="dataset-current dataset-card">
            <div class="dataset-heading"><h2>{{ data.current.name }}</h2><span>{{ data.current.id }}</span></div>
            <p>{{ data.current.description }}</p>
            <div class="dataset-fields"><span><b>Owner</b>{{ data.current.owner }}</span><span><b>Created</b>{{ data.current.createdAt }}</span><span><b>Dimensions</b>{{ data.current.dimensions.length }}</span></div>
            <div class="dataset-dimensions"><strong>评测维度</strong><b v-for="item in data.current.dimensions" :key="item">{{ item }}</b></div>
            <div class="dataset-tags"><strong>标签分布</strong><p v-for="[tag, count] in data.current.tags" :key="tag"><span>{{ tag }}</span><meter min="0" max="150" :value="count"></meter><b>{{ count }}</b></p></div>
          </section>
          <section class="dataset-preview dataset-card">
            <div class="dataset-heading"><h2>样本预览</h2><span>{{ data.preview.caseId }}</span></div>
            <dl><dt>输入</dt><dd>{{ data.preview.input }}</dd><dt>期望输出</dt><dd>{{ data.preview.expected }}</dd><dt>标签</dt><dd>{{ data.preview.tag }}</dd><dt>难度</dt><dd>{{ data.preview.difficulty }}</dd></dl>
            <pre>{{ data.preview.schema }}</pre>
            <div class="dataset-hit"><p v-for="[label, value] in data.preview.hit" :key="label"><span>{{ label }}</span><strong>{{ value }}</strong></p></div>
          </section>
          <section class="dataset-table dataset-card">
            <div class="dataset-heading"><h2>样本列表</h2><span>{{ data.samples.length }} 条可见样本</span></div>
            <div class="dataset-table-head"><span>Case ID</span><span>输入</span><span>期望</span><span>标签</span><span>难度</span><span>得分</span><span>命中</span><span>更新时间</span></div>
            <div v-for="row in data.samples" :key="row[0]" class="dataset-table-row"><span v-for="cell in row" :key="cell">{{ cell }}</span></div>
          </section>
          <section class="dataset-coverage dataset-card">
            <div class="dataset-heading"><h2>覆盖率</h2><span>当前样本</span></div>
            <p v-for="[label, value] in data.preview.coverage" :key="label"><span>{{ label }}</span><meter min="0" max="100" :value="parseInt(value)"></meter><strong>{{ value }}</strong></p>
            <div class="dataset-difficulty"><strong>难度结构</strong><p v-for="[label, percent, count] in data.current.difficulty" :key="label"><span>{{ label }}</span><b>{{ percent }}</b><em>{{ count }}</em></p></div>
          </section>
          <section class="dataset-flow dataset-card">
            <h2>测试集评测链路</h2>
            <div><button v-for="(item, index) in data.flow" :key="item[0]" type="button" :data-current="item[0] === 'Dataset'"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong><ArrowRight v-if="index < data.flow.length - 1" :size="14" /></button></div>
          </section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.dataset-shell{display:grid;grid-template-columns:190px minmax(0,1fr);height:100vh;overflow:hidden;color:#111827;background:#f6f8fb;--line:#d7e0ec;--blue:#2563eb;--green:#16a34a}.dataset-sidebar{display:grid;grid-template-rows:auto 1fr auto;height:100vh;border-right:1px solid var(--line);background:#fff}.dataset-brand{height:50px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--line);font-size:14px}.dataset-brand button{display:grid;place-items:center;width:28px;height:28px;border:0;background:transparent;color:var(--blue)}.dataset-nav{display:flex;flex-direction:column;gap:3px;padding:8px}.dataset-nav button{position:relative;display:flex;align-items:center;gap:9px;height:34px;padding:0 9px;color:#344054;border:0;border-radius:6px;background:transparent;font-size:12px;cursor:pointer}.dataset-nav button[aria-current=page]{color:#175cd3;background:#eaf1fd;font-weight:700}.dataset-nav button[aria-current=page]:before{position:absolute;left:0;width:3px;height:22px;background:#2563eb;content:""}.dataset-local-card{margin:8px;padding:10px;border:1px solid #cfdcf1;border-radius:8px;background:#f7faff}.dataset-local-card strong{color:#175cd3;font-size:12px}.dataset-local-card p{display:flex;gap:6px;margin:5px 0;color:#475467;font-size:10px}.dataset-local-card small{font-size:9px;color:#7b8797}
.dataset-topbar{height:50px;display:grid;grid-template-columns:minmax(260px,1fr) auto auto auto;align-items:center;gap:10px;padding:0 15px;border-bottom:1px solid var(--line);background:#fff}.dataset-search{display:flex;align-items:center;gap:8px;height:32px;padding:0 12px;color:#667085;border:1px solid #ccd7e5;border-radius:8px;background:#f8fafc}.dataset-search input{width:100%;border:0;outline:0;background:transparent}.dataset-pills,.dataset-user{display:flex;align-items:center;gap:7px;white-space:nowrap}.dataset-pills span{padding:5px 8px;border-radius:6px;color:#175cd3;background:#eaf1fd;font-size:11px;font-weight:700}.dataset-pills span:first-child{color:#087a42;background:#e8f8ef}.dataset-pills span:last-child{color:#6941c6;background:#f2ebff}.dataset-user{color:#667085;font-size:11px}.dataset-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}.dataset-primary{height:32px;padding:0 12px;color:#fff;border:1px solid #1d4ed8;border-radius:7px;background:#2563eb;font-size:11px;font-weight:700}
.dataset-main{height:calc(100vh - 50px);padding:10px 15px 12px;overflow:hidden}.dataset-title{height:50px;display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}.dataset-title span{color:#667085;font-size:11px;font-weight:700;text-transform:uppercase}.dataset-title h1{margin:0;font-size:25px}.dataset-title p{width:100%;margin:-2px 0 0;color:#667085;font-size:10px}.dataset-card{min-width:0;overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#fff}.dataset-metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;height:82px;margin-bottom:8px}.dataset-metrics article{padding:12px 14px}.dataset-metrics span{color:#667085;font-size:10px}.dataset-metrics strong{display:block;margin-top:8px;font-family:"Cascadia Code",Consolas,monospace;font-size:22px}.dataset-metrics small{color:#667085}.dataset-grid{display:grid;grid-template-columns:300px minmax(0,1fr) 330px;grid-template-rows:240px 256px 66px;gap:8px}.dataset-heading{height:30px;display:flex;align-items:center;justify-content:space-between;padding:0 10px;border-bottom:1px solid #edf1f6}.dataset-heading h2{margin:0;font-size:13px}.dataset-heading span{color:#718096;font-size:9px}.dataset-list{grid-row:1/3}.dataset-list button{display:grid;gap:4px;width:calc(100% - 20px);min-height:64px;margin:8px 10px 0;padding:8px 10px;text-align:left;border:1px solid #edf1f6;border-radius:8px;background:#fff}.dataset-list button[aria-pressed=true]{border-color:#8fb5fb;background:#eef5ff;box-shadow:inset 3px 0 #2563eb}.dataset-list strong{font-size:12px}.dataset-list span,.dataset-list time{color:#667085;font-size:9px}.dataset-current p{margin:10px 12px;color:#344054;font-size:10px;line-height:1.45}.dataset-fields{display:grid;grid-template-columns:repeat(3,1fr);margin:0 12px 10px;border:1px solid #edf1f6;border-radius:7px}.dataset-fields span{padding:8px;border-left:1px solid #edf1f6;font-size:10px}.dataset-fields span:first-child{border-left:0}.dataset-fields b{display:block;color:#667085;font-size:8px}.dataset-dimensions{display:flex;align-items:center;gap:7px;margin:0 12px 10px}.dataset-dimensions strong,.dataset-tags strong{font-size:10px}.dataset-dimensions b{padding:5px 7px;border-radius:5px;color:#175cd3;background:#eaf1fd;font-size:9px}.dataset-tags{margin:0 12px}.dataset-tags p{display:grid;grid-template-columns:80px 1fr 32px;align-items:center;gap:8px;margin:6px 0;font-size:9px}.dataset-tags meter,.dataset-coverage meter{width:100%;height:8px}.dataset-preview dl{display:grid;grid-template-columns:70px 1fr;gap:5px 8px;margin:10px 12px;font-size:10px}.dataset-preview dt{color:#667085}.dataset-preview dd{margin:0}.dataset-preview pre{height:78px;margin:0 12px 8px;padding:8px;color:#dbeafe;background:#0f223b;border-radius:7px;font-family:"Cascadia Code",Consolas,monospace;font-size:8px;line-height:1.35;white-space:pre-wrap}.dataset-hit{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin:0 12px}.dataset-hit p{margin:0;padding:7px;border:1px solid #edf1f6;border-radius:7px}.dataset-hit span{display:block;color:#667085;font-size:8px}.dataset-hit strong{font-size:11px}.dataset-table{grid-column:2;grid-row:2}.dataset-table-head,.dataset-table-row{display:grid;grid-template-columns:.75fr 2fr .8fr .8fr .6fr .55fr .65fr .8fr;align-items:center;height:27px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:8px}.dataset-table-head{color:#667085;font-weight:700;background:#f8fafc}.dataset-table-row span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.dataset-coverage{grid-column:3;grid-row:2}.dataset-coverage>p{display:grid;grid-template-columns:95px 1fr 44px;align-items:center;gap:8px;margin:12px 12px;font-size:10px}.dataset-difficulty{margin:10px 12px;padding-top:8px;border-top:1px solid #edf1f6}.dataset-difficulty p{display:grid;grid-template-columns:1fr 52px 45px;margin:7px 0;font-size:10px}.dataset-difficulty b{color:#087a42}.dataset-difficulty em{font-style:normal;color:#667085}.dataset-flow{grid-column:1/-1;display:grid;grid-template-columns:180px 1fr;align-items:center}.dataset-flow h2{margin:0;padding-left:12px;font-size:12px}.dataset-flow>div{display:grid;grid-template-columns:repeat(5,1fr);height:100%}.dataset-flow button{position:relative;display:flex;flex-direction:column;justify-content:center;padding:0 10px;border:0;border-left:1px solid #e5ebf2;background:#fff}.dataset-flow button[data-current=true]{color:#087a42;background:#f0fdf4;box-shadow:inset 0 0 0 1px #b7e4c7}.dataset-flow span{color:#7b8797;font-size:7px}.dataset-flow strong{margin-top:2px;font-size:9px}.dataset-flow svg{position:absolute;right:-8px;background:#fff;color:#8ba0b9}
@media(max-width:1180px){.dataset-shell{display:block;height:auto;overflow:visible}.dataset-sidebar{height:auto}.dataset-nav{display:grid;grid-template-columns:repeat(3,1fr)}.dataset-local-card{display:none}.dataset-topbar{height:auto;grid-template-columns:1fr auto;padding:10px 14px}.dataset-pills,.dataset-user{display:none}.dataset-main{height:auto;overflow:visible}.dataset-metrics,.dataset-grid{display:block}.dataset-card{margin-bottom:10px}}
</style>
