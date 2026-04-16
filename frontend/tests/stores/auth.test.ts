import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '~/stores/auth'

// Mock fetch globally
const mockFetch = vi.fn()
vi.stubGlobal('fetch', mockFetch)

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem:    (k: string) => store[k] ?? null,
    setItem:    (k: string, v: string) => { store[k] = v },
    removeItem: (k: string) => { Reflect.deleteProperty(store, k) },
    clear:      () => { store = {} },
  }
})()
vi.stubGlobal('localStorage', localStorageMock)

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorageMock.clear()
    mockFetch.mockReset()
  })

  it('starts with null token and user', () => {
    const auth = useAuthStore()
    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('setToken stores token and marks as authenticated', () => {
    const auth = useAuthStore()
    auth.setToken('my-jwt')
    expect(auth.token).toBe('my-jwt')
    expect(auth.isAuthenticated).toBe(true)
    expect(localStorageMock.getItem('jobdesk_token')).toBe('my-jwt')
  })

  it('logout clears token, user and localStorage', () => {
    const auth = useAuthStore()
    auth.setToken('my-jwt')
    auth.logout()
    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
    expect(localStorageMock.getItem('jobdesk_token')).toBeNull()
  })

  it('fetchMe sets user on success', async () => {
    const auth = useAuthStore()
    auth.setToken('valid-jwt')

    const fakeUser = { id: '1', email: 'test@example.com', name: 'Test' }
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => fakeUser,
    })

    await auth.fetchMe()

    expect(auth.user).toEqual(fakeUser)
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/me',
      expect.objectContaining({ headers: { Authorization: 'Bearer valid-jwt' } }),
    )
  })

  it('fetchMe calls logout when response is not ok', async () => {
    const auth = useAuthStore()
    auth.setToken('expired-jwt')
    mockFetch.mockResolvedValueOnce({ ok: false })

    await auth.fetchMe()

    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
  })

  it('fetchMe calls logout on network error', async () => {
    const auth = useAuthStore()
    auth.setToken('valid-jwt')
    mockFetch.mockRejectedValueOnce(new Error('Network error'))

    await auth.fetchMe()

    expect(auth.token).toBeNull()
  })

  it('fetchMe is a no-op when no token is set', async () => {
    const auth = useAuthStore()
    await auth.fetchMe()
    expect(mockFetch).not.toHaveBeenCalled()
  })
})
