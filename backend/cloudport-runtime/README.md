# Runtime canônico do CloudPort

O projeto `cloudport-runtime` é o ponto de entrada oficial do backend. Ele reúne, no mesmo processo Spring Boot, os módulos de Autenticação, Gate, Rail, Visibilidade, Yard, Navio, Navio Siderúrgico e Carga Geral.

O runtime anterior `backend/cloudport-monolito-navio` permanece somente como rollback intermediário e não deve ser usado em novos comandos de implantação principal.

## Limites mantidos

Cada módulo continua responsável por suas entidades, repositories, contratos e migrações. O runtime centraliza inicialização, segurança, CORS, conexão PostgreSQL, Flyway, jobs e infraestrutura transversal.

TOS, OCR, EDI, RabbitMQ, Redis, armazenamento e outros sistemas externos permanecem adaptadores de borda. Não devem ser substituídos por dependências diretas entre entidades dos módulos.

## Integração Navio e Yard

No runtime geral, `cloudport.modulo.yard.integracao=local` desativa os clientes HTTP internos. Os adaptadores locais chamam os serviços do Yard no mesmo processo, preservando os contratos REST externos.

As portas de otimização, aplicação e compensação de plano também possuem adaptadores locais no runtime.

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

Cada artefato fornece `db/migration`. O build extrai essas migrações para namespaces exclusivos no runtime e cria um histórico Flyway por módulo.

## Build

```bash
cd backend/cloudport-modules
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

## Execução

```bash
java -jar backend/cloudport-runtime/target/cloudport-runtime-*.jar
```

O ambiente deve fornecer, no mínimo:

- `SPRING_DATASOURCE_URL`;
- `SPRING_DATASOURCE_USERNAME`;
- `SPRING_DATASOURCE_PASSWORD`;
- `CLOUDPORT_SECURITY_JWT_SECRET`, compartilhado pelo emissor e pelo decoder e com pelo menos 32 bytes;
- `CLOUDPORT_SECURITY_JWT_EXPIRATION`, em formato ISO-8601, por exemplo `PT2H`;
- `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_USERNAME` e `SPRING_RABBITMQ_PASSWORD`;
- `SPRING_REDIS_HOST` e `SPRING_REDIS_PORT`.

Para entrega de alertas de reconciliação de barcode, configure `GATE_ALERTAS_WEBHOOK_URL` e, quando exigido pelo provedor, `GATE_ALERTAS_BEARER_TOKEN`. Sem URL configurada, a ocorrência permanece pendente e registra a falha de entrega para nova tentativa.

## EasyPanel

Use a aplicação do tipo GitHub com os seguintes valores:

- repositório: `diogo2806/CloudPort`;
- ramo: `main`;
- caminho de build: `/backend`;
- construção: `Dockerfile`;
- arquivo: `Dockerfile`;
- porta da aplicação: `8080`;
- verificação de saúde: `/actuator/health/readiness`.

O arquivo `backend/Dockerfile` foi criado especificamente para esse contexto. Não use `backend/cloudport-runtime/Dockerfile` quando o caminho de build estiver definido como `/backend`, pois esse segundo arquivo foi mantido para builds executados a partir da raiz do repositório.

## Docker Compose

A partir da raiz:

```bash
docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

A porta pública padrão é `8080`.

## Coexistência e rollback

O `cloudport-runtime` deve ser o único escritor, executor de jobs e consumidor ativo durante a operação consolidada. Deployments anteriores devem permanecer parados ou com escrita, jobs e consumidores desativados.

O retorno para `cloudport-monolito-navio` exige `CLOUDPORT_ROLLBACK_ENABLED=true` e execução conforme o runbook `docs/operacao-corte-rollback-navio.md`.
