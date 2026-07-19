#!/bin/sh
set -eu

CONFIG_FILE="/usr/share/nginx/html/assets/configuracao.json"
API_URL="${CLOUDPORT_API_URL:-}"
NAVIO_CONTROL_ROOM_URL="${CLOUDPORT_NAVIO_CONTROL_ROOM_URL:-}"

is_valid_http_url() {
  case "$1" in
    http://*|https://*) return 0 ;;
    *) return 1 ;;
  esac
}

is_loopback_config() {
  grep -Eq '"baseApiUrl"[[:space:]]*:[[:space:]]*"https?://(localhost|127\.0\.0\.1|\[::1\])([:/"]|$)' "$CONFIG_FILE"
}

escape_sed_replacement() {
  printf '%s' "$1" | sed 's/[&|]/\\&/g'
}

if [ ! -f "$CONFIG_FILE" ]; then
  echo "ERRO: arquivo de configuração do frontend não encontrado: $CONFIG_FILE" >&2
  exit 1
fi

if [ -z "$API_URL" ]; then
  if is_loopback_config; then
    echo "ERRO: CLOUDPORT_API_URL não foi definida e configuracao.json aponta para localhost." >&2
    echo "Defina CLOUDPORT_API_URL com a origem pública HTTPS do cloudport-runtime." >&2
    exit 1
  fi

  echo "CloudPort: mantendo baseApiUrl existente em configuracao.json."
  exit 0
fi

if ! is_valid_http_url "$API_URL"; then
  echo "ERRO: CLOUDPORT_API_URL deve começar com http:// ou https://." >&2
  exit 1
fi

if [ -n "$NAVIO_CONTROL_ROOM_URL" ] && ! is_valid_http_url "$NAVIO_CONTROL_ROOM_URL"; then
  echo "ERRO: CLOUDPORT_NAVIO_CONTROL_ROOM_URL deve começar com http:// ou https://." >&2
  exit 1
fi

API_URL="${API_URL%/}"
NAVIO_CONTROL_ROOM_URL="${NAVIO_CONTROL_ROOM_URL%/}"
API_URL_ESCAPED="$(escape_sed_replacement "$API_URL")"
NAVIO_URL_ESCAPED="$(escape_sed_replacement "$NAVIO_CONTROL_ROOM_URL")"
TEMP_FILE="$(mktemp)"
trap 'rm -f "$TEMP_FILE"' EXIT

sed \
  -e "s|\"baseApiUrl\"[[:space:]]*:[[:space:]]*\"[^\"]*\"|\"baseApiUrl\": \"$API_URL_ESCAPED\"|" \
  -e "s|\"navioControlRoomUrl\"[[:space:]]*:[[:space:]]*\"[^\"]*\"|\"navioControlRoomUrl\": \"$NAVIO_URL_ESCAPED\"|" \
  "$CONFIG_FILE" > "$TEMP_FILE"

if ! grep -Fq "\"baseApiUrl\": \"$API_URL\"" "$TEMP_FILE"; then
  echo "ERRO: não foi possível atualizar baseApiUrl em configuracao.json." >&2
  exit 1
fi

mv "$TEMP_FILE" "$CONFIG_FILE"
trap - EXIT

echo "CloudPort: configuração de runtime atualizada para $API_URL."
