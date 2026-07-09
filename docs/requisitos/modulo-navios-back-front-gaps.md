# Requisitos pendentes - CloudPort

## Instrucoes obrigatorias para agentes de IA

Esta pasta deve manter um unico arquivo: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Nao criar `README.md`, `AGENTS.md`, relatorios de execucao automatica, historicos por data, rascunhos ou novos arquivos de requisito dentro de `docs/requisitos`. Este arquivo e o ponto unico de manutencao de tudo que ainda falta fazer.

Todo agente de IA deve atualizar este mesmo arquivo sempre que analisar, implementar ou descobrir lacunas. Itens concluidos devem ser removidos daqui e consolidados no arquivo unico de implementados (`docs/implementados/requisitos-implementados.md`). Novas lacunas encontradas na base de conhecimento, no codigo, nas APIs, nas telas do frontend, nos contratos, nos testes, nas metricas ou nas memorias de calculo devem ser adicionadas aqui.

Se existir outro arquivo dentro de `docs/requisitos`, o agente deve consolidar o conteudo util neste arquivo e remover o arquivo excedente no mesmo PR. Nao deixar conteudo duplicado entre requisitos pendentes e implementados.

## Rotina obrigatoria de atualizacao

1. Antes de desenvolver, ler este arquivo e `docs/implementados/requisitos-implementados.md`.
2. Durante a analise, manter aqui somente requisitos ainda pendentes.
3. Apos desenvolver, remover daqui tudo que foi implementado.
4. Registrar o que foi entregue em `docs/implementados/requisitos-implementados.md`.
5. Acrescentar aqui novas lacunas encontradas na base de conhecimento e no codigo, incluindo APIs, telas do frontend, contratos de API, testes, observabilidade e memorias de calculo.
6. Manter nomes de endpoints, DTOs, telas, servicos, entidades e contratos alinhados ao codigo.
7. Nao criar arquivos adicionais nesta pasta.

## Escopo deste arquivo

Este documento contem apenas o que ainda falta fazer no CloudPort para o fluxo Navio + Patio, Control Room, contratos externos, testes e observabilidade. Itens ja entregues foram removidos desta pasta e consolidados em `docs/implementados/requisitos-implementados.md`.

## Pendencias de integracao Back x Front

### A. Contratos existentes no back sem uso completo no front

1. `GET /visitas-navio/{id}/integracao-patio/work-queues`: o service Angular possui `listarWorkQueuesPatio`, mas o `AppComponent` ainda precisa importar `WorkQueuePatioDaVisita`, manter estado `workQueuesPatio`, chamar o metodo em `carregarIntegracaoPatio()` e renderizar cards de work queue persistente.
2. `GET /yard/patio/work-queues/{id}/job-list`: o backend do yard dispoe de job list por fila, mas o frontend do modulo de navio ainda precisa exibir a job list da work queue persistente.
3. `POST /yard/patio/work-queues/{id}/dispatch`: falta botao/fluxo no frontend para despachar uma work queue e exibir o resultado do dispatch.
4. `PATCH /yard/patio/work-queues/{id}/ativar` e `/desativar`: falta acao de ativar/desativar work queue na tela.
5. `PATCH /yard/patio/work-queues/{id}/pow` e `/equipamento`: falta formulario de associacao/edicao de POW, pool operacional e equipamento.
6. `POST /yard/patio/work-instructions/{id}/reset` e `/cancelar`: falta expor reset/cancelamento da work instruction no frontend.
7. `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`: falta acao de concluir/publicar plano de estiva no frontend.
8. `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`: falta edicao operacional completa de visita e item.
9. `DELETE /visitas-navio/{id}` e `DELETE /visitas-navio/{id}/itens/{itemId}`: falta fluxo de exclusao/cancelamento administrativo diferenciado.
10. `/api/public/v1/*`: falta tela de validacao/diagnostico para testar filtros, respostas, erros, `correlationId` e payloads externos.

### B. Contratos usados pelo front que ainda dependem de evolucao no back

1. `POST /visitas-navio/{id}/integracao-patio/sincronizar-status`: hoje e acionado manualmente; falta evento, callback ou job de reconciliacao para atualizar Navio automaticamente quando o Yard mudar status de ordem.
2. `POST /visitas-navio/{id}/integracao-patio/replanejar`: falta garantir uso exclusivo de mapa real, bloqueios, capacidade e cancelamento/criacao auditada de reservas.
3. `POST /visitas-navio/{id}/integracao-patio/reservas`: falta fechar consumo, cancelamento, expiracao e auditoria completa ligada ao movimento real.
4. `POST /visitas-navio/{id}/integracao-patio/gerar-ordens`: falta rastrear transicao completa da work instruction, historico de dispatch e associacao fisica a CHE/POW/pool.
5. `PATCH /visitas-navio/{id}/integracao-patio/ordens/{ordemId}/prioridade`: falta diferenciar prioridade operacional de prioridade de fetch/busca e auditar a alteracao.
6. `PATCH /visitas-navio/{id}/integracao-patio/ordens/{ordemId}/suspender` e `/retomar`: falta matriz oficial de transicao, motivo obrigatorio, usuario, auditoria e evento operacional.
7. `GET /visitas-navio/{id}/integracao-patio/filas` e `/sem-cobertura`: falta retornar separacao estruturada entre sem fila, sem POW, sem equipamento e sem job list.
8. `GET /visitas-navio/{id}/relatorio-operacional-integrado`: falta fornecer secoes exportaveis, produtividade, divergencias detalhadas e comparativo planejado x realizado.

## Pendencias especificas da tela Control Room

1. Substituir polling de 30 segundos por SSE, WebSocket ou mecanismo equivalente para visitas, ordens e work queues.
2. Criar estado e renderizacao de `workQueuesPatio` com cards por fila persistente, exibindo identificador, status, berco, porao, bloco/zona, POW, pool, equipamento, prioridade e total de ordens.
3. Permitir expandir job list da work queue com work instructions, status, sequencia, prioridade, origem/destino e acoes disponiveis.
4. Expor acoes de work queue: ativar, desativar, associar POW, associar pool/equipamento e despachar.
5. Expor acoes de work instruction: resetar, cancelar, suspender, retomar, marcar prioridade de fetch e visualizar historico.
6. Adicionar drill-down de ordem/work instruction com eventos, divergencias, reserva vinculada, item de navio e movimento de patio.
7. Separar visualmente excecoes: sem fila, sem POW, sem equipamento, sem job list, posicao invalida, reserva bloqueada e divergencia Navio x Patio.
8. Adicionar painel de CHE/job list por equipamento, mesmo que inicialmente baseado em dados persistidos e nao telemetria real.
9. Adicionar Quay Monitor inicial quando os contratos de berth/crane estiverem disponiveis.
10. Adicionar feedback por acao de backend com loading por botao, erro padronizado e `correlationId` quando existir.

## Pendencias de DTO e contrato compartilhado

1. Criar resposta paginada padronizada para listas grandes no back e adaptar o front para pagina, tamanho, total e ordenacao.
2. Padronizar enums entre backend e frontend para status de visita, item, ordem, reserva, work queue, severidade e alerta.
3. Padronizar contrato de erro com codigo, mensagem, detalhes, `correlationId` e timestamp.
4. Adicionar `usuario`, `motivo`, `origemAcao` e `correlationId` nos comandos operacionais enviados pelo frontend.
5. Criar contratos OpenAPI para o modulo Navio + Patio e gerar/validar tipos TypeScript a partir do contrato.
6. Evitar duplicacao de conversao de `WorkQueuePatioYardDTO` em controladores diferentes do modulo de navio, centralizando mapper/adapter.
7. Diferenciar DTO resumido de lista e DTO detalhado de work queue/job list para reduzir payload em atualizacoes frequentes.
8. Definir contrato de evento de frontend para SSE/WebSocket: tipo, entidade, id, `visitaNavioId`, payload resumido, versao e data.

## Pendencias de testes de integracao Back x Front

1. Testar no backend o proxy `/visitas-navio/{id}/integracao-patio/work-queues` com sucesso, falha do yard e retorno vazio controlado.
2. Testar no frontend que `carregarIntegracaoPatio()` chama tambem `listarWorkQueuesPatio()` quando a tela passar a renderizar work queues persistentes.
3. Testar compatibilidade dos DTOs `WorkQueuePatioDaVisita`, `OrdemPatioDaVisita`, `ReservaPatioNavio` e `AlertaIntegracaoNavioPatio` com respostas reais do backend.
4. Criar contract test entre `servico-navio-siderurgico` e `servico-yard` para work queues, job list, dispatch, reset e cancelamento.
5. Criar testes e2e ou componentes para a tela Control Room: filtros, event stream, expandir job list, dispatch, reset/cancelamento e exibicao de erros.
6. Criar teste de regressao para garantir que endpoints existentes no service Angular tenham acao de tela ou estejam marcados explicitamente como API tecnica.
7. Criar validacao de build para impedir divergencia entre endpoints documentados neste requisito e metodos do `SiderurgicoApiService`.

## P0 - Pendencias obrigatorias restantes

### 1. Sincronizacao real e automatica Patio -> Navio

A operacao precisa reagir automaticamente quando a ordem de patio mudar.

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

Falta fechar o ciclo completo de reserva, consumo, cancelamento, expiracao e auditoria entre `ReservaPosicaoPatioNavio`, ocupacao real, bloqueios e movimento concluido.

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

Falta evoluir para cobertura operacional real com entidade de equipamento/CHE, historico detalhado e vinculo fisico com POW/pool.

Requisitos restantes:

1. Persistir historico de dispatch, suspensao, retomada, bloqueio, reset, cancelamento e conclusao.
2. Associar work queue a porao, plano de guindaste e recurso de cais quando o modulo Quay/Crane existir.
3. Associar fila a CHE real quando o modulo de equipamentos existir.
4. Permitir marcar ordem como prioridade de busca/fetch de forma auditavel.
5. Distinguir ordens sem fila, sem POW, sem equipamento e sem job list.
6. Criar matriz oficial de transicao de work instruction.
7. Expor painel de job list por equipamento no frontend.
8. Permitir drill-down da work instruction com eventos e divergencias.

### 4. Replanejamento usando otimizacao real de patio

O replanejamento deve usar o motor real de patio, considerando ocupacao, distancia, rehandle, sequencia de navio e disponibilidade operacional.

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

Falta ligar visita, berco, recurso de cais, guindaste/equipamento e patio.

Contratos pendentes:

```text
GET  /visitas-navio/{id}/quay-monitor
POST /visitas-navio/{id}/crane-plan
PATCH /visitas-navio/{id}/crane-plan/{cranePlanId}/ativar
PATCH /visitas-navio/{id}/crane-plan/{cranePlanId}/paradas
GET  /visitas-navio/{id}/produtividade-cais
```

### 6. Control Room quase em tempo real

Tecnologia alvo:

```text
SSE ou WebSocket para /visitas-navio/{id}/stream
SSE ou WebSocket para /yard/patio/ordens/stream
SSE ou WebSocket para /yard/patio/work-queues/stream
```

### 7. Contratos externos de API, EVP e EDI

Pendencias dos contratos `/api/public/v1`:

1. Autenticacao e autorizacao por client/app.
2. Filtro por facility, terminal, visita, fase, status, periodo e pagina.
3. Campos selecionaveis quando fizer sentido.
4. Paginacao para listas grandes.
5. `correlationId` em chamadas e logs.
6. Contrato de erro padronizado.
7. Documentacao OpenAPI.

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

1. BAPLIE para inbound stowage/stow plan.
2. COPRAR para ordens/listas de carga e descarga.
3. COARRI para confirmacao de movimentos.
4. VERMAS quando houver peso verificado.
5. Contratos de importacao, validacao, rejeicao, reprocessamento e auditoria.

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

1. Log com `visitaNavioId`, `itemOperacaoNavioId`, `ordemTrabalhoPatioId`, `workQueueId` e `correlationId`.
2. Metrica de ordens criadas por visita.
3. Metrica de falhas de sincronizacao.
4. Metrica de reservas sem consumo.
5. Metrica de divergencias Navio x Patio.
6. Metrica de ordens sem cobertura.
7. Metrica de dispatch por work queue.
8. Metrica de atraso por fila/berco/bloco.
9. Tracing entre `servico-navio-siderurgico` e `servico-yard`.

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
18. Toda acao operacional exposta no frontend deve enviar payload completo com usuario, motivo quando aplicavel, origem da acao e `correlationId` quando existir.
19. A tela Control Room deve diferenciar visualmente fila derivada, work queue persistente, work instruction, job list e excecao operacional.

## Fora do escopo deste corte

1. Telemetria real de guindaste, RTG, reach stacker ou terminal tractor.
2. Dispatch direto para VMT real.
3. Motor matematico global multi-recurso.
4. Controle aduaneiro/documental completo.
5. Substituicao integral de um TOS comercial.
