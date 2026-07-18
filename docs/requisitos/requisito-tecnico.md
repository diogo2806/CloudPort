# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Operações intermodais de carga geral

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS10 | Implementar fluxo operacional persistido de stuff e unstuff vinculado a contêiner, cargo lots e local de operação. | O operador cria uma ordem de estufagem ou desova, seleciona o contêiner e os lotes, registra quantidades planejadas e realizadas por etapa, executa parcialmente, trata divergências e avarias, e conclui ou cancela a operação com atualização transacional dos saldos e do histórico. | ⬜ Pendente |
| BUS1000 | Implementar ordens de trabalho de carga com ciclo de vida, itens, recursos e eventos de serviço. | Cada serviço operacional possui ordem persistida com tipo, prioridade, janela, local, lotes, equipe ou equipamento, estados planejada, liberada, em execução, concluída e cancelada, além de eventos realizados e cancelamento motivado. | ⬜ Pendente |
| BUS1010 | Separar planejamento de stuff e strip da execução física e controlar planejado versus realizado. | Planos de estufagem e desova são versionados, validados por capacidade e saldo, liberados para execução e conciliados por item sem alterar estoque antes da confirmação operacional. | ⬜ Pendente |
| BUS1020 | Implementar transload entre unidades com rastreabilidade de origem, destino e atualização atômica dos saldos. | A transferência entre contêineres, veículos ou áreas registra unidade de origem e destino, múltiplos lotes, quantidades, divergências, lacres e avarias, sem saldo negativo ou atualização parcial. | ⬜ Pendente |
| BUS1030 | Integrar Gate à carga geral para agendamento, retirada e entrega parcial por BL, delivery order e cargo lot. | A transação de Gate reserva quantidade liberada, valida veículo, documento e janela, suporta atendimento parcial e somente baixa ou recebe estoque após conclusão do estágio físico correspondente. | ⬜ Pendente |
| BUS1040 | Implementar planejamento de pátio e armazém para cargo lots com allocation, posição e instruções de trabalho. | O planejador define faixa ou posição válida, capacidade e restrições; a execução gera instruções para CHE, registra origem e destino reais e mantém histórico de ocupação e saldo por posição. | ⬜ Pendente |
| BUS1050 | Implementar plano operacional de carga e descarga de cargo lots por visita de navio. | O plano associa BL, lote, porão ou área, sequência, equipamento e quantidades; permite execução parcial, reconciliação, cancelamento e atualização transacional entre bordo, cais e estoque. | ⬜ Pendente |
| BUS1060 | Implementar plano operacional de carga e descarga de cargo lots por visita ferroviária e vagão. | Cada lote é planejado para vagão e posição, com capacidade, incompatibilidades, sequência, execução parcial e confirmação da custódia sem reduzir o processo a vínculo textual de visita. | ⬜ Pendente |
| BUS1070 | Implementar fluxo completo de avaria, inspeção, evidências, segregação e reparo da carga. | Uma carga pode possuir múltiplas avarias com quantidade afetada, fotos ou documentos, responsável, bloqueio, inspeção, reparo ou baixa e histórico, preservando o saldo disponível separado do saldo avariado. | ⬜ Pendente |
| BUS1080 | Implementar identificação por código de barras ou QR e inventário físico reconciliável de cargo lots. | O operador identifica lote e embalagem no ponto de operação, registra contagem física por posição, apura divergências e confirma ajuste motivado e auditável sem sobrescrever diretamente o saldo lógico. | ⬜ Pendente |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/CargaGeralTipos.java` | `TipoMovimentacaoCarga` | O domínio possui recebimento, carga ou descarga parcial, consolidação e desconsolidação, mas não representa ordem, ciclo de vida ou etapas próprias de estufagem e desova. | Criar contratos específicos para tipo e status da operação de stuff e unstuff, sem reduzir o processo a uma movimentação genérica de estoque. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarMovimentacao(UUID, RegistrarMovimentacaoRequest)` | O método altera o saldo de um único lote e registra uma movimentação. Não vincula contêiner, conjunto de lotes, planejamento, conferência, lacre, execução parcial, divergência ou fechamento atômico. | Implementar novo método sugerido: `criarOperacaoStuffUnstuff()`, além de comandos transacionais para iniciar, registrar execução parcial, concluir e cancelar. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `blankMovement()` e `registerMovement()` | A interface oferece somente formulário genérico por lote, sem ordem, contêiner, múltiplos lotes, etapas, progresso ou encerramento. | Criar tela operacional de stuff e unstuff baseada no estado persistido da operação. |

### BUS1000 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/CargaGeralControlador.java` | rotas de `/api/carga-geral` | O controlador expõe BL, itens, lotes, movimentações, avarias e referências, mas nenhuma ordem de trabalho. | Criar contratos e endpoints para criar, liberar, atribuir, iniciar, registrar evento, concluir e cancelar ordem de trabalho. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | serviços públicos existentes | Não existe agregado que mantenha prioridade, janela, local, recursos, itens e histórico de execução. | Criar novo agregado sugerido: `OrdemTrabalhoCarga`, com repositório e transições de estado transacionais. |

### BUS1010 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarMovimentacao()` | Planejamento e execução não são entidades distintas; qualquer chamada já altera o saldo do lote. | Criar novos métodos sugeridos: `criarPlanoStuffStrip()`, `liberarPlano()` e `registrarExecucaoPlano()`, mantendo versões, itens planejados e realizados. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | formulário de movimentação | Não há visualização de plano, versão, capacidade, sequência nem planejado versus realizado. | Adicionar quadro de planos com validações, liberação e acompanhamento da execução por item. |

### BUS1020 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `transferirEntreLotes()` chamado por `registrarMovimentacao()` | A consolidação ou desconsolidação opera a partir de um lote e de um lote relacionado, sem unidade de transporte de origem e destino nem operação recuperável. | Criar novo método sugerido: `executarTransload()`, bloqueando todos os lotes e unidades envolvidos e persistindo a composição antes e depois. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dto/CargaGeralDTOs.java` | `RegistrarMovimentacaoRequest` | O contrato genérico não representa contêiner de origem e destino, lacres, conferência, divergências ou múltiplos lotes. | Criar DTO específico de transload com itens, unidades, locais, quantidades planejadas e realizadas e resultado da conferência. |

### BUS1030 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | fluxo de atendimento do Gate | O fluxo do Gate não possui contrato canônico com reserva e consumo de quantidade de cargo lot do módulo de carga geral. | Criar porta de integração para reservar, confirmar ou liberar quantidade por BL, delivery order e lote conforme a transição física do veículo. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/CargaGeralControlador.java` | rotas de lotes e movimentações | Não existem comandos de reserva para Gate nem identificação de appointment, truck visit ou transação. | Criar endpoints internos idempotentes de reserva, confirmação e compensação vinculados ao identificador da transação de Gate. |

### BUS1040 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/LoteCarga.java` | `armazemId` e `posicaoArmazenagem` | O lote mantém apenas localização corrente, sem allocation, capacidade reservada, posição planejada, instrução ou histórico de ocupação. | Criar entidades de allocation e instrução de trabalho para cargo lot, com origem, destino, recurso, prioridade, estado e quantidades. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/BayPlanServico.java` | planejamento do Yard | O planejamento existente está orientado às unidades conteinerizadas e não recebe plano operacional de cargo lot. | Criar adaptador local para validar posição, reservar capacidade e confirmar movimentos de carga geral no pátio ou armazém. |

### BUS1050 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio/estiva/controlador/PlanoEstivaControlador.java` | contratos de plano de estiva | O plano de estiva trata unidades e slots, sem itens de cargo lot, porão ou área de carga geral e quantidades operadas. | Criar contrato de plano de carga geral por visita, BL, lote, porão ou área, sequência e equipamento. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `visitaNavioId` e `registrarMovimentacao()` | O vínculo com navio é apenas um identificador informado na movimentação; não há plano, confirmação de bordo, reconciliação ou compensação. | Implementar comandos idempotentes para planejar, carregar, descarregar, reconciliar e cancelar quantidades por visita. |

### BUS1060 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | lista de trabalho ferroviária | A lista de trabalho existente opera contêineres e ordens ferroviárias, sem itens de cargo lot e quantidades por vagão. | Estender por contrato específico de carga geral com lote, vagão, posição, capacidade, sequência e quantidade planejada e realizada. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/LoteCarga.java` | vínculos de transporte | O lote não mantém visita ferroviária, vagão, posição no vagão nem histórico de custódia ferroviária. | Adicionar associação persistida à operação ferroviária e atualizar custódia e saldo somente após confirmação da execução. |

### BUS1070 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarAvaria()` | O método sobrescreve um único código e descrição no lote e muda todo o lote para avariado, sem quantidade afetada, evidências, inspeção ou resolução. | Criar agregado de avaria com múltiplos registros, quantidade afetada, anexos, responsável, estado, bloqueio, reparo e encerramento. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `registerDamage()` | A interface envia apenas código e descrição e não exibe histórico ou tratamento da avaria. | Criar inspector de avarias com evidências, segregação de saldo, inspeção, reparo, baixa e trilha de decisões. |

### BUS1080 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `AJUSTE_INVENTARIO` em `registrarMovimentacao()` | O ajuste altera diretamente o saldo informado sem sessão de contagem, posição física, apuração de divergência ou aprovação. | Criar novos métodos sugeridos: `abrirInventarioFisico()`, `registrarContagem()` e `confirmarDivergencia()`, com bloqueio e histórico auditável. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | cadastro e seleção de lote | A identificação é manual e não existe leitura de código de barras ou QR, contagem por posição nem conciliação. | Criar fluxo de identificação e inventário móvel ou web, preservando contagens individuais e exigindo confirmação motivada do ajuste. |
