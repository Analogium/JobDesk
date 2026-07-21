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

  return { token, user, isAuthenticated, setToken, logout, fetchMe }
}, {
  persist: false,
})
