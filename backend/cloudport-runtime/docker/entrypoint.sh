#!/bin/sh
set -eu

# Variaveis vazias sao consideradas valores validos pelo Spring e impedem o
# fallback definido em application.properties. Normaliza host e porta antes de
# iniciar o runtime para evitar a criacao do Lettuce com host vazio.
REDIS_EFFECTIVE_HOST="${SPRING_REDIS_HOST:-${REDIS_HOST:-localhost}}"
REDIS_EFFECTIVE_PORT="${SPRING_REDIS_PORT:-${REDIS_PORT:-6379}}"

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

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
