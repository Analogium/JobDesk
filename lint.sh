#!/usr/bin/env bash
set -euo pipefail

BACKEND_CONTAINER="jobdesk_backend"
FRONTEND_CONTAINER="jobdesk_frontend"

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

CS_STATUS=0
PHPSTAN_STATUS=0
ESLINT_STATUS=0

# ── PHP CS Fixer ──────────────────────────────────────────────────────────────
echo ""
info "Backend — PHP CS Fixer"
echo "────────────────────────────────────────"

if ! docker inspect "$BACKEND_CONTAINER" &>/dev/null; then
  fail "Container '$BACKEND_CONTAINER' not found. Run: docker compose up -d"
  exit 1
fi

if [[ $FIX_MODE -eq 1 ]]; then
  docker exec "$BACKEND_CONTAINER" composer lint:fix || CS_STATUS=$?
else
  docker exec "$BACKEND_CONTAINER" composer lint || CS_STATUS=$?
fi

if [ $CS_STATUS -eq 0 ]; then
  pass "PHP CS Fixer OK"
else
  fail "PHP CS Fixer found issues (run ./lint.sh --fix to auto-correct)"
fi

# ── PHPStan ───────────────────────────────────────────────────────────────────
echo ""
info "Backend — PHPStan (level 5)"
echo "────────────────────────────────────────"

# Ensure the test container XML exists for PHPStan's Symfony extension
docker exec "$BACKEND_CONTAINER" php bin/console cache:warmup --env=test -q

docker exec "$BACKEND_CONTAINER" composer phpstan || PHPSTAN_STATUS=$?

if [ $PHPSTAN_STATUS -eq 0 ]; then
  pass "PHPStan OK"
else
  fail "PHPStan found issues"
fi

# ── ESLint ────────────────────────────────────────────────────────────────────
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
if [ $CS_STATUS -eq 0 ] && [ $PHPSTAN_STATUS -eq 0 ] && [ $ESLINT_STATUS -eq 0 ]; then
  pass "All lint checks passed"
  exit 0
else
  fail "Some lint checks failed"
  exit 1
fi
