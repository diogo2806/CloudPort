# Runtime canônico do CloudPort

O projeto `cloudport-runtime` é o ponto de entrada oficial do backend. Ele reúne, no mesmo processo Spring Boot, os módulos de Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.

O runtime anterior `backend/cloudport-monolito-navio` permanece somente como rollback intermediário e não deve ser usado em novos comandos de implantação principal.

## Funcionalidades incorporadas

O runtime publica, pela mesma origem de API:

- autenticação, navegação, papéis e usuários;
- carga geral, carga de projeto e break-bulk;
- Gate operacional, Gate visual, Billing e CAP;
- Rail operacional, composição gráfica e line-up ferroviário;
- Yard, Inventory Management, telemetria de CHE, reefers e allocations;
- visitas de navio, line-up, Vessel Planner, Quay Monitor e crane plan;
- Control Room, work queues, job lists, dispatch, transições e alarmes;
- Visibilidade, rastreamento, histórico e central global de alertas;
- BAPLIE, COPRAR, COARRI e VERMAS com auditoria e processamento assíncrono;
- API pública, SSE, WebSocket, métricas e health checks.

## Limites mantidos

Cada módulo continua responsável por suas entidades, repositories, contratos e migrações. O runtime centraliza inicialização, segurança, CORS, conexão PostgreSQL, Flyway, jobs e infraestrutura transversal.

TOS, OCR, EDI, RabbitMQ, Redis, armazenamento e outros sistemas externos permanecem adaptadores de borda. Não devem ser substituídos por dependências diretas entre entidades dos módulos.

## Integrações locais

No runtime geral, os modos locais desativam clientes HTTP entre módulos incorporados.

As principais portas locais cobrem:

- Navio Siderúrgico para cadastro canônico de Navio;
- Navio e Navio Siderúrgico para Yard;
- Gate para status e operações do Yard;
- Gate para Autenticação;
- otimização, aplicação e compensação de planos;
- work queues usadas pelo crane plan.

Os adaptadores HTTP permanecem condicionados às propriedades de integração e servem somente à borda externa ou ao rollback.

## Persistência

O runtime usa uma conexão PostgreSQL com schemas independentes:

- `cloudport_autenticacao`;
- `cloudport_carga_geral`;
- `cloudport_gate`;
- `cloudport_rail`;
- `cloudport_visibilidade`;
- `cloudport_yard`;
- `cloudport_navio`;
- `cloudport_siderurgico`.

Cada artefato fornece suas migrações. O build as publica em namespaces exclusivos e o runtime mantém um histórico Flyway por módulo.

O `search_path` inclui os oito schemas e `public`.

## Build

```bash
cd backend/cloudport-modules
mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

O reator inclui `cloudport-contracts`, todos os módulos operacionais e o runtime.

## Execução

```bash
java -jar backend/cloudport-runtime/target/cloudport-runtime-*.jar
```

## Variáveis obrigatórias

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cloudport
SPRING_DATASOURCE_USERNAME=cloudport
SPRING_DATASOURCE_PASSWORD=cloudport
CLOUDPORT_SECURITY_JWT_SECRET=segredo-com-pelo-menos-32-bytes
CLOUDPORT_PUBLIC_API_CLIENTS=cliente:segredo
```

A expiração do JWT usa `CLOUDPORT_SECURITY_JWT_EXPIRATION`, em formato ISO-8601. O padrão é `PT2H`.

## Infraestrutura externa

```bash
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
REDIS_HOST=localhost
REDIS_PORT=6379
```

As propriedades de RabbitMQ, Redis, integrações EDI, TOS, OCR e storage possuem variáveis específicas em `src/main/resources/application.properties`.

Para armazenamento local de documentos:

```bash
DOCUMENT_STORAGE_PROVIDER=local
DOCUMENT_STORAGE_BASE_PATH=/var/lib/cloudport/documents
```

Para entrega de alertas de reconciliação de barcode, configure `GATE_ALERTAS_WEBHOOK_URL` e, quando exigido pelo provedor, `GATE_ALERTAS_BEARER_TOKEN`. Sem URL configurada, a ocorrência permanece pendente e registra a falha de entrega para nova tentativa.

## Controles de execução

- `CLOUDPORT_WRITES_ENABLED`: habilita comandos persistentes;
- `CLOUDPORT_JOBS_ENABLED`: habilita jobs periódicos;
- consumidores RabbitMQ devem ficar ativos em apenas uma implantação durante coexistência;
- jobs críticos utilizam exclusão distribuída no PostgreSQL quando aplicável.

O runtime canônico deve ser o único escritor, executor de jobs e consumidor ativo no ambiente consolidado.

## Endpoints operacionais

- readiness: `/actuator/health/readiness`;
- liveness: `/actuator/health/liveness`;
- métricas: `/actuator/metrics`;
- Prometheus: `/actuator/prometheus`;
- OpenAPI: publicado pelo runtime consolidado conforme a configuração do Springdoc.

## EasyPanel

Use uma aplicação GitHub com:

- repositório: `diogo2806/CloudPort`;
- branch: `main`;
- caminho de build: `/backend`;
- arquivo: `Dockerfile`;
- porta da aplicação: `8080`;
- health check: `/actuator/health/readiness`.

O `backend/Dockerfile` foi criado para o contexto `/backend`. Ele:

1. instala o parent Maven necessário ao reator;
2. compila `cloudport-contracts`, módulos e runtime;
3. gera `/app/app.jar`;
4. executa com usuário sem privilégios;
5. prepara `/var/lib/cloudport/documents`;
6. aguarda até 120 segundos antes de considerar o health check inicial como falho.

Não use `backend/cloudport-runtime/Dockerfile` quando o caminho de build estiver definido como `/backend`. Esse arquivo é destinado a builds cujo contexto é a raiz do repositório.

## Docker Compose

A partir da raiz:

```bash
docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

A porta pública padrão é `8080`.

## Coexistência e rollback

Deployments anteriores devem permanecer parados ou com escrita, jobs e consumidores desativados.

O retorno para `cloudport-monolito-navio` exige `CLOUDPORT_ROLLBACK_ENABLED=true` e execução conforme `docs/operacao-corte-rollback-navio.md`.

O rollback troca binário e roteamento. Não deve executar downgrade automático do banco.
