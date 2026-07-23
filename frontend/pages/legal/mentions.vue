<template>
  <LegalPage title="Mentions légales" :updated="updated">
    <h2>Éditeur du site</h2>
    <p>
      {{ legal.editor }}<br>
      {{ legal.status }}<br>
      {{ legal.address }}<br>
      Contact : <a :href="`mailto:${legal.contact}`">{{ legal.contact }}</a>
    </p>

    <h2>Directeur de la publication</h2>
    <p>{{ legal.editor }}</p>

    <h2>Hébergement</h2>
    <p>
      Hostinger International Ltd.<br>
      61 Lordou Vironos Street, 6023 Larnaca, Chypre<br>
      <a href="https://www.hostinger.fr" target="_blank" rel="noopener">hostinger.fr</a>
    </p>

    <h2>Propriété intellectuelle</h2>
    <p>
      Le nom JobDesk, l'interface et le code de l'application sont la propriété de leur
      auteur. Les contenus que vous saisissez (candidatures, notes, contacts) restent les
      vôtres : vous pouvez les exporter ou les supprimer à tout moment depuis les Paramètres.
    </p>

    <h2>Données personnelles</h2>
    <p>
      Le traitement de vos données est détaillé dans la
      <NuxtLink to="/legal/confidentialite">politique de confidentialité</NuxtLink>.
    </p>

    <h2>Responsabilité</h2>
    <p>
      JobDesk est un outil de suivi personnel fourni en l'état. Le suivi automatique par
      Gmail repose sur une détection heuristique : un statut peut être mal interprété ou un
      message ignoré. Vérifiez les informations importantes à la source, dans vos échanges
      avec les recruteurs.
    </p>
  </LegalPage>
</template>

<script setup lang="ts">
definePageMeta({ layout: false })

// Renseignées par les variables NUXT_PUBLIC_LEGAL_* (cf. nuxt.config.ts).
const legal = useLegalInfo()
const updated = '23 juillet 2026'

useHead({ title: 'Mentions légales — JobDesk' })
</script>
