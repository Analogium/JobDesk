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
| Auth | Google OAuth 2.0 · JWT (HS256, jjwt) |
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

Pipeline GitHub Actions (lint + tests + build Docker) sur chaque push et PR. Les jobs backend sont en cours d'alignement sur le build Maven du nouveau backend Java.

---

## Roadmap

Voir [ROADMAP.md](ROADMAP.md).
