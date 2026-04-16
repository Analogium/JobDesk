<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Sidebar -->
    <aside class="fixed inset-y-0 left-0 w-64 bg-white border-r border-gray-200 flex flex-col z-10">
      <div class="h-16 flex items-center px-6 border-b border-gray-200">
        <span class="text-xl font-bold text-brand-700">JobDesk</span>
      </div>

      <nav class="flex-1 px-4 py-6 space-y-1">
        <NuxtLink
          to="/"
          class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors"
          :class="$route.path === '/' ? 'bg-brand-50 text-brand-700' : 'text-gray-600 hover:bg-gray-100'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          Dashboard
        </NuxtLink>

        <NuxtLink
          to="/applications"
          class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors"
          :class="$route.path.startsWith('/applications') ? 'bg-brand-50 text-brand-700' : 'text-gray-600 hover:bg-gray-100'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          Candidatures
        </NuxtLink>
      </nav>

      <!-- User -->
      <div v-if="authStore.user" class="px-4 py-4 border-t border-gray-200">
        <div class="flex items-center gap-3">
          <img
            v-if="authStore.user.avatarUrl"
            :src="authStore.user.avatarUrl"
            :alt="authStore.user.name"
            class="w-8 h-8 rounded-full"
          />
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-900 truncate">{{ authStore.user.name }}</p>
          </div>
          <button
            class="text-gray-400 hover:text-gray-600 transition-colors"
            title="Se déconnecter"
            @click="authStore.logout(); navigateTo('/auth/login')"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- Main -->
    <main class="pl-64 min-h-screen">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
const authStore = useAuthStore()
const route = useRoute()

// Redirect to login if not authenticated
watchEffect(() => {
  if (!authStore.isAuthenticated && !route.path.startsWith('/auth')) {
    navigateTo('/auth/login')
  }
})
</script>
