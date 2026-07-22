import { defineStore } from 'pinia'
import type { User } from '~/types'

export const useAuthStore = defineStore('auth', () => {
  // Cookie plutôt que localStorage : il voyage avec la requête HTTP, donc le token
  // est disponible côté serveur (SSR) et le middleware global peut rediriger AVANT
  // le rendu de la page.
  const cookieOptions = {
    maxAge: 60 * 60 * 24 * 30,
    sameSite: 'lax' as const,
    // En prod (HTTPS derrière Traefik) le cookie ne doit jamais transiter en clair.
    // Désactivé en dev, servi en HTTP sur localhost.
    secure: !import.meta.dev,
  }

  // Cookie plutôt que localStorage : il voyage avec la requête HTTP, donc le token
  // est disponible côté serveur (SSR) et le middleware global peut rediriger AVANT
  // le rendu de la page.
  const token = useCookie<string | null>('jobdesk_token', { default: () => null, ...cookieOptions })
  // Access token court : quand il expire, ce refresh token permet d'en obtenir un
  // nouveau sans reconnexion (voir `refresh` + l'intercepteur 401 de useApi).
  const refreshToken = useCookie<string | null>('jobdesk_refresh', { default: () => null, ...cookieOptions })
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  function setToken(t: string) {
    token.value = t
  }

  function setTokens(access: string, refresh: string) {
    token.value = access
    refreshToken.value = refresh
  }

  function logout() {
    // Révoque le refresh token côté serveur pour que la session ne reprenne pas.
    if (refreshToken.value) {
      const config = useRuntimeConfig()
      fetch(`${config.public.apiUrl}/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: refreshToken.value }),
      }).catch(() => {}) // best-effort : on efface la session locale quoi qu'il arrive
    }
    token.value = null
    refreshToken.value = null
    user.value = null
  }

  /**
   * Échange le refresh token contre un nouvel access token (rotation). Renvoie true si la
   * session a pu être prolongée, false sinon (refresh absent, expiré ou révoqué).
   */
  async function refresh(): Promise<boolean> {
    if (!refreshToken.value) return false
    const config = useRuntimeConfig()
    try {
      const res = await fetch(`${config.public.apiUrl}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ refreshToken: refreshToken.value }),
      })
      if (!res.ok) return false
      const data = await res.json()
      token.value = data.token
      refreshToken.value = data.refreshToken
      if (data.user) user.value = data.user
      return true
    } catch {
      return false
    }
  }

  /**
   * Inscription et connexion par mot de passe. On n'utilise pas `useApi` ici : son
   * intercepteur 401 déclenche un logout + redirection, alors qu'un 401 signifie
   * simplement « identifiants incorrects » et doit remonter au formulaire.
   */
  async function postJson(path: string, body: Record<string, string>) {
    const config = useRuntimeConfig()
    const res = await fetch(`${config.public.apiUrl}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(body),
    })

    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      throw new Error(data.message ?? 'Une erreur est survenue, réessayez.')
    }
    return data
  }

  /** Ouvre la session à partir d'une réponse `{ token, refreshToken, user }`. */
  async function authRequest(path: string, body: Record<string, string>) {
    const data = await postJson(path, body)
    setTokens(data.token, data.refreshToken)
    user.value = data.user
  }

  function login(email: string, password: string) {
    return authRequest('/auth/login', { email, password })
  }

  function register(email: string, name: string, password: string) {
    return authRequest('/auth/register', { email, name, password })
  }

  /**
   * Réussit toujours, même si l'adresse est inconnue : le backend répond 204 dans
   * les deux cas pour ne pas révéler quelles adresses ont un compte.
   */
  function forgotPassword(email: string) {
    return postJson('/auth/password/forgot', { email })
  }

  function resetPassword(resetToken: string, password: string) {
    return postJson('/auth/password/reset', { token: resetToken, password })
  }

  async function fetchMe() {
    if (!token.value) return
    const config = useRuntimeConfig()
    try {
      const res = await fetch(`${config.public.apiUrl}/api/me`, {
        headers: { Authorization: `Bearer ${token.value}` },
      })
      if (res.ok) {
        user.value = await res.json()
      } else {
        logout()
      }
    } catch {
      logout()
    }
  }

  return {
    token,
    refreshToken,
    user,
    isAuthenticated,
    setToken,
    setTokens,
    refresh,
    logout,
    login,
    register,
    forgotPassword,
    resetPassword,
    fetchMe,
  }
}, {
  persist: false,
})
