# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Persistência e consistência do autoplanejamento do pátio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| DATA20 | Tornar o autoplanejamento do Yard seguro para contêineres ainda sem posição e impedir alocações concorrentes na mesma posição. | `POST /api/yard/patio/automacao/executar-autoplanejamento` processa contêineres sem posição sem lançar `NullPointerException`, seleciona cada posição considerando as alocações feitas no mesmo ciclo e persiste o lote de forma coerente, sem duas unidades ocuparem a mesma posição. | ⬜ Pendente |

### DATA20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/controlador/AutomacaoPatioControlador.java` | `POST /api/yard/patio/automacao/executar-autoplanejamento` e `executarAutoplanejamento()` | O endpoint público chama diretamente o serviço e retorna `200 OK` com o resumo do lote. Portanto, as falhas internas de seleção de posição afetam o fluxo operacional real, não apenas código auxiliar. | Manter o contrato de resposta, mas somente retornar sucesso após o serviço concluir o lote com posições válidas e sem colisões de ocupação. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/AutomacaoPatioServico.java` | `executarAutoplanejamento()`, `encontrarMelhorPosicao()`, `estaOcupada()` e `calcularRehandles()` | O serviço seleciona como candidatos os contêineres que podem estar sem posição, mas `estaOcupada()` e `calcularRehandles()` dereferenciam `c.getPosicao()` sem verificar `null`. Além disso, `encontrarMelhorPosicao()` executa novo `findAll()` a cada contêiner e a lista `posicoes` não é atualizada ou reservada em memória após uma alocação, permitindo que duas iterações escolham a mesma posição antes de uma leitura coerente do estado persistido. A captura ampla de `Exception` converte essas falhas em item de exceção e ainda retorna sucesso global. | Ignorar contêineres sem posição nas consultas de ocupação e rehandles; manter no ciclo uma visão mutável de posições já ocupadas ou reservar a posição com garantia transacional/constraint antes de seguir; eliminar o `findAll()` repetido por item; tratar colisão de persistência como falha operacional explícita e não como sucesso silencioso do lote. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/modelo/ConteinerPatio.java` e migrations proprietárias do Yard | associação de posição e restrição de ocupação | O fluxo depende da associação de posição para representar ocupação, mas a seleção em memória não demonstra exclusividade entre contêineres durante o lote. | Confirmar a cardinalidade e criar, se inexistente, restrição persistente que impeça duas unidades ativas de ocupar a mesma posição; alinhar a regra do serviço à restrição, preservando rollback transacional quando houver conflito. |
