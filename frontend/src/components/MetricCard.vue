<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  dataMetric?: string
  helper?: string
  icon: Component
  label: string
  tone?: 'neutral' | 'primary' | 'success' | 'warning' | 'danger'
  unit?: string
  value: number | string
}>()
</script>

<template>
  <article class="metric-card" :data-tone="tone ?? 'neutral'" :data-metric="dataMetric ?? label">
    <span class="metric-icon"><component :is="icon" :size="17" /></span>
    <span>{{ label }}</span>
    <strong>{{ value }}<small>{{ unit }}</small></strong>
    <em v-if="helper">{{ helper }}</em>
  </article>
</template>
