#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/dev/compose.yml"
ENV_FILE="$ROOT_DIR/.env"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

wait_for_http() {
  local name="$1" url="$2" attempts="${3:-40}"
  for ((i=1; i<=attempts; i++)); do
    if curl --fail --silent --show-error "$url" >/dev/null 2>&1; then
      echo "$name is ready"
      return 0
    fi
    sleep 3
  done
  echo "$name did not become ready at $url" >&2
  return 1
}

wait_for_container_health() {
  local container="$1" attempts="${2:-60}"
  for ((i=1; i<=attempts; i++)); do
    local status
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo "unknown")
    if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
      echo "Container $container status: $status"
      return 0
    fi
    sleep 2
  done
  echo "Container $container did not report healthy status" >&2
  return 1
}

require_command docker
require_command curl

"$ROOT_DIR/scripts/generate-dev-certs.sh"

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose plugin is required" >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing .env file. Copy .env.example to .env and fill in secrets." >&2
  exit 1
fi

COMPOSE_CMD=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

"${COMPOSE_CMD[@]}" config >/dev/null
"${COMPOSE_CMD[@]}" up -d --build

wait_for_http "Caddy" "http://localhost:8085/healthz"
wait_for_http "Keycloak" "http://localhost:8081/health/ready"
wait_for_http "Backend" "http://localhost:8080/actuator/health" 60 || echo "Backend health check not ready yet" >&2

wait_for_container_health bss-postgres
wait_for_container_health bss-redis
wait_for_container_health bss-keycloak
wait_for_container_health bss-backend
wait_for_container_health bss-frontend
wait_for_container_health bss-caddy

"${COMPOSE_CMD[@]}" ps
