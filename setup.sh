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
  # We'll rely on service-level Maven Wrapper scripts (discovery-service removed in simplified stack)
  MVN_CMD="./mvnw"
  # find the first existing service that has a mvnw to display version
  MVN_VERSION_LINE="Maven Wrapper will be used (version shown at first build)"
  for candidate in api-gateway inventory-service cart-service user-service; do
    if [ -f "$ROOT_DIR/$candidate/mvnw" ]; then
      (cd "$ROOT_DIR/$candidate" && chmod +x mvnw 2>/dev/null || true)
      MVN_VERSION_LINE=$(cd "$ROOT_DIR/$candidate" && ./mvnw -v | head -n1)
      break
    fi
  done
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
# Discovery service removed; only build active services
for svc in api-gateway inventory-service cart-service user-service; do
  (cd "$ROOT_DIR/$svc" && chmod +x mvnw 2>/dev/null || true)
  (cd "$ROOT_DIR/$svc" && $MVN_CMD -q -DskipTests dependency:go-offline || true)
done

echo "-> Installing frontend dependencies"
(cd "$ROOT_DIR/frontend" && npm install --no-audit --no-fund)

echo "Setup complete. Use ./run.sh to start all services."
