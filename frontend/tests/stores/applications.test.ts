import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useApplicationsStore } from '~/stores/applications'
import type { Application } from '~/types'

// ── Mock useApi ───────────────────────────────────────────────────────────────
const mockApiFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ apiFetch: mockApiFetch }),
}))

// ── Helpers ───────────────────────────────────────────────────────────────────
function makeApp(overrides: Partial<Application> = {}): Application {
  return {
    id: '550e8400-e29b-41d4-a716-446655440000',
    companyName: 'ACME',
    jobTitle: 'Developer',
    status: 'APPLIED',
    source: 'MANUAL',
    createdAt: '2026-01-01T00:00:00+00:00',
    updatedAt: '2026-01-01T00:00:00+00:00',
    statusHistories: [],
    contacts: [],
    ...overrides,
  }
}

describe('useApplicationsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockApiFetch.mockReset()
  })

  // ── Initial state ───────────────────────────────────────────────────────────

  it('starts with empty applications, not loading, no error', () => {
    const store = useApplicationsStore()
    expect(store.applications).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  // ── fetchAll ────────────────────────────────────────────────────────────────

  it('fetchAll populates applications from API', async () => {
    const apps = [makeApp({ id: '1', companyName: 'Google' }), makeApp({ id: '2', companyName: 'Meta' })]
    mockApiFetch.mockResolvedValueOnce({ member: apps })

    const store = useApplicationsStore()
    await store.fetchAll()

    expect(store.applications).toEqual(apps)
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(mockApiFetch).toHaveBeenCalledWith('/api/applications?order[createdAt]=desc')
  })

  it('fetchAll sets error on failure', async () => {
    mockApiFetch.mockRejectedValueOnce(new Error('Network error'))

    const store = useApplicationsStore()
    await store.fetchAll()

    expect(store.applications).toEqual([])
    expect(store.error).toBe('Network error')
    expect(store.loading).toBe(false)
  })

  // ── create ──────────────────────────────────────────────────────────────────

  it('create adds application to the front of the list', async () => {
    const existing = makeApp({ id: '1', companyName: 'Old Co' })
    const newApp   = makeApp({ id: '2', companyName: 'New Co' })
    mockApiFetch.mockResolvedValueOnce(newApp)

    const store = useApplicationsStore()
    store.applications = [existing]

    const result = await store.create({ companyName: 'New Co', jobTitle: 'Dev', status: 'APPLIED', source: 'MANUAL' })

    expect(result).toEqual(newApp)
    expect(store.applications[0]).toEqual(newApp)
    expect(store.applications).toHaveLength(2)
    expect(mockApiFetch).toHaveBeenCalledWith('/api/applications', expect.objectContaining({ method: 'POST' }))
  })

  // ── update ──────────────────────────────────────────────────────────────────

  it('update replaces the application in the list', async () => {
    const original = makeApp({ id: '1', status: 'APPLIED' })
    const updated  = makeApp({ id: '1', status: 'INTERVIEW' })
    mockApiFetch.mockResolvedValueOnce(updated)

    const store = useApplicationsStore()
    store.applications = [original]

    const result = await store.update('1', { status: 'INTERVIEW' })

    expect(result).toEqual(updated)
    expect(store.applications[0].status).toBe('INTERVIEW')
    expect(mockApiFetch).toHaveBeenCalledWith(
      '/api/applications/1',
      expect.objectContaining({ method: 'PATCH' }),
    )
  })

  it('update does not change list when id not found', async () => {
    const app = makeApp({ id: '1' })
    mockApiFetch.mockResolvedValueOnce(makeApp({ id: '99', companyName: 'Unknown' }))

    const store = useApplicationsStore()
    store.applications = [app]

    await store.update('99', { companyName: 'Unknown' })

    expect(store.applications).toHaveLength(1)
    expect(store.applications[0].id).toBe('1')
  })

  // ── remove ──────────────────────────────────────────────────────────────────

  it('remove deletes the application from the list', async () => {
    const app1 = makeApp({ id: '1' })
    const app2 = makeApp({ id: '2' })
    mockApiFetch.mockResolvedValueOnce(undefined)

    const store = useApplicationsStore()
    store.applications = [app1, app2]

    await store.remove('1')

    expect(store.applications).toHaveLength(1)
    expect(store.applications[0].id).toBe('2')
    expect(mockApiFetch).toHaveBeenCalledWith('/api/applications/1', expect.objectContaining({ method: 'DELETE' }))
  })

  // ── computed ────────────────────────────────────────────────────────────────

  it('byStatus groups applications by status', () => {
    const store = useApplicationsStore()
    store.applications = [
      makeApp({ id: '1', status: 'APPLIED' }),
      makeApp({ id: '2', status: 'APPLIED' }),
      makeApp({ id: '3', status: 'INTERVIEW' }),
    ]

    expect(store.byStatus['APPLIED']).toHaveLength(2)
    expect(store.byStatus['INTERVIEW']).toHaveLength(1)
    expect(store.byStatus['REFUSED']).toBeUndefined()
  })

  it('activeCount excludes REFUSED and ABANDONED', () => {
    const store = useApplicationsStore()
    store.applications = [
      makeApp({ id: '1', status: 'APPLIED' }),
      makeApp({ id: '2', status: 'INTERVIEW' }),
      makeApp({ id: '3', status: 'REFUSED' }),
      makeApp({ id: '4', status: 'ABANDONED' }),
    ]

    expect(store.activeCount).toBe(2)
  })
})
