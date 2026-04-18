# JobDesk — Roadmap

## Phase 1 — Socle 🔧 (en cours)

- [x] Docker Compose dev (backend + nginx + frontend + postgres)
- [x] Docker Compose prod (labels Traefik, réseaux VPS)
- [x] Symfony 7.4 + API Platform 4 + PostgreSQL
- [x] Nuxt 3 + Tailwind CSS
- [x] Google OAuth 2.0 + JWT
- [x] Entités : User, Application, StatusHistory, MailScan, Contact
- [x] Enums : ApplicationStatus, ContractType, ApplicationSource
- [x] API REST CRUD candidatures (GET, POST, PATCH, DELETE) — testé et validé
- [x] Auto-historique des changements de statut — testé et validé
- [x] Frontend : login ✓, dashboard, liste, détail, formulaire ajout — à tester en UI

---

## Phase 2 — Import URL ✅

Objectif : coller une URL d'offre → pré-remplissage automatique du formulaire.

- [x] `ScraperService` (Symfony HttpClient + DomCrawler)
- [x] Parser Welcome to the Jungle — JSON-LD `JobPosting` en priorité, CSS selectors en fallback
- [x] Parser LinkedIn — CSS selectors + criteria list pour le type de contrat
  - [x] Normalisation URL recherche (`currentJobId`) → `/jobs/view/{id}`
- [x] Parser Indeed — bloqué par Cloudflare (403) → erreur explicite renvoyée
  - [x] Normalisation URL recherche (`vjk`) → `/viewjob?jk={id}`
  - *(Playwright headless prévu en phase 4 si besoin)*
- [x] Fallback générique : extraction via meta tags (`og:title`, `og:description`…)
- [x] Endpoint API `POST /api/scrape` — auth JWT, validation URL (accents inclus), 400/502 explicites
- [x] Frontend : champ URL dans le formulaire → appel → pré-remplissage *(déjà câblé)*

---

## Phase 3 — Analyse automatique des mails 📬

Objectif : détecter les réponses (refus, entretien, offre) dans Gmail et mettre à jour les statuts automatiquement.

- [ ] Connexion Gmail API via OAuth (scope `gmail.readonly`)
  - [ ] Bouton "Connecter Gmail" dans les settings utilisateur
  - [ ] Stockage `gmailToken` + `gmailRefreshToken` chiffrés en base
- [ ] `MailScanService` : récupération et analyse des mails
  - [ ] Matching expéditeur ↔ entreprise (nom + domaine)
  - [ ] Détection refus (mots-clés FR/EN)
  - [ ] Détection entretien (mots-clés FR/EN)
  - [ ] Détection offre (mots-clés FR/EN)
- [ ] Mise à jour automatique des statuts + entrée `StatusHistory` (trigger `auto_mail`)
- [ ] `MailScanScheduler` (Symfony Scheduler, toutes les 2h)
- [ ] Logs de scan dans `MailScan` (mails analysés, matches trouvés, erreurs)
- [ ] Notifications in-app des changements détectés

---

## Phase 4 — Dashboard & polish 📊

Objectif : dashboard lisible, rappels de relance, UI soignée, prêt pour démo.

- [ ] Graphes dashboard
  - [ ] Donut répartition par statut (Chart.js ou unocss-based)
  - [ ] Funnel conversion Applied → Interview → Offer
  - [ ] Barres : activité par semaine
  - [ ] Taux de réponse + délai moyen
  - [ ] Répartition par source (LinkedIn, WTTJ…)
- [ ] Système de rappels de relance
  - [ ] Détection candidatures APPLIED/WAITING depuis X jours (défaut 7j)
  - [ ] Bannière in-app "Pensez à relancer [Entreprise]"
  - [ ] Actions : marquer relancé / ignorer
- [ ] Page settings utilisateur (configurer délai de relance, déconnecter Gmail)
- [ ] UI finale soignée (animations, empty states, responsive mobile)
- [ ] Tests unitaires backend (PHPUnit) — ScraperService + MailScanService

---

## Phase 5 — Mise en production 🚀

- [ ] Créer le repo GitHub + pousser le code
- [ ] Configurer les secrets GitHub (GOOGLE_CLIENT_ID, JWT_PASSPHRASE…)
- [ ] GitHub Actions : build → push images → deploy SSH sur VPS
- [ ] Choisir un domaine (ex: `jobdesk.theolambert.dev`)
- [ ] DNS Cloudflare : enregistrement A → IP VPS
- [ ] Intégrer `docker-compose.prod.yml` dans `/home/lambert/apps/docker-compose.yml`
- [ ] Créer la base PostgreSQL sur le VPS
- [ ] Premier déploiement + vérification SSL Traefik
- [ ] Ajouter à Uptime Kuma
- [ ] Google Cloud Console : ajouter l'URI de redirection prod

---

## Points de vigilance

| Sujet | Détail |
|---|---|
| LinkedIn scraping | Anti-bot instable — prévoir fallback meta tags propre |
| Gmail API validation | Mode test en dev (comptes autorisés), vérification Google requise en prod |
| Tokens OAuth Gmail | Stocker chiffrés (`symfony/encryption` ou champ encrypté Doctrine) |
| RGPD | Mails analysés à la volée, jamais stockés |
| `APP_SECRET` | Générer une vraie valeur en prod (`openssl rand -hex 32`) |
