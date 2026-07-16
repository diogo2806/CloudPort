# Runtime anterior de rollback do CloudPort

O projeto `cloudport-monolito-navio` preserva o primeiro corte do monólito modular exclusivamente para rollback intermediário.

O ponto de entrada canônico é `backend/cloudport-runtime`. Não use este projeto em novos comandos de build, execução ou implantação principal.

## Proteção de inicialização

A aplicação só conclui a inicialização quando a propriedade abaixo é ativada explicitamente:

```text
CLOUDPORT_ROLLBACK_ENABLED=true
```

Escrita, jobs e consumidores ficam desativados por padrão. Em um rollback real, cada controle deve ser habilitado somente depois que o `cloudport-runtime` estiver parado e fora do roteamento.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `CLOUDPORT_WRITES_ENABLED` | `false` | permite comandos de escrita |
| `CLOUDPORT_JOBS_ENABLED` | `false` | habilita jobs agendados |
| `CLOUDPORT_CONSUMERS_ENABLED` | `false` | inicia listeners RabbitMQ |

## Módulos carregados

O executável incorpora:

- `servico-navio`;
- `servico-navio-siderurgico`;
- `servico-yard`;
- `servico-gate`;
- `servico-rail`;
- `servico-autenticacao`;
- `servico-visibilidade`.

A arquitetura está em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md) e o procedimento de retorno em [`../../docs/operacao-corte-rollback-navio.md`](../../docs/operacao-corte-rollback-navio.md).

## Portas locais do rollback

O modo `cloudport.modulo.yard.integracao=local` possui implementações para todas as dependências obrigatórias carregadas pelo Navio Siderúrgico.

| Porta | Implementação no rollback |
| --- | --- |
| `CadastroNavioPorta` | `CadastroNavioLocalAdapter` |
| `OrdemPatioYardCliente` | `OrdemPatioLocalAdapter` |
| `PosicaoPatioYardCliente` | `PosicaoPatioLocalAdapter` |
| `OtimizacaoYardCliente` | `OtimizacaoYardLocalAdapter` |
| `PlanoOtimizadoYardCliente` | `PlanoOtimizadoYardLocalAdapter` |
| `ClienteStatusPatio` | `StatusPatioLocalAdapter` |
| `AutenticacaoClient` | `AutenticacaoLocalAdapter` |

Os adaptadores HTTP permanecem condicionados ao modo `http` para deployments isolados. O rollback monolítico usa as portas locais e não depende de beans ausentes.

## Banco e Flyway

O executável usa uma conexão PostgreSQL e preserva um schema e um histórico Flyway por módulo. O retorno troca aplicação e roteamento, sem downgrade de banco. Migrações durante a janela de retorno devem seguir `expand and contract`.

## Compilação de validação

Este build existe para comprovar que o artefato de rollback continua utilizável. O build principal permanece no `cloudport-runtime`.

```bash
cd backend/cloudport-navio-modules
mvn -B -Pmodulo-monolito -pl :cloudport-monolito-navio -am test package
```

## Execução direta de rollback

```bash
export CLOUDPORT_ROLLBACK_ENABLED=true
export CLOUDPORT_WRITES_ENABLED=true
export CLOUDPORT_JOBS_ENABLED=true
export CLOUDPORT_CONSUMERS_ENABLED=true
export CLOUDPORT_DB_URL='jdbc:postgresql://localhost:5432/cloudport'
export CLOUDPORT_DB_USERNAME='cloudport'
export CLOUDPORT_DB_PASSWORD='cloudport'
export JWT_SECRET='substitua-por-segredo-com-pelo-menos-32-bytes'
export SPRING_RABBITMQ_HOST='localhost'
export SPRING_REDIS_HOST='localhost'

java -jar target/cloudport-monolito-navio-*.jar
```

Antes de habilitar os três controles, confirme que o runtime canônico está parado e que o proxy não envia tráfego para ele.

## Docker Compose de rollback

A partir da raiz:

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile rollback \
  up -d --build
```

O perfil `rollback` ativa explicitamente o modo de retorno. O perfil `legado` mantém os executáveis isolados de Navio e Navio Siderúrgico para um retorno adicional quando necessário.
