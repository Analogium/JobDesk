<template>
  <div class="p-8 max-w-3xl">
    <div class="flex items-center gap-4 mb-8">
      <NuxtLink to="/applications" class="text-gray-400 hover:text-gray-600 transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
        </svg>
      </NuxtLink>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ app?.companyName }}</h1>
        <p class="text-gray-500 text-sm">{{ app?.jobTitle }}</p>
      </div>
    </div>

    <div v-if="loading" class="text-sm text-gray-400">Chargement…</div>
    <template v-else-if="app">
      <!-- Status selector -->
      <div class="bg-white border border-gray-200 rounded-xl p-5 mb-6">
        <h2 class="text-sm font-semibold text-gray-700 mb-3">Statut</h2>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="s in statusOrder"
            :key="s"
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-all border"
            :class="app.status === s
              ? STATUS_COLORS[s] + ' border-transparent'
              : 'bg-white text-gray-500 border-gray-200 hover:border-gray-300'"
            @click="changeStatus(s)"
          >
            {{ STATUS_LABELS[s] }}
          </button>
        </div>
      </div>

      <!-- Info -->
      <div class="bg-white border border-gray-200 rounded-xl p-5 mb-6 space-y-4">
        <h2 class="text-sm font-semibold text-gray-700">Informations</h2>
        <dl class="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
          <div v-if="app.location">
            <dt class="text-gray-500">Localisation</dt>
            <dd class="font-medium text-gray-900">{{ app.location }}</dd>
          </div>
          <div v-if="app.contractType">
            <dt class="text-gray-500">Contrat</dt>
            <dd class="font-medium text-gray-900">{{ CONTRACT_LABELS[app.contractType] }}</dd>
          </div>
          <div v-if="app.salaryRange">
            <dt class="text-gray-500">Salaire</dt>
            <dd class="font-medium text-gray-900">{{ app.salaryRange }}</dd>
          </div>
          <div>
            <dt class="text-gray-500">Source</dt>
            <dd class="font-medium text-gray-900">{{ SOURCE_LABELS[app.source] }}</dd>
          </div>
          <div v-if="app.appliedAt">
            <dt class="text-gray-500">Date de candidature</dt>
            <dd class="font-medium text-gray-900">{{ formatDate(app.appliedAt) }}</dd>
          </div>
          <div v-if="app.jobUrl">
            <dt class="text-gray-500">Offre</dt>
            <dd>
              <a :href="app.jobUrl" target="_blank" class="text-brand-600 hover:underline text-xs truncate block max-w-xs">
                Voir l'offre
              </a>
            </dd>
          </div>
        </dl>
      </div>

      <!-- Notes -->
      <div class="bg-white border border-gray-200 rounded-xl p-5 mb-6">
        <h2 class="text-sm font-semibold text-gray-700 mb-3">Notes</h2>
        <textarea
          v-model="notesValue"
          rows="4"
          placeholder="Ajouter des notes…"
          class="w-full text-sm border-gray-200 rounded-lg resize-none focus:ring-brand-500 focus:border-brand-500"
          @blur="saveNotes"
        />
      </div>

      <!-- Status history -->
      <div v-if="app.statusHistories.length" class="bg-white border border-gray-200 rounded-xl p-5 mb-6">
        <h2 class="text-sm font-semibold text-gray-700 mb-3">Historique</h2>
        <ul class="space-y-2">
          <li
            v-for="h in app.statusHistories"
            :key="h.id"
            class="flex items-center gap-3 text-xs text-gray-500"
          >
            <span class="text-gray-400 shrink-0">{{ formatDate(h.changedAt) }}</span>
            <span v-if="h.previousStatus" :class="STATUS_COLORS[h.previousStatus]" style="padding: 1px 6px; border-radius: 9999px;">
              {{ STATUS_LABELS[h.previousStatus] }}
            </span>
            <span class="text-gray-400">→</span>
            <span :class="STATUS_COLORS[h.newStatus]" style="padding: 1px 6px; border-radius: 9999px;">
              {{ STATUS_LABELS[h.newStatus] }}
            </span>
            <span v-if="h.trigger === 'auto_mail'" class="text-gray-400">(auto)</span>
          </li>
        </ul>
      </div>

      <!-- Delete -->
      <button
        class="text-xs text-red-500 hover:text-red-700 transition-colors"
        @click="deleteApp"
      >
        Supprimer cette candidature
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
import {
  STATUS_LABELS,
  STATUS_COLORS,
  CONTRACT_LABELS,
  SOURCE_LABELS,
  type ApplicationStatus,
  type Application,
} from '~/types'

const route = useRoute()
const store = useApplicationsStore()

const app = ref<Application | null>(null)
const loading = ref(true)
const notesValue = ref('')

const statusOrder: ApplicationStatus[] = [
  'DRAFT', 'APPLIED', 'WAITING', 'RELAUNCH', 'INTERVIEW', 'OFFER', 'REFUSED', 'ABANDONED',
]

onMounted(async () => {
  app.value = await store.fetchOne(route.params.id as string)
  notesValue.value = app.value?.notes ?? ''
  loading.value = false
})

async function changeStatus(status: ApplicationStatus) {
  if (!app.value || app.value.status === status) return
  app.value = await store.update(app.value.id, { status })
}

async function saveNotes() {
  if (!app.value) return
  app.value = await store.update(app.value.id, { notes: notesValue.value })
}

async function deleteApp() {
  if (!app.value || !confirm('Supprimer cette candidature ?')) return
  await store.remove(app.value.id)
  await navigateTo('/applications')
}

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
}
</script>
