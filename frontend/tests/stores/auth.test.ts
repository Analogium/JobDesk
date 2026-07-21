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

  describe('email + password', () => {
    const fakeUser = { id: '1', email: 'alice@example.com', name: 'Alice' }

    it('login stores the token and the user returned by the API', async () => {
      const auth = useAuthStore()
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: 'jwt-from-login', user: fakeUser }),
      })

      await auth.login('alice@example.com', 'correct-horse')

      expect(auth.token).toBe('jwt-from-login')
      expect(auth.user).toEqual(fakeUser)
      expect(auth.isAuthenticated).toBe(true)
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8000/auth/login',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ email: 'alice@example.com', password: 'correct-horse' }),
        }),
      )
    })

    it('register posts the name too and stores the token', async () => {
      const auth = useAuthStore()
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: 'jwt-from-register', user: fakeUser }),
      })

      await auth.register('alice@example.com', 'Alice', 'correct-horse')

      expect(auth.token).toBe('jwt-from-register')
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8000/auth/register',
        expect.objectContaining({
          body: JSON.stringify({
            email: 'alice@example.com',
            name: 'Alice',
            password: 'correct-horse',
          }),
        }),
      )
    })

    // Un 401 ici veut dire « identifiants incorrects » : il doit remonter au
    // formulaire, surtout pas déconnecter ni rediriger.
    it('login surfaces the API message and leaves the session untouched', async () => {
      const auth = useAuthStore()
      mockFetch.mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: 'Email ou mot de passe incorrect' }),
      })

      await expect(auth.login('alice@example.com', 'wrong')).rejects.toThrow(
        'Email ou mot de passe incorrect',
      )
      expect(auth.token).toBeNull()
      expect(auth.isAuthenticated).toBe(false)
    })

    it('falls back to a generic message when the API sends no body', async () => {
      const auth = useAuthStore()
      mockFetch.mockResolvedValueOnce({
        ok: false,
        json: async () => {
          throw new Error('not json')
        },
      })

      await expect(auth.register('a@b.com', 'A', 'password123')).rejects.toThrow(
        'Une erreur est survenue, réessayez.',
      )
    })
  })
})
