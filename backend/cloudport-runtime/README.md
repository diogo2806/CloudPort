# Runtime canônico do CloudPort

O projeto `cloudport-runtime` é o ponto de entrada oficial do backend. Ele reúne, no mesmo processo Spring Boot, os módulos de Autenticação, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.

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
- `CLOUDPORT_SECURITY_JWT_SECRET`;
- `API_SECURITY_TOKEN_SECRET` com o mesmo valor do segredo JWT;
- configuração de RabbitMQ e Redis.

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
