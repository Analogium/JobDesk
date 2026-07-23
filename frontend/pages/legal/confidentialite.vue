<template>
  <LegalPage title="Politique de confidentialité" :updated="updated">
    <p>
      JobDesk permet de suivre ses candidatures à un emploi. Cette page décrit quelles
      données sont traitées, pourquoi, combien de temps, et comment exercer vos droits.
    </p>

    <h2>Responsable du traitement</h2>
    <p>
      {{ legal.editor }} — contact :
      <a :href="`mailto:${legal.contact}`">{{ legal.contact }}</a>
      (voir les <NuxtLink to="/legal/mentions">mentions légales</NuxtLink>).
    </p>

    <h2>Données traitées</h2>
    <table>
      <thead>
        <tr><th>Données</th><th>Origine</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>Adresse email, nom, photo de profil</td>
          <td>Vous, à l'inscription — ou votre compte Google si vous vous connectez avec</td>
        </tr>
        <tr>
          <td>Mot de passe</td>
          <td>Vous. Jamais stocké en clair : seule une empreinte bcrypt est conservée</td>
        </tr>
        <tr>
          <td>
            Vos candidatures : entreprise, intitulé du poste, lieu, type de contrat,
            rémunération, description, notes, statut et historique
          </td>
          <td>Vous, ou l'import depuis une offre en ligne</td>
        </tr>
        <tr>
          <td>Contacts associés à une candidature (nom, email, rôle)</td>
          <td>Vous</td>
        </tr>
        <tr>
          <td>
            Jetons d'accès Google/Gmail, si vous activez le suivi automatique.
            Ils sont chiffrés (AES-256-GCM) en base
          </td>
          <td>Google, avec votre autorisation explicite</td>
        </tr>
        <tr>
          <td>
            Journaux de scan : date, nombre de messages analysés et de correspondances.
            Le contenu des messages n'est jamais conservé
          </td>
          <td>Généré par l'application</td>
        </tr>
      </tbody>
    </table>

    <h2>Finalités et bases légales</h2>
    <ul>
      <li>
        <strong>Fournir le service</strong> (compte, suivi des candidatures) — exécution du
        contrat qui nous lie lorsque vous utilisez JobDesk.
      </li>
      <li>
        <strong>Suivi automatique par Gmail</strong> — votre <em>consentement</em>, donné en
        connectant Gmail. Vous pouvez le retirer à tout moment depuis les Paramètres, ce qui
        supprime les jetons d'accès correspondants.
      </li>
      <li>
        <strong>Sécurité du compte</strong> (limitation des tentatives de connexion,
        détection de réutilisation d'un jeton de session) — intérêt légitime à protéger les
        comptes.
      </li>
    </ul>

    <h2>Le suivi automatique par Gmail</h2>
    <p>
      Si vous l'activez, JobDesk lit vos messages afin d'y repérer les réponses à vos
      candidatures (accusé de réception, refus, invitation à un entretien, offre) et de
      mettre à jour leur statut. Cette lecture est automatisée : <strong>aucune personne
      n'accède à vos messages</strong>, et leur contenu n'est ni copié ni conservé — seuls
      le statut détecté et un compteur de messages analysés sont enregistrés.
    </p>
    <p>
      Vous pouvez désactiver ce suivi à tout moment depuis les Paramètres, ou révoquer
      l'accès directement dans
      <a href="https://myaccount.google.com/permissions" target="_blank" rel="noopener">
        les autorisations de votre compte Google</a>.
    </p>

    <h2>Durées de conservation</h2>
    <ul>
      <li>
        <strong>Compte et candidatures</strong> : conservés tant que votre compte existe.
        Leur suppression est immédiate et définitive dès que vous supprimez votre compte.
      </li>
      <li><strong>Sessions</strong> : 30 jours, puis purgées automatiquement.</li>
      <li><strong>Liens de réinitialisation de mot de passe</strong> : 1 heure, usage unique.</li>
      <li><strong>Journaux de scan Gmail</strong> : 1 an.</li>
    </ul>

    <h2>Destinataires</h2>
    <p>
      Vos données ne sont ni vendues, ni cédées, ni utilisées à des fins publicitaires.
      Elles sont hébergées en Europe et ne sont accessibles qu'aux prestataires strictement
      nécessaires au fonctionnement du service :
    </p>
    <ul>
      <li><strong>Hostinger</strong> (Lituanie) — hébergement du serveur et de la base de données.</li>
      <li><strong>Cloudflare</strong> — routage du trafic et protection du domaine.</li>
      <li><strong>Brevo</strong> (France) — envoi des emails de réinitialisation de mot de passe.</li>
      <li>
        <strong>Google</strong> — uniquement si vous utilisez la connexion Google ou le suivi
        Gmail, dans le cadre des accès que vous avez autorisés.
      </li>
    </ul>

    <h2>Sécurité</h2>
    <ul>
      <li>Toutes les communications sont chiffrées (HTTPS).</li>
      <li>Les mots de passe sont hachés avec bcrypt, jamais réversibles.</li>
      <li>Les jetons Gmail sont chiffrés en base (AES-256-GCM).</li>
      <li>
        Les jetons de session et de réinitialisation ne sont stockés que sous forme
        d'empreinte : une copie de la base ne permettrait pas de reprendre une session.
      </li>
      <li>Les tentatives de connexion et les demandes de réinitialisation sont plafonnées.</li>
    </ul>

    <h2>Cookies</h2>
    <p>
      JobDesk ne dépose que deux cookies, strictement nécessaires à votre connexion
      (<code>jobdesk_token</code> et <code>jobdesk_refresh</code>). Ils servent uniquement à
      vous maintenir authentifié·e et expirent au bout de 30 jours.
      <strong>Aucun cookie publicitaire, aucune mesure d'audience, aucun traceur tiers</strong>
      — c'est pourquoi aucune bannière de consentement ne vous est demandée.
    </p>

    <h2>Vos droits</h2>
    <p>
      Vous disposez d'un droit d'accès, de rectification, d'effacement, de portabilité, de
      limitation et d'opposition. Deux d'entre eux s'exercent directement dans l'application,
      sans avoir à nous écrire :
    </p>
    <ul>
      <li>
        <strong>Accès et portabilité</strong> — Paramètres → « Télécharger mes données »
        exporte l'intégralité de votre compte dans un fichier JSON réutilisable.
      </li>
      <li>
        <strong>Effacement</strong> — Paramètres → « Supprimer mon compte » efface
        immédiatement et définitivement votre compte et toutes vos candidatures.
      </li>
      <li>
        <strong>Rectification</strong> — vos candidatures sont modifiables à tout moment.
      </li>
    </ul>
    <p>
      Pour toute autre demande :
      <a :href="`mailto:${legal.contact}`">{{ legal.contact }}</a>. Vous avez
      également le droit d'introduire une réclamation auprès de la
      <a href="https://www.cnil.fr" target="_blank" rel="noopener">CNIL</a>.
    </p>

    <h2>Modifications</h2>
    <p>
      Cette politique peut évoluer avec le service. La date de dernière mise à jour figure
      en tête de page.
    </p>
  </LegalPage>
</template>

<script setup lang="ts">
definePageMeta({ layout: false })

// Renseignées par les variables NUXT_PUBLIC_LEGAL_* (cf. nuxt.config.ts).
const legal = useLegalInfo()
const updated = '23 juillet 2026'

useHead({ title: 'Politique de confidentialité — JobDesk' })
</script>
