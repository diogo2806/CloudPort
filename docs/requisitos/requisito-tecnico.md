# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após conclusão dos BUS1000, BUS1010, BUS1030, BUS1050, BUS1090, BUS1100, BUS1110, BUS1150, BUS1160, BUS1170 e DATA1180 na branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Operações intermodais de carga geral

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS10 | Completar o fluxo de stuff e unstuff com contêiner canônico e apontamentos idempotentes. | A operação valida e bloqueia o contêiner no inventário, rejeita repetição do mesmo apontamento e mantém saldo, movimentação, eventos e compensação consistentes. | 🟡 Em andamento |
| BUS1020 | Implementar transload entre unidades com rastreabilidade e atualização atômica. | Origem, destino, lotes, quantidades, lacres, divergências e avarias são persistidos sem saldo negativo ou atualização parcial. | ⬜ Pendente |
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

### BUS1020 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `transferirEntreLotes()` | Consolidação e desconsolidação não registram unidades de transporte de origem e destino nem operação recuperável. | Criar novo método sugerido: `executarTransload()` com bloqueio dos lotes e unidades. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dto/CargaGeralDTOs.java` | `RegistrarMovimentacaoRequest` | O contrato não representa múltiplos lotes, unidades, lacres, conferência e divergências. | Criar DTO específico de transload. |

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
| BUS1120 | Implementar tratamento de unidades fora de posição. | A divergência gera caso, bloqueio, investigação e instrução corretiva com origem e destino confirmados. | ⬜ Pendente |
| BUS1130 | Implementar Lost & Found e unidades TBD. | Unidade sem registro ou sem localização entra em caso persistido com investigação, associação, baixa e encerramento. | ⬜ Pendente |
| INT1140 | Integrar VMT à confirmação de work instructions. | Aceite, início e conclusão atualizam a instrução uma única vez e rejeitam evento duplicado ou fora de sequência. | ⬜ Pendente |

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
