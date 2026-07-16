# Requisitos pendentes - CloudPort

## Instrucoes obrigatorias para agentes de IA

Esta pasta deve manter um unico arquivo: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Nao criar outros arquivos de requisito, relatorios de execucao, historicos ou rascunhos nesta pasta. Itens concluidos devem sair deste arquivo e ser registrados em `docs/implementados/requisitos-implementados.md`.

Antes de desenvolver, ler os dois arquivos. Depois de desenvolver, remover daqui o que foi entregue, registrar a entrega no arquivo de implementados e acrescentar novas lacunas encontradas em APIs, telas, contratos, testes, observabilidade e memorias de calculo.

## Pendencias de integracao Back x Front

1. Expor no frontend a conclusao/publicacao do plano por `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`.
2. Completar edicao de visita e item pelos contratos `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`.
3. Criar cancelamento administrativo diferenciado para visita e item.
4. Criar tela de diagnostico dos contratos `/api/public/v1/*`.
5. Integrar o motor real de otimizacao ao endpoint de replanejamento da visita.
6. Separar em `/filas` e `/sem-cobertura` as causas sem fila, sem POW, sem equipamento e sem job list.
7. Evoluir o relatorio integrado com produtividade, divergencias detalhadas, planejado x realizado e exportacao.

## Pendencias do Control Room

1. Substituir o polling de 30 segundos por SSE ou WebSocket. O carregamento atual ja e paralelo, atomico e protegido contra sobreposicao, mas continua baseado em polling.
2. Criar drill-down da work instruction com eventos, auditoria, divergencias, reserva, item de navio e movimento de patio.
3. Diferenciar visualmente sem fila, sem POW, sem equipamento, sem job list, posicao invalida, reserva bloqueada e divergencia Navio x Patio.
4. Criar painel de CHE/job list por equipamento.
5. Criar Quay Monitor quando os contratos de berth/crane estiverem disponiveis.
6. Expandir para os demais backends o contrato de erro com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp ja aplicado no `servico-visibilidade`.
7. Criar e2e para login/SSO, job list, dispatch, reset, cancelamento e indisponibilidade do Yard.

## Pendencias de contratos compartilhados

1. Padronizar paginacao para listas grandes.
2. Padronizar enums de visita, item, ordem, reserva, work queue, severidade e alerta.
3. Tornar `motivo` obrigatorio nos comandos de cancelamento, suspensao, retomada, reset e alteracoes administrativas. A resolucao de alerta da visibilidade ja exige motivo.
4. Gerar tipos TypeScript a partir de OpenAPI.
5. Centralizar conversao de `WorkQueuePatioYardDTO`.
6. Separar DTO resumido de lista e DTO detalhado de job list.
7. Definir contrato versionado de evento para SSE/WebSocket.

## Pendencias do servico de visibilidade

1. Persistir `eventId` ou `messageId` e impedir duplicacao de historico em redelivery do RabbitMQ.
2. Substituir buscas com `repository.findAll()` e filtros em memoria por consultas paginadas no banco.
3. Publicar atualizacao do dashboard imediatamente apos eventos de gate, patio, rail e navio, mantendo o agendamento apenas como reconciliacao.
4. Persistir quantidades reais de movimentos, produtividade, equipamentos alocados e previsao de saida antes de preencher esses campos nos contratos.
5. Integrar a visibilidade ao contrato compartilhado de enums e eventos versionados.
6. Criar teste de contexto com PostgreSQL, RabbitMQ e mapeamentos Spring reais, alem dos testes unitarios atuais.

## Pendencias da consolidacao em monolito modular

O runtime `backend/cloudport-monolito-navio` ja executa `servico-navio` e `servico-navio-siderurgico` no mesmo processo, consome os dois como modulos Maven reais, carrega as migracoes publicadas pelos proprios artefatos, incorpora o frontend Angular, expoe configuracao dinamica, possui Compose com perfis unificado e legado, usa a porta local do cadastro canonico, migra separadamente os dois schemas, possui seguranca centralizada e inicia com PostgreSQL real em teste cobrindo todos os repositorios JPA. Ainda falta:

1. Remover os deployments legados somente depois de validar paridade dos endpoints, jobs agendados, seguranca, frontend e integracao com o Yard.
2. Substituir outras chamadas HTTP entre modulos incorporados por portas locais, mantendo adaptadores HTTP apenas durante a transicao.
3. Criar testes de arquitetura que impecam acesso direto indevido entre modulos e preservem os limites de dominio.
4. Avaliar a incorporacao do Yard somente depois de estabilizar Navio + Navio Siderurgico no mesmo processo.
5. Definir estrategia definitiva de rollback e compatibilidade do historico Flyway antes de apontar ambientes existentes para o runtime unificado.
6. Centralizar versoes e `pluginManagement` em um parent Maven compartilhado, sem transformar os limites de dominio em dependencias ciclicas.

## P0 - Pendencias obrigatorias restantes

### 1. Eventos Patio -> Navio

A reconciliacao automatica e idempotente por job ja atualiza item, posicao real e reserva. Falta substituir a consulta periodica por callback, evento ou fila para reduzir latencia.

Eventos alvo:

```text
OrdemPatioCriada
StatusOrdemPatioAlterado
ReservaPatioConsumida
ReservaPatioCancelada
MovimentoPatioConfirmado
DivergenciaNavioPatioDetectada
```

### 2. Fechar o ciclo de reserva no mapa real

Ja entregue: consulta de posicoes reais, rejeicao de posicao inexistente ou ocupada, rejeicao de reserva ativa duplicada, remocao de posicao textual artificial, consumo ao concluir ordem e cancelamento ao cancelar ordem.

Ainda falta:

1. Validar bloqueio, interdicao e area permitida.
2. Validar tipo de carga, peso, altura/camada e capacidade da pilha.
3. Expirar reserva por prazo configuravel.
4. Cancelar reserva ao cancelar visita ou replanejar item.
5. Persistir auditoria especifica de reserva criada, consumida, cancelada e expirada.
6. Aplicar replanejamento com reserva nova e compensacao da reserva anterior.

### 3. Work queues e cobertura operacional

Ja entregue: vinculo persistente `workQueueId`, endpoint `PATCH /yard/patio/work-queues/{id}/ordens`, auditoria de criacao/status/POW/equipamento/vinculo/dispatch/reset/cancelamento e limite real no dispatch.

Ainda falta:

1. Auditar suspensao, retomada, bloqueio e conclusao.
2. Associar work queue a porao, plano de guindaste e recurso de cais.
3. Associar fila a CHE real.
4. Auditar prioridade de fetch/busca separadamente da prioridade operacional.
5. Criar matriz oficial de transicao de work instruction.
6. Expor painel de job list por equipamento e drill-down completo.

### 4. Replanejamento real

O scheduler nao gera mais equipamentos, conteineres ou coordenadas aleatorias e exige dados operacionais reais. Falta conectar esse contrato ao replanejamento da visita, considerando ETA, ETB, ETD, cutoff, mapa, bloqueios, capacidade, dual-cycling e rehandle.

### 5. Quay/berth/crane

```text
GET  /visitas-navio/{id}/quay-monitor
POST /visitas-navio/{id}/crane-plan
GET  /visitas-navio/{id}/produtividade-cais
```

### 6. Contratos externos e EDI

1. Proteger `/api/public/v1` por client/app.
2. Implementar filtros, paginacao, campos selecionaveis, `correlationId`, erro padronizado e OpenAPI.
3. Implementar eventos externos versionados de visita, estiva, reserva, ordem, movimento e work queue.
4. Completar BAPLIE, COPRAR, COARRI e VERMAS com validacao, rejeicao, reprocessamento e auditoria.

### 7. Testes e observabilidade

1. Testar o proxy de work queues com sucesso, retorno vazio legitimo e falha do Yard convertida em `503`.
2. Criar contract tests entre `servico-navio-siderurgico`, `servico-yard` e `servico-navio`, incluindo `X-CloudPort-Service-Key`.
3. Testar vinculo `workQueueId`, limite de dispatch, auditoria e autorizacao por perfil.
4. Testar reserva contra mapa real: inexistente, ocupada, ja reservada e mapa vazio.
5. Criar e2e do fluxo operacional completo.
6. Adicionar logs estruturados, metricas e tracing com visita, item, reserva, ordem, work queue e `correlationId`.
7. Criar smoke test da imagem iniciada pelo Compose, cobrindo frontend, configuracao dinamica, autenticacao e conexao com o Yard.
8. Criar teste de contexto do `servico-visibilidade` com PostgreSQL, RabbitMQ e todos os mapeamentos de controller.

## P1

1. Relatorios operacionais e exportacao CSV/PDF.
2. Completar permissoes e auditoria de reservas, ordens, replanejamento, sincronizacao e prioridades.
3. Padronizar status entre Navio, Patio, work queue e alertas.
4. Substituir a sincronizacao periodica da projecao siderurgica do cadastro canonico por evento.
5. Concluir idempotencia, consultas paginadas e publicacao orientada a eventos no `servico-visibilidade`.

## P2

1. Integracao EDI operacional atualizando reservas e ordens automaticamente.
2. Otimizacao global Navio + Patio + Equipamento.
3. Comparacao automatica entre estiva, patio e execucao.
4. Previsao de gargalos por berco, porao, bloco, fila e equipamento.
5. Control Room completo com yard view, vessel view, CHE detail, alerts e quay monitor.
6. Telemetria/VMT real.
7. Lashing, estabilidade, segregacao e restricoes estruturais.
8. EVP/event streaming versionado.

## Criterios de aceite pendentes

1. Impedir reserva bloqueada, sem capacidade ou incompatível com a carga.
2. Expirar e auditar reservas automaticamente.
3. Replanejar usando mapa e otimizacao real.
4. Atualizar o Control Room por eventos, sem polling.
5. Integrar quay/berth/crane as filas e ordens.
6. Padronizar, versionar, paginar e proteger contratos externos.
7. Cobrir o fluxo por testes de service, controller, contrato e frontend.
8. Rastrear o fluxo por logs, metricas e tracing.
9. Exigir motivo e usuario autenticado nas acoes aplicaveis.
10. Diferenciar fila derivada, work queue persistente, work instruction, job list e excecao operacional.

## Fora do escopo deste corte

1. Telemetria real de equipamentos.
2. Dispatch direto para VMT real.
3. Motor matematico global multi-recurso.
4. Controle aduaneiro/documental completo.
5. Substituicao integral de um TOS comercial.
