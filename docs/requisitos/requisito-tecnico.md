# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após conclusão dos BUS1000, BUS1050, BUS1090, BUS1170 e DATA1180 na branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Operações intermodais de carga geral

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS10 | Completar o fluxo de stuff e unstuff com contêiner canônico e apontamentos idempotentes. | A operação valida e bloqueia o contêiner no inventário, rejeita repetição do mesmo apontamento e mantém saldo, movimentação, eventos e compensação consistentes. | 🟡 Em andamento |
| BUS1010 | Versionar e liberar planos de stuff e unstuff antes da execução física. | O plano é versionado, aprovado, validado por capacidade e conciliado por item sem alterar estoque antes da confirmação operacional. | ⬜ Pendente |
| BUS1020 | Implementar transload entre unidades com rastreabilidade e atualização atômica. | Origem, destino, lotes, quantidades, lacres, divergências e avarias são persistidos sem saldo negativo ou atualização parcial. | ⬜ Pendente |
| BUS1030 | Integrar Gate à carga geral para retirada e entrega parcial. | O Gate reserva quantidade por BL, delivery order e cargo lot e só confirma o estoque após o estágio físico correspondente. | ⬜ Pendente |
| BUS1040 | Implementar planejamento de pátio e armazém para cargo lots. | Allocation, capacidade, restrições, origem, destino, recurso e saldo por posição são persistidos e confirmados pela execução. | ⬜ Pendente |
| BUS1060 | Implementar plano de carga e descarga de cargo lots por visita ferroviária. | Lotes são planejados e executados por vagão e posição, com capacidade, incompatibilidades, sequência e custódia persistida. | ⬜ Pendente |
| BUS1070 | Implementar ciclo completo de avaria da carga. | Avarias possuem quantidade afetada, evidências, responsável, bloqueio, inspeção, reparo ou baixa e saldo segregado. | ⬜ Pendente |
| BUS1080 | Implementar identificação e inventário físico reconciliável de cargo lots. | Código de barras ou QR identifica lote e embalagem; contagens e divergências geram ajuste motivado sem sobrescrever diretamente o saldo lógico. | ⬜ Pendente |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/StuffUnstuffServico.java` | `criarOperacaoStuffUnstuff()` | Cria a operação, bloqueia cargo lots e valida saldo ou capacidade, mas recebe `conteinerId` como texto sem validar nem bloquear a unidade canônica. | Integrar porta local do inventário para validar existência, disponibilidade e vínculo operacional do contêiner. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/StuffUnstuffServico.java` | `registrarExecucao()` | Atualiza item, lote, movimentação e eventos na mesma transação, mas não possui chave idempotente para retries. | Adicionar `commandId` ou `eventId` único e devolver o resultado já aplicado em repetições equivalentes. |
| `frontend/cloudport/src/pages/StuffUnstuffPage.jsx` | criação da operação | A tela alcança o backend, mas usa identificador textual do contêiner. | Selecionar unidade elegível do inventário canônico. |

### BUS1010 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/StuffUnstuffServico.java` | `criarOperacaoStuffUnstuff()` e `registrarExecucao()` | Há planejamento inicial e execução parcial, mas não há versão nem liberação explícita; o primeiro apontamento inicia a operação. | Criar versão imutável, comando `liberarPlano()` e conciliação por item. |
| `frontend/cloudport/src/pages/StuffUnstuffPage.jsx` | planejamento | Não existe histórico de versões, aprovação ou comparação. | Exibir versões e estado de liberação antes da execução. |

### BUS1020 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `transferirEntreLotes()` | Consolidação e desconsolidação não registram unidades de transporte de origem e destino nem operação recuperável. | Criar novo método sugerido: `executarTransload()` com bloqueio dos lotes e unidades. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dto/CargaGeralDTOs.java` | `RegistrarMovimentacaoRequest` | O contrato não representa múltiplos lotes, unidades, lacres, conferência e divergências. | Criar DTO específico de transload. |

### BUS1030 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `registrarEntrada()` e `registrarSaida()` | Valida agendamento e TOS, mas não reserva nem consome quantidade de cargo lot. | Criar porta local para reservar, confirmar e compensar quantidade. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/CargaGeralControlador.java` | comandos internos | Não há comandos idempotentes ligados à transação de Gate. | Criar comandos de reserva, confirmação e compensação por BL, ordem e lote. |

### BUS1040 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/LoteCarga.java` | `armazemId` e `posicaoArmazenagem` | Mantém somente a localização corrente. | Criar allocation e instrução com origem, destino, recurso, prioridade, estado e quantidades. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/BayPlanServico.java` | planejamento do Yard | O planejamento é orientado a unidades conteinerizadas. | Criar adaptador para reservar capacidade e confirmar movimentos de cargo lot. |

### BUS1060 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | lista de trabalho | Opera contêineres e ordens ferroviárias, sem cargo lots e quantidades por vagão. | Estender por contrato específico de carga geral. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/LoteCarga.java` | vínculos de transporte | Não mantém visita ferroviária, vagão, posição e histórico de custódia. | Adicionar associação persistida à operação ferroviária. |

### BUS1070 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarAvaria()` | Sobrescreve um único código e descrição e muda todo o lote para avariado. | Criar agregado de avaria com quantidade afetada, anexos, responsável, estado, bloqueio e reparo. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `registerDamage()` | Envia somente código e descrição. | Criar inspector de avarias com evidências e segregação de saldo. |

### BUS1080 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `AJUSTE_INVENTARIO` | Altera diretamente o saldo sem sessão de contagem e aprovação. | Criar novos métodos sugeridos: `abrirInventarioFisico()`, `registrarContagem()` e `confirmarDivergencia()`. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | seleção de lote | A identificação é manual. | Criar leitura por código de barras ou QR e inventário por posição. |

## 2. Gate, pátio e controle de equipamentos

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1100 | Implementar chamada operacional de caminhões com fila persistida. | Posição, prioridade, gate ou pista, chamada, aceite, expiração, rechamada e cancelamento são persistidos. | ⬜ Pendente |
| BUS1110 | Impedir truck hopping por visita ativa. | Motorista, cavalo, chassis e unidades não participam simultaneamente de visitas conflitantes e são liberados atomicamente. | ⬜ Pendente |
| BUS1120 | Implementar tratamento de unidades fora de posição. | A divergência gera caso, bloqueio, investigação e instrução corretiva com origem e destino confirmados. | ⬜ Pendente |
| BUS1130 | Implementar Lost & Found e unidades TBD. | Unidade sem registro ou sem localização entra em caso persistido com investigação, associação, baixa e encerramento. | ⬜ Pendente |
| INT1140 | Integrar VMT à confirmação de work instructions. | Aceite, início e conclusão atualizam a instrução uma única vez e rejeitam evento duplicado ou fora de sequência. | ⬜ Pendente |

### BUS1100 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `confirmarChegadaAntecipada()` e `registrarEntrada()` | Muda estados de agendamento e gate pass, mas não existe entidade de chamada com posição, aceite ou expiração. | Criar novo agregado sugerido: `ChamadaCaminhao`. |
| `frontend/cloudport/src/pages/gate/GateVisualOperationsPage.jsx` | filas e pistas | Não consome contrato persistido de chamada. | Exibir histórico, estado e confirmação da chamada. |

### BUS1110 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `obterOuCriarGatePass()` e `registrarEntrada()` | Não comprova exclusividade conjunta de motorista, caminhão, chassis e unidades em visita ativa. | Criar validação transacional e retorno funcional de conflito. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/model/GatePass.java` | vínculos da passagem | Não existe ocupação explícita dos recursos com liberação atômica. | Persistir vínculos ativos e liberá-los no encerramento ou cancelamento. |

### BUS1120 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/AlertasPatioServico.java` | alertas | O alerta informa a condição, mas não cria caso com bloqueio, investigação e correção. | Criar novo agregado sugerido: `DivergenciaPosicaoPatio`. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/ValidadorYardPlacementService.java` | validação de posição | Impede posição inválida, mas não resolve unidade já encontrada fora da posição lógica. | Criar comando de reconciliação com movimento corretivo auditável. |

### BUS1130 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/inventario/controlador/InventarioCanonicoControlador.java` | inventário e divergências | Registra unidades conhecidas, mas não possui caso específico para unidade sem registro ou não localizada. | Criar endpoints de abertura, associação, investigação, regularização e baixa. |
| `frontend/cloudport/src/pages/InventoryReportsPages.jsx` | relatórios | Não oferece fila operacional de investigação e encerramento. | Criar tela com estado, responsável, evidências e decisão final. |

### INT1140 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio` | work instructions | Não há contrato comprovado que use confirmação real de VMT com deduplicação e sequência. | Criar porta de eventos com `eventId`, `instructionId`, estado esperado, timestamp e resultado. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/controlroom` | conclusão operacional | A instrução pode ser concluída sem confirmação idempotente do equipamento. | Integrar aceite, início, falha e conclusão do VMT ao ciclo persistido. |

## 3. Navio e ferrovia

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1150 | Registrar execução da sequência de guindastes. | Cada movimento possui guindaste, ordem, janela, início, conclusão, exceção e quantidade realizada. | ⬜ Pendente |
| BUS1160 | Implementar operação persistida de tampas de porão. | Tarefas de abrir, remover, posicionar e fechar controlam dependências e bloqueiam movimentos incompatíveis. | ⬜ Pendente |

### BUS1150 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio` | sequência de guindastes | Existe planejamento, mas não agregado de execução por movimento. | Criar novo agregado sugerido: `ExecucaoSequenciaGuindaste`. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | plano visual | Não apresenta linha do tempo persistida de execução real. | Exibir progresso, exceções e reconciliação. |

### BUS1160 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/SlotNavio.java` | geometria do slot | Não existe ciclo operacional de tampas e dependências. | Criar entidades de tampa, posição, tarefa e dependência. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | overlay de tampas | O overlay não bloqueia transacionalmente movimentos incompatíveis. | Integrar ao estado persistido e impedir início incompatível. |
