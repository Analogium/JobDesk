<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-3xl mx-auto px-4 py-10">
      <!-- En-tête -->
      <header class="mb-8">
        <div class="flex items-center gap-2 text-xs font-medium text-brand-600 mb-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          Vue partagée · lecture seule
        </div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ data ? headerTitle : 'Candidatures partagées' }}
        </h1>
      </header>

      <!-- Erreur : lien invalide ou expiré -->
      <div v-if="error" class="bg-white border border-gray-200 rounded-xl p-8 text-center">
        <h2 class="font-semibold text-gray-900">Lien indisponible</h2>
        <p class="text-sm text-gray-500 mt-2">
          Ce lien de partage est invalide, a été révoqué ou a expiré. Demandez-en un
          nouveau à la personne qui vous l'a transmis.
        </p>
      </div>

      <!-- Chargement -->
      <div v-else-if="pending" class="text-sm text-gray-400">Chargement…</div>

      <!-- Liste en lecture seule -->
      <template v-else-if="data">
        <p class="text-sm text-gray-500 mb-6">
          {{ data.applications.length }}
          candidature{{ data.applications.length > 1 ? 's' : '' }}
        </p>

        <div v-if="!data.applications.length" class="text-sm text-gray-400 py-12 text-center">
          Aucune candidature à afficher.
        </div>

        <ul v-else class="space-y-3">
          <li
            v-for="app in data.applications"
            :key="app.id"
            class="bg-white border border-gray-200 rounded-xl p-5"
          >
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
            <div class="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 text-xs text-gray-400">
              <span v-if="app.location">{{ app.location }}</span>
              <span v-if="app.contractType">{{ CONTRACT_LABELS[app.contractType] }}</span>
              <span v-if="app.salaryRange">{{ app.salaryRange }}</span>
              <span>{{ SOURCE_LABELS[app.source] }}</span>
              <span v-if="app.appliedAt">Postulé le {{ formatDate(app.appliedAt) }}</span>
            </div>
          </li>
        </ul>
      </template>

      <footer class="mt-12 text-center text-xs text-gray-400">
        Propulsé par <NuxtLink to="/" class="text-brand-600 hover:text-brand-700">JobDesk</NuxtLink>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  STATUS_LABELS,
  STATUS_COLORS,
  CONTRACT_LABELS,
  SOURCE_LABELS,
  type SharedView,
} from '~/types'

// Page publique, sans le shell applicatif : destinée à un tiers non authentifié.
definePageMeta({ layout: false })

const route = useRoute()
const config = useRuntimeConfig()
const token = route.params.token as string

const { data, error, pending } = await useAsyncData(
  `shared-${token}`,
  () => $fetch<SharedView>(`${config.public.apiUrl}/api/shared/${encodeURIComponent(token)}`),
)

const headerTitle = computed(() =>
  data.value?.ownerName
    ? `Candidatures de ${data.value.ownerName}`
    : 'Candidatures partagées')

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })
}

useHead({ title: 'Candidatures partagées — JobDesk' })
</script>
