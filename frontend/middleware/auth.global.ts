/**
 * Garde d'authentification globale, exécutée AVANT le rendu de la page (serveur ET client).
 * Le token étant stocké en cookie, il est disponible côté SSR : le HTML d'une page protégée
 * n'est jamais envoyé à un visiteur non authentifié.
 */
export default defineNuxtRouteMiddleware((to) => {
  const authStore = useAuthStore()
  const isAuthRoute = to.path.startsWith('/auth')
  // Les pages légales doivent rester lisibles sans compte : on doit pouvoir consulter
  // la politique de confidentialité avant de s'inscrire.
  const isPublicRoute = isAuthRoute || to.path.startsWith('/legal')

  if (!authStore.isAuthenticated && !isPublicRoute) {
    return navigateTo('/auth/login')
  }

  // `/auth/callback` est exclu : il doit pouvoir poser le token puis rediriger lui-même.
  if (authStore.isAuthenticated && ['/auth/login', '/auth/register'].includes(to.path)) {
    return navigateTo('/')
  }
})
