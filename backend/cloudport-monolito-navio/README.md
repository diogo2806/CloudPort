# Runtime monolítico de Navio

O projeto `cloudport-monolito-navio` é o primeiro runtime do monólito modular CloudPort. Ele carrega os módulos `servico-navio` e `servico-navio-siderurgico` no mesmo processo Spring Boot, sem copiar os diretórios de fontes dos módulos.

A arquitetura geral e o plano de migração estão em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md).

## Responsabilidades do runtime

- iniciar uma única aplicação Spring Boot;
- descobrir componentes, entidades e repositórios dos dois módulos;
- impedir a inicialização das classes executáveis e configurações de segurança isoladas dos módulos;
- centralizar JWT, CORS, endpoints públicos e credencial interna transitória;
- fornecer a implementação local de `CadastroNavioPorta`;
- usar uma conexão PostgreSQL com schemas separados;
- executar um histórico Flyway por módulo antes de criar o `EntityManagerFactory`;
- empacotar um único JAR e uma única imagem Docker.

## Módulos carregados

| Artefato Maven | Papel |
| --- | --- |
| `br.com.cloudport:servico-navio` | cadastro canônico, visitas, itens, plano de estiva, eventos e integração com o Yard |
| `br.com.cloudport:servico-navio-siderurgico` | regras específicas de navios e operações siderúrgicas |

A comunicação do módulo siderúrgico com o cadastro canônico usa `CadastroNavioLocalAdapter` quando `cloudport.modulo.navio.integracao=local`.

## Banco de dados

O runtime usa uma única conexão configurada por `MONOLITO_NAVIO_DB_URL`, mas preserva a propriedade dos dados por schema:

- `cloudport_navio`, configurável por `MONOLITO_NAVIO_SCHEMA`;
- `cloudport_siderurgico`, configurável por `MONOLITO_SIDERURGICO_SCHEMA`.

As migrações são fornecidas pelos próprios artefatos:

```text
classpath:cloudport/migrations/navio
classpath:cloudport/migrations/navio-siderurgico
```

O auto-configurador Flyway padrão permanece desativado porque o runtime cria dois objetos Flyway independentes.

## Compilação e testes

Execute a partir do reator Maven:

```bash
cd backend/cloudport-navio-modules

mvn -B -Pmodulo-monolito \
  -pl :servico-navio,:servico-navio-siderurgico \
  -DskipTests install

mvn -B -Pmodulo-monolito \
  -pl :cloudport-monolito-navio \
  test package
```

Os testes do runtime verificam:

- carregamento completo do contexto;
- PostgreSQL 16 por Testcontainers;
- criação dos dois schemas;
- históricos Flyway separados;
- origem das migrações nos artefatos dos módulos;
- validação do `EntityManagerFactory`;
- consultas nos repositórios JPA incorporados;
- configuração de segurança e CORS;
- integração local do cadastro canônico por ID e IMO.

## Variáveis de ambiente

### Obrigatórias

| Variável | Descrição |
| --- | --- |
| `MONOLITO_NAVIO_DB_URL` | URL JDBC PostgreSQL |
| `MONOLITO_NAVIO_DB_USERNAME` | usuário do banco |
| `MONOLITO_NAVIO_DB_PASSWORD` | senha do banco |
| `JWT_SECRET` | segredo HS256 com no mínimo 32 bytes |

### Opcionais e transitórias

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `MONOLITO_NAVIO_SERVER_PORT` | `8086` | porta HTTP do runtime |
| `MONOLITO_NAVIO_SCHEMA` | `cloudport_navio` | schema do módulo Navio |
| `MONOLITO_SIDERURGICO_SCHEMA` | `cloudport_siderurgico` | schema do módulo siderúrgico |
| `YARD_SERVICE_URL` | `http://localhost:8081` | endpoint legado do Yard enquanto ele não estiver incorporado |
| `YARD_RECONCILIACAO_MS` | `60000` | intervalo de reconciliação com o Yard |
| `NAVIO_SINCRONIZACAO_MS` | `300000` | intervalo de sincronização da projeção siderúrgica |
| `CLOUDPORT_INTERNAL_SERVICE_KEY` | vazio | credencial para deployments legados |
| `SECURITY_CORS_ALLOWED_ORIGINS` | `http://localhost:4200,http://localhost:4201` | origens permitidas |

## Execução local

Após os comandos de build anteriores, execute a partir da pasta do runtime:

```bash
cd ../cloudport-monolito-navio

export MONOLITO_NAVIO_DB_URL='jdbc:postgresql://localhost:5432/cloudport'
export MONOLITO_NAVIO_DB_USERNAME='cloudport'
export MONOLITO_NAVIO_DB_PASSWORD='cloudport'
export JWT_SECRET='substitua-por-um-segredo-com-pelo-menos-32-bytes'

java -jar target/cloudport-monolito-navio-*.jar
```

## Imagem Docker

O contexto de build deve ser a pasta `backend`:

```bash
cd backend
docker build -f cloudport-monolito-navio/Dockerfile -t cloudport-monolito-navio .
```

A imagem expõe a porta `8086` e executa somente o JAR consolidado.

## Regras para evolução

- não adicionar novos clientes HTTP entre Navio e Navio Siderúrgico;
- manter adaptadores HTTP apenas para compatibilidade com deployments isolados;
- não acessar repositories ou entidades do outro módulo diretamente;
- não copiar fontes ou migrações por caminhos relativos no runtime;
- publicar recursos no artefato do módulo proprietário;
- centralizar configurações transversais no runtime;
- manter os contratos REST existentes enquanto o frontend e integrações dependerem deles;
- atualizar a matriz de migração quando outro módulo for incorporado.
