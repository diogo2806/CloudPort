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
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/CargaGeralTipos.java` | `TipoMovimentacaoCarga` | O domínio possui recebimento, carga ou descarga parcial, consolidação e desconsolidação, mas não representa ordem, ciclo de vida ou etapas próprias de estufagem e desova. | Criar contratos específicos para tipo e status da operação de stuff e unstuff. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarMovimentacao(UUID, RegistrarMovimentacaoRequest)` | O método altera o saldo de um único lote e não vincula contêiner, conjunto de lotes, planejamento, conferência, lacre, execução parcial, divergência ou fechamento atômico. | Implementar novo método sugerido: `criarOperacaoStuffUnstuff()`, além dos comandos transacionais de execução e encerramento. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `blankMovement()` e `registerMovement()` | A interface oferece somente formulário genérico por lote. | Criar tela operacional de stuff e unstuff baseada no estado persistido da operação. |

### BUS1000 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/CargaGeralControlador.java` | rotas de `/api/carga-geral` | Não existe contrato de ordem de trabalho. | Criar endpoints para criar, liberar, atribuir, iniciar, registrar evento, concluir e cancelar ordem de trabalho. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | serviços públicos existentes | Não existe agregado com prioridade, janela, local, recursos, itens e histórico de execução. | Criar novo agregado sugerido: `OrdemTrabalhoCarga`. |

### BUS1010 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarMovimentacao()` | Planejamento e execução não são entidades distintas; a chamada altera o saldo imediatamente. | Criar novos métodos sugeridos: `criarPlanoStuffStrip()`, `liberarPlano()` e `registrarExecucaoPlano()`. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | formulário de movimentação | Não há plano, versão, capacidade, sequência nem planejado versus realizado. | Adicionar quadro de planos e execução por item. |

### BUS1020 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `transferirEntreLotes()` | A consolidação ou desconsolidação não registra unidade de transporte de origem e destino nem operação recuperável. | Criar novo método sugerido: `executarTransload()`, bloqueando os lotes e unidades envolvidos. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dto/CargaGeralDTOs.java` | `RegistrarMovimentacaoRequest` | O contrato não representa unidades, lacres, conferência, divergências ou múltiplos lotes. | Criar DTO específico de transload. |

### BUS1030 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `registrarEntrada()` e `registrarSaida()` | O fluxo valida agendamento e TOS, mas não reserva nem consome quantidade de cargo lot. | Criar porta local para reservar, confirmar e compensar quantidade por BL, delivery order e lote. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/CargaGeralControlador.java` | rotas de lotes e movimentações | Não existem comandos idempotentes vinculados à transação de Gate. | Criar comandos internos de reserva, confirmação e compensação. |

### BUS1040 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/LoteCarga.java` | `armazemId` e `posicaoArmazenagem` | O lote mantém somente localização corrente. | Criar allocation e instrução de trabalho com origem, destino, recurso, prioridade, estado e quantidades. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/BayPlanServico.java` | planejamento do Yard | O planejamento é orientado às unidades conteinerizadas. | Criar adaptador para validar posição, reservar capacidade e confirmar movimentos de cargo lot. |

### BUS1050 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio/estiva/controlador/PlanoEstivaControlador.java` | contratos de plano de estiva | O plano trata unidades e slots, sem itens de cargo lot e quantidades operadas. | Criar contrato de plano de carga geral por visita, BL, lote, porão ou área. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `visitaNavioId` e `registrarMovimentacao()` | O vínculo com navio é apenas identificador informado na movimentação. | Implementar comandos idempotentes de planejamento, carga, descarga, reconciliação e cancelamento. |

### BUS1060 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | lista de trabalho ferroviária | A lista opera contêineres e ordens ferroviárias, sem cargo lots e quantidades por vagão. | Estender por contrato específico de carga geral. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/LoteCarga.java` | vínculos de transporte | O lote não mantém visita ferroviária, vagão, posição nem histórico de custódia. | Adicionar associação persistida à operação ferroviária. |

### BUS1070 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarAvaria()` | O método sobrescreve um único código e descrição e muda todo o lote para avariado. | Criar agregado de avaria com quantidade afetada, anexos, responsável, estado, bloqueio, reparo e encerramento. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `registerDamage()` | A interface envia somente código e descrição. | Criar inspector de avarias com evidências e segregação de saldo. |

### BUS1080 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `AJUSTE_INVENTARIO` em `registrarMovimentacao()` | O ajuste altera diretamente o saldo sem sessão de contagem e aprovação. | Criar novos métodos sugeridos: `abrirInventarioFisico()`, `registrarContagem()` e `confirmarDivergencia()`. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | cadastro e seleção de lote | A identificação é manual e não há leitura de código de barras ou QR. | Criar fluxo de identificação e inventário por posição. |

## 2. Gate, pátio e controle de equipamentos

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1100 | Implementar chamada operacional de caminhões com fila persistida e confirmação de atendimento. | O sistema mantém posição, prioridade, gate ou pista de destino, instante da chamada, aceite, expiração, rechamada e cancelamento, sem derivar a fila apenas do status do agendamento. | ⬜ Pendente |
| BUS1110 | Impedir truck hopping por visita ativa e vínculo consistente entre cavalo, motorista, chassis e unidades. | Uma nova entrada conflitante é bloqueada ou tratada por fluxo autorizado, com motivo e histórico; a saída libera os vínculos na mesma transação. | ⬜ Pendente |
| BUS1120 | Implementar tratamento operacional de unidades fora de posição no pátio. | A divergência entre posição lógica e física gera ocorrência, bloqueia movimentos incompatíveis, permite localizar a unidade e cria instrução corretiva com confirmação de origem e destino. | ⬜ Pendente |
| BUS1130 | Implementar fluxo de Lost & Found e unidades TBD com reconciliação de inventário. | Unidades encontradas sem registro ou registradas sem localização entram em caso persistido, recebem investigação, associação ou baixa motivada e encerramento auditável. | ⬜ Pendente |
| INT1140 | Integrar VMT ou dispositivo operacional à confirmação de work instructions sem aceitar mensagens duplicadas ou fora de sequência. | A instrução despachada possui identificador de correlação; aceite, início e conclusão do equipamento atualizam o movimento uma única vez e rejeitam confirmação incompatível com o estado atual. | ⬜ Pendente |

### BUS1100 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `confirmarChegadaAntecipada()`, `registrarEntrada()` e estados de `GatePass` | O fluxo muda estados de agendamento e gate pass, mas não existe entidade de chamada, posição da fila, aceite ou expiração. | Criar novo agregado sugerido: `ChamadaCaminhao`, com comandos `chamar()`, `aceitar()`, `rechamar()`, `expirar()` e `cancelar()`. |
| `frontend/cloudport/src/pages/gate/GateVisualOperationsPage.jsx` | quadro de filas e pistas | A visualização não possui contrato persistido de chamada e confirmação do motorista ou operador. | Consumir a fila persistida e exibir histórico e estado da chamada. |

### BUS1110 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | `obterOuCriarGatePass()` e `registrarEntrada()` | A validação se concentra no agendamento e não comprova exclusividade operacional conjunta de motorista, caminhão, chassis e unidades em visita ativa. | Criar validação transacional de vínculos ativos e retorno funcional de conflito. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/model/GatePass.java` | vínculos da passagem | Não existe contrato explícito de ocupação dos recursos da visita com liberação atômica na saída ou cancelamento. | Persistir vínculos operacionais ativos e liberá-los na mesma transação do encerramento. |

### BUS1120 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/AlertasPatioServico.java` | geração de alertas do pátio | O alerta informa condição operacional, mas não cria caso de divergência com bloqueio, investigação e instrução corretiva. | Criar novo agregado sugerido: `DivergenciaPosicaoPatio`, vinculado à unidade e às posições lógica e física. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/ValidadorYardPlacementService.java` | validação de posicionamento | A validação impede posição inválida, mas não resolve unidade já encontrada fora da posição registrada. | Adicionar comando de reconciliação que gere e conclua movimento corretivo auditável. |

### BUS1130 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/inventario/controlador/InventarioCanonicoControlador.java` | inventário físico e divergências | O inventário registra unidades conhecidas, mas não existe caso operacional específico para unidade encontrada sem registro ou unidade lógica não localizada. | Criar endpoints de abertura, associação, investigação, regularização e baixa de Lost & Found/TBD. |
| `frontend/cloudport/src/pages/InventoryReportsPages.jsx` | relatórios de inventário | A interface não oferece fila de investigação e encerramento dessas ocorrências. | Criar tela operacional com estado, responsável, evidências e decisão final. |

### INT1140 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio` | telemetria, CHEs e work instructions | Existem telemetria e instruções, mas não há contrato comprovado que use confirmação real de VMT para transicionar a instrução com deduplicação e validação de sequência. | Criar porta de entrada de eventos VMT com `eventId`, `instructionId`, estado esperado, timestamp e resultado. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/controlroom` | despacho e conclusão operacional | A conclusão pode ser comandada pelo sistema sem confirmação idempotente do equipamento físico. | Integrar aceite, início, falha e conclusão do VMT ao ciclo de vida persistido da instrução. |

## 3. Navio e ferrovia

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1090 | Persistir o replanejamento visual de contêineres entre vagões. | O drag-and-drop confirmado atualiza vagão, posição, ordem e manifesto na mesma transação, valida capacidade e compatibilidade, usa versão ou lock e mantém auditoria de origem, destino, usuário e motivo. | ⬜ Pendente |
| BUS1150 | Registrar execução planejada versus realizada da sequência de guindastes do navio. | Cada movimento da sequência possui guindaste, janela, ordem, início, conclusão, exceção e quantidade realizada; mudanças concorrentes não sobrescrevem a execução e o plano é reconciliado com o realizado. | ⬜ Pendente |
| BUS1160 | Implementar operação persistida de tampas de porão vinculada à sequência de carga e descarga. | Abrir, remover, posicionar e fechar tampas gera tarefas com dependências, recursos, estado e confirmação; movimentos de contêiner bloqueados pela tampa não podem iniciar. | ⬜ Pendente |
| BUS1170 | Implementar movimentos ferroviários internos com ocupação transacional de linhas, switches e trechos. | Um movimento de composição ou locomotiva reserva rota e recursos, detecta conflito, registra autorização, execução, liberação e cancelamento, sem depender somente da posição final da visita. | ⬜ Pendente |
| DATA1180 | Reconciliar BAPLIE, plano aprovado, inventário, execução e posição física do navio. | Divergências de unidade, slot, peso, porto, perigosos ou reefer são persistidas, bloqueiam publicação ou conclusão quando críticas e possuem resolução auditável sem substituir silenciosamente uma fonte pela outra. | ⬜ Pendente |

### BUS1090 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/rail` | composição gráfica e drag-and-drop | O replanejamento visual é simulado no frontend e não permanece ao recarregar. | Chamar endpoint persistente com versão da composição, origem, destino e motivo. |
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/controlador/VisitaTremControlador.java` | rotas da visita e composição | Não existe endpoint específico para replanejar contêiner entre vagões. | Criar novo método sugerido: `replanejarConteinerEntreVagoes()`. |
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/servico/OrdemMovimentacaoServico.java` | ordens ferroviárias | Não existe atualização atômica comprovada entre composição, ordem e manifesto para o replanejamento visual. | Bloquear os agregados envolvidos, validar capacidade e persistir todas as alterações na mesma transação. |

### BUS1150 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio` | sequência de guindastes | O sistema mantém planejamento, mas não há agregado de execução por movimento com planejado versus realizado. | Criar novo agregado sugerido: `ExecucaoSequenciaGuindaste`, com transições de início, conclusão, falha e replanejamento. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | visualização do plano | A interface trabalha com o plano e overlays, sem linha do tempo persistida de execução real por guindaste. | Exibir progresso, exceções e reconciliação usando dados persistidos. |

### BUS1160 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/SlotNavio.java` | geometria e disponibilidade do slot | A geometria representa slots, mas não há ciclo de vida operacional de tampas de porão e suas dependências. | Criar entidades de tampa, posição, tarefa e dependência operacional. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | overlay de tampas de porão | A representação visual não comprova bloqueio transacional dos movimentos enquanto a tampa estiver fechada ou em operação. | Integrar o overlay ao estado persistido e impedir início incompatível. |

### BUS1170 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/modelo/VisitaTrem.java` | posição e estado da visita | O agregado representa a visita, mas não uma autorização de movimento com rota e ocupação temporal de trechos e switches. | Criar novo agregado sugerido: `MovimentoFerroviarioInterno`. |
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/servico/VisitaTremServico.java` | atualização da visita | A alteração de posição não reserva e libera todos os recursos ferroviários em uma operação transacional. | Criar comandos de planejar, autorizar, iniciar, concluir e cancelar movimento com detecção de conflito. |

### DATA1180 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/parser/BaplieParser.java` | importação BAPLIE | A importação produz dados de plano, mas não existe reconciliação persistida com inventário e execução física. | Criar novo serviço sugerido: `ReconciliacaoBaplieExecucaoServico.reconciliar()`. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/SlotNavio.java` | ocupação planejada do slot | A ocupação planejada não mantém caso de divergência por fonte e decisão de resolução. | Persistir divergências, severidade, origem dos valores, decisão e usuário responsável. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | plano visual | O workspace não apresenta fila consolidada de divergências entre BAPLIE, plano, inventário e execução. | Exibir divergências e bloquear publicação ou conclusão quando a regra de severidade exigir. |
