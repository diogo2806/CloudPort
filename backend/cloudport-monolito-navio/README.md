# Runtime monolítico CloudPort

O projeto `cloudport-monolito-navio` é o runtime único do monólito modular CloudPort. O nome é preservado temporariamente por compatibilidade com pipelines e rollback.

Ele carrega no mesmo processo Spring Boot os módulos:

- `servico-navio`;
- `servico-navio-siderurgico`;
- `servico-yard`;
- `servico-gate`;
- `servico-rail`;
- `servico-autenticacao`;
- `servico-visibilidade`.

A arquitetura está em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md) e o runbook em [`../../docs/operacao-corte-rollback-navio.md`](../../docs/operacao-corte-rollback-navio.md).

## Responsabilidades

- iniciar uma única aplicação Spring Boot;
- carregar componentes, entidades, repositories e controllers dos sete módulos;
- excluir executáveis e configurações transversais standalone;
- substituir integrações internas HTTP por portas locais;
- centralizar Maven, segurança, CORS, Jackson, erros, logs, métricas, tracing e agendamento;
- usar uma conexão PostgreSQL com schema e Flyway próprios por módulo;
- controlar escrita, jobs e consumidores durante coexistência;
- empacotar um único JAR e uma única imagem Docker.

## Portas locais

| Porta | Implementação no runtime |
| --- | --- |
| `CadastroNavioPorta` | `CadastroNavioLocalAdapter` |
| `OrdemPatioYardCliente` | `OrdemPatioLocalAdapter` |
| `PosicaoPatioYardCliente` | `PosicaoPatioLocalAdapter` |
| `ClienteStatusPatio` | `StatusPatioLocalAdapter` |
| `AutenticacaoClient` | `AutenticacaoLocalAdapter` |

Os adaptadores HTTP permanecem nos módulos somente para execução isolada e rollback. As propriedades `cloudport.modulo.navio.integracao`, `cloudport.modulo.yard.integracao` e `cloudport.modulo.autenticacao.integracao` ficam em `local` no runtime.

## Banco e Flyway

| Variável | Padrão |
| --- | --- |
| `MONOLITO_NAVIO_SCHEMA` | `cloudport_navio` |
| `MONOLITO_SIDERURGICO_SCHEMA` | `cloudport_siderurgico` |
| `MONOLITO_YARD_SCHEMA` | `cloudport_yard` |
| `MONOLITO_GATE_SCHEMA` | `cloudport_gate` |
| `MONOLITO_RAIL_SCHEMA` | `cloudport_rail` |
| `MONOLITO_AUTENTICACAO_SCHEMA` | `cloudport_autenticacao` |
| `MONOLITO_VISIBILIDADE_SCHEMA` | `cloudport_visibilidade` |

Cada módulo publica migrações em `classpath:cloudport/migrations/<modulo>` e mantém seu próprio `flyway_schema_history`. O auto-configurador Flyway padrão fica desabilitado porque o runtime cria sete instâncias explícitas.

Rollback normal troca aplicação e roteamento, sem downgrade de banco. Migrações durante a coexistência devem seguir `expand and contract`.

## Execução única

| Variável | Padrão | Uso |
| --- | --- | --- |
| `CLOUDPORT_WRITES_ENABLED` | `true` | permite comandos de escrita |
| `CLOUDPORT_JOBS_ENABLED` | `true` | habilita scheduler central |
| `CLOUDPORT_CONSUMERS_ENABLED` | `true` | inicia listeners RabbitMQ |

Nos deployments legados, os três controles devem permanecer `false`. Escritas desabilitadas retornam `503`. Jobs críticos usam também advisory lock PostgreSQL.

## Compilação e testes

Pré-requisitos: JDK 17, Maven 3.9+ e Docker para Testcontainers.

```bash
cd backend/cloudport-navio-modules
mvn -B -Pmodulo-monolito -pl :cloudport-monolito-navio -am test package
```

Os testes verificam:

- inicialização com PostgreSQL 16;
- sete schemas e históricos Flyway;
- validação JPA;
- uma única cadeia de segurança;
- portas locais e ausência dos adaptadores HTTP no contexto;
- exclusão mútua de jobs;
- ausência de ciclos;
- proibição de repository de outro módulo.

## Variáveis obrigatórias

| Variável | Descrição |
| --- | --- |
| `CLOUDPORT_DB_URL` ou `MONOLITO_NAVIO_DB_URL` | URL JDBC PostgreSQL |
| `CLOUDPORT_DB_USERNAME` ou `MONOLITO_NAVIO_DB_USERNAME` | usuário do banco |
| `CLOUDPORT_DB_PASSWORD` ou `MONOLITO_NAVIO_DB_PASSWORD` | senha do banco |
| `JWT_SECRET` | segredo HS256 com no mínimo 32 bytes |

## Infraestrutura

O runtime usa RabbitMQ e Redis centralizados:

```text
SPRING_RABBITMQ_HOST
SPRING_RABBITMQ_PORT
SPRING_RABBITMQ_USERNAME
SPRING_RABBITMQ_PASSWORD
SPRING_REDIS_HOST
SPRING_REDIS_PORT
```

CORS é configurado por `SECURITY_CORS_ALLOWED_ORIGINS`. `CLOUDPORT_INTERNAL_SERVICE_KEY` é transitório e serve apenas para deployments legados.

## Execução local

```bash
export CLOUDPORT_DB_URL='jdbc:postgresql://localhost:5432/cloudport'
export CLOUDPORT_DB_USERNAME='cloudport'
export CLOUDPORT_DB_PASSWORD='cloudport'
export JWT_SECRET='substitua-por-segredo-com-pelo-menos-32-bytes'
export SPRING_RABBITMQ_HOST='localhost'
export SPRING_REDIS_HOST='localhost'

java -jar target/cloudport-monolito-navio-*.jar
```

## Docker

A partir da raiz:

```bash
docker build \
  -f backend/cloudport-monolito-navio/Dockerfile \
  -t cloudport-monolito .
```

Ou:

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile monolito \
  up -d --build
```

## Regras de evolução

- não criar HTTP entre módulos incorporados;
- não acessar entidade ou repository de outro módulo;
- usar portas ou eventos internos;
- manter migrações no artefato proprietário;
- manter configurações transversais no runtime;
- conservar apenas um escritor, scheduler e grupo consumidor;
- manter contratos REST externos durante a migração;
- não remover deployments ou credenciais legadas antes da validação de paridade e rollback.
