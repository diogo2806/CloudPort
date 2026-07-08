# Requisito Back/Front - Pendencias restantes pos-integracao Navio + Patio

## Objetivo

Atualizar o requisito considerando o desenvolvimento mais recente do modulo Navio + Patio e a base de conhecimento N4 Vessel, Equipment Control, Control Room, EVP/API e EDI.

O primeiro corte ja entregou a integracao basica entre visita de navio e patio: vinculos entre item de navio, reserva, ordem, movimento e posicao de patio; endpoints `/visitas-navio/{id}/integracao-patio/*`; campos de origem/destino Navio/Patio em ordens de trabalho; painel Angular Navio + Patio com KPIs, reservas, ordens, alertas, replanejamento e relatorio integrado.

O corte atual acrescentou a visao operacional de filas e excecoes: o `servico-navio-siderurgico` passou a consultar o `servico-yard` para listar filas por visita e ordens sem cobertura; o frontend passou a exibir uma area Control Room com filtros de status, berco/bloco/zona, severidade, movimentos iminentes, filas/POW operacionais, ordens sem cobertura e auto-refresh por polling.

Este documento remove do escopo pendente o que ja foi implementado e mantem como pendencia apenas o que ainda falta para transformar o modulo em execucao operacional real, aderente a fila de patio, controle de equipamento, monitoramento e integracoes externas.

## Base de conhecimento considerada

A base N4 Vessel mostra que o fluxo de navio envolve visita, descarga, preplan, planejamento de carga, plano de guindaste e inbound stow plan. Para o CloudPort, a lacuna principal continua sendo ligar plano do navio, sequencia operacional, descarga/carga, berth/quay/crane planning e execucao real no patio.

A base N4 Equipment Control mostra que a execucao depende de work queues, work instructions, job lists, CHE, pools, points of work, zone coverage, dispatch, monitoramento de progresso, cancelamento, reset, rehandle e controle de equipamento. Para o CloudPort, a integracao precisa evoluir de uma ordem persistida para uma ordem executavel e monitoravel por fila, cobertura e equipamento.

A base N4 Control Room mostra uma visao operacional integrada de patio, vessel information, alertas, CHE detail panel, job lists, work instructions, movimentos iminentes, Quay Monitor, uncovered moves e RTG optimization. Para o CloudPort, o painel atual cobre uma primeira visao operacional, mas ainda nao possui WebSocket/SSE real, CHE real, job list de equipamento, dispatch, Quay Monitor completo e drill-down operacional completo.

A base EVP/API indica que integracoes modernas devem expor contratos de API, filtros, paginacao, campos selecionaveis e sincronizacao por plataforma de eventos/dados, sem depender de acesso direto ao banco. Para o CloudPort, os contratos externos ainda precisam ser formalizados para consumo por TOS, BI, EDI, EVP ou sistemas parceiros.

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

Esses itens ja foram cobertos. As pendencias abaixo sao de maturidade operacional, consistencia entre servicos, sincronizacao automatica, contratos externos e aderencia ao fluxo real de execucao.

## Contratos de API implementados neste corte

### Navio Siderurgico

```text
GET /visitas-navio/{id}/integracao-patio/filas
```

Retorna filas operacionais da visita, consumindo o `servico-yard` quando disponivel e usando agrupamento local como fallback.

Contrato resumido:

```json
[
  {
    "identificador": "B1|A|PENDENTE",
    "agrupamento": "VISITA_BERCO_ZONA_STATUS",
    "visitaNavioId": 10,
    "berco": "B1",
    "blocoZona": "A",
    "sequenciaInicial": 1,
    "status": "PENDENTE",
    "totalOrdens": 4,
    "ordens": [
      {
        "id": 100,
        "visitaNavioId": 10,
        "itemOperacaoNavioId": 50,
        "codigoLote": "LOTE-01",
        "tipoMovimento": "DESCARGA",
        "statusOrdem": "PENDENTE",
        "origem": "NAVIO",
        "destino": "B1",
        "posicaoPlanejada": "1-1-A",
        "posicaoReal": null,
        "sequenciaNavio": 1,
        "prioridadeOperacional": 1
      }
    ]
  }
]
```

```text
GET /visitas-navio/{id}/integracao-patio/sem-cobertura
```

Retorna ordens da visita sem cobertura operacional minima, por exemplo sem destino/cobertura, sem prioridade ou sem sequencia.

### Yard

```text
GET /yard/patio/ordens/visita-navio/{visitaNavioId}/filas
GET /yard/patio/ordens/visita-navio/{visitaNavioId}/sem-cobertura
PATCH /yard/patio/ordens/{id}/prioridade
PATCH /yard/patio/ordens/{id}/suspender
PATCH /yard/patio/ordens/{id}/retomar
POST  /yard/patio/ordens/navio
```

Esses contratos ja existem ou foram integrados no fluxo Navio + Patio. O requisito restante deve evoluir a semantica dessas APIs, nao recria-las.

## Telas de frontend implementadas neste corte

### Painel Navio + Patio / Control Room

A tela Angular `frontend/servico-navio-siderurgico/src/app/app.component.html` passou a exibir:

- KPIs de itens, reservas, ordens, execucao e ordens sem cobertura.
- Filtro por status da ordem.
- Filtro por berco/bloco/zona.
- Filtro por severidade de alerta.
- Botao de atualizacao manual.
- Auto-refresh por polling.
- Movimentos iminentes ordenados por sequencia operacional.
- Filas/POW operacionais por visita.
- Ordens sem cobertura.
- Reservas de patio.
- Ordens de patio com sequencia e prioridade.
- Alertas e divergencias filtraveis.
- Replanejamento e relatorio integrado.

Esta tela ainda nao substitui um Control Room completo, pois nao tem WebSocket/SSE real, CHE detail panel real, job list por equipamento, envio de mensagem para operador, Quay Monitor real, yard view grafico ou telemetria.

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

### 3. Work queues, POW, CHE e cobertura operacional real

O corte atual exibe agrupamentos de fila e ordens sem cobertura, mas ainda nao modela CHE real, pool, POW, zona, dispatch ou job list executavel.

Requisito minimo:

- Criar entidade ou contrato de `WorkQueuePatio` quando o agrupamento deixar de ser apenas derivado.
- Associar fila a visita, berco/cais, porao, sequencia do plano de estiva e bloco/zona de patio.
- Associar fila a POW e pool operacional.
- Associar cobertura de equipamento/CHE quando o modulo de equipamentos existir.
- Permitir prioridade operacional da ordem.
- Permitir marcar ordem como prioridade de busca/fetch.
- Permitir listar ordens descobertas, sem equipamento/cobertura definida.
- Permitir filtro por `PENDENTE`, `EM_EXECUCAO`, `BLOQUEADA`, `SUSPENSA`, `CONCLUIDA` e `CANCELADA`.
- Criar historico de dispatch, suspensao, retomada, bloqueio e conclusao.

Endpoints a manter/evoluir:

```text
GET  /yard/patio/ordens/visita-navio/{visitaNavioId}/filas
GET  /yard/patio/ordens/visita-navio/{visitaNavioId}/sem-cobertura
PATCH /yard/patio/ordens/{id}/prioridade
PATCH /yard/patio/ordens/{id}/suspender
PATCH /yard/patio/ordens/{id}/retomar
```

Endpoints ainda pendentes:

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

Contrato pendente:

```text
POST /visitas-navio/{id}/integracao-patio/replanejar
```

Deve evoluir para retornar justificativa estruturada por item:

```json
{
  "reservasSugeridas": [],
  "ordensReordenadas": [],
  "economiaEstimadaDistanciaPercentual": 12.5,
  "riscoRehandle": "MEDIO",
  "alertasImpeditivos": [],
  "itensNaoReplanejados": [],
  "justificativas": [
    {
      "itemOperacaoNavioId": 50,
      "posicaoAnterior": "A-1-1-A",
      "posicaoSugerida": "B-2-3-A",
      "motivo": "Menor distancia ate berco e sem ocupacao conflitante"
    }
  ]
}
```

### 5. Quay/berth/crane planning ligado ao patio

A base Vessel indica plano de guindaste e planejamento de carga/descarga. A base Control Room possui Quay Monitor. Falta ligar visita, berco, recurso de cais, guindaste/equipamento e patio.

Requisito minimo:

- Vincular visita a berco operacional.
- Registrar janela planejada e realizada de trabalho no cais.
- Associar recurso de cais, guindaste/equipamento e equipe.
- Associar filas de patio a porao/berco/recurso.
- Medir produtividade planejada x realizada.
- Registrar atraso, parada e motivo.
- Mostrar impacto do atraso no patio e nas ordens pendentes.
- Criar visao de Quay Monitor no frontend.

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

Requisito minimo de frontend:

- Atualizar status de visita, item, reserva e ordem sem recarregar a pagina inteira.
- Usar WebSocket ou SSE em vez de apenas polling.
- Exibir movimentos iminentes da visita com origem do backend.
- Exibir ordens sem cobertura de equipamento/fila.
- Exibir alertas com severidade, responsavel e status de tratamento.
- Permitir filtro por berco, visita, fase, status, bloco, tipo de movimento e severidade.
- Exibir drill-down da ordem: item, reserva, posicao planejada, posicao real, eventos e divergencias.
- Exibir quadro de execucao por fila/POW/equipamento quando houver dados.
- Exibir CHE detail panel quando existir modulo de equipamento.
- Exibir Quay Monitor quando existir plano de guindaste.

Tecnologia alvo:

```text
SSE ou WebSocket para /visitas-navio/{id}/stream
SSE ou WebSocket para /yard/patio/ordens/stream
```

### 7. Contratos externos de API, EVP e EDI

Ainda falta formalizar contratos externos para integracao com TOS, BI, EVP, EDI ou sistemas parceiros.

Contratos REST externos pendentes:

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

Requisitos desses contratos:

- Autenticacao e autorizacao por client/app.
- Filtro por facility, terminal, visita, fase, status, periodo e pagina.
- Campos selecionaveis quando fizer sentido.
- Paginacao para listas grandes.
- CorrelationId em chamadas e logs.
- Contrato de erro padronizado.
- Versionamento `/v1`.

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
VesselYardDivergenceDetected
```

EDI pendente:

- BAPLIE para inbound stowage/stow plan.
- COPRAR para ordens/listas de carga e descarga.
- COARRI para confirmacao de movimentos.
- VERMAS quando houver peso verificado.
- Contratos de importacao, validacao, rejeicao, reprocessamento e auditoria.

### 8. Testes, contratos e observabilidade

O desenvolvimento recente informou que os testes nao foram executados localmente por limitacao de ambiente. Antes de considerar o modulo pronto, faltam testes e observabilidade.

Testes minimos:

1. Service test para criar ordem real de patio a partir de item de visita.
2. Service test para impedir ordem duplicada ativa.
3. Service test para reservar apenas posicao disponivel.
4. Service test para consumir reserva ao concluir ordem.
5. Service test para cancelar reserva ao cancelar ordem/visita.
6. Service test para sincronizacao idempotente Patio -> Navio.
7. Service test para filas operacionais por visita.
8. Service test para ordens sem cobertura.
9. Controller test para endpoints `/integracao-patio`.
10. Contract test entre `servico-navio-siderurgico` e `servico-yard`.
11. Frontend test para botoes de gerar reservas, gerar ordens, sincronizar, replanejar, filtrar filas e filtrar excecoes.

Observabilidade minima:

- log com `visitaNavioId`, `itemOperacaoNavioId`, `ordemTrabalhoPatioId` e `correlationId`;
- metrica de ordens criadas por visita;
- metrica de falhas de sincronizacao;
- metrica de reservas sem consumo;
- metrica de divergencias Navio x Patio;
- metrica de ordens sem cobertura;
- metrica de atraso por fila/berco/bloco;
- tracing entre `servico-navio-siderurgico` e `servico-yard`.

## P1 - Pendencias importantes apos o P0

### 1. Relatorios operacionais em formato de trabalho

O relatorio integrado basico existe, mas faltam saidas operacionais:

- lista de descarga por sequencia;
- lista de embarque por sequencia;
- work list por berco/porão/fila;
- recap planejado x realizado;
- divergencias de patio;
- produtividade por janela;
- exportacao CSV/PDF.

### 2. Permissoes e auditoria operacional

Definir permissoes para:

- gerar reserva;
- gerar ordem;
- cancelar ordem;
- suspender/retomar ordem;
- aplicar replanejamento;
- forcar sincronizacao;
- alterar prioridade;
- marcar prioridade de fetch;
- ativar/desativar fila;
- associar POW/equipamento;
- encerrar visita com divergencia pendente.

### 3. Padronizacao de status entre servicos

Hoje existem status de item, status de integracao, status de reserva e status de ordem. Falta uma matriz oficial de transicao para evitar interpretacoes divergentes.

A matriz deve cobrir:

- item de navio;
- reserva de patio;
- ordem de patio;
- movimento de patio;
- fila/work queue;
- visita de navio;
- alerta/divergencia.

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
9. Replanejar usando mapa e otimizacao real de patio, nao posicao artificial fixa.
10. Control Room deve receber atualizacao por SSE/WebSocket ou mecanismo equivalente sem depender apenas de recarga manual.
11. Quay/berth/crane planning deve impactar filas e ordens pendentes.
12. Contratos externos `/api/public/v1` devem estar documentados e versionados.
13. Contratos EDI devem validar, rejeitar, reprocessar e auditar mensagens.
14. Testes de service, controller, contrato e frontend devem cobrir o fluxo Navio + Patio.
15. Logs e metricas devem permitir rastrear visita, item, reserva, ordem, fila e movimento.

## Fora do escopo deste corte

- Telemetria real de guindaste, RTG, reach stacker ou terminal tractor.
- Dispatch direto para VMT real.
- Motor matematico global multi-recurso.
- Controle aduaneiro/documental completo.
- Substituicao integral de um TOS comercial.

Esses pontos continuam como evolucao. O proximo corte deve priorizar sincronizacao automatica Patio -> Navio, consumo/cancelamento real de reservas, replanejamento com mapa real, WebSocket/SSE e contratos externos/EDI formalizados.
