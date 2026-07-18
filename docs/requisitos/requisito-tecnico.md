# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Processamento assíncrono e execução standalone

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC90 | Desabilitar por padrão os jobs persistentes do Gate quando a execução não tiver habilitação explícita. | O serviço Gate e o runtime consolidado só registram e executam o job de recuperação OCR quando `cloudport.runtime.jobs-enabled=true`; a ausência da propriedade não inicia polling nem reivindicação de documentos. | ⬜ Pendente |

### ASYNC90 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/resources/application.properties` | `cloudport.runtime.jobs-enabled` | A propriedade usa `${CLOUDPORT_JOBS_ENABLED:true}`. Na execução standalone, a ausência da variável habilita jobs persistentes, contrariando o comportamento fail-closed registrado na arquitetura. | Alterar o valor padrão para `false` e exigir habilitação explícita no deployment responsável pelo processamento. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/integration/ocr/ProcessamentoOcrRecuperacaoJob.java` | `@ConditionalOnProperty` em `ProcessamentoOcrRecuperacaoJob` | A condição define `matchIfMissing = true`. Se a propriedade não for carregada, o bean é registrado e `recuperarProcessamentosPendentes()` consulta documentos pendentes, expirados e em retentativa, reivindicando-os para processamento. | Remover `matchIfMissing = true` ou defini-lo como `false`, mantendo `havingValue = "true"`, para que o job exista somente quando a habilitação for explícita. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/integration/ocr/ProcessamentoOcrRecuperacaoJob.java` | `recuperarProcessamentosPendentes()` e `reivindicarESubmeter(Long)` | O fluxo alcança o repositório e o publisher reais; portanto, uma instância standalone ou de rollback iniciada sem configuração pode concorrer com o runtime ativo pela mesma fila persistida. | Garantir que nenhuma consulta, reivindicação ou submissão ocorra quando jobs estiverem desabilitados, preservando uma única instância responsável pelo processamento OCR. |
