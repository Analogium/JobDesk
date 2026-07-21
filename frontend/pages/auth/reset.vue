<template>
  <AuthCard subtitle="Choisir un nouveau mot de passe">
    <div v-if="!token" class="text-sm text-red-600">
      Ce lien est incomplet. Refaites une demande depuis la page « mot de passe oublié ».
    </div>

    <div v-else-if="done" class="text-sm text-gray-600 space-y-4">
      <p class="text-green-700">Votre mot de passe a été mis à jour.</p>
      <NuxtLink
        to="/auth/login"
        class="block w-full text-center px-4 py-3 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors"
      >
        Se connecter
      </NuxtLink>
    </div>

    <form v-else class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label for="password" class="block text-sm font-medium text-gray-700 mb-1">
          Nouveau mot de passe
        </label>
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

      <div>
        <label for="passwordConfirm" class="block text-sm font-medium text-gray-700 mb-1">
          Confirmer le mot de passe
        </label>
        <input
          id="passwordConfirm"
          v-model="passwordConfirm"
          type="password"
          required
          autocomplete="new-password"
          class="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
          :class="mismatch ? 'border-red-400' : 'border-gray-300'"
        >
        <p v-if="mismatch" class="text-xs text-red-600 mt-1">
          Les deux mots de passe ne correspondent pas
        </p>
      </div>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <button
        type="submit"
        :disabled="loading || mismatch"
        class="w-full px-4 py-3 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {{ loading ? 'Enregistrement…' : 'Changer mon mot de passe' }}
      </button>
    </form>

    <template #footer>
      <NuxtLink to="/auth/login" class="text-brand-600 hover:text-brand-700 font-medium">
        Retour à la connexion
      </NuxtLink>
    </template>
  </AuthCard>
</template>

<script setup lang="ts">
definePageMeta({ layout: false })

const authStore = useAuthStore()
const route = useRoute()

const token = computed(() => (route.query.token as string | undefined) ?? '')
const password = ref('')
const passwordConfirm = ref('')
const loading = ref(false)
const done = ref(false)
const error = ref('')

const mismatch = computed(() => passwordConfirm.value !== '' && passwordConfirm.value !== password.value)

async function onSubmit() {
  if (password.value !== passwordConfirm.value) return
  loading.value = true
  error.value = ''
  try {
    await authStore.resetPassword(token.value, password.value)
    done.value = true
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>
