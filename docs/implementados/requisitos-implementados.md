# Requisitos implementados - CloudPort

## Instrucoes obrigatorias para agentes de IA

Esta pasta deve manter um unico arquivo: `docs/implementados/requisitos-implementados.md`.

Nao criar `README.md`, `AGENTS.md`, arquivos de evidencia, logs de execucao, historicos por data, rascunhos ou novos documentos dentro de `docs/implementados`. Este arquivo e o ponto unico de manutencao de tudo que ja foi implementado.

Todo agente de IA deve atualizar este mesmo arquivo quando concluir qualquer requisito. O texto implementado deve sair do arquivo unico de requisitos pendentes (`docs/requisitos/modulo-navios-back-front-gaps.md`) e entrar aqui com descricao objetiva do que foi entregue, incluindo APIs, telas do frontend, contratos, DTOs, testes, metricas e memorias de calculo quando existirem.

Se existir outro arquivo dentro de `docs/implementados`, o agente deve consolidar o conteudo util neste arquivo e remover o arquivo excedente no mesmo PR. Nao deixar conteudo duplicado entre implementados e requisitos pendentes.

## Rotina obrigatoria de atualizacao

1. Antes de desenvolver, ler este arquivo e `docs/requisitos/modulo-navios-back-front-gaps.md`.
2. Apos desenvolver, remover de `docs/requisitos/modulo-navios-back-front-gaps.md` tudo que foi implementado.
3. Registrar aqui a entrega sem duplicar pendencias.
4. Manter nomes de endpoints, DTOs, telas, servicos, entidades e contratos alinhados ao codigo.
5. Nao criar arquivos adicionais nesta pasta.

## Escopo deste arquivo

Este documento consolida os textos dos requisitos ja implementados no CloudPort para evitar que eles voltem como pendencia. O arquivo unico de pendencias permanece em `docs/requisitos/modulo-navios-back-front-gaps.md`.

## Modulo Navio implementado

1. Criar visita de navio.
2. Criar item operacional de embarque, descarga e restow.
3. Criar plano de estiva por visita.
4. Criar eventos da visita.
5. Criar resumo operacional da visita.
6. Criar endpoints basicos `/visitas-navio`.
7. Criar endpoints basicos de integracao em `/visitas-navio/{id}/integracao-patio`.
8. Adicionar campos de integracao em `ItemOperacaoNavio`.
9. Expor relatorio operacional integrado basico em `/visitas-navio/{id}/relatorio-operacional-integrado`.

## Integracao Navio + Patio implementada

1. Criar entidade simples de reserva de patio vinculada ao item de navio.
2. Adicionar campos de visita, item e plano em `OrdemTrabalhoPatio`.
3. Criar ordem real no `servico-yard` por endpoint interno `/yard/patio/ordens/navio`.
4. Impedir ordem ativa duplicada por `visitaNavioId + itemOperacaoNavioId` no `servico-yard`.
5. Expor no `servico-yard` filas operacionais por visita em `/yard/patio/ordens/visita-navio/{visitaNavioId}/filas`.
6. Expor no `servico-yard` ordens sem cobertura em `/yard/patio/ordens/visita-navio/{visitaNavioId}/sem-cobertura`.
7. Expor no `servico-navio-siderurgico` o proxy de filas em `/visitas-navio/{id}/integracao-patio/filas`.
8. Expor no `servico-navio-siderurgico` o proxy de excecoes em `/visitas-navio/{id}/integracao-patio/sem-cobertura`.
9. Permitir sincronizacao manual de status via `POST /visitas-navio/{id}/integracao-patio/sincronizar-status`.
10. Permitir gerar reservas de patio via `POST /visitas-navio/{id}/integracao-patio/reservas`.
11. Permitir gerar ordens de patio via `POST /visitas-navio/{id}/integracao-patio/gerar-ordens`.
12. Permitir replanejamento inicial via `POST /visitas-navio/{id}/integracao-patio/replanejar`.
13. Permitir alterar prioridade, suspender e retomar ordens de patio pelo contrato de integracao.

## Control Room inicial implementado no frontend

1. Criar painel Angular inicial Navio + Patio.
2. Criar alertas basicos de integracao.
3. Criar relatorio operacional integrado basico.
4. Criar contrato frontend `FilaPatioDaVisita`.
5. Consumir endpoints de filas e excecoes.
6. Exibir uma primeira visao Control Room com filtros, movimentos iminentes, filas/POW, ordens sem cobertura e auto-refresh por polling.
7. Exibir reservas, ordens, alertas e excecoes no painel operacional.
8. Permitir gerar reservas, gerar ordens, sincronizar status, replanejar, priorizar, suspender e retomar ordens no fluxo atual da tela.
9. Importar `WorkQueuePatioDaVisita` no `AppComponent`, manter estado `workQueuesPatio`, carregar `listarWorkQueuesPatio(visitaId)` dentro de `carregarIntegracaoPatio()` e limpar esse estado quando nao houver visita selecionada.
10. Disponibilizar helpers de frontend `workQueuesPatioFiltradas()` e `totalJobListWorkQueuesPatio()` para reutilizar filtros de status, berco, bloco/zona, POW, pool e equipamento.
11. Renderizar `workQueuesPatio` na tela Control Room com cards por fila persistente, exibindo identificador, status, berco, porao, bloco/zona, POW, pool, equipamento, prioridade, total de ordens e sequencia inicial.

## Work queues e contratos de patio implementados no backend

1. Criar contrato inicial de work queues de patio no `servico-yard`.
2. Listar work queues por visita via `GET /yard/patio/work-queues?visitaNavioId={id}`.
3. Criar work queue por `POST /yard/patio/work-queues`.
4. Ativar work queue por `PATCH /yard/patio/work-queues/{id}/ativar`.
5. Desativar work queue por `PATCH /yard/patio/work-queues/{id}/desativar`.
6. Associar POW e pool operacional a work queue por `PATCH /yard/patio/work-queues/{id}/pow`.
7. Associar equipamento a work queue por `PATCH /yard/patio/work-queues/{id}/equipamento`.
8. Expor job list de work queue por `GET /yard/patio/work-queues/{id}/job-list`.
9. Executar dispatch basico de work queue por `POST /yard/patio/work-queues/{id}/dispatch`.
10. Resetar work instruction por `POST /yard/patio/work-instructions/{id}/reset`.
11. Cancelar work instruction por `POST /yard/patio/work-instructions/{id}/cancelar`.
12. Expor work queues da visita no modulo de navio em `GET /visitas-navio/{id}/integracao-patio/work-queues`.

## Contratos de API implementados

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

Contrato resumido de `WorkQueuePatioRespostaDto` implementado:

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

O endpoint retorna work queues persistentes ou derivadas da visita, consultando o `servico-yard` quando disponivel.

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

## Contratos de frontend implementados

1. `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts` possui a interface `WorkQueuePatioDaVisita`.
2. `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts` possui o metodo `listarWorkQueuesPatio(visitaId)`.
3. O metodo `listarWorkQueuesPatio(visitaId)` chama `GET /visitas-navio/{id}/integracao-patio/work-queues`.
4. `frontend/servico-navio-siderurgico/src/app/app.component.ts` consome o contrato no carregamento do Control Room e mantem `workQueuesPatio` sincronizado com a visita selecionada.
5. `frontend/servico-navio-siderurgico/src/app/app.component.html` renderiza cards basicos de work queues persistentes com filtro reaproveitado por status e berco/bloco/zona.

Observacao: os cards basicos estao implementados. A job list expansivel no template, as acoes de work queue, o dispatch visual e as acoes de work instruction ainda permanecem pendentes em `docs/requisitos/modulo-navios-back-front-gaps.md`.

## Testes de contrato frontend implementados

1. `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.spec.ts` valida que `SiderurgicoApiService.listarWorkQueuesPatio(42)` consome `GET /visitas-navio/42/integracao-patio/work-queues` apos carregar `assets/configuracao.json`.
2. O teste cobre o payload TypeScript `WorkQueuePatioDaVisita` com `jobList` de `OrdemPatioDaVisita`, preservando campos de berco, porao, bloco/zona, POW, pool operacional, equipamento, prioridade operacional e total de ordens.
3. Esta entrega cobre apenas a regressao de contrato do service Angular. A renderizacao visual basica dos cards foi adicionada depois; testes de componente/e2e seguem pendentes.

## Itens que nao devem voltar como pendencia principal

1. Criar visita de navio.
2. Criar item operacional de embarque, descarga e restow.
3. Criar plano de estiva por visita.
4. Criar eventos e resumo operacional da visita.
5. Criar endpoints basicos `/visitas-navio`.
6. Criar endpoints basicos `/visitas-navio/{id}/integracao-patio`.
7. Criar entidade simples de reserva de patio vinculada ao item.
8. Adicionar campos de integracao em `ItemOperacaoNavio`.
9. Adicionar campos de visita, item e plano em `OrdemTrabalhoPatio`.
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
24. Ativar e desativar work queue pelo backend.
25. Associar POW, pool operacional e equipamento a work queue pelo backend.
26. Expor job list de work queue pelo backend.
27. Executar dispatch basico de work queue pelo backend.
28. Resetar e cancelar work instructions pelo contrato de patio.
29. Expor work queues da visita no modulo de navio em `/visitas-navio/{id}/integracao-patio/work-queues`.
30. Criar contratos publicos iniciais `/api/public/v1/vessel-visits`, stow plan, yard orders, work queues, events e yard reservations.
31. Adicionar contrato TypeScript `WorkQueuePatioDaVisita` e metodo frontend para consultar work queues.
32. Validar por teste unitario de service Angular o contrato `GET /visitas-navio/{id}/integracao-patio/work-queues`.
33. Carregar work queues persistentes no `AppComponent` por `carregarIntegracaoPatio()` e manter estado `workQueuesPatio` para a visita selecionada.
34. Renderizar cards basicos de work queues persistentes no Control Room.

## Arquivos de execucao consolidados e removidos de `docs/requisitos`

Os seguintes registros eram historicos de execucao automatica e foram consolidados nesta organizacao. Eles nao devem voltar para `docs/requisitos`, pois a pasta de requisitos deve conter somente o arquivo unico de pendencias.

```text
docs/requisitos/execucao-automatica-cloudport-20260708-2036.md
docs/requisitos/execucao-automatica-cloudport-20260708-2139.md
docs/requisitos/execucao-automatica-cloudport-20260708-2238.md
docs/requisitos/execucao-automatica-cloudport-20260709-0630.md
docs/requisitos/execucao-automatica-cloudport-20260709-1030.md
docs/requisitos/execucao-automatica-cloudport-20260709-1038.md
docs/requisitos/execucao-automatica-cloudport-20260709-claudeport-fallback.md
docs/requisitos/execucao-automatica-cloudport-20260709-frontend-workqueues.md
```
