#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
FRONTEND_CONTAINER="jobdesk_frontend"
MAVEN_IMAGE="maven:3.9-eclipse-temurin-21"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

pass() { echo -e "${GREEN}${BOLD}✔ $*${RESET}"; }
fail() { echo -e "${RED}${BOLD}✘ $*${RESET}"; }
info() { echo -e "${CYAN}▶ $*${RESET}"; }

BACKEND_STATUS=0
FRONTEND_STATUS=0

# ── Backend Java — JUnit (H2, aucune dépendance externe) ──────────────────────
echo ""
info "Backend — JUnit / Maven (Java 21, H2)"
echo "────────────────────────────────────────"

# --user : sans ça le conteneur écrit target/ en root, et le Maven de l'IDE (m2e),
# qui tourne sous l'utilisateur courant, ne peut plus y écrire ("Operation not permitted").
# Le dépôt Maven de l'hôte (~/.m2) est réutilisé : cache partagé avec l'IDE.
docker run --rm \
  --user "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -v "$ROOT/backend-java":/app \
  -v "$HOME/.m2":/m2 \
  -w /app \
  "$MAVEN_IMAGE" mvn -B -Dmaven.repo.local=/m2 test || BACKEND_STATUS=$?

if [ $BACKEND_STATUS -eq 0 ]; then
  pass "Backend tests passed"
else
  fail "Backend tests failed"
fi

# ── Frontend — Vitest ─────────────────────────────────────────────────────────
echo ""
info "Frontend — Vitest"
echo "────────────────────────────────────────"

if ! docker inspect "$FRONTEND_CONTAINER" &>/dev/null; then
  fail "Container '$FRONTEND_CONTAINER' not found. Run: docker compose up -d"
  exit 1
fi

docker exec "$FRONTEND_CONTAINER" npm run test || FRONTEND_STATUS=$?

if [ $FRONTEND_STATUS -eq 0 ]; then
  pass "Frontend tests passed"
else
  fail "Frontend tests failed"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════"
if [ $BACKEND_STATUS -eq 0 ] && [ $FRONTEND_STATUS -eq 0 ]; then
  pass "All tests passed"
  exit 0
else
  fail "Some tests failed"
  exit 1
fi
