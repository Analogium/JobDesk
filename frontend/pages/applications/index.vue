<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Candidatures</h1>
        <p class="text-gray-500 text-sm mt-1">
          {{ search || filter !== 'ALL'
            ? `${filtered.length} sur ${store.applications.length}`
            : `${store.applications.length} au total` }}
        </p>
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

    <!-- Search -->
    <div class="relative mb-4">
      <svg
        class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
        fill="none" stroke="currentColor" viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <input
        v-model="search"
        type="search"
        placeholder="Rechercher une entreprise, un poste, un lieu…"
        aria-label="Rechercher une candidature"
        class="w-full pl-10 pr-10 py-2.5 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
      >
      <button
        v-if="search"
        type="button"
        aria-label="Effacer la recherche"
        class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
        @click="search = ''"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
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
      {{ search ? `Aucune candidature ne correspond à « ${search} ».` : 'Aucune candidature dans cette catégorie.' }}
    </div>
    <ul v-else class="space-y-3">
      <li
        v-for="app in paginated"
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
              <span
                v-if="hasRecentAutoMail(app)"
                title="Statut mis à jour automatiquement depuis Gmail"
                class="inline-flex items-center gap-1 text-xs text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full"
              >
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
                Gmail
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

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
      <button
        class="px-3 py-1.5 rounded-lg text-sm font-medium border border-gray-200 bg-white text-gray-600 hover:border-brand-300 disabled:opacity-40 disabled:cursor-not-allowed"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        Précédent
      </button>
      <button
        v-for="p in totalPages"
        :key="p"
        class="w-9 h-9 rounded-lg text-sm font-medium border transition-colors"
        :class="p === currentPage
          ? 'bg-brand-600 text-white border-brand-600'
          : 'bg-white text-gray-600 border-gray-200 hover:border-brand-300'"
        @click="currentPage = p"
      >
        {{ p }}
      </button>
      <button
        class="px-3 py-1.5 rounded-lg text-sm font-medium border border-gray-200 bg-white text-gray-600 hover:border-brand-300 disabled:opacity-40 disabled:cursor-not-allowed"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        Suivant
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  STATUS_LABELS,
  STATUS_COLORS,
  CONTRACT_LABELS,
  SOURCE_LABELS,
  type Application,
  type ApplicationStatus,
} from '~/types'

const store = useApplicationsStore()
onMounted(() => store.fetchAll())

const filter = ref<ApplicationStatus | 'ALL'>('ALL')
const search = ref('')

const statusOrder: ApplicationStatus[] = [
  'APPLIED', 'WAITING', 'RELAUNCH', 'INTERVIEW', 'OFFER', 'DRAFT', 'REFUSED', 'ABANDONED',
]

// Filtre par statut d'abord (les compteurs des onglets restent le total par statut),
// puis recherche texte par-dessus. Les deux se cumulent.
const filtered = computed(() => {
  const base = filter.value === 'ALL'
    ? store.applications
    : (store.byStatus[filter.value] ?? [])
  const q = search.value.trim()
  return q ? base.filter(app => matchesQuery(app, q)) : base
})

const pageSize = 20
const currentPage = ref(1)

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))

const paginated = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

// Revenir à la première page quand on change de filtre ou de recherche
watch([filter, search], () => { currentPage.value = 1 })

// Éviter de rester sur une page vide si la liste rétrécit (suppression, etc.)
watch(totalPages, (n) => { if (currentPage.value > n) currentPage.value = n })

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })
}

function hasRecentAutoMail(app: Application): boolean {
  const cutoff = Date.now() - 24 * 60 * 60 * 1000
  return app.statusHistories.some(
    h => h.trigger === 'auto_mail' && new Date(h.changedAt).getTime() > cutoff,
  )
}
</script>
