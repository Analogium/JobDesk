export function useApi() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  function send(path: string, options: RequestInit): Promise<Response> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...(options.headers as Record<string, string> ?? {}),
    }
    if (authStore.token) {
      headers['Authorization'] = `Bearer ${authStore.token}`
    }
    return fetch(`${config.public.apiUrl}${path}`, { ...options, headers })
  }

  async function apiFetch<T>(
    path: string,
    options: RequestInit = {},
  ): Promise<T> {
    let res = await send(path, options)

    // Access token expiré : on tente un refresh transparent puis on rejoue une seule
    // fois. C'est ce qui évite de se reconnecter tant que le refresh token est valide.
    if (res.status === 401 && await authStore.refresh()) {
      res = await send(path, options)
    }

    if (res.status === 401) {
      // Refresh impossible (session réellement expirée/révoquée) : on déconnecte.
      authStore.logout()
      await navigateTo('/auth/login')
      throw new Error('Unauthorized')
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body.message ?? body.error ?? `HTTP ${res.status}`)
    }

    if (res.status === 204) return undefined as T
    return res.json()
  }

  return { apiFetch }
}
