# Requisito Back/Front - Pendencias restantes pos-integracao Navio + Patio

## Objetivo

Atualizar o requisito considerando o desenvolvimento mais recente do modulo Navio + Patio e a base de conhecimento N4 Vessel, Equipment Control e Control Room.

O PR `feat: integra visitas de navio com patio` ja entregou o primeiro corte de integracao: vinculos entre item de navio, reserva, ordem, movimento e posicao de patio; endpoints `/visitas-navio/{id}/integracao-patio/*`; campos de origem/destino Navio/Patio em ordens de trabalho; painel Angular Navio + Patio com KPIs, reservas, ordens, alertas, replanejamento e relatorio integrado.

Portanto, este documento deixa de tratar como pendencia a criacao basica de reservas, ordens, alertas, sincronizacao, replanejamento e painel. O que falta agora e transformar esse corte em execucao operacional real, integrada ao servico de yard, filas de trabalho, equipamentos, monitoramento e validacoes de patio.

## Base de conhecimento considerada

A base N4 Vessel mostra que o fluxo de navio envolve visita, descarga, preplan, planejamento de carga, plano de guindaste e inbound stow plan. Para o CloudPort, isso significa que o requisito restante deve focar no elo entre plano do navio, sequencia operacional, descarga/carga e trabalho de patio.

A base N4 Equipment Control mostra que a execucao depende de work queues, work instructions, job lists, CHE, pools, points of work, zone coverage, dispatch, monitoramento de progresso, cancelamento, reset, rehandle e controle de equipamento. Para o CloudPort, isso significa que a integracao Navio + Patio nao deve parar em IDs e DTOs: ela precisa gerar trabalho real para a fila de patio e acompanhar a execucao.

A base N4 Control Room mostra uma visao operacional integrada de patio, vessel information, alertas, CHE detail panel, job lists, work instructions, movimentos iminentes, Quay Monitor e RTG optimization. Para o CloudPort, isso significa que a tela atual Navio + Patio ainda precisa evoluir para uma visao operacional viva, com filtros, filas, excecoes, equipamentos e atualizacao quase em tempo real.

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

Esses itens ja foram cobertos. As pendencias abaixo sao de maturidade operacional, consistencia entre servicos e aderencia ao fluxo real de execucao.

## P0 - Pendencias obrigatorias restantes

### 1. Trocar ordem simulada por ordem real do servico de yard

O corte atual gera e exibe ordens vinculadas a visita, mas ainda pode operar com identificador sintetico ou derivado do item. A proxima entrega deve criar `OrdemTrabalhoPatio` real no `servico-yard` e persistir o ID real retornado.

Regras minimas:

1. `IntegracaoNavioPatioServico` deve criar ordem real no `servico-yard`, por chamada interna, evento ou client HTTP.
2. `ItemOperacaoNavio.ordemTrabalhoPatioId` deve apontar para a ordem real criada.
3. A criacao deve ser idempotente por `visitaNavioId + itemOperacaoNavioId`.
4. Nao permitir duas ordens ativas para o mesmo item de visita.
5. Em erro parcial, registrar alerta e evento, sem deixar item marcado como executavel indevidamente.
6. A resposta de geracao deve separar ordem criada, ordem ja existente, item ignorado e item com erro.

### 2. Validar reserva contra o mapa real do patio

O corte atual possui reserva e posicao textual. A proxima entrega deve validar a reserva contra o modelo real do patio, ocupacao, bloqueios, tipo de carga e capacidade.

Regras minimas:

1. Consultar posicoes reais do patio antes de reservar.
2. Nao reservar posicao ocupada por `ConteinerPatio` ou carga equivalente.
3. Nao reservar posicao bloqueada, interditada ou fora de area permitida.
4. Validar tipo de carga, status, peso e altura/camada quando esses dados existirem.
5. Diferenciar posicao inexistente, ocupada, bloqueada e sem capacidade.
6. Consumir a reserva quando a ordem for concluida.
7. Cancelar ou expirar reserva quando a ordem for cancelada, a visita for cancelada ou o item for replanejado.
8. Registrar auditoria de reserva criada, consumida, cancelada e expirada.

### 3. Sincronizacao real e automatica Patio -> Navio

A sincronizacao manual existe, mas a operacao precisa reagir automaticamente quando a ordem de patio muda.

Regras minimas:

1. Quando `OrdemTrabalhoPatio` mudar para `EM_EXECUCAO`, atualizar o item para `EM_MOVIMENTO`.
2. Quando `OrdemTrabalhoPatio` mudar para `CONCLUIDA`, atualizar o item para `OPERADO`, preencher posicao real e consumir reserva.
3. Quando `OrdemTrabalhoPatio` mudar para `BLOQUEADA` ou `SUSPENSA`, refletir bloqueio operacional no item ou criar divergencia de alta severidade.
4. Quando `OrdemTrabalhoPatio` mudar para `CANCELADA`, reabrir o item conforme regra de negocio e liberar/cancelar reserva.
5. A sincronizacao deve ser idempotente.
6. A sincronizacao deve registrar evento na visita.
7. Se houver falha de comunicacao entre servicos, deve existir reconciliacao por job agendado.

Eventos sugeridos:

```text
OrdemPatioCriada
StatusOrdemPatioAlterado
ReservaPatioConsumida
ReservaPatioCancelada
MovimentoPatioConfirmado
DivergenciaNavioPatioDetectada
```

### 4. Work queues, POW e agrupamento operacional

A base Equipment Control usa work queues, work instructions, job lists, CHE, pools, POW e zone coverage. O CloudPort precisa transformar a integracao Navio + Patio em filas de trabalho operacionais.

Requisito minimo:

- Criar agrupamento de ordens por visita.
- Criar agrupamento por berco/cais.
- Criar agrupamento por porao ou sequencia do plano de estiva.
- Criar agrupamento por bloco/zona de patio.
- Permitir prioridade operacional da ordem.
- Permitir marcar ordem como prioridade de busca/fetch.
- Permitir listar ordens descobertas, sem equipamento/cobertura definida.
- Permitir filtro por `PENDENTE`, `EM_EXECUCAO`, `BLOQUEADA`, `SUSPENSA`, `CONCLUIDA` e `CANCELADA`.

Endpoints sugeridos:

```text
GET  /yard/patio/ordens/visita-navio/{visitaNavioId}/filas
GET  /yard/patio/ordens/visita-navio/{visitaNavioId}/sem-cobertura
PATCH /yard/patio/ordens/{id}/prioridade
PATCH /yard/patio/ordens/{id}/suspender
PATCH /yard/patio/ordens/{id}/retomar
```

### 5. Replanejamento usando otimizacao real de patio

O replanejamento atual deve deixar de sugerir posicoes artificiais e passar a usar o motor real de patio, considerando ocupacao, distancia, rehandle, sequencia de navio e disponibilidade operacional.

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

### 6. Quay/berth/crane planning ligado ao patio

A base Vessel indica plano de guindaste e planejamento de carga/descarga. A base Control Room possui Quay Monitor. Falta ligar visita, berco, recurso de cais e patio.

Requisito minimo:

- Vincular visita a berco operacional.
- Registrar janela planejada e realizada de trabalho no cais.
- Associar recurso de cais, guindaste/equipamento e equipe.
- Associar filas de patio a porao/berco/recurso.
- Medir produtividade planejada x realizada.
- Registrar atraso, parada e motivo.
- Mostrar impacto do atraso no patio e nas ordens pendentes.

### 7. Control Room operacional quase em tempo real

O painel atual Navio + Patio e um bom primeiro corte, mas ainda nao cobre uma sala de controle operacional.

Requisito minimo de frontend:

- Atualizar status de visita, item, reserva e ordem sem recarregar a pagina inteira.
- Exibir movimentos iminentes da visita.
- Exibir ordens sem cobertura de equipamento/fila.
- Exibir alertas com severidade e responsavel.
- Permitir filtro por berco, visita, fase, status, bloco, tipo de movimento e severidade.
- Exibir drill-down da ordem: item, reserva, posicao planejada, posicao real, eventos e divergencias.
- Exibir quadro de execucao por fila/POW/equipamento quando houver dados.

Tecnologia sugerida:

```text
WebSocket ou SSE para /visitas-navio/{id}/stream
WebSocket ou SSE para /yard/patio/ordens/stream
```

### 8. Testes, contratos e observabilidade

O desenvolvimento recente informou que os testes nao foram executados localmente por limitacao de ambiente. Antes de considerar o modulo pronto, faltam testes e observabilidade.

Testes minimos:

1. Service test para criar ordem real de patio a partir de item de visita.
2. Service test para impedir ordem duplicada ativa.
3. Service test para reservar apenas posicao disponivel.
4. Service test para consumir reserva ao concluir ordem.
5. Service test para cancelar reserva ao cancelar ordem/visita.
6. Service test para sincronizacao idempotente Patio -> Navio.
7. Controller test para endpoints `/integracao-patio`.
8. Contract test entre `servico-navio-siderurgico` e `servico-yard`.
9. Frontend test para botoes de gerar reservas, gerar ordens, sincronizar e replanejar.

Observabilidade minima:

- log com `visitaNavioId`, `itemOperacaoNavioId`, `ordemTrabalhoPatioId` e `correlationId`;
- metrica de ordens criadas por visita;
- metrica de falhas de sincronizacao;
- metrica de reservas sem consumo;
- metrica de divergencias Navio x Patio.

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
- encerrar visita com divergencia pendente.

### 3. Padronizacao de status entre servicos

Hoje existem status de item, status de integracao e status de ordem. Falta uma matriz oficial de transicao para evitar interpretacoes divergentes.

A matriz deve cobrir:

- item de navio;
- reserva de patio;
- ordem de patio;
- movimento de patio;
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

## Criterios de aceite atualizados

1. Gerar ordem de patio real no `servico-yard` a partir de item de visita.
2. Persistir no item da visita o ID real da ordem criada no patio.
3. Impedir ordem ativa duplicada para o mesmo item de visita.
4. Validar reserva contra ocupacao real do patio.
5. Impedir reserva em posicao inexistente, ocupada, bloqueada ou sem capacidade.
6. Consumir reserva automaticamente quando ordem for concluida.
7. Cancelar ou expirar reserva quando ordem/visita for cancelada.
8. Sincronizar status de ordem de patio para item de navio automaticamente.
9. Ter reconciliacao manual ou agendada para falhas de sincronizacao.
10. Exibir ordens agrupadas por visita, berco, fila e status.
11. Exibir ordens sem cobertura/fila/equipamento como excecao operacional.
12. Replanejar usando mapa e otimizacao real de patio, nao posicao artificial fixa.
13. Control Room deve atualizar status e alertas sem recarregar a pagina inteira.
14. Testes de service, controller, contrato e frontend devem cobrir o fluxo Navio + Patio.
15. Logs e metricas devem permitir rastrear visita, item, reserva, ordem e movimento.

## Fora do escopo deste corte

- Telemetria real de guindaste, RTG, reach stacker ou terminal tractor.
- Dispatch direto para VMT real.
- Motor matematico global multi-recurso.
- Controle aduaneiro/documental completo.
- Substituicao integral de um TOS comercial.

Esses pontos continuam como evolucao. O proximo corte deve priorizar a conversao da integracao atual em execucao real de yard, com fila, ordem persistida no servico de patio, sincronizacao automatica e validacao contra o mapa real.
