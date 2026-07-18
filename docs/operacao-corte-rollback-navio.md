# Operação de corte e rollback do monólito modular CloudPort

Este runbook usa `backend/cloudport-runtime` como runtime canônico do CloudPort.

O executável `backend/cloudport-monolito-navio` é o primeiro corte preservado exclusivamente para rollback intermediário. Os serviços `backend/servico-*` permanecem como última camada de retorno durante a janela de compatibilidade.

## Invariantes

1. Somente um runtime aceita comandos de escrita para cada domínio.
2. Somente um runtime executa jobs agendados.
3. Somente um grupo de consumidores RabbitMQ processa cada fila operacional.
4. O `cloudport-runtime` usa portas ou eventos internos, sem HTTP entre módulos incorporados.
5. Cada schema e seu `flyway_schema_history` permanecem sob ownership do módulo correspondente.
6. Rollback troca binário e roteamento; não executa downgrade de banco.
7. Deployments, imagens e credenciais anteriores permanecem disponíveis até a paridade e o retorno serem comprovados.
8. Os oito módulos usam o mesmo emissor e decoder JWT no runtime consolidado.

## Artefatos oficiais

| Finalidade | Artefato |
| --- | --- |
| Runtime canônico | `backend/cloudport-runtime` |
| Reator canônico | `backend/cloudport-modules` |
| Contratos compartilhados | `backend/cloudport-contracts` |
| Imagem backend com contexto `/backend` | `backend/Dockerfile` |
| Imagem backend com contexto da raiz | `backend/cloudport-runtime/Dockerfile` |
| Imagem frontend com contexto `/frontend` | `frontend/Dockerfile` |
| Compose canônico | `deploy/cloudport-runtime/docker-compose.yml` |
| Rollback intermediário | `backend/cloudport-monolito-navio` |
| Compose de rollback | `deploy/navio-monolito/docker-compose.yml`, perfil `rollback` |
| Serviços isolados anteriores | `backend/servico-*` e manifests do ambiente |

## Variáveis obrigatórias do backend

| Variável | Finalidade |
| --- | --- |
| `DB_HOST` | host do PostgreSQL |
| `DB_PORT` | porta do PostgreSQL |
| `DB_NAME` | banco do CloudPort |
| `DB_USER` | usuário do banco |
| `DB_PASS` | senha do banco |
| `DB_SCHEMA` | search path dos oito schemas, separado por vírgulas |
| `SECURITY_JWT_SECRET` | segredo JWT com pelo menos 32 bytes |
| `SECURITY_JWT_EXPIRATION_MS` | duração do token em milissegundos |
| `ADMIN_EMAIL` | administrador inicial |
| `ADMIN_PASSWORD` | senha segura do administrador inicial |

Valor de referência para `DB_SCHEMA`:

```text
cloudport_autenticacao,cloudport_carga_geral,cloudport_gate,cloudport_rail,cloudport_visibilidade,cloudport_yard,cloudport_navio,cloudport_siderurgico
```

RabbitMQ, Redis, TOS, alertas, armazenamento e outras integrações continuam usando variáveis específicas.

## Controles de execução única

| Variável | Runtime ativo | Runtime parado ou em observação | Efeito |
| --- | --- | --- | --- |
| `CLOUDPORT_WRITES_ENABLED` | `true` | `false` | bloqueia comandos de escrita com `503` |
| `CLOUDPORT_JOBS_ENABLED` | `true` | `false` | habilita ou remove agendamentos |
| `CLOUDPORT_CONSUMERS_ENABLED` | `true` | `false` | inicia ou impede listeners RabbitMQ |

O runtime anterior exige também `CLOUDPORT_ROLLBACK_ENABLED=true`. Sem essa propriedade, a inicialização é rejeitada.

Jobs críticos usam `pg_try_advisory_xact_lock`. Consumidores, comandos e recepções EDI sujeitos a retry devem possuir idempotência persistente.

## Schemas e históricos

| Módulo | Schema |
| --- | --- |
| Autenticação | `cloudport_autenticacao` |
| Carga Geral | `cloudport_carga_geral` |
| Gate | `cloudport_gate` |
| Rail | `cloudport_rail` |
| Visibilidade | `cloudport_visibilidade` |
| Yard e Inventário | `cloudport_yard` |
| Navio | `cloudport_navio` |
| Navio Siderúrgico | `cloudport_siderurgico` |

Cada schema mantém sua própria tabela `flyway_schema_history`. Não alterar checksum, versão ou conteúdo de migração já aplicada.

## Build de validação

```bash
cd backend/cloudport-modules
mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

Também devem ser construídas as formas suportadas das imagens:

```bash
docker build -f backend/cloudport-runtime/Dockerfile .
docker build -f backend/Dockerfile backend
docker build -f frontend/Dockerfile frontend
```

## Validações antes do corte

1. Sincronizar a branch de implantação com `main` e confirmar ausência de conflitos.
2. Executar o build do parent, do reator e os testes de `cloudport-runtime`.
3. Construir as imagens pelos contextos usados no ambiente.
4. Validar `deploy/cloudport-runtime/docker-compose.yml`.
5. Executar `Flyway.validate()` nos oito schemas e confirmar ausência de migração pendente.
6. Confirmar ownership e contagens essenciais de cada domínio.
7. Criar backup consistente e registrar o ponto de restauração.
8. Confirmar que o proxy direciona cada rota para exatamente um backend.
9. Confirmar que o runtime canônico usa integrações internas em modo `local`.
10. Validar login, autorização, CORS, OpenAPI, erros, correlação, métricas e health checks.
11. Validar produção e consumo de eventos externos sem duplicação.
12. Executar smoke dos oito módulos e dos fluxos frontend principais.
13. Confirmar que escrita, jobs e consumidores estão desativados nos deployments anteriores.
14. Registrar responsável, janela, critérios de aborto e procedimento de comunicação.

## Configuração do EasyPanel

### Backend

- repositório: `diogo2806/CloudPort`;
- branch: `main`;
- caminho de build: `/backend`;
- arquivo: `Dockerfile`;
- porta: `8080`;
- health check: `/actuator/health/readiness`.

### Frontend

- repositório: `diogo2806/CloudPort`;
- branch: `main`;
- caminho de build: `/frontend`;
- arquivo: `Dockerfile`;
- porta: `80`;
- health check: `/health`.

O backend prepara o diretório persistente de documentos. O frontend publica a SPA em Nginx e encaminha rotas desconhecidas para `index.html`.

## Início do runtime canônico

```bash
docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

Durante a operação consolidada:

- o proxy aponta as rotas incorporadas somente para `cloudport-runtime`;
- o runtime canônico é o único escritor, scheduler e consumidor ativo;
- o `cloudport-monolito-navio` permanece parado;
- serviços isolados permanecem parados ou com escrita, jobs e consumidores desativados;
- nenhuma credencial ou imagem de rollback é removida antes do encerramento formal da janela.

## Smoke obrigatório do runtime canônico

Validar, no mínimo:

1. health, readiness e Prometheus;
2. portal, Control Room, assets e configuração dinâmica;
3. login, administrador inicial e emissão de JWT;
4. rejeição de chamada sem token;
5. roles e restrições administrativas;
6. Carga Geral: conhecimento, item, lote, referência e movimentação;
7. Navio: cadastro, escala, Bay Plan e Vessel Planner;
8. Navio Siderúrgico: projeção canônica e integração local;
9. Yard: mapa, reserva, ordem, work queue, work instruction, allocation e reefer;
10. Inventário: unidade, ciclo de vida, hold e consulta canônica;
11. Gate: appointment, truck visit, estágio, documento e EIR;
12. Rail: visita, composição e associação de vagão;
13. Visibilidade: dashboard, histórico e alertas;
14. Control Room: stream, telemetria, alarmes e comando;
15. `X-Correlation-Id` e `traceparent`;
16. erro padronizado sem exposição de SQL ou corpo externo;
17. OpenAPI sem rota, schema ou `operationId` duplicado;
18. persistência no schema proprietário;
19. um único job e um único consumidor por chave ou fila;
20. frontend servido pelo Nginx com `/health` e fallback de SPA.

## Critérios de aprovação

O corte é aprovado quando:

- todos os smokes passam;
- não há erro de Flyway, JPA, rota duplicada ou bean duplicado;
- nenhuma chamada HTTP interna aparece nos logs do runtime canônico;
- eventos não apresentam duplicação não tratada;
- contagens e vínculos de dados permanecem consistentes;
- a operação confirma paridade dos fluxos críticos;
- autenticação e autorização foram validadas nos canais HTTP, SSE e WebSocket aplicáveis;
- o procedimento de rollback foi ensaiado no ambiente de aceitação.

## Rollback intermediário para `cloudport-monolito-navio`

O retorno não desfaz migrações. Ele reativa um binário anterior compatível com as estruturas já aplicadas.

1. Bloquear novas entradas no proxy ou colocar a aplicação em manutenção.
2. Retirar o `cloudport-runtime` do proxy.
3. Parar o `cloudport-runtime`.
4. Confirmar que não existem transações, jobs ou mensagens em processamento.
5. Validar compatibilidade do banco com o artefato anterior.
6. Iniciar o perfil `rollback`.
7. Executar health checks e leituras controladas.
8. Direcionar somente as rotas suportadas ao runtime de rollback.
9. Reativar serviços isolados necessários para os domínios não cobertos pelo rollback intermediário.
10. Executar uma escrita controlada em cada domínio afetado.
11. Validar auditoria, dados, filas e integrações externas.

```bash
ROLLBACK_WRITES_ENABLED=true \
ROLLBACK_JOBS_ENABLED=true \
ROLLBACK_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile rollback \
  up -d --build
```

O perfil define `CLOUDPORT_ROLLBACK_ENABLED=true`. Nunca o execute com escrita, jobs ou consumidores ativos enquanto o `cloudport-runtime` estiver operacional.

## Rollback adicional para serviços isolados

```bash
LEGACY_NAVIO_WRITES_ENABLED=true \
LEGACY_NAVIO_JOBS_ENABLED=true \
LEGACY_NAVIO_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile legado \
  up -d --build servico-navio servico-navio-siderurgico
```

Os demais serviços anteriores devem ser reativados pelos manifests atuais de cada ambiente. A execução standalone precisa ter segurança equivalente à do runtime canônico antes de receber tráfego.

## Compatibilidade Flyway

Durante a janela de retorno:

1. adicionar antes de remover;
2. manter colunas e contratos antigos enquanto o binário anterior puder voltar;
3. fazer backfill idempotente e observável;
4. evitar `DROP`, redução de tamanho e alteração incompatível de tipo;
5. evitar `NOT NULL` sem valor padrão e sem preenchimento prévio;
6. não renomear diretamente estruturas usadas pelo rollback;
7. nunca editar migração aplicada;
8. corrigir por nova versão;
9. validar os oito históricos antes da promoção;
10. encerrar formalmente a janela de rollback antes da fase destrutiva.

## Retirada de deployments e credenciais anteriores

A remoção exige:

- período de observação concluído;
- zero tráfego nos endpoints anteriores;
- zero uso de credencial interna entre módulos incorporados;
- zero consumidores e jobs anteriores ativos;
- rollback ensaiado e documentado;
- backups e imagens retidos pelo prazo definido;
- aprovação de operação e segurança.

## Condições que bloqueiam ou abortam o corte

- conflito com `main`;
- build, teste, smoke ou ArchUnit falhando;
- checksum divergente ou migração pendente;
- rota simultaneamente apontada para dois runtimes;
- mais de um escritor, scheduler ou grupo consumidor ativo;
- adaptador HTTP interno registrado no `cloudport-runtime`;
- diferença de dados sem explicação;
- falha de autenticação, autorização, CORS ou token;
- WebSocket operacional aceitando conexão ou assinatura anônima;
- OpenAPI com operação duplicada;
- ausência de correlação, tracing ou métricas operacionais;
- incapacidade de executar o rollback ensaiado.
