# JobDesk

Application web fullstack de suivi de candidatures. Gestion manuelle du cycle de vie (statuts, historique, contacts), avec des phases d'automatisation prévues : import d'offres depuis une URL et analyse des réponses dans Gmail.

---

## Stack

| Couche | Technologie |
|---|---|
| Frontend | Nuxt 3 · TypeScript · Tailwind CSS |
| Backend | Symfony 7.4 · API Platform 4 |
| Base de données | PostgreSQL 15 |
| Auth | Google OAuth 2.0 · JWT (LexikJWT) |
| Infrastructure | Docker · Docker Compose · Nginx |
| CI/CD | GitHub Actions |
| Hébergement | VPS Ubuntu 22.04 (Traefik) |

---

## Prérequis

- Docker & Docker Compose
- Un projet Google Cloud avec OAuth 2.0 configuré ([console.cloud.google.com](https://console.cloud.google.com))

---

## Installation

### 1. Variables d'environnement

```bash
cp backend/.env backend/.env.local
```

Renseigner dans `backend/.env.local` :

```env
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
JWT_PASSPHRASE=your_passphrase
```

### 2. Clés JWT

```bash
docker compose run --rm backend php bin/console lexik:jwt:generate-keypair
```

### 3. Démarrage

```bash
docker compose up -d
```

### 4. Base de données

```bash
docker exec jobdesk_backend php bin/console doctrine:migrations:migrate --no-interaction
```

### 5. Accès

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8000/api |
| Doc API | http://localhost:8000/api/docs |

---

## Développement

### Tests

```bash
./test.sh
```

Lance PHPUnit (SQLite in-process, pas de dépendance PostgreSQL) et Vitest dans les containers.

### Lint & analyse statique

```bash
./lint.sh           # vérification
./lint.sh --fix     # correction automatique
```

| Outil | Périmètre |
|---|---|
| PHP CS Fixer | Style PSR-12 + règles Symfony |
| PHPStan (niveau 5) | Analyse statique avec extensions Symfony & Doctrine |
| ESLint (`@nuxt/eslint`) | TypeScript + Vue 3 |

### Git hooks

Les hooks sont versionnés dans `.githooks/`. À activer une fois après le clone :

```bash
make setup
```

Le hook `pre-push` bloque le push si lint ou tests échouent.

### Commandes utiles

```bash
make setup    # installe les git hooks
make lint     # lint
make test     # tests
```

```bash
# Backend
docker exec jobdesk_backend php bin/console <commande>
docker exec jobdesk_backend composer <script>

# Frontend
docker exec jobdesk_frontend npm run <script>
```

---

## Structure

```
JobDesk/
├── backend/               # Symfony 7.4
│   ├── src/
│   │   ├── Entity/        # User, Application, StatusHistory, Contact, MailScan
│   │   ├── Enum/          # ApplicationStatus, ApplicationSource, ContractType
│   │   ├── Repository/
│   │   ├── State/         # API Platform processors & providers
│   │   ├── Doctrine/      # Extensions ORM (filtrage par utilisateur)
│   │   └── Security/      # GoogleAuthenticator
│   ├── tests/
│   │   ├── Unit/          # PHPUnit sans DB (mocks)
│   │   └── Integration/   # WebTestCase + SQLite
│   └── phpstan.dist.neon
├── frontend/              # Nuxt 3
│   ├── pages/
│   ├── components/
│   ├── stores/            # Pinia (auth, applications)
│   ├── composables/
│   └── tests/stores/      # Vitest
├── .github/workflows/     # CI : lint + tests + build Docker
├── .githooks/             # pre-push hook
├── lint.sh
├── test.sh
└── Makefile
```

---

## API

L'API suit les conventions JSON-LD / Hydra d'API Platform.

| Endpoint | Méthode | Description |
|---|---|---|
| `/api/me` | GET | Profil de l'utilisateur connecté |
| `/api/applications` | GET | Liste des candidatures (filtrées par user) |
| `/api/applications` | POST | Créer une candidature |
| `/api/applications/{id}` | GET | Détail d'une candidature |
| `/api/applications/{id}` | PATCH | Modifier (change de statut → historique auto) |
| `/api/applications/{id}` | DELETE | Supprimer |

Toutes les routes nécessitent un JWT dans le header `Authorization: Bearer <token>`.

---

## CI/CD

Le pipeline GitHub Actions tourne sur chaque push et PR vers `main` / `develop` :

```
backend-lint  ──┐
backend-tests ──┼──► build-check (Docker)
frontend-lint ──┤
frontend-tests──┘
```

- **backend-lint** : PHP CS Fixer + PHPStan
- **backend-tests** : PHPUnit sur SQLite (aucun service externe)
- **frontend-lint** : ESLint
- **frontend-tests** : Vitest

---

## Roadmap

Voir [ROADMAP.md](ROADMAP.md).
