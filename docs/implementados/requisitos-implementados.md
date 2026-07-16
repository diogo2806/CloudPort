# Requisitos implementados - CloudPort

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/implementados/requisitos-implementados.md`.

Não criar outros documentos, arquivos de evidência, logs, históricos ou rascunhos nesta pasta. Toda entrega deve sair de `docs/requisitos/modulo-navios-back-front-gaps.md` e ser registrada aqui sem duplicação.

## Módulo Navio implementado

1. Criar visita de navio.
2. Criar item operacional de embarque, descarga e restow.
3. Criar plano de estiva por visita.
4. Criar eventos e resumo operacional da visita.
5. Criar endpoints básicos `/visitas-navio`.
6. Criar endpoints de integração em `/visitas-navio/{id}/integracao-patio`.
7. Adicionar campos de integração em `ItemOperacaoNavio`.
8. Expor relatório operacional integrado básico.

## Integração Navio + Yard implementada

1. Criar reserva de pátio vinculada ao item de navio.
2. Adicionar visita, item e plano em `OrdemTrabalhoPatio`.
3. Criar ordem real no Yard.
4. Impedir ordem ativa duplicada por visita e item.
5. Expor filas e ordens sem cobertura por visita.
6. Permitir sincronização manual e automática de status.
7. Permitir gerar reservas e ordens.
8. Permitir replanejamento inicial.
9. Permitir alterar prioridade, suspender e retomar ordens.
10. Atualizar item conforme o estado real da ordem.
11. Preencher posição real, consumir reserva ao concluir e cancelar reserva ao cancelar ordem.
12. Registrar evento somente quando a reconciliação altera dados.

## Control Room implementado

1. Criar painel Navio + Yard com filtros, movimentos iminentes, filas, reservas, ordens, alertas e exceções.
2. Permitir gerar reservas e ordens, sincronizar, replanejar, priorizar, suspender e retomar.
3. Carregar work queues persistentes com job list expansível.
4. Ativar ou desativar fila, editar POW, pool e equipamento, executar dispatch, resetar e cancelar work instruction.
5. Exibir loading por ação e feedback de sucesso ou erro.
6. Integrar ao portal pela rota autenticada `/home/navio/control-room`.
7. Implementar SSO por `postMessage` restrito a origens configuradas.
8. Implementar login próprio como fallback e restringir perfis.
9. Enviar JWT, usuário, origem e `X-Correlation-Id` nas ações.
10. Exibir erro com `codigo`, `mensagem`, `detalhes` e `correlationId`.
11. Executar consultas do snapshot em paralelo, aplicar resultado atomicamente e impedir sobreposição.
12. Solicitar motivo antes de alterações de fase, prioridade, suspensão, retomada, reset, cancelamento e alterações administrativas de work queue.

## Work queues implementadas

1. Listar, criar, ativar e desativar work queue.
2. Associar POW, pool operacional e equipamento.
3. Expor job list e executar dispatch.
4. Resetar e cancelar work instruction.
5. Expor work queues pelo módulo de Navio.
6. Persistir `workQueueId` em `OrdemTrabalhoPatio`.
7. Atualizar job list por `PATCH /yard/patio/work-queues/{id}/ordens`.
8. Vincular automaticamente somente quando houver uma fila compatível inequívoca.
9. Remover comparação incorreta entre camada e bloco/zona.
10. Honrar `limiteOrdens` no dispatch.
11. Padronizar a resposta de dispatch.
12. Auditar criação, ativação, desativação, POW, pool, equipamento, vínculo, dispatch, reset e cancelamento.
13. Restringir operações por perfil.
14. Exigir e auditar motivo em ativação, desativação, alteração de POW, pool, equipamento, job list, reset e cancelamento.
15. Exigir e auditar motivo em alteração de status, prioridade, suspensão e retomada de ordens.

## Reserva contra mapa real implementada

1. Consultar `GET /yard/patio/reservas/posicoes` antes de reservar.
2. Selecionar posição real com linha, coluna e camada.
3. Recusar mapa vazio, posição inexistente, posição ocupada e reserva ativa duplicada.
4. Remover identificadores artificiais de posição.
5. Armazenar identificador e coordenadas reais.
6. Garantir dados necessários à criação da ordem real.
7. Validar bloqueio, interdição, área permitida, tipo de carga, peso, altura, camada e capacidade da pilha.
8. Expirar reservas por prazo configurável.
9. Cancelar reserva ao cancelar visita ou replanejar item.
10. Auditar criação, consumo, cancelamento e expiração.
11. Compensar a reserva anterior durante replanejamento.

## Autenticação e segurança implementadas

1. Preservar a senha digitada sem remover caracteres.
2. Não armazenar senha no `localStorage`.
3. Armazenar somente dados seguros da sessão.
4. Autenticar integrações legadas por `X-CloudPort-Service-Key`.
5. Comparar credencial interna em tempo constante.
6. Aplicar roles de serviço separadas.
7. Restringir manutenção do cadastro canônico.
8. Liberar cabeçalhos de correlação no CORS.
9. Retornar `503` quando uma integração obrigatória falhar, sem mascarar como lista vazia.
10. Proteger `/api/public/v1/**` por cliente ou aplicação usando `X-CloudPort-Client-Id` e `X-CloudPort-Client-Secret`.
11. Comparar o segredo do cliente externo em tempo constante e associar a role `INTEGRACAO_EXTERNA`.

## Scheduler operacional implementado

1. Remover dados aleatórios de equipamentos, contêineres e coordenadas.
2. Exigir requisição com navio, equipamentos e posições reais.
3. Validar quantidades manifestadas.
4. Validar janela de chegada e partida.
5. Considerar conflito somente no mesmo berço.
6. Preservar duração ao deslocar slot.
7. Persistir agenda em `vessel_schedule`.
8. Calcular diagnóstico por movimentos planejados reais.
9. Restringir a API por perfil.

## Cadastro canônico de navios implementado

1. Definir Navio como fonte dos dados comuns.
2. Vincular `NavioSiderurgico` por `navioCadastroId` único.
3. Resolver cadastro por ID ou IMO.
4. Manter localmente somente a projeção operacional siderúrgica.
5. Sincronizar a projeção com o cadastro canônico.

## Monólito modular CloudPort implementado no código

### Runtime e módulos

1. Criar o runtime `backend/cloudport-monolito-navio`.
2. Incorporar Navio e Navio Siderúrgico.
3. Incorporar Yard.
4. Incorporar Gate e Rail.
5. Incorporar Autenticação e Visibilidade.
6. Manter os diretórios `servico-*` como módulos compiláveis isoladamente para rollback.
7. Produzir um único JAR executável e uma única imagem Docker.
8. Incorporar o frontend React do Control Room ao JAR.
9. Expor `GET /assets/configuracao.json` dinamicamente.

### Comunicação interna por portas

1. Extrair `CadastroNavioPorta` e implementar `CadastroNavioLocalAdapter`.
2. Manter `NavioCadastroCliente` somente como adaptador HTTP legado.
3. Transformar `OrdemPatioYardCliente` em porta e criar `OrdemPatioLocalAdapter`.
4. Transformar `PosicaoPatioYardCliente` em porta e criar `PosicaoPatioLocalAdapter`, preservando restrições e capacidade do mapa real.
5. Transformar `ClienteStatusPatio` em porta e criar `StatusPatioLocalAdapter` para Gate → Yard.
6. Transformar `AutenticacaoClient` em porta e criar `AutenticacaoLocalAdapter` para Gate → Autenticação.
7. Registrar adaptadores HTTP somente quando a propriedade de integração estiver em `http`.
8. Configurar Navio, Yard e Autenticação em modo `local` no runtime.
9. Impedir por ArchUnit que o runtime dependa de classes `*HttpAdapter`.

### Maven e empacotamento

1. Evoluir `backend/cloudport-navio-modules` para parent e reator Maven comum.
2. Centralizar Java, versões, BOMs, `dependencyManagement`, `pluginManagement` e Enforcer.
3. Incluir os sete módulos e o runtime no reator.
4. Permitir JAR de biblioteca pelo perfil `modulo-monolito` e preservar execução standalone.
5. Remover inclusão direta de fontes de projetos irmãos.
6. Publicar recursos e migrações dentro do artefato proprietário.
7. Atualizar Dockerfile para copiar e compilar todos os módulos pelo reator.
8. Incluir `cloudport-contracts` no reator Maven, no workflow e nas imagens Docker standalone e consolidada.
9. Publicar um único OpenAPI no runtime consolidado com segurança JWT e credenciais de cliente externo.
10. Garantir `operationId` único no OpenAPI consolidado.

### Schemas, ownership e Flyway

1. Usar uma conexão PostgreSQL e sete schemas proprietários:
   - `cloudport_navio`;
   - `cloudport_siderurgico`;
   - `cloudport_yard`;
   - `cloudport_gate`;
   - `cloudport_rail`;
   - `cloudport_autenticacao`;
   - `cloudport_visibilidade`.
2. Definir como proprietário o módulo que publica a migração que cria a estrutura.
3. Publicar migrações em `cloudport/migrations/<modulo>`.
4. Criar um objeto Flyway e um `flyway_schema_history` por schema.
5. Executar todos os Flyway antes do `EntityManagerFactory`.
6. Validar nomes de schema.
7. Habilitar `validateOnMigrate`, desabilitar `clean` e criar schemas quando necessário.
8. Configurar o `search_path` com os sete schemas e `public`.
9. Preservar rollback por estratégia `expand and contract`, sem downgrade automático.
10. Documentar ownership, compatibilidade e regras destrutivas.

### Infraestrutura transversal centralizada

1. Centralizar uma cadeia de segurança stateless.
2. Incorporar login e emissão de token do módulo Autenticação.
3. Centralizar JWT, roles, CORS e credencial interna transitória.
4. Centralizar Jackson com Java Time, UTC e propriedades não nulas.
5. Publicar OpenAPI consolidado.
6. Centralizar tratamento de erros com código, mensagem, detalhes, status, caminho, timestamp e `correlationId`.
7. Criar filtro de `X-Correlation-Id` e `traceId` no MDC.
8. Criar métrica HTTP central e exportação Prometheus.
9. Centralizar padrão de logs.
10. Centralizar scheduler e seu tratamento de erro.
11. Centralizar cliente HTTP para integrações externas.
12. Centralizar conversor JSON principal do RabbitMQ.
13. Excluir do runtime configurações standalone duplicadas de segurança, erros, OpenAPI, observabilidade e conversores genéricos.

### Execução única e coexistência

1. Controlar escrita por `cloudport.runtime.writes-enabled`.
2. Retornar `503` para comandos de escrita no runtime desabilitado.
3. Controlar jobs por `cloudport.runtime.jobs-enabled`.
4. Controlar consumidores por `cloudport.runtime.consumers-enabled` e `auto-startup` do RabbitMQ.
5. Manter monólito como escritor, scheduler e consumidor ativo.
6. Manter legados sem escrita, jobs e consumidores durante coexistência.
7. Serializar jobs críticos por `pg_try_advisory_xact_lock`.
8. Adicionar PostgreSQL, RabbitMQ e Redis ao Compose consolidado.
9. Manter deployments e credenciais legadas até validar paridade e rollback.

### Testes e proteção arquitetural

1. Criar teste de contexto com PostgreSQL 16 em Testcontainers.
2. Validar os sete schemas e históricos Flyway.
3. Validar ausência de migrações pendentes.
4. Validar portas locais e ausência dos adaptadores HTTP no contexto.
5. Validar uma única cadeia de segurança.
6. Validar controllers incorporados no mesmo contexto.
7. Testar exclusão mútua por advisory lock.
8. Criar testes ArchUnit contra ciclos.
9. Impedir dependência de módulo para o runtime.
10. Impedir acesso direto ao repository de outro módulo.
11. Impedir uso de adaptador HTTP pelo runtime.
12. Validar build da imagem com todos os módulos.

### Documentação e operação

1. Registrar monólito modular como arquitetura alvo.
2. Atualizar `README.md` da raiz, do runtime e do deploy.
3. Definir responsabilidades, comunicação, ownership, segurança e observabilidade.
4. Documentar corte, coexistência, critérios de aprovação e rollback.
5. Definir que rollback troca binário e roteamento sem downgrade de banco.
6. Definir que deployments, imagens e credenciais legadas só podem ser removidos após paridade, observação e ensaio de retorno.

## Contratos e integrações implementados

1. Criar `backend/cloudport-contracts` com paginação, erro padronizado, comando motivado, envelope de evento versionado e enums externos.
2. Padronizar respostas paginadas com `conteudo`, `pagina`, `tamanho`, `totalElementos`, `totalPaginas`, `primeira` e `ultima`.
3. Separar `VisitaNavioResumoDTO` do DTO detalhado da visita.
4. Separar `WorkQueuePatioResumoDTO` do DTO detalhado que contém a job list.
5. Centralizar a conversão de `WorkQueuePatioYardDTO` em `ConversorWorkQueuePatioServico`.
6. Implementar filtros no banco para fase, período, navio, código de visita, berço e linha operadora.
7. Implementar seleção segura de campos e whitelist de ordenação na API pública.
8. Padronizar erros do módulo Navio Siderúrgico com código, mensagem, detalhes, correlationId e timestamp.
9. Gerar e propagar `X-Correlation-Id` nas requisições autenticadas.
10. Criar o comando `npm run generate:api-types` com `openapi-typescript` e manter snapshot dos tipos gerados.
11. Criar envelope `EventoIntegracaoV1` com `eventId`, `eventType`, `eventVersion`, `occurredAt`, `correlationId`, `source` e `data`.
12. Publicar eventos de visita por SSE, WebSocket/STOMP e evento interno Spring.
13. Expor `GET /api/public/v1/events/stream` e `GET /visitas-navio/{id}/eventos/stream`.
14. Expor WebSocket em `/ws/integrations` e tópicos versionados em `/topic/v1`.
15. Proteger a API pública por cadastro de cliente ou aplicação configurado em `CLOUDPORT_PUBLIC_API_CLIENTS`.
16. Completar BAPLIE, COPRAR e COARRI com validação de tipo, rejeição e auditoria persistente.
17. Implementar VERMAS, converter peso em quilogramas e atualizar o VGM dos contêineres do Bay Plan.
18. Persistir status `RECEBIDO`, `PROCESSANDO`, `CONCLUIDO` e `REJEITADO` para cada processamento EDI.
19. Expor consulta paginada e detalhamento da auditoria EDI.
20. Permitir reprocessamento motivado de mensagens rejeitadas, com encadeamento da tentativa e limite de cinco execuções.
21. Retornar `X-EDI-Processing-Id` nos processamentos aceitos.

## Testes, corte, rollback e observabilidade implementados

1. Criar teste ArchUnit contra ciclo e repository de outro módulo.
2. Criar teste PostgreSQL/Testcontainers do runtime.
3. Validar Flyway e JPA reais.
4. Testar modo somente leitura com `503`.
5. Testar propriedades de consumidores RabbitMQ.
6. Criar smoke funcional do Compose.
7. Validar JWT, criação persistida e portas locais no smoke.
8. Criar runbook de corte e rollback.
9. Adicionar logs estruturados com módulo, operação, resultado, `correlationId`, `traceId`, visita, item, reserva, ordem, work queue e equipamento.
10. Propagar `X-Correlation-Id` e `traceparent` nas chamadas HTTP externas.
11. Publicar métricas de contagem e duração com tags de baixa cardinalidade.
12. Expor health, métricas e Prometheus no runtime e nos módulos operacionais.
13. Cobrir filtros de observabilidade por testes unitários.
14. Testar o parser VERMAS para quilogramas, toneladas e rejeição de mensagem sem VGM.

## Streaming do Control Room implementado

1. Expor `GET /visitas-navio/{id}/integracao-patio/stream` como SSE autenticado.
2. Exigir perfil operacional na inscrição.
3. Enviar snapshot inicial e eventos versionados `control-room.v1`.
4. Emitir heartbeat no canal.
5. Publicar atualização após comandos operacionais e reconciliação automática.
6. Substituir polling principal por SSE no frontend.
7. Manter polling somente como fallback em falha de conexão.
8. Reconectar com backoff.
9. Preservar token, `X-Correlation-Id` e `traceparent`.
10. Encerrar stream ao trocar visita ou destruir o componente.

## Reservas no pátio implementadas

1. Validar bloqueio, interdição e área permitida.
2. Validar carga, peso, altura, camada e capacidade da pilha.
3. Expor restrições em `GET /yard/patio/reservas/posicoes`.
4. Persistir validade e motivo de cancelamento.
5. Expirar reservas por job com lock PostgreSQL.
6. Cancelar reservas ao cancelar visita.
7. Cancelar reservas anteriores durante replanejamento.
8. Criar reserva nova antes de cancelar a anterior.
9. Restaurar estado anterior quando o replanejamento falha.
10. Impedir que reserva expirada seja usada para criar ordem.
11. Registrar auditoria de criação, consumo, cancelamento e expiração.
12. Expor histórico em `GET /yard/patio/reservas/auditoria`.

## Quay, berth e crane implementados

1. Criar `GET /visitas-navio/{id}/quay-monitor`.
2. Criar `POST /visitas-navio/{id}/crane-plan`.
3. Criar `GET /visitas-navio/{id}/produtividade-cais`.
4. Persistir plano de guindastes.
5. Validar existência da visita.
6. Validar período do plano.
7. Impedir equipamentos repetidos e janelas sobrepostas.
8. Consolidar work queues, porão, equipamento, POW, status e movimentos.
9. Calcular produtividade planejada e realizada.
10. Usar ordens concluídas como produção realizada.
11. Criar testes de serviço e controller.
12. Criar migração `V4__criar_plano_guindaste_visita.sql`.

## Visibilidade operacional implementada

1. Remover mapeamentos MVC duplicados que impediam inicialização.
2. Consolidar rastreamento e histórico de contêineres.
3. Persistir eventos de entrada e saída no Gate, armazenagem no Yard e movimento ferroviário.
4. Processar capacidade do Yard sem exigir `containerId`.
5. Preservar status do navio quando o evento altera somente o berço.
6. Criar projeção quando o evento chega antes do cadastro.
7. Resolver alertas de atraso após confirmação de chegada.
8. Substituir `System.out` por logging estruturado nos fluxos alterados.
9. Remover métricas fictícias dos DTOs.
10. Calcular throughput do Gate por ciclos reais.
11. Corrigir `estimadoParaida` para `estimadoParaSaida` com alias compatível.
12. Padronizar erros da API.
13. Exigir motivo para resolver alertas.
14. Externalizar banco, RabbitMQ, Redis, porta, emissor JWT e meta do Gate.
15. Desabilitar Open Session in View.
16. Incluir Autenticação, Gate, Rail e Visibilidade na matriz de validação do backend.

## Contratos de API implementados

```text
GET   /assets/configuracao.json
GET   /yard/patio/work-queues?visitaNavioId={id}
POST  /yard/patio/work-queues
PATCH /yard/patio/work-queues/{id}/ativar
PATCH /yard/patio/work-queues/{id}/desativar
PATCH /yard/patio/work-queues/{id}/pow
PATCH /yard/patio/work-queues/{id}/equipamento
PATCH /yard/patio/work-queues/{id}/ordens
GET   /yard/patio/work-queues/{id}/job-list
POST  /yard/patio/work-queues/{id}/dispatch
POST  /yard/patio/work-instructions/{id}/reset
POST  /yard/patio/work-instructions/{id}/cancelar
GET   /visitas-navio/{id}/integracao-patio/work-queues
GET   /visitas-navio/{id}/integracao-patio/stream
GET   /visitas-navio/{id}/quay-monitor
POST  /visitas-navio/{id}/crane-plan
GET   /visitas-navio/{id}/produtividade-cais
GET   /yard/patio/reservas/posicoes
GET   /yard/patio/reservas/auditoria
GET   /api/public/v1/vessel-visits
GET   /api/public/v1/vessel-visits/{id}
GET   /api/public/v1/vessel-visits/{id}/work-queues
GET   /api/public/v1/vessel-visits/{id}/work-queues/{workQueueId}
GET   /api/public/v1/events/stream
GET   /visitas-navio/{id}/eventos/stream
POST  /api/edi/baplie/upload
POST  /api/edi/baplie/texto
POST  /api/edi/coprar
POST  /api/edi/coarri
POST  /api/edi/vermas
GET   /api/edi/processamentos
GET   /api/edi/processamentos/{id}
POST  /api/edi/processamentos/{id}/reprocessar
POST  /api/scheduler/gerar-plano
GET   /api/v1/visibilidade/dashboard
GET   /api/v1/visibilidade/navios
GET   /api/v1/visibilidade/navios/{navioId}/detalhes
GET   /api/v1/visibilidade/patio/ocupacao
GET   /api/v1/visibilidade/gate/throughput
GET   /api/v1/visibilidade/alertas
POST  /api/v1/visibilidade/alertas/{id}/resolver
GET   /api/v1/visibilidade/conteiners/{containerId}/track
GET   /api/v1/visibilidade/conteiners/{containerId}/historico
GET   /api/v1/visibilidade/conteiners/buscar
```

## Itens que não devem voltar como pendência principal

1. CRUD operacional básico de visita, item e plano.
2. Integração inicial Navio + Yard.
3. Work queues, job list e ações básicas do Control Room.
4. Reconciliação agendada Yard → Navio.
5. Reserva em posição real livre e ciclo completo de bloqueio, expiração, cancelamento e compensação.
6. Autenticação do Control Room e integração ao portal.
7. Scheduler baseado em dados reais.
8. Cadastro canônico de Navio.
9. Streaming SSE do Control Room.
10. Contratos backend de quay, berth e crane.
11. Incorporação estrutural dos sete módulos no runtime.
12. Portas locais para Navio, Yard e Autenticação.
13. Parent Maven e build único.
14. Sete schemas e históricos Flyway independentes.
15. Segurança, CORS, Jackson, erros, logs, métricas, tracing e agendamento centralizados.
16. Controles de escrita, jobs e consumidores para coexistência.
17. Testes ArchUnit de ciclos e repositories entre módulos.
18. Definição de ownership e rollback Flyway.
19. Preservação dos deployments e credenciais legadas até o aceite operacional.
20. Contratos paginados, protegidos, filtráveis e versionados da API pública de Navio.
21. BAPLIE, COPRAR, COARRI e VERMAS com rejeição, auditoria e reprocessamento motivado.
