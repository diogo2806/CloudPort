# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Operações intermodais de carga geral

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS10 | Completar o fluxo de stuff e unstuff com contêiner canônico e apontamentos idempotentes. | A operação valida e bloqueia o contêiner no inventário, rejeita repetição do mesmo apontamento e mantém saldo, movimentação, eventos e compensação consistentes. | 🟡 Em andamento |
| BUS1020 | Implementar transload entre unidades com rastreabilidade e atualização atômica. | Origem, destino, lotes, quantidades, lacres, divergências e avarias são persistidos sem saldo negativo ou atualização parcial. | ⬜ Pendente |
| BUS1030 | Integrar Gate à carga geral para retirada e entrega parcial. | O Gate reserva quantidade por BL, delivery order e cargo lot e só confirma o estoque após o estágio físico correspondente. | ⬜ Pendente |
| BUS1040 | Implementar planejamento de pátio e armazém para cargo lots. | Allocation, capacidade, restrições, origem, destino, recurso e saldo por posição são persistidos e confirmados pela execução. | ⬜ Pendente |
| BUS1060 | Implementar plano de carga e descarga de cargo lots por visita ferroviária. | Lotes são planejados e executados por vagão e posição, com capacidade, incompatibilidades, sequência e custódia persistida. | ⬜ Pendente |
| BUS1070 | Implementar ciclo completo de avaria da carga. | Avarias possuem quantidade afetada, evidências, responsável, bloqueio, inspeção, reparo ou baixa e saldo segregado. | ⬜ Pendente |
| BUS1080 | Implementar identificação e inventário físico reconciliável de cargo lots. | Código de barras ou QR identifica lote e embalagem; contagens e divergências geram ajuste motivado sem sobrescrever diretamente o saldo lógico. | ⬜ Pendente |
| BUS1280 | Registrar tally operacional de stuff e unstuff por embalagem. | Cada leitura identifica operação, item, embalagem, quantidade, operador e instante; duplicidades são rejeitadas e divergências ficam pendentes de decisão antes da conciliação. | ⬜ Pendente |

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

### BUS1030 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `registrarEntrada()` e `registrarSaida()` | A reserva de carga geral é confirmada durante o fluxo, mas não há compensação coordenada comprovada quando uma persistência ou integração posterior falha. | Implementar reserva, confirmação e compensação idempotentes com estado durável por transação de Gate. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/OperacaoIntermodalCargaControlador.java` | rotas de reserva do Gate | Existem confirmação e liberação, mas falta contrato explícito de compensação após falha parcial do fluxo físico. | Criar novo método sugerido: `compensarReservaGate()` vinculado ao identificador da transação. |

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
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarAvaria()` | Sobrescreve um único código e descrição e muda todo o lote para avariado. | Integrar o fluxo legado ao agregado operacional de avaria ou removê-lo como fonte concorrente. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `registerDamage()` | Envia somente código e descrição pelo fluxo legado. | Consumir o contrato operacional com quantidade afetada, inspeção, evidências e encerramento. |

### BUS1080 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `AJUSTE_INVENTARIO` | O ajuste direto permanece disponível paralelamente ao inventário operacional conciliado. | Desativar o ajuste direto como fonte concorrente e exigir sessão de contagem e aprovação. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | seleção de lote | A identificação é manual. | Criar leitura por código de barras ou QR e inventário por posição. |

### BUS1280 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/StuffUnstuffServico.java` | `registrarExecucao()` | O apontamento agrega quantidade no item, sem identidade persistida para cada embalagem ou leitura operacional. | Criar novo método sugerido: `registrarTallyEmbalagem()` com identificador idempotente, operador, instante e resultado da conferência. |
| `frontend/cloudport/src/pages/StuffUnstuffPage.jsx` | execução | A interface informa quantidade consolidada e não mantém fila de leituras, rejeições e divergências. | Registrar leituras identificáveis e bloquear a conclusão enquanto houver divergência sem decisão. |

## 2. Gate, pátio e controle de equipamentos

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1110 | Impedir truck hopping por visita ativa. | Motorista, cavalo, chassis e unidades não participam simultaneamente de visitas conflitantes e são liberados atomicamente. | ⬜ Pendente |
| BUS1120 | Implementar tratamento de unidades fora de posição. | A divergência gera caso, bloqueio, investigação e instrução corretiva com origem e destino confirmados. | ⬜ Pendente |
| BUS1130 | Implementar Lost & Found e unidades TBD. | Unidade sem registro ou sem localização entra em caso persistido com investigação, associação, baixa e encerramento. | ⬜ Pendente |
| INT1140 | Integrar VMT à confirmação de work instructions. | Aceite, início e conclusão atualizam a instrução uma única vez e rejeitam evento duplicado ou fora de sequência. | ⬜ Pendente |
| BUS1190 | Implementar ciclo persistido de trouble transaction no Gate. | Uma transação rejeitada cria caso com motivo, estágio, responsável, ações, liberação ou cancelamento e retoma o fluxo somente após decisão válida. | ⬜ Pendente |
| BUS1200 | Implementar transferência operacional entre facilities. | A transferência mantém origem, destino, visita, unidade, EDO ou ERO, custódia, estágios físicos e compensação sem criar duas presenças ativas. | ⬜ Pendente |
| BUS1210 | Vincular inspeção de Gate e EIR à liberação da visita. | Checklist, imagens, avarias, lacres e decisão são versionados e a saída é bloqueada quando existir inspeção obrigatória pendente ou reprovada. | ⬜ Pendente |
| BUS1220 | Persistir reordenação e conexão das work queues aos pontos de trabalho. | Alterações de sequência, inclusão, remoção e vínculo ao ponto de trabalho são versionadas, rejeitam conflito concorrente e alcançam as work instructions ainda não iniciadas. | ⬜ Pendente |
| BUS1230 | Aplicar restrições e notas de stack como regra de execução. | A instrução de pátio valida restrições vigentes da pilha, registra override autorizado e não conclui movimento incompatível silenciosamente. | ⬜ Pendente |

### BUS1110 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `registrarEntrada()` | A ocupação ocorre antes da autorização, mas o fluxo não comprova bloqueio de unicidade no banco para todos os recursos concorrentes. | Garantir restrição transacional para motorista, cavalo, chassis e unidades e devolver conflito funcional. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateResourceOccupationService.java` | ocupação e liberação | Centraliza a ocupação, mas a exclusividade precisa permanecer válida sob requisições simultâneas e retries. | Persistir chaves únicas ativas e liberar vínculos atomicamente no encerramento ou cancelamento. |

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

### BUS1190 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | blocos `catch` de `registrarEntrada()` e `registrarSaida()` | A falha registra evento `RETIDO` e relança a exceção, sem agregado de caso, responsável, decisão e retomada controlada. | Criar novo agregado sugerido: `TroubleTransactionGate` e comandos de assumir, corrigir, liberar e cancelar. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowController.java` | fluxo operacional | Não há contrato específico para consultar e resolver transações problemáticas. | Expor endpoints com transição de estado validada e vínculo ao `GatePass`. |

### BUS1200 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `registrarEntrada()` e `registrarSaida()` | O fluxo encerra uma visita local, mas não representa transferência com duas facilities e custódia em trânsito. | Criar novo agregado sugerido: `TransferenciaEntreFacilities` com reserva da unidade e estágios de saída, trânsito e recebimento. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/model/GatePass.java` | facility e estado | O passe não comprova correlação persistida entre os dois Gates participantes. | Persistir identificador da transferência, origem, destino e confirmação idempotente de cada estágio. |

### BUS1210 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateOperadorOperacoesService.java` | inspeção e liberação | As operações visuais e a decisão do fluxo não comprovam inspeção versionada obrigatória antes da liberação. | Criar contrato persistido de checklist, imagens, lacres, avarias e decisão por visita e unidade. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `registrarSaida()` | Finaliza o Gate sem validação explícita de todas as inspeções obrigatórias vinculadas. | Bloquear a saída quando houver inspeção pendente ou reprovada e registrar o EIR final imutável. |

### BUS1220 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueuePatioServico.java` | ordenação e manutenção da fila | Existe fila persistida, mas não há comprovação de versionamento otimista da sequência e propagação atômica para instruções pendentes. | Implementar comando de reordenação com versão, validação de duplicidade e atualização transacional. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/modelo/WorkQueuePatio.java` | vínculo operacional | O modelo não comprova associação temporal persistida ao ponto de trabalho e histórico de mudanças. | Persistir ponto de trabalho, vigência e eventos de inclusão, remoção e resequenciamento. |

### BUS1230 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/ValidadorYardPlacementService.java` | validação de destino | Valida posicionamento, mas não comprova consumo de restrições e notas vigentes da pilha como condição para concluir a instrução. | Consultar restrições ativas, rejeitar incompatibilidade e exigir override com permissão, motivo e validade. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/modelo/OrdemTrabalhoPatio.java` | conclusão | A ordem não registra a versão das restrições considerada nem eventual override. | Persistir snapshot da regra aplicada e decisão autorizada na conclusão. |

## 3. Navio e ferrovia

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1150 | Registrar execução da sequência de guindastes. | Cada movimento possui guindaste, ordem, janela, início, conclusão, exceção e quantidade realizada. | ⬜ Pendente |
| BUS1240 | Controlar ocupação e manobras ferroviárias por trecho e janela. | Vagões, locomotivas e equipamentos não ocupam trechos conflitantes; cada manobra possui origem, destino, sequência, autorização, execução e cancelamento persistidos. | ⬜ Pendente |
| BUS1250 | Implementar prestage ferroviário conciliado com a lista de trabalho. | O planejamento posiciona unidades por linha, vagão e sequência, reserva capacidade e é reconciliado com cada movimento realizado ou não executado. | ⬜ Pendente |
| BUS1260 | Controlar estado operacional de tampas de porão. | Abertura, fechamento, remoção, reinstalação, bloqueio e confirmação são persistidos e impedem movimento incompatível no porão. | ⬜ Pendente |
| BUS1270 | Conciliar restows planejados com execução física. | Cada restow é identificado, vinculado aos movimentos de descarga e reembarque e encerrado somente quando a unidade retorna ao slot válido ou recebe decisão de exceção. | ⬜ Pendente |

### BUS1150 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio` | sequência de guindastes | Existe planejamento, mas não agregado de execução por movimento. | Criar novo agregado sugerido: `ExecucaoSequenciaGuindaste`. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | plano visual | Não apresenta linha do tempo persistida de execução real. | Exibir progresso, exceções e reconciliação. |

### BUS1240 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail` | visitas, composição e ocupação | Há representação da visita e das linhas, mas não foi comprovado comando transacional de manobra com bloqueio de trecho e janela. | Criar novo agregado sugerido: `ManobraFerroviaria` e reserva exclusiva de trechos. |
| `frontend/cloudport/src/pages` | composição ferroviária | A visualização não comprova fila executável de manobras e conflitos operacionais. | Exibir sequência autorizada, recurso, trecho, estado e motivo de bloqueio. |

### BUS1250 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | lista de trabalho | A lista permite operação por visita, mas não comprova prestage versionado por linha e posição antes da execução. | Criar endpoints de planejar, liberar, replanejar e cancelar prestage. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueuePatioServico.java` | integração com ferrovia | Não há correlação comprovada entre reserva de posição ferroviária e work instruction do pátio. | Persistir correlação e conciliar resultado do movimento com a lista ferroviária. |

### BUS1260 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio/estiva` | tampas de porão | A geometria do navio representa porões e tampas, mas não há ciclo operacional persistido que condicione movimentos. | Criar novo agregado sugerido: `OperacaoTampaPorao` com estados e dependência dos movimentos. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | overlay de tampas | A visualização não comprova confirmação operacional, responsável e bloqueios. | Exibir comandos, estado real, divergência e movimentos impedidos. |

### BUS1270 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio/estiva/controlador/PlanoEstivaControlador.java` | restow | O planejamento representa restow, mas não comprova correlação durável entre retirada, posição temporária e reembarque. | Criar identificador de restow e máquina de estados para planejar, descarregar, aguardar, reembarcar, divergir e cancelar. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | execução do restow | A interface apresenta planejamento sem reconciliação física completa. | Exibir as duas pernas do movimento, posição temporária, exceções e pendências de retorno. |
