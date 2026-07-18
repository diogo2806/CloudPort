# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Erros, concorrência e consistência operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ERR10 | Tratar disputas concorrentes no registro de entrada e saída de pessoas sem converter conflito operacional em erro interno. | Requisições simultâneas para o mesmo documento consolidam somente uma transição e uma movimentação; a operação perdedora recebe `409 Conflict`, sem resposta `500`, entrada duplicada ou estado divergente. | ⬜ Pendente |

### ERR10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/ControleAcessoPessoasService.java` | `registrarEntrada()` | O fluxo executa `findByDocumentoNormalizado()`, valida `situacao`, altera ou cria `PessoaAcesso` e só consolida a gravação no final da transação. Duas requisições concorrentes podem validar o mesmo estado anterior; a restrição única ou o `@Version` rejeita uma delas apenas no flush/commit, sem conversão para conflito de negócio. | Serializar a transição por documento com lock pessimista ou atualização condicional atômica. Converter colisão de criação e falha de versão em `409 Conflict`, garantindo rollback integral da movimentação perdedora. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/ControleAcessoPessoasService.java` | `registrarSaida()` | A saída também lê e valida a situação antes da atualização. Saídas simultâneas podem passar pela validação `DENTRO`; a proteção otimista atua somente na persistência e a exceção de concorrência não é tratada pelo fluxo HTTP. | Usar a mesma reivindicação atômica da entrada e responder `409 Conflict` quando outra operação já tiver consumido a transição. Persistir somente uma movimentação de saída. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/model/PessoaAcesso.java` | `documentoNormalizado` e `versao` | A entidade possui unicidade do documento e `@Version`, que evitam parte da corrupção, mas as exceções técnicas resultantes não são convertidas em resultado funcional coerente. | Manter as garantias de banco e versão como última defesa, integrando-as ao tratamento explícito de concorrência do serviço. |
| `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/ControleAcessoPessoasController.java` | `POST /gate/pessoas/entradas` e `POST /gate/pessoas/saidas` | Os endpoints não possuem contrato específico para concorrência; falhas de integridade ou optimistic locking escapam do caso de uso. | Garantir resposta `409` com mensagem operacional estável quando a transição já tiver sido realizada por outra requisição. |

## 2. Segurança de exportação

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC60 | Neutralizar fórmulas em valores exportados pela grade operacional antes de gerar CSV. | Células iniciadas por `=`, `+`, `-` ou `@`, inclusive após espaços, tabulação ou caracteres de controle, são exportadas como texto literal e não são executadas como fórmulas ao abrir o arquivo em planilhas. | ⬜ Pendente |

### SEC60 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/operationalDataGrid.js` | `escapeCsv()` e `buildGridCsv()` | O exportador apenas duplica aspas e envolve o valor em aspas. Conteúdo operacional controlado por dados que comece com `=`, `+`, `-` ou `@` permanece interpretável como fórmula por aplicativos de planilha, mesmo estando entre aspas no CSV. | Antes do escape CSV, detectar prefixos de fórmula após espaços e caracteres de controle e prefixar o conteúdo com apóstrofo ou aplicar codificação equivalente que preserve o valor como texto. Manter o tratamento centralizado em `escapeCsv()`. |
| `frontend/cloudport/src/OperationalDataGrid.jsx` | `exportRows()` e `downloadCsv()` | A ação exporta diretamente os registros selecionados ou filtrados usando `buildGridCsv()` e entrega o arquivo ao operador, sem etapa adicional de neutralização. | Consumir somente o CSV neutralizado pelo utilitário central, preservando busca, filtros, seleção, ordem e colunas visíveis. |
