# Requisito Back/Front - Pendencias restantes pos-integracao Navio + Patio

## Objetivo

Atualizar o requisito considerando o desenvolvimento mais recente do modulo Navio + Patio e a base de conhecimento N4 Vessel, Equipment Control, Control Room, EVP/API e EDI.

O primeiro corte entregou a integracao basica entre visita de navio e patio: vinculos entre item de navio, reserva, ordem, movimento e posicao de patio; endpoints `/visitas-navio/{id}/integracao-patio/*`; campos de origem/destino Navio/Patio em ordens de trabalho; painel Angular Navio + Patio com KPIs, reservas, ordens, alertas, replanejamento e relatorio integrado.

O segundo corte entregou a visao operacional inicial de filas e excecoes: o `servico-navio-siderurgico` passou a consultar o `servico-yard` para listar filas por visita e ordens sem cobertura; o frontend passou a exibir uma area Control Room com filtros de status, berco/bloco/zona, severidade, movimentos iminentes, filas/POW operacionais, ordens sem cobertura e auto-refresh por polling.

O corte atual adiciona o primeiro contrato executavel de work queues no `servico-yard`, expondo filas persistentes ou derivadas por visita, job list por fila, ativacao/desativacao, associacao de POW, pool e equipamento, dispatch basico e reset/cancelamento de work instructions. Tambem adiciona proxy no modulo de navio para listar work queues da visita e contratos publicos `/api/public/v1` para visitas, stow plan, yard orders, work queues, eventos e reservas.

Este documento remove do escopo pendente o que ja foi implementado e mantem como pendencia apenas o que ainda falta para transformar o modulo em execucao operacional real aderente a fila de patio, controle de equipamento, monitoramento e integracoes externas.

## Base de conhecimento considerada

A base N4 Vessel mostra que o fluxo de navio envolve visita, descarga, preplan, planejamento de carga, plano de guindaste e inbound stow plan. Para o CloudPort, a lacuna principal continua sendo ligar plano do navio, sequencia operacional, descarga/carga, berth/quay/crane planning e execucao real no patio.

A base N4 Equipment Control mostra que a execucao depende de work queues, work instructions, job lists, CHE, pools, points of work, zone coverage, dispatch, monitoramento de progresso, cancelamento, reset, rehandle e controle de equipamento. Para o CloudPort, o corte atual cobre o contrato inicial de work queue e job list, mas CHE real, cobertura, telemetria e historico operacional completo continuam pendentes.

A base N4 Control Room mostra uma visao operacional integrada de patio, vessel information, alertas, CHE detail panel, job lists, work instructions, movimentos iminentes, Quay Monitor, uncovered moves e RTG optimization. Para o CloudPort, o painel atual cobre uma primeira visao operacional, mas ainda nao possui WebSocket/SSE real, CHE detail panel real, job list de equipamento, envio de mensagem para operador, Quay Monitor completo e drill-down operacional completo.

A base EVP/API indica que integracoes modernas devem expor contratos de API, filtros, paginacao, campos selecionaveis e sincronizacao por plataforma de eventos/dados, sem depender de acesso direto ao banco. Para o CloudPort, o corte atual abriu os contratos `/api/public/v1`; ainda faltam paginacao, fields selecionaveis, autenticacao por client/app, correlationId e contrato de erro padronizado.

A base EDI indica que mensagens como BAPLIE, COPRAR, COARRI e fluxos SFTP/EDI sao parte natural da operacao de navio. Para o CloudPort, EDI permanece evolucao: ainda falta criar ou atualizar visita, stow plan, reservas, ordens e eventos operacionais a partir desses fluxos.

## O que nao deve voltar como pendencia

Nao reabrir como requisito principal:

1. Criar visita de navio.
2. Criar item operacional de embarque, descarga e restow.
3. Criar plano de estiva por visita.
4. Criar eventos e resumo operacional da visita.
5. Criar endpoints basicos `/visitas-navio`.
6. Criar endpoints basicos `/visitas-navio/{id}/integracao-patio`.
7. Criar entidade simples de reserva de patio vinculada ao item.
8. Adicionar campos de integracao em `ItemOperacaoNavio`.
9. Adicionar campos de visita/item/plano em `OrdemTrabalhoPatio`.
10. Criar painel Angular inicial Navio + Patio.
11. Criar alertas basicos de integracao.
12. Criar relatorio operacional integrado basico.
13. Criar ordem real no `servico-yard` por endpoint interno `/yard/patio/ordens/navio`.
14. Impedir ordem ativa duplicada por `visitaNavioId + itemOperacaoNavioId` no `servico-yard`.
15. Expor no `servico-yard` filas operacionais por visita em `/yard/patio/ordens/visita-navio/{visitaNavioId}/filas`.
16. Expor no `servico-yard` ordens sem cobertura em `/yard/patio/ordens/visita-navio/{visitaNavioId}/sem-cobertura`.
17. Expor no `servico-navio-siderurgico` o proxy de filas em `/visitas-navio/{id}/integracao-patio/filas`.
18. Expor no `servico-navio-siderurgico` o proxy de excecoes em `/visitas-navio/{id}/integracao-patio/sem-cobertura`.
19. Criar contrato frontend `FilaPatioDaVisita` e consumo dos endpoints de filas/excecoes.
20. Exibir no frontend uma primeira visao Control Room com filtros, movimentos iminentes, filas/POW, ordens sem cobertura e auto-refresh por polling.
21. Criar contrato inicial de work queues de patio no `servico-yard`.
22. Listar work queues por visita via `GET /yard/patio/work-queues?visitaNavioId={id}`.
23. Criar work queue por `POST /yard/patio/work-queues`.
24. Ativar e desativar work queue.
25. Associar POW, pool operacional e equipamento a work queue.
26. Expor job list de work queue.
27. Executar dispatch basico de work queue para ordens pendentes.
28. Resetar e cancelar work instructions pelo contrato de patio.
29. Expor work queues da visita no modulo de navio em `/visitas-navio/{id}/integracao-patio/work-queues`.
30. Criar contratos publicos iniciais `/api/public/v1/vessel-visits`, stow plan, yard orders, work queues, events e yard reservations.
31. Adicionar contrato TypeScript `WorkQueuePatioDaVisita` e metodo frontend para consultar work queues.

## Contratos de API implementados neste corte

### Yard

```text
GET  /yard/patio/work-queues?visitaNavioId={id}
POST /yard/patio/work-queues
PATCH /yard/patio/work-queues/{id}/ativar
PATCH /yard/patio/work-queues/{id}/desativar
PATCH /yard/patio/work-queues/{id}/pow
PATCH /yard/patio/work-queues/{id}/equipamento
GET  /yard/patio/work-queues/{id}/job-list
POST /yard/patio/work-queues/{id}/dispatch
POST /yard/patio/work-instructions/{id}/reset
POST /yard/patio/work-instructions/{id}/cancelar
```

Contrato resumido de `WorkQueuePatioRespostaDto`:

```json
{
  "id": 1,
  "identificador": "VISITA-10|B1|A|POW-01",
  "agrupamento": "WORK_QUEUE_PATIO",
  "visitaNavioId": 10,
  "berco": "B1",
  "porao": 2,
  "blocoZona": "A",
  "sequenciaInicial": 1,
  "pow": "POW-01",
  "poolOperacional": "POOL-RTG",
  "equipamento": "RTG-01",
  "status": "ATIVA",
  "prioridadeOperacional": 1,
  "totalOrdens": 4,
  "jobList": []
}
```

### Navio Siderurgico

```text
GET /visitas-navio/{id}/integracao-patio/work-queues
```

Retorna work queues persistentes ou derivadas da visita, consultando o `servico-yard` quando disponivel.

### Public API v1

```text
GET /api/public/v1/vessel-visits
GET /api/public/v1/vessel-visits/{id}
GET /api/public/v1/vessel-visits/{id}/stow-plan
GET /api/public/v1/vessel-visits/{id}/yard-orders
GET /api/public/v1/vessel-visits/{id}/work-queues
GET /api/public/v1/vessel-visits/{id}/events
GET /api/public/v1/yard/orders?visitaNavioId={id}&status={status}
GET /api/public/v1/yard/reservations?visitaNavioId={id}
```

## Telas e contratos de frontend implementados neste corte

O frontend `frontend/servico-navio-siderurgico` recebeu o contrato `WorkQueuePatioDaVisita` e o metodo `listarWorkQueuesPatio(visitaId)`, apontando para o proxy de navio. A tela Control Room continua exibindo o painel operacional de filas, movimentos iminentes, ordens, reservas, alertas e excecoes; a proxima evolucao deve trocar os agrupamentos exibidos por cards completos de work queue persistente, com POW, pool, equipamento e job list expandivel.

## Levantamento atual de pendencias de integracao Back x Front

Esta lista consolida as lacunas encontradas entre os contratos de backend ja expostos e o que o frontend realmente consome ou permite operar hoje. O objetivo e transformar pendencias soltas em backlog rastreavel por tela, contrato e acao de usuario.

### A. Contratos existentes no back sem uso completo no front

1. `GET /visitas-navio/{id}/integracao-patio/work-queues`: o service Angular possui o metodo `listarWorkQueuesPatio`, mas o `AppComponent` ainda nao importa `WorkQueuePatioDaVisita`, nao mantem estado `workQueuesPatio`, nao chama o metodo em `carregarIntegracaoPatio()` e nao renderiza cards de work queue persistente.
2. `GET /yard/patio/work-queues/{id}/job-list`: o backend do yard dispoe de job list por fila, mas o frontend do modulo de navio ainda exibe apenas agrupamentos antigos de `FilaPatioDaVisita`, sem expandir a job list da work queue persistente.
3. `POST /yard/patio/work-queues/{id}/dispatch`: o backend ja possui dispatch basico, mas nao ha botao/fluxo no frontend para despachar uma work queue nem exibir o resultado do dispatch.
4. `PATCH /yard/patio/work-queues/{id}/ativar` e `/desativar`: o backend ja altera status da fila, mas a tela nao oferece ativar/desativar work queue.
5. `PATCH /yard/patio/work-queues/{id}/pow` e `/equipamento`: o backend ja permite associar POW, pool e equipamento, mas a tela nao possui formulario de associacao/edicao.
6. `POST /yard/patio/work-instructions/{id}/reset` e `/cancelar`: o backend ja possui reset/cancelamento, mas o front so opera prioridade, suspensao e retomada da ordem; falta expor reset/cancelamento da work instruction.
7. `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`: o backend possui conclusao de plano de estiva, mas o frontend so cria, edita posicoes e valida; falta acao de concluir/publicar plano.
8. `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`: o backend possui atualizacao completa de visita e item, mas o frontend so cria e altera status/bloqueio; falta edicao operacional completa.
9. `DELETE /visitas-navio/{id}` e `DELETE /visitas-navio/{id}/itens/{itemId}`: o backend possui exclusao, mas o frontend nao possui fluxo de exclusao/cancelamento administrativo diferenciado.
10. `/api/public/v1/*`: contratos publicos existem no backend para integracao externa, mas nao ha tela de validacao/diagnostico no front para testar filtros, resposta, erros, correlationId ou payloads externos.

### B. Contratos usados pelo front que ainda dependem de evolucao no back

1. `POST /visitas-navio/{id}/integracao-patio/sincronizar-status`: hoje e acionado manualmente pela tela; falta evento/callback/job de reconciliacao para atualizar Navio automaticamente quando o Yard mudar status de ordem.
2. `POST /visitas-navio/{id}/integracao-patio/replanejar`: o front permite simular/aplicar, mas o back ainda precisa garantir uso exclusivo de mapa real, bloqueios, capacidade e cancelamento/criacao auditada de reservas.
3. `POST /visitas-navio/{id}/integracao-patio/reservas`: o front gera reservas, mas falta no back fechar consumo, cancelamento, expiracao e auditoria completa ligada ao movimento real.
4. `POST /visitas-navio/{id}/integracao-patio/gerar-ordens`: o front gera ordens, mas falta rastrear a transicao completa da work instruction, historico de dispatch e associacao fisica a CHE/POW/pool.
5. `PATCH /visitas-navio/{id}/integracao-patio/ordens/{ordemId}/prioridade`: o front permite priorizar, mas falta diferenciar prioridade operacional de prioridade de fetch/busca e auditar a alteracao.
6. `PATCH /visitas-navio/{id}/integracao-patio/ordens/{ordemId}/suspender` e `/retomar`: o front opera suspensao/retomada, mas falta matriz oficial de transicao, motivo obrigatorio, usuario, auditoria e evento operacional.
7. `GET /visitas-navio/{id}/integracao-patio/filas` e `/sem-cobertura`: o front consome, mas a separacao entre sem fila, sem POW, sem equipamento e sem job list ainda precisa vir estruturada do back.
8. `GET /visitas-navio/{id}/relatorio-operacional-integrado`: o front apenas mostra contadores resumidos; o back ainda precisa fornecer secoes exportaveis, produtividade, divergencias detalhadas e comparativo planejado x realizado.

### C. Pendencias especificas da tela Control Room

1. Substituir o polling de 30 segundos por SSE/WebSocket ou mecanismo equivalente para visitas, ordens e work queues.
2. Criar estado e renderizacao de `workQueuesPatio` com cards por fila persistente, exibindo identificador, status, berco, porao, bloco/zona, POW, pool, equipamento, prioridade e total de ordens.
3. Permitir expandir job list da work queue com work instructions, status, sequencia, prioridade, origem/destino e acoes disponiveis.
4. Expor acoes de work queue: ativar, desativar, associar POW, associar pool/equipamento e despachar.
5. Expor acoes de work instruction: resetar, cancelar, suspender, retomar, marcar prioridade de fetch e visualizar historico.
6. Adicionar drill-down de ordem/work instruction com eventos, divergencias, reserva vinculada, item de navio e movimento de patio.
7. Separar visualmente excecoes: sem fila, sem POW, sem equipamento, sem job list, posicao invalida, reserva bloqueada e divergencia Navio x Patio.
8. Adicionar painel de CHE/job list por equipamento, mesmo que inicialmente baseado em dados persistidos e nao telemetria real.
9. Adicionar Quay Monitor inicial quando os contratos de berth/crane estiverem disponiveis.
10. Adicionar feedback por acao de backend com loading por botao, erro padronizado e correlationId quando existir.

### D. Pendencias de DTO/contrato compartilhado

1. Criar resposta paginada padronizada para listas grandes no back e adaptar o front para pagina, tamanho, total e ordenacao.
2. Padronizar enums entre backend e frontend para status de visita, item, ordem, reserva, work queue, severidade e alerta.
3. Padronizar contrato de erro com codigo, mensagem, detalhes, correlationId e timestamp.
4. Adicionar `usuario`, `motivo`, `origemAcao` e `correlationId` nos comandos operacionais enviados pelo frontend.
5. Criar contratos OpenAPI para o modulo Navio + Patio e gerar/validar tipos TypeScript a partir do contrato.
6. Evitar duplicacao de conversao de `WorkQueuePatioYardDTO` em controladores diferentes do modulo de navio, centralizando mapper/adapter.
7. Diferenciar DTO resumido de lista e DTO detalhado de work queue/job list para reduzir payload em atualizacoes frequentes.
8. Definir contrato de evento de frontend para SSE/WebSocket: tipo, entidade, id, visitaNavioId, payload resumido, versao e data.

### E. Pendencias de testes de integracao Back x Front

1. Testar no backend o proxy `/visitas-navio/{id}/integracao-patio/work-queues` com sucesso, falha do yard e retorno vazio controlado.
2. Testar no frontend que `carregarIntegracaoPatio()` chama tambem `listarWorkQueuesPatio()` quando a tela passar a renderizar work queues persistentes.
3. Testar a compatibilidade dos DTOs `WorkQueuePatioDaVisita`, `OrdemPatioDaVisita`, `ReservaPatioNavio` e `AlertaIntegracaoNavioPatio` com respostas reais do backend.
4. Criar contract test entre `servico-navio-siderurgico` e `servico-yard` para work queues, job list, dispatch, reset e cancelamento.
5. Criar testes e2e ou componentes para a tela Control Room: filtros, auto-refresh/event stream, expandir job list, dispatch, reset/cancelamento e exibicao de erros.
6. Criar teste de regressao para garantir que endpoints existentes no service Angular tenham acao de tela ou estejam marcados explicitamente como API tecnica.
7. Criar validacao de build para impedir divergencia entre endpoints documentados neste requisito e metodos do `SiderurgicoApiService`.

## P0 - Pendencias obrigatorias restantes

### 1. Sincronizacao real e automatica Patio -> Navio

A sincronizacao manual existe, mas a operacao precisa reagir automaticamente quando a ordem de patio muda.

Regras minimas:

1. Quando `OrdemTrabalhoPatio` mudar para `EM_EXECUCAO`, atualizar o item para `EM_MOVIMENTO`.
2. Quando `OrdemTrabalhoPatio` mudar para `CONCLUIDA`, atualizar o item para `OPERADO`, preencher posicao real e consumir reserva.
3. Quando `OrdemTrabalhoPatio` mudar para `BLOQUEADA` ou `SUSPENSA`, refletir bloqueio operacional no item ou criar divergencia de alta severidade.
4. Quando `OrdemTrabalhoPatio` mudar para `CANCELADA`, reabrir o item conforme regra de negocio e liberar/cancelar reserva.
5. A sincronizacao deve ser idempotente.
6. A sincronizacao deve registrar evento na visita.
7. Se houver falha de comunicacao entre servicos, deve existir reconciliacao por job agendado.
8. Criar contrato de callback, evento interno ou fila para evitar depender apenas do botao manual.

Eventos sugeridos:

```text
OrdemPatioCriada
StatusOrdemPatioAlterado
ReservaPatioConsumida
ReservaPatioCancelada
MovimentoPatioConfirmado
DivergenciaNavioPatioDetectada
```

### 2. Validar e reservar contra o mapa real do patio em todo o ciclo

A ordem real no yard ja valida destino contra posicao real quando origem e navio. Ainda falta fechar o ciclo completo de reserva, consumo, cancelamento, expiracao e auditoria entre `ReservaPosicaoPatioNavio`, ocupacao real, bloqueios e movimento concluido.

Regras minimas:

1. Consultar posicoes reais do patio antes de reservar.
2. Nao reservar posicao ocupada por `ConteinerPatio` ou carga equivalente.
3. Nao reservar posicao bloqueada, interditada ou fora de area permitida.
4. Validar tipo de carga, status, peso e altura/camada quando esses dados existirem.
5. Diferenciar posicao inexistente, ocupada, bloqueada e sem capacidade.
6. Consumir a reserva quando a ordem for concluida.
7. Cancelar ou expirar reserva quando a ordem for cancelada, a visita for cancelada ou o item for replanejado.
8. Registrar auditoria de reserva criada, consumida, cancelada e expirada.
9. Impedir que replanejamento aplique reserva textual que nao exista no mapa real.

### 3. Work queues, POW, CHE e cobertura operacional real - segunda etapa

O corte atual cria o contrato inicial de work queues, job list e dispatch. Ainda falta evoluir para cobertura operacional real com entidade de equipamento/CHE, historico detalhado e vinculo fisico com POW/pool.

Requisito restante:

- Persistir historico de dispatch, suspensao, retomada, bloqueio, reset, cancelamento e conclusao.
- Associar work queue a porao, plano de guindaste e recurso de cais quando o modulo Quay/Crane existir.
- Associar fila a CHE real quando o modulo de equipamentos existir.
- Permitir marcar ordem como prioridade de busca/fetch de forma auditavel.
- Distinguir ordens sem fila, sem POW, sem equipamento e sem job list.
- Criar matriz oficial de transicao de work instruction.
- Expor painel de job list por equipamento no frontend.
- Permitir drill-down da work instruction com eventos e divergencias.

### 4. Replanejamento usando otimizacao real de patio

O replanejamento atual ainda pode sugerir posicoes artificiais no formato `RP-{visitaId}-{sequencia}`. A proxima entrega deve usar o motor real de patio, considerando ocupacao, distancia, rehandle, sequencia de navio e disponibilidade operacional.

Regras minimas:

1. Replanejar apenas item nao operado e ordem nao concluida.
2. Considerar ETA, ETB, ETD, cutoff, fase, berco e sequencia de estiva.
3. Considerar mapa real, ocupacao, bloqueios, zonas e capacidade.
4. Considerar dual-cycling quando houver embarque e descarga na mesma janela.
5. Retornar justificativa por item replanejado.
6. Retornar motivo por item nao replanejado.
7. Permitir simular antes de aplicar.
8. Ao aplicar, cancelar reservas antigas e criar novas reservas auditadas.
9. Nao sobrescrever execucao concluida.
10. Nunca aplicar posicao inexistente no mapa real.

### 5. Quay/berth/crane planning ligado ao patio

A base Vessel indica plano de guindaste e planejamento de carga/descarga. A base Control Room possui Quay Monitor. Falta ligar visita, berco, recurso de cais, guindaste/equipamento e patio.

Contratos pendentes:

```text
GET  /visitas-navio/{id}/quay-monitor
POST /visitas-navio/{id}/crane-plan
PATCH /visitas-navio/{id}/crane-plan/{cranePlanId}/ativar
PATCH /visitas-navio/{id}/crane-plan/{cranePlanId}/paradas
GET  /visitas-navio/{id}/produtividade-cais
```

### 6. Control Room quase em tempo real

A tela atual usa polling e cobre uma primeira visao operacional. Falta trocar por atualizacao real por evento e ampliar o drill-down operacional.

Tecnologia alvo:

```text
SSE ou WebSocket para /visitas-navio/{id}/stream
SSE ou WebSocket para /yard/patio/ordens/stream
SSE ou WebSocket para /yard/patio/work-queues/stream
```

### 7. Contratos externos de API, EVP e EDI

Os contratos publicos iniciais foram criados, mas ainda falta maturidade operacional.

Pendencias dos contratos `/api/public/v1`:

- Autenticacao e autorizacao por client/app.
- Filtro por facility, terminal, visita, fase, status, periodo e pagina.
- Campos selecionaveis quando fizer sentido.
- Paginacao para listas grandes.
- CorrelationId em chamadas e logs.
- Contrato de erro padronizado.
- Documentacao OpenAPI.

Eventos externos pendentes:

```text
VesselVisitCreated
VesselVisitPhaseChanged
StowPlanUpdated
YardReservationCreated
YardReservationConsumed
YardOrderCreated
YardOrderStatusChanged
YardMoveConfirmed
WorkQueueCreated
WorkQueueDispatched
VesselYardDivergenceDetected
```

EDI pendente:

- BAPLIE para inbound stowage/stow plan.
- COPRAR para ordens/listas de carga e descarga.
- COARRI para confirmacao de movimentos.
- VERMAS quando houver peso verificado.
- Contratos de importacao, validacao, rejeicao, reprocessamento e auditoria.

### 8. Testes, contratos e observabilidade

Testes minimos restantes:

1. Service test para criar ordem real de patio a partir de item de visita.
2. Service test para impedir ordem duplicada ativa.
3. Service test para reservar apenas posicao disponivel.
4. Service test para consumir reserva ao concluir ordem.
5. Service test para cancelar reserva ao cancelar ordem/visita.
6. Service test para sincronizacao idempotente Patio -> Navio.
7. Service test para filas operacionais por visita.
8. Service test para work queues, job list, ativacao/desativacao e dispatch.
9. Controller test para endpoints `/integracao-patio`.
10. Contract test entre `servico-navio-siderurgico` e `servico-yard`.
11. Frontend test para botoes de gerar reservas, gerar ordens, sincronizar, replanejar, filtrar filas, filtrar excecoes e carregar work queues.

Observabilidade minima restante:

- log com `visitaNavioId`, `itemOperacaoNavioId`, `ordemTrabalhoPatioId`, `workQueueId` e `correlationId`;
- metrica de ordens criadas por visita;
- metrica de falhas de sincronizacao;
- metrica de reservas sem consumo;
- metrica de divergencias Navio x Patio;
- metrica de ordens sem cobertura;
- metrica de dispatch por work queue;
- metrica de atraso por fila/berco/bloco;
- tracing entre `servico-navio-siderurgico` e `servico-yard`.

## P1 - Pendencias importantes apos o P0

1. Relatorios operacionais: lista de descarga por sequencia, lista de embarque por sequencia, work list por berco/porao/fila, recap planejado x realizado, divergencias de patio, produtividade por janela e exportacao CSV/PDF.
2. Permissoes e auditoria operacional: gerar reserva, gerar ordem, cancelar ordem, suspender/retomar ordem, aplicar replanejamento, forcar sincronizacao, alterar prioridade, marcar prioridade de fetch, ativar/desativar fila, associar POW/equipamento e encerrar visita com divergencia pendente.
3. Padronizacao de status entre item de navio, reserva de patio, ordem de patio, movimento de patio, fila/work queue, visita de navio e alerta/divergencia.

## P2 - Evolucoes avancadas

1. Integracao EDI operacional: BAPLIE/COPRAR/COARRI gerando ou atualizando reservas e ordens de patio automaticamente.
2. Otimizacao global Navio + Patio + Equipamento, considerando guindaste, fila, caminho, rehandle, bloco e capacidade.
3. Comparacao automatica entre plano de estiva, plano de patio e execucao realizada.
4. Previsao de gargalo por berco, porao, bloco, fila e equipamento.
5. Painel Control Room completo com yard view, vessel view, CHE detail, work instructions, alerts, quay monitor e movimentos iminentes.
6. Integracao com telemetria ou VMT real de equipamento.
7. Controle completo de lashing, estabilidade, segregacao e restricoes estruturais cruzando plano de navio com realizacao operacional.
8. Integracao EVP/event streaming com contratos versionados para consumidores externos.

## Criterios de aceite atualizados

1. Sincronizar status de ordem de patio para item de navio automaticamente.
2. Ter reconciliacao manual ou agendada para falhas de sincronizacao.
3. Consumir reserva automaticamente quando ordem for concluida.
4. Cancelar ou expirar reserva quando ordem/visita for cancelada.
5. Validar reserva contra ocupacao real do patio em todos os pontos de entrada.
6. Impedir reserva em posicao inexistente, ocupada, bloqueada ou sem capacidade.
7. Exibir ordens agrupadas por visita, berco, fila e status.
8. Exibir ordens sem cobertura/fila/equipamento como excecao operacional.
9. Listar work queues, job list e dispatch por contrato backend.
10. Replanejar usando mapa e otimizacao real de patio, nao posicao artificial fixa.
11. Control Room deve receber atualizacao por SSE/WebSocket ou mecanismo equivalente sem depender apenas de recarga manual.
12. Quay/berth/crane planning deve impactar filas e ordens pendentes.
13. Contratos externos `/api/public/v1` devem estar documentados, versionados, paginados e protegidos por autenticacao/autorizacao.
14. Contratos EDI devem validar, rejeitar, reprocessar e auditar mensagens.
15. Testes de service, controller, contrato e frontend devem cobrir o fluxo Navio + Patio.
16. Logs e metricas devem permitir rastrear visita, item, reserva, ordem, fila, work queue e movimento.
17. Todo contrato backend usado pelo modulo Navio + Patio deve possuir consumo de frontend, teste de contrato ou justificativa de endpoint tecnico.
18. Toda acao operacional exposta no frontend deve enviar payload completo com usuario, motivo quando aplicavel, origem da acao e correlationId quando existir.
19. A tela Control Room deve diferenciar visualmente fila derivada, work queue persistente, work instruction, job list e excecao operacional.

## Fora do escopo deste corte

- Telemetria real de guindaste, RTG, reach stacker ou terminal tractor.
- Dispatch direto para VMT real.
- Motor matematico global multi-recurso.
- Controle aduaneiro/documental completo.
- Substituicao integral de um TOS comercial.

O proximo corte deve priorizar sincronizacao automatica Patio -> Navio, consumo/cancelamento real de reservas, replanejamento com mapa real, WebSocket/SSE e drill-down completo de work queue no Control Room.
