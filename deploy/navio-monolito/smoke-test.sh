#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_COMPOSE="$ROOT_DIR/deploy/navio-monolito/docker-compose.yml"
SMOKE_COMPOSE="$ROOT_DIR/deploy/navio-monolito/docker-compose.smoke.yml"
PUBLIC_URL="${MONOLITO_SMOKE_URL:-http://localhost:8086}"
PROJECT_NAME="${MONOLITO_SMOKE_PROJECT:-cloudport-navio-smoke}"
COMPOSE_LOG="$(mktemp)"
STATUS_FILE="${SMOKE_STATUS_FILE:-/tmp/cloudport-smoke-status.txt}"
CURRENT_STAGE="inicializacao"

export SMOKE_JWT_SECRET="${SMOKE_JWT_SECRET:-$(openssl rand -hex 32)}"
export SMOKE_SERVICE_KEY="${SMOKE_SERVICE_KEY:-$(openssl rand -hex 24)}"

COMPOSE=(docker compose -p "$PROJECT_NAME" -f "$BASE_COMPOSE" -f "$SMOKE_COMPOSE" --profile monolito)

stage() {
    CURRENT_STAGE="$1"
    printf 'STAGE=%s\n' "$CURRENT_STAGE" > "$STATUS_FILE"
    echo "[smoke] $CURRENT_STAGE"
}

cleanup() {
    local exit_code=$?
    if [[ $exit_code -ne 0 ]]; then
        printf 'RESULT=failure\nSTAGE=%s\n' "$CURRENT_STAGE" > "$STATUS_FILE"
        echo "[smoke] falha no estágio: $CURRENT_STAGE" >&2
        tail -n 40 "$COMPOSE_LOG" >&2 || true
        "${COMPOSE[@]}" ps || true
    else
        printf 'RESULT=success\nSTAGE=%s\n' "$CURRENT_STAGE" > "$STATUS_FILE"
    fi
    "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
    exit "$exit_code"
}
trap cleanup EXIT

stage "limpando ambiente anterior"
"${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true

stage "construindo e iniciando ambiente"
if ! "${COMPOSE[@]}" up -d --build >"$COMPOSE_LOG" 2>&1; then
    exit 1
fi

stage "aguardando configuracao dinamica"
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
    exit 1
fi

stage "validando frontend React"
index_file="$(mktemp)"
curl -fsS "$PUBLIC_URL/" > "$index_file"
grep -q 'id="root"' "$index_file"

stage "validando configuracao de runtime"
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

stage "validando bloqueio sem autenticacao"
unauthorized_status="$(curl -sS -o /tmp/cloudport-smoke-unauthorized.json -w '%{http_code}' "$PUBLIC_URL/visitas-navio")"
if [[ "$unauthorized_status" != "401" ]]; then
    printf 'HTTP_STATUS=%s\n' "$unauthorized_status" >> "$STATUS_FILE"
    exit 1
fi

stage "gerando JWT temporario"
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
            -H 'X-Correlation-Id: cloudport-smoke-flow' \
            -H 'traceparent: 00-0123456789abcdef0123456789abcdef-0123456789abcdef-01' \
            --data "$body" \
            "$PUBLIC_URL$path"
    else
        curl -fsS -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H 'X-Correlation-Id: cloudport-smoke-flow' \
            -H 'traceparent: 00-0123456789abcdef0123456789abcdef-0123456789abcdef-01' \
            "$PUBLIC_URL$path"
    fi
}

stage "criando cadastro canonico"
canonical_response="$(request_json POST /navios '{"nome":"Navio Smoke","codigoImo":"IMO1234567","paisBandeira":"Brasil","empresaArmadora":"CloudPort","capacidadeTeu":1000,"loaMetros":200.50,"caladoMaximoMetros":12.40,"callSign":"SMOKE"}')"
canonical_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["identificador"])' <<< "$canonical_response")"

stage "criando extensao siderurgica"
siderurgico_response="$(request_json POST /navios-siderurgicos "{\"navioCadastroId\":$canonical_id,\"nome\":\"Navio Smoke\",\"codigoImo\":\"IMO1234567\",\"paisBandeira\":\"Brasil\",\"empresaArmadora\":\"CloudPort\",\"tipoNavio\":\"CARGUEIRO\",\"loaMetros\":200.50,\"dwtToneladas\":50000,\"quantidadePoroes\":5,\"status\":\"PLANEJADO\"}")"
siderurgico_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<< "$siderurgico_response")"

stage "criando visita"
visita_response="$(request_json POST /visitas-navio "{\"navioId\":$siderurgico_id,\"codigoVisita\":\"SMOKE-001\",\"fase\":\"PREVISTA\"}")"
visita_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<< "$visita_response")"

stage "validando integracao autenticada com o Yard"
work_queues_response="$(request_json GET "/visitas-navio/$visita_id/integracao-patio/work-queues")"
python3 -c 'import json,sys; assert json.load(sys.stdin) == []' <<< "$work_queues_response"

stage "criando item operacional de descarga"
item_response="$(request_json POST "/visitas-navio/$visita_id/itens" '{"tipoMovimento":"DESCARGA","codigoLote":"LOTE-SMOKE-001","produto":"BOBINA DE ACO","tipoCarga":"BOBINA","quantidade":1,"pesoUnitarioToneladas":12.500,"pesoTotalToneladas":12.500,"poraoPlanejado":1,"destinoPatio":"BLOCO-SMOKE","sequenciaOperacional":1,"status":"PLANEJADO"}')"
item_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<< "$item_response")"

stage "reservando posicao no mapa real"
reservas_response="$(request_json POST "/visitas-navio/$visita_id/integracao-patio/reservas" '{"tipoReserva":"DEFINITIVA","somentePendentes":true,"usuario":"cloudport-smoke"}')"
python3 - "$item_id" <<'PY' <<< "$reservas_response"
import json
import sys
reservas = json.load(sys.stdin)
item_id = int(sys.argv[1])
assert len(reservas) == 1
assert reservas[0]["itemOperacaoNavioId"] == item_id
assert reservas[0]["posicaoPatioId"] == "101"
assert reservas[0]["linha"] == 1
assert reservas[0]["coluna"] == 2
assert reservas[0]["camada"] == "3"
assert reservas[0]["status"] == "ATIVA"
PY

stage "gerando ordem real no Yard"
geracao_response="$(request_json POST "/visitas-navio/$visita_id/integracao-patio/gerar-ordens" '{"tipoMovimento":"DESCARGA","modo":"SOMENTE_PENDENTES","usuario":"cloudport-smoke","gerarReservasAutomaticas":false}')"
python3 - <<'PY' <<< "$geracao_response"
import json
import sys
resultado = json.load(sys.stdin)
assert resultado["totalOrdensCriadas"] == 1
assert resultado["totalItensComErro"] == 0
assert resultado["errosPorItem"] == []
PY

stage "validando work queue e job list por equipamento"
work_queues_response="$(request_json GET "/visitas-navio/$visita_id/integracao-patio/work-queues")"
python3 - "$item_id" <<'PY' <<< "$work_queues_response"
import json
import sys
filas = json.load(sys.stdin)
item_id = int(sys.argv[1])
assert len(filas) == 1
assert filas[0]["equipamento"] == "RTG-SMOKE-01"
assert filas[0]["status"] == "ATIVA"
assert len(filas[0]["jobList"]) == 1
assert filas[0]["jobList"][0]["itemOperacaoNavioId"] == item_id
PY

stage "sincronizando conclusao do Yard"
resumo_response="$(request_json POST "/visitas-navio/$visita_id/integracao-patio/sincronizar-status")"
python3 - <<'PY' <<< "$resumo_response"
import json
import sys
resumo = json.load(sys.stdin)
assert resumo["totalItens"] == 1
assert resumo["itensComOrdem"] == 1
assert resumo["ordensConcluidas"] == 1
assert resumo["statusPredominante"] == "SINCRONIZADO"
PY

stage "validando consumo da reserva e item operado"
itens_response="$(request_json GET "/visitas-navio/$visita_id/itens")"
reservas_response="$(request_json GET "/visitas-navio/$visita_id/integracao-patio/reservas")"
python3 - <<'PY' <<< "$itens_response"
import json
import sys
itens = json.load(sys.stdin)
assert len(itens) == 1
assert itens[0]["status"] == "OPERADO"
assert itens[0]["statusIntegracaoPatio"] == "SINCRONIZADO"
assert itens[0]["posicaoPatioReal"] == "1-2-3"
PY
python3 - <<'PY' <<< "$reservas_response"
import json
import sys
reservas = json.load(sys.stdin)
assert len(reservas) == 1
assert reservas[0]["status"] == "CONSUMIDA"
PY

stage "validando relatorio operacional integrado"
relatorio_response="$(request_json GET "/visitas-navio/$visita_id/relatorio-operacional-integrado")"
python3 - "$visita_id" <<'PY' <<< "$relatorio_response"
import json
import sys
relatorio = json.load(sys.stdin)
assert relatorio["visita"]["id"] == int(sys.argv[1])
assert len(relatorio["itens"]) == 1
assert len(relatorio["reservas"]) == 1
PY

stage "smoke test concluido"
