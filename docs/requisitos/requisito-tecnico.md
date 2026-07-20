# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-20 após a conclusão do BUS1320 no PR #605.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Carga geral, stuff e unstuff

Nenhuma pendência técnica permanece nesta seção. O BUS1380 foi concluído no PR #598 e o BUS1390 no PR #599; ambos estão registrados no documento canônico de requisitos implementados.

## 2. Gate e pátio

Nenhuma pendência técnica permanece nesta seção. O BUS1320 foi concluído no PR #605 e está registrado no documento canônico de requisitos implementados.

## 3. Ferrovia

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1330 | Implementar plano de manobra ferroviária com sequência e ocupação de linhas. | Movimentos de locomotiva e vagões possuem origem, destino, composição, sequência, linha, conflito, autorização, início e conclusão; duas manobras não reservam o mesmo trecho simultaneamente. | ⬜ Pendente |
| BUS1340 | Implementar inspeção de vagões e bloqueio por defeito antes da carga ou descarga. | Checklist, defeitos, evidências, severidade, responsável e liberação são persistidos; vagão reprovado não entra na lista de trabalho sem override autorizado. | ⬜ Pendente |

### BUS1330 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | composição e lista de trabalho | O fluxo controla visita, vagões e trabalho, mas não representa plano de manobra com trechos reservados, sequência e conflito de linha. | Criar novo serviço sugerido: `PlanoManobraFerroviariaServico`, com reserva transacional de linhas e execução por etapa. |
| `frontend/cloudport/src/pages/RailLineUpPage.jsx` | line-up ferroviário | A composição visual não permite planejar e confirmar manobras de pátio ferroviário. | Adicionar quadro de manobras, ocupação, conflitos e progresso. |

### BUS1340 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | elegibilidade do vagão | A inclusão do vagão na operação não depende de inspeção física persistida com defeitos e decisão. | Criar novo agregado sugerido: `InspecaoVagao`, consultado antes de liberar carga, descarga ou partida. |
| `frontend/cloudport/src/pages/RailLineUpPage.jsx` | inspector do vagão | Não há checklist, registro de defeito, evidência e liberação operacional. | Criar inspector com estados pendente, aprovado, reprovado e liberado por override. |

## 4. Navio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1350 | Implementar prontidão de berço e autorização operacional antes do início da escala. | Berço, calado, defensas, amarração, acesso, recursos, restrições e liberações são confirmados; operação de carga não inicia com item crítico pendente. | ⬜ Pendente |
| BUS1360 | Registrar paralisações, trocas de turno e handover por guindaste. | Início, fim, motivo, responsável, impacto, turno e pendências são persistidos e reconciliados com a execução real sem sobrescrever o plano. | ⬜ Pendente |

### BUS1350 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio/escala/servico/EscalaServico.java` | início da operação da escala | O cadastro e a evolução da escala não comprovam checklist bloqueante de prontidão do berço e liberações operacionais. | Criar novo método sugerido: `confirmarProntidaoBerco()` e validar itens críticos antes de autorizar início. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | operação de navio | Não há painel consolidado de prontidão com bloqueios e responsáveis. | Criar checklist de berço versionado e visível antes do início da operação. |

### BUS1360 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/VesselPlannerServico.java` | execução por guindaste | A execução acompanha movimentos e progresso, mas não há entidade específica para paralisação, troca de turno e handover de pendências. | Criar novos métodos sugeridos: `registrarParalisacaoGuindaste()` e `registrarHandoverTurno()`. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | monitor operacional | O painel não registra handover estruturado nem separa indisponibilidade planejada de paralisação operacional. | Adicionar timeline de paralisações, turnos, responsáveis e pendências abertas. |

## 5. Planejamento preditivo de pátio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1400 | Implementar estados de planejamento tentativo, definitivo e iminente para posições e work instructions. | Toda proposta de posição registra horizonte, estado, validade e origem; a conversão entre tentativo e definitivo é auditada, e o dispatch só usa posição definitiva ou revalida a posição tentativa transacionalmente. | ⬜ Pendente |
| BUS1410 | Implementar Yard Impact com previsão de ocupação, movimentos e demanda de CHE por bloco e POW. | O sistema projeta ao menos seis horas de entradas, saídas, rehandles, reservas e work instructions, identifica saturação e falta de equipamento e permite drill-down até as unidades responsáveis. | ⬜ Pendente |

### BUS1400 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/scheduler/servico/PredictiveSchedulerService.java` | proposta de posição e horizonte operacional | O scheduler calcula propostas, mas não persiste estados operacionais equivalentes a tentativo, definitivo e iminente por work instruction. | Criar agregado sugerido `PlanoPosicaoOperacional`, com `TENTATIVO`, `DEFINITIVO`, `IMINENTE`, expiração, versão, motivo e revalidação no dispatch. |
| `frontend/cloudport/src/pages/yard/YardReceivingPlanPage.jsx` | visualização do planejamento | A tela apresenta planejamento e propostas sem uma linha do tempo operacional formal por estado. | Adicionar filtros, badges, conversão motivada e contagem regressiva para o horizonte iminente. |

### BUS1410 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/scheduler/servico/RealYardReplanningOptimizerService.java` | cálculo de impacto futuro | O otimizador avalia posições e custo da proposta, mas não publica uma projeção temporal consolidada por bloco, POW e equipamento. | Criar serviço sugerido `YardImpactServico`, agregando ocupação futura, reservas, filas, produtividade e demanda de CHE em janelas configuráveis. |
| `frontend/cloudport/src/pages/yard/OperationalYardViews.jsx` | painel de impacto | As vistas operacionais não exibem recap temporal de movimentos futuros, saturação e déficit de equipamento. | Criar visão Yard Impact com heatmap temporal, slider de tempo, comparação atual versus futuro, animação, recap por bloco/POW e drill-down das unidades e ordens. |

## 6. Dispatch e equipamentos

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1420 | Implementar scheduler de dispatch dinâmico por família de CHE. | Straddle carrier, terminal tractor, RTG/RMG, ASC, equipamento de cais e ferroviário usam perfis próprios de tempo, velocidade, capacidade, pool, POW, distância, atraso e restrições. | ⬜ Pendente |
| BUS1430 | Implementar dispatch automático e semiautomático com job steps persistidos. | A conclusão ou falha de um trabalho seleciona o próximo elegível; cada instrução registra etapas de deslocamento, chegada, coleta, transporte, entrega e confirmação física, impedindo avanço antecipado ou fora de sequência. | ⬜ Pendente |
| BUS1440 | Implementar autodespacho controlado pelo operador do equipamento. | O operador informa unidade ou trabalho, e o backend valida fila, pool, POW, fase da visita, equipamento, prioridade, holds e concorrência antes de atribuir a instrução. | ⬜ Pendente |
| BUS1450 | Implementar parâmetros operacionais versionados e alteráveis em runtime. | Pesos, penalidades, velocidades, tempos, tolerâncias, regras de pool, overrides e políticas de dispatch podem ser ativados por escopo, possuem validação, auditoria, vigência e rollback. | ⬜ Pendente |
| BUS1460 | Incorporar GPS, rotas e congestionamento à decisão de dispatch. | O score considera posição atual, distância de rota, sentido, bloqueios, interdições, zonas de transferência, limite regional de CHE e tempo estimado; telemetria atrasada impede decisão automática. | ⬜ Pendente |
| BUS1470 | Selecionar automaticamente chassis e equipamentos auxiliares durante o dispatch. | Chassis, bomb cart, cassette e acessórios são escolhidos por origem, destino, tipo de movimento, pool, armador e disponibilidade; movimentos auxiliares e associações físicas são persistidos. | ⬜ Pendente |

### BUS1420 e BUS1430 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueuePatioServico.java` | `dispatch` e ciclo da work instruction | O serviço despacha filas e mantém estados principais, mas não aplica schedulers especializados por família nem uma cadeia persistida de job steps. | Introduzir estratégia `DispatchScheduler` por tipo de CHE, score explicável e agregado `EtapaWorkInstruction` com transições idempotentes. |
| `frontend/cloudport/src/pages/yard/YardWorkPages.jsx` | filas, job list e comandos | A interface permite dispatch e comandos manuais, mas não mostra score, motivos, etapas, previsão de chegada ou seleção automática contínua. | Exibir ranking, memória de cálculo, job steps, modo automático/semiautomático e próximo trabalho previsto. |

### BUS1440 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/controlador/WorkQueueOperacaoControlador.java` | atribuição de trabalho | Os contratos operacionais não comprovam solicitação de autodespacho por unidade iniciada pelo operador do CHE. | Criar endpoint sugerido `POST /work-instructions/auto-dispatch`, exigindo operador, equipamento, unidade, fase, pool, POW e chave idempotente. |
| `frontend/cloudport/src/pages/ControlRoomEquipamentosPage.jsx` | operação do equipamento | O Control Room acompanha equipamento e instrução atual, mas não oferece solicitação de trabalho pelo operador com bloqueios explicados. | Adicionar fluxo de autodespacho com validação, rejeição funcional e auditoria. |

### BUS1450 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/dto/RegraAutomacaoDto.java` | regras de automação | Existem regras pontuais, mas não há catálogo versionado de parâmetros do scheduler com escopo, vigência e rollback. | Criar agregado `ConfiguracaoDispatch`, resolução hierárquica por terminal, pátio, bloco, POW, pool, fila e tipo de CHE. |
| `frontend/cloudport/src/pages/yard/YardWorkPages.jsx` | administração do dispatch | Os parâmetros usados pelo motor não são administrados em tela operacional própria. | Criar editor com comparação de versões, simulação, ativação, rollback e permissões administrativas. |

### BUS1460 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/controlroom/ControlRoomEquipamentoServico.java` | telemetria e posição de CHE | Latitude, longitude, coordenadas, heading e posição próxima são armazenados, mas não há comprovação de uso obrigatório no score transacional de cada dispatch. | Criar porta `RoteamentoEquipamentoServico` e integrar rota, congestionamento, bloqueios e frescor da telemetria ao scheduler. |
| `frontend/cloudport/src/pages/ControlRoomEquipamentosPage.jsx` | mapa e recomendação | A tela mostra estado operacional, mas não explica rota recomendada, ETA, congestionamento ou motivo da escolha do CHE. | Exibir rota, tempo estimado, conflitos e justificativa do dispatch. |

### BUS1470 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/inventario/modelo/UnidadeInventario.java` | vínculos de equipamentos auxiliares | O inventário representa unidades e vínculos, mas o dispatch não comprova seleção automática de chassis, bomb cart ou cassette. | Criar serviço `SelecaoEquipamentoAuxiliarServico`, reserva concorrente, associação, desassociação e geração de movimentos auxiliares. |
| `frontend/cloudport/src/pages/yard/YardWorkPages.jsx` | detalhe da work instruction | A instrução não apresenta equipamento auxiliar escolhido, alternativas rejeitadas ou etapas de coleta e devolução. | Exibir seleção, vínculo físico, estado e comandos de substituição motivada. |

## 7. Integração física e operação de campo

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1480 | Integrar work instructions com VMT, RDT, ECS e PLC homologados. | O dispositivo recebe, aceita, inicia, conclui, falha, cancela e aborta transport orders; store-and-forward, reconexão, deduplicação e reconciliação impedem perda ou dupla execução. | ⬜ Pendente |
| BUS1490 | Associar operador, turno, equipamento e dispositivo durante a operação. | Login, logout, troca de turno, equipamento ativo e dispositivo são persistidos; eventos sem sessão válida ou provenientes de equipamento divergente são bloqueados. | ⬜ Pendente |
| BUS1500 | Registrar avarias e exceções operacionais diretamente no terminal de campo. | O operador informa código, observação e evidência; o sistema bloqueia a instrução, avalia disponibilidade do CHE, replaneja a unidade e cria trabalho substituto sem perder rastreabilidade. | ⬜ Pendente |

### BUS1480 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/EventoVmtWorkInstructionServico.java` | eventos VMT | O backend aceita eventos idempotentes de aceite, início, falha e conclusão, mas não implementa protocolo homologado de entrega, cancelamento, aborto, store-and-forward e reconciliação com ECS/PLC. | Criar adaptadores por fornecedor, transport order persistido, outbox/inbox, confirmação de entrega e rotina de reconciliação do estado físico. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/controlroom/ControlRoomEquipamentoServico.java` | comandos remotos | Os comandos são genéricos e não representam o ciclo completo de uma ordem física executada por ECS ou PLC. | Acrescentar contratos de transport order, cancelamento, aborto emergencial, timeout, retentativa e confirmação do fornecedor. |

### BUS1490 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/controlroom/ControlRoomEquipamentoServico.java` | dispositivo e equipamento | Há vínculo entre dispositivo e CHE, porém não existe sessão operacional completa de operador e turno vinculada aos eventos. | Criar agregado `SessaoOperadorEquipamento`, com exclusão mútua, heartbeat, troca de turno e encerramento seguro. |
| `frontend/cloudport/src/pages/ControlRoomEquipamentosPage.jsx` | sessão de campo | A interface administrativa não exibe operador autenticado, turno e divergência entre sessão, CHE e dispositivo. | Adicionar painel de sessão, reassociação autorizada e histórico. |

### BUS1500 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueuePatioServico.java` | falha e bloqueio operacional | A instrução pode ser bloqueada ou falhar, mas não há contrato de campo que abra avaria, replaneje a unidade e gere substituição de forma atômica. | Criar método sugerido `registrarExcecaoCampo()` com evidência, severidade, impacto no CHE, compensação e replanejamento. |
| `frontend/cloudport/src/pages/ControlRoomEquipamentosPage.jsx` | exceção pelo operador | O fluxo não oferece registro operacional simplificado para RDT/VMT. | Criar formulário de exceção com códigos configuráveis, foto, observação, indisponibilidade e resultado do replanejamento. |

## 8. Integração Gate, Yard e reefer

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1510 | Automatizar seleção de unidade, exchange area e work queue a partir da transação de Gate. | Importação, exportação e vazios selecionam unidade elegível por BL, booking, delivery order, grupo, tipo e grade; a posição física, exchange area e ordem de pátio são reservadas e compensadas com a visita. | ⬜ Pendente |
| BUS1520 | Implementar operação reefer de campo com rounds, conexão e desconexão. | Técnicos recebem ordens por área, registram temperatura, tomada, conexão, desconexão e falha; atrasos e desvios geram alertas, e rehandles preservam a reconciliação elétrica e operacional. | ⬜ Pendente |

### BUS1510 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | integração da visita com o pátio | O Gate evolui estágios e transações, mas não comprova seleção automática completa da unidade e da exchange area com compensação da work instruction. | Criar orquestração `GateYardDispatchServico`, idempotente, com reserva, swap, cancelamento e compensação transacional. |
| `frontend/cloudport/src/pages/GateOperationsPage.jsx` | processamento da transação | A tela não apresenta unidade sugerida, alternativas, posição, exchange area e work instruction como uma única decisão operacional. | Adicionar inspector da seleção automática e override motivado. |

### BUS1520 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/ReeferTelemetriaPatioServico.java` | telemetria reefer | A telemetria registra dados do reefer, mas não representa rounds, ordens de conexão/desconexão, técnico responsável e confirmação física da tomada. | Criar agregados `RoundReefer` e `OrdemConexaoReefer`, com SLA, alarmes, histórico e idempotência. |
| `frontend/cloudport/src/pages/yard/YardReeferPanel.jsx` | painel reefer | O painel monitora unidades, mas não opera fila de técnicos, rounds e ordens físicas completas. | Adicionar modo de campo por área, leitura, conexão, desconexão, falha e reconciliação. |

## 9. Planejamento e execução de navio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1530 | Implementar editor versionado de geometria e templates de classe de navio. | Bays, rows, tiers, porões, tampas, células, limites e atributos são editáveis, validados, versionados e reutilizáveis por classe, sem alterar visitas já planejadas. | ⬜ Pendente |
| BUS1540 | Implementar operação móvel de Hatch Clerk, merge de unidade TBD e transbordo direto navio-navio. | O conferente valida unidade, slot, visita e fase; substituições TBD preservam regras de estiva, e transbordos dentro da janela operacional podem gerar transferência direta auditada. | ⬜ Pendente |
| BUS1550 | Implementar Advanced Berth Scheduler integrado a guindastes e recursos terrestres. | Berços, navios, janelas, calado, guindastes, produtividade, restrições e recursos são planejados em um horizonte comum; conflitos e impactos são detectados antes da confirmação. | ⬜ Pendente |

### BUS1530 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/GeometriaNavioServico.java` | geometria do navio | O serviço mantém geometria utilizada pelo planner, mas não comprova um editor completo de templates de classe com versionamento independente das visitas. | Criar agregado `TemplateClasseNavio`, cópia imutável por visita, validações geométricas e publicação de versão. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | edição visual | O workspace planeja carga sobre uma geometria existente, mas não funciona como editor de classe do navio. | Criar modo Ship Editor com validação, preview, comparação e publicação. |

### BUS1540 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/VesselPlannerServico.java` | confirmação de movimento e posição | O planner controla posições e execução, mas não possui contrato móvel específico de Hatch Clerk, merge TBD e transferência direta entre visitas. | Criar serviços `HatchClerkServico`, `MergeUnidadeTbdServico` e `TransbordoDiretoServico`, com bloqueios de fase, navio e slot. |
| `frontend/cloudport/src/pages/ContainerVesselPlannerCompletePage.jsx` | execução no cais | A tela principal não oferece fluxo móvel reduzido para conferência de unidade e tratamento de divergência no costado. | Criar interface responsiva de Hatch Clerk com leitura, confirmação, bloqueio e override autorizado. |

### BUS1550 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/recursos/entidade/BercoPortuario.java` | reserva do berço | O recurso representa o berço, mas não existe motor integrado de programação que combine navio, guindaste, produtividade e recursos de pátio. | Criar `BerthSchedulerServico`, reservas concorrentes, score, simulação e confirmação versionada. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | line-up e recursos | As telas não apresentam um Gantt integrado com conflitos de berço, guindaste, janela e impacto terrestre. | Criar scheduler gráfico com dependências, alertas e memória de cálculo. |

## 10. Ferrovia integrada ao dispatch

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1560 | Integrar work queues ferroviárias ao scheduler de CHE e ao impacto do pátio. | Carga, descarga e manobra ferroviária reservam equipamentos, posições e janelas; o dispatch considera linha, vagão, sequência, interferência no pátio e confirmação por dispositivo de campo. | ⬜ Pendente |

### BUS1560 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | lista de trabalho ferroviária | A lista ferroviária controla a operação, mas não participa do mesmo scheduler dinâmico de CHE do pátio. | Publicar demanda ferroviária por contrato compartilhado e reservar CHE, posição e janela antes do dispatch. |
