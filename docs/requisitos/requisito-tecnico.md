# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-16 após implementação de ASYNC10, ASYNC20, ASYNC30, STATE10 e DATA10.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Arquitetura e fluxos principais

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ARCH10 | Substituir a chamada HTTP interna do otimizador do Yard por uma porta local no runtime modular. | A otimização Navio + Pátio executa no `cloudport-runtime` por chamada local ao módulo Yard; o adaptador HTTP permanece condicionado somente ao modo de rollback. | ⬜ Pendente |
| BUS10 | Aplicar o resultado real do otimizador no replanejamento de pátio. | O replanejamento usa o plano calculado para atualizar reservas, posições, ordens e vínculos operacionais em uma transação, compensa o estado anterior em falha e calcula ganho e risco com dados do plano. | ⬜ Pendente |

### ARCH10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/cliente/OtimizacaoYardCliente.java` | `otimizar(Map<String, Object>)` | O componente sempre usa `RestTemplate` para chamar `/api/scheduler/gerar-plano`, mesmo quando Navio Siderúrgico e Yard executam no mesmo processo. | Transformar o cliente em porta e manter esta implementação como adaptador HTTP condicionado ao modo de rollback. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/scheduler/servico/PredictiveSchedulerService.java` | `gerarPlanoOperacional(...)` | O serviço real do otimizador já está incorporado ao runtime, mas não é chamado diretamente pelo módulo consumidor. | Criar adaptador local que converta o contrato de entrada e invoque o serviço do Yard sem atravessar HTTP. |
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | composição dos módulos | O runtime incorpora os dois módulos, porém não registra uma implementação local para o contrato de otimização. | Registrar a implementação local como padrão do runtime e impedir a ativação simultânea dos adaptadores local e HTTP. |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/IntegracaoNavioPatioServico.java` | `replanejarPatioDaVisita()` | O fluxo apenas troca reservas item a item; não consome o plano do otimizador e retorna ganho fixo `12` e risco derivado somente da existência de falhas. | Consumir um resultado de otimização identificável, validar todas as posições e recursos e aplicar as mudanças de forma atômica. Calcular ganho e risco a partir do plano aplicado. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/OtimizacaoGlobalNavioPatioServico.java` | `otimizar()` | O método monta e devolve um plano em transação somente leitura, mas não possui comando para aplicar ou versionar o resultado. | Persistir uma proposta versionada ou devolver um identificador imutável; criar `novo método sugerido: aplicarPlanoOtimizado()` para efetivar reservas, ordens e work queues com validação de concorrência. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/dto/ResultadoReplanejamentoPatioNavioDTO.java` | ganho e risco | Os campos aceitam valores que não correspondem a uma memória de cálculo real. | Preencher os campos com indicadores calculados e rastreáveis do plano aplicado, sem constantes artificiais. |

## 2. Eventos, integrações e processamento assíncrono

Nenhuma pendência técnica permanece nesta seção após a implementação de ASYNC10, ASYNC20 e ASYNC30.

## 3. Interface e navegação operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Expor no frontend a edição de visita e item e a conclusão do plano de estiva. | Usuário autorizado consegue editar dados pelos contratos existentes e concluir o plano após validação, com retorno de erro exibido sem atualizar a tela como sucesso. | ⬜ Pendente |
| UI20 | Tornar o Quay Monitor e o painel de equipamentos operacionais, não apenas consultivos. | O frontend salva plano de guindastes com work queues validadas, exibe drill-down de work instruction e usa job lists por equipamento e transições oficiais. | ⬜ Pendente |

### UI10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | objeto `api` | Não há métodos para `PUT /visitas-navio/{id}`, `PUT /visitas-navio/{id}/itens/{itemId}` ou `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`. | Adicionar clientes tipados para os três contratos e propagar motivo, usuário e correlação quando aplicável. |
| `frontend/servico-navio-siderurgico/src/AdvancedApp.jsx` | fluxo de visita, itens e plano | A interface permite ações operacionais de pátio, mas não oferece formulários de edição nem publicação do plano. | Criar formulários com estado de carregamento, validação do contrato, confirmação de conclusão e recarga somente após resposta persistida. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/VisitaNavioControlador.java` | `atualizar()`, `atualizarItem()` e `concluirPlano()` | Os endpoints existem e são alcançáveis no backend, porém não possuem consumidor na interface operacional. | Preservar os contratos e garantir que o frontend use os identificadores retornados, sem criar rota concorrente. |

### UI20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | `obterQuayMonitor()` e métodos de work queue | O Quay Monitor é somente leitura e as operações de fila usam os endpoints legados. | Adicionar gravação de `POST /visitas-navio/{id}/crane-plan`, consulta do plano, recursos operacionais, drill-down, matriz de estados e job lists por equipamento. |
| `frontend/servico-navio-siderurgico/src/AdvancedApp.jsx` | `QuayMonitor`, `CheDetail` e `WorkQueue` | Os componentes exibem dados, mas não conectam o plano de guindastes nem os controles novos do Yard ao fluxo real. | Permitir selecionar work queue válida por alocação, salvar o plano, abrir drill-down e executar somente transições permitidas pelo backend. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/QuayBerthCraneControlador.java` | contratos de plano e monitor | O backend possui leitura e gravação, mas a gravação não é usada pela interface. | Manter uma única rota de gravação e retornar os dados necessários para atualização imediata do monitor. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/controlador/WorkQueueOperacaoControlador.java` | `/recursos-operacionais`, `/drill-down`, `/matriz-estados` e `/equipamentos/job-lists` | Os contratos robustos existem, mas não são consumidos pelo Control Room. | Fazer a interface utilizar esses contratos como fonte de verdade para ações e detalhamento. |
