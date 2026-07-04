<script setup lang="ts">
import { ref } from 'vue'
import { AlertTriangle, ArrowRight, BarChart3, CheckCircle2, CircleDashed, Code2, Database, FileText, GitCompare, Layers3, Search, ShieldCheck, SlidersHorizontal, UserRound } from 'lucide-vue-next'
import { outputCompareMockData } from '../data/outputCompareMockData'

type CompareView = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'run' | 'versions' | 'review'

defineProps<{ runningEval: boolean }>()
const emit = defineEmits<{ openView: [view: CompareView]; runEval: [] }>()

const data = outputCompareMockData
const compareSearch = ref('')
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
  <div class="compare-shell">
    <aside class="compare-sidebar" aria-label="PromptOps Studio 导航">
      <div class="compare-brand"><button type="button" aria-label="展开导航"><Layers3 :size="18" /></button><strong>PromptOps Studio</strong></div>
      <nav class="compare-nav">
        <button v-for="item in navItems" :key="item.id" type="button" :aria-current="item.id === 'compare' ? 'page' : undefined" :data-view="item.view" @click="emit('openView', item.view)">
          <component :is="item.icon" :size="17" /><span>{{ item.label }}</span>
        </button>
      </nav>
      <section class="compare-local-card"><strong>本地演示环境</strong><p><CheckCircle2 :size="14" />Mock Output 对比</p><p><CheckCircle2 :size="14" />Rule-based Eval 打分</p><small>无需 API Key</small></section>
    </aside>

    <section class="compare-workspace">
      <header class="compare-topbar">
        <label class="compare-search"><Search :size="17" /><span class="sr-only">搜索 Prompt、Run 或 Dataset</span><input v-model="compareSearch" type="search" aria-label="搜索 Prompt、Run 或 Dataset" placeholder="搜索 Prompt / Run / Dataset" /></label>
        <div class="compare-pills"><span>本地演示</span><span>规则评测</span><span>无需 API Key</span></div>
        <div class="compare-user"><span>环境：<strong>Local Demo</strong></span><span><UserRound :size="15" />Dev User</span></div>
        <button class="compare-primary" type="button" :disabled="runningEval" @click="emit('runEval')">重新评测</button>
      </header>

      <main id="main" class="compare-main" tabindex="-1">
        <header class="compare-title-row"><span>{{ data.page.eyebrow }}</span><h1>{{ data.page.title }}</h1><p>{{ data.page.subtitle }}</p></header>

        <section class="compare-context" aria-label="当前对比上下文">
          <div v-for="[label, value] in data.context" :key="label"><span>{{ label }}</span><strong>{{ value }}</strong></div>
        </section>

        <section class="compare-grid">
          <section class="compare-metrics compare-card">
            <div class="compare-heading"><h2>评分变化</h2><span>{{ data.decision.result }}</span></div>
            <article v-for="metric in data.metrics" :key="metric.label">
              <span>{{ metric.label }}</span><strong>{{ metric.a }}</strong><b>{{ metric.b }}</b><em>{{ metric.delta }}</em>
            </article>
          </section>

          <section class="compare-output compare-card">
            <div class="compare-heading"><h2>输出 A / Prompt v3.0</h2><span>上一版</span></div>
            <pre>{{ data.outputs.promptA }}</pre>
          </section>

          <section class="compare-output compare-card">
            <div class="compare-heading"><h2>输出 B / Prompt v3.1</h2><span>推荐版本</span></div>
            <pre>{{ data.outputs.promptB }}</pre>
          </section>

          <section class="compare-diff compare-card">
            <div class="compare-heading"><h2>结构化差异</h2><span>{{ data.diff.length }} 个变更</span></div>
            <div class="compare-diff-row compare-diff-head"><span>字段</span><span>Before</span><span>After</span><span>类型</span></div>
            <div v-for="row in data.diff" :key="row.field" class="compare-diff-row">
              <strong>{{ row.field }}</strong><span>{{ row.before }}</span><b>{{ row.after }}</b><em>{{ row.type }}</em>
            </div>
          </section>

          <section class="compare-schema compare-card">
            <div class="compare-heading"><h2>Schema 校验</h2><span>confidence 已修复</span></div>
            <div class="compare-table-head"><span>字段</span><span>A</span><span>B</span><span>结论</span></div>
            <div v-for="row in data.schemaRows" :key="row[0]"><span>{{ row[0] }}</span><strong :data-fail="row[1] === '缺失'">{{ row[1] }}</strong><b>{{ row[2] }}</b><em>{{ row[3] }}</em></div>
          </section>

          <section class="compare-risk compare-card">
            <div class="compare-heading"><h2>风险对比</h2><span>风险降低</span></div>
            <div v-for="row in data.risks" :key="row[0]"><span>{{ row[0] }}</span><strong>{{ row[1] }}</strong><b>{{ row[2] }}</b><em>{{ row[3] }}</em></div>
          </section>

          <section class="compare-decision compare-card">
            <div class="compare-heading"><h2>结论</h2><span>{{ data.decision.recommended }}</span></div>
            <strong>{{ data.decision.result }}</strong>
            <p>{{ data.decision.reason }}</p>
            <button type="button" @click="emit('openView', 'prompt')"><Code2 :size="16" />打开 Prompt 工作台</button>
          </section>

          <section class="compare-recent compare-card">
            <div class="compare-heading"><h2>历史对比记录</h2><span>最近 3 次</span></div>
            <div class="compare-recent-head"><span>ID</span><span>Dataset</span><span>Case</span><span>A</span><span>B</span><span>Score</span><span>Delta</span><span>推荐</span><span>状态</span><span>时间</span></div>
            <div v-for="row in data.recent" :key="row[0]" class="compare-recent-row"><span v-for="cell in row" :key="cell">{{ cell }}</span></div>
          </section>

          <section class="compare-flow compare-card">
            <h2>PromptOps 输出对比链路</h2>
            <div>
              <button v-for="(item, index) in data.flow" :key="item[0]" type="button" :data-current="item[0] === 'Recommended Version'">
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
.compare-shell{display:grid;grid-template-columns:190px minmax(0,1fr);height:100vh;overflow:hidden;color:#111827;background:#f6f8fb;--line:#d7e0ec;--blue:#2563eb;--green:#16a34a;--red:#ef4444}
button,input{font:inherit}.compare-sidebar{display:grid;grid-template-rows:auto 1fr auto;height:100vh;border-right:1px solid var(--line);background:#fff}.compare-brand{height:50px;display:flex;align-items:center;gap:10px;padding:0 12px;border-bottom:1px solid var(--line);font-size:14px}.compare-brand button{display:grid;place-items:center;width:28px;height:28px;padding:0;color:var(--blue);border:0;background:transparent}.compare-nav{display:flex;flex-direction:column;gap:3px;padding:8px}.compare-nav button{position:relative;display:flex;align-items:center;gap:9px;height:34px;padding:0 9px;color:#344054;border:0;border-radius:6px;background:transparent;font-size:12px;cursor:pointer}.compare-nav button[aria-current=page]{color:#175cd3;background:#eaf1fd;font-weight:700}.compare-nav button[aria-current=page]:before{position:absolute;left:0;width:3px;height:22px;border-radius:2px;background:var(--blue);content:""}.compare-local-card{margin:8px;padding:10px;border:1px solid #cfdcf1;border-radius:8px;background:#f7faff}.compare-local-card strong{color:#175cd3;font-size:12px}.compare-local-card p{display:flex;gap:6px;margin:5px 0;color:#475467;font-size:10px}.compare-local-card small{color:#7b8797;font-size:9px}
.compare-topbar{height:50px;display:grid;grid-template-columns:minmax(260px,1fr) auto auto auto;align-items:center;gap:10px;padding:0 15px;border-bottom:1px solid var(--line);background:#fff}.compare-search{display:flex;align-items:center;gap:8px;height:32px;padding:0 12px;color:#667085;border:1px solid #ccd7e5;border-radius:8px;background:#f8fafc}.compare-search input{width:100%;border:0;outline:0;background:transparent}.compare-pills,.compare-user{display:flex;align-items:center;gap:7px;white-space:nowrap}.compare-pills span{padding:5px 8px;border-radius:6px;color:#175cd3;background:#eaf1fd;font-size:11px;font-weight:700}.compare-pills span:first-child{color:#087a42;background:#e8f8ef}.compare-pills span:last-child{color:#6941c6;background:#f2ebff}.compare-user{color:#667085;font-size:11px}.compare-user span:last-child{display:flex;align-items:center;gap:5px;color:#25324a;font-weight:700}.compare-primary{height:32px;padding:0 12px;color:#fff;border:1px solid #1d4ed8;border-radius:7px;background:#2563eb;font-size:11px;font-weight:700}
.compare-main{height:calc(100vh - 50px);padding:10px 15px 12px;overflow:hidden}.compare-title-row{height:50px;display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}.compare-title-row span{color:#667085;font-size:11px;font-weight:700;text-transform:uppercase}.compare-title-row h1{margin:0;font-size:25px}.compare-title-row p{width:100%;margin:-2px 0 0;color:#667085;font-size:10px}.compare-context{display:grid;grid-template-columns:repeat(6,1fr);height:44px;margin-bottom:8px;border:1px solid var(--line);border-radius:8px;background:#fff}.compare-context div{min-width:0;padding:7px 12px;border-left:1px solid #e6ebf2}.compare-context div:first-child{border-left:0}.compare-context span{display:block;color:#667085;font-size:9px}.compare-context strong{display:block;overflow:hidden;margin-top:4px;font-family:"Cascadia Code",Consolas,monospace;font-size:10px;white-space:nowrap;text-overflow:ellipsis}
.compare-grid{display:grid;grid-template-columns:1fr 1fr 1fr;grid-template-rows:138px 154px 118px 124px 66px;grid-template-areas:"metrics outa outb" "diff diff schema" "risk decision schema" "recent recent recent" "flow flow flow";gap:8px}.compare-card{min-width:0;overflow:hidden;border:1px solid var(--line);border-radius:8px;background:#fff}.compare-heading{height:30px;display:flex;align-items:center;justify-content:space-between;gap:10px;padding:0 10px;border-bottom:1px solid #edf1f6}.compare-heading h2{margin:0;font-size:13px}.compare-heading span{color:#718096;font-size:9px}.compare-metrics{grid-area:metrics}.compare-metrics article{display:grid;grid-template-columns:1.2fr .6fr .6fr .8fr;height:21px;align-items:center;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.compare-metrics strong,.compare-metrics b{font-family:"Cascadia Code",Consolas,monospace}.compare-metrics b,.compare-metrics em{color:#087a42;font-style:normal;font-weight:700}.compare-output pre{height:calc(100% - 30px);margin:0;padding:10px;color:#dbeafe;background:#0f223b;font-family:"Cascadia Code",Consolas,monospace;font-size:10px;line-height:1.45;white-space:pre-wrap}.compare-output:nth-of-type(2){grid-area:outa}.compare-output:nth-of-type(3){grid-area:outb}
.compare-diff{grid-area:diff}.compare-diff-row{display:grid;grid-template-columns:.55fr 1.3fr 1.55fr .5fr;align-items:center;gap:8px;min-height:31px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.compare-diff-head{color:#667085;font-weight:700;background:#f8fafc}.compare-diff-row span,.compare-diff-row b{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.compare-diff-row b{color:#087a42}.compare-diff-row em{color:#175cd3;font-style:normal;font-weight:700}.compare-schema{grid-area:schema}.compare-table-head,.compare-schema>div:not(.compare-heading){display:grid;grid-template-columns:1fr .75fr .75fr .8fr;align-items:center;min-height:30px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.compare-table-head{color:#667085;font-weight:700;background:#f8fafc}.compare-schema strong[data-fail=true]{color:#ef4444}.compare-schema b{color:#087a42}.compare-risk{grid-area:risk}.compare-risk>div:not(.compare-heading){display:grid;grid-template-columns:1.5fr .65fr .55fr .65fr;align-items:center;height:29px;padding:0 10px;border-bottom:1px solid #edf1f5;font-size:9px}.compare-risk strong{color:#ef4444}.compare-risk b,.compare-risk em{color:#087a42;font-style:normal;font-weight:700}.compare-decision{grid-area:decision;padding-bottom:8px}.compare-decision>strong{display:block;margin:10px 12px 4px;color:#087a42;font-size:20px}.compare-decision p{margin:0 12px 8px;color:#526174;font-size:10px}.compare-decision button{display:flex;align-items:center;gap:6px;margin-left:12px;height:28px;padding:0 10px;color:#175cd3;border:1px solid #8fb5fb;border-radius:7px;background:#fff;font-size:10px;font-weight:700}.compare-recent{grid-area:recent}.compare-recent-head,.compare-recent-row{display:grid;grid-template-columns:1.1fr 1fr .7fr 1fr 1fr .8fr .6fr 1fr .6fr 1.2fr;align-items:center;min-height:22px;padding:0 8px;border-bottom:1px solid #edf1f5;font-size:8px}.compare-recent-head{color:#667085;font-weight:700;background:#f8fafc}.compare-recent-row span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.compare-flow{grid-area:flow;display:grid;grid-template-columns:180px 1fr;align-items:center}.compare-flow h2{margin:0;padding-left:12px;font-size:12px}.compare-flow>div{display:grid;grid-template-columns:repeat(6,1fr);height:100%}.compare-flow button{position:relative;display:flex;flex-direction:column;justify-content:center;min-width:0;padding:0 10px;border:0;border-left:1px solid #e5ebf2;background:#fff}.compare-flow button[data-current=true]{color:#087a42;background:#f0fdf4;box-shadow:inset 0 0 0 1px #b7e4c7}.compare-flow span{color:#7b8797;font-size:7px}.compare-flow strong{margin-top:2px;font-size:9px}.compare-flow svg{position:absolute;right:-8px;z-index:1;color:#8ba0b9;background:#fff}
@media(max-width:1180px){.compare-shell{display:block;height:auto;overflow:visible}.compare-sidebar{height:auto}.compare-nav{display:grid;grid-template-columns:repeat(3,1fr)}.compare-local-card{display:none}.compare-topbar{height:auto;grid-template-columns:1fr auto;padding:10px 14px}.compare-pills,.compare-user{display:none}.compare-main{height:auto;overflow:visible}.compare-context,.compare-grid{display:block}.compare-card{margin-bottom:10px}}
</style>
