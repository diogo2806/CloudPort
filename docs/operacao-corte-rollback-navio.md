# Operação de corte e rollback do monólito modular CloudPort

Atualizado em 2026-07-18 para o runtime com oito módulos, oito schemas, Carga Geral incorporada e imagens de implantação do EasyPanel.

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
8. Nenhum corte é aprovado enquanto existir conflito de branch, migração pendente, rota duplicada ou execução concorrente de escritor, job ou consumidor.

## Artefatos oficiais

| Finalidade | Artefato |
| --- | --- |
| Runtime canônico | `backend/cloudport-runtime` |
| Parent Maven | `backend/cloudport-navio-modules` |
| Reator Maven | `backend/cloudport-modules` |
| Contratos compartilhados | `backend/cloudport-contracts` |
| Docker pela raiz | `backend/cloudport-runtime/Dockerfile` |
| Docker com contexto `/backend` | `backend/Dockerfile` |
| Compose canônico | `deploy/cloudport-runtime/docker-compose.yml` |
| Frontend no EasyPanel | `frontend/Dockerfile` e `frontend/nginx.conf` |
| Rollback intermediário | `backend/cloudport-monolito-navio` |
| Compose de rollback | `deploy/navio-monolito/docker-compose.yml`, perfil `rollback` |
| Serviços isolados anteriores | `backend/servico-*` e manifests do ambiente |

## Controles de execução única

| Variável | Runtime ativo | Runtime parado ou em observação | Efeito |
| --- | --- | --- | --- |
| `CLOUDPORT_WRITES_ENABLED` | `true` | `false` | bloqueia comandos de escrita com `503` |
| `CLOUDPORT_JOBS_ENABLED` | `true` | `false` | habilita ou remove agendamentos |
| `CLOUDPORT_CONSUMERS_ENABLED` | `true` | `false` | inicia ou impede listeners RabbitMQ |

O runtime anterior exige também `CLOUDPORT_ROLLBACK_ENABLED=true`. Sem essa propriedade, a inicialização é rejeitada.

Jobs críticos usam `pg_try_advisory_xact_lock`. Consumidores e comandos sujeitos a retry devem possuir idempotência persistente.

## Schemas e históricos

| Módulo | Schema |
| --- | --- |
| Autenticação | `cloudport_autenticacao` |
| Carga Geral | `cloudport_carga_geral` |
| Gate, Billing e CAP | `cloudport_gate` |
| Rail | `cloudport_rail` |
| Visibilidade | `cloudport_visibilidade` |
| Yard e Inventory Management | `cloudport_yard` |
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

Validar também as duas formas de construção da imagem:

```bash
# contexto da raiz
docker build -f backend/cloudport-runtime/Dockerfile -t cloudport-runtime:cutover .

# mesmo contexto usado pelo EasyPanel
docker build -f backend/Dockerfile -t cloudport-runtime-easypanel:cutover backend
```

## Variáveis mínimas do runtime

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cloudport
SPRING_DATASOURCE_USERNAME=cloudport
SPRING_DATASOURCE_PASSWORD=<segredo>
CLOUDPORT_SECURITY_JWT_SECRET=<mínimo-32-bytes>
CLOUDPORT_SECURITY_JWT_EXPIRATION=PT2H
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_USERNAME=cloudport
SPRING_RABBITMQ_PASSWORD=<segredo>
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
CLOUDPORT_WRITES_ENABLED=true
CLOUDPORT_JOBS_ENABLED=true
CLOUDPORT_CONSUMERS_ENABLED=true
```

Também devem ser configurados CORS, credencial interna de compatibilidade, TOS, storage, webhooks e demais integrações habilitadas no ambiente.

## Validações antes do corte

1. Sincronizar a branch de implantação com `main` e confirmar ausência de conflitos.
2. Executar o build do parent, do reator e os testes de `cloudport-runtime`.
3. Validar `deploy/cloudport-runtime/docker-compose.yml` e construir as imagens canônicas.
4. Executar `Flyway.validate()` nos oito schemas e confirmar ausência de migração pendente.
5. Confirmar ownership, contagens essenciais e vínculos de cada domínio.
6. Criar backup consistente e registrar o ponto de restauração.
7. Confirmar que o proxy direciona cada rota para exatamente um backend.
8. Confirmar que o runtime canônico usa integrações internas em modo `local`.
9. Validar login, autorização, CORS, OpenAPI, erros, correlação, métricas e health checks.
10. Validar produção e consumo de eventos externos sem duplicação.
11. Executar smoke de Autenticação, Carga Geral, Gate, Billing/CAP, Rail, Visibilidade, Yard, Inventory, Navio e Navio Siderúrgico.
12. Validar o portal, Control Room, central de alertas, grade operacional e ajuda contextual.
13. Registrar responsável, janela, critérios de aborto e procedimento de comunicação.

## Início pelo Docker Compose

```bash
docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

Durante a operação consolidada:

- o proxy aponta as rotas incorporadas somente para `cloudport-runtime`;
- o runtime canônico é o único escritor, scheduler e consumidor ativo;
- o `cloudport-monolito-navio` permanece parado;
- serviços isolados anteriores permanecem parados ou com escrita, jobs e consumidores desativados;
- nenhuma credencial ou imagem de rollback é removida antes do encerramento formal da janela.

## Início pelo EasyPanel

### Backend

- caminho de build: `/backend`;
- arquivo: `Dockerfile`;
- porta: `8080`;
- health check: `/actuator/health/readiness`.

### Frontend

- caminho de build: `/frontend`;
- arquivo: `Dockerfile`;
- porta: `80`;
- health check: `/health`.

Antes de promover, confirmar que o frontend usa a URL do backend canônico e que o fallback de SPA retorna `index.html` para rotas do portal.

## Smoke obrigatório do runtime canônico

Validar, no mínimo:

1. `health`, readiness e Prometheus;
2. portal, Control Room e configuração dinâmica;
3. login e emissão de JWT;
4. rejeição de chamada sem token;
5. roles e restrições administrativas;
6. Carga Geral com Bill of Lading, item, lote e movimentação de estoque;
7. Gate com appointment, truck visit, transação, inspeção, documento e EIR;
8. Billing/CAP com tarifa, cobrança, fatura, pagamento e isolamento da transportadora;
9. inventário canônico e movimentação do Yard;
10. mapa, reserva, allocation, work queue e work instruction do Yard;
11. Gate → Yard e Gate → Autenticação por porta local;
12. visita de trem, line-up e integração Rail/Yard;
13. cadastro canônico, escala, Vessel Planner, crane plan e integração Navio/Yard;
14. projeções e alertas de Visibilidade;
15. telemetria, alarmes, dispositivos e comandos do Control Room;
16. BAPLIE, COPRAR, COARRI e VERMAS com idempotência e auditoria;
17. `X-Correlation-Id` e `traceparent`;
18. erro padronizado;
19. OpenAPI sem rota, schema ou `operationId` duplicado;
20. persistência no schema proprietário;
21. um único job e um único consumidor por chave ou fila.

## Critérios de aprovação

O corte é aprovado quando:

- todos os smokes passam;
- não há erro de Flyway, JPA, rota duplicada ou bean duplicado;
- nenhuma chamada HTTP interna aparece nos logs do runtime canônico;
- eventos não apresentam duplicação não tratada;
- contagens e vínculos de dados permanecem consistentes;
- a operação confirma paridade dos fluxos críticos;
- o procedimento de rollback foi ensaiado no ambiente de aceitação;
- as pendências técnicas conhecidas foram aceitas formalmente ou corrigidas antes da produção.

## Rollback intermediário para `cloudport-monolito-navio`

O retorno não desfaz migrações. Ele reativa um binário anterior compatível com as estruturas já aplicadas.

1. Bloquear novas entradas no proxy ou colocar a aplicação em manutenção.
2. Retirar o `cloudport-runtime` do proxy.
3. Parar o `cloudport-runtime`.
4. Confirmar que não existem transações, jobs ou mensagens em processamento.
5. Validar que o banco permanece compatível com o artefato anterior.
6. Iniciar o perfil `rollback`.
7. Executar health checks e leituras controladas.
8. Direcionar as rotas para o runtime de rollback.
9. Executar uma escrita controlada em cada domínio afetado.
10. Validar auditoria, dados, filas e integrações externas.

```bash
ROLLBACK_WRITES_ENABLED=true \
ROLLBACK_JOBS_ENABLED=true \
ROLLBACK_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile rollback \
  up -d --build
```

O perfil define `CLOUDPORT_ROLLBACK_ENABLED=true`. Nunca execute o perfil com escrita, jobs ou consumidores ativos enquanto o `cloudport-runtime` estiver operacional.

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

Os demais serviços anteriores devem ser reativados pelos manifests atuais de cada ambiente. Antes de cada ativação, garantir que o domínio correspondente não está aceitando escrita no runtime canônico.

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
- OpenAPI com operação duplicada;
- ausência de correlação, tracing ou métricas operacionais;
- incapacidade de executar o rollback ensaiado.

As pendências técnicas que exigem correção antes de determinados ambientes estão em `docs/requisitos/requisito-tecnico.md`.
