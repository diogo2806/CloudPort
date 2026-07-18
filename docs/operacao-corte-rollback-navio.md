# Operação de corte e rollback do monólito modular CloudPort

Este runbook usa `backend/cloudport-runtime` como runtime canônico do CloudPort.

O executável `backend/cloudport-monolito-navio` é preservado exclusivamente para rollback intermediário do conjunto funcional que ele suporta. Os serviços `backend/servico-*` permanecem como última camada de retorno durante a janela de compatibilidade. O módulo de Carga Geral deve ser tratado separadamente em um rollback para uma versão anterior que não o contenha.

## Invariantes

1. Somente um runtime aceita comandos de escrita para cada domínio.
2. Somente um runtime executa jobs agendados.
3. Somente um grupo de consumidores RabbitMQ processa cada fila operacional.
4. O `cloudport-runtime` usa portas ou eventos internos, sem HTTP entre módulos incorporados.
5. Cada schema e seu `flyway_schema_history` permanecem sob ownership do módulo correspondente.
6. Rollback troca binário e roteamento; não executa downgrade de banco.
7. Deployments, imagens e credenciais anteriores permanecem disponíveis até a paridade e o retorno serem comprovados.
8. Integrações públicas e operacionais utilizam uma única origem ativa por rota.
9. Canais SSE e WebSocket precisam aplicar a mesma política de autenticação e autorização das APIs HTTP.
10. Dados sensíveis de integrações externas não podem ser registrados integralmente em logs.

## Artefatos oficiais

| Finalidade | Artefato |
| --- | --- |
| Runtime canônico | `backend/cloudport-runtime` |
| Reator Maven | `backend/cloudport-modules` |
| Contratos compartilhados | `backend/cloudport-contracts` |
| Docker pela raiz | `backend/cloudport-runtime/Dockerfile` |
| Docker no EasyPanel | `backend/Dockerfile`, contexto `/backend` |
| Compose canônico | `deploy/cloudport-runtime/docker-compose.yml` |
| Frontend EasyPanel | `frontend/Dockerfile`, contexto `/frontend` |
| Configuração Nginx | `frontend/nginx.conf` |
| Rollback intermediário | `backend/cloudport-monolito-navio` |
| Compose de rollback | `deploy/navio-monolito/docker-compose.yml`, perfil `rollback` |
| Serviços isolados anteriores | `backend/servico-*` e manifests do ambiente |

## Controles de execução única

| Variável | Runtime ativo | Runtime parado ou em observação | Efeito |
| --- | --- | --- | --- |
| `CLOUDPORT_WRITES_ENABLED` | `true` | `false` | Bloqueia comandos de escrita com `503` |
| `CLOUDPORT_JOBS_ENABLED` | `true` | `false` | Habilita ou remove agendamentos |
| `CLOUDPORT_CONSUMERS_ENABLED` | `true` | `false` | Inicia ou impede listeners RabbitMQ |

O runtime anterior exige também `CLOUDPORT_ROLLBACK_ENABLED=true`. Sem essa propriedade, a inicialização deve ser rejeitada.

Jobs críticos usam lock distribuído no PostgreSQL quando aplicável. Consumidores e comandos sujeitos a retry devem possuir idempotência persistente.

## Schemas e históricos

| Módulo | Schema |
| --- | --- |
| Autenticação | `cloudport_autenticacao` |
| Carga Geral | `cloudport_carga_geral` |
| Gate | `cloudport_gate` |
| Rail | `cloudport_rail` |
| Visibilidade | `cloudport_visibilidade` |
| Yard | `cloudport_yard` |
| Navio | `cloudport_navio` |
| Navio Siderúrgico | `cloudport_siderurgico` |

Cada schema mantém sua própria tabela `flyway_schema_history`. Não alterar checksum, versão ou conteúdo de migração já aplicada.

## Variáveis mínimas do backend

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
CLOUDPORT_SECURITY_JWT_SECRET
CLOUDPORT_SECURITY_JWT_EXPIRATION
SPRING_RABBITMQ_HOST
SPRING_RABBITMQ_USERNAME
SPRING_RABBITMQ_PASSWORD
SPRING_REDIS_HOST
SPRING_REDIS_PORT
```

Também devem ser fornecidas as credenciais das integrações habilitadas, clientes públicos, storage, webhooks e qualquer chave de API exigida pelo ambiente. O segredo JWT deve possuir pelo menos 32 bytes.

## Build canônico

```bash
cd backend/cloudport-modules
mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

Validar as duas formas de build Docker:

```bash
docker build \
  -f backend/cloudport-runtime/Dockerfile \
  -t cloudport-runtime:root .

docker build \
  -f backend/Dockerfile \
  -t cloudport-runtime:easypanel \
  backend
```

Validar o frontend:

```bash
docker build \
  -f frontend/Dockerfile \
  -t cloudport-frontend:easypanel \
  frontend
```

## Configuração do EasyPanel

### Backend

| Campo | Valor |
| --- | --- |
| Repositório | `diogo2806/CloudPort` |
| Branch | `main` |
| Caminho de build | `/backend` |
| Dockerfile | `Dockerfile` |
| Porta | `8080` |
| Health check | `/actuator/health/readiness` |

### Frontend

| Campo | Valor |
| --- | --- |
| Repositório | `diogo2806/CloudPort` |
| Branch | `main` |
| Caminho de build | `/frontend` |
| Dockerfile | `Dockerfile` |
| Porta | `80` |
| Health check | `/health` |

O Nginx do frontend possui fallback de SPA para `index.html`. O backend prepara o diretório persistente de documentos e executa com usuário não privilegiado.

## Validações antes do corte

1. Sincronizar a branch de implantação com `main` e confirmar ausência de conflitos.
2. Executar o build em `backend/cloudport-modules` e os testes de `cloudport-runtime`.
3. Validar `deploy/cloudport-runtime/docker-compose.yml`.
4. Construir as imagens pela raiz e pelos contextos usados no EasyPanel.
5. Executar `Flyway.validate()` nos oito schemas e confirmar ausência de migração pendente.
6. Confirmar ownership, constraints e contagens essenciais de cada domínio.
7. Criar backup consistente e registrar o ponto de restauração.
8. Confirmar que o proxy direciona cada rota para exatamente um backend.
9. Confirmar que o runtime canônico usa integrações internas em modo local.
10. Validar login, autorização, CORS, OpenAPI, erros, correlação, métricas e health checks.
11. Validar autenticação e autorização dos canais SSE/WebSocket.
12. Validar produção e consumo de eventos externos sem duplicação.
13. Executar smoke dos fluxos de Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.
14. Validar o portal, a página pública de line-up, o CAP e o Control Room.
15. Confirmar que logs não expõem tokens, credenciais, corpos externos ou dados operacionais sensíveis.
16. Registrar responsável, janela, critérios de aborto e procedimento de comunicação.

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
- serviços isolados anteriores permanecem parados ou com escrita, jobs e consumidores desativados;
- nenhuma credencial ou imagem de rollback é removida antes do encerramento formal da janela;
- o frontend utiliza somente a origem canônica da API;
- a página pública `/line-up` permanece restrita ao contrato sanitizado.

## Smoke obrigatório do runtime canônico

Validar, no mínimo:

1. `/actuator/health/readiness` e métricas operacionais;
2. `/health` do frontend e fallback da SPA;
3. portal, Control Room, CAP e configuração dinâmica;
4. login e emissão de JWT;
5. rejeição de chamada protegida sem token;
6. papéis e restrições administrativas;
7. cadastro canônico de navio e projeção siderúrgica por porta local;
8. mapa, inventário, reserva, allocation, ordem, work queue e work instruction do Yard;
9. agrupamento e otimização de recebimento do pátio;
10. telemetria reefer, alarmes e rotas no mapa;
11. Gate para Yard, Autenticação e Navio por portas locais;
12. Gate visual, appointment, truck visit, inspeção, trouble e EIR;
13. controle de entrada e saída de pessoas;
14. saída direta de carga autopropelida;
15. embarque de contêiner diretamente do Gate para o navio;
16. visita de trem, manifesto, ordem, conclusão e partida;
17. locomotiva isolada até o embarque no navio;
18. line-up ferroviário e composição gráfica;
19. Bill of Lading, cargo lot, movimentação parcial, consolidação e saldo da Carga Geral;
20. Vessel Planner, BAPLIE, geometria, estabilidade e restrições;
21. planejamento e validação de bobinas;
22. line-up interno e line-up público de navios;
23. projeções, SSE e central de alertas da Visibilidade;
24. Billing, geração de cobrança, fatura e pagamento controlado;
25. `X-Correlation-Id` e `traceparent`;
26. erro padronizado sem conteúdo sensível;
27. OpenAPI sem rota, schema ou `operationId` duplicado;
28. persistência no schema proprietário;
29. um único job e um único consumidor por chave ou fila.

## Critérios de aprovação

O corte é aprovado quando:

- todos os smokes passam;
- não há erro de Flyway, JPA, rota duplicada ou bean duplicado;
- nenhuma chamada HTTP interna aparece nos logs do runtime canônico;
- eventos não apresentam duplicação não tratada;
- contagens, saldos e vínculos de dados permanecem consistentes;
- a operação confirma paridade dos fluxos críticos;
- o procedimento de rollback foi ensaiado no ambiente de aceitação;
- os oito schemas estão validados;
- o frontend e o backend permanecem saudáveis no EasyPanel;
- não existem dois escritores, schedulers ou consumidores ativos para o mesmo domínio;
- as pendências de segurança que bloqueiam o ambiente foram tratadas ou formalmente aceitas.

## Rollback intermediário para `cloudport-monolito-navio`

O retorno não desfaz migrações. Ele reativa um binário anterior compatível com as estruturas já aplicadas.

1. Bloquear novas entradas no proxy ou colocar a aplicação em manutenção.
2. Retirar o `cloudport-runtime` do proxy.
3. Parar o `cloudport-runtime`.
4. Confirmar que não existem transações, jobs ou mensagens em processamento.
5. Validar que o banco permanece compatível com o artefato anterior.
6. Identificar quais domínios não existem no rollback intermediário, especialmente Carga Geral.
7. Iniciar o perfil `rollback`.
8. Executar health checks e leituras controladas.
9. Direcionar somente as rotas suportadas para o runtime de rollback.
10. Executar uma escrita controlada em cada domínio suportado.
11. Validar auditoria, dados, filas e integrações externas.
12. Manter indisponíveis ou em modo de manutenção as rotas sem implementação compatível.

Comando:

```bash
ROLLBACK_WRITES_ENABLED=true \
ROLLBACK_JOBS_ENABLED=true \
ROLLBACK_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile rollback \
  up -d --build
```

O perfil define `CLOUDPORT_ROLLBACK_ENABLED=true`.

Nunca execute o perfil `rollback` com escrita, jobs ou consumidores ativos enquanto o `cloudport-runtime` estiver operacional.

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

Os demais serviços anteriores devem ser reativados pelos manifests atuais de cada ambiente. Serviços standalone só podem receber tráfego depois de confirmar segurança, variáveis obrigatórias e compatibilidade das migrações.

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
10. encerrar formalmente a janela de rollback antes da fase destrutiva;
11. documentar explicitamente schemas sem suporte no binário de retorno.

## Retirada de deployments e credenciais anteriores

A remoção exige:

- período de observação concluído;
- zero tráfego nos endpoints anteriores;
- zero uso de credencial interna entre módulos incorporados;
- zero consumidores e jobs anteriores ativos;
- rollback ensaiado e documentado;
- backups e imagens retidos pelo prazo definido;
- aprovação de operação e segurança;
- smoke de Carga Geral incluído;
- confirmação de que frontend e integrações apontam somente para a origem canônica.

## Condições que bloqueiam ou abortam o corte

- conflito com `main`;
- build, teste, smoke ou ArchUnit falhando;
- checksum divergente ou migração pendente;
- rota simultaneamente apontada para dois runtimes;
- mais de um escritor, scheduler ou grupo consumidor ativo;
- adaptador HTTP interno registrado no `cloudport-runtime`;
- diferença de dados, estoque ou saldo sem explicação;
- falha de autenticação, autorização, CORS ou token;
- WebSocket operacional sem proteção;
- OpenAPI com operação duplicada;
- ausência de correlação, tracing ou métricas operacionais;
- exposição de segredo ou corpo externo sensível em logs;
- módulo standalone exposto sem cadeia de segurança;
- incapacidade de executar o rollback ensaiado;
- tentativa de retornar Carga Geral para um binário que não possui o domínio sem plano explícito de indisponibilidade.