#!/usr/bin/env bash
set -euo pipefail
BASE=${1:-http://localhost:8080}
echo "Smoke: inventory list"; curl -fsS "$BASE/api/v1/items" >/dev/null && echo OK || echo FAIL
echo "Smoke: users list"; curl -fsS "$BASE/api/v1/users" >/dev/null && echo OK || echo FAIL
echo "Smoke: cart summary demo"; curl -fsS "$BASE/api/v1/cart-items/summary?username=demo" >/dev/null && echo OK || echo FAIL