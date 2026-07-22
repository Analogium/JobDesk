import type { Application } from '~/types'

/**
 * Normalise pour une comparaison tolérante : minuscules et sans accents, afin qu'une
 * recherche « societe » retrouve « Société ». NFD sépare la lettre de son accent, la
 * plage ̀-ͯ retire les diacritiques.
 */
export function normalizeText(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
}

/**
 * Une candidature correspond si chacun des mots de la requête apparaît dans l'un de ses
 * champs texte. Découper en mots (et non en sous-chaîne brute) permet à « acme dev » de
 * retrouver une candidature « Développeur » chez « Acme » quel que soit l'ordre saisi.
 */
export function matchesQuery(app: Application, query: string): boolean {
  const terms = normalizeText(query).split(/\s+/).filter(Boolean)
  if (!terms.length) return true

  const haystack = normalizeText(
    [app.companyName, app.jobTitle, app.location, app.notes]
      .filter(Boolean)
      .join(' '),
  )

  return terms.every(term => haystack.includes(term))
}
