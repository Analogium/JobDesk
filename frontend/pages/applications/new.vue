<template>
  <div class="p-8 max-w-2xl">
    <div class="flex items-center gap-4 mb-8">
      <NuxtLink to="/applications" class="text-gray-400 hover:text-gray-600 transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
      </NuxtLink>
      <h1 class="text-2xl font-bold text-gray-900">Nouvelle candidature</h1>
    </div>

    <!-- URL import -->
    <div class="bg-brand-50 border border-brand-200 rounded-xl p-5 mb-6">
      <h2 class="text-sm font-semibold text-brand-700 mb-3">Import depuis une URL</h2>
      <div class="flex gap-2">
        <input
          v-model="importUrl"
          type="url"
          placeholder="https://www.welcometothejungle.com/…"
          class="flex-1 text-sm border-brand-200 rounded-lg focus:ring-brand-500 focus:border-brand-500"
        >
        <button
          :disabled="importLoading || !importUrl"
          class="px-4 py-2 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors disabled:opacity-50"
          @click="importFromUrl"
        >
          {{ importLoading ? '…' : 'Importer' }}
        </button>
      </div>
      <p v-if="importError" class="text-red-500 text-xs mt-2">{{ importError }}</p>
    </div>

    <!-- Form -->
    <form class="bg-white border border-gray-200 rounded-xl p-6 space-y-5" @submit.prevent="submit">
      <div class="grid grid-cols-2 gap-5">
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Entreprise *</label>
          <input v-model="form.companyName" required type="text" class="w-full text-sm border-gray-200 rounded-lg" >
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Poste *</label>
          <input v-model="form.jobTitle" required type="text" class="w-full text-sm border-gray-200 rounded-lg" >
        </div>
      </div>

      <div class="grid grid-cols-2 gap-5">
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Localisation</label>
          <input v-model="form.location" type="text" class="w-full text-sm border-gray-200 rounded-lg" >
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Contrat</label>
          <select v-model="form.contractType" class="w-full text-sm border-gray-200 rounded-lg">
            <option value="">—</option>
            <option v-for="(label, val) in CONTRACT_LABELS" :key="val" :value="val">{{ label }}</option>
          </select>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-5">
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Salaire</label>
          <input v-model="form.salaryRange" type="text" placeholder="45-55k€" class="w-full text-sm border-gray-200 rounded-lg" >
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-700 mb-1">Source</label>
          <select v-model="form.source" class="w-full text-sm border-gray-200 rounded-lg">
            <option v-for="(label, val) in SOURCE_LABELS" :key="val" :value="val">{{ label }}</option>
          </select>
        </div>
      </div>

      <div>
        <label class="block text-xs font-medium text-gray-700 mb-1">Date de candidature</label>
        <input v-model="form.appliedAt" type="date" class="w-full text-sm border-gray-200 rounded-lg" >
      </div>

      <div>
        <label class="block text-xs font-medium text-gray-700 mb-1">Description du poste</label>
        <textarea v-model="form.jobDescription" rows="4" class="w-full text-sm border-gray-200 rounded-lg resize-none" />
      </div>

      <p v-if="error" class="text-red-500 text-xs">{{ error }}</p>

      <div class="flex justify-end gap-3">
        <NuxtLink to="/applications" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors">
          Annuler
        </NuxtLink>
        <button
          type="submit"
          :disabled="submitting"
          class="px-4 py-2 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors disabled:opacity-50"
        >
          {{ submitting ? 'Enregistrement…' : 'Enregistrer' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { CONTRACT_LABELS, SOURCE_LABELS, type ApplicationCreatePayload } from '~/types'

const store = useApplicationsStore()
const { apiFetch } = useApi()

const importUrl = ref('')
const importLoading = ref(false)
const importError = ref('')

const form = reactive<ApplicationCreatePayload>({
  companyName: '',
  jobTitle: '',
  location: '',
  contractType: undefined,
  salaryRange: '',
  source: 'manual',
  appliedAt: undefined,
  jobDescription: '',
  jobUrl: '',
})

const submitting = ref(false)
const error = ref('')

async function importFromUrl() {
  if (!importUrl.value) return
  importLoading.value = true
  importError.value = ''
  try {
    const data = await apiFetch<Partial<ApplicationCreatePayload>>('/api/scrape', {
      method: 'POST',
      body: JSON.stringify({ url: importUrl.value }),
    })
    Object.assign(form, data)
    form.jobUrl = importUrl.value
  } catch (e: any) {
    importError.value = e.message ?? 'Erreur lors de l\'import'
  } finally {
    importLoading.value = false
  }
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    const app = await store.create({ ...form })
    await navigateTo(`/applications/${app.id}`)
  } catch (e: any) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>
