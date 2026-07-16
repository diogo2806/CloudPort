#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_COMPOSE="$ROOT_DIR/deploy/navio-monolito/docker-compose.yml"
SMOKE_COMPOSE="$ROOT_DIR/deploy/navio-monolito/docker-compose.smoke.yml"
PUBLIC_URL="${MONOLITO_SMOKE_URL:-http://localhost:8086}"
PROJECT_NAME="${MONOLITO_SMOKE_PROJECT:-cloudport-navio-smoke}"

export SMOKE_JWT_SECRET="${SMOKE_JWT_SECRET:-$(openssl rand -hex 32)}"
export SMOKE_SERVICE_KEY="${SMOKE_SERVICE_KEY:-$(openssl rand -hex 24)}"

COMPOSE=(docker compose -p "$PROJECT_NAME" -f "$BASE_COMPOSE" -f "$SMOKE_COMPOSE" --profile monolito)

cleanup() {
    local exit_code=$?
    if [[ $exit_code -ne 0 ]]; then
        "${COMPOSE[@]}" ps || true
        "${COMPOSE[@]}" logs --no-color || true
    fi
    "${COMPOSE[@]}" down -v --remove-orphans || true
    exit "$exit_code"
}
trap cleanup EXIT

"${COMPOSE[@]}" down -v --remove-orphans || true
"${COMPOSE[@]}" up -d --build

config_file="$(mktemp)"
ready=false
for _ in $(seq 1 120); do
    if curl -fsS "$PUBLIC_URL/assets/configuracao.json" > "$config_file"; then
        if python3 - "$config_file" <<'PY'
import json
import sys
with open(sys.argv[1], encoding="utf-8") as response:
    payload = json.load(response)
if "baseApiUrl" not in payload or "trustedParentOrigins" not in payload:
    raise SystemExit(1)
PY
        then
            ready=true
            break
        fi
    fi
    sleep 2
done

if [[ "$ready" != "true" ]]; then
    echo "Runtime unificado nao ficou pronto no prazo." >&2
    exit 1
fi

index_file="$(mktemp)"
curl -fsS "$PUBLIC_URL/" > "$index_file"
grep -q 'id="root"' "$index_file"

python3 - "$config_file" <<'PY'
import json
import sys
with open(sys.argv[1], encoding="utf-8") as response:
    payload = json.load(response)
if payload.get("baseApiUrl") != "":
    raise SystemExit("baseApiUrl deve usar a mesma origem no smoke test")
if payload.get("trustedParentOrigins") != ["http://portal-smoke.local"]:
    raise SystemExit("origens confiaveis divergentes")
PY

unauthorized_status="$(curl -sS -o /tmp/cloudport-smoke-unauthorized.json -w '%{http_code}' "$PUBLIC_URL/visitas-navio")"
if [[ "$unauthorized_status" != "401" ]]; then
    echo "API sem autenticacao deveria responder 401, mas respondeu $unauthorized_status." >&2
    exit 1
fi

TOKEN="$(python3 - <<'PY'
import base64
import hashlib
import hmac
import json
import os
import time

def encode(value: dict) -> str:
    raw = json.dumps(value, separators=(",", ":")).encode("utf-8")
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")

header = encode({"alg": "HS256", "typ": "JWT"})
now = int(time.time())
payload = encode({
    "sub": "cloudport-smoke",
    "roles": ["PLANEJADOR"],
    "iat": now,
    "exp": now + 600,
})
signing_input = f"{header}.{payload}".encode("ascii")
signature = hmac.new(
    os.environ["SMOKE_JWT_SECRET"].encode("utf-8"),
    signing_input,
    hashlib.sha256,
).digest()
encoded_signature = base64.urlsafe_b64encode(signature).rstrip(b"=").decode("ascii")
print(f"{header}.{payload}.{encoded_signature}")
PY
)"

request_json() {
    local method=$1
    local path=$2
    local body=${3:-}
    if [[ -n "$body" ]]; then
        curl -fsS -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H 'Content-Type: application/json' \
            --data "$body" \
            "$PUBLIC_URL$path"
    else
        curl -fsS -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            "$PUBLIC_URL$path"
    fi
}

canonical_response="$(request_json POST /navios '{"nome":"Navio Smoke","codigoImo":"IMO1234567","paisBandeira":"Brasil","empresaArmadora":"CloudPort","capacidadeTeu":1000,"loaMetros":200.50,"caladoMaximoMetros":12.40,"callSign":"SMOKE"}')"
canonical_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["identificador"])' <<< "$canonical_response")"

siderurgico_response="$(request_json POST /navios-siderurgicos "{\"navioCadastroId\":$canonical_id,\"nome\":\"Navio Smoke\",\"codigoImo\":\"IMO1234567\",\"paisBandeira\":\"Brasil\",\"empresaArmadora\":\"CloudPort\",\"tipoNavio\":\"CARGUEIRO\",\"loaMetros\":200.50,\"dwtToneladas\":50000,\"quantidadePoroes\":5,\"status\":\"PLANEJADO\"}")"
siderurgico_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<< "$siderurgico_response")"

visita_response="$(request_json POST /visitas-navio "{\"navioId\":$siderurgico_id,\"codigoVisita\":\"SMOKE-001\",\"fase\":\"PREVISTA\"}")"
visita_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<< "$visita_response")"

work_queues_response="$(request_json GET "/visitas-navio/$visita_id/integracao-patio/work-queues")"
python3 -c 'import json,sys; assert json.load(sys.stdin) == []' <<< "$work_queues_response"

echo "Smoke test do runtime unificado concluido com sucesso."
