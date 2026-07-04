<script setup lang="ts">
import { ref } from 'vue'
import { AlertTriangle, BarChart3, CheckCircle2, CircleDashed, Code2, Database, FileText, GitCompare, Layers3, Plus, Search, ShieldCheck, SlidersHorizontal, UserRound } from 'lucide-vue-next'
import { promptStudioMockData } from '../data/promptStudioMockData'

type StudioView = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'run' | 'versions' | 'review'

defineProps<{ runningEval: boolean }>()
const emit = defineEmits<{ openView: [view: StudioView]; runEval: [] }>()

const data = promptStudioMockData
const promptSearch = ref('')
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
  <div class="prompt-shell">
    <aside class="prompt-sidebar">
      <div class="prompt-brand"><button type="button" aria-label="展开导航"><Layers3 :size="18" /></button><strong>PromptOps Studio</strong></div>
      <nav class="prompt-nav">
        <button v-for="item in navItems" :key="item.id" type="button" :aria-current="item.id === 'prompt' ? 'page' : undefined" :data-view="item.view" @click="emit('openView', item.view)">
          <component :is="item.icon" :size="17" /><span>{{ item.label }}</span>
        </button>
      </nav>
      <section class="prompt-local-card"><strong>本地演示环境</strong><p><CheckCircle2 :size="14" />结构化 mock data</p><p><CheckCircle2 :size="14" />无需 API Key</p><small>非生产环境</small></section>
    </aside>
    <section class="prompt-workspace">
      <header class="prompt-topbar">
        <label class="prompt-search"><Search :size="17" /><span class="sr-only">搜索 Prompt、Run 或 Dataset</span><input v-model="promptSearch" type="search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索 Prompt / Run / Dataset" /></label>
        <div class="prompt-pills"><span>本地演示</span><span>规则评测</span><span>无需 API Key</span></div>
        <div class="prompt-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <button class="prompt-primary" type="button" :disabled="runningEval" @click="emit('runEval')"><Plus :size="15" />新建 Prompt</button>
      </header>
      <main id="main" class="prompt-main" tabindex="-1">
        <header class="prompt-title"><span>{{ data.page.eyebrow }}</span><h1>{{ data.page.title }}</h1><p>{{ data.page.subtitle }}</p></header>
        <section class="prompt-grid">
          <aside class="prompt-list prompt-card">
            <div class="prompt-heading"><h2>模板列表</h2><span>5 个常用模板</span></div>
            <label class="prompt-inner-search"><Search :size="14" /><input type="search" placeholder="搜索 Prompt 名称或场景" /></label>
            <button v-for="template in data.templates" :key="template.name" type="button" :aria-pressed="template.active === true">
              <span><strong>{{ template.name }}</strong><small>{{ template.scenario }}</small></span>
              <b>{{ template.score }}</b><em>{{ template.run }}</em>
              <i><small v-for="tag in template.tags" :key="tag">{{ tag }}</small></i>
            </button>
          </aside>
          <section class="prompt-editor prompt-card">
            <div class="prompt-heading"><h2>{{ data.current.name }}</h2><span>{{ data.current.version }}</span></div>
            <div class="prompt-vars"><strong>变量（{{ data.current.variables.length }}）</strong><span v-for="[name, desc] in data.current.variables" :key="name"><code>{{ name }}</code>{{ desc }}</span></div>
            <div class="prompt-editor-layout">
              <section><h3>Prompt 编辑</h3><pre>{{ data.current.prompt }}</pre></section>
              <section><h3>Prompt Preview</h3><pre>{{ data.current.preview }}</pre></section>
            </div>
          </section>
          <aside class="prompt-side">
            <section class="prompt-card prompt-score">
              <div class="prompt-heading"><h2>版本评分</h2><span>基于本地规则集</span></div>
              <strong>{{ data.score.total }}</strong>
              <div><span v-for="[label, value] in data.score.dimensions" :key="label">{{ label }} <b>{{ value }}</b></span></div>
              <p>评测集：{{ data.score.evalRun }} · {{ data.score.updatedAt }}</p>
            </section>
            <section class="prompt-card prompt-runs">
              <div class="prompt-heading"><h2>最近 Eval Run</h2><span>查看全部</span></div>
              <div v-for="run in data.evalRuns" :key="run[0]"><strong>{{ run[0] }}</strong><b :data-fail="run[1] === '失败'">{{ run[1] }}</b><span>{{ run[2] }}</span><time>{{ run[3] }}</time></div>
            </section>
            <section class="prompt-card prompt-summary">
              <div class="prompt-heading"><h2>版本 Diff 摘要</h2><span>v3.0 -> v3.1</span></div>
              <p v-for="[type, text] in data.diff" :key="text"><b>{{ type }}</b>{{ text }}</p>
            </section>
            <section class="prompt-card prompt-suggestions">
              <div class="prompt-heading"><h2>使用建议</h2></div>
              <p v-for="item in data.suggestions" :key="item">{{ item }}</p>
            </section>
          </aside>
          <section class="prompt-history prompt-card">
            <div class="prompt-heading"><h2>版本历史 / Prompt Diff</h2><span>对比模式</span></div>
            <div v-for="[version, text] in data.history" :key="version"><strong>{{ version }}</strong><pre>{{ text }}</pre></div>
          </section>
          <section class="prompt-recent prompt-card">
            <div class="prompt-heading"><h2>最近运行记录</h2><span>查看全部</span></div>
            <div class="prompt-recent-head"><span>Run ID</span><span>Dataset</span><span>Prompt</span><span>样本数</span><span>分数</span><span>通过率</span><span>状态</span><span>耗时</span><span>时间</span></div>
            <div v-for="row in data.recentRuns" :key="row[0]" class="prompt-recent-row"><span v-for="cell in row" :key="cell">{{ cell }}</span></div>
          </section>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.prompt-shell{display:grid;grid-template-columns:190px minmax(0,1fr);height:100vh;overflow:hidden;color:#111827;background:#f6f8fb;--line:#d7e0ec;--blue:#2563eb;--green:#16a34a;--red:#ef4444}.prompt-sidebar{display:grid;grid-template-rows:auto 1fr auto;height:100vh;border-right:1px solid var(--line);background:#fff}.prompt-brand{height:50px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--line);font-size:14px}.prompt-brand button{display:grid;place-items:center;width:28px;height:28px;border:0;background:transparent;color:var(--blue)}.prompt-nav{display:flex;flex-direction:column;gap:3px;padding:8px}.prompt-nav button{position:relative;display:flex;align-items:center;gap:9px;height:34px;padding:0 9px;color:#344054;border:0;border-radius:6px;background:transparent;font-size:12px;cursor:pointer}.prompt-nav button[aria-current=page]{color:#175cd3;background:#eaf1fd;font-weight:700}.prompt-nav button[aria-current=page]:before{position:absolute;left:0;width:3px;height:22px;background:#2563eb;content:""}.prompt-local-card{margin:8px;padding:10px;border:1px solid #cfdcf1;border-radius:8px;background:#f7faff}.prompt-local-card strong{color:#175cd3;font-size:12px}.prompt-local-card p{display:flex;gap:6px;margin:5px 0;color:#475467;font-size:10px}.prompt-local-card small{font-size:9px;color:#7b8797}
.prompt-topbar{height:50px;display:grid;grid-template-columns:minmax(260px,1fr) auto auto auto;align-items:center;gap:10px;padding:0 15px;border-bottom:1px solid var(--line);background:#fff}.prompt-search{display:flex;align-items:center;gap:8px;height:32px;padding:0 12px;color:#667085;border:1px solid #ccd7e5;border-radius:8px;background:#f8fafc}.prompt-search input,.prompt-inner-search input{width:100%;border:0;outline:0;background:transparent}.prompt-pills,.prompt-user{display:flex;align-items:center;gap:7px;white-space:nowrap}.prompt-pills span{padding:5px 8px;border-radius:6px;color:#175cd3;background:#eaf1fd;font-size:11px;font-weight:700}.prompt-pills span:first-child{color:#087a42;background:#e8f8ef}.prompt-pills span:last-child{color:#6941c6;background:#f2ebff}.prompt-user{color:#667085;font-size:11px}.prompt-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}.prompt-primary{display:flex;align-items:center;gap:6px;height:32px;padding:0 12px;color:#fff;border:1px solid #1d4ed8;border-radius:7px;background:#2563eb;font-size:11px;font-weight:700}
.prompt-main{height:calc(100vh - 50px);padding:10px 15px 12px;overflow:hidden}.prompt-title{height:50px;display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}.prompt-title span{color:#667085;font-size:11px;font-weight:700;text-transform:uppercase}.prompt-title h1{margin:0;font-size:25px}.prompt-title p{width:100%;margin:-2px 0 0;color:#667085;font-size:10px}.prompt-grid{display:grid;grid-template-columns:330px minmax(0,1fr) 340px;grid-template-rows:430px 242px;gap:8px}.prompt-card{min-width:0;overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#fff}.prompt-heading{height:30px;display:flex;align-items:center;justify-content:space-between;gap:10px;padding:0 10px;border-bottom:1px solid #edf1f6}.prompt-heading h2{margin:0;font-size:13px}.prompt-heading span{color:#718096;font-size:9px}.prompt-list{padding-bottom:8px}.prompt-inner-search{display:flex;align-items:center;gap:7px;height:30px;margin:8px 10px;padding:0 9px;border:1px solid #ccd7e5;border-radius:7px;color:#667085;background:#f8fafc}.prompt-list>button{display:grid;grid-template-columns:1fr 48px 48px;gap:6px;width:calc(100% - 20px);min-height:68px;margin:0 10px 6px;padding:8px 9px;text-align:left;border:1px solid #edf1f6;border-radius:8px;background:#fff;cursor:pointer}.prompt-list>button[aria-pressed=true]{border-color:#8fb5fb;background:#eef5ff;box-shadow:inset 3px 0 #2563eb}.prompt-list strong{font-family:"Cascadia Code",Consolas,monospace;font-size:11px}.prompt-list small{display:block;color:#667085;font-size:8px}.prompt-list b{color:#087a42;font-family:"Cascadia Code",Consolas,monospace}.prompt-list em{font-style:normal;font-size:10px;color:#344054}.prompt-list i{grid-column:1/-1;display:flex;gap:5px;font-style:normal}.prompt-list i small{padding:3px 6px;border-radius:5px;color:#175cd3;background:#eaf1fd}
.prompt-editor{grid-column:2;grid-row:1}.prompt-vars{display:flex;align-items:center;gap:8px;height:45px;padding:0 10px;border-bottom:1px solid #edf1f6}.prompt-vars strong{font-size:12px}.prompt-vars span{padding:5px 8px;border:1px solid #bfd3f5;border-radius:6px;color:#175cd3;background:#f3f7ff;font-size:10px}.prompt-editor-layout{display:grid;grid-template-columns:1fr 1fr;height:calc(100% - 75px)}.prompt-editor-layout section{min-width:0;padding:10px;border-right:1px solid #edf1f6}.prompt-editor-layout section:last-child{border-right:0}.prompt-editor-layout h3{margin:0 0 8px;font-size:11px}.prompt-editor-layout pre,.prompt-history pre{height:calc(100% - 20px);margin:0;padding:10px;overflow:hidden;border:1px solid #dce5ee;border-radius:7px;background:#f8fafc;font-family:"Cascadia Code",Consolas,monospace;font-size:10px;line-height:1.55;white-space:pre-wrap}.prompt-editor-layout section:last-child pre{color:#dbeafe;background:#0f223b;border-color:#25496d}.prompt-side{display:grid;grid-template-rows:128px 150px 118px 1fr;gap:8px}.prompt-score>strong{display:block;margin:12px 14px 8px;font-family:"Cascadia Code",Consolas,monospace;font-size:22px}.prompt-score>div:not(.prompt-heading){display:grid;grid-template-columns:repeat(4,1fr);gap:4px;margin:0 14px}.prompt-score>div span{font-size:9px}.prompt-score b{display:block;color:#087a42}.prompt-score p{margin:8px 14px 0;color:#667085;font-size:9px}.prompt-runs>div:not(.prompt-heading){display:grid;grid-template-columns:1fr 48px 44px 54px;align-items:center;height:24px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.prompt-runs b{color:#087a42}.prompt-runs b[data-fail=true]{color:#ef4444}.prompt-summary p,.prompt-suggestions p{margin:7px 10px;color:#344054;font-size:10px;line-height:1.35}.prompt-summary b{margin-right:6px;color:#087a42}.prompt-history{grid-column:2;grid-row:2;display:grid;grid-template-columns:1fr 1fr}.prompt-history .prompt-heading{grid-column:1/-1}.prompt-history>div:not(.prompt-heading){padding:8px 10px;border-right:1px solid #edf1f6}.prompt-history strong{font-size:11px}.prompt-history pre{height:150px;margin-top:6px}.prompt-recent{grid-column:1/-1;grid-row:2;align-self:end;margin-left:338px;width:calc(100% - 338px);height:92px}.prompt-recent-head,.prompt-recent-row{display:grid;grid-template-columns:1fr 1.35fr .7fr .6fr .6fr .6fr .55fr .55fr 1.3fr;align-items:center;height:20px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:8px}.prompt-recent-head{color:#667085;font-weight:700;background:#f8fafc}.prompt-recent-row span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}
@media(max-width:1180px){.prompt-shell{display:block;height:auto;overflow:visible}.prompt-sidebar{height:auto}.prompt-nav{display:grid;grid-template-columns:repeat(3,1fr)}.prompt-local-card{display:none}.prompt-topbar{height:auto;grid-template-columns:1fr auto;padding:10px 14px}.prompt-pills,.prompt-user{display:none}.prompt-main{height:auto;overflow:visible}.prompt-grid{display:block}.prompt-card,.prompt-side{margin-bottom:10px}.prompt-recent{margin-left:0;width:auto;height:auto}}
</style>
