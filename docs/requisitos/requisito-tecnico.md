# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após implementação do requisito `SEC50`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC70 | Desabilitar o worker EDI quando a propriedade canônica de jobs não estiver explicitamente habilitada. | `EdiProcessamentoWorker` só é criado quando `cloudport.runtime.jobs-enabled=true`; ausência da propriedade ou valor `false` não inicia polling nem reivindica mensagens, evitando que serviços standalone de coexistência processem a mesma fila do runtime. | ⬜ Pendente |

### ASYNC70 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiProcessamentoWorker.java` | `@ConditionalOnProperty` e `executar()` | A condição usa `matchIfMissing = true`. Assim, qualquer deployment que não declare a propriedade cria o worker e executa polling a cada segundo, mesmo que apenas a instância canônica deva processar a mesma fila do runtime. | Alterar a condição para exigir explicitamente `cloudport.runtime.jobs-enabled=true`, sem habilitação por ausência da propriedade. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiProcessamentoWorkerServico.java` | `processarProximo()` | O método reivindica e processa mensagens reais da fila persistente; portanto, a criação indevida do worker pode concorrer com o runtime ativo. | Manter a reivindicação transacional existente, mas garantir que somente deployments explicitamente habilitados chamem o método pelo agendamento. |
