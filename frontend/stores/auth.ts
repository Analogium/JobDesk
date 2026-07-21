import { defineStore } from 'pinia'
import type { User } from '~/types'

export const useAuthStore = defineStore('auth', () => {
  // Cookie plutôt que localStorage : il voyage avec la requête HTTP, donc le token
  // est disponible côté serveur (SSR) et le middleware global peut rediriger AVANT
  // le rendu de la page.
  const token = useCookie<string | null>('jobdesk_token', {
    default: () => null,
    maxAge: 60 * 60 * 24 * 30,
    sameSite: 'lax',
    // En prod (HTTPS derrière Traefik) le cookie ne doit jamais transiter en clair.
    // Désactivé en dev, servi en HTTP sur localhost.
    secure: !import.meta.dev,
  })
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  function setToken(t: string) {
    token.value = t
  }

  function logout() {
    token.value = null
    user.value = null
  }

  /**
   * Inscription et connexion par mot de passe. On n'utilise pas `useApi` ici : son
   * intercepteur 401 déclenche un logout + redirection, alors qu'un 401 signifie
   * simplement « identifiants incorrects » et doit remonter au formulaire.
   */
  async function authRequest(path: string, body: Record<string, string>) {
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

    token.value = data.token
    user.value = data.user
  }

  function login(email: string, password: string) {
    return authRequest('/auth/login', { email, password })
  }

  function register(email: string, name: string, password: string) {
    return authRequest('/auth/register', { email, name, password })
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

  return { token, user, isAuthenticated, setToken, logout, login, register, fetchMe }
}, {
  persist: false,
})
