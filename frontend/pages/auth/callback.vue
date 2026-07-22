<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center">
    <div class="text-center">
      <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-brand-600 mx-auto mb-4" />
      <p class="text-gray-500 text-sm">Connexion en cours…</p>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: false })

const route = useRoute()
const authStore = useAuthStore()

onMounted(async () => {
  const token = route.query.token as string | undefined
  const refresh = route.query.refresh as string | undefined

  if (!token || !refresh) {
    await navigateTo('/auth/login')
    return
  }

  authStore.setTokens(token, refresh)
  await authStore.fetchMe()
  await navigateTo('/')
})
</script>
