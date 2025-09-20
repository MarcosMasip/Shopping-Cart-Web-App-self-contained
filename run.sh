#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

source .env 2>/dev/null || true

GATEWAY_PORT=${GATEWAY_PORT:-8080}
INVENTORY_PORT=${INVENTORY_PORT:-8081}
USER_PORT=${USER_PORT:-8082}
CART_PORT=${CART_PORT:-8083}
FRONTEND_DEV_PORT=${FRONTEND_DEV_PORT:-5173}

# Flags
FORCE_RESTART=false
DOCKER_ARG=false
REBUILD=false
OPEN_BROWSER=false
for arg in "$@"; do
  case "$arg" in
    --force-restart) FORCE_RESTART=true ;;
    --docker) DOCKER_ARG=true ;;
    --rebuild) REBUILD=true ;;
    --open) OPEN_BROWSER=true ;;
  esac
done

PIDS=()
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

JAR_VERSION="1.0.0" # keep in sync with pom versions if they change
SERVICES=(inventory-service user-service cart-service api-gateway)

jar_path(){
  local svc=$1
  echo "$ROOT_DIR/$svc/target/$svc-$JAR_VERSION.jar"
}

ensure_built(){
  local svc=$1
  local jar
  jar=$(jar_path "$svc")
  if $REBUILD || [[ ! -f "$jar" ]]; then
    echo "[BUILD] Packaging $svc"
    (cd "$ROOT_DIR/$svc" && ./mvnw -q -DskipTests package) || {
      echo "[ERROR] Build failed for $svc"; exit 1; }
  fi
}

start_service(){
  local svc=$1 port=$2
  local jar; jar=$(jar_path "$svc")
  ensure_built "$svc"
  echo "==> Starting $svc (port $port)"
  # Use separate log per service
  local log_file="$LOG_DIR/$svc.log"
  # Basic health logging: first line + tail background
  local effective_java_opts="${JAVA_OPTS:-}"
  (SPRING_DEVTOOLS_RESTART_ENABLED=false java $effective_java_opts -jar "$jar" --server.port=$port >"$log_file" 2>&1 & echo $! >"$LOG_DIR/$svc.pid")
  local pid=$(cat "$LOG_DIR/$svc.pid")
  PIDS+=("$pid")
}

stop_existing_on_port(){
  local p=$1
  local pids
  pids=$(lsof -t -nP -iTCP:"$p" -sTCP:LISTEN 2>/dev/null || true)
  if [[ -n "$pids" ]]; then
    echo "[INFO] Terminating processes on port $p: $pids"
    echo "$pids" | xargs -r kill 2>/dev/null || true
    for i in {1..15}; do
      lsof -nP -iTCP:"$p" -sTCP:LISTEN >/dev/null 2>&1 || break
      sleep 0.2
    done
    if lsof -nP -iTCP:"$p" -sTCP:LISTEN >/dev/null 2>&1; then
      echo "[WARN] Port $p still busy; sending SIGKILL"; echo "$pids" | xargs -r kill -9 2>/dev/null || true
    fi
  fi
}

cleanup(){
  if [[ "${MODE:-local}" == "local" ]]; then
    echo -e "\n==> Shutting down..."
    for pid in "${PIDS[@]:-}"; do
      if kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
      fi
    done
    wait || true
    echo "All processes stopped. Logs in $LOG_DIR"  
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
  echo "==> Running in LOCAL mode (jar launch)"
  # helper to check if a port is already in use
  port_in_use(){ lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }
  if $FORCE_RESTART; then
    echo "==> FORCE_RESTART enabled: will terminate existing processes on target ports"
  fi

  # Build backend jars (skip gateway until after frontend build so static assets are in place if resources packaging matters)
  # Build backend service jars ahead of time (not starting yet)
  for svc in inventory-service user-service cart-service; do
    # Derive associated port variable manually
    case "$svc" in
      inventory-service) svc_port=$INVENTORY_PORT ;;
      user-service) svc_port=$USER_PORT ;;
      cart-service) svc_port=$CART_PORT ;;
    esac
    if $FORCE_RESTART && port_in_use "$svc_port"; then
      stop_existing_on_port "$svc_port"
    fi
    ensure_built "$svc"
  done

  echo "==> Building frontend (production)"
  (cd frontend && npm run build >/dev/null 2>&1)
  echo "==> Copying frontend dist into gateway static resources"
  GATEWAY_STATIC_DIR="api-gateway/src/main/resources/static"
  mkdir -p "$GATEWAY_STATIC_DIR"
  rm -rf "$GATEWAY_STATIC_DIR"/*
  cp -R frontend/dist/* "$GATEWAY_STATIC_DIR"/

  # Build gateway after assets copied (so they can be included if using default resource filtering)
  ensure_built api-gateway

  # Start backend services
  port_map=("inventory-service:$INVENTORY_PORT" "user-service:$USER_PORT" "cart-service:$CART_PORT" "api-gateway:$GATEWAY_PORT")
  for entry in "${port_map[@]}"; do
    svc="${entry%%:*}"; port="${entry##*:}"
    if port_in_use "$port"; then
      if $FORCE_RESTART; then
        stop_existing_on_port "$port"
      fi
    fi
    if port_in_use "$port"; then
      echo "[WARN] Port $port already in use. Skipping start of $svc."
    else
      start_service "$svc" "$port"
      # confirm log file touched
      touch "$LOG_DIR/$svc.log" 2>/dev/null || true
      sleep 1
    fi
  done

  echo "==> Waiting for service readiness"
  # readiness endpoints (direct service ports)
  declare -A readiness
  readiness[INVENTORY]="http://localhost:$INVENTORY_PORT/api/v1/items"
  readiness[USER]="http://localhost:$USER_PORT/api/v1/users"
  readiness[CART]="http://localhost:$CART_PORT/api/v1/cart-items"
  readiness[GATEWAY]="http://localhost:$GATEWAY_PORT/api/v1/items"

  printf "%-10s %-45s %s\n" "SERVICE" "URL" "STATUS"
  for key in INVENTORY USER CART GATEWAY; do
    url="${readiness[$key]}"
    status="WAITING"
    for i in {1..30}; do
      if curl -fsS "$url" >/dev/null 2>&1; then status="OK"; break; fi
      sleep 1
    done
    if [[ "$status" != "OK" ]]; then status="TIMEOUT"; fi
    printf "%-10s %-45s %s\n" "$key" "$url" "$status"
  done

  echo
  echo "All services launched. Gateway: http://localhost:$GATEWAY_PORT"
  echo "Logs: $LOG_DIR (tail -f logs/api-gateway.log)"
  [[ $OPEN_BROWSER == true ]] && { command -v open >/dev/null 2>&1 && open "http://localhost:$GATEWAY_PORT" || true; }
  echo "Press Ctrl+C to stop."
  # Keep foreground process alive so trap handles Ctrl+C.
  # Instead of plain 'wait' (which would return immediately once background startup completes), loop while any service pid lives.
  while true; do
    live=0
    for pid in "${PIDS[@]}"; do
      if kill -0 "$pid" 2>/dev/null; then
        live=1; break
      fi
    done
    if [[ $live -eq 0 ]]; then
      echo "One or more services exited; shutting down supervisor."; break
    fi
    sleep 2
  done
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

