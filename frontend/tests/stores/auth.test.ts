import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '~/stores/auth'

// Mock fetch globally
const mockFetch = vi.fn()
vi.stubGlobal('fetch', mockFetch)

// Les tokens sont persistés via useCookie (et non localStorage) :
// on repart de cookies vides avant chaque test.
function clearAuthCookie() {
  document.cookie = 'jobdesk_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  document.cookie = 'jobdesk_refresh=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
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

  it('logout clears both tokens and the user', () => {
    const auth = useAuthStore()
    auth.setTokens('my-jwt', 'my-refresh')
    // logout notifie le serveur pour révoquer le refresh token.
    mockFetch.mockResolvedValueOnce({ ok: true })
    auth.logout()
    expect(auth.token).toBeNull()
    expect(auth.refreshToken).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8000/auth/logout',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ refreshToken: 'my-refresh' }),
      }),
    )
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
        json: async () => ({ token: 'jwt-from-login', refreshToken: 'refresh-from-login', user: fakeUser }),
      })

      await auth.login('alice@example.com', 'correct-horse')

      expect(auth.token).toBe('jwt-from-login')
      expect(auth.refreshToken).toBe('refresh-from-login')
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
        json: async () => ({ token: 'jwt-from-register', refreshToken: 'refresh-from-register', user: fakeUser }),
      })

      await auth.register('alice@example.com', 'Alice', 'correct-horse')

      expect(auth.token).toBe('jwt-from-register')
      expect(auth.refreshToken).toBe('refresh-from-register')
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

    it('forgotPassword posts the email and never opens a session', async () => {
      const auth = useAuthStore()
      // Le backend répond 204 sans corps : res.json() échoue, ce qui doit rester sans effet.
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => {
          throw new Error('no content')
        },
      })

      await auth.forgotPassword('alice@example.com')

      expect(auth.token).toBeNull()
      expect(auth.isAuthenticated).toBe(false)
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8000/auth/password/forgot',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ email: 'alice@example.com' }),
        }),
      )
    })

    it('resetPassword posts the token and the new password', async () => {
      const auth = useAuthStore()
      mockFetch.mockResolvedValueOnce({ ok: true, json: async () => ({}) })

      await auth.resetPassword('a-token', 'nouveau-mot-de-passe')

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8000/auth/password/reset',
        expect.objectContaining({
          body: JSON.stringify({ token: 'a-token', password: 'nouveau-mot-de-passe' }),
        }),
      )
    })

    it('resetPassword surfaces an expired link message', async () => {
      const auth = useAuthStore()
      mockFetch.mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: 'Ce lien de réinitialisation est invalide ou expiré.' }),
      })

      await expect(auth.resetPassword('vieux-token', 'nouveau-mot-de-passe')).rejects.toThrow(
        'Ce lien de réinitialisation est invalide ou expiré.',
      )
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

  describe('refresh', () => {
    it('is a no-op returning false when there is no refresh token', async () => {
      const auth = useAuthStore()
      const ok = await auth.refresh()
      expect(ok).toBe(false)
      expect(mockFetch).not.toHaveBeenCalled()
    })

    it('rotates both tokens on success', async () => {
      const auth = useAuthStore()
      auth.setTokens('old-jwt', 'old-refresh')
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: 'new-jwt', refreshToken: 'new-refresh' }),
      })

      const ok = await auth.refresh()

      expect(ok).toBe(true)
      expect(auth.token).toBe('new-jwt')
      expect(auth.refreshToken).toBe('new-refresh')
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8000/auth/refresh',
        expect.objectContaining({ body: JSON.stringify({ refreshToken: 'old-refresh' }) }),
      )
    })

    it('returns false and keeps state when the refresh token is rejected', async () => {
      const auth = useAuthStore()
      auth.setTokens('old-jwt', 'expired-refresh')
      mockFetch.mockResolvedValueOnce({ ok: false, json: async () => ({}) })

      const ok = await auth.refresh()

      expect(ok).toBe(false)
      // On ne déconnecte pas ici : c'est l'appelant (useApi) qui décide.
      expect(auth.token).toBe('old-jwt')
    })
  })
})
