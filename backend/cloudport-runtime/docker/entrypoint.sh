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

normalizar_booleano() {
    nome="$1"
    valor="$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"

    case "$valor" in
        true|1|yes|on)
            printf 'true'
            ;;
        false|0|no|off|'')
            printf 'false'
            ;;
        *)
            printf 'CloudPort runtime: valor inválido para %s: %s\n' "$nome" "$2" >&2
            exit 64
            ;;
    esac
}

normalizar_inteiro_positivo() {
    valor="$1"
    valor_padrao="$2"

    case "$valor" in
        ''|*[!0-9]*|0)
            printf '%s' "$valor_padrao"
            ;;
        *)
            printf '%s' "$valor"
            ;;
    esac
}

aguardar_rabbitmq() {
    tentativa=1

    while [ "$tentativa" -le "$RABBIT_EFFECTIVE_STARTUP_ATTEMPTS" ]; do
        if nc -z -w "$RABBIT_EFFECTIVE_STARTUP_TIMEOUT_SECONDS" \
            "$RABBIT_EFFECTIVE_HOST" "$RABBIT_EFFECTIVE_PORT" >/dev/null 2>&1; then
            printf 'CloudPort runtime: RabbitMQ disponível em %s:%s\n' \
                "$RABBIT_EFFECTIVE_HOST" "$RABBIT_EFFECTIVE_PORT"
            return 0
        fi

        if [ "$tentativa" -lt "$RABBIT_EFFECTIVE_STARTUP_ATTEMPTS" ]; then
            sleep "$RABBIT_EFFECTIVE_STARTUP_INTERVAL_SECONDS"
        fi
        tentativa=$((tentativa + 1))
    done

    printf '%s\n' \
        "CloudPort runtime: RABBITMQ_ENABLED=true, mas RabbitMQ não está acessível em ${RABBIT_EFFECTIVE_HOST}:${RABBIT_EFFECTIVE_PORT}." \
        "Inicie o Docker Compose com --profile messaging ou corrija RABBITMQ_HOST/RABBITMQ_PORT." >&2
    exit 78
}

# Variaveis vazias ou compostas somente por espacos sao consideradas valores
# validos pelo Spring e impedem o fallback definido em application.properties.
REDIS_EFFECTIVE_HOST="$(normalizar_variavel "${SPRING_REDIS_HOST:-${REDIS_HOST:-}}" "localhost")"
REDIS_EFFECTIVE_PORT="$(normalizar_variavel "${SPRING_REDIS_PORT:-${REDIS_PORT:-}}" "6379")"
RABBIT_EFFECTIVE_ENABLED="$(normalizar_booleano "RABBITMQ_ENABLED" "${RABBITMQ_ENABLED:-false}")"
RABBIT_EFFECTIVE_HOST="$(normalizar_variavel "${SPRING_RABBITMQ_HOST:-${RABBITMQ_HOST:-}}" "localhost")"
RABBIT_EFFECTIVE_PORT="$(normalizar_variavel "${SPRING_RABBITMQ_PORT:-${RABBITMQ_PORT:-}}" "5672")"
RABBIT_EFFECTIVE_STARTUP_ATTEMPTS="$(normalizar_inteiro_positivo "${RABBITMQ_STARTUP_ATTEMPTS:-}" "30")"
RABBIT_EFFECTIVE_STARTUP_INTERVAL_SECONDS="$(normalizar_inteiro_positivo "${RABBITMQ_STARTUP_INTERVAL_SECONDS:-}" "2")"
RABBIT_EFFECTIVE_STARTUP_TIMEOUT_SECONDS="$(normalizar_inteiro_positivo "${RABBITMQ_STARTUP_TIMEOUT_SECONDS:-}" "2")"
JAVA_EFFECTIVE_MAX_RAM_PERCENTAGE="$(normalizar_variavel "${JAVA_MAX_RAM_PERCENTAGE:-}" "65.0")"
JAVA_EFFECTIVE_THREAD_STACK_SIZE="$(normalizar_variavel "${JAVA_THREAD_STACK_SIZE:-}" "512k")"
JAVA_EFFECTIVE_MAX_METASPACE_SIZE="$(normalizar_variavel "${JAVA_MAX_METASPACE_SIZE:-}" "384m")"
JAVA_EFFECTIVE_MAX_DIRECT_MEMORY_SIZE="$(normalizar_variavel "${JAVA_MAX_DIRECT_MEMORY_SIZE:-}" "256m")"

export REDIS_HOST="${REDIS_EFFECTIVE_HOST}"
export REDIS_PORT="${REDIS_EFFECTIVE_PORT}"
export SPRING_REDIS_HOST="${REDIS_EFFECTIVE_HOST}"
export SPRING_REDIS_PORT="${REDIS_EFFECTIVE_PORT}"
export RABBITMQ_ENABLED="${RABBIT_EFFECTIVE_ENABLED}"
export RABBITMQ_HOST="${RABBIT_EFFECTIVE_HOST}"
export RABBITMQ_PORT="${RABBIT_EFFECTIVE_PORT}"
export SPRING_RABBITMQ_HOST="${RABBIT_EFFECTIVE_HOST}"
export SPRING_RABBITMQ_PORT="${RABBIT_EFFECTIVE_PORT}"

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

if [ "$RABBIT_EFFECTIVE_ENABLED" = "true" ]; then
    aguardar_rabbitmq
else
    printf 'CloudPort runtime: RabbitMQ desabilitado; conexão, listeners e infraestrutura AMQP não serão iniciados\n'
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
