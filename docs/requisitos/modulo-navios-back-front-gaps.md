# Requisito Back/Front - Pendencias restantes de navio e patio

## Objetivo

Atualizar o requisito do modulo de navios removendo o que ja foi desenvolvido e deixando somente o que ainda falta para integrar o fluxo de navio com o fluxo de patio.

Este documento considera como ja entregue ou em desenvolvimento atual:

- CRUD e fluxo operacional de visitas de navio.
- Lista operacional de carga, descarga e restow por visita.
- Plano de estiva persistido por visita.
- Eventos da visita e resumo operacional.
- Tela Angular operacional consumindo APIs reais de navio/visita/itens/plano/eventos.
- Modulo de patio com mapa, conteineres, movimentacoes, posicoes, equipamentos, ordens de trabalho, KPIs, alertas, simulador, autoplanejamento, otimizacao de rota e dual-cycling.
- Planejamento de estivagem bulk/coils com validacoes estruturais, lashing, estabilidade e plano final.
- Vessel Planner, EDI e validacoes nauticas ja implementadas em cortes anteriores.

Portanto, o foco agora nao e recriar visita, item, estiva ou tela basica. O foco restante e fazer o navio gerar, consumir e sincronizar trabalho real com o patio.

## Base de analise

A base N4 Vessel trata o fluxo de navio como um processo que envolve visita, descarga, preplan, carga, plano de guindaste e inbound stow plan. No CloudPort, boa parte do cadastro e do plano de estiva ja foi coberta; a lacuna agora esta na amarracao operacional entre navio, patio e execucao.

A base N4 Equipment Control mostra que a execucao depende de work queues, work instructions, CHE, POW, pools, zonas e monitoramento de progresso. No CloudPort, ordens de patio e otimizacoes existem, mas ainda nao estao ligadas diretamente aos itens da visita de navio.

A base Control Room tambem reforca que a operacao precisa mostrar patio, navios, equipamentos, alertas, work instructions, movimentos iminentes e monitor de cais em uma visao integrada. O CloudPort tem partes separadas, mas ainda falta a tela operacional unica Navio + Patio.

## O que nao deve voltar para o requisito

Nao repetir como pendencia principal:

1. Criar entidade de visita de navio.
2. Criar endpoints basicos de `/visitas-navio`.
3. Criar item operacional de embarque/descarga/restow.
4. Criar plano de estiva simples por porao/camada/coluna/bordo.
5. Criar evento de visita.
6. Criar resumo operacional da visita.
7. Substituir o iframe principal por tela Angular basica integrada.
8. Criar mapa basico de patio.
9. Criar ordens de trabalho de patio.
10. Criar KPI, alerta, simulador e autoplanejamento de patio.

Esses itens ja existem ou foram cobertos pelo desenvolvimento recente. A pendencia real e a integracao operacional entre eles.

## P0 - O que ainda falta

### 1. Integracao operacional Navio x Patio

Criar um servico de integracao que conecte `VisitaNavio`, `ItemOperacaoNavio`, `PlanoEstivaNavio`, `ConteinerPatio`, `MovimentoPatio` e `OrdemTrabalhoPatio`.

O sistema deve conseguir transformar itens de uma visita em trabalho de patio e, depois, sincronizar o resultado do patio de volta para a visita.

Fluxo minimo esperado:

- Descarga: item planejado no navio gera ordem de patio do cais/navio para uma posicao de patio reservada.
- Embarque: item planejado para embarque gera ordem do patio para cais/navio, respeitando sequencia de estiva.
- Restow: item de restow gera ordem interna vinculada a visita, podendo ir para patio, posicao temporaria ou nova posicao a bordo.
- Ao concluir ordem no patio, o status do item da visita deve ser atualizado.
- Ao bloquear item no navio, a ordem de patio vinculada deve ficar bloqueada ou suspensa.
- Ao cancelar visita, as ordens pendentes vinculadas devem ser canceladas ou sinalizadas para revisao.

### 2. Modelo de vinculo entre item de navio e ordem de patio

Adicionar vinculos explicitos nos modelos existentes, evitando rastreamento apenas por texto/lote.

Campos sugeridos:

#### Em `ItemOperacaoNavio`

- `conteinerPatioId` ou `cargaPatioId`, quando existir entidade fisica no patio.
- `ordemTrabalhoPatioId`, quando houver ordem ativa vinculada.
- `movimentoPatioId`, quando houver movimentacao confirmada.
- `posicaoPatioPlanejada`.
- `posicaoPatioReal`.
- `statusIntegracaoPatio`: `NAO_GERADO`, `RESERVADO`, `ORDEM_GERADA`, `EM_EXECUCAO`, `SINCRONIZADO`, `ERRO`, `CANCELADO`.

#### Em `OrdemTrabalhoPatio`

- `visitaNavioId`.
- `itemOperacaoNavioId`.
- `planoEstivaNavioId` opcional.
- `tipoOrigem`: `PATIO`, `NAVIO`, `CAIS`, `AREA_TEMPORARIA`.
- `tipoDestino`: `PATIO`, `NAVIO`, `CAIS`, `AREA_TEMPORARIA`.
- `sequenciaNavio`.
- `prioridadeOperacional`.

### 3. Reserva e pre-alocacao de patio para descarga

Antes de iniciar descarga, o sistema deve conseguir reservar posicoes no patio para os itens que serao descarregados.

Regras minimas:

1. Nao reservar posicao ocupada.
2. Nao reservar posicao bloqueada.
3. Nao reservar fora da area permitida para tipo de carga/produto/status.
4. Validar peso e capacidade da posicao quando houver limite cadastrado.
5. Permitir reserva tentativa e reserva definitiva.
6. Permitir replanejar reservas antes da execucao.
7. Exibir itens sem posicao disponivel como excecao operacional.

Entidade sugerida:

#### `ReservaPosicaoPatioNavio`

- `id`
- `visitaNavioId`
- `itemOperacaoNavioId`
- `posicaoPatioId` ou campos bloco/linha/coluna/camada
- `tipoReserva`: `TENTATIVA`, `DEFINITIVA`
- `status`: `ATIVA`, `CONSUMIDA`, `CANCELADA`, `EXPIRADA`
- `motivoCancelamento`
- `criadoEm`
- `atualizadoEm`

### 4. Geracao automatica de ordens de patio pela visita

Criar comando para gerar ordens de patio a partir da visita e do plano de estiva.

Endpoint sugerido:

`POST /visitas-navio/{id}/integracao-patio/gerar-ordens`

Payload sugerido:

```json
{
  "tipoMovimento": "DESCARGA",
  "modo": "SOMENTE_PENDENTES",
  "usuario": "operador",
  "gerarReservasAutomaticas": true
}
```

Retorno esperado:

- quantidade de ordens criadas;
- quantidade de itens ignorados;
- quantidade de itens com erro;
- lista de erros por item;
- lista de alertas de capacidade/rehandle/bloqueio.

Regras minimas:

1. Nao duplicar ordem para item que ja possui ordem ativa.
2. Respeitar `tipoMovimento` do item.
3. Respeitar sequencia operacional do plano de estiva.
4. Para descarga, exigir destino de patio ou gerar reserva automatica.
5. Para embarque, exigir origem de patio ou conteiner/carga ja localizado no patio.
6. Para item bloqueado, nao gerar ordem executavel.
7. Registrar evento na visita ao gerar ordens.

### 5. Sincronizacao de status patio -> navio

Quando uma ordem de patio mudar de status, o item de navio vinculado deve refletir o avanco operacional.

Mapeamento inicial sugerido:

- Ordem `PENDENTE` -> item permanece `LIBERADO` ou `PLANEJADO`.
- Ordem `EM_EXECUCAO` -> item `EM_MOVIMENTO`.
- Ordem `CONCLUIDA` -> item `OPERADO`.
- Ordem `CANCELADA` -> item volta para `PLANEJADO` ou `CANCELADO`, conforme motivo.
- Ordem `BLOQUEADA`/`SUSPENSA` -> item `BLOQUEADO`.

Endpoint de reconciliacao manual:

`POST /visitas-navio/{id}/integracao-patio/sincronizar-status`

O backend tambem deve ter metodo interno para sincronizar automaticamente quando `OrdemTrabalhoPatioServico` atualizar status.

### 6. Quadro operacional Navio + Patio no frontend

Criar uma tela unica para acompanhar visita de navio e patio juntos.

Rota sugerida:

`/visitas-navio/:id/operacao-patio`

A tela deve mostrar:

- cabecalho da visita;
- fase da visita;
- resumo planejado x operado;
- mapa ou lista de patio filtrada pelos itens da visita;
- ordens de patio vinculadas;
- reservas de patio;
- alertas de integracao;
- itens de navio sem ordem;
- ordens de patio sem item vinculado;
- divergencias entre posicao planejada e real.

Componentes sugeridos:

- `OperacaoNavioPatioComponent`
- `PainelResumoNavioPatioComponent`
- `OrdensPatioDaVisitaComponent`
- `ReservasPatioDaVisitaComponent`
- `MapaPatioDaVisitaComponent`
- `AlertasIntegracaoNavioPatioComponent`
- `DivergenciasNavioPatioComponent`

### 7. Alertas e excecoes integradas

Criar alertas especificos para falhas entre navio e patio.

Alertas minimos:

1. Item de descarga sem destino de patio.
2. Item de embarque sem origem de patio.
3. Ordem de patio atrasada em relacao a sequencia do navio.
4. Posicao de patio reservada, mas ocupada antes da execucao.
5. Item bloqueado com ordem ativa.
6. Ordem concluida no patio sem atualizar item da visita.
7. Item operado no navio sem movimento de patio correspondente.
8. Divergencia entre destino planejado e destino real.
9. Capacidade de patio insuficiente para a visita.
10. Rehandle previsto acima do limite aceitavel.

### 8. Integracao com otimizacao de patio

A automacao de patio ja existe, mas precisa receber contexto de navio.

A otimizacao deve considerar:

- fase da visita;
- ETA/ETB/ETD;
- prioridade por berco;
- sequencia operacional de embarque/descarga;
- cutoff;
- peso/tipo/produto;
- destino de carga;
- risco de rehandle;
- proximidade com area de cais;
- dual-cycling quando houver combinacao de embarque e descarga.

Endpoint sugerido:

`POST /visitas-navio/{id}/integracao-patio/replanejar`

Retorno esperado:

- reservas sugeridas;
- ordens reordenadas;
- economia estimada de distancia/tempo;
- risco de rehandle;
- alertas impeditivos.

### 9. Relatorio operacional integrado

Criar recap unico da visita com informacoes de navio e patio.

Endpoint sugerido:

`GET /visitas-navio/{id}/relatorio-operacional-integrado`

Conteudo minimo:

- dados da visita;
- plano de estiva;
- itens planejados/operados;
- ordens de patio vinculadas;
- reservas de patio;
- tempos de execucao;
- divergencias;
- bloqueios;
- produtividade;
- eventos relevantes.

## P1 - Pendencias importantes apos a integracao minima

### 1. Planejamento de berco ligado ao patio

Criar visao de agenda de berco que tambem mostre impacto no patio:

- ocupacao prevista por janela de navio;
- demanda de patio por visita;
- capacidade de patio por periodo;
- conflito de visitas usando mesma area de apoio;
- impacto de atraso de ETA/ETB/ETD nas ordens de patio.

### 2. Recursos operacionais do cais

Criar cadastro/vinculo de recursos de cais:

- guindaste;
- equipe;
- hatch clerk/conferente;
- equipamento de apoio;
- berco/area operacional;
- janela de uso;
- produtividade planejada e realizada.

### 3. Work queues por visita/berco/porao

Agrupar ordens em filas operacionais mais proximas do modelo de execucao:

- fila por visita;
- fila por berco;
- fila por porao;
- fila por tipo de movimento;
- fila por recurso/equipamento;
- ordenacao por sequencia de estiva e prioridade de patio.

### 4. Visibilidade quase em tempo real

Adicionar WebSocket/SSE para:

- mudanca de fase da visita;
- status de item;
- status de ordem de patio;
- alerta novo;
- reserva consumida/cancelada;
- divergencia operacional.

### 5. Importacao/exportacao operacional

Criar importacao/exportacao simples antes de evoluir para EDI completo no fluxo integrado:

- CSV/JSON de itens de visita;
- CSV/JSON de reservas de patio;
- exportacao do plano integrado;
- exportacao de recap operacional.

## P2 - Evolucoes avancadas

1. Otimizacao automatica de sequencia Navio + Patio considerando guindaste, caminho, equipamento e rehandle.
2. Simulacao what-if de atraso de navio e impacto no patio.
3. Previsao de gargalo por berco, porao, bloco e equipamento.
4. Integracao EDI operacional completa com reflexo automatico no patio.
5. Comparacao entre plano de estiva, plano de patio e realizado.
6. Controle de produtividade por recurso do cais e por equipamento de patio.
7. Painel estilo control room com navio, patio, equipamentos, alertas e movimentos iminentes.

## Requisito de backend restante

### Services novos ou adaptados

#### `IntegracaoNavioPatioServico`

Responsavel por orquestrar visita, itens, plano, reservas, ordens e sincronizacao.

Metodos minimos:

- `gerarOrdensDaVisita(visitaId, comando)`
- `gerarReservasDaVisita(visitaId, comando)`
- `sincronizarStatus(visitaId)`
- `listarAlertasIntegracao(visitaId)`
- `replanejarPatioDaVisita(visitaId, comando)`

#### `ReservaPatioNavioServico`

Responsavel por reservar, confirmar, cancelar e consumir posicoes de patio vinculadas a visita.

#### `ValidadorIntegracaoNavioPatioServico`

Responsavel por validar:

- item sem posicao;
- item sem carga fisica no patio;
- posicao duplicada;
- capacidade do bloco;
- ordem duplicada;
- status incoerente;
- bloqueio impeditivo;
- divergencia entre planejado e real.

#### `SincronizadorStatusNavioPatioServico`

Responsavel por refletir eventos/status entre `OrdemTrabalhoPatio`, `MovimentoPatio` e `ItemOperacaoNavio`.

### Endpoints novos

```text
GET  /visitas-navio/{id}/integracao-patio
POST /visitas-navio/{id}/integracao-patio/reservas
GET  /visitas-navio/{id}/integracao-patio/reservas
POST /visitas-navio/{id}/integracao-patio/gerar-ordens
GET  /visitas-navio/{id}/integracao-patio/ordens
GET  /visitas-navio/{id}/integracao-patio/alertas
POST /visitas-navio/{id}/integracao-patio/replanejar
POST /visitas-navio/{id}/integracao-patio/sincronizar-status
GET  /visitas-navio/{id}/relatorio-operacional-integrado
```

### DTOs minimos

- `ComandoGeracaoOrdensPatioDTO`
- `ResultadoGeracaoOrdensPatioDTO`
- `ReservaPatioNavioDTO`
- `AlertaIntegracaoNavioPatioDTO`
- `ResumoIntegracaoNavioPatioDTO`
- `ComandoReplanejamentoPatioNavioDTO`
- `ResultadoReplanejamentoPatioNavioDTO`
- `RelatorioOperacionalIntegradoDTO`

## Requisito de frontend restante

### Ajustes no modulo de visita

A tela de visita deve ganhar uma aba nova: `Patio`.

Essa aba deve permitir:

- gerar reservas;
- gerar ordens;
- visualizar ordens da visita;
- visualizar mapa/lista de patio filtrada pela visita;
- sincronizar status;
- replanejar patio;
- ver alertas e divergencias.

### Rotas sugeridas

```text
/visitas-navio/:id/patio
/visitas-navio/:id/patio/ordens
/visitas-navio/:id/patio/reservas
/visitas-navio/:id/patio/alertas
```

### UX minima

1. O operador deve saber quais itens da visita ainda nao viraram ordem de patio.
2. O operador deve saber quais ordens de patio estao atrasadas ou bloqueadas.
3. O operador deve ver a posicao planejada e real de patio por item.
4. O operador deve conseguir replanejar sem editar manualmente cada item.
5. O operador deve receber erro claro quando nao houver posicao disponivel.
6. O operador deve conseguir abrir o mapa de patio filtrado apenas pela visita.
7. O operador deve ver divergencias entre estiva, patio e execucao.

## Criterios de aceite atualizados

1. A partir de uma visita com itens de descarga, o sistema deve gerar reservas de patio.
2. A partir de uma visita com itens de descarga, o sistema deve gerar ordens de patio vinculadas aos itens.
3. A partir de uma visita com itens de embarque, o sistema deve gerar ordens do patio para o navio/cais.
4. Uma ordem de patio gerada pela visita deve guardar `visitaNavioId` e `itemOperacaoNavioId`.
5. Ao concluir uma ordem de patio, o item de navio vinculado deve ser atualizado para status equivalente.
6. Ao bloquear um item da visita, a ordem de patio vinculada deve ser bloqueada/suspensa ou indicada como divergente.
7. A API deve listar todas as ordens de patio de uma visita.
8. A API deve listar todas as reservas de patio de uma visita.
9. A API deve listar alertas de integracao Navio x Patio.
10. O frontend deve ter aba/tela de patio dentro da visita.
11. A tela deve mostrar itens sem ordem, ordens sem sincronizacao e divergencias de posicao.
12. O replanejamento deve retornar sugestoes e alertas sem sobrescrever execucao concluida.
13. Deve haver teste de service para geracao de ordens, reserva, sincronizacao e bloqueio.
14. Deve haver teste de controller para endpoints de integracao.
15. Nenhum item ja operado deve ser replanejado automaticamente sem erro ou confirmacao explicita.

## Fora do escopo deste corte

- Controle completo de equipamento real via telemetria.
- Integracao com hardware de guindaste ou terminal tractor.
- Otimizacao matematica avancada multi-recurso.
- EDI completo do fluxo integrado.
- Painel control room completo em tempo real.

Esses itens devem continuar como evolucao, nao como bloqueio do proximo corte.
