/**
 * Identité de l'éditeur affichée dans les pages légales.
 *
 * Les valeurs viennent de l'environnement (`NUXT_PUBLIC_LEGAL_*`) : ce sont des données
 * personnelles réelles (nom, statut), qui n'ont pas à être figées dans un dépôt public.
 * L'adresse postale n'est volontairement pas publiée : particulier non-professionnel,
 * l'éditeur la communique à l'hébergeur (art. 6-III-2 LCEN) sans l'exposer au public.
 * Une valeur absente est rendue visible plutôt que silencieusement vide — une mention
 * légale incomplète doit se voir.
 */
export function useLegalInfo() {
  const { legal } = useRuntimeConfig().public

  const missing = 'À compléter'
  const orMissing = (value: string) => (value?.trim() ? value : missing)

  return {
    editor: orMissing(legal.editor),
    status: orMissing(legal.status),
    contact: orMissing(legal.contact),
    /** Vrai tant qu'au moins une mention obligatoire manque. */
    incomplete: [legal.editor, legal.status, legal.contact]
      .some(v => !v?.trim()),
  }
}
