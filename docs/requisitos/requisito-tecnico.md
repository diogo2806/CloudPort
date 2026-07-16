# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-16 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Inicialização e fluxos principais

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INIT10 | Tornar o `cloudport-runtime` o ponto de entrada canônico e manter o runtime anterior somente para rollback. | Build, execução, Compose e documentação principal apontam para `backend/cloudport-runtime`; o runtime anterior inicia apenas em configuração de rollback coerente, sem portas obrigatórias sem implementação. | ⬜ Pendente |

### INIT10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `README.md` | arquitetura, compilação e execução | Apresenta `cloudport-monolito-navio` como runtime principal, embora a decisão vigente defina `cloudport-runtime` como runtime geral. | Apontar comandos e execução principal para `cloudport-runtime` e identificar o runtime anterior apenas como rollback. |
| `backend/cloudport-monolito-navio/src/main/java/br/com/cloudport/monolitonavio/CloudPortMonolitoNavioApplication.java` | `@ComponentScan` | Carrega serviços que exigem `OtimizacaoYardCliente` e `PlanoOtimizadoYardCliente`. | Registrar implementações compatíveis com o modo de rollback ou excluir explicitamente os fluxos indisponíveis. |
| `backend/cloudport-monolito-navio/src/main/resources/application.properties` | `cloudport.modulo.yard.integracao=local` | O modo local desativa os adaptadores HTTP, mas os adaptadores locais de otimização existem somente em `backend/cloudport-runtime`. | Configurar o rollback de acordo com os adaptadores registrados ou incorporar as portas locais obrigatórias. |
| `docs/arquitetura-monolito-modular.md` | runtime geral e rollback | Define corretamente `cloudport-runtime` como alvo e `cloudport-monolito-navio` como primeiro corte de rollback. | Manter esta decisão como fonte única e eliminar orientações concorrentes. |

## 2. Persistência, contratos e estado operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| DATA10 | Validar o plano de guindastes contra work queues reais do Yard antes de persistir. | Cada `workQueueId` pertence à mesma visita, é compatível com berço e porão, está disponível e possui cobertura e recursos válidos; plano inválido não substitui o vigente. | ⬜ Pendente |
| BUS10 | Concluir a aplicação idempotente e compatível do plano otimizado no Yard. | A aplicação rejeita fila incompatível com o bloco e registra de forma única o `planoId`, retornando o resultado anterior em repetição segura sem reaplicar alterações. | 🟡 Em andamento |

### DATA10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/QuayBerthCraneServico.java` | `validarAlocacao()` | `workQueueId` é validado somente como número positivo; existência, visita, status, cobertura, equipamento e compatibilidade não são consultados. | Injetar porta de consulta do Yard e validar a fila real antes de aceitar cada alocação. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/QuayBerthCraneServico.java` | `salvarPlano()` | O plano anterior é substituído e o ID informado é persistido sem confirmação da fonte proprietária. | Validar o comando completo antes da substituição e persistir referências somente após confirmação do Yard. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueueOperacaoServico.java` | consulta de fila e recursos | O Yard possui os vínculos necessários, mas não expõe uma porta local usada pelo plano de guindastes. | Expor contrato de leitura com visita, porão, status, POW, pool, equipamento, recurso de cais e job list. |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/AplicacaoPlanoOtimizadoNavioPatioServico.java` | `replanejar()` e `gerarPlanoId()` | O fluxo já aplica o plano real, calcula indicadores e compensa falhas, mas o ID determinístico não é persistido como aplicação única. | Criar `novo método sugerido: buscarOuRegistrarAplicacaoPlano()` para reutilizar resultado concluído e impedir aplicação concorrente do mesmo plano. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/PlanoOtimizadoPatioServico.java` | `selecionarFila()` | Sem candidata do mesmo `blocoZona`, retorna a primeira fila do equipamento e pode vincular a ordem a outro bloco. | Rejeitar o plano quando não houver fila compatível; não usar fallback para zona diferente. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/PlanoOtimizadoPatioServico.java` | `aplicar()` | `planoId` aparece apenas no histórico e não diferencia primeira aplicação, repetição ou execução em andamento. | Persistir identidade, visita, status e resultado na mesma transação das alterações do Yard. |

## 3. Eventos, integrações e processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC20 | Fazer todos os ingressos EDI usarem a recepção idempotente e o worker transacional. | HTTP e RabbitMQ registram a mesma identidade antes de confirmar; nenhum listener chama o processador diretamente ou converte falha em sucesso; worker e retentativa respeitam o controle de execução. | 🟡 Em andamento |
| ASYNC30 | Usar eventos internos como fluxo principal de atualização Yard → Navio e Navio canônico → projeção siderúrgica. | Mudanças publicam eventos no processo e atualizam consumidores imediatamente; jobs periódicos apenas reconciliam perdas. | ⬜ Pendente |

### ASYNC20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/controlador/EdiIntegracaoControlador.java` | recepção EDI HTTP | Os endpoints persistem a recepção idempotente e retornam `202` com o processamento. | Preservar este fluxo como contrato único de entrada assíncrona. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/mensagem/EdiMensagemListenerServico.java` | `receberCoprar()` e `receberCoarri()` | Os listeners chamam `EdiProcessadorServico` diretamente e capturam exceções sem propagá-las, contornando idempotência, retentativa e quarentena. | Registrar a recepção idempotente e deixar o worker processar; falha de recepção deve impedir confirmação da mensagem. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiProcessamentoWorker.java` | `executar()` | O componente agendado não está condicionado a `cloudport.runtime.jobs-enabled`. | Condicionar criação ou execução ao controle de jobs do runtime. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiAuditoriaServico.java` | chave, retentativa e quarentena | O fluxo auditado está implementado, mas é ignorado pelos listeners RabbitMQ. | Reutilizar o mesmo contrato em todos os canais de entrada. |

### ASYNC30 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/ReconciliacaoNavioPatioJob.java` | `reconciliarVisitasAtivas()` | Varre todas as visitas não terminais para consultar o Yard. | Consumir eventos internos de ordem, reserva e movimento e manter o job apenas para reparar perdas. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/SincronizacaoCadastroCanonicoJob.java` | `sincronizarCadastroCanonico()` | Sincroniza periodicamente toda a projeção siderúrgica. | Publicar evento do cadastro canônico e atualizar somente a projeção afetada. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueueOperacaoServico.java` | `transicionar()` e recursos | Persiste e audita, mas não publica evento interno para Navio Siderúrgico. | Publicar evento após commit com identidade e versão; consumidor deve ser idempotente. |
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio` | atualização canônica | Alterações do domínio não disparam contrato interno para a projeção. | Publicar evento versionado com ID canônico e campos alterados. |

## 4. Interface e navegação operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Expor no frontend a edição de visita e item e a conclusão do plano de estiva. | Usuário autorizado edita pelos contratos existentes e conclui o plano após validação, sem atualizar a tela como sucesso quando a persistência falhar. | ⬜ Pendente |
| UI20 | Concluir a operação do Quay Monitor com o plano de guindastes real. | O frontend carrega o monitor do backend, salva plano com work queues validadas e mantém drill-down, job lists e transições oficiais. | 🟡 Em andamento |

### UI10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | objeto `api` | Não há clientes para atualizar visita, atualizar item ou concluir plano de estiva. | Adicionar chamadas aos contratos existentes com usuário e correlação. |
| `frontend/servico-navio-siderurgico/src/App.jsx` | fluxo de visita, itens e plano | Não oferece formulários de edição nem conclusão do plano. | Criar formulários, confirmação e recarga somente após resposta persistida. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/VisitaNavioControlador.java` | `atualizar()`, `atualizarItem()` e `concluirPlano()` | Os endpoints existem, mas não possuem consumidor na interface. | Preservar os contratos e conectá-los à interface sem rota concorrente. |

### UI20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | Quay Monitor e Yard | Já expõe recursos, dispatch, transições, drill-down, matriz e job lists, mas não chama `POST /visitas-navio/{id}/crane-plan`. | Adicionar gravação do plano e usar a resposta persistida. |
| `frontend/servico-navio-siderurgico/src/App.jsx` | `QuayMonitor`, `EquipmentPanel`, `WorkQueue` e `InstructionDrawer` | Job lists, recursos e drill-down usam os contratos novos. O monitor é derivado localmente das filas e não carrega `GET /quay-monitor`; não há editor de guindastes. | Carregar o monitor real, selecionar filas elegíveis e salvar o plano validado. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/QuayBerthCraneControlador.java` | `GET /quay-monitor` e `POST /crane-plan` | Os contratos existem, mas não são usados integralmente pela interface. | Manter uma única rota de gravação e alimentar o monitor com a resposta real. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/controlador/WorkQueueOperacaoControlador.java` | recursos, transições e drill-down | Os contratos robustos já são consumidos pelo Control Room. | Preservá-los como única fonte de verdade para as ações da interface. |