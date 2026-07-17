# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após implementação do requisito `ASYNC30`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Processamento assíncrono e coexistência operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC50 | Subordinar o reshuffling noturno do Yard ao controle canônico de jobs para impedir criação duplicada de ordens durante coexistência entre o runtime e o serviço standalone. | `executarReshuffflingNoturno()` só é registrado quando `cloudport.runtime.jobs-enabled=true`; deployments legados ou de rollback com jobs desabilitados não analisam candidatos nem criam ordens de remanejamento, enquanto a análise e a execução explícitas permanecem disponíveis aos casos de uso autorizados. | ⬜ Pendente |

### ASYNC50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `executarReshuffflingNoturno()` | O método transacional está anotado diretamente com `@Scheduled(cron = "0 0 2 * * ?")` dentro do serviço de caso de uso. A classe não consulta `cloudport.runtime.jobs-enabled`; cada instância que carregar o módulo Yard e habilitar scheduling pode analisar os mesmos contêineres e chamar `registrarOrdem()` para os mesmos candidatos. | Remover o agendamento do serviço e criar novo componente sugerido: `PredictiveReshuffflingJob`, condicionado por `@ConditionalOnProperty(prefix = "cloudport.runtime", name = "jobs-enabled", havingValue = "true", matchIfMissing = true)`, delegando a `executarReshuffflingNoturno()`. Manter `analisarNecessidadeReshuffling()` e a execução explícita disponíveis sem registrar cron quando jobs estiverem desabilitados. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/ServicoYardApplication.java` | `@EnableScheduling` | O serviço standalone do Yard habilita scheduling incondicionalmente. Durante coexistência ou rollback, ele pode registrar o mesmo reshuffling noturno carregado pelo runtime principal. | Preservar o scheduling da aplicação, mas impedir o registro específico do job de reshuffling quando `cloudport.runtime.jobs-enabled=false`. |
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | `@ComponentScan` de `br.com.cloudport.servicoyard` e `@EnableScheduling` | O runtime canônico também carrega o serviço e habilita scheduling. Sem condição no job, runtime e standalone podem executar o cron das 02:00 simultaneamente e criar ordens concorrentes. | Usar a propriedade canônica de jobs para garantir um único executor durante coexistência, sem criar flag paralela específica para reshuffling. |
