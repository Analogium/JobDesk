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

BACKEND_STATUS=0
FRONTEND_STATUS=0

# ── Backend ───────────────────────────────────────────────────────────────────
echo ""
info "Backend — PHPUnit (SQLite)"
echo "────────────────────────────────────────"

if ! docker inspect "$BACKEND_CONTAINER" &>/dev/null; then
  fail "Container '$BACKEND_CONTAINER' not found. Run: docker compose up -d"
  exit 1
fi

docker exec "$BACKEND_CONTAINER" php bin/phpunit --no-coverage || BACKEND_STATUS=$?

if [ $BACKEND_STATUS -eq 0 ]; then
  pass "Backend tests passed"
else
  fail "Backend tests failed"
fi

# ── Frontend ──────────────────────────────────────────────────────────────────
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
