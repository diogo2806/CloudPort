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

## 2. Eventos, idempotência e confirmação de mensagens

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC10 | Validar eventos ferroviários antes de registrá-los como processados e impedir confirmação definitiva de mensagens funcionalmente inválidas. | Eventos sem `containerId`/`codigoConteiner`, com tipo não suportado ou com campos obrigatórios inválidos não ficam gravados como processados; falhas transitórias provocam rollback e redelivery, enquanto rejeições definitivas são classificadas e encaminhadas de forma explícita sem bloquear correções legítimas. | ⬜ Pendente |

### ASYNC10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/service/EventoProcessadoService.java` | `processarUmaVez()` | O registro idempotente é salvo e sincronizado com `saveAndFlush()` antes da execução do processador. A transação protege exceções propagadas, mas qualquer validação que apenas retorne normalmente confirma o registro como processado. | Separar validação funcional da gravação idempotente ou exigir que rejeições funcionais interrompam a transação com exceção tipada; gravar como processado somente depois que o evento tiver sido aceito e aplicado com sucesso. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/listener/RailEventListener.java` | `handleRailEvent()`, `processarEvento()`, `processarEventoLegado()` e `processarEventoOperacional()` | Eventos sem identificador de contêiner apenas geram `warn` e retornam; tipos sem processador apenas geram `debug` e retornam. Como esses retornos acontecem dentro de `processarUmaVez()`, a transação é concluída e a identidade fica registrada, tornando uma mensagem corrigida com a mesma identidade uma colisão ou redelivery ignorado. Datas inválidas também são convertidas em `null` sem classificação explícita. | Validar tipo, identidade e campos obrigatórios antes de concluir o processamento; lançar exceção funcional tipada para rejeição definitiva ou transitória; definir tratamento explícito para dead-letter/rejeição e somente confirmar o evento quando o movimento tiver sido persistido ou quando a rejeição definitiva tiver sido registrada em estado próprio, distinto de “processado com sucesso”. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/dto/evento/EventoRecebido.java` | contrato do envelope e identidade do evento | O envelope fornece identidade, tipo e campos auxiliares, mas o fluxo do listener não demonstra uma etapa única de validação do contrato ferroviário antes da deduplicação persistente. | Criar novo método sugerido: `validarEventoFerroviario()` ou DTO específico validado para cada tipo, garantindo que a identidade idempotente só seja consumida após contrato mínimo válido. |
