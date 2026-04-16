import { defineStore } from 'pinia'
import type { Application, ApplicationCreatePayload, ApplicationStatus } from '~/types'

export const useApplicationsStore = defineStore('applications', () => {
  const { apiFetch } = useApi()

  const applications = ref<Application[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      const data = await apiFetch<{ 'hydra:member': Application[] }>('/api/applications?order[createdAt]=desc')
      applications.value = data['hydra:member']
    } catch (e: any) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  async function fetchOne(id: string): Promise<Application> {
    return apiFetch<Application>(`/api/applications/${id}`)
  }

  async function create(payload: ApplicationCreatePayload): Promise<Application> {
    const app = await apiFetch<Application>('/api/applications', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    applications.value.unshift(app)
    return app
  }

  async function update(id: string, payload: Partial<ApplicationCreatePayload & { status: ApplicationStatus }>): Promise<Application> {
    const app = await apiFetch<Application>(`/api/applications/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/merge-patch+json' },
      body: JSON.stringify(payload),
    })
    const idx = applications.value.findIndex(a => a.id === id)
    if (idx !== -1) applications.value[idx] = app
    return app
  }

  async function remove(id: string): Promise<void> {
    await apiFetch(`/api/applications/${id}`, { method: 'DELETE' })
    applications.value = applications.value.filter(a => a.id !== id)
  }

  const byStatus = computed(() => {
    const map: Partial<Record<ApplicationStatus, Application[]>> = {}
    for (const app of applications.value) {
      if (!map[app.status]) map[app.status] = []
      map[app.status]!.push(app)
    }
    return map
  })

  const activeCount = computed(() =>
    applications.value.filter(a => !['REFUSED', 'ABANDONED'].includes(a.status)).length
  )

  return { applications, loading, error, fetchAll, fetchOne, create, update, remove, byStatus, activeCount }
})
