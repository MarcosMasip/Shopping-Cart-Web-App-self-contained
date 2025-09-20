#!/usr/bin/env bash
set -euo pipefail

echo "==> Shopping Cart Self-Contained Setup"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

command -v java >/dev/null || { echo "Java not found. Install JDK 17+"; exit 1; }
command -v node >/dev/null || { echo "Node.js not found. Install Node 18+"; exit 1; }
command -v npm >/dev/null || { echo "npm not found."; exit 1; }

if command -v mvn >/dev/null 2>&1; then
  MVN_CMD="mvn"
  MVN_VERSION_LINE=$(mvn -v | head -n1)
else
  # We'll rely on service-level Maven Wrapper scripts
  MVN_CMD="./mvnw"
  # pick one wrapper to show version
  if [ -f "$ROOT_DIR/discovery-service/mvnw" ]; then
    (cd "$ROOT_DIR/discovery-service" && chmod +x mvnw 2>/dev/null || true)
    MVN_VERSION_LINE=$(cd "$ROOT_DIR/discovery-service" && ./mvnw -v | head -n1)
  else
    MVN_VERSION_LINE="Maven Wrapper will be used (version shown at first build)"
  fi
  echo "Global 'mvn' not found. Falling back to per-service Maven Wrapper scripts. (No global Maven installation required.)"
fi

echo "-> Java version: $(java -version 2>&1 | head -n1)"
echo "-> Maven version: $MVN_VERSION_LINE"
echo "-> Node version: $(node -v)"
echo "-> NPM version: $(npm -v)"

if [ ! -f "$ROOT_DIR/.env" ]; then
  echo "Creating .env from example";
  cp "$ROOT_DIR/.env.example" "$ROOT_DIR/.env" || true
fi

echo "-> Pre-building backend services (offline deps)"
for svc in discovery-service api-gateway inventory-service cart-service user-service; do
  (cd "$ROOT_DIR/$svc" && chmod +x mvnw 2>/dev/null || true)
  (cd "$ROOT_DIR/$svc" && $MVN_CMD -q -DskipTests dependency:go-offline || true)
done

echo "-> Installing frontend dependencies"
(cd "$ROOT_DIR/frontend" && npm install --no-audit --no-fund)

echo "Setup complete. Use ./run.sh to start all services."
