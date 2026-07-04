<script setup lang="ts">
import { ref, shallowRef } from 'vue'
import EvalDashboard from './components/EvalDashboard.vue'
import BatchEvaluation from './components/BatchEvaluation.vue'
import TraceDetail from './components/TraceDetail.vue'
import OutputCompare from './components/OutputCompare.vue'
import PromptStudio from './components/PromptStudio.vue'
import FailureCases from './components/FailureCases.vue'
import DatasetView from './components/DatasetView.vue'
import SystemBoundary from './components/SystemBoundary.vue'
import type { PromptProfile } from './types'

type ViewKey = 'overview' | 'batch' | 'compare' | 'trace' | 'prompt' | 'failure' | 'dataset' | 'boundary' | 'metrics' | 'run' | 'versions' | 'review'

const viewKeys: ViewKey[] = ['overview', 'batch', 'compare', 'trace', 'prompt', 'failure', 'dataset', 'boundary', 'metrics', 'run', 'versions', 'review']
const initialView = new URLSearchParams(window.location.search).get('view') as ViewKey | null
const activeView = shallowRef<ViewKey>(initialView && viewKeys.includes(initialView) ? initialView : 'overview')
const runningEval = ref(false)
const activePromptId = ref<number | null>(1)

const prompts: PromptProfile[] = [
  {
    id: 1,
    key: 'refund',
    name: '退款问答 Prompt',
    owner: 'Dev User',
    status: 'Candidate',
    currentVersion: 'v3.1',
    currentVersionId: 31,
    datasetId: 1,
    tags: ['refund', 'json'],
    versionLabels: ['v3.0', 'v3.1'],
    variables: [],
    outputRequirements: ['JSON 输出'],
    scoringRules: [],
    template: '',
    updatedAt: '2025-05-20',
  },
]

function setView(view: ViewKey) {
  const normalized = view === 'run' || view === 'versions' || view === 'review' || view === 'metrics' ? 'overview' : view
  activeView.value = normalized
  const url = new URL(window.location.href)
  url.searchParams.set('view', normalized)
  window.history.replaceState({}, '', url)
}

function runMockBatch() {
  runningEval.value = true
  window.setTimeout(() => {
    runningEval.value = false
  }, 400)
}

function selectPrompt(prompt: PromptProfile) {
  activePromptId.value = prompt.id
}
</script>

<template>
  <div class="app-shell dashboard-mode" data-ready="true">
    <EvalDashboard
      v-if="activeView === 'overview'"
      :active-prompt-id="activePromptId"
      :prompts="prompts"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
      @select-prompt="selectPrompt"
    />
    <BatchEvaluation
      v-else-if="activeView === 'batch'"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
    />
    <OutputCompare
      v-else-if="activeView === 'compare'"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
    />
    <TraceDetail
      v-else-if="activeView === 'trace'"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
    />
    <PromptStudio
      v-else-if="activeView === 'prompt'"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
    />
    <FailureCases
      v-else-if="activeView === 'failure'"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
    />
    <DatasetView
      v-else-if="activeView === 'dataset'"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
    />
    <SystemBoundary
      v-else-if="activeView === 'boundary'"
      :running-eval="runningEval"
      @open-view="setView"
      @run-eval="runMockBatch"
    />
  </div>
</template>
