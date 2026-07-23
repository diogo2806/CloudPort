#!/bin/sh
set -eu

CONTAINER_PRUNE_UNTIL="${DOCKER_CONTAINER_PRUNE_UNTIL:-24h}"
IMAGE_PRUNE_UNTIL="${DOCKER_IMAGE_PRUNE_UNTIL:-168h}"
BUILD_CACHE_MAX_USED_SPACE="${DOCKER_BUILD_CACHE_MAX_USED_SPACE:-6GB}"
BUILD_CACHE_MIN_FREE_SPACE="${DOCKER_BUILD_CACHE_MIN_FREE_SPACE:-5GB}"
BUILD_CACHE_RESERVED_SPACE="${DOCKER_BUILD_CACHE_RESERVED_SPACE:-1GB}"
BUILD_CACHE_FALLBACK_UNTIL="${DOCKER_BUILD_CACHE_FALLBACK_UNTIL:-24h}"
BUILDX_DISCOVERY_TIMEOUT="${DOCKER_BUILDX_DISCOVERY_TIMEOUT:-5s}"
LOCK_DIR="${DOCKER_CLEANUP_LOCK_DIR:-/tmp/cloudport-docker-cleanup.lock}"

log() {
    printf '%s %s\n' "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$*"
}

release_lock() {
    rmdir "$LOCK_DIR" 2>/dev/null || true
}

if [ "${CLOUDPORT_DOCKER_CLEANUP_DISABLED:-false}" = "true" ]; then
    log "Limpeza Docker desabilitada por configuração."
    exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
    log "ERRO: comando docker não encontrado."
    exit 1
fi

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    log "Outra limpeza Docker já está em execução; encerrando sem concorrência."
    exit 0
fi
trap release_lock EXIT HUP INT TERM

if ! docker info >/dev/null 2>&1; then
    log "ERRO: daemon Docker indisponível ou usuário sem permissão."
    exit 1
fi

log "Uso do Docker antes da limpeza:"
docker system df || true

log "Removendo contêineres encerrados há mais de ${CONTAINER_PRUNE_UNTIL}."
docker container prune --force --filter "until=${CONTAINER_PRUNE_UNTIL}"

log "Removendo imagens não utilizadas criadas há mais de ${IMAGE_PRUNE_UNTIL}."
# Imagens referenciadas por qualquer contêiner preservado não são removidas.
docker image prune --all --force --filter "until=${IMAGE_PRUNE_UNTIL}"

prune_buildx_builder() {
    builder="$1"

    if docker buildx prune --help 2>/dev/null | grep -q -- '--max-used-space'; then
        log "Limitando cache do builder ${builder} a ${BUILD_CACHE_MAX_USED_SPACE}, com ${BUILD_CACHE_MIN_FREE_SPACE} livres."
        docker buildx --builder "$builder" prune \
            --all \
            --force \
            --max-used-space "$BUILD_CACHE_MAX_USED_SPACE" \
            --min-free-space "$BUILD_CACHE_MIN_FREE_SPACE" \
            --reserved-space "$BUILD_CACHE_RESERVED_SPACE"
        return
    fi

    if docker buildx prune --help 2>/dev/null | grep -q -- '--keep-storage'; then
        log "Limitando cache do builder ${builder} a ${BUILD_CACHE_MAX_USED_SPACE} com opção compatível."
        docker buildx --builder "$builder" prune \
            --all \
            --force \
            --keep-storage "$BUILD_CACHE_MAX_USED_SPACE"
        return
    fi

    log "Buildx antigo detectado; removendo cache sem uso há mais de ${BUILD_CACHE_FALLBACK_UNTIL} no builder ${builder}."
    docker buildx --builder "$builder" prune \
        --all \
        --force \
        --filter "until=${BUILD_CACHE_FALLBACK_UNTIL}"
}

prune_legacy_builder() {
    if docker builder prune --help 2>/dev/null | grep -q -- '--keep-storage'; then
        log "Limitando cache do builder Docker padrão a ${BUILD_CACHE_MAX_USED_SPACE}."
        docker builder prune --all --force --keep-storage "$BUILD_CACHE_MAX_USED_SPACE"
        return
    fi

    log "Builder Docker antigo detectado; removendo cache sem uso há mais de ${BUILD_CACHE_FALLBACK_UNTIL}."
    docker builder prune --all --force --filter "until=${BUILD_CACHE_FALLBACK_UNTIL}"
}

if docker buildx version >/dev/null 2>&1; then
    builders="$(
        docker buildx ls \
            --timeout "$BUILDX_DISCOVERY_TIMEOUT" \
            --format '{{.Builder.Name}}' 2>/dev/null \
        | sed '/^[[:space:]]*$/d' \
        | sort -u \
        || true
    )"

    if [ -n "$builders" ]; then
        for builder in $builders; do
            prune_buildx_builder "$builder"
        done
    else
        log "Não foi possível enumerar builders Buildx; limpando o builder selecionado."
        if docker buildx prune --help 2>/dev/null | grep -q -- '--max-used-space'; then
            docker buildx prune \
                --all \
                --force \
                --max-used-space "$BUILD_CACHE_MAX_USED_SPACE" \
                --min-free-space "$BUILD_CACHE_MIN_FREE_SPACE" \
                --reserved-space "$BUILD_CACHE_RESERVED_SPACE"
        else
            prune_legacy_builder
        fi
    fi
else
    prune_legacy_builder
fi

log "Uso do Docker após a limpeza:"
docker system df || true
log "Limpeza concluída. Volumes e imagens em uso foram preservados."