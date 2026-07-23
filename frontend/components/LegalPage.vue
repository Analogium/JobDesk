<template>
  <div class="min-h-screen bg-gray-50 py-12 px-4">
    <div class="max-w-3xl mx-auto">
      <NuxtLink to="/" class="text-sm text-brand-600 hover:text-brand-700">← Retour à JobDesk</NuxtLink>

      <div class="bg-white rounded-2xl border border-gray-200 p-8 md:p-10 mt-4">
        <h1 class="text-2xl font-bold text-gray-900">{{ title }}</h1>
        <p class="text-sm text-gray-400 mt-1">Dernière mise à jour : {{ updated }}</p>

        <!-- `legal-content` porte la typographie : les pages n'écrivent que du HTML sémantique. -->
        <div class="legal-content mt-8 text-gray-700">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string, updated: string }>()
</script>

<style scoped>
.legal-content :deep(h2) {
  @apply text-lg font-semibold text-gray-900 mt-8 mb-3;
}
.legal-content :deep(p) {
  @apply text-sm leading-relaxed mb-4;
}
.legal-content :deep(ul) {
  @apply text-sm leading-relaxed mb-4 space-y-2 list-disc pl-5;
}
.legal-content :deep(a) {
  @apply text-brand-600 hover:text-brand-700 underline;
}
.legal-content :deep(code) {
  @apply text-xs bg-gray-100 px-1.5 py-0.5 rounded;
}
.legal-content :deep(table) {
  @apply w-full text-sm text-left mb-4 border-collapse;
}
.legal-content :deep(th) {
  @apply font-semibold text-gray-900 border-b border-gray-200 py-2 pr-4 align-top;
}
.legal-content :deep(td) {
  @apply border-b border-gray-100 py-2 pr-4 align-top;
}
</style>
