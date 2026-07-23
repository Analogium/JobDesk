<template>
  <LegalPage title="Mentions légales" :updated="updated">
    <h2>Éditeur du site</h2>
    <p>
      <!-- ⚠️ À COMPLÉTER : la loi impose le nom, le statut et une adresse postale. -->
      {{ EDITEUR }}<br>
      {{ STATUT }}<br>
      {{ ADRESSE }}<br>
      Contact : <a :href="`mailto:${CONTACT}`">{{ CONTACT }}</a>
    </p>

    <h2>Directeur de la publication</h2>
    <p>{{ EDITEUR }}</p>

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

// ⚠️ À COMPLÉTER avant mise en ligne publique — champs obligatoires (art. 6 LCEN).
const EDITEUR = 'Théo Lambert'
const STATUT = 'Particulier — projet personnel non commercial'
const ADRESSE = 'À compléter'
const CONTACT = 'contact@theolambert.dev'
const updated = '22 juillet 2026'

useHead({ title: 'Mentions légales — JobDesk' })
</script>
