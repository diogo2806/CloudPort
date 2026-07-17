# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após auditoria da branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Processamento assíncrono e coexistência operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC30 | Subordinar a recuperação periódica do OCR ao controle canônico de jobs e impedir processamento concorrente entre runtime principal e deployments de rollback. | `recuperarProcessamentosPendentes()` só é registrado quando `cloudport.runtime.jobs-enabled=true`; deployments legados ou de rollback com jobs desabilitados não consultam nem submetem documentos OCR, e uma mesma pendência não pode ser reivindicada simultaneamente por mais de uma instância ativa. | ⬜ Pendente |

### ASYNC30 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/integration/ocr/ProcessamentoOcrPublisher.java` | `recuperarProcessamentosPendentes()` | O método está anotado diretamente com `@Scheduled` e a classe não possui `@ConditionalOnProperty` para `cloudport.runtime.jobs-enabled`. Assim, qualquer deployment que carregue o módulo Gate e tenha scheduling habilitado registra o recuperador, inclusive instâncias legadas mantidas para rollback. | Condicionar o bean ou o método agendado a `cloudport.runtime.jobs-enabled=true`, mantendo o runtime canônico como único executor durante coexistência. Se a publicação imediata após upload precisar continuar disponível com jobs desabilitados, separar o recuperador em novo componente sugerido: `ProcessamentoOcrRecuperacaoJob`. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/integration/ocr/ProcessamentoOcrPublisher.java` | consultas de `PENDENTE`, `PROCESSANDO` expirado e `FALHA` elegível; `mensagens.values().forEach(this::submeter)` | A recuperação apenas lê até 100 registros e os submete ao executor local. Não existe reivindicação persistente, lock ou atualização atômica antes da submissão; duas instâncias ativas podem selecionar e executar o mesmo documento no mesmo ciclo. | Reivindicar cada documento de forma atômica antes da submissão, usando lock transacional, atualização condicional de estado/versão ou contrato equivalente; ignorar itens já reivindicados por outra instância e liberar a pendência de modo coerente em rejeição do executor ou falha antes do início. |
| `backend/cloudport-runtime/src/main/resources/application.properties`, propriedades dos deployments legados e documentação de coexistência | `cloudport.runtime.jobs-enabled` | O projeto define essa propriedade como controle canônico de execução única dos jobs e documenta deployments legados sem jobs, mas o recuperador OCR não a consulta. | Conectar o job à propriedade existente, sem criar uma segunda flag concorrente; manter o valor habilitado apenas no runtime escritor e desabilitado nos deployments de rollback/coexistência. |
