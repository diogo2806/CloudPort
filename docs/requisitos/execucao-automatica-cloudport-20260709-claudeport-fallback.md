# Execucao automatica CloudPort - 2026-07-09

## Escopo confirmado

Repositorio correto confirmado pelo usuario: `diogo2806/CloudPort`.

Esta execucao revisa os requisitos em `docs/requisitos` relacionados a back-end e front-end, com foco na lacuna entre os contratos ja expostos de work queues e a tela Angular Control Room.

## Arquivos de requisito analisados

- `docs/requisitos/modulo-navios-back-front-gaps.md`
- `docs/requisitos/execucao-automatica-cloudport-20260708-2036.md`
- `docs/requisitos/execucao-automatica-cloudport-20260708-2238.md`

## Situacao encontrada

O requisito principal ja registra que o backend e o service Angular possuem o contrato para work queues persistentes:

- `GET /visitas-navio/{id}/integracao-patio/work-queues`
- Tipo frontend `WorkQueuePatioDaVisita`
- Metodo `listarWorkQueuesPatio(visitaId)` no `SiderurgicoApiService`

A lacuna de integracao Back x Front permanece no componente principal do frontend:

- `frontend/servico-navio-siderurgico/src/app/app.component.ts` ainda importa `FilaPatioDaVisita`, mas nao importa `WorkQueuePatioDaVisita`.
- O componente ainda possui `filasPatio`, mas nao possui estado `workQueuesPatio`.
- `carregarIntegracaoPatio()` ainda chama `listarFilasPatio(visitaId)`, mas nao chama `listarWorkQueuesPatio(visitaId)`.
- `substituirOrdemPatio()` ainda atualiza `ordensPatio`, `ordensSemCobertura` e `filasPatio`, mas nao propaga alteracoes para `workQueuesPatio.jobList`.
- `frontend/servico-navio-siderurgico/src/app/app.component.html` ainda renderiza a tabela antiga de `Filas / POW operacionais`, sem cards de work queue persistente com POW, pool, equipamento e job list expandivel.

## Patch funcional pendente

### `frontend/servico-navio-siderurgico/src/app/app.component.ts`

- Importar `WorkQueuePatioDaVisita` de `./siderurgico-api.service`.
- Adicionar `workQueuesPatio: WorkQueuePatioDaVisita[] = [];` junto de `filasPatio`.
- Adicionar `workQueuesPatioFiltradas()` usando filtro de status e bloco/zona/berco/POW/pool/equipamento.
- Limpar `workQueuesPatio` quando nao houver visita selecionada.
- Chamar `this.workQueuesPatio = await this.api.listarWorkQueuesPatio(visitaId);` em `carregarIntegracaoPatio()`.
- Atualizar `substituirOrdemPatio()` para propagar a ordem atualizada dentro de `workQueue.jobList`.

### `frontend/servico-navio-siderurgico/src/app/app.component.html`

- Inserir antes da tabela `Filas / POW operacionais` uma grade de cards de work queue persistente.
- Cada card deve mostrar identificador, status, berco, porao, bloco/zona, POW, pool operacional, equipamento, prioridade e total de ordens.
- Cada card deve expor `details/summary` com job list expandivel contendo sequencia, lote, movimento, origem, destino e status.

### `frontend/servico-navio-siderurgico/src/app/app.component.css`

- Adicionar estilos `.work-queue-grid`, `.work-queue-card`, `.work-queue-card header`, `.work-queue-card p`, `.work-queue-card details` e `.work-queue-card summary`.

## Atualizacao do requisito

Nao foi correto marcar a pendencia como entregue nesta execucao, porque o patch de codigo nao foi aplicado com seguranca.

Continuam pendentes no requisito:

- Consumo real no componente Angular de `GET /visitas-navio/{id}/integracao-patio/work-queues`.
- Estado `workQueuesPatio` no `AppComponent`.
- Cards de work queue persistente no Control Room.
- Job list expandivel por work queue persistente.
- Ativar/desativar work queue pela tela.
- Associar/editar POW, pool operacional e equipamento pela tela.
- Dispatch de work queue pelo frontend.
- Reset/cancelamento de work instruction pela tela.
- Drill-down completo de historico operacional.
- SSE/WebSocket substituindo polling.
- Contratos OpenAPI e testes de contrato.

## Validacoes executadas

Validacao por leitura via conector GitHub:

- `diogo2806/CloudPort` foi confirmado como repositorio correto.
- `docs/requisitos/modulo-navios-back-front-gaps.md` contem a pendencia de work queues no frontend.
- `app.component.ts` ainda nao possui `workQueuesPatio` nem chamada a `listarWorkQueuesPatio(visitaId)`.
- `app.component.html` ainda nao possui cards de work queue persistente.

Nao foi possivel executar build Angular nem testes automatizados porque o ambiente local nao conseguiu clonar o repositorio para aplicar diff e rodar validacoes.

## Bloqueio para merge automatico

Este PR deve permanecer aberto, sem merge automatico, pelos seguintes motivos:

1. O patch funcional exige substituicao segura de arquivos grandes (`app.component.ts`, `app.component.html`, `app.component.css`).
2. O ambiente local nao conseguiu clonar o repositorio para aplicar diff e rodar build/testes.
3. Sem build/testes, nao e seguro afirmar que a alteracao compila.
4. O PR documenta o bloqueio e o patch exato para aplicacao segura em uma execucao com clone/build disponiveis.
