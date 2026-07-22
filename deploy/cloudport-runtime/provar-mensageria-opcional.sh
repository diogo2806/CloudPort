#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/cloudport-runtime/docker-compose.yml"
PROJECT_NAME="${CLOUDPORT_OPTIONAL_MESSAGING_PROJECT:-cloudport-optional-messaging-proof}"
PUBLIC_PORT="${CLOUDPORT_OPTIONAL_MESSAGING_PORT:-18083}"
IMAGE="${CLOUDPORT_RUNTIME_IMAGE:-cloudport-runtime:optional-messaging-smoke}"
EVIDENCE_DIR="${CLOUDPORT_OPTIONAL_MESSAGING_EVIDENCE_DIR:-/tmp/cloudport-optional-messaging-evidence}"
CURRENT_STAGE="inicializacao"

export DB_NAME="${DB_NAME:-cloudport_optional_messaging}"
export DB_USER="${DB_USER:-cloudport}"
export DB_PASS="${DB_PASS:-cloudport-optional-postgres}"
export DB_SCHEMA="${DB_SCHEMA:-cloudport_autenticacao,cloudport_carga_geral,cloudport_gate,cloudport_rail,cloudport_visibilidade,cloudport_yard,cloudport_navio,cloudport_siderurgico}"
export SECURITY_JWT_SECRET="${SECURITY_JWT_SECRET:-cloudport-optional-jwt-secret-with-at-least-32-bytes}"
export SECURITY_JWT_EXPIRATION_MS="${SECURITY_JWT_EXPIRATION_MS:-7200000}"
export ADMIN_EMAIL="${ADMIN_EMAIL:-admin-optional@cloudport.local}"
export ADMIN_PASSWORD="${ADMIN_PASSWORD:-cloudport-optional-admin}"
export RABBITMQ_USERNAME="${RABBITMQ_USERNAME:-cloudport}"
export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-cloudport-optional-rabbit}"
export CLOUDPORT_INTERNAL_SERVICE_KEY="${CLOUDPORT_INTERNAL_SERVICE_KEY:-cloudport-optional-service-key}"
export SECURITY_CORS_ALLOWED_ORIGINS="${SECURITY_CORS_ALLOWED_ORIGINS:-http://localhost:${PUBLIC_PORT}}"
export TOS_API_BASE_URL="${TOS_API_BASE_URL:-http://127.0.0.1:65530}"
export CLOUDPORT_PUBLIC_PORT="$PUBLIC_PORT"
export CLOUDPORT_NETWORK_NAME="${CLOUDPORT_NETWORK_NAME:-${PROJECT_NAME}-network}"
export CLOUDPORT_RUNTIME_IMAGE="$IMAGE"
export CLOUDPORT_RUNTIME_INSTANCE_ID="optional-messaging-smoke"
export CLOUDPORT_RUNTIME_REVISAO="${GITHUB_SHA:-local-smoke}"
export CLOUDPORT_RUNTIME_CUTOVER_WRITES_ENABLED="true"
export CLOUDPORT_RUNTIME_JOBS_ENABLED="false"
export RABBITMQ_STARTUP_ATTEMPTS="30"
export RABBITMQ_STARTUP_INTERVAL_SECONDS="2"
export RABBITMQ_STARTUP_TIMEOUT_SECONDS="2"

COMPOSE=(docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE")

rm -rf "$EVIDENCE_DIR"
mkdir -p "$EVIDENCE_DIR"

stage() {
  CURRENT_STAGE="$1"
  printf 'STAGE=%s\n' "$CURRENT_STAGE" > "$EVIDENCE_DIR/status.txt"
  echo "[optional-messaging] $CURRENT_STAGE"
}

capture_diagnostics() {
  "${COMPOSE[@]}" --profile messaging ps -a > "$EVIDENCE_DIR/compose-ps.txt" 2>&1 || true
  "${COMPOSE[@]}" --profile messaging logs --no-color --tail=300 > "$EVIDENCE_DIR/compose.log" 2>&1 || true
}

cleanup() {
  local exit_code=$?
  capture_diagnostics
  if [[ $exit_code -eq 0 ]]; then
    printf 'RESULT=success\nSTAGE=%s\n' "$CURRENT_STAGE" > "$EVIDENCE_DIR/status.txt"
  else
    printf 'RESULT=failure\nSTAGE=%s\n' "$CURRENT_STAGE" > "$EVIDENCE_DIR/status.txt"
    echo "[optional-messaging] falha no estágio: $CURRENT_STAGE" >&2
  fi
  if [[ "${CLOUDPORT_OPTIONAL_MESSAGING_KEEP_ENV:-false}" != "true" ]]; then
    "${COMPOSE[@]}" --profile messaging down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  exit "$exit_code"
}
trap cleanup EXIT

wait_http() {
  local url=$1
  local attempts=${2:-120}
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS --max-time 3 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "timeout aguardando $url" >&2
  return 1
}

wait_healthy() {
  local service=$1
  local attempts=${2:-60}
  local container_id
  container_id="$("${COMPOSE[@]}" --profile messaging ps -q "$service")"
  [[ -n "$container_id" ]]

  for _ in $(seq 1 "$attempts"); do
    if [[ "$(docker inspect -f '{{.State.Health.Status}}' "$container_id" 2>/dev/null || true)" == "healthy" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "timeout aguardando health do serviço $service" >&2
  return 1
}

stage "limpando ambiente anterior"
"${COMPOSE[@]}" --profile messaging down -v --remove-orphans >/dev/null 2>&1 || true

stage "validando serviços expostos por profile"
default_services="$("${COMPOSE[@]}" config --services)"
profile_services="$("${COMPOSE[@]}" --profile messaging config --services)"
printf '%s\n' "$default_services" > "$EVIDENCE_DIR/services-default.txt"
printf '%s\n' "$profile_services" > "$EVIDENCE_DIR/services-messaging.txt"
if grep -qx 'rabbitmq' "$EVIDENCE_DIR/services-default.txt"; then
  echo "RabbitMQ apareceu no compose sem o profile messaging" >&2
  exit 1
fi
grep -qx 'rabbitmq' "$EVIDENCE_DIR/services-messaging.txt"

stage "preparando imagem do runtime"
if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
  docker build -f "$ROOT_DIR/backend/cloudport-runtime/Dockerfile" -t "$IMAGE" "$ROOT_DIR"
fi

stage "iniciando sem mensageria"
export RABBITMQ_ENABLED="false"
export CLOUDPORT_RUNTIME_CONSUMERS_ENABLED="false"
"${COMPOSE[@]}" up -d postgres redis cloudport-runtime
wait_http "http://localhost:${PUBLIC_PORT}/actuator/health/readiness"
"${COMPOSE[@]}" logs --no-color cloudport-runtime > "$EVIDENCE_DIR/runtime-disabled.log"
grep -Fq 'RabbitMQ desabilitado' "$EVIDENCE_DIR/runtime-disabled.log"
if "${COMPOSE[@]}" ps --services --status running | grep -qx 'rabbitmq'; then
  echo "RabbitMQ foi iniciado mesmo com RABBITMQ_ENABLED=false" >&2
  exit 1
fi
curl -fsS "http://localhost:${PUBLIC_PORT}/actuator/health/readiness" > "$EVIDENCE_DIR/readiness-disabled.json"

stage "comprovando falha clara sem profile"
set +e
failure_output="$("${COMPOSE[@]}" run --rm --no-deps \
  -e RABBITMQ_ENABLED=true \
  -e RABBITMQ_STARTUP_ATTEMPTS=2 \
  -e RABBITMQ_STARTUP_INTERVAL_SECONDS=1 \
  -e RABBITMQ_STARTUP_TIMEOUT_SECONDS=1 \
  cloudport-runtime 2>&1)"
failure_status=$?
set -e
printf '%s\n' "$failure_output" > "$EVIDENCE_DIR/enabled-without-profile.log"
if [[ $failure_status -ne 78 ]]; then
  echo "runtime deveria sair com código 78 sem RabbitMQ; código recebido: $failure_status" >&2
  exit 1
fi
grep -Fq 'RABBITMQ_ENABLED=true, mas RabbitMQ não está acessível' "$EVIDENCE_DIR/enabled-without-profile.log"
grep -Fq 'Inicie o Docker Compose com --profile messaging' "$EVIDENCE_DIR/enabled-without-profile.log"

stage "iniciando com profile de mensageria"
export RABBITMQ_ENABLED="true"
export CLOUDPORT_RUNTIME_CONSUMERS_ENABLED="true"
"${COMPOSE[@]}" --profile messaging up -d rabbitmq
wait_healthy rabbitmq
"${COMPOSE[@]}" --profile messaging up -d --force-recreate cloudport-runtime
wait_http "http://localhost:${PUBLIC_PORT}/actuator/health/readiness"
"${COMPOSE[@]}" --profile messaging exec -T rabbitmq rabbitmq-diagnostics -q ping
"${COMPOSE[@]}" --profile messaging logs --no-color cloudport-runtime > "$EVIDENCE_DIR/runtime-enabled.log"
grep -Fq 'RabbitMQ disponível' "$EVIDENCE_DIR/runtime-enabled.log"
"${COMPOSE[@]}" --profile messaging ps --services --status running > "$EVIDENCE_DIR/services-running-enabled.txt"
grep -qx 'rabbitmq' "$EVIDENCE_DIR/services-running-enabled.txt"
curl -fsS "http://localhost:${PUBLIC_PORT}/actuator/health/readiness" > "$EVIDENCE_DIR/readiness-enabled.json"

stage "gerando evidência consolidada"
cat > "$EVIDENCE_DIR/report.md" <<EOF
# Evidência de mensageria opcional

- Resultado: **APROVADO**
- Revisão: \`${GITHUB_SHA:-local-smoke}\`
- Sem profile: RabbitMQ não foi criado e o runtime ficou saudável.
- Habilitado sem profile: runtime encerrou com código 78 e orientação operacional explícita.
- Com \`--profile messaging\`: RabbitMQ e runtime ficaram saudáveis.
EOF

stage "mensageria opcional comprovada"
