#!/bin/sh
set -eu

if [ "$(id -u)" -ne 0 ]; then
    echo "ERRO: execute este instalador como root (sudo)." >&2
    exit 1
fi

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
CLEANUP_SOURCE="${SCRIPT_DIR}/cleanup-docker-storage.sh"
SERVICE_SOURCE="${SCRIPT_DIR}/systemd/cloudport-docker-cleanup.service"
TIMER_SOURCE="${SCRIPT_DIR}/systemd/cloudport-docker-cleanup.timer"
ENV_FILE="/etc/default/cloudport-docker-cleanup"

for required_file in "$CLEANUP_SOURCE" "$SERVICE_SOURCE" "$TIMER_SOURCE"; do
    if [ ! -f "$required_file" ]; then
        echo "ERRO: arquivo obrigatório não encontrado: ${required_file}" >&2
        exit 1
    fi
done

if ! command -v docker >/dev/null 2>&1; then
    echo "ERRO: Docker não está instalado no servidor." >&2
    exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
    echo "ERRO: systemd não está disponível no servidor." >&2
    exit 1
fi

install -m 0755 "$CLEANUP_SOURCE" /usr/local/sbin/cloudport-docker-cleanup
install -m 0644 "$SERVICE_SOURCE" /etc/systemd/system/cloudport-docker-cleanup.service
install -m 0644 "$TIMER_SOURCE" /etc/systemd/system/cloudport-docker-cleanup.timer

if [ ! -f "$ENV_FILE" ]; then
    cat > "$ENV_FILE" <<'EOF'
# Contêineres encerrados são preservados por 24 horas.
DOCKER_CONTAINER_PRUNE_UNTIL=24h

# Imagens sem uso são preservadas por sete dias para permitir rollback recente.
DOCKER_IMAGE_PRUNE_UNTIL=168h

# Política de retenção do cache BuildKit/Buildx.
DOCKER_BUILD_CACHE_MAX_USED_SPACE=6GB
DOCKER_BUILD_CACHE_MIN_FREE_SPACE=5GB
DOCKER_BUILD_CACHE_RESERVED_SPACE=1GB
DOCKER_BUILD_CACHE_FALLBACK_UNTIL=24h
DOCKER_BUILDX_DISCOVERY_TIMEOUT=5s
EOF
    chmod 0644 "$ENV_FILE"
fi

systemctl daemon-reload
systemctl enable --now cloudport-docker-cleanup.timer

# Executa imediatamente para recuperar espaço no servidor já saturado.
systemctl start cloudport-docker-cleanup.service

systemctl --no-pager --full status cloudport-docker-cleanup.timer || true
systemctl --no-pager --full status cloudport-docker-cleanup.service || true

echo "Limpeza automática instalada. Consulte os logs com:"
echo "  journalctl -u cloudport-docker-cleanup.service --no-pager"