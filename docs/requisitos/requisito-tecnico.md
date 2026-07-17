# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após auditoria da branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Estado transacional e notificações

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| STATE20 | Publicar atualizações WebSocket de visibilidade somente após a confirmação das transações que alteram o estado exibido. | Alterações transacionais de alertas e detecções automáticas só emitem `DASHBOARD_ATUALIZADO` após commit bem-sucedido; rollback ou falha de commit não publica estado inexistente, e chamadas não transacionais continuam podendo atualizar o dashboard diretamente. | ⬜ Pendente |

### STATE20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/service/VisibilidadeDashboardService.java` | `resolverAlerta()` | O método é transacional, persiste o alerta como resolvido e chama `publicarDashboard()` antes de a transação ser confirmada. A consulta executada na mesma transação pode enxergar a alteração ainda não commitada e enviá-la ao tópico `/topic/dashboard/geral`; se o commit falhar depois, os clientes recebem um estado que não existe no banco. | Registrar a publicação para execução após commit, por exemplo com evento transacional ou `TransactionSynchronization`, sem emitir atualização quando houver rollback. Preservar o retorno da operação somente após a persistência coerente. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/service/VisibilidadeDashboardService.java` | `detectarAlertasAutomaticos()` e `publicarDashboard()` | `detectarAlertasAutomaticos()` abre uma transação, executa detecções que podem alterar alertas e publica o dashboard dentro da mesma unidade transacional. A notificação não está vinculada ao resultado final do commit. | Separar a mutação da emissão e disparar a atualização WebSocket após commit bem-sucedido. Novo método sugerido: `publicarDashboardAposCommit()`, com fallback direto apenas quando não houver transação ativa. |

## 2. Processamento assíncrono e coexistência operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC40 | Subordinar os agendamentos de visibilidade ao controle canônico de jobs para impedir detecção e publicação periódica duplicadas entre o runtime e o serviço standalone. | Os agendamentos que chamam `publicarDashboard()` e `detectarAlertasAutomaticos()` só são registrados quando `cloudport.runtime.jobs-enabled=true`; deployments legados ou de rollback com jobs desabilitados não executam detecção automática nem publicação periódica concorrente, sem impedir chamadas explícitas dos casos de uso. | ⬜ Pendente |

### ASYNC40 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/service/VisibilidadeDashboardService.java` | `publicarDashboard()` e `detectarAlertasAutomaticos()` | `publicarDashboard()` possui `@Scheduled` diretamente no serviço e `detectarAlertasAutomaticos()` também é executado como job do módulo. O bean não é condicionado a `cloudport.runtime.jobs-enabled`, embora o mesmo pacote seja carregado pelo runtime canônico e pelo aplicativo standalone de visibilidade. Em coexistência, mais de uma instância pode detectar os mesmos alertas e publicar atualizações periódicas duplicadas. | Remover as anotações de agendamento do serviço de caso de uso e criar novo componente sugerido: `VisibilidadeDashboardJob`, condicionado por `@ConditionalOnProperty(prefix = "cloudport.runtime", name = "jobs-enabled", havingValue = "true", matchIfMissing = true)`. Manter `publicarDashboard()` disponível para chamadas explícitas e aplicar a mesma condição ao disparo de detecção automática. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/VisibilidadeApplication.java` | `@EnableScheduling` | O aplicativo standalone habilita scheduling sempre que iniciado. Como não há condição no bean de visibilidade, uma instância de rollback registra os mesmos agendamentos do runtime. | Preservar o suporte a scheduling para outros componentes, mas impedir o registro dos jobs de visibilidade quando `cloudport.runtime.jobs-enabled=false`. |
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | `@ComponentScan` e `@EnableScheduling` | O runtime carrega `br.com.cloudport.visibilidade` e habilita scheduling. Portanto, os métodos anotados em `VisibilidadeDashboardService` são registrados no runtime ao mesmo tempo em que podem permanecer ativos no serviço standalone. | Usar exclusivamente a propriedade canônica para escolher a instância escritora dos jobs, sem introduzir uma flag específica concorrente para visibilidade. |
