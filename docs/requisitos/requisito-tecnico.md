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
| `backend/cloudport-monolito-navio/src/main/java/br/com/cloudport/monolitonavio/CloudPortMonolitoNavioApplication.java` | `@ComponentScan` | Carrega serviços que exigem `OtimizacaoYardCliente` e `PlanoOtimizadoYardCliente`, mas os adaptadores locais dessas duas portas existem somente em `backend/cloudport-runtime`. | Registrar implementações compatíveis com o modo de rollback ou excluir explicitamente os fluxos indisponíveis. |
| `backend/cloudport-monolito-navio/src/main/resources/application.properties` | `cloudport.modulo.yard.integracao=local` | O modo local desativa os adaptadores HTTP de otimização sem disponibilizar todas as implementações locais obrigatórias nesse executável. | Configurar o rollback de acordo com os adaptadores registrados ou incorporar as portas locais obrigatórias. |
| `docs/arquitetura-monolito-modular.md` | runtime geral e rollback | Define `cloudport-runtime` como alvo e `cloudport-monolito-navio` como primeiro corte preservado para rollback. | Manter esta decisão como fonte única e eliminar as orientações concorrentes nos documentos de entrada. |

## 2. Persistência e regras de negócio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS10 | Concluir a aplicação idempotente e compatível do plano otimizado no Yard. | A aplicação rejeita work queue incompatível com o bloco de destino e registra de forma única o `planoId`, retornando o resultado anterior em repetição segura sem reaplicar alterações ou duplicar auditoria. | 🟡 Em andamento |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/AplicacaoPlanoOtimizadoNavioPatioServico.java` | `replanejar()` e `gerarPlanoId()` | O fluxo aplica o plano real, calcula indicadores e compensa falhas, mas o identificador determinístico não é persistido como aplicação única. Uma repetição após resposta perdida pode aplicar novamente o mesmo plano. | Criar `novo método sugerido: buscarOuRegistrarAplicacaoPlano()` para reutilizar resultado concluído e impedir aplicação concorrente do mesmo plano. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/PlanoOtimizadoPatioServico.java` | `selecionarFila()` | Quando não encontra candidata com `blocoZona` compatível, retorna a primeira fila do equipamento e pode vincular a ordem a outro bloco. | Rejeitar o plano quando não houver fila compatível; não usar fallback para zona diferente. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/PlanoOtimizadoPatioServico.java` | `aplicar()` | `planoId` aparece somente nos detalhes do histórico e não diferencia primeira aplicação, repetição concluída ou execução em andamento. | Persistir identidade, visita, status e resultado na mesma transação das alterações do Yard, com unicidade por plano e visita. |

## 3. Integrações e processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC20 | Fazer todos os ingressos EDI usarem a recepção idempotente e o worker transacional. | HTTP e RabbitMQ registram a mesma identidade antes de confirmar; nenhum listener chama o processador diretamente ou converte falha em sucesso; worker, retentativa e quarentena respeitam os controles de execução do runtime. | 🟡 Em andamento |

### ASYNC20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/controlador/EdiIntegracaoControlador.java` | recepção BAPLIE, COPRAR, COARRI e VERMAS | Os endpoints HTTP persistem a recepção idempotente e retornam `202` com o identificador do processamento. | Preservar este fluxo como contrato único de entrada para o processamento assíncrono. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/mensagem/EdiMensagemListenerServico.java` | `receberCoprar()` e `receberCoarri()` | Os listeners RabbitMQ chamam `EdiProcessadorServico` diretamente e capturam exceções sem propagá-las, contornando idempotência, retentativa e quarentena e permitindo confirmação da mensagem com falha. | Registrar a recepção idempotente e deixar o worker executar o negócio; falha de recepção deve impedir a confirmação da mensagem. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiProcessamentoWorker.java` | `executar()` | O componente agendado não está condicionado a `cloudport.runtime.jobs-enabled`. | Condicionar a criação ou execução do worker ao controle de jobs do runtime para impedir processamento concorrente em coexistência e rollback. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiAuditoriaServico.java` e `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/repositorio/ProcessamentoEdiRepositorio.java` | chave idempotente, retentativa e quarentena | O fluxo auditado e transacional está implementado para HTTP, mas é ignorado pelos listeners RabbitMQ. | Reutilizar os mesmos contratos em todos os canais de entrada, sem manter caminho síncrono concorrente. |

## 4. Interface e navegação operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Expor no frontend a edição de visita e item e a conclusão do plano de estiva. | Usuário autorizado edita pelos contratos existentes e conclui o plano após validação, sem atualizar a tela como sucesso quando a persistência falhar. | ⬜ Pendente |

### UI10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | objeto `api` | Não há clientes para `PUT /visitas-navio/{id}`, `PUT /visitas-navio/{id}/itens/{itemId}` ou `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`. | Adicionar chamadas aos contratos existentes e propagar usuário e correlação quando aplicável. |
| `frontend/servico-navio-siderurgico/src/Ui20ControlRoom.jsx` | fluxo de visita, itens e plano de estiva | A interface operacional permite ações do Yard e plano de guindastes, mas não oferece formulários de edição da visita e dos itens nem conclusão do plano de estiva. | Criar formulários com estado de carregamento, validação, confirmação de conclusão e recarga somente após resposta persistida. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/VisitaNavioControlador.java` | `atualizar()`, `atualizarItem()` e `concluirPlano()` | Os endpoints existem e são alcançáveis no backend, porém não possuem consumidor na interface operacional. | Preservar os contratos e conectá-los à interface sem criar rota concorrente. |