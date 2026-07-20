#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
FRONTEND_CONTAINER="jobdesk_frontend"
MAVEN_IMAGE="maven:3.9-eclipse-temurin-21"
M2_VOLUME="jobdesk_m2"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

pass() { echo -e "${GREEN}${BOLD}✔ $*${RESET}"; }
fail() { echo -e "${RED}${BOLD}✘ $*${RESET}"; }
info() { echo -e "${CYAN}▶ $*${RESET}"; }

FIX_MODE=0
if [[ "${1:-}" == "--fix" ]]; then
  FIX_MODE=1
fi

COMPILE_STATUS=0
ESLINT_STATUS=0

# ── Backend Java — vérification de compilation ────────────────────────────────
# (Aucun formateur Java configuré ; la compilation sert de garde-fou statique.)
echo ""
info "Backend — compilation Java (Maven)"
echo "────────────────────────────────────────"

docker volume create "$M2_VOLUME" >/dev/null
docker run --rm \
  -v "$ROOT/backend-java":/app \
  -v "$M2_VOLUME":/root/.m2 \
  -w /app \
  "$MAVEN_IMAGE" mvn -B -q -DskipTests compile || COMPILE_STATUS=$?

if [ $COMPILE_STATUS -eq 0 ]; then
  pass "Compilation Java OK"
else
  fail "Compilation Java échouée"
fi

# ── Frontend — ESLint ─────────────────────────────────────────────────────────
echo ""
info "Frontend — ESLint"
echo "────────────────────────────────────────"

if ! docker inspect "$FRONTEND_CONTAINER" &>/dev/null; then
  fail "Container '$FRONTEND_CONTAINER' not found. Run: docker compose up -d"
  exit 1
fi

if [[ $FIX_MODE -eq 1 ]]; then
  docker exec "$FRONTEND_CONTAINER" npm run lint:fix || ESLINT_STATUS=$?
else
  docker exec "$FRONTEND_CONTAINER" npm run lint || ESLINT_STATUS=$?
fi

if [ $ESLINT_STATUS -eq 0 ]; then
  pass "ESLint OK"
else
  fail "ESLint found issues (run ./lint.sh --fix to auto-correct)"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════"
if [ $COMPILE_STATUS -eq 0 ] && [ $ESLINT_STATUS -eq 0 ]; then
  pass "All lint checks passed"
  exit 0
else
  fail "Some lint checks failed"
  exit 1
fi
