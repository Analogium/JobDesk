<template>
  <AuthCard subtitle="Réinitialiser votre mot de passe">
    <!--
      Le message de confirmation est volontairement identique que l'adresse existe ou
      non : afficher « compte inconnu » révélerait qui est inscrit.
    -->
    <div v-if="sent" class="text-sm text-gray-600 space-y-3">
      <p class="text-green-700">
        Si un compte existe avec cette adresse, un lien de réinitialisation vient d'y être envoyé.
      </p>
      <p class="text-gray-500">
        Le lien est valable une heure. Pensez à regarder dans vos spams.
      </p>
    </div>

    <form v-else class="space-y-4" @submit.prevent="onSubmit">
      <p class="text-sm text-gray-500">
        Saisissez votre adresse : nous vous enverrons un lien pour choisir un nouveau mot de passe.
      </p>

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

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <button
        type="submit"
        :disabled="loading"
        class="w-full px-4 py-3 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {{ loading ? 'Envoi…' : 'Envoyer le lien' }}
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

const email = ref('')
const loading = ref(false)
const sent = ref(false)
const error = ref('')

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    await authStore.forgotPassword(email.value)
    sent.value = true
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>
