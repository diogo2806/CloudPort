# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Carga geral, stuff e unstuff

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1080 | Implementar identificação e inventário físico reconciliável de cargo lots. | Código de barras ou QR identifica lote e embalagem; contagens e divergências geram ajuste motivado sem sobrescrever diretamente o saldo lógico. | ⬜ Pendente |
| BUS1370 | Controlar lacres durante todo o ciclo de stuff e unstuff. | Lacres previstos, aplicados, rompidos, substituídos e conferidos são vinculados à operação, ao contêiner, ao operador e ao instante; divergência impede conclusão sem decisão autorizada. | ⬜ Pendente |
| BUS1380 | Confirmar pesagem e VGM após a conclusão física do stuffing. | A operação concluída registra tara, peso bruto, método de pesagem, equipamento, responsável e VGM; o contêiner não é liberado para embarque quando o peso exceder limites ou não estiver confirmado. | ⬜ Pendente |
| BUS1390 | Implementar staging de carga por doca e janela operacional antes de stuff e unstuff. | Doca, área de espera, janela, recurso, lotes e contêiner são reservados sem conflito; início e conclusão atualizam ocupação e liberam recursos de forma transacional. | ⬜ Pendente |

### BUS1080 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `AJUSTE_INVENTARIO` | Altera diretamente o saldo sem sessão de contagem e aprovação. | Criar novos métodos sugeridos: `abrirInventarioFisico()`, `registrarContagem()` e `confirmarDivergencia()`. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | seleção de lote | A identificação é manual. | Criar leitura por código de barras ou QR e inventário por posição. |

### BUS1370 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/StuffUnstuffServico.java` | fluxo de execução e conclusão | A operação controla quantidade e saldo, mas não mantém ciclo persistido de lacres por evento operacional. | Criar novos métodos sugeridos: `aplicarLacre()`, `romperLacre()`, `substituirLacre()` e `confirmarLacres()`, com idempotência e autorização. |
| `frontend/cloudport/src/pages/StuffUnstuffPage.jsx` | execução física | Não há conferência obrigatória do lacre previsto versus lacre encontrado ou aplicado. | Exibir histórico de lacres e bloquear conclusão quando houver divergência aberta. |

### BUS1380 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/StuffUnstuffServico.java` | conclusão de stuffing | A conclusão não exige pesagem física nem produz VGM operacional vinculado ao contêiner. | Criar novo método sugerido: `confirmarPesagemStuffing()` e validar tara, peso bruto, capacidade e método de pesagem antes da liberação. |
| `frontend/cloudport/src/pages/StuffUnstuffPage.jsx` | confirmação da operação | A tela não captura equipamento de pesagem, responsável, método nem VGM. | Adicionar etapa de pesagem e estado de liberação por peso confirmado. |

### BUS1390 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/StuffUnstuffServico.java` | criação da operação | A operação referencia carga e contêiner, mas não reserva doca, área de espera, janela ou recurso operacional. | Criar novo agregado sugerido: `ProgramacaoDocaCarga`, com reserva concorrente, início, conclusão e cancelamento. |
| `frontend/cloudport/src/pages/StuffUnstuffPage.jsx` | planejamento | Não há quadro de docas, filas, janelas ou conflitos de ocupação. | Criar agenda operacional de docas e staging ligada às operações. |

## 2. Gate e pátio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1290 | Exigir verificação operacional de motorista e credencial antes de avançar a visita no Gate. | PIN, documento ou credencial válida é conferido contra motorista, transportadora e visita; tentativas, bloqueios e override autorizado são persistidos. | ⬜ Pendente |
| BUS1300 | Implementar fila de pré-gate com chamada, aceite e expiração. | Chegada antecipada cria posição persistida; chamada, aceite, rechamada, expiração, cancelamento e entrada física mantêm ordem e prioridade sem duplicar visita ativa. | ⬜ Pendente |
| BUS1310 | Implementar handoff de custódia em exchange areas do pátio. | Entrega e recebimento registram unidade, área, posição, equipamento, operador, condição, lacres e instante; a custódia muda uma única vez e divergência gera bloqueio. | ⬜ Pendente |
| BUS1320 | Confirmar grounding e ungrounding por equipamento e posição física. | Cada retirada ou colocação confirma CHE, work instruction, origem, destino e leitura da unidade; eventos repetidos ou fora de sequência não alteram inventário. | ⬜ Pendente |

### BUS1290 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | avanço de estágios da visita | O fluxo valida dados operacionais, mas não há desafio persistido de PIN ou credencial associado à decisão de entrada. | Criar novo método sugerido: `verificarCredencialMotorista()` e impedir avanço enquanto a verificação estiver ausente, expirada ou bloqueada. |
| `frontend/cloudport/src/pages/GateOperationsPage.jsx` | processamento da visita | A interface não apresenta etapa de autenticação operacional do motorista com tentativas e override. | Adicionar etapa de verificação antes da autorização da pista. |

### BUS1300 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/GateFlowService.java` | chegada antecipada e entrada | Estados da visita não representam uma fila de chamada com posição, aceite e expiração persistidos. | Criar novo agregado sugerido: `FilaPreGate`, com ordenação transacional e idempotência por visita. |
| `frontend/cloudport/src/pages/GateVisualPage.jsx` | filas e pistas | O quadro mostra operação de Gate, mas não possui contrato persistido de chamada e aceite pelo motorista. | Exibir posição, prioridade, chamada, tempo restante, aceite e rechamada. |

### BUS1310 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/PublicadorEventoMovimentoPatio.java` | eventos de movimento | Publica movimentações de pátio, mas não há contrato específico de entrega e recebimento bilateral de custódia em exchange area. | Criar novos métodos sugeridos: `entregarNaExchangeArea()` e `receberDaExchangeArea()`, com condição e conferência física. |
| `frontend/cloudport/src/pages/yard/YardInsightPages.jsx` | operação de áreas | Não existe fluxo de handoff com duas confirmações e divergência de condição. | Criar painel de custódia pendente, entregue, recebida e divergente. |

### BUS1320 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueuePatioServico.java` | conclusão de work instruction | A conclusão atualiza o trabalho, mas não comprova confirmação física separada de grounding ou ungrounding com leitura da unidade e sequência do equipamento. | Criar novo método sugerido: `confirmarTransferenciaFisica()` com chave idempotente e validação do estado anterior. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | confirmação de instruções | A interface não exige leitura da unidade e confirmação explícita da ação física. | Adicionar confirmação operacional por unidade, CHE, posição e tipo de ação. |

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
