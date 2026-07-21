<template>
  <AuthCard subtitle="Suivi intelligent de candidatures">
    <form class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label for="email" class="block text-sm font-medium text-gray-700 mb-1">Email</label>
        <input
          id="email"
          v-model="email"
          type="email"
          required
          autocomplete="email"
          class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
        >
      </div>

      <div>
        <div class="flex items-baseline justify-between mb-1">
          <label for="password" class="block text-sm font-medium text-gray-700">Mot de passe</label>
          <NuxtLink to="/auth/forgot" class="text-xs text-brand-600 hover:text-brand-700">
            Mot de passe oublié ?
          </NuxtLink>
        </div>
        <input
          id="password"
          v-model="password"
          type="password"
          required
          autocomplete="current-password"
          class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
        >
      </div>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <button
        type="submit"
        :disabled="loading"
        class="w-full px-4 py-3 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {{ loading ? 'Connexion…' : 'Se connecter' }}
      </button>
    </form>

    <template #footer>
      Pas encore de compte ?
      <NuxtLink to="/auth/register" class="text-brand-600 hover:text-brand-700 font-medium">
        Créer un compte
      </NuxtLink>
    </template>
  </AuthCard>
</template>

<script setup lang="ts">
definePageMeta({ layout: false })

const authStore = useAuthStore()
const route = useRoute()

const email = ref('')
const password = ref('')
const loading = ref(false)
// Le flux Google redirige ici avec ?error=google quand l'échange du code échoue.
const error = ref(route.query.error === 'google' ? 'La connexion avec Google a échoué, réessayez.' : '')

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    await authStore.login(email.value, password.value)
    await navigateTo('/')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
// La redirection d'un utilisateur déjà connecté est gérée par `auth.global.ts`.
</script>
