#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

source .env 2>/dev/null || true

DISCOVERY_PORT=${DISCOVERY_PORT:-8084}
GATEWAY_PORT=${GATEWAY_PORT:-8080}
INVENTORY_PORT=${INVENTORY_PORT:-8081}
USER_PORT=${USER_PORT:-8082}
CART_PORT=${CART_PORT:-8083}
FRONTEND_DEV_PORT=${FRONTEND_DEV_PORT:-5173}

PIDS=()

cleanup(){
  if [[ "${MODE:-local}" == "local" ]]; then
    echo -e "\n==> Shutting down..."
    for pid in "${PIDS[@]:-}"; do
      if kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
      fi
    done
    wait || true
    echo "All processes stopped."  
  else
    echo "==> To stop Docker services run: docker compose down"
  fi
}
trap cleanup INT TERM EXIT

wait_for(){
  local name=$1 url=$2
  echo -n "Waiting for $name"
  for i in {1..60}; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo " - ready"; return 0; fi
    echo -n "."; sleep 1
  done
  echo "\n$name did not start in time"; return 1
}

local_mode(){
  MODE=local
  echo "==> Running in LOCAL mode"
  if command -v mvn >/dev/null 2>&1; then
    MVN_CMD="mvn"
  else
    MVN_CMD="./mvnw"
    echo "(mvn not found, using Maven Wrapper per service)"
  fi
  echo "==> Starting discovery-service"
  (cd discovery-service && chmod +x mvnw 2>/dev/null || true && $MVN_CMD -q -DskipTests spring-boot:run) & PIDS+=("$!")
  wait_for "discovery" "http://localhost:$DISCOVERY_PORT/actuator/health" || true

  for svc in inventory-service user-service cart-service; do
    echo "==> Starting $svc"
    (cd "$svc" && chmod +x mvnw 2>/dev/null || true && $MVN_CMD -q -DskipTests spring-boot:run) & PIDS+=("$!")
    sleep 2
  done

  echo "==> Building frontend (production)"
  (cd frontend && npm run build >/dev/null 2>&1)
  echo "==> Copying frontend dist into gateway static resources"
  GATEWAY_STATIC_DIR="api-gateway/src/main/resources/static"
  mkdir -p "$GATEWAY_STATIC_DIR"
  rm -rf "$GATEWAY_STATIC_DIR"/*
  cp -R frontend/dist/* "$GATEWAY_STATIC_DIR"/

  echo "==> Starting api-gateway"
  (cd api-gateway && chmod +x mvnw 2>/dev/null || true && $MVN_CMD -q -DskipTests spring-boot:run) & PIDS+=("$!")
  echo "All services started (local). Access gateway at http://localhost:$GATEWAY_PORT"
  echo "Press Ctrl+C to stop."
  wait
}

docker_mode(){
  MODE=docker
  echo "==> Running in DOCKER mode"
  if ! command -v docker >/dev/null; then
    echo "Docker not found, falling back to local mode."; local_mode; return
  fi
  # Prefer new docker compose subcommand
  COMPOSE_CMD="docker compose"
  if ! docker compose version >/dev/null 2>&1; then
    if command -v docker-compose >/dev/null; then
      COMPOSE_CMD="docker-compose"
    else
      echo "Neither 'docker compose' nor 'docker-compose' available. Falling back to local mode."; local_mode; return
    fi
  fi
  BUILD_FLAG=""
  if [[ "${1:-}" == "--build" ]]; then BUILD_FLAG="--build"; fi
  set +e
  $COMPOSE_CMD up -d $BUILD_FLAG
  local status=$?
  set -e
  if [[ $status -ne 0 ]]; then
    echo "Docker compose failed (exit $status). Falling back to local mode."; local_mode; return
  fi
  echo "Services starting in background. Follow logs with: $COMPOSE_CMD logs -f"
  echo "Gateway: http://localhost:$GATEWAY_PORT"
  echo "To stop: $COMPOSE_CMD down"
  # Keep script alive to show minimal health waiting
  wait_for "gateway" "http://localhost:$GATEWAY_PORT/actuator/health" || true
  # Tail logs for a short period as a convenience (optional)
  echo "(Ctrl+C to exit log tail without stopping containers)"
  $COMPOSE_CMD logs -f api-gateway
}

if [[ "${1:-}" == "--docker" || "${DOCKER_MODE:-false}" == "true" ]]; then
  docker_mode "${2:-}"
else
  local_mode
fi

