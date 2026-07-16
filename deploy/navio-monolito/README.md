# Deploy de rollback do runtime anterior

Este diretório preserva o `cloudport-monolito-navio` exclusivamente para rollback intermediário.

A implantação principal usa `deploy/cloudport-runtime/docker-compose.yml`. Não use este Compose para iniciar a operação normal do CloudPort.

## Iniciar o rollback intermediário

Antes de executar, retire o `cloudport-runtime` do proxy, pare o processo canônico e confirme que não existem jobs ou mensagens em processamento.

A partir da raiz:

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile rollback \
  up -d --build
```

O perfil ativa `CLOUDPORT_ROLLBACK_ENABLED=true` e inicia o runtime anterior na porta `8086`.

Os controles podem ser definidos explicitamente:

```bash
ROLLBACK_WRITES_ENABLED=true \
ROLLBACK_JOBS_ENABLED=true \
ROLLBACK_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile rollback \
  up -d --build
```

Nunca mantenha escrita, jobs ou consumidores ativos simultaneamente no `cloudport-runtime` e no runtime de rollback.

## Portas internas

O rollback monolítico usa integrações locais para Navio, Yard e Autenticação. As portas de otimização e aplicação de plano do Yard também possuem implementações locais, portanto o executável não inicia com dependências obrigatórias sem bean.

## Smoke automatizado do rollback

```bash
bash deploy/navio-monolito/smoke-test.sh
```

O smoke cria um ambiente descartável e valida inicialização, frontend incorporado, autenticação, persistência, integração local com o Yard e schemas Flyway.

## Retorno para serviços isolados

O perfil `legado` mantém os executáveis isolados de Navio e Navio Siderúrgico para um retorno adicional:

```bash
LEGACY_NAVIO_WRITES_ENABLED=true \
LEGACY_NAVIO_JOBS_ENABLED=true \
LEGACY_NAVIO_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile legado \
  up -d --build servico-navio servico-navio-siderurgico
```

Os demais deployments legados continuam nos manifests próprios de cada ambiente.

Detalhes: [`../../docs/operacao-corte-rollback-navio.md`](../../docs/operacao-corte-rollback-navio.md).
