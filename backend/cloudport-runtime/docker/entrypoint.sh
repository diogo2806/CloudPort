#!/bin/sh
set -eu

normalizar_variavel() {
    valor="$1"
    valor_padrao="$2"
    valor="$(printf '%s' "$valor" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"

    if [ -n "$valor" ]; then
        printf '%s' "$valor"
        return
    fi

    printf '%s' "$valor_padrao"
}

# Variaveis vazias ou compostas somente por espacos sao consideradas valores
# validos pelo Spring e impedem o fallback definido em application.properties.
REDIS_EFFECTIVE_HOST="$(normalizar_variavel "${SPRING_REDIS_HOST:-${REDIS_HOST:-}}" "localhost")"
REDIS_EFFECTIVE_PORT="$(normalizar_variavel "${SPRING_REDIS_PORT:-${REDIS_PORT:-}}" "6379")"

export REDIS_HOST="${REDIS_EFFECTIVE_HOST}"
export REDIS_PORT="${REDIS_EFFECTIVE_PORT}"
export SPRING_REDIS_HOST="${REDIS_EFFECTIVE_HOST}"
export SPRING_REDIS_PORT="${REDIS_EFFECTIVE_PORT}"

if [ -n "${REDIS_URL:-}" ]; then
    export SPRING_REDIS_URL="${REDIS_URL}"
fi
if [ -n "${REDIS_USER:-}" ]; then
    export SPRING_REDIS_USERNAME="${REDIS_USER}"
fi
if [ -n "${REDIS_PASSWORD:-}" ]; then
    export SPRING_REDIS_PASSWORD="${REDIS_PASSWORD}"
fi

printf 'CloudPort runtime: Redis configurado em %s:%s\n' \
    "${REDIS_EFFECTIVE_HOST}" "${REDIS_EFFECTIVE_PORT}"

exec java \
    -XX:MaxRAMPercentage=75.0 \
    -Dspring.redis.host="${REDIS_EFFECTIVE_HOST}" \
    -Dspring.redis.port="${REDIS_EFFECTIVE_PORT}" \
    -jar /app/app.jar
