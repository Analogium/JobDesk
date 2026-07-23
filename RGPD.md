# Conformité RGPD — JobDesk

Document interne : registre des traitements (art. 30) et état de la conformité.
La version destinée aux utilisateurs est la page `/legal/confidentialite`.

## Registre des traitements

### 1. Gestion des comptes utilisateurs

| | |
|---|---|
| **Finalité** | Créer et authentifier un compte, permettre le suivi de candidatures |
| **Base légale** | Exécution du contrat (art. 6.1.b) |
| **Personnes concernées** | Utilisateurs de JobDesk |
| **Données** | Email, nom, photo de profil, empreinte du mot de passe (bcrypt) |
| **Source** | L'utilisateur, ou Google (connexion OAuth) |
| **Conservation** | Durée de vie du compte ; effacement immédiat à sa suppression |
| **Destinataires** | Hostinger (hébergement), Brevo (emails de réinitialisation) |

### 2. Suivi des candidatures

| | |
|---|---|
| **Finalité** | Enregistrer et suivre l'avancement des candidatures |
| **Base légale** | Exécution du contrat (art. 6.1.b) |
| **Données** | Entreprise, poste, lieu, contrat, rémunération, description, notes, statut et historique ; contacts associés (nom, email, rôle) |
| **Source** | L'utilisateur, ou import depuis une offre en ligne (scraping) |
| **Conservation** | Durée de vie du compte |
| **Point d'attention** | Les contacts sont des **tiers** (recruteurs). Données limitées au strict nécessaire, saisies par l'utilisateur, jamais utilisées pour les démarcher |

### 3. Suivi automatique par Gmail

| | |
|---|---|
| **Finalité** | Détecter dans les emails les réponses aux candidatures et mettre à jour leur statut |
| **Base légale** | **Consentement** (art. 6.1.a), donné en connectant Gmail, retirable à tout moment |
| **Données** | Jetons OAuth Google (chiffrés AES-256-GCM) ; journaux de scan (date, nombre de messages analysés, correspondances) |
| **Traitement** | Lecture automatisée. **Le contenu des messages n'est ni copié ni conservé** ; aucune intervention humaine |
| **Conservation** | Jetons : jusqu'au retrait du consentement. Journaux : 1 an |
| **Destinataire** | Google (API Gmail) |

### 4. Sécurité des comptes

| | |
|---|---|
| **Finalité** | Prévenir le bourrinage de mots de passe, l'abus d'envoi d'emails et le vol de session |
| **Base légale** | Intérêt légitime (art. 6.1.f) |
| **Données** | Adresse IP et email (compteurs en mémoire), empreintes des jetons de session |
| **Conservation** | Compteurs : 5 à 15 min. Jetons de session : 30 j |

## Sous-traitants

| Sous-traitant | Rôle | Localisation |
|---|---|---|
| Hostinger | Hébergement serveur + base de données | UE (Lituanie / Chypre) |
| Cloudflare | DNS, routage HTTPS | UE (edge) |
| Brevo | Envoi des emails transactionnels | France |
| Google | OAuth + API Gmail (si activé par l'utilisateur) | Hors UE — encadré par les clauses contractuelles types et le Data Privacy Framework |

## Droits des personnes

| Droit | Mise en œuvre |
|---|---|
| Accès (art. 15) | `GET /api/me/export` — Paramètres → « Télécharger mes données » |
| Portabilité (art. 20) | Même export, format JSON structuré |
| Effacement (art. 17) | `DELETE /api/me` — Paramètres → « Supprimer mon compte ». Immédiat, sans conservation différée |
| Rectification (art. 16) | Édition des candidatures dans l'application |
| Retrait du consentement (art. 7.3) | Paramètres → « Déconnecter » Gmail (supprime les jetons) |
| Information (art. 13) | Page `/legal/confidentialite`, liée depuis l'inscription et la connexion |

## Mesures techniques (art. 32)

- HTTPS partout (Traefik + Let's Encrypt).
- Mots de passe hachés en bcrypt.
- Jetons Gmail chiffrés en base (AES-256-GCM), clé dérivée d'`APP_SECRET`.
- Jetons de session et de réinitialisation stockés **hachés** (SHA-256) : une copie de la
  base ne permet pas de reprendre une session.
- Rotation des refresh tokens avec détection de rejeu (révocation globale du compte).
- Limitation de débit sur la connexion et la réinitialisation de mot de passe.
- Cloisonnement par utilisateur sur toutes les lectures/écritures de candidatures.
- Sauvegardes quotidiennes de la base sur le VPS.

## Limitation de conservation (art. 5.1.e)

`DataRetentionScheduler` purge quotidiennement :

- les refresh tokens expirés, et les révoqués de plus de 30 jours ;
- les liens de réinitialisation consommés ou expirés ;
- les journaux de scan Gmail de plus d'un an (`app.retention.mail-scan-days`).

Il ne supprime **jamais** de compte ni de candidature : l'effacement reste une décision de
l'utilisateur.

## Cookies

Deux cookies strictement nécessaires (`jobdesk_token`, `jobdesk_refresh`), aucun traceur ni
mesure d'audience. Aucune bannière de consentement n'est donc requise (art. 82 loi
Informatique et Libertés / directive ePrivacy).

## Reste à faire

- [ ] Compléter l'identité de l'éditeur et l'adresse de contact dans
      `frontend/pages/legal/mentions.vue` et `confidentialite.vue` (marqueurs « À COMPLÉTER »).
- [ ] Créer l'adresse de contact annoncée dans les pages légales et vérifier qu'elle est relevée.
- [ ] En cas d'ouverture à d'autres utilisateurs : prévoir une procédure de notification de
      violation de données sous 72 h (art. 33).
