/**
 * Helpers du lien de partage en lecture seule.
 *
 * Isolés ici (fonctions pures) pour être testables sans monter de composant Nuxt.
 */

/** URL complète à transmettre au tiers, construite à partir de l'origine courante. */
export function shareUrl(origin: string, token: string): string {
  return `${origin.replace(/\/$/, '')}/shared/${token}`
}

/**
 * Vrai si le lien a une échéance déjà dépassée. Un lien sans échéance (`null`)
 * n'expire jamais.
 */
export function isShareExpired(expiresAt: string | null, now: Date = new Date()): boolean {
  if (!expiresAt) return false
  return new Date(expiresAt).getTime() <= now.getTime()
}
