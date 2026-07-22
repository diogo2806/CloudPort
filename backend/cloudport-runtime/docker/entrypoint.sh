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
JAVA_EFFECTIVE_MAX_RAM_PERCENTAGE="$(normalizar_variavel "${JAVA_MAX_RAM_PERCENTAGE:-}" "65.0")"
JAVA_EFFECTIVE_THREAD_STACK_SIZE="$(normalizar_variavel "${JAVA_THREAD_STACK_SIZE:-}" "512k")"
JAVA_EFFECTIVE_MAX_METASPACE_SIZE="$(normalizar_variavel "${JAVA_MAX_METASPACE_SIZE:-}" "384m")"
JAVA_EFFECTIVE_MAX_DIRECT_MEMORY_SIZE="$(normalizar_variavel "${JAVA_MAX_DIRECT_MEMORY_SIZE:-}" "256m")"

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
printf 'CloudPort runtime: JVM maxRAM=%s%% stack=%s metaspace=%s direct=%s\n' \
    "${JAVA_EFFECTIVE_MAX_RAM_PERCENTAGE}" \
    "${JAVA_EFFECTIVE_THREAD_STACK_SIZE}" \
    "${JAVA_EFFECTIVE_MAX_METASPACE_SIZE}" \
    "${JAVA_EFFECTIVE_MAX_DIRECT_MEMORY_SIZE}"
if [ -n "${REDIS_URL:-}" ]; then
    printf 'CloudPort runtime: REDIS_URL definida; a URL tem precedencia sobre host e porta\n'
fi

exec java \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:MaxRAMPercentage="${JAVA_EFFECTIVE_MAX_RAM_PERCENTAGE}" \
    -XX:MaxMetaspaceSize="${JAVA_EFFECTIVE_MAX_METASPACE_SIZE}" \
    -XX:MaxDirectMemorySize="${JAVA_EFFECTIVE_MAX_DIRECT_MEMORY_SIZE}" \
    -Xss"${JAVA_EFFECTIVE_THREAD_STACK_SIZE}" \
    -Dspring.redis.host="${REDIS_EFFECTIVE_HOST}" \
    -Dspring.redis.port="${REDIS_EFFECTIVE_PORT}" \
    -jar /app/app.jar
