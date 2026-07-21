/**
 * Garde d'authentification globale, exécutée AVANT le rendu de la page (serveur ET client).
 * Le token étant stocké en cookie, il est disponible côté SSR : le HTML d'une page protégée
 * n'est jamais envoyé à un visiteur non authentifié.
 */
export default defineNuxtRouteMiddleware((to) => {
  const authStore = useAuthStore()
  const isAuthRoute = to.path.startsWith('/auth')

  if (!authStore.isAuthenticated && !isAuthRoute) {
    return navigateTo('/auth/login')
  }

  if (authStore.isAuthenticated && to.path === '/auth/login') {
    return navigateTo('/')
  }
})
