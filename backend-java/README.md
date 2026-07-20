# JobDesk — Backend Java (Spring Boot)

Réécriture du backend Symfony/API Platform en **Java 21 + Spring Boot 3.x**.
Le mapping JPA colle au schéma existant (créé par Doctrine) : `ddl-auto: validate`,
**aucune table n'est recréée**, les données PostgreSQL actuelles sont conservées.

## État de la migration

| Étape | Contenu | Statut |
|-------|---------|--------|
| 1 | Entités + CRUD candidatures + auth Google/JWT + `/api/me` | ✅ fait |
| 2 | Scan Gmail (`/api/gmail/*`) + scheduler 2h | ✅ fait |
| 3 | Scraper d'offres (`/api/scrape`, JSoup + Playwright) | ✅ fait |

Le backend Java couvre désormais **toutes** les fonctionnalités. L'ancien backend
`../backend` (Symfony) reste en place comme fallback tant que la bascule finale
(suppression de `backend/` + `nginx`) n'est pas confirmée en prod.

## Développement

Java 21 requis. Sans Java/Maven local, tout passe par Docker :

```bash
# Tests (H2 en mémoire, aucune dépendance externe)
docker run --rm -v "$PWD":/app -v jobdesk_m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn -B test
```

## Lancer en parallèle de l'ancien backend

Service gated derrière le profil compose `java` (n'affecte pas `docker compose up`).
Depuis la racine du repo :

```bash
export APP_SECRET=change_me_generate_a_32_char_secret   # DOIT = APP_SECRET Symfony
export GOOGLE_CLIENT_ID=...  GOOGLE_CLIENT_SECRET=...
docker compose --profile java up -d --build backend-java   # écoute sur http://localhost:8001
```

- `APP_SECRET` doit être **identique** à celui de l'ancien backend : c'est la clé
  AES-256-GCM qui déchiffre les tokens Gmail déjà stockés.
- Le JWT est signé en HS256 avec `APP_SECRET` (ou `JWT_SECRET` s'il est défini).
- Login Google : le `redirect_uri` est `${BACKEND_URL}/auth/google/check`. Pour tester
  sur le port 8001, ajouter `http://localhost:8001/auth/google/check` aux URIs de
  redirection autorisées dans la console Google Cloud.

### Pointer le frontend dessus

Dans `docker-compose.yml`, passer `NUXT_PUBLIC_API_URL: http://localhost:8001` sur le
service `frontend`, puis `docker compose up -d frontend`.

## Contrat API (inchangé côté frontend, hors format)

- `GET /api/applications?order[createdAt]=desc&page=N&status=&source=&contractType=`
  → `{ "member": [...], "totalItems": N }` (pagination 30, comme l'ancien API Platform).
- `GET|POST|PATCH|DELETE /api/applications/{id}` — PATCH en `application/merge-patch+json`.
- `GET /api/me` — profil courant.
- `GET /auth/google` → consentement Google → `GET /auth/google/check` → redirection
  vers `${FRONTEND_URL}/auth/callback?token=<JWT>`.

## Architecture

```
domain/            entités JPA + enums (+ converters : chiffrement, source minuscule)
repository/        Spring Data JPA (filtrage par user)
service/           logique métier (historique de statut au changement)
web/ + web/dto/    controllers REST + DTO (l'entité n'est jamais exposée)
security/          Google OAuth (RestClient), JWT HS256, filtre d'auth
config/            SecurityConfig/CORS, init clé de chiffrement
```
