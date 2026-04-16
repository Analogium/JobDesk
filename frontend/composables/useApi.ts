export function useApi() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  async function apiFetch<T>(
    path: string,
    options: RequestInit = {},
  ): Promise<T> {
    const url = `${config.public.apiUrl}${path}`
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'application/ld+json',
      ...(options.headers as Record<string, string> ?? {}),
    }

    if (authStore.token) {
      headers['Authorization'] = `Bearer ${authStore.token}`
    }

    const res = await fetch(url, { ...options, headers })

    if (res.status === 401) {
      authStore.logout()
      await navigateTo('/auth/login')
      throw new Error('Unauthorized')
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body['hydra:description'] ?? body.message ?? `HTTP ${res.status}`)
    }

    if (res.status === 204) return undefined as T
    return res.json()
  }

  return { apiFetch }
}
