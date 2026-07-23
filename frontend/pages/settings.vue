<template>
  <div class="p-8 max-w-2xl">
    <h1 class="text-2xl font-bold text-gray-900 mb-8">
      Paramètres
    </h1>

    <!-- Gmail card -->
    <div class="bg-white rounded-xl border border-gray-200 p-6">
      <div class="flex items-start gap-4">
        <!-- Gmail icon -->
        <div class="w-10 h-10 rounded-lg bg-red-50 flex items-center justify-center flex-shrink-0">
          <svg class="w-5 h-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4-8 5-8-5V6l8 5 8-5v2z" />
          </svg>
        </div>

        <div class="flex-1 min-w-0">
          <h2 class="text-base font-semibold text-gray-900">
            Analyse automatique des mails
          </h2>
          <p class="text-sm text-gray-500 mt-0.5">
            Détecte les réponses (refus, entretien, offre) dans Gmail et met à jour tes candidatures automatiquement.
          </p>

          <!-- Status -->
          <div class="mt-4 flex items-center gap-2">
            <span
              class="inline-flex items-center gap-1.5 text-sm font-medium"
              :class="gmailStatus?.connected ? 'text-green-600' : 'text-gray-400'"
            >
              <span
                class="w-2 h-2 rounded-full"
                :class="gmailStatus?.connected ? 'bg-green-500' : 'bg-gray-300'"
              />
              {{ gmailStatus?.connected ? 'Connecté' : 'Non connecté' }}
            </span>
          </div>

          <div v-if="gmailStatus?.connected && gmailStatus.lastMailScanAt" class="mt-1 text-xs text-gray-400">
            Dernier scan : {{ formatDate(gmailStatus.lastMailScanAt) }}
          </div>

          <!-- Actions -->
          <div class="mt-5 flex flex-wrap gap-3">
            <button
              v-if="!gmailStatus?.connected"
              class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-brand-600 text-white hover:bg-brand-700 transition-colors"
              @click="connectGmail"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
              Connecter Gmail
            </button>

            <template v-if="gmailStatus?.connected">
              <button
                :disabled="scanning"
                class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-brand-600 text-white hover:bg-brand-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                @click="scanNow"
              >
                <svg
                  class="w-4 h-4"
                  :class="{ 'animate-spin': scanning }"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                {{ scanning ? 'Scan en cours…' : 'Scanner maintenant' }}
              </button>

              <button
                class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg border border-red-200 text-red-600 hover:bg-red-50 transition-colors"
                @click="disconnect"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
                Déconnecter
              </button>
            </template>
          </div>

          <!-- Scan result feedback -->
          <div v-if="scanResult" class="mt-4 p-3 rounded-lg bg-green-50 border border-green-200 text-sm text-green-700">
            Scan terminé — {{ scanResult.mailsAnalyzed }} mail(s) analysé(s), {{ scanResult.matchesFound }} correspondance(s) trouvée(s).
          </div>
          <div v-if="errorMessage" class="mt-4 p-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-600">
            {{ errorMessage }}
          </div>
        </div>
      </div>
    </div>

    <!-- Mes données (RGPD) -->
    <div class="bg-white rounded-xl border border-gray-200 p-6 mt-6">
      <h2 class="font-semibold text-gray-900">Mes données</h2>
      <p class="text-sm text-gray-500 mt-1">
        Vous pouvez récupérer une copie de vos données à tout moment, ou supprimer
        définitivement votre compte.
        <NuxtLink to="/legal/confidentialite" class="text-brand-600 hover:text-brand-700">
          Politique de confidentialité
        </NuxtLink>
      </p>

      <div class="mt-4">
        <button
          type="button"
          :disabled="exporting"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
          @click="exportData"
        >
          {{ exporting ? 'Préparation…' : 'Télécharger mes données (JSON)' }}
        </button>
        <p class="text-xs text-gray-400 mt-2">
          Compte, candidatures, historiques de statut, contacts et journaux de scan.
        </p>
      </div>
    </div>

    <!-- Zone de danger -->
    <div class="bg-white rounded-xl border border-red-200 p-6 mt-6">
      <h2 class="font-semibold text-red-700">Supprimer mon compte</h2>
      <p class="text-sm text-gray-500 mt-1">
        Efface définitivement votre compte et toutes vos candidatures. Cette action est
        immédiate et irréversible : pensez à exporter vos données avant.
      </p>

      <button
        v-if="!confirmingDelete"
        type="button"
        class="mt-4 px-4 py-2 border border-red-300 text-red-700 rounded-lg text-sm font-medium hover:bg-red-50 transition-colors"
        @click="confirmingDelete = true"
      >
        Supprimer mon compte
      </button>

      <form v-else class="mt-4 space-y-3" @submit.prevent="deleteAccount">
        <!-- Le mot de passe n'est demandé qu'aux comptes qui en ont un (pas les comptes Google). -->
        <div v-if="hasPassword">
          <label for="deletePassword" class="block text-sm font-medium text-gray-700 mb-1">
            Confirmez avec votre mot de passe
          </label>
          <input
            id="deletePassword"
            v-model="deletePassword"
            type="password"
            required
            autocomplete="current-password"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
          >
        </div>

        <p v-if="deleteError" class="text-sm text-red-600">{{ deleteError }}</p>

        <div class="flex gap-2">
          <button
            type="submit"
            :disabled="deleting"
            class="px-4 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {{ deleting ? 'Suppression…' : 'Supprimer définitivement' }}
          </button>
          <button
            type="button"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
            @click="cancelDelete"
          >
            Annuler
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
const { apiFetch } = useApi()
const route = useRoute()

interface GmailStatus {
  connected: boolean
  lastMailScanAt: string | null
}

interface ScanResult {
  mailsAnalyzed: number
  matchesFound: number
}

const gmailStatus = ref<GmailStatus | null>(null)
const scanning = ref(false)
const scanResult = ref<ScanResult | null>(null)
const errorMessage = ref<string | null>(null)

// ── Mes données (RGPD) ───────────────────────────────────────────────────────
const authStore = useAuthStore()
const config = useRuntimeConfig()

const exporting = ref(false)
const confirmingDelete = ref(false)
const deletePassword = ref('')
const deleteError = ref<string | null>(null)
const deleting = ref(false)

const hasPassword = computed(() => authStore.user?.hasPassword ?? true)

/**
 * Téléchargement via un blob plutôt qu'un lien direct : l'endpoint est protégé par le
 * header Authorization, qu'une navigation classique n'enverrait pas.
 */
async function exportData() {
  exporting.value = true
  errorMessage.value = null
  try {
    const res = await fetch(`${config.public.apiUrl}/api/me/export`, {
      headers: { Authorization: `Bearer ${authStore.token}` },
    })
    if (!res.ok) throw new Error('Export impossible, réessayez.')

    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `jobdesk-donnees-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : 'Export impossible'
  } finally {
    exporting.value = false
  }
}

function cancelDelete() {
  confirmingDelete.value = false
  deletePassword.value = ''
  deleteError.value = null
}

async function deleteAccount() {
  deleting.value = true
  deleteError.value = null
  try {
    await apiFetch('/api/me', {
      method: 'DELETE',
      body: JSON.stringify({ password: deletePassword.value }),
    })
    // Le compte n'existe plus : on efface la session locale avant de sortir.
    authStore.logout()
    await navigateTo('/auth/login')
  } catch (e: unknown) {
    deleteError.value = e instanceof Error ? e.message : 'Suppression impossible'
  } finally {
    deleting.value = false
  }
}

async function connectGmail() {
  errorMessage.value = null
  try {
    const data = await apiFetch<{ url: string }>('/api/gmail/connect', {
      headers: { Accept: 'application/json' },
    })
    window.location.href = data.url
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : 'Erreur lors de la connexion Gmail'
  }
}

async function fetchStatus() {
  try {
    gmailStatus.value = await apiFetch<GmailStatus>('/api/gmail/status', {
      headers: { Accept: 'application/json' },
    })
  } catch {
    // ignore
  }
}

async function scanNow() {
  scanning.value = true
  scanResult.value = null
  errorMessage.value = null
  try {
    scanResult.value = await apiFetch<ScanResult>('/api/gmail/scan', {
      method: 'POST',
      headers: { Accept: 'application/json' },
    })
    await fetchStatus()
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : 'Erreur lors du scan'
  } finally {
    scanning.value = false
  }
}

async function disconnect() {
  errorMessage.value = null
  try {
    await apiFetch('/api/gmail/disconnect', {
      method: 'DELETE',
      headers: { Accept: 'application/json' },
    })
    await fetchStatus()
    scanResult.value = null
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : 'Erreur lors de la déconnexion'
  }
}

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}

// Handle redirect from Gmail OAuth callback
onMounted(() => {
  if (route.query.gmail === 'connected') {
    scanResult.value = null
  } else if (route.query.gmail === 'error') {
    errorMessage.value = 'La connexion Gmail a échoué. Réessaie.'
  }
})

await fetchStatus()
</script>
