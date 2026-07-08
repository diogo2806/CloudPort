# Requisito Back/Front - Somente pendencias restantes de Navio + Patio

## Objetivo

Este requisito substitui a lista antiga de gaps do modulo de navios. O desenvolvimento recente ja entregou o fluxo operacional de visita, itens de carga/descarga/restow, plano de estiva, eventos, resumo operacional e tela Angular conectada ao backend. O patio tambem ja possui mapa, conteineres, posicoes, equipamentos, movimentacoes, ordens de trabalho, KPIs, alertas, simulador, autoplanejamento e otimizacoes.

O que falta agora e ligar esses dois mundos: a visita de navio precisa gerar trabalho de patio, reservar posicoes, acompanhar ordens, receber confirmacao de execucao e mostrar uma visao unica Navio + Patio.

## Escopo restante P0

### 1. Vinculo formal entre item da visita e patio

Hoje o item da visita possui dados operacionais, e o patio possui conteineres, movimentos e ordens. Falta uma referencia persistida entre eles.

Alteracoes minimas:

- `ItemOperacaoNavio` deve guardar o vinculo com a carga/conteiner no patio, quando existir.
- `ItemOperacaoNavio` deve guardar a ordem de patio ativa ou a ultima ordem relacionada.
- `OrdemTrabalhoPatio` deve guardar `visitaNavioId` e `itemOperacaoNavioId`.
- O sistema nao deve depender apenas de lote, codigo textual ou descricao para reconciliar navio e patio.

Campos sugeridos em `ItemOperacaoNavio`:

- `conteinerPatioId` ou `cargaPatioId`.
- `ordemTrabalhoPatioId`.
- `movimentoPatioId`.
- `posicaoPatioPlanejada`.
- `posicaoPatioReal`.
- `statusIntegracaoPatio`: `NAO_GERADO`, `RESERVADO`, `ORDEM_GERADA`, `EM_EXECUCAO`, `SINCRONIZADO`, `ERRO`, `CANCELADO`.

Campos sugeridos em `OrdemTrabalhoPatio`:

- `visitaNavioId`.
- `itemOperacaoNavioId`.
- `planoEstivaNavioId` opcional.
- `tipoOrigem`: `PATIO`, `NAVIO`, `CAIS`, `AREA_TEMPORARIA`.
- `tipoDestino`: `PATIO`, `NAVIO`, `CAIS`, `AREA_TEMPORARIA`.
- `sequenciaNavio`.
- `prioridadeOperacional`.

### 2. Reserva de patio para descarga

Antes de iniciar a descarga, o sistema deve reservar posicoes de patio para os itens que descem do navio.

Endpoint sugerido:

```text
POST /visitas-navio/{id}/integracao-patio/reservas
GET  /visitas-navio/{id}/integracao-patio/reservas
```

Entidade sugerida: `ReservaPosicaoPatioNavio`.

Campos minimos:

- `id`.
- `visitaNavioId`.
- `itemOperacaoNavioId`.
- `posicaoPatioId` ou bloco/linha/coluna/camada.
- `tipoReserva`: `TENTATIVA`, `DEFINITIVA`.
- `status`: `ATIVA`, `CONSUMIDA`, `CANCELADA`, `EXPIRADA`.
- `motivoCancelamento`.
- `criadoEm`.
- `atualizadoEm`.

Regras minimas:

1. Nao reservar posicao ocupada.
2. Nao reservar posicao bloqueada.
3. Nao reservar fora da area permitida para tipo de carga/produto/status.
4. Validar peso/capacidade da posicao quando houver limite.
5. Permitir reserva tentativa e reserva definitiva.
6. Permitir replanejar reserva ainda nao consumida.
7. Exibir item sem posicao disponivel como excecao operacional.

### 3. Geracao de ordens de patio pela visita

A visita de navio deve gerar ordens de patio automaticamente com base nos itens, tipo de movimento e plano de estiva.

Endpoint sugerido:

```text
POST /visitas-navio/{id}/integracao-patio/gerar-ordens
GET  /visitas-navio/{id}/integracao-patio/ordens
```

Payload sugerido:

```json
{
  "tipoMovimento": "DESCARGA",
  "modo": "SOMENTE_PENDENTES",
  "usuario": "operador",
  "gerarReservasAutomaticas": true
}
```

Retorno minimo:

- total de ordens criadas;
- total de itens ignorados;
- total de itens com erro;
- erros por item;
- alertas de capacidade, bloqueio, rehandle e falta de posicao.

Regras minimas:

1. Nao duplicar ordem para item com ordem ativa.
2. Respeitar o tipo de movimento: `EMBARQUE`, `DESCARGA` ou `RESTOW`.
3. Respeitar a sequencia operacional do plano de estiva.
4. Na descarga, exigir destino de patio ou gerar reserva automatica.
5. No embarque, exigir origem de patio/carga localizada.
6. Em restow, permitir destino temporario, patio ou nova posicao a bordo.
7. Nao gerar ordem executavel para item bloqueado.
8. Registrar evento na visita para cada lote de ordens gerado.

### 4. Sincronizacao de status Patio -> Navio

Quando a ordem de patio evoluir, o item da visita deve refletir a execucao.

Endpoint de reconciliacao manual:

```text
POST /visitas-navio/{id}/integracao-patio/sincronizar-status
```

Mapeamento inicial:

- Ordem `PENDENTE` -> item continua `PLANEJADO` ou `LIBERADO`.
- Ordem `EM_EXECUCAO` -> item `EM_MOVIMENTO`.
- Ordem `CONCLUIDA` -> item `OPERADO`.
- Ordem `CANCELADA` -> item volta para `PLANEJADO` ou fica `CANCELADO`, conforme motivo.
- Ordem `BLOQUEADA` ou `SUSPENSA` -> item `BLOQUEADO`.

Regras minimas:

1. Atualizacao manual deve reconciliar visita inteira.
2. Atualizacao automatica deve ocorrer quando `OrdemTrabalhoPatioServico` mudar status de uma ordem vinculada.
3. Toda sincronizacao deve criar evento de visita.
4. Divergencia deve ser registrada como alerta, nao ignorada.
5. Item ja `OPERADO` nao deve voltar de status sem confirmacao explicita.

### 5. Alertas integrados Navio x Patio

Criar alertas especificos da integracao.

Endpoint sugerido:

```text
GET /visitas-navio/{id}/integracao-patio/alertas
```

Alertas minimos:

1. Item de descarga sem destino/reserva de patio.
2. Item de embarque sem origem de patio.
3. Item com ordem duplicada ativa.
4. Ordem de patio atrasada em relacao a sequencia do navio.
5. Posicao reservada ocupada antes da execucao.
6. Item bloqueado com ordem ativa.
7. Ordem concluida sem atualizar o item da visita.
8. Item operado sem movimento de patio correspondente.
9. Divergencia entre posicao planejada e real.
10. Capacidade de patio insuficiente para a visita.
11. Rehandle previsto acima do limite aceitavel.

### 6. Replanejamento de patio com contexto de navio

A automacao/otimizacao de patio ja existe, mas deve receber contexto da visita.

Endpoint sugerido:

```text
POST /visitas-navio/{id}/integracao-patio/replanejar
```

A otimizacao deve considerar:

- fase da visita;
- ETA/ETB/ETD/cutoff;
- berco;
- tipo de movimento;
- sequencia do plano de estiva;
- peso, produto e tipo de carga;
- origem/destino de patio;
- risco de rehandle;
- proximidade com cais;
- dual-cycling quando houver embarque e descarga na mesma janela.

Retorno minimo:

- reservas sugeridas;
- ordens reordenadas;
- economia estimada de distancia/tempo;
- risco de rehandle;
- alertas impeditivos;
- lista de itens que nao podem ser replanejados.

Regra critica: item ja operado ou ordem concluida nao deve ser replanejado automaticamente.

### 7. Tela unica Navio + Patio

Criar uma aba ou rota operacional dentro da visita para mostrar patio e navio juntos.

Rota sugerida:

```text
/visitas-navio/:id/patio
```

Componentes sugeridos:

- `OperacaoNavioPatioComponent`.
- `PainelResumoNavioPatioComponent`.
- `OrdensPatioDaVisitaComponent`.
- `ReservasPatioDaVisitaComponent`.
- `MapaPatioDaVisitaComponent`.
- `AlertasIntegracaoNavioPatioComponent`.
- `DivergenciasNavioPatioComponent`.

A tela deve mostrar:

- cabecalho da visita;
- fase da visita;
- progresso planejado x realizado;
- mapa/lista do patio filtrado pela visita;
- ordens de patio vinculadas;
- reservas de patio;
- itens sem ordem;
- ordens sem sincronizacao;
- alertas e divergencias;
- botoes para gerar reservas, gerar ordens, replanejar e sincronizar status.

### 8. Relatorio operacional integrado

Criar recap unico da visita com dados de navio e patio.

Endpoint sugerido:

```text
GET /visitas-navio/{id}/relatorio-operacional-integrado
```

Conteudo minimo:

- dados da visita;
- plano de estiva;
- itens planejados x operados;
- reservas de patio;
- ordens de patio;
- tempos de execucao;
- divergencias;
- bloqueios;
- produtividade;
- eventos relevantes.

## Backend restante

### Services

#### `IntegracaoNavioPatioServico`

Responsavel por orquestrar visita, plano, itens, reservas, ordens, alertas e sincronizacao.

Metodos minimos:

- `obterResumoIntegracao(visitaId)`.
- `gerarReservasDaVisita(visitaId, comando)`.
- `gerarOrdensDaVisita(visitaId, comando)`.
- `listarOrdensDaVisita(visitaId)`.
- `listarAlertasIntegracao(visitaId)`.
- `sincronizarStatus(visitaId)`.
- `replanejarPatioDaVisita(visitaId, comando)`.
- `gerarRelatorioOperacionalIntegrado(visitaId)`.

#### `ReservaPatioNavioServico`

Responsavel por reservar, confirmar, cancelar e consumir posicoes de patio vinculadas a itens de visita.

#### `ValidadorIntegracaoNavioPatioServico`

Responsavel por validar posicao indisponivel, ordem duplicada, item bloqueado, item sem carga fisica, capacidade, status incoerente e divergencia planejado x real.

#### `SincronizadorStatusNavioPatioServico`

Responsavel por refletir status/eventos entre `OrdemTrabalhoPatio`, `MovimentoPatio` e `ItemOperacaoNavio`.

### Endpoints consolidados

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

- `ResumoIntegracaoNavioPatioDTO`.
- `ComandoGeracaoReservasPatioDTO`.
- `ComandoGeracaoOrdensPatioDTO`.
- `ResultadoGeracaoOrdensPatioDTO`.
- `ReservaPatioNavioDTO`.
- `OrdemPatioDaVisitaDTO`.
- `AlertaIntegracaoNavioPatioDTO`.
- `ComandoReplanejamentoPatioNavioDTO`.
- `ResultadoReplanejamentoPatioNavioDTO`.
- `RelatorioOperacionalIntegradoDTO`.

## Frontend restante

### Aba `Patio` na visita

A tela de detalhe da visita deve ganhar uma aba `Patio`.

A aba deve permitir:

- gerar reservas;
- gerar ordens;
- visualizar ordens da visita;
- visualizar reservas da visita;
- abrir mapa/lista de patio filtrado pela visita;
- sincronizar status;
- replanejar patio;
- ver alertas e divergencias;
- abrir relatorio integrado.

### UX minima

1. O operador deve saber quais itens da visita ainda nao viraram ordem de patio.
2. O operador deve saber quais itens ainda nao possuem reserva de patio.
3. O operador deve saber quais ordens estao atrasadas, bloqueadas ou divergentes.
4. O operador deve ver posicao planejada e real de patio por item.
5. O operador deve conseguir gerar ordens em lote, sem editar item por item.
6. O operador deve receber erro claro quando nao houver posicao disponivel.
7. O operador deve conseguir abrir o mapa de patio filtrado apenas pela visita.
8. O operador deve ver divergencias entre estiva, patio e execucao.

## Criterios de aceite

1. Uma visita com itens de descarga deve gerar reservas de patio.
2. Uma visita com itens de descarga deve gerar ordens de patio vinculadas aos itens.
3. Uma visita com itens de embarque deve gerar ordens do patio para navio/cais.
4. Uma ordem gerada pela visita deve guardar `visitaNavioId` e `itemOperacaoNavioId`.
5. O backend deve impedir ordem duplicada ativa para o mesmo item.
6. Ao concluir ordem de patio, o item da visita deve ser atualizado para status equivalente.
7. Ao bloquear item da visita, a ordem vinculada deve ser bloqueada/suspensa ou apontada como divergencia.
8. A API deve listar ordens, reservas e alertas da visita.
9. A API deve replanejar apenas itens ainda nao operados.
10. A API deve gerar relatorio operacional integrado.
11. O frontend deve ter aba/tela de patio dentro da visita.
12. A tela deve mostrar itens sem ordem, itens sem reserva, ordens sem sincronizacao e divergencias de posicao.
13. Deve haver teste de service para reserva, geracao de ordem, sincronizacao e bloqueio.
14. Deve haver teste de controller para os endpoints de integracao.
15. Nenhum item operado ou ordem concluida deve ser alterado por replanejamento automatico sem confirmacao explicita.

## Fora do escopo deste corte

- Controle real de equipamento por telemetria.
- Integracao com hardware de guindaste, RTG ou terminal tractor.
- Otimizacao matematica global multi-recurso.
- EDI completo refletindo automaticamente no patio.
- Painel control room completo em tempo real.

Esses itens continuam como evolucao, nao como bloqueio da proxima entrega.
