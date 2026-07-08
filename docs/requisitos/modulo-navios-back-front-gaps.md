# Requisito Back/Front - Gaps do modulo de navios

## Objetivo

Evoluir o modulo de navios do CloudPort para deixar de ser apenas um cadastro/operacao basica e passar a cobrir o fluxo operacional minimo de uma visita de navio: cadastro do navio, escala/visita, linha/viagem, berco, janela operacional, lista de carga/descarga, plano de estiva, acompanhamento da operacao, eventos, bloqueios e indicadores.

A referencia funcional usada para este requisito vem da base de conhecimento de operacoes vessel/N4 e de estiva siderurgica. O objetivo nao e copiar todo o N4, mas trazer para o CloudPort um MVP coerente com o dominio portuario e com operacoes de carga siderurgica.

## Situacao atual encontrada no repositorio

### Backend atual

Existem dois recortes de navio:

1. `backend/servico-navio`
   - CRUD basico de navios em `/navios`.
   - Escalas em `/escalas` e `/navios/{navioId}/escalas`.
   - Transicao simples de fase da escala.
   - Campos principais: nome, IMO, bandeira, armadora, capacidade TEU, LOA, calado e call sign.

2. `backend/servico-navio-siderurgico`
   - Cadastro/listagem de navios siderurgicos em `/navios-siderurgicos`.
   - Criacao/listagem de operacoes siderurgicas em `/operacoes-siderurgicas`.
   - Criacao/listagem de itens de carga por operacao em `/operacoes-siderurgicas/{operacaoId}/itens`.
   - Campos principais: tipo de navio, DWT, quantidade de poroes, tipo de operacao, berco, viagem, ETA, origem/destino e itens por lote.

### Frontend atual

O frontend `frontend/servico-navio-siderurgico` possui service Angular para consumir APIs de navios, operacoes e itens, mas a tela principal ainda esta baseada em iframe estatico de assets HTML de estiva. Nao existe tela Angular integrada para listar, detalhar, editar, filtrar, acompanhar e operar navios/visitas/cargas.

## Principais lacunas funcionais

### P0 - Gaps obrigatorios para o modulo virar operacional

1. Visita/escala de navio unificada
   - Hoje ha escala simples e operacao siderurgica separada.
   - Falta uma entidade operacional unica de visita do navio, com ETA/ATA/ETB/ATB/ETD/ATD, berco previsto/real, fase operacional, viagem de entrada, viagem de saida, linha/armador, terminal/facility e janelas de recebimento/cutoff.

2. Lista de carga e descarga
   - Hoje existe item de carga siderurgica por operacao, mas nao ha separacao formal de lista de embarque e lista de descarga.
   - Falta controle de manifesto operacional: item/lote, produto, tipo de carga, peso, quantidade, porao, posicao planejada, origem patio, destino patio, status e divergencia.

3. Plano de estiva operacional
   - Hoje os assets exibem estiva em HTML, mas nao existe persistencia real de plano por porao, camada, bordo, sequencia e restricoes.
   - Falta salvar plano de estiva por visita, com posicoes por porao, sequencia de movimentacao, peso planejado/real e validacoes basicas.

4. Acompanhamento de operacao
   - Hoje a operacao pode ser criada, mas nao ha ciclo operacional completo para iniciar, pausar, concluir ou cancelar itens/movimentos.
   - Falta historico de eventos, status por item, total planejado x realizado, produtividade e progresso por visita.

5. Validacoes minimas de seguranca e consistencia
   - Falta validar capacidade de porao, peso total por porao, posicao repetida, lote duplicado por visita, ordem de datas, fase invalida e tentativa de alterar visita encerrada.

6. Tela Angular real do modulo
   - Falta substituir o iframe principal por telas Angular conectadas ao backend.
   - A tela deve permitir listar navios, selecionar visita, visualizar plano de carga/descarga, cadastrar item, editar status e acompanhar progresso.

### P1 - Gaps importantes apos MVP

1. Port rotation/itinerario
   - Cadastro de portos de origem, destino, proximo porto, porto anterior e pontos intermediarios.
   - Edicao de rotacao por visita.

2. Berth planning basico
   - Calendario de berco por periodo.
   - Conflito de berco/horario.
   - Visualizacao de navios previstos, atracados, operando e encerrados.

3. Crane plan / recursos operacionais
   - Associar guindastes/equipamentos a visita.
   - Registrar janela de uso, paradas, produtividade e atrasos.

4. Eventos, holds e permissoes
   - Registrar eventos por visita/item.
   - Bloquear item ou visita por motivo operacional, documental ou seguranca.
   - Liberar bloqueio com usuario/data/motivo.

5. Relatorios operacionais
   - Plano de estiva.
   - Lista de descarga.
   - Lista de embarque.
   - Recap de operacao.
   - Divergencias planejado x realizado.

### P2 - Integracoes e automacao

1. Importacao/exportacao EDI ou arquivo estruturado
   - Importar lista de carga/descarga por JSON/CSV inicialmente.
   - Preparar arquitetura para BAPLIE/COPRAR/COARRI/VERMAS no futuro.

2. Validacoes avancadas de estiva
   - Lashing, estabilidade, limites estruturais, segregacao de carga e restricoes por tipo de produto.
   - No MVP, manter validacoes simplificadas de peso, porao e duplicidade.

3. Visibilidade em tempo real
   - WebSocket/SSE para status da operacao.
   - Atualizacao automatica do progresso da visita.

## Requisito de backend

### Novas entidades sugeridas

#### `VisitaNavio`

Campos minimos:

- `id`
- `navioId`
- `codigoVisita`
- `viagemEntrada`
- `viagemSaida`
- `linhaOperadora`
- `bercoPrevisto`
- `bercoAtual`
- `eta`
- `ata`
- `etb`
- `atb`
- `inicioOperacao`
- `fimOperacao`
- `etd`
- `atd`
- `fase`: `PREVISTA`, `FUNDEADA`, `ATRACADA`, `OPERANDO`, `OPERACAO_CONCLUIDA`, `PARTIU`, `CANCELADA`
- `observacoes`
- `criadoEm`
- `atualizadoEm`

#### `ItemOperacaoNavio`

Campos minimos:

- `id`
- `visitaNavioId`
- `tipoMovimento`: `EMBARQUE`, `DESCARGA`, `RESTOW`
- `codigoLote`
- `produto`
- `tipoCarga`: reaproveitar tipos siderurgicos existentes
- `quantidade`
- `pesoUnitarioToneladas`
- `pesoTotalToneladas`
- `poraoPlanejado`
- `poraoReal`
- `posicaoPlanejada`
- `posicaoReal`
- `origemPatio`
- `destinoPatio`
- `sequenciaOperacional`
- `status`: `PLANEJADO`, `LIBERADO`, `EM_MOVIMENTO`, `OPERADO`, `BLOQUEADO`, `CANCELADO`
- `motivoBloqueio`
- `observacoes`

#### `PlanoEstivaNavio`

Campos minimos:

- `id`
- `visitaNavioId`
- `versao`
- `status`: `RASCUNHO`, `VALIDADO`, `EM_EXECUCAO`, `CONCLUIDO`, `CANCELADO`
- `pesoTotalPlanejado`
- `pesoTotalRealizado`
- `criadoEm`
- `validadoEm`

#### `PosicaoEstivaNavio`

Campos minimos:

- `id`
- `planoEstivaId`
- `itemOperacaoId`
- `porao`
- `camada`
- `coluna`
- `bordo`: `BB`, `BE`, `CENTRO`
- `sequencia`
- `pesoToneladas`
- `status`

#### `EventoVisitaNavio`

Campos minimos:

- `id`
- `visitaNavioId`
- `itemOperacaoId` opcional
- `tipoEvento`
- `descricao`
- `usuario`
- `criadoEm`
- `dadosAntes` opcional
- `dadosDepois` opcional

### Endpoints minimos

#### Visitas

- `GET /visitas-navio?fase=&dataInicio=&dataFim=&navioId=`
- `GET /visitas-navio/{id}`
- `POST /visitas-navio`
- `PUT /visitas-navio/{id}`
- `PATCH /visitas-navio/{id}/fase`
- `DELETE /visitas-navio/{id}` somente se nao houver operacao iniciada

#### Itens de operacao

- `GET /visitas-navio/{id}/itens?tipoMovimento=&status=`
- `POST /visitas-navio/{id}/itens`
- `PUT /visitas-navio/{id}/itens/{itemId}`
- `PATCH /visitas-navio/{id}/itens/{itemId}/status`
- `PATCH /visitas-navio/{id}/itens/{itemId}/bloqueio`
- `DELETE /visitas-navio/{id}/itens/{itemId}` somente se status permitir

#### Plano de estiva

- `GET /visitas-navio/{id}/plano-estiva`
- `POST /visitas-navio/{id}/plano-estiva`
- `PUT /visitas-navio/{id}/plano-estiva/{planoId}/posicoes`
- `POST /visitas-navio/{id}/plano-estiva/{planoId}/validar`
- `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`

#### Indicadores

- `GET /visitas-navio/{id}/resumo-operacional`

Retorno esperado:

- total de itens planejados
- total de itens operados
- peso planejado
- peso operado
- percentual de progresso
- divergencias de porao/posicao
- itens bloqueados
- tempo de operacao

### Regras de negocio minimas

1. Nao permitir `codigoImo` duplicado.
2. Nao permitir `codigoVisita` duplicado.
3. Nao permitir lote duplicado dentro da mesma visita e mesmo tipo de movimento.
4. Nao permitir duas cargas na mesma posicao de estiva ativa.
5. Nao permitir peso total planejado por porao acima do limite configurado do navio, quando o limite existir.
6. Nao permitir editar visita em fase `PARTIU` ou `CANCELADA`, exceto observacao/evento administrativo.
7. Ao mudar fase, gravar evento automatico.
8. Ao operar item, atualizar resumo da visita.
9. Ao bloquear item, impedir conclusao enquanto houver item bloqueado obrigatorio.
10. Ao validar plano, retornar lista de alertas e erros separados.

## Requisito de frontend

### Rotas/telas sugeridas

1. `/navios`
   - Lista de navios.
   - Criar/editar navio.
   - Filtro por nome, IMO, armadora e status.

2. `/navios/:id`
   - Detalhe do navio.
   - Dados tecnicos.
   - Historico de visitas.

3. `/visitas-navio`
   - Board/lista de visitas por fase.
   - Filtro por periodo, berco, fase, navio e tipo de operacao.

4. `/visitas-navio/:id`
   - Cabecalho com navio, viagem, berco, ETA/ETB/ETD, fase e progresso.
   - Abas:
     - Resumo
     - Carga/Descarga
     - Plano de estiva
     - Eventos
     - Bloqueios
     - Indicadores

5. `/visitas-navio/:id/plano-estiva`
   - Visualizacao por porao.
   - Drag/drop ou formulario simples para posicionar lote.
   - Alertas de peso, duplicidade e posicao.
   - Botao validar plano.

### Componentes minimos

- `NaviosListComponent`
- `NavioFormComponent`
- `VisitasNavioListComponent`
- `VisitaNavioDetalheComponent`
- `ItensOperacaoTableComponent`
- `PlanoEstivaComponent`
- `ResumoOperacionalComponent`
- `EventosVisitaComponent`
- `BloqueiosOperacionaisComponent`

### UX minima

1. A tela inicial nao deve depender de iframe para operacao principal.
2. Toda acao operacional deve exibir feedback claro de sucesso/erro.
3. Status e fase devem ter cores consistentes.
4. O usuario deve conseguir ver rapidamente:
   - navio em operacao;
   - peso total planejado x realizado;
   - itens pendentes;
   - itens bloqueados;
   - divergencias;
   - proxima acao operacional.
5. O plano de estiva deve aceitar uma versao simples em tabela antes de qualquer desenho complexo.

## Criterios de aceite

1. Deve ser possivel cadastrar uma visita de navio vinculada a um navio existente.
2. Deve ser possivel listar visitas por fase e periodo.
3. Deve ser possivel detalhar uma visita com seus dados de viagem, berco e datas.
4. Deve ser possivel cadastrar itens de embarque e descarga para uma visita.
5. Deve ser possivel montar um plano de estiva simples por porao/posicao/sequencia.
6. O backend deve impedir lote duplicado dentro da mesma visita e movimento.
7. O backend deve impedir posicao duplicada ativa no mesmo plano.
8. Deve ser possivel alterar status de item e fase da visita com historico de evento.
9. Deve existir resumo operacional calculado pela API.
10. O frontend deve consumir APIs reais, sem mock, para navios, visitas, itens, plano e resumo.
11. A tela principal Angular deve exibir dados reais do backend e nao apenas assets estaticos.
12. Deve haver testes de service/controller para regras principais do backend.
13. Deve haver tratamento de erro padronizado para validacoes de negocio.

## Fora do escopo do primeiro corte

- Calculo nautico real de estabilidade.
- Integracao EDI completa BAPLIE/COPRAR/COARRI/VERMAS.
- Otimizacao automatica de estiva.
- WebSocket em tempo real.
- Integracao com equipamentos de patio/quay crane.

Esses itens devem ficar preparados na arquitetura, mas nao devem bloquear o MVP operacional.

## Ordem sugerida de implementacao

1. Criar entidades e migrations de `VisitaNavio`, `ItemOperacaoNavio`, `PlanoEstivaNavio`, `PosicaoEstivaNavio` e `EventoVisitaNavio`.
2. Criar DTOs, repositories, services e controllers das visitas.
3. Criar validacoes de fase, duplicidade de lote e duplicidade de posicao.
4. Criar endpoint de resumo operacional.
5. Criar telas Angular de lista/detalhe de visitas.
6. Criar tela de itens de carga/descarga.
7. Criar plano de estiva em tabela por porao/posicao.
8. Substituir iframe da tela principal pelo fluxo Angular integrado.
9. Adicionar testes backend.
10. Adicionar documentacao de uso dos endpoints.
