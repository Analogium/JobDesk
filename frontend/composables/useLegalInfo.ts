/**
 * Identité de l'éditeur affichée dans les pages légales.
 *
 * Les valeurs viennent de l'environnement (`NUXT_PUBLIC_LEGAL_*`) : ce sont des données
 * personnelles réelles (nom, adresse postale), qui n'ont pas à être figées dans un dépôt
 * public. Une valeur absente est rendue visible plutôt que silencieusement vide — une
 * mention légale incomplète doit se voir.
 */
export function useLegalInfo() {
  const { legal } = useRuntimeConfig().public

  const missing = 'À compléter'
  const orMissing = (value: string) => (value?.trim() ? value : missing)

  return {
    editor: orMissing(legal.editor),
    status: orMissing(legal.status),
    address: orMissing(legal.address),
    contact: orMissing(legal.contact),
    /** Vrai tant qu'au moins une mention obligatoire manque. */
    incomplete: [legal.editor, legal.status, legal.address, legal.contact]
      .some(v => !v?.trim()),
  }
}
