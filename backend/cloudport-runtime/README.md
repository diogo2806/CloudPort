# Runtime geral do CloudPort

O projeto `cloudport-runtime` reúne, no mesmo processo Spring Boot, os módulos de Autenticação, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.

## Limites mantidos

Cada módulo continua responsável por suas entidades, repositories, contratos e migrações. O runtime centraliza inicialização, segurança, CORS, conexão PostgreSQL, Flyway, jobs e infraestrutura transversal.

TOS, OCR, EDI, RabbitMQ, Redis, armazenamento e outros sistemas externos permanecem adaptadores de borda. Não devem ser substituídos por dependências diretas entre entidades dos módulos.

## Integração Navio e Yard

No runtime geral, `cloudport.modulo.yard.integracao=local` desativa os clientes HTTP de ordens e posições do Yard. Os adaptadores locais chamam `OrdemTrabalhoPatioServico`, `WorkQueuePatioServico` e `MapaPatioServico` no mesmo processo, preservando os contratos REST externos.

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

## Configuração obrigatória

O ambiente deve fornecer:

- `SPRING_DATASOURCE_URL`;
- `SPRING_DATASOURCE_USERNAME`;
- `SPRING_DATASOURCE_PASSWORD`;
- `CLOUDPORT_SECURITY_JWT_SECRET`;
- `API_SECURITY_TOKEN_SECRET` com o mesmo valor do segredo JWT.

RabbitMQ e Redis continuam necessários para os adaptadores externos de eventos e projeções. Os deployments antigos devem permanecer com escrita, jobs e consumidores desativados durante a coexistência.
