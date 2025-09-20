#!/usr/bin/env bash
set -euo pipefail

echo "==> Shopping Cart Self-Contained Setup"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

command -v java >/dev/null || { echo "Java not found. Install JDK 17+"; exit 1; }
command -v mvn >/dev/null || { echo "Maven not found. Install Maven"; exit 1; }
command -v node >/dev/null || { echo "Node.js not found. Install Node 18+"; exit 1; }
command -v npm >/dev/null || { echo "npm not found."; exit 1; }

echo "-> Java version: $(java -version 2>&1 | head -n1)"
echo "-> Maven version: $(mvn -v | head -n1)"
echo "-> Node version: $(node -v)"
echo "-> NPM version: $(npm -v)"

if [ ! -f "$ROOT_DIR/.env" ]; then
  echo "Creating .env from example";
  cp "$ROOT_DIR/.env.example" "$ROOT_DIR/.env" || true
fi

echo "-> Pre-building backend services (offline deps)"
for svc in discovery-service api-gateway inventory-service cart-service user-service; do
  (cd "$ROOT_DIR/$svc" && mvn -q -DskipTests dependency:go-offline || true)
done

echo "-> Installing frontend dependencies"
(cd "$ROOT_DIR/frontend" && npm install --no-audit --no-fund)

echo "Setup complete. Use ./run.sh to start all services."
