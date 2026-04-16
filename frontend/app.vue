<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
const authStore = useAuthStore()
// fetchMe en arrière-plan pour hydrater le profil utilisateur
onMounted(() => { if (authStore.isAuthenticated) authStore.fetchMe() })
</script>
