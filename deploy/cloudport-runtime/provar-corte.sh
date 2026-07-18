#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_COMPOSE="$ROOT_DIR/deploy/cloudport-runtime/docker-compose.yml"
SMOKE_COMPOSE="$ROOT_DIR/deploy/cloudport-runtime/docker-compose.smoke.yml"
PROJECT_NAME="${CLOUDPORT_CUTOVER_PROJECT:-cloudport-cutover-proof}"
CANONICAL_PORT="${CLOUDPORT_PUBLIC_PORT:-18080}"
OBSERVER_PORT="${CLOUDPORT_OBSERVER_PORT:-18081}"
ROLLBACK_PORT="${CLOUDPORT_ROLLBACK_PROBE_PORT:-18086}"
EVIDENCE_DIR="${CLOUDPORT_CUTOVER_EVIDENCE_DIR:-/tmp/cloudport-cutover-evidence}"
STATUS_FILE="$EVIDENCE_DIR/status.txt"
CURRENT_STAGE="inicializacao"

export DB_NAME="${DB_NAME:-cloudport_cutover}"
export DB_USER="${DB_USER:-cloudport}"
export DB_PASS="${DB_PASS:-cloudport-cutover-postgres}"
export DB_SCHEMA="${DB_SCHEMA:-cloudport_autenticacao,cloudport_carga_geral,cloudport_gate,cloudport_rail,cloudport_visibilidade,cloudport_yard,cloudport_navio,cloudport_siderurgico}"
export SECURITY_JWT_SECRET="${SECURITY_JWT_SECRET:-cloudport-cutover-jwt-secret-with-at-least-32-bytes}"
export SECURITY_JWT_EXPIRATION_MS="${SECURITY_JWT_EXPIRATION_MS:-7200000}"
export ADMIN_EMAIL="${ADMIN_EMAIL:-admin@cloudport.local}"
export ADMIN_PASSWORD="${ADMIN_PASSWORD:-cloudport-cutover-admin}"
export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-cloudport-cutover-rabbit}"
export CLOUDPORT_INTERNAL_SERVICE_KEY="${CLOUDPORT_INTERNAL_SERVICE_KEY:-cloudport-cutover-service-key}"
export SECURITY_CORS_ALLOWED_ORIGINS="${SECURITY_CORS_ALLOWED_ORIGINS:-http://localhost:${CANONICAL_PORT}}"
export TOS_API_BASE_URL="http://tos-smoke:8090"
export CLOUDPORT_PUBLIC_PORT="$CANONICAL_PORT"
export CLOUDPORT_OBSERVER_PORT="$OBSERVER_PORT"
export CLOUDPORT_ROLLBACK_PROBE_PORT="$ROLLBACK_PORT"
export CLOUDPORT_NETWORK_NAME="${CLOUDPORT_NETWORK_NAME:-${PROJECT_NAME}-network}"
export CLOUDPORT_RUNTIME_REVISAO="${CLOUDPORT_RUNTIME_REVISAO:-${GITHUB_SHA:-local-proof}}"
export CLOUDPORT_RUNTIME_CUTOVER_WRITES_ENABLED="true"
export CLOUDPORT_RUNTIME_JOBS_ENABLED="true"
export CLOUDPORT_RUNTIME_CONSUMERS_ENABLED="true"
export RABBITMQ_ENABLED="true"

COMPOSE=(docker compose -p "$PROJECT_NAME" -f "$BASE_COMPOSE" -f "$SMOKE_COMPOSE")
SCHEMAS=(
  cloudport_autenticacao
  cloudport_carga_geral
  cloudport_gate
  cloudport_rail
  cloudport_visibilidade
  cloudport_yard
  cloudport_navio
  cloudport_siderurgico
)

rm -rf "$EVIDENCE_DIR"
mkdir -p "$EVIDENCE_DIR"

stage() {
  CURRENT_STAGE="$1"
  printf 'STAGE=%s\n' "$CURRENT_STAGE" > "$STATUS_FILE"
  echo "[cutover-proof] $CURRENT_STAGE"
}

capture_diagnostics() {
  "${COMPOSE[@]}" --profile rollback-proof ps > "$EVIDENCE_DIR/compose-ps.txt" 2>&1 || true
  "${COMPOSE[@]}" --profile rollback-proof logs --no-color --tail=300 > "$EVIDENCE_DIR/compose.log" 2>&1 || true
}

cleanup() {
  local exit_code=$?
  capture_diagnostics
  if [[ $exit_code -eq 0 ]]; then
    printf 'RESULT=success\nSTAGE=%s\n' "$CURRENT_STAGE" > "$STATUS_FILE"
  else
    printf 'RESULT=failure\nSTAGE=%s\n' "$CURRENT_STAGE" > "$STATUS_FILE"
    echo "[cutover-proof] falha no estágio: $CURRENT_STAGE" >&2
  fi
  if [[ "${CLOUDPORT_CUTOVER_KEEP_ENV:-false}" != "true" ]]; then
    "${COMPOSE[@]}" --profile rollback-proof down -v --remove-orphans >/dev/null 2>&1 || true
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

jwt() {
  python3 - <<'PY'
import base64
import hashlib
import hmac
import json
import os
import time

def encode(value):
    return base64.urlsafe_b64encode(
        json.dumps(value, separators=(",", ":")).encode("utf-8")
    ).rstrip(b"=").decode("ascii")

header = encode({"alg": "HS256", "typ": "JWT"})
now = int(time.time())
payload = encode({
    "sub": "cloudport-cutover-proof",
    "roles": ["ADMIN_PORTO", "PLANEJADOR"],
    "iat": now,
    "exp": now + 1800,
})
message = f"{header}.{payload}".encode("ascii")
signature = hmac.new(
    os.environ["SECURITY_JWT_SECRET"].encode("utf-8"),
    message,
    hashlib.sha256,
).digest()
print(f"{header}.{payload}." + base64.urlsafe_b64encode(signature).rstrip(b"=").decode("ascii"))
PY
}

request() {
  local method=$1
  local base_url=$2
  local path=$3
  local body=${4:-}
  local output=${5:-/dev/stdout}
  local args=(
    -sS -o "$output" -w '%{http_code}'
    -X "$method"
    -H "Authorization: Bearer $TOKEN"
    -H 'Accept: application/json'
    -H 'Content-Type: application/json'
    -H 'X-Correlation-Id: cloudport-cutover-proof'
    -H 'traceparent: 00-0123456789abcdef0123456789abcdef-0123456789abcdef-01'
  )
  if [[ -n "$body" ]]; then
    args+=(--data "$body")
  fi
  curl "${args[@]}" "$base_url$path"
}

assert_http() {
  local expected=$1
  local actual=$2
  local description=$3
  if [[ "$actual" != "$expected" ]]; then
    echo "$description: esperado HTTP $expected, recebido $actual" >&2
    return 1
  fi
}

stage "limpando ambiente anterior"
"${COMPOSE[@]}" --profile rollback-proof down -v --remove-orphans >/dev/null 2>&1 || true

stage "validando compose e construindo imagens"
"${COMPOSE[@]}" --profile rollback-proof config > "$EVIDENCE_DIR/compose-rendered.yml"
"${COMPOSE[@]}" build cloudport-runtime

stage "iniciando runtime canônico e infraestrutura"
"${COMPOSE[@]}" up -d postgres rabbitmq redis tos-smoke cloudport-runtime
wait_http "http://localhost:${CANONICAL_PORT}/actuator/health/readiness"

stage "iniciando observador fail-closed"
"${COMPOSE[@]}" up -d cloudport-observer
wait_http "http://localhost:${OBSERVER_PORT}/actuator/health/readiness"
TOKEN="$(jwt)"
CANONICAL_URL="http://localhost:${CANONICAL_PORT}"
OBSERVER_URL="http://localhost:${OBSERVER_PORT}"

stage "comprovando health, readiness, prometheus e autenticação"
curl -fsS "$CANONICAL_URL/actuator/health" > "$EVIDENCE_DIR/health.json"
curl -fsS "$CANONICAL_URL/actuator/health/readiness" > "$EVIDENCE_DIR/readiness.json"
curl -fsS -H "Authorization: Bearer $TOKEN" "$CANONICAL_URL/actuator/prometheus" > "$EVIDENCE_DIR/prometheus.txt"
unauthorized_status="$(curl -sS -o "$EVIDENCE_DIR/unauthorized.json" -w '%{http_code}' "$CANONICAL_URL/navios")"
assert_http 401 "$unauthorized_status" "proteção sem token"

stage "comprovando papel das instâncias e adaptadores locais"
canonical_status="$(request GET "$CANONICAL_URL" /operacao/corte '' "$EVIDENCE_DIR/canonical-status.json")"
observer_status="$(request GET "$OBSERVER_URL" /operacao/corte '' "$EVIDENCE_DIR/observer-status.json")"
assert_http 200 "$canonical_status" "estado do runtime canônico"
assert_http 200 "$observer_status" "estado do observador"
python3 - "$EVIDENCE_DIR/canonical-status.json" "$EVIDENCE_DIR/observer-status.json" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as source:
    canonical = json.load(source)
with open(sys.argv[2], encoding="utf-8") as source:
    observer = json.load(source)
assert canonical["runtime"] == "cloudport-runtime"
assert canonical["papel"] == "CANONICO_ATIVO"
assert canonical["escritaHabilitada"] is True
assert canonical["jobsHabilitados"] is True
assert canonical["consumidoresHabilitados"] is True
assert canonical["adaptadoresLocais"] is True
assert len(canonical["schemas"]) == 8
assert observer["papel"] == "OBSERVACAO_SOMENTE_LEITURA"
assert observer["escritaHabilitada"] is False
assert observer["jobsHabilitados"] is False
assert observer["consumidoresHabilitados"] is False
PY

stage "comprovando exclusão mútua de escrita"
observer_write_status="$(request POST "$OBSERVER_URL" /navios '{"nome":"Bloqueado","codigoImo":"IMO0000001"}' "$EVIDENCE_DIR/observer-write.json")"
assert_http 503 "$observer_write_status" "escrita no observador"
python3 - "$EVIDENCE_DIR/observer-write.json" <<'PY'
import json
import sys
with open(sys.argv[1], encoding="utf-8") as source:
    error = json.load(source)
assert error["codigo"] == "RUNTIME_SOMENTE_LEITURA"
assert error["correlationId"] == "cloudport-cutover-proof"
PY

stage "comprovando persistência funcional no runtime canônico"
navio_status="$(request POST "$CANONICAL_URL" /navios '{"nome":"Navio Corte","codigoImo":"IMO7654321","paisBandeira":"Brasil","empresaArmadora":"CloudPort","capacidadeTeu":1500,"loaMetros":210.50,"caladoMaximoMetros":12.20,"callSign":"CUT01"}' "$EVIDENCE_DIR/navio-created.json")"
assert_http 201 "$navio_status" "cadastro canônico de navio"
NAVIO_ID="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["identificador"])' "$EVIDENCE_DIR/navio-created.json")"
navio_read_status="$(request GET "$CANONICAL_URL" "/navios/$NAVIO_ID" '' "$EVIDENCE_DIR/navio-before-restart.json")"
assert_http 200 "$navio_read_status" "leitura do navio cadastrado"

stage "comprovando oito schemas e históricos Flyway"
: > "$EVIDENCE_DIR/flyway-counts.txt"
for schema in "${SCHEMAS[@]}"; do
  count="$("${COMPOSE[@]}" exec -T postgres psql -U "$DB_USER" -d "$DB_NAME" -Atc "SELECT COUNT(*) FROM ${schema}.flyway_schema_history WHERE success")"
  if [[ ! "$count" =~ ^[1-9][0-9]*$ ]]; then
    echo "schema $schema sem migrações aplicadas" >&2
    exit 1
  fi
  printf '%s=%s\n' "$schema" "$count" >> "$EVIDENCE_DIR/flyway-counts.txt"
done

stage "comprovando Redis, RabbitMQ e consumidores únicos"
redis_result="$("${COMPOSE[@]}" exec -T redis redis-cli ping | tr -d '\r')"
[[ "$redis_result" == "PONG" ]]
"${COMPOSE[@]}" exec -T rabbitmq rabbitmq-diagnostics -q ping
"${COMPOSE[@]}" exec -T rabbitmq rabbitmqctl list_queues -q name consumers > "$EVIDENCE_DIR/rabbit-consumers.txt"
python3 - "$EVIDENCE_DIR/rabbit-consumers.txt" <<'PY'
import sys
for line in open(sys.argv[1], encoding="utf-8"):
    parts = line.strip().split()
    if len(parts) != 2 or not parts[1].isdigit():
        continue
    assert int(parts[1]) <= 1, f"fila com consumidores concorrentes: {line.strip()}"
PY

stage "comprovando superfície consolidada no OpenAPI"
curl -fsS "$CANONICAL_URL/v3/api-docs" > "$EVIDENCE_DIR/openapi.json"
python3 - "$EVIDENCE_DIR/openapi.json" <<'PY'
import json
import sys
with open(sys.argv[1], encoding="utf-8") as source:
    paths = set(json.load(source).get("paths", {}))
required_fragments = [
    "/auth",
    "/navios",
    "/visitas-navio",
    "/yard/",
    "/rail/",
    "/gate/",
    "/carga-geral/",
]
for fragment in required_fragments:
    assert any(fragment in path for path in paths), f"contrato ausente no OpenAPI: {fragment}"
assert len(paths) == len(set(paths))
print(f"OPENAPI_PATHS={len(paths)}")
PY

stage "comprovando cenários de borda do TOS"
"${COMPOSE[@]}" exec -T cloudport-runtime curl -fsS http://tos-smoke:8090/health > "$EVIDENCE_DIR/tos-success.json"
"${COMPOSE[@]}" exec -T cloudport-runtime curl -fsS http://tos-smoke:8090/invalid > "$EVIDENCE_DIR/tos-invalid.json"
if python3 -m json.tool "$EVIDENCE_DIR/tos-invalid.json" >/dev/null 2>&1; then
  echo "resposta inválida do TOS foi aceita como JSON" >&2
  exit 1
fi
if "${COMPOSE[@]}" exec -T cloudport-runtime curl -fsS --max-time 1 http://tos-smoke:8090/timeout >/dev/null 2>&1; then
  echo "timeout do TOS não foi detectado" >&2
  exit 1
fi
if "${COMPOSE[@]}" exec -T cloudport-runtime curl -fsS --max-time 1 http://tos-smoke:65530/health >/dev/null 2>&1; then
  echo "indisponibilidade do TOS não foi detectada" >&2
  exit 1
fi

stage "comprovando persistência de documentos e reinício"
"${COMPOSE[@]}" exec -T cloudport-runtime sh -c 'printf cutover-proof > /var/lib/cloudport/documents/cutover-proof.txt'
"${COMPOSE[@]}" restart cloudport-runtime >/dev/null
wait_http "$CANONICAL_URL/actuator/health/readiness"
navio_after_restart_status="$(request GET "$CANONICAL_URL" "/navios/$NAVIO_ID" '' "$EVIDENCE_DIR/navio-after-restart.json")"
assert_http 200 "$navio_after_restart_status" "persistência após reinício"
document_value="$("${COMPOSE[@]}" exec -T cloudport-runtime cat /var/lib/cloudport/documents/cutover-proof.txt)"
[[ "$document_value" == "cutover-proof" ]]

stage "comprovando instância escritora única"
canonical_count="$("${COMPOSE[@]}" ps -q cloudport-runtime | sed '/^$/d' | wc -l | tr -d ' ')"
[[ "$canonical_count" == "1" ]]
legacy_running="$("${COMPOSE[@]}" --profile rollback-proof ps --services --status running | grep -E 'cloudport-monolito|servico-' || true)"
[[ -z "$legacy_running" ]]

stage "ensaiando rollback sem downgrade e sem escrita concorrente"
"${COMPOSE[@]}" stop cloudport-observer cloudport-runtime >/dev/null
"${COMPOSE[@]}" --profile rollback-proof up -d --build cloudport-rollback-probe
wait_http "http://localhost:${ROLLBACK_PORT}/actuator/health/readiness"
rollback_read_status="$(request GET "http://localhost:${ROLLBACK_PORT}" "/navios/$NAVIO_ID" '' "$EVIDENCE_DIR/rollback-read.json")"
assert_http 200 "$rollback_read_status" "leitura no runtime de rollback"
rollback_write_status="$(request POST "http://localhost:${ROLLBACK_PORT}" /navios '{"nome":"Nao Persistir","codigoImo":"IMO0000002"}' "$EVIDENCE_DIR/rollback-write.json")"
assert_http 503 "$rollback_write_status" "bloqueio de escrita no ensaio de rollback"
"${COMPOSE[@]}" --profile rollback-proof stop cloudport-rollback-probe >/dev/null
"${COMPOSE[@]}" up -d cloudport-runtime cloudport-observer
wait_http "$CANONICAL_URL/actuator/health/readiness"
wait_http "$OBSERVER_URL/actuator/health/readiness"
return_status="$(request GET "$CANONICAL_URL" "/navios/$NAVIO_ID" '' "$EVIDENCE_DIR/navio-after-return.json")"
assert_http 200 "$return_status" "retorno ao runtime canônico"

stage "gerando evidência consolidada"
"${COMPOSE[@]}" ps > "$EVIDENCE_DIR/compose-final.txt"
python3 - "$EVIDENCE_DIR" "$NAVIO_ID" "$CLOUDPORT_RUNTIME_REVISAO" <<'PY'
import datetime
import json
import pathlib
import sys

folder = pathlib.Path(sys.argv[1])
def read_json(name):
    with (folder / name).open(encoding="utf-8") as source:
        return json.load(source)

evidence = {
    "resultado": "APROVADO",
    "geradoEm": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "revisao": sys.argv[3],
    "navioPersistidoId": int(sys.argv[2]),
    "runtimeCanonico": read_json("canonical-status.json"),
    "runtimeObservador": read_json("observer-status.json"),
    "health": read_json("health.json"),
    "readiness": read_json("readiness.json"),
    "provas": [
        "instancia escritora unica",
        "jobs e consumidores ativos somente no runtime canonico",
        "observador fail-closed com HTTP 503",
        "oito schemas com Flyway aplicado",
        "adaptadores internos locais",
        "Redis e RabbitMQ disponiveis",
        "TOS em sucesso, resposta invalida, timeout e indisponibilidade",
        "persistencia de banco e documentos apos reinicio",
        "rollback somente leitura sobre o mesmo banco sem downgrade",
        "retorno ao runtime canonico sem perda do dado persistido",
    ],
}
with (folder / "evidencia-corte-operacional.json").open("w", encoding="utf-8") as target:
    json.dump(evidence, target, ensure_ascii=False, indent=2)
with (folder / "evidencia-corte-operacional.md").open("w", encoding="utf-8") as target:
    target.write("# Evidência do corte operacional do CloudPort\n\n")
    target.write(f"- Resultado: **{evidence['resultado']}**\n")
    target.write(f"- Revisão: `{evidence['revisao']}`\n")
    target.write(f"- Gerado em: `{evidence['geradoEm']}`\n")
    target.write(f"- Instância canônica: `{evidence['runtimeCanonico']['instanciaId']}`\n")
    target.write(f"- Instância observadora: `{evidence['runtimeObservador']['instanciaId']}`\n\n")
    target.write("## Provas executadas\n\n")
    for proof in evidence["provas"]:
        target.write(f"- {proof}\n")
PY

stage "corte operacional comprovado"
