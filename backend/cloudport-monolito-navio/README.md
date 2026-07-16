# Runtime monolítico de Navio

O projeto `cloudport-monolito-navio` é o primeiro runtime do monólito modular CloudPort. Ele carrega os módulos `servico-navio` e `servico-navio-siderurgico` no mesmo processo Spring Boot, sem copiar os diretórios de fontes dos módulos.

A arquitetura geral e o plano de migração estão em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md). O procedimento operacional de corte e retorno está em [`../../docs/operacao-corte-rollback-navio.md`](../../docs/operacao-corte-rollback-navio.md).

## Responsabilidades do runtime

- iniciar uma única aplicação Spring Boot;
- descobrir componentes, entidades e repositórios dos dois módulos;
- impedir a inicialização das classes executáveis e configurações de segurança isoladas dos módulos;
- centralizar JWT, CORS, endpoints públicos e credencial interna transitória;
- fornecer a implementação local de `CadastroNavioPorta`;
- usar uma conexão PostgreSQL com schemas separados;
- executar um histórico Flyway por módulo antes de criar o `EntityManagerFactory`;
- manter o runtime consolidado como único caminho de escrita durante o corte;
- serializar jobs por bloqueio transacional PostgreSQL;
- empacotar um único JAR e uma única imagem Docker.

## Módulos carregados

| Artefato Maven | Papel |
| --- | --- |
| `br.com.cloudport:servico-navio` | cadastro canônico, visitas, itens, plano de estiva, eventos e integração com o Yard |
| `br.com.cloudport:servico-navio-siderurgico` | regras específicas de navios e operações siderúrgicas |

A comunicação do módulo siderúrgico com o cadastro canônico usa `CadastroNavioLocalAdapter` quando `cloudport.modulo.navio.integracao=local`. O cliente HTTP continua no artefato siderúrgico somente para rollback e execução isolada, mas não é registrado no contexto monolítico.

## Banco de dados

O runtime usa uma única conexão configurada por `MONOLITO_NAVIO_DB_URL`, mas preserva a propriedade dos dados por schema:

- `cloudport_navio`, configurável por `MONOLITO_NAVIO_SCHEMA`;
- `cloudport_siderurgico`, configurável por `MONOLITO_SIDERURGICO_SCHEMA`.

As migrações são fornecidas pelos próprios artefatos:

```text
classpath:cloudport/migrations/navio
classpath:cloudport/migrations/navio-siderurgico
```

O auto-configurador Flyway padrão permanece desativado porque o runtime cria dois objetos Flyway independentes. O rollback normal troca a aplicação e o roteamento, sem executar downgrade do banco. Migrações durante a janela de retorno devem seguir `expand and contract`.

## Proteção contra execução duplicada

Durante a coexistência temporária:

- o monólito usa `CLOUDPORT_WRITES_ENABLED=true` e `CLOUDPORT_JOBS_ENABLED=true`;
- os deployments legados usam escrita e jobs desativados por padrão no Compose;
- comandos de escrita enviados ao legado recebem `503 Service Unavailable`;
- os jobs usam `pg_try_advisory_xact_lock` e somente uma transação compartilhando o mesmo PostgreSQL executa cada chave;
- o bloqueio local usado em bancos de teste não substitui o bloqueio distribuído PostgreSQL em ambiente real.

## Compilação e testes

Execute a partir do reator Maven:

```bash
cd backend/cloudport-navio-modules

mvn -B -Pmodulo-monolito \
  -pl :servico-navio,:servico-navio-siderurgico \
  install

mvn -B -Pmodulo-monolito \
  -pl :cloudport-monolito-navio \
  test package
```

Os testes do runtime verificam:

- carregamento completo do contexto;
- PostgreSQL 16 por Testcontainers;
- criação dos dois schemas;
- históricos Flyway separados, válidos e sem migração pendente;
- origem das migrações nos artefatos dos módulos;
- validação do `EntityManagerFactory`;
- consultas nos repositórios JPA incorporados;
- configuração única de segurança;
- presença dos controllers dos dois módulos;
- integração local do cadastro canônico e ausência do cliente HTTP no contexto;
- exclusão mútua dos jobs por bloqueio PostgreSQL;
- modo somente leitura dos deployments legados;
- ausência de ciclos e de acesso direto a domínio/repository de outro módulo por ArchUnit.

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
| `CLOUDPORT_WRITES_ENABLED` | `true` | habilita comandos de escrita no runtime ativo |
| `CLOUDPORT_JOBS_ENABLED` | `true` | registra os jobs agendados no runtime ativo |
| `YARD_SERVICE_URL` | `http://localhost:8081` | endpoint legado do Yard enquanto ele não estiver incorporado |
| `YARD_RECONCILIACAO_MS` | `60000` | intervalo de reconciliação com o Yard |
| `NAVIO_SINCRONIZACAO_MS` | `300000` | intervalo de sincronização da projeção siderúrgica |
| `CLOUDPORT_INTERNAL_SERVICE_KEY` | vazio | credencial para deployments legados |
| `SECURITY_CORS_ALLOWED_ORIGINS` | `http://localhost:4200,http://localhost:4201` | origens permitidas |

O perfil legado do Compose usa adicionalmente `LEGACY_NAVIO_WRITES_ENABLED` e `LEGACY_NAVIO_JOBS_ENABLED`, ambos `false` por padrão. Eles só devem ser alterados para `true` após o monólito ser parado em um rollback.

## Execução local

Após os comandos de build anteriores, execute a partir da pasta do runtime:

```bash
cd ../cloudport-monolito-navio

export MONOLITO_NAVIO_DB_URL='jdbc:postgresql://localhost:5432/cloudport'
export MONOLITO_NAVIO_DB_USERNAME='cloudport'
export MONOLITO_NAVIO_DB_PASSWORD='cloudport'
export JWT_SECRET='substitua-por-um-segredo-com-pelo-menos-32-bytes'
export CLOUDPORT_WRITES_ENABLED='true'
export CLOUDPORT_JOBS_ENABLED='true'

java -jar target/cloudport-monolito-navio-*.jar
```

## Imagem Docker

O contexto de build deve ser a raiz do repositório:

```bash
docker build \
  -f backend/cloudport-monolito-navio/Dockerfile \
  -t cloudport-monolito-navio .
```

A imagem expõe a porta `8086` e executa somente o JAR consolidado.

## Regras para evolução

- não adicionar novos clientes HTTP entre Navio e Navio Siderúrgico;
- manter adaptadores HTTP apenas para compatibilidade com deployments isolados;
- não acessar repositories ou entidades do outro módulo diretamente;
- não copiar fontes ou migrações por caminhos relativos no runtime;
- publicar recursos no artefato do módulo proprietário;
- centralizar configurações transversais no runtime;
- manter uma única instância escritora e executora por domínio durante cada corte;
- manter os contratos REST existentes enquanto o frontend e integrações dependerem deles;
- atualizar a matriz de migração quando outro módulo for incorporado.
