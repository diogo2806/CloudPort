# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após implementação do requisito `STATE30`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Planejamento de embarque

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI40 | Reabrir o fluxo React de planejamento de steel coils com contratos compatíveis com a API bulk. | A seleção de navio e visita lista planos persistidos, a criação envia o identificador obrigatório da visita e o cálculo de tacktop usa o método HTTP publicado pelo backend, sem respostas `400` ou `405` causadas pelo próprio portal. | ⬜ Pendente |
| UI60 | Reabrir a criação de plano do Vessel Planner vinculando a escala selecionada. | `POST /api/vessel-planner/planos` recebe `bayPlanId` e `visitaNavioId` da escala selecionada; o plano é criado e persiste a identidade canônica da visita utilizada na tela. | ⬜ Pendente |

### UI40 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/SteelCoilPlannerPage.jsx` | `loadPlans()` | A tela chama a listagem por navio e código de viagem, porém a API atual não publica `GET /api/estivagem-bulk/planos`; a seleção de plano não pode ser carregada pelo fluxo normal. | Consumir uma listagem persistida compatível com navio e visita. Caso a seleção por lista seja mantida, criar no backend o novo método sugerido: `listarPlanos(Long navioId, Long visitaNavioId)`. |
| `frontend/cloudport/src/pages/SteelCoilPlannerPage.jsx` | `createPlan()` | A visita é selecionada na interface, mas o corpo contém apenas `navioId`, `codigoViagem`, `portoCarga` e `portoDescarga`. O backend exige `visitaNavioId`, portanto a criação é rejeitada pela validação. | Enviar `visitaNavioId` com o `id` da visita selecionada e manter o código de viagem apenas como atributo derivado do contexto canônico. |
| `frontend/cloudport/src/pages/SteelCoilPlannerPage.jsx` | `loadSecuring()` | O comando depende de `calcularSecuringEstivagemBulk()`, que usa `POST`, embora o contrato publicado seja somente leitura. | Chamar o contrato de tacktop com o método HTTP efetivamente publicado e exibir o resultado retornado, sem tratar uma consulta como comando persistente. |
| `frontend/cloudport/src/api.js` | `listarPlanosEstivagemBulk()`, `criarPlanoEstivagemBulk()` e `calcularSecuringEstivagemBulk()` | A listagem aponta para rota inexistente, a criação apenas repassa um corpo que não contém a visita e tacktop é chamado por `POST`. | Alinhar assinaturas, parâmetros e métodos HTTP aos contratos corrigidos e exigir `visitaNavioId` na criação. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/controlador/EstivaBulkControlador.java` | `/api/estivagem-bulk/planos` e `/planos/{id}/tacktop` | O caminho `/planos` possui apenas `POST`; tacktop possui apenas `GET`. | Publicar a consulta filtrada necessária para a tela ou remover a seleção por listagem, e manter o cliente aderente ao `GET` de tacktop. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/dto/CriarPlanoEstivaBulkRequisicaoDto.java` | campo `visitaNavioId` | O campo é obrigatório com `@NotNull`, mas não é enviado pelo portal React. | Preservar a obrigatoriedade e corrigir o produtor do contrato no frontend. |

### UI60 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/ContainerVesselPlannerPage.jsx` | `createPlan()` e `selectedScale` | A tela exige e exibe uma escala, mas cria o plano chamando a API somente com `bayPlan.id`; `selectedScale.id` não participa do comando. | Bloquear a criação sem escala válida e enviar `selectedScale.id` como `visitaNavioId` junto com `bayPlan.id`. |
| `frontend/cloudport/src/api.js` | `criarPlanoVesselPlanner()` | A assinatura aceita somente `bayPlanId` e produz `{ bayPlanId }`. | Alterar a assinatura para receber também `visitaNavioId` e produzir os dois campos obrigatórios. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/dto/CriarEstivagemPlanRequisicaoDto.java` | campos `bayPlanId` e `visitaNavioId` | Ambos são `@NotNull`; o corpo atual do portal sempre omite a visita e é rejeitado antes do serviço. | Manter o contrato obrigatório e corrigir o frontend para fornecer a identidade da escala selecionada. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/controlador/VesselPlannerControlador.java` | `criarPlano()` | O controller encaminha os dois identificadores ao serviço, mas o produtor React fornece somente um deles. | Manter o vínculo canônico existente no backend e tornar o fluxo React compatível com o contrato. |

## 2. Mapa operacional do pátio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI50 | Reabrir a sobreposição de destinos planejados das work instructions no mapa. | Toda ordem não final com destino válido marca exatamente a pilha definida por `linhaDestino`, `colunaDestino` e `camadaDestino`; a camada é exibida como planejada sem alterar a ocupação real. | ⬜ Pendente |

### UI50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/yard/YardShared.jsx` | `positionKey()` e `buildStacks()` | A mesma função lê `linha` e `coluna` tanto para posições quanto para ordens. As ordens retornam `linhaDestino` e `colunaDestino`, então a chave planejada fica sem coordenadas e nunca coincide com a posição real. | Separar a chave da posição da chave de destino; novo método sugerido: `orderDestinationKey(order)`, usando `linhaDestino`, `colunaDestino` e `camadaDestino`. |
| `frontend/cloudport/src/pages/yard/YardMapPages.jsx` | `YardMapPage()` e `layer.plannedOrder` | A interface possui estado visual para destino planejado, mas ele depende do mapa produzido por `buildStacks()` e permanece ausente para contratos reais de ordem. | Consumir a associação corrigida e manter ocupação real, restrição e destino planejado como estados distintos. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/dto/OrdemTrabalhoPatioRespostaDto.java` | `linhaDestino`, `colunaDestino` e `camadaDestino` | O contrato publica explicitamente as coordenadas de destino e não publica `linha` ou `coluna`. | Preservar o contrato e ajustar somente o consumidor frontend para os nomes oficiais. |

## 3. Telas operacionais do Gate

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI70 | Exibir os agendamentos da visão consolidada nas rotas de relatório e console do Gate. | `/home/gate/relatorios`, `/home/gate/operador/console` e `/home/gate/operador/eventos` renderizam os registros de `agendamentos` retornados pelo backend, com estado vazio somente quando essa lista estiver realmente vazia. | ⬜ Pendente |

### UI70 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/OperationalPages.jsx` | `GenericDatasetPage()` e `DATASET_ROUTES` do Gate | As rotas usam `api.obterCentralGate()` como se a resposta inteira fosse uma lista paginada. `normalizePage()` não reconhece o campo `agendamentos`, produz zero linhas e a tela mostra estado vazio mesmo com registros retornados. | Fornecer ao componente somente `response.agendamentos` ou adicionar um seletor explícito por rota; não converter o objeto consolidado inteiro em lista. |
| `frontend/cloudport/src/api.js` | `obterCentralGate()` | O método retorna corretamente o objeto consolidado da API, sem normalização para as telas genéricas. | Manter o objeto para o dashboard e criar um método ou adaptador específico que retorne `agendamentos` para as rotas tabulares. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/cidadao/centralacao/dto/CentralAcaoAgendamentoRespostaDTO.java` | campo `agendamentos` | O backend publica a coleção em uma propriedade própria, ao lado de `usuario` e `situacaoPatio`. | Preservar o contrato e fazer as telas consumirem a propriedade correta. |
