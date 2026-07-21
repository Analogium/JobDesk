import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '~/stores/auth'

// Mock fetch globally
const mockFetch = vi.fn()
vi.stubGlobal('fetch', mockFetch)

// Le token est désormais persisté via useCookie (et non localStorage) :
// on repart d'un cookie vide avant chaque test.
function clearAuthCookie() {
  document.cookie = 'jobdesk_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
}

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearAuthCookie()
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
  })

  it('logout clears token and user', () => {
    const auth = useAuthStore()
    auth.setToken('my-jwt')
    auth.logout()
    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
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
