# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após conclusão dos BUS10, BUS1000, BUS1010, BUS1040, BUS1050, BUS1090, BUS1100, BUS1110, BUS1150, BUS1160, BUS1170, DATA1180 e INT1140 na branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Operações intermodais de carga geral

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1020 | Implementar transload entre unidades com rastreabilidade e atualização atômica. | Origem, destino, lotes, quantidades, lacres, divergências e avarias são persistidos sem saldo negativo ou atualização parcial. | ⬜ Pendente |
| BUS1030 | Integrar Gate à carga geral para retirada e entrega parcial. | O Gate reserva quantidade por BL, delivery order e cargo lot e só confirma o estoque após o estágio físico correspondente. | ⬜ Pendente |
| BUS1060 | Implementar plano de carga e descarga de cargo lots por visita ferroviária. | Lotes são planejados e executados por vagão e posição, com capacidade, incompatibilidades, sequência e custódia persistida. | ⬜ Pendente |
| BUS1070 | Implementar ciclo completo de avaria da carga. | Avarias possuem quantidade afetada, evidências, responsável, bloqueio, inspeção, reparo ou baixa e saldo segregado. | ⬜ Pendente |
| BUS1080 | Implementar identificação e inventário físico reconciliável de cargo lots. | Código de barras ou QR identifica lote e embalagem; contagens e divergências geram ajuste motivado sem sobrescrever diretamente o saldo lógico. | ⬜ Pendente |

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

### BUS1120 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/AlertasPatioServico.java` | alertas | O alerta informa a condição, mas não cria caso com bloqueio, investigação e correção. | Criar novo agregado sugerido: `DivergenciaPosicaoPatio`. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/ValidadorYardPlacementService.java` | validação de posição | Impede posição inválida, mas não resolve unidade já encontrada fora da posição lógica. | Criar comando de reconciliação com movimento corretivo auditável. |
