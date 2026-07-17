# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Autenticação e autorização

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC50 | Aplicar autorização por operação nas work queues e work instructions do Pátio. | Consultas permanecem disponíveis aos perfis operacionais necessários, mas criação e alteração de fila, associação de recursos, dispatch, mudança de prioridade, bloqueio, reset, cancelamento e conclusão exigem explicitamente os perfis responsáveis por cada comando; `OPERADOR_GATE` e `SERVICE_NAVIO` não recebem poderes administrativos por uma regra global do controller. | ⬜ Pendente |

### SEC50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/controlador/WorkQueuePatioControlador.java` | `@PreAuthorize` da classe; `criar()`, `ativar()`, `desativar()`, `atualizarPow()`, `atualizarEquipamento()`, `atualizarOrdens()` | A autorização única da classe concede todos os comandos a `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_GATE` e `SERVICE_NAVIO`, inclusive criação e alterações administrativas de filas e recursos. | Manter uma regra de leitura separada e declarar `@PreAuthorize` específico em cada comando, limitando alterações administrativas aos perfis responsáveis pelo planejamento do Pátio. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/controlador/WorkQueueOperacaoControlador.java` | `@PreAuthorize` da classe; `despachar()`, `suspender()`, `retomar()`, `bloquear()`, `concluir()`, `resetar()`, `cancelar()`, `atualizarPrioridades()` | A mesma regra global permite que perfis de Gate e integração de Navio executem dispatch, conclusão, bloqueio, reset, cancelamento e alteração de prioridades. | Definir autorização por método conforme a responsabilidade operacional e falhar com `403` antes de executar o serviço ou publicar eventos quando o perfil não for autorizado. |
| `frontend/cloudport/src/pages/yard/YardWorkPages.jsx` | `canOperate` | A interface replica a regra ampla e exibe todos os comandos para `OPERADOR_GATE` e `SERVICE_NAVIO`, sem distinguir leitura, planejamento, execução e administração. | Derivar permissões por comando a partir dos perfis autorizados pelo backend e ocultar ou desabilitar apenas as ações não permitidas, sem usar a interface como única proteção. |

## 2. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC50 | Subordinar o reshuffling noturno ao controle canônico de jobs. | `executarReshuffflingNoturno()` só é registrado com `cloudport.runtime.jobs-enabled=true`; deployments de rollback não analisam candidatos nem criam ordens, enquanto chamadas explícitas permanecem disponíveis. | ⬜ Pendente |
| ASYNC70 | Desabilitar o worker EDI quando a propriedade canônica de jobs não estiver explicitamente habilitada. | `EdiProcessamentoWorker` só é criado quando `cloudport.runtime.jobs-enabled=true`; ausência da propriedade ou valor `false` não inicia polling nem reivindica mensagens, evitando que serviços standalone de coexistência processem a mesma fila do runtime. | ⬜ Pendente |

### ASYNC50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `executarReshuffflingNoturno()` | O `@Scheduled` está no serviço e não consulta `cloudport.runtime.jobs-enabled`; runtime e standalone podem executar o mesmo cron. | Criar `novo método sugerido: PredictiveReshuffflingJob.executar()`, condicionado pela propriedade canônica, e manter o caso de uso sem anotação. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/ServicoYardApplication.java` e `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | `@EnableScheduling` e component scan | Ambos carregam scheduling e o serviço do Yard. | Garantir que somente a instância com jobs habilitados registre o cron, sem criar flag concorrente. |

### ASYNC70 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiProcessamentoWorker.java` | `@ConditionalOnProperty` e `executar()` | A condição usa `matchIfMissing = true`. Assim, qualquer deployment que não declare a propriedade cria o worker e executa polling a cada segundo, mesmo que apenas a instância canônica deva processar a fila EDI. | Alterar a condição para exigir explicitamente `cloudport.runtime.jobs-enabled=true`, sem habilitação por ausência da propriedade. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiProcessamentoWorkerServico.java` | `processarProximo()` | O método reivindica e processa mensagens reais da fila persistente; portanto, a criação indevida do worker pode concorrer com o runtime ativo. | Manter a reivindicação transacional existente, mas garantir que somente deployments explicitamente habilitados chamem o método pelo agendamento. |