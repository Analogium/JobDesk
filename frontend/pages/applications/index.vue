<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Candidatures</h1>
        <p class="text-gray-500 text-sm mt-1">{{ store.applications.length }} au total</p>
      </div>
      <NuxtLink
        to="/applications/new"
        class="flex items-center gap-2 px-4 py-2 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Ajouter
      </NuxtLink>
    </div>

    <!-- Filters -->
    <div class="flex gap-2 mb-6 flex-wrap">
      <button
        v-for="s in ['ALL', ...statusOrder]"
        :key="s"
        class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors border"
        :class="filter === s
          ? 'bg-brand-600 text-white border-brand-600'
          : 'bg-white text-gray-600 border-gray-200 hover:border-brand-300'"
        @click="filter = s as any"
      >
        {{ s === 'ALL' ? 'Toutes' : STATUS_LABELS[s as ApplicationStatus] }}
        <span v-if="s !== 'ALL'" class="ml-1 opacity-70">
          {{ (store.byStatus[s as ApplicationStatus] ?? []).length }}
        </span>
      </button>
    </div>

    <!-- List -->
    <div v-if="store.loading" class="text-sm text-gray-400">Chargement…</div>
    <div v-else-if="!filtered.length" class="text-sm text-gray-400 py-12 text-center">
      Aucune candidature dans cette catégorie.
    </div>
    <ul v-else class="space-y-3">
      <li
        v-for="app in filtered"
        :key="app.id"
        class="bg-white border border-gray-200 rounded-xl p-5 hover:border-brand-300 transition-colors cursor-pointer"
        @click="navigateTo(`/applications/${app.id}`)"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <h3 class="font-semibold text-gray-900 truncate">{{ app.companyName }}</h3>
              <span
                class="text-xs shrink-0"
                :class="STATUS_COLORS[app.status]"
                style="padding: 2px 8px; border-radius: 9999px;"
              >
                {{ STATUS_LABELS[app.status] }}
              </span>
            </div>
            <p class="text-sm text-gray-600 truncate">{{ app.jobTitle }}</p>
            <div class="flex items-center gap-4 mt-2 text-xs text-gray-400">
              <span v-if="app.location">{{ app.location }}</span>
              <span v-if="app.contractType">{{ CONTRACT_LABELS[app.contractType] }}</span>
              <span>{{ SOURCE_LABELS[app.source] }}</span>
              <span v-if="app.appliedAt">Postulé le {{ formatDate(app.appliedAt) }}</span>
            </div>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import {
  STATUS_LABELS,
  STATUS_COLORS,
  CONTRACT_LABELS,
  SOURCE_LABELS,
  type ApplicationStatus,
} from '~/types'

const store = useApplicationsStore()
onMounted(() => store.fetchAll())

const filter = ref<ApplicationStatus | 'ALL'>('ALL')

const statusOrder: ApplicationStatus[] = [
  'APPLIED', 'WAITING', 'RELAUNCH', 'INTERVIEW', 'OFFER', 'DRAFT', 'REFUSED', 'ABANDONED',
]

const filtered = computed(() =>
  filter.value === 'ALL'
    ? store.applications
    : (store.byStatus[filter.value] ?? [])
)

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })
}
</script>
