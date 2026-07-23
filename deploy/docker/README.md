# Retenção de imagens e cache Docker

Os builds do backend e do frontend usam BuildKit. Sem uma política de retenção, o servidor mantém imagens antigas, contêineres encerrados e cache de camadas até consumir todo o disco.

## Instalação no servidor

Execute a partir da raiz do repositório no servidor:

```bash
sudo sh deploy/docker/install-cleanup-systemd.sh
```

O instalador:

1. copia `cleanup-docker-storage.sh` para `/usr/local/sbin/cloudport-docker-cleanup`;
2. instala um serviço `systemd` do tipo `oneshot`;
3. instala e habilita um timer diário para 03:30, com atraso aleatório de até 30 minutos;
4. executa a limpeza imediatamente para recuperar espaço;
5. preserva volumes e imagens que ainda estejam referenciadas por contêineres.

## Política padrão

| Recurso | Retenção |
| --- | --- |
| Contêiner encerrado | 24 horas |
| Imagem sem uso | 7 dias |
| Cache máximo de build | 6 GB |
| Espaço livre desejado após o prune do Buildx | 5 GB |
| Cache mínimo reservado | 1 GB |

A janela de sete dias mantém imagens recentes disponíveis para rollback. Uma imagem usada por qualquer contêiner preservado não é removida pelo `docker image prune`.

O script não executa `docker volume prune`. Bancos, documentos e demais dados persistidos em volumes não entram na limpeza.

## Configuração

Os valores ficam em:

```text
/etc/default/cloudport-docker-cleanup
```

Configuração padrão:

```bash
DOCKER_CONTAINER_PRUNE_UNTIL=24h
DOCKER_IMAGE_PRUNE_UNTIL=168h
DOCKER_BUILD_CACHE_MAX_USED_SPACE=6GB
DOCKER_BUILD_CACHE_MIN_FREE_SPACE=5GB
DOCKER_BUILD_CACHE_RESERVED_SPACE=1GB
DOCKER_BUILD_CACHE_FALLBACK_UNTIL=24h
DOCKER_BUILDX_DISCOVERY_TIMEOUT=5s
```

Após alterar o arquivo, execute:

```bash
sudo systemctl start cloudport-docker-cleanup.service
```

Para desabilitar temporariamente a rotina sem remover o timer:

```bash
echo 'CLOUDPORT_DOCKER_CLEANUP_DISABLED=true' | sudo tee -a /etc/default/cloudport-docker-cleanup
```

Remova ou altere essa variável para `false` antes de reativar a limpeza.

## Execução e diagnóstico

Executar manualmente pelo serviço:

```bash
sudo systemctl start cloudport-docker-cleanup.service
```

Executar diretamente a partir do repositório:

```bash
sudo sh deploy/docker/cleanup-docker-storage.sh
```

Ver uso atual:

```bash
docker system df
```

Ver o próximo agendamento:

```bash
systemctl list-timers cloudport-docker-cleanup.timer
```

Ver os logs:

```bash
journalctl -u cloudport-docker-cleanup.service --no-pager
```

## Como a limpeza funciona

A rotina segue esta ordem:

1. impede duas execuções concorrentes por meio de um lock;
2. remove contêineres encerrados além da retenção configurada;
3. remove imagens antigas que não estejam ligadas a nenhum contêiner preservado;
4. enumera os builders Buildx e limita o cache de cada um;
5. limpa também o cache do builder integrado ao Docker Engine;
6. exibe `docker system df` antes e depois.

Falha em um builder Buildx remoto ou inativo não impede a limpeza dos demais builders e das imagens locais.