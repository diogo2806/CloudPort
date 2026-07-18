#!/bin/sh
set -eu

if [ -n "${REDIS_URL:-}" ]; then
    export SPRING_REDIS_URL="${REDIS_URL}"
fi
if [ -n "${REDIS_USER:-}" ]; then
    export SPRING_REDIS_USERNAME="${REDIS_USER}"
fi
if [ -n "${REDIS_HOST:-}" ]; then
    export SPRING_REDIS_HOST="${REDIS_HOST}"
fi
if [ -n "${REDIS_PORT:-}" ]; then
    export SPRING_REDIS_PORT="${REDIS_PORT}"
fi
if [ -n "${REDIS_PASSWORD:-}" ]; then
    export SPRING_REDIS_PASSWORD="${REDIS_PASSWORD}"
fi

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
