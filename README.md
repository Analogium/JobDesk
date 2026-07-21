# JobDesk

Application web fullstack de suivi de candidatures. Gestion du cycle de vie (statuts, historique, contacts), import d'offres depuis une URL, et analyse automatique des réponses dans Gmail.

> **Migration backend en cours** : le backend est passé de Symfony 7.4 à **Java Spring Boot** (`backend-java/`). C'est désormais le backend **par défaut**. L'ancien backend Symfony (`backend/`) est conservé comme fallback, non démarré par défaut (voir [Backend legacy](#backend-legacy-symfony)).

---

## Stack

| Couche | Technologie |
|---|---|
| Frontend | Nuxt 3 · TypeScript · Tailwind CSS |
| Backend | **Spring Boot 3.4 · Java 21** (Tomcat embarqué) |
| Base de données | PostgreSQL 15 |
| Auth | Email + mot de passe (BCrypt) · Google OAuth 2.0 · JWT (HS256, jjwt) |
| Rendu headless | Microservice Playwright (repli du scraper) |
| Infrastructure | Docker · Docker Compose |
| CI/CD | GitHub Actions |
| Hébergement | VPS Ubuntu 22.04 (Traefik pour le TLS/routage) |

> Le backend Java embarque son propre serveur HTTP (Tomcat) : **pas de Nginx** nécessaire. En prod, Traefik assure la terminaison TLS devant le JAR.

---

## Prérequis

- Docker & Docker Compose
- Un projet Google Cloud avec OAuth 2.0 configuré ([console.cloud.google.com](https://console.cloud.google.com))
  - URIs de redirection autorisées : `http://localhost:8000/auth/google/check` (login) et `http://localhost:8000/api/gmail/callback` (connexion Gmail)

---

## Installation

### 1. Variables d'environnement

Renseigner à la racine dans `.env` :

```env
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
# Clé de chiffrement des tokens Gmail + signature JWT.
# ⚠️ À définir UNE fois avec une valeur forte, puis NE JAMAIS la changer
# (sinon les tokens Gmail déjà stockés deviennent illisibles).
APP_SECRET=une_valeur_forte_et_stable

# Envoi des mails de réinitialisation (relais SMTP, Brevo en prod).
# Facultatif en local : sans SMTP_HOST, le lien est écrit dans les logs du backend
# au lieu d'être envoyé, ce qui suffit pour développer.
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USER=
SMTP_PASSWORD=
EMAIL_FROM=expediteur_valide_chez_brevo@example.com
```

### 2. Démarrage

```bash
docker compose up -d
```

Le schéma PostgreSQL existant est **validé** au démarrage (`ddl-auto: validate`) et adopté comme baseline Flyway — aucune table n'est recréée, les données sont conservées.

### 3. Accès

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8000/api |

---

## Développement

### Tests

```bash
./test.sh
```

Lance les tests **JUnit** du backend (via une image Maven jetable, base **H2** en mémoire — aucune dépendance externe) et **Vitest** pour le frontend.

Directement en Maven (sans passer par le script) :

```bash
docker run --rm -v "$PWD/backend-java":/app -v jobdesk_m2:/root/.m2 \
  -w /app maven:3.9-eclipse-temurin-21 mvn -B test
```

### Lint

```bash
./lint.sh           # vérification
./lint.sh --fix     # correction automatique (ESLint)
```

| Outil | Périmètre |
|---|---|
| Compilation Maven | Garde-fou statique du backend Java |
| ESLint (`@nuxt/eslint`) | TypeScript + Vue 3 |

### Git hooks

Les hooks sont versionnés dans `.githooks/`. À activer une fois après le clone :

```bash
make setup      # ou : git config core.hooksPath .githooks
```

Le hook `pre-push` bloque le push si lint ou tests échouent.

### Commandes utiles

```bash
# Backend Java
docker compose logs -f backend-java
docker exec jobdesk_backend_java <cmd>

# Frontend
docker exec jobdesk_frontend npm run <script>
```

---

## Structure

```
JobDesk/
├── backend-java/          # Backend Spring Boot (Java 21) — ACTIF
│   ├── src/main/java/com/jobdesk/
│   │   ├── domain/        # Entités JPA + enums (+ converters : chiffrement, source)
│   │   ├── repository/    # Spring Data JPA (filtrage par user)
│   │   ├── service/       # Métier : candidatures, scan Gmail, scraper
│   │   ├── web/ + dto/    # Controllers REST + DTO
│   │   ├── security/      # Google OAuth, JWT, filtre d'auth, state HMAC
│   │   └── config/        # SecurityConfig/CORS, init clé de chiffrement
│   ├── src/test/java/     # JUnit (H2)
│   └── Dockerfile         # build multi-stage (Maven → JRE 21)
├── backend/               # Ancien backend Symfony 7.4 — LEGACY (fallback)
├── frontend/              # Nuxt 3 (pages, stores Pinia, composables, tests Vitest)
├── playwright-scraper/    # Microservice de rendu headless
├── .github/workflows/     # CI
├── .githooks/             # pre-push hook
├── lint.sh · test.sh · Makefile
└── docker-compose.yml
```

---

## API

API REST JSON. Toutes les routes `/api/**` nécessitent un JWT (`Authorization: Bearer <token>`).

| Endpoint | Méthode | Description |
|---|---|---|
| `/auth/register` | POST | Inscription email + mot de passe → `{ token, user }` |
| `/auth/login` | POST | Connexion email + mot de passe → `{ token, user }` |
| `/auth/password/forgot` | POST | Envoie un lien de réinitialisation (204 même si l'email est inconnu, 429 au-delà de 3 demandes / 15 min) |
| `/auth/password/reset` | POST | Définit un nouveau mot de passe à partir du token du lien |
| `/auth/google` | GET | Démarre le login Google (→ redirige vers `/auth/callback?token=`) |
| `/api/me` | GET | Profil de l'utilisateur connecté |
| `/api/applications` | GET | Liste paginée `{ member, totalItems }` (filtres `status`, `source`, `contractType`, tri `order[...]`) |
| `/api/applications` | POST | Créer une candidature |
| `/api/applications/{id}` | GET · PATCH · DELETE | Détail · modif (`merge-patch+json`, historique auto au changement de statut) · suppression |
| `/api/gmail/connect` · `/status` · `/scan` · `/disconnect` | — | Connexion & scan Gmail |
| `/api/gmail/callback` | GET | Callback OAuth Gmail (public) |
| `/api/scrape` | POST | Import d'une offre depuis une URL |

Un scan Gmail périodique (toutes les 2 h) tourne aussi en tâche de fond.

---

## Backend legacy (Symfony)

L'ancien backend est conservé derrière le profil Compose `legacy` et **ne démarre pas** par défaut. Pour le relancer en secours (il écoute aussi sur le port 8000) :

```bash
docker compose stop backend-java                     # libère le port 8000
docker compose --profile legacy up -d backend nginx  # relance Symfony + Nginx
```

---

## CI/CD

Pipeline GitHub Actions (`.github/workflows/ci.yml`), sur chaque push et PR :

```
backend-tests (Maven + JUnit) ─┐
frontend-lint  (ESLint)        ├─→ build-check (images Docker) ─→ deploy (VPS)
frontend-tests (Vitest)        ─┘                                  push master
```

**Déploiement continu** : un push sur `master` dont le CI est vert déclenche
automatiquement, sur le VPS, un `git pull` + `docker compose build && up -d` des services
`jobdesk-backend`, `jobdesk-frontend`, `jobdesk-playwright`, suivi d'un smoke test HTTP.
L'accès SSH est fourni par les secrets `VPS_HOST` / `VPS_PORT` / `VPS_USER` /
`VPS_SSH_KEY` / `VPS_KNOWN_HOSTS`.

---

## Roadmap

Voir [ROADMAP.md](ROADMAP.md).
