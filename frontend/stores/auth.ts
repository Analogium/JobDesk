import { defineStore } from 'pinia'
import type { User } from '~/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  function setToken(t: string) {
    token.value = t
    if (import.meta.client) {
      localStorage.setItem('jobdesk_token', t)
    }
  }

  function logout() {
    token.value = null
    user.value = null
    if (import.meta.client) {
      localStorage.removeItem('jobdesk_token')
    }
  }

  async function fetchMe() {
    if (!token.value) return
    const config = useRuntimeConfig()
    try {
      const res = await fetch(`${config.public.apiUrl}/api/users/me`, {
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

  function init() {
    if (import.meta.client) {
      const stored = localStorage.getItem('jobdesk_token')
      if (stored) {
        token.value = stored
        fetchMe()
      }
    }
  }

  return { token, user, isAuthenticated, setToken, logout, fetchMe, init }
}, {
  persist: false,
})
