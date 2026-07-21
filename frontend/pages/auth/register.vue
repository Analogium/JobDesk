<template>
  <AuthCard subtitle="Créez votre compte">
    <form class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
        <input
          id="name"
          v-model="name"
          type="text"
          required
          autocomplete="name"
          class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
        >
      </div>

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
        <label for="password" class="block text-sm font-medium text-gray-700 mb-1">Mot de passe</label>
        <input
          id="password"
          v-model="password"
          type="password"
          required
          minlength="8"
          autocomplete="new-password"
          class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
        >
        <p class="text-xs text-gray-400 mt-1">8 caractères minimum</p>
      </div>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <button
        type="submit"
        :disabled="loading"
        class="w-full px-4 py-3 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {{ loading ? 'Création…' : 'Créer mon compte' }}
      </button>
    </form>

    <template #footer>
      Déjà un compte ?
      <NuxtLink to="/auth/login" class="text-brand-600 hover:text-brand-700 font-medium">
        Se connecter
      </NuxtLink>
    </template>
  </AuthCard>
</template>

<script setup lang="ts">
definePageMeta({ layout: false })

const authStore = useAuthStore()

const name = ref('')
const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    await authStore.register(email.value, name.value, password.value)
    await navigateTo('/')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>
