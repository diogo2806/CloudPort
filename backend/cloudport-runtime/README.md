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

## Estrutura Maven

O build consolidado usa dois níveis distintos:

- `backend/cloudport-navio-modules/pom.xml`: parent Maven compartilhado, responsável por versões, `dependencyManagement`, `pluginManagement` e regras comuns;
- `backend/cloudport-modules/pom.xml`: reator canônico que agrega contratos, módulos de domínio e `cloudport-runtime`.

O diretório `cloudport-runtime` é o executável consolidado e não substitui o parent nem o reator. Por isso, o parent deve ser instalado antes de executar o build pelo reator.

## Build

```bash
cd backend/cloudport-modules
mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

## Execução

```bash
java -jar backend/cloudport-runtime/target/cloudport-runtime-*.jar
```

O ambiente deve fornecer:

- `DB_HOST`: host do PostgreSQL;
- `DB_PORT`: porta do PostgreSQL;
- `DB_NAME`: banco do CloudPort;
- `DB_USER`: usuário do banco;
- `DB_PASS`: senha do banco;
- `DB_SCHEMA`: search path dos schemas do runtime, separado por vírgulas;
- `SECURITY_JWT_SECRET`: segredo compartilhado pelo emissor e decoder, com pelo menos 32 bytes;
- `SECURITY_JWT_EXPIRATION_MS`: duração do token em milissegundos;
- `ADMIN_EMAIL`: login do administrador inicial;
- `ADMIN_PASSWORD`: senha do administrador inicial, entre 6 e 255 caracteres;
- `CLOUDPORT_PUBLIC_API_CLIENTS`: clientes opcionais da API pública no formato `cliente:segredo`, separados por vírgula, com cada segredo contendo pelo menos 32 bytes.

Quando `CLOUDPORT_PUBLIC_API_CLIENTS` não estiver configurada, o runtime inicia normalmente e mantém `/api/public/v1/**` bloqueado com HTTP 401. Uma configuração não vazia, porém inválida, continua impedindo a inicialização para evitar credenciais fracas ou malformadas.

Valor recomendado para `DB_SCHEMA`:

```text
cloudport_autenticacao,cloudport_carga_geral,cloudport_gate,cloudport_rail,cloudport_visibilidade,cloudport_yard,cloudport_navio,cloudport_siderurgico
```

O administrador configurado é criado somente quando ainda não existe. A migração remove apenas a antiga credencial insegura `gitpod/gitpod`; contas que já tiveram a senha alterada não são excluídas.

## Redis

Na imagem Docker do runtime, configure o Redis com estas variáveis:

```text
REDIS_USER=
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=
REDIS_URL=
```

`REDIS_URL` é opcional. Quando preenchida, a URL tem precedência sobre host, porta, usuário e senha separados. São aceitas URLs `redis://` e `rediss://`. Quando o provedor não usar ACL por usuário, `REDIS_USER` pode permanecer vazio.

Hosts com underscore (`_`), comuns em nomes de containers do Docker Compose como `pessoal_redis-guardiao`, não são interpretados pelo parser de URL do Java. Nesses casos o runtime decompõe automaticamente a `REDIS_URL` em host, porta e credenciais discretos antes de criar a conexão, evitando a falha `Host must not be empty`.

O entrypoint da imagem converte essas variáveis para as propriedades nativas do Spring Boot sem registrar os valores sensíveis nos logs.

## RabbitMQ

A mensageria é opcional e fica desabilitada por padrão no Docker Compose:

```text
RABBITMQ_ENABLED=false
```

Nesse modo, o runtime não valida conexão, não cria infraestrutura AMQP e o serviço `rabbitmq` não é iniciado. Para usar o RabbitMQ interno do Compose:

```bash
export RABBITMQ_ENABLED=true
export RABBITMQ_PASSWORD='substitua-a-senha-do-rabbitmq'

docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  --profile messaging \
  up -d --build
```

Para um provedor externo, não habilite o profile e informe `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD`. Quando a mensageria está habilitada, o entrypoint aguarda a porta TCP antes de iniciar a JVM. Os tempos são configuráveis por `RABBITMQ_STARTUP_ATTEMPTS`, `RABBITMQ_STARTUP_INTERVAL_SECONDS` e `RABBITMQ_STARTUP_TIMEOUT_SECONDS`.

Se `RABBITMQ_ENABLED=true` e o destino permanecer indisponível, o processo encerra com código `78` e uma mensagem indicando o uso de `--profile messaging` ou a correção de host e porta. Isso evita um runtime aparentemente ativo com mensageria configurada incorretamente.

As demais integrações continuam usando suas variáveis específicas, incluindo Redis, TOS, alertas e armazenamento.

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

Sem mensageria:

```bash
export RABBITMQ_ENABLED=false

docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

Com o RabbitMQ interno:

```bash
export RABBITMQ_ENABLED=true
export RABBITMQ_PASSWORD='substitua-a-senha-do-rabbitmq'

docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  --profile messaging \
  up -d --build
```

A porta pública padrão é `8080`.

O smoke dos dois modos pode ser executado com:

```bash
bash deploy/cloudport-runtime/provar-mensageria-opcional.sh
```

## Coexistência e rollback

O `cloudport-runtime` deve ser o único escritor, executor de jobs e consumidor ativo durante a operação consolidada. Deployments anteriores devem permanecer parados ou com escrita, jobs e consumidores desativados.

O retorno para `cloudport-monolito-navio` exige `CLOUDPORT_ROLLBACK_ENABLED=true` e execução conforme o runbook `docs/operacao-corte-rollback-navio.md`.
