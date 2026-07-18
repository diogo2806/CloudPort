# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após implementação dos requisitos ERR10, ERR20, ERR30, ERR40, SEC80 e SEC90.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Segurança e proteção de dados

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC70 | Impedir que respostas brutas do TOS e motivos operacionais potencialmente sensíveis sejam gravados integralmente nos logs do Gate. | Falhas da integração registram somente status, recurso, identificador mascarado, código de erro permitido e `correlationId`; corpos externos, dados aduaneiros, tokens, credenciais e dados pessoais não aparecem nos logs nem são incorporados a mensagens técnicas. | ⬜ Pendente |

### SEC70 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/integration/tos/TosClient.java` | `tratarExcecao()` | Para qualquer `WebClientResponseException`, o método lê `getResponseBodyAsString()` e registra o corpo completo em nível `ERROR`. O TOS pode devolver payload com detalhes de booking, contêiner, liberação aduaneira, identificadores internos ou conteúdo técnico não controlado pelo CloudPort. | Não registrar o corpo bruto. Extrair somente código de domínio explicitamente permitido por contrato, limitar tamanho e caracteres, mascarar o identificador e manter o payload completo fora de logs e mensagens. Novo método sugerido: `resumirErroSeguro(WebClientResponseException ex)`. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/integration/tos/TosClient.java` | criação de `TosIntegrationException` para respostas HTTP | A mensagem da exceção incorpora o corpo externo integral. Embora o handler genérico atual devolva mensagem neutra para exceções não mapeadas, o dado sensível continua circulando na cadeia de exceção e é gravado pelo log com stack trace do tratamento global. | Construir exceção apenas com status, recurso, identificador mascarado e código seguro. Preservar a causa técnica sem copiar o corpo da resposta para a mensagem da exceção. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/comum/erro/TratadorExcecoes.java` | `inesperado()` | O handler registra a exceção completa. Quando a causa é `TosIntegrationException` criada com o corpo remoto, o conteúdo reaparece no stack trace mesmo que a resposta HTTP enviada ao cliente seja neutra. | Sanitizar a exceção na origem e garantir que o tratamento central registre apenas metadados operacionais e `correlationId` para falhas de integração conhecidas, sem duplicar payload externo. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/integration/tos/TosIntegrationService.java` | `obterStatusContainer()` e validações de booking/entrada | O fluxo registra número de contêiner, booking e `motivoRestricao` sem classificação ou mascaramento. O motivo pode vir diretamente do sistema externo e conter informação aduaneira ou texto livre. | Definir campos permitidos para observabilidade, mascarar identificadores conforme política e não registrar `motivoRestricao` em texto livre. Manter o detalhe necessário apenas na resposta operacional autorizada ou em armazenamento auditável com acesso restrito. |
