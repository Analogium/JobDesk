<template>
  <div class="p-8">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">Dashboard</h1>
      <p class="text-gray-500 text-sm mt-1">Vue d'ensemble de ta recherche d'emploi</p>
    </div>

    <!-- Stats cards -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      <StatCard label="Total" :value="store.applications.length" />
      <StatCard label="En cours" :value="store.activeCount" color="blue" />
      <StatCard
        label="Entretiens"
        :value="(store.byStatus['INTERVIEW'] ?? []).length"
        color="purple"
      />
      <StatCard
        label="Offres"
        :value="(store.byStatus['OFFER'] ?? []).length"
        color="green"
      />
    </div>

    <!-- Status breakdown -->
    <div class="bg-white rounded-xl border border-gray-200 p-6 mb-6">
      <h2 class="text-sm font-semibold text-gray-700 mb-4">Répartition par statut</h2>
      <div class="space-y-3">
        <div
          v-for="status in statusOrder"
          :key="status"
          class="flex items-center gap-3"
        >
          <span class="text-xs w-28 truncate" :class="STATUS_COLORS[status]"
            style="padding: 2px 8px; border-radius: 9999px; display: inline-block;">
            {{ STATUS_LABELS[status] }}
          </span>
          <div class="flex-1 bg-gray-100 rounded-full h-2">
            <div
              class="bg-brand-500 h-2 rounded-full transition-all"
              :style="{ width: barWidth(status) }"
            />
          </div>
          <span class="text-xs text-gray-500 w-4 text-right">
            {{ (store.byStatus[status] ?? []).length }}
          </span>
        </div>
      </div>
    </div>

    <!-- Recent applications -->
    <div class="bg-white rounded-xl border border-gray-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-sm font-semibold text-gray-700">Dernières candidatures</h2>
        <NuxtLink to="/applications" class="text-xs text-brand-600 hover:underline">
          Voir tout
        </NuxtLink>
      </div>
      <div v-if="store.loading" class="text-sm text-gray-400">Chargement…</div>
      <div v-else-if="!store.applications.length" class="text-sm text-gray-400">
        Aucune candidature pour l'instant.
        <NuxtLink to="/applications/new" class="text-brand-600 hover:underline ml-1">Ajouter</NuxtLink>
      </div>
      <ul v-else class="divide-y divide-gray-100">
        <li
          v-for="app in recent"
          :key="app.id"
          class="py-3 flex items-center gap-4"
        >
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-900 truncate">{{ app.companyName }}</p>
            <p class="text-xs text-gray-500 truncate">{{ app.jobTitle }}</p>
          </div>
          <span
            class="text-xs shrink-0"
            :class="STATUS_COLORS[app.status]"
            style="padding: 2px 8px; border-radius: 9999px;"
          >
            {{ STATUS_LABELS[app.status] }}
          </span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { STATUS_LABELS, STATUS_COLORS, type ApplicationStatus } from '~/types'

const store = useApplicationsStore()

onMounted(() => store.fetchAll())

const statusOrder: ApplicationStatus[] = [
  'APPLIED', 'WAITING', 'RELAUNCH', 'INTERVIEW', 'OFFER', 'DRAFT', 'REFUSED', 'ABANDONED',
]

const recent = computed(() => store.applications.slice(0, 5))

function barWidth(status: ApplicationStatus) {
  const total = store.applications.length
  if (!total) return '0%'
  const count = (store.byStatus[status] ?? []).length
  return `${Math.round((count / total) * 100)}%`
}
</script>
