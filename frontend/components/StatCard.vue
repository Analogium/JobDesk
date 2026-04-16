<template>
  <div class="bg-white rounded-xl border border-gray-200 p-5">
    <p class="text-xs text-gray-500 mb-1">{{ label }}</p>
    <p class="text-3xl font-bold" :class="colorClass">{{ value }}</p>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  label: string
  value: number
  color?: 'blue' | 'purple' | 'green' | 'default'
}>()

const colorClass = computed(() => ({
  blue: 'text-blue-600',
  purple: 'text-purple-600',
  green: 'text-green-600',
  default: 'text-gray-900',
}[props.color ?? 'default']))
</script>
