#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${CLOUDPORT_BASE_URL:-http://localhost:8080}"
TOKEN="${CLOUDPORT_TOKEN:-}"

if [[ -z "$TOKEN" ]]; then
  echo "CLOUDPORT_TOKEN é obrigatório" >&2
  exit 2
fi

OPENAPI_FILE="$(mktemp)"
RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$OPENAPI_FILE" "$RESPONSE_FILE"' EXIT

curl -fsS "$BASE_URL/v3/api-docs" -o "$OPENAPI_FILE"
python3 - "$OPENAPI_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as source:
    paths = json.load(source).get("paths", {})

required = "/api/scheduler/planos-posicao"
if required not in paths:
    raise SystemExit(f"rota ausente no OpenAPI: {required}")
PY

status="$(curl -sS -o "$RESPONSE_FILE" -w '%{http_code}' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Accept: application/json' \
  "$BASE_URL/api/scheduler/planos-posicao")"

if [[ "$status" != "200" ]]; then
  echo "GET /api/scheduler/planos-posicao retornou HTTP $status" >&2
  cat "$RESPONSE_FILE" >&2
  exit 1
fi

python3 - "$RESPONSE_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as source:
    payload = json.load(source)

if not isinstance(payload, list):
    raise SystemExit("resposta de planos-posicao não é uma lista JSON")
PY

echo "rota /api/scheduler/planos-posicao publicada e operacional"
