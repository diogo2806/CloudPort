# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-20 após a conclusão dos BUS1400 e BUS1410 no PR #607 e a auditoria de exposição dos modelos do backend no frontend.

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

Nenhuma pendência técnica permanece nesta seção. O BUS1400 e o BUS1410 foram concluídos no PR #607 e estão registrados no documento canônico de requisitos implementados.

## 6. Dispatch e equipamentos

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1420 | Implementar scheduler de dispatch dinâmico por família de CHE. | Straddle carrier, terminal tractor, RTG/RMG, ASC, equipamento de cais e ferroviário usam perfis próprios de tempo, velocidade, capacidade, pool, POW, distância, atraso e restrições. | ⬜ Pendente |
| BUS1430 | Implementar dispatch automático e semiautomático com job steps persistidos. | A conclusão ou falha de um trabalho seleciona o próximo elegível; cada instrução registra etapas de deslocamento, chegada, coleta, transporte, entrega e confirmação física, impedindo avanço antecipado ou fora de sequência. | ⬜ Pendente |
| BUS1440 | Implementar autodespacho controlado pelo operador do equipamento. | O operador informa unidade ou trabalho, e o backend valida fila, pool, POW, fase da visita, equipamento, prioridade, holds e concorrência antes de atribuir a instrução. | ⬜ Pendente |
| BUS1450 | Implementar parâmetros operacionais versionados e alteráveis em runtime. | Pesos, penalidades, velocidades, tempos, tolerâncias, regras de pool, overrides e políticas de dispatch podem ser ativados por escopo, possuem validação, auditoria, vigência e rollback. | ⬜ Pendente |
| BUS1460 | Incorporar GPS, rotas e congestionamento à decisão de dispatch. | O score considera posição atual, distância de rota, sentido, bloqueios, interdições, zonas de transferência, limite regional de CHE e tempo estimado; telemetria atrasada impede decisão automática. | ⬜ Pendente |
| BUS1470 | Selecionar automaticamente chassis e equipamentos auxiliares durante o dispatch. | Chassis, bomb cart, cassette e acessórios são escolhidos por origem, destino, tipo de movimento, pool, armador e disponibilidade; movimentos auxiliares e associações físicas são persistidos. | ⬜ Pendente |
| BUS1740 | Sincronizar navegação dinâmica, catálogo de rotas e componentes do portal. | Toda aba retornada por `/api/navegacao/abas` é validada contra um registro central de telas; menu, breadcrumb, autorização e resolução de componente usam a mesma definição, sem rota válida terminar em 404. | ⬜ Pendente |
| BUS1750 | Organizar contratos e modelos do frontend por domínio. | DTOs de entrada, resposta, enum, comando e resumo são tipados e gerados ou validados contra o OpenAPI; adapters normalizam paginação, datas, identificadores e campos legados, detectando quebra de contrato antes da execução. | ⬜ Pendente |
| BUS1760 | Expor a Visibilidade Operacional completa no portal. | Navios, detalhes, ocupação do pátio, throughput do Gate, busca de contêineres, rastreamento e histórico possuem telas, filtros, inspector, timeline e navegação contextual. | ⬜ Pendente |
| BUS1770 | Completar a configuração operacional do Gate. | Estágios, tarefas, regras de acesso, bookings, Bills of Lading, ordens e pré-avisos podem ser consultados e administrados conforme permissões, com sequência, validade, saldo, impacto e bloqueios visíveis. | ⬜ Pendente |
| BUS1780 | Completar as ações da operação do Gate. | O portal cria truck visit, anexa evidências, consulta e reimprime documentos, recebe transferências e registra override, trouble, inspeção e resolução por formulários estruturados e auditáveis. | ⬜ Pendente |
| BUS1790 | Completar os comandos do inventário canônico no inspector da unidade. | Propriedade, posição, documentos, avarias, manutenção, reefer, montagem, desmontagem, contagem e divergências podem ser operados na interface com motivo, permissão, estado e histórico. | ⬜ Pendente |
| BUS1800 | Expor operações intermodais de carga geral. | Transload, reservas de Gate, allocations, planos modais, identificações por código ou QR e inventário físico possuem telas com consulta, execução, compensação, cancelamento e rastreabilidade. | ⬜ Pendente |
| BUS1810 | Expor movimentos ferroviários internos. | Planejamento, consulta, autorização, início, conclusão e cancelamento de movimentos internos ficam disponíveis por visita, com recursos, conflitos, responsáveis e timeline de estados. | ⬜ Pendente |
| BUS1820 | Expor capacidade e reservas de cargo lot no Yard. | Capacidade total, reservada, ocupada e disponível por posição é configurável e consultável; reservas podem ser criadas, confirmadas e canceladas a partir do pátio e da carga geral. | ⬜ Pendente |
| BUS1830 | Criar operação controlada de contingência do Gate. | Quando a funcionalidade estiver habilitada no backend, o portal permite agendamento e liberação emergencial com indicação visual, justificativa, confirmação, operador e auditoria; quando desabilitada, nenhuma ação é oferecida. | ⬜ Pendente |

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

### Exposição e organização dos modelos já disponíveis no backend

As pendências BUS1740 a BUS1830 são predominantemente de frontend e devem consumir contratos já disponíveis, sem transformar entidades técnicas, outbox, idempotência ou históricos imutáveis em CRUD comum.

### BUS1740 e BUS1750 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/App.jsx` | `FALLBACK_NAVIGATION`, `normalizeBackendTabs()` e `RouteContent()` | O menu aceita rotas dinâmicas do backend, mas a resolução das páginas permanece em uma sequência fixa de condicionais; uma aba publicada pode abrir a tela 404. | Criar catálogo único de rotas com caminho, componente, aliases, papéis, breadcrumb e metadados de ajuda; validar as abas antes de exibi-las. |
| `frontend/cloudport/src/api.js` | clientes e normalização de respostas | Contratos de vários domínios ficam concentrados ou são consumidos com campos alternativos e inferência em tempo de execução. | Separar contratos e adapters por domínio, gerar ou validar tipos a partir do OpenAPI e adicionar verificação de compatibilidade dos contratos. |
| `frontend/cloudport/package.json` | toolchain JavaScript/JSX | O projeto não possui etapa de geração ou validação de modelos tipados. | Adicionar geração de contratos, checagem estática e comando de validação executável no build do frontend. |

### BUS1760 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/controller/VisibilidadeController.java` | navios, ocupação, throughput, busca, rastreamento e histórico | O backend expõe os modelos operacionais, mas o portal consome principalmente o resumo do dashboard e a central de alertas. | Criar clientes e rotas para contêineres, navios, ocupação e throughput, com filtros, inspector, timeline e links para os módulos relacionados. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | `HomeDashboard()` | A visão geral apresenta métricas agregadas sem drill-down completo para os modelos de visibilidade. | Vincular cada indicador ao detalhamento correspondente e preservar filtros e contexto na navegação. |

### BUS1770 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/MasterDataPages.jsx` | `GateInfrastructurePage()` | A configuração cobre instalação, Gate e pista, mas não administra o fluxo operacional e suas referências. | Organizar abas ou telas para estágios, tarefas, regras de acesso, bookings, Bills of Lading, ordens e pré-avisos. |
| `frontend/cloudport/src/gateOperationsApi.js` | `salvarStage()`, `salvarTask()`, `salvarRegraAcesso()`, `salvarBooking()`, `salvarBillOfLading()`, `vincularBillOfLading()`, `salvarOrder()` e `salvarPreadvice()` | Os comandos já existem no cliente, porém não são acionados pelas páginas do portal. | Criar formulários, listagens, vínculos, ordenação, ativação, validação de impacto e mensagens de bloqueio conforme o contrato. |

### BUS1780 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/gateOperationsApi.js` | `criarVisita()`, `anexar()`, `reimprimirDocumento()` e `receberTransferencia()` | Os contratos existem no cliente, mas não estão disponíveis no fluxo visual principal. | Expor criação de visita, anexos, histórico e reimpressão de documentos e recebimento da transferência. |
| `frontend/cloudport/src/pages/GateOperationsPage.jsx` | override, trouble, inspeção e resolução | As ações usam `window.prompt()`, sem campos estruturados, catálogo de opções ou resumo para confirmação. | Substituir prompts por modais acessíveis, validação de domínio, motivo obrigatório, confirmação e retorno auditável. |

### BUS1790 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/inventoryReportsApi.js` | comandos do inventário canônico | O cliente já altera propriedade, posição, documentos, avarias, manutenção, reefer, montagens, contagens e divergências. | Manter os comandos no domínio de inventário e padronizar payload, motivo, atualização otimista segura e tratamento de conflito. |
| `frontend/cloudport/src/pages/InventoryReportsPages.jsx` | `UnitInspector()` | O inspector permite somente parte das operações e apresenta os demais modelos principalmente como consulta. | Adicionar formulários e transições para todos os comandos existentes, exibindo permissões, estados, bloqueios e histórico após cada atualização. |

### BUS1800 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/OperacoesIntermodaisControlador.java` | transload, reservas, allocations, planos, avarias, identificações e inventário | O backend possui o ciclo operacional completo, mas esses contratos não estão organizados no portal React. | Criar clientes e telas de operação intermodal, com seleção de cargo lot, saldos, estados, comandos motivados e rastreabilidade. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/controlador/TransloadControlador.java` | execução e consulta de transload recuperável | Existe contrato específico de execução atômica e consulta, sem fluxo correspondente no frontend. | Integrar o transload à tela intermodal, exibindo origem, destino, quantidade, resultado, compensações e identificador da operação. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | operação de carga geral | A página cobre conhecimentos, itens, lotes, movimentações e referências, mas não centraliza os modelos intermodais. | Adicionar navegação contextual para transload, allocations, planos modais, identificações e inventário físico. |

### BUS1810 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/movimento/controlador/MovimentoFerroviarioInternoControlador.java` | ciclo do movimento interno | Planejar, consultar, listar por visita, autorizar, iniciar, concluir e cancelar já estão expostos pelo backend. | Criar cliente ferroviário para todos os comandos e normalizar permissões e transições. |
| `frontend/cloudport/src/railApi.js` | contratos ferroviários | O cliente cobre visitas, ordens, replanejamento de contêiner e partida, sem movimentos internos. | Adicionar os endpoints de movimento interno e integrá-los ao line-up e à lista de trabalho. |
| `frontend/cloudport/src/pages/RailLineUpPage.jsx` | planejamento e execução | A tela não apresenta o ciclo operacional dos movimentos internos. | Criar quadro por visita com origem, destino, composição, recursos, conflito, estado, responsáveis e ações permitidas. |

### BUS1820 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/inventario/controlador/CapacidadeCargoLotControlador.java` | capacidade, saldos e reservas | Configuração, consulta de saldos, reserva, confirmação e cancelamento já existem no backend, sem cliente dedicado no portal. | Criar API de frontend e integrar capacidade às posições, ao mapa, ao cargo lot, às allocations e ao inventário físico. |
| `frontend/cloudport/src/pages/yard/YardMapPages.jsx` | posição e ocupação | A posição não apresenta o saldo específico de carga geral nem comandos de reserva de cargo lot. | Exibir capacidade total, reservada, confirmada e disponível, restrições e operações permitidas. |

### BUS1830 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/administracao/ContingenciaController.java` | `/gate/contingencia/agendar` e `/gate/contingencia/liberar` | Os comandos são condicionais por propriedade de ambiente e não possuem cliente ou tela no portal. | Criar cliente e modo de contingência somente quando o recurso estiver disponível, com destaque visual, justificativa, confirmação e auditoria. |
| `frontend/cloudport/src/App.jsx` | navegação e disponibilidade | Não existe rota específica nem mecanismo visual para indicar que o Gate está operando em contingência. | Exibir a rota apenas para usuários autorizados e ambientes habilitados, sem oferecer fallback inseguro quando o backend responder indisponível. |

Todas as telas criadas ou ampliadas pelos BUS1740 a BUS1830 devem reutilizar `PageHeader`, a grade operacional, a central de alertas e o manual contextual com finalidade, fluxo operacional, explicação dos campos, permissões necessárias, estados possíveis, motivos de bloqueio, exemplos, atalhos e link para o processo completo.

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
| `frontend/cloudport/src/pages/RailLineUpPage.jsx` | execução ferroviária | A tela não mostra equipamentos atribuídos, ETA, job steps e impacto futuro nos blocos. | Adicionar painel integrado de filas, CHE, sequência e Yard Impact. |

## 11. Relatórios e inteligência operacional global

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1570 | Implementar relatórios operacionais especializados equivalentes ao XPS. | Yard Impact Recap, sequências de carga e descarga, hatch summary, bay notes, rail discharge, produtividade por CHE/POW e planejado versus realizado são gerados com filtros, layouts, PDF, agendamento e retenção autorizada. | ⬜ Pendente |
| BUS1580 | Implementar otimização global, previsão de gargalos e reconciliação física contínua. | Navio, pátio, Gate, ferrovia e equipamentos são avaliados no mesmo horizonte; gargalos são previstos e BAPLIE, plano, inventário, work instruction, VMT e GPS são reconciliados continuamente com divergências acionáveis. | ⬜ Pendente |

### BUS1570 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/InventoryReportsPages.jsx` | relatórios e exportações | Existem relatórios de inventário e exportações de grade, mas não a suíte operacional especializada com layouts e distribuição programada. | Criar infraestrutura comum de relatório, templates versionados, geração PDF, armazenamento, autorização, agenda e retenção. |
| `frontend/cloudport/src/pages/yard/YardWorkPages.jsx` | relatórios de filas | A página permite consulta operacional, mas não gera sequence sheet, recap por POW ou produtividade planejada versus realizada. | Adicionar relatórios específicos reutilizando a infraestrutura comum. |

### BUS1580 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/scheduler/servico/RealYardReplanningOptimizerService.java` | otimização | O motor otimiza o replanejamento Navio e Yard, mas não resolve simultaneamente Gate, Rail, berço e toda a frota de equipamentos. | Criar orquestrador `OtimizacaoOperacionalGlobalServico`, com simulação, restrições por domínio, proposta reproduzível e aplicação transacional por etapas compensáveis. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/AnaliseOperacionalAvancadaServico.java` | análise e previsão | Há análise operacional, mas não uma previsão unificada de gargalos nem reconciliação contínua entre todas as fontes físicas e planejadas. | Criar projeções por berço, porão, bloco, fila, pista, linha e CHE, além de reconciliador contínuo com severidade, responsável e ação recomendada. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | Control Room global | Os painéis mostram estados e alertas, mas não explicam gargalos previstos, simulações e divergências entre plano e realidade física em um único fluxo. | Criar cockpit global com horizonte temporal, causa, impacto, recomendação, simulação e confirmação motivada. |

## 12. Interface gráfica 2D operacional equivalente ao XPS

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1590 | Implementar workspace gráfico integrado entre Navio, Yard, Rail e equipamentos. | A mesma unidade pode ser selecionada e localizada em todas as vistas; origem, destino, work instruction e CHE são sincronizados, e movimentos entre domínios podem ser simulados por drag-and-drop antes da confirmação transacional. | ⬜ Pendente |
| BUS1600 | Implementar navegação hierárquica e ferramentas próprias de operação 2D. | O usuário navega da visão geral até bloco, linha, pilha, tier e slot por zoom e duplo clique, retorna preservando contexto e utiliza ponteiro, lupa, informação, seleção múltipla e listagem da área marcada. | ⬜ Pendente |
| BUS1610 | Implementar timeframes gráficos Current, Future, Stow, Preplan, Composite e Imminent. | A mesma geometria alterna entre posição atual, planejada, futura, composta e movimentos iminentes; diferenças são explicadas e a seleção permanece sincronizada entre os horizontes. | ⬜ Pendente |
| BUS1620 | Representar estados operacionais completos diretamente nos elementos 2D. | Slots, contêineres, filas e equipamentos diferenciam proposta, tentativa, definitivo, reservado, atribuído, despachado, em execução, bloqueado, falha e concluído por símbolos acessíveis, legenda e tooltip. | ⬜ Pendente |
| BUS1630 | Implementar flow tools para seleção, sequenciamento e planejamento em lote. | O planejador seleciona múltiplas unidades e destinos por varredura, aplica padrões stack-wise, tier-wise, sentidos de preenchimento, paired 20 e alternância de bays, visualiza toda a proposta e confirma ou cancela o lote atomicamente. | ⬜ Pendente |
| BUS1640 | Desenhar work queues e projeções operacionais sobre o perfil do navio. | Cada bay mostra filas de carga e descarga, quantidade planejada, restante, estado, bloqueio, guindaste e projeções ainda não planejadas; clicar no ícone abre a job list correspondente. | ⬜ Pendente |
| BUS1650 | Implementar Quay Commander 2D com filas e turnos de guindaste editáveis. | Filas podem ser ordenadas, divididas e transferidas entre guindastes por drag-and-drop; a tela calcula término previsto, produtividade, pausas, conflitos e impacto da alteração antes de persistir. | ⬜ Pendente |
| BUS1660 | Implementar EC Console gráfico integrado ao mapa operacional. | POWs, pools, equipamentos, job lists, produtividade, push rate e modo de dispatch são exibidos e operados sobre o desenho, com drill-down e comandos autorizados sem depender apenas de tabelas separadas. | ⬜ Pendente |
| BUS1670 | Exibir CHEs em tempo real dentro do mapa 2D. | Cada equipamento possui posição, heading, conectividade, estado, contêiner transportado, job atual, próximos jobs, trilha e rota; telemetria atrasada é visualmente distinta e impede interpretação como posição atual. | ⬜ Pendente |
| BUS1680 | Implementar alcances operacionais editáveis de CHE no mapa. | RTG, RMG, ASC e demais equipamentos exibem área alcançável, sobreposições, transfer points e regiões inacessíveis; alteração autorizada recalcula cobertura e bloqueia dispatch fora do alcance. | ⬜ Pendente |
| BUS1690 | Integrar filtros, recaps, métricas e desenho de forma bidirecional. | Selecionar uma métrica, célula ou lista destaca os elementos correspondentes no canvas; selecionar um elemento atualiza recaps e tabelas, mantendo o restante acinzentado sem perder contexto. | ⬜ Pendente |
| BUS1700 | Persistir paletas, layouts e workspaces gráficos no servidor. | Cores, atributos, filtros, painéis e posições de tela são versionados, compartilháveis por equipe ou papel, importáveis, exportáveis e recuperáveis em outro navegador, com padrão administrativo e preferência individual. | ⬜ Pendente |
| BUS1710 | Implementar editor gráfico 2D da geometria física do terminal. | Blocos, linhas, pilhas, vias, trilhos, exchange areas, transfer points, tomadas reefer, limites e zonas são desenhados, validados, versionados e publicados sem alterar retroativamente operações encerradas. | ⬜ Pendente |
| BUS1720 | Exibir rede de rotas, sentidos, bloqueios e congestionamento no mapa. | O canvas mostra grafo viário, sentidos, interdições, congestionamento, rota prevista, alternativas, ETA e conflitos; o operador abre a memória de cálculo e simula bloqueios antes de aplicá-los. | ⬜ Pendente |
| BUS1730 | Implementar canvas integrado Rail × Yard × Dispatch. | Contêineres podem ser selecionados no pátio e planejados para vagões, work instructions aparecem sobre vagões e posições, CHE e sequência ficam visíveis e conflitos entre linha ferroviária, pátio e equipamentos são destacados. | ⬜ Pendente |

### BUS1590 e BUS1600 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | workspace e seleção sincronizada | O modo múltiplo sincroniza somente as vistas internas do navio. | Criar store de seleção operacional compartilhada e composição de canvas que integre navio, pátio, ferrovia, equipamento, origem, destino e work instruction. |
| `frontend/cloudport/src/pages/yard/OperationalYardViews.jsx` | navegação das vistas | Há bloco, seção, scan e microvisão selecionados por botões e listas, sem conjunto completo de ferramentas hierárquicas. | Criar controlador de viewport com zoom, pan, breadcrumbs, duplo clique, seleção retangular, ferramenta de informação e retorno preservando posição e filtros. |

### BUS1610 e BUS1620 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/yard/OperationalYardViews.jsx` | overlays e camadas | A tela diferencia situação, ocupação, dwell e reefer, mas não representa horizontes temporais nem todo o ciclo operacional. | Adicionar seletor de timeframe, comparação lado a lado, símbolos de estado, legenda acessível e tooltip com origem do dado e instante de referência. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | overlays do navio | O planner oferece overlays técnicos e vistas sincronizadas, mas não separa graficamente Current, Future, Preplan, Composite e Imminent. | Consumir os estados do BUS1400 e renderizar posições, filas e movimentos com simbologia temporal consistente com o pátio. |

### BUS1630 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | drag-and-drop de slots | A movimentação visual opera predominantemente uma unidade e um destino por vez. | Criar seleção múltipla, ferramentas de varredura, padrões de preenchimento, preview numerado, validação em lote e confirmação única. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/VesselPlannerServico.java` | aplicação do plano | O contrato atual não comprova aplicação atômica de um fluxo gráfico ordenado com vários movimentos dependentes. | Criar comando versionado `AplicarFluxoPlanejamento`, com sequência, validações, idempotência, rollback integral e resposta por movimento. |

### BUS1640 e BUS1650 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | perfil e timeline de guindastes | O perfil mostra slots, tampas, restow, guindastes e alertas, mas as work queues não são objetos gráficos editáveis sobre os bays. | Renderizar filas, quantidades, estado, guindaste e dependências sobre o perfil, com seleção e abertura da job list. |
| `frontend/cloudport/src/pages/CraneExecutionTimeline.jsx` | sequência de execução | A timeline acompanha execução, mas não funciona como Quay Commander para dividir e transferir filas entre turnos e guindastes. | Criar blocos arrastáveis, divisão por ponto da sequência, previsão de término, conflitos, produtividade e simulação antes da confirmação. |

### BUS1660 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/yard/YardWorkPages.jsx` | POW, pool, filas e dispatch | Os dados e comandos existem principalmente em tabelas e painéis separados da representação do pátio. | Criar camada EC Console sobre o mapa com badges, produtividade, job count, push rate, modo de dispatch, drill-down e comandos conforme permissão. |
| `frontend/cloudport/src/pages/yard/OperationalYardViews.jsx` | integração com trabalho | As pilhas mostram ocupação e reservas, mas não exibem POW, pool, fila, equipamento e dispatch como objetos relacionados. | Vincular elementos gráficos aos contratos de fila e equipamento, mantendo atualização em tempo real e seleção sincronizada. |

### BUS1670 e BUS1680 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/ControlRoomEquipamentosPage.jsx` | monitoramento de CHE | Equipamentos aparecem em tabela e inspector com posição textual, telemetria e WI atual. | Criar mapa 2D ao vivo com ícones orientados, trilha, unidade transportada, job atual, próximos jobs, conectividade e stale state. |
| `frontend/cloudport/src/pages/yard/OperationalYardViews.jsx` | equipamentos ao vivo | A tela exibe cartões de CHE, mas não posiciona os equipamentos no canvas nem desenha seu alcance. | Renderizar CHEs sobre a geometria e adicionar editor de ranges, sobreposição, transfer points e cobertura após indisponibilidade. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/controlroom/ControlRoomEquipamentoServico.java` | posição e capacidade operacional | O serviço fornece posição e estado, mas não possui contrato versionado de range geométrico por equipamento e período. | Criar `AlcanceOperacionalEquipamento` com geometria, vigência, restrições, origem, auditoria e consulta para dispatch. |

### BUS1690 e BUS1700 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/yard/OperationalYardViews.jsx` | filtros e workspaces | Filtros e vistas não possuem integração bidirecional completa com recaps; workspaces ficam no `localStorage` do navegador. | Criar selection bus compartilhado, highlight/dimming, recaps clicáveis e API de workspace persistido no servidor. |
| `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx` | legenda e layout | Legendas são predefinidas e o layout não é compartilhado por equipe ou papel. | Criar editor de paleta e layout, preferências por usuário, modelos de grupo e aplicação consistente em todas as vistas 2D. |

### BUS1710 e BUS1720 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/yard/OperationalYardViews.jsx` | geometria do pátio e vias | A interface consome geometrias e posições existentes, mas não funciona como editor físico completo do terminal nem exibe grafo viário operacional. | Criar modo Terminal Editor com desenho, snapping, validação topológica, versionamento, preview, publicação e rollback. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/controlroom/ControlRoomEquipamentoServico.java` | rotas e congestionamento | Posição e heading são disponíveis, mas o frontend não recebe um modelo completo de segmentos, sentidos, bloqueios, custos e congestionamento. | Criar contratos de grafo viário, estado do segmento, rota calculada, alternativas, ETA e simulação de interdição. |

### BUS1730 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/RailLineUpPage.jsx` | composição e pátio ferroviário | A tela permite organizar vagões e contêineres dentro do domínio ferroviário, mas não compartilha um canvas operacional com Yard e CHE. | Criar vista combinada de linhas, vagões, posições do pátio, equipamentos, rotas e work instructions com drag-and-drop validado entre os domínios. |
| `backend/servico-rail/src/main/java/br/com/cloudport/servicorail/ferrovia/listatrabalho/controlador/ListaTrabalhoTremControlador.java` | planejamento visual integrado | Os contratos não expõem uma proposta gráfica consolidada com origem Yard, vagão, sequência e CHE. | Criar DTO de plano Rail × Yard, simulação, validação de conflito, assinatura reproduzível e confirmação coordenada com o BUS1560. |

Todas as novas telas e modos visuais desta seção devem apresentar ícone de manual contextual contendo finalidade, fluxo operacional, explicação dos campos, permissões necessárias, estados possíveis, motivos de bloqueio, exemplos, atalhos e link para o processo completo.