<script setup lang="ts">
defineProps<{
  dot?: boolean
  label?: string
  tone?: 'neutral' | 'primary' | 'success' | 'warning' | 'danger'
}>()
</script>

<template>
  <span class="status-badge" :data-tone="tone ?? 'neutral'">
    <i v-if="dot" aria-hidden="true"></i>
    <slot>{{ label }}</slot>
  </span>
</template>
