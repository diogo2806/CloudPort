# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Operações de stuff e unstuff

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS10 | Implementar fluxo operacional persistido de stuff e unstuff vinculado a contêiner, cargo lots e local de operação. | O operador cria uma ordem de estufagem ou desova, seleciona o contêiner e os lotes, registra quantidades planejadas e realizadas por etapa, executa parcialmente, trata divergências e avarias, e conclui ou cancela a operação com atualização transacional dos saldos e do histórico. | ⬜ Pendente |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/CargaGeralTipos.java` | `TipoMovimentacaoCarga` | O domínio possui recebimento, carga/descarga parcial, consolidação e desconsolidação, mas não representa ordem, ciclo de vida ou etapas próprias de estufagem e desova. | Criar contratos específicos para tipo e status da operação de stuff/unstuff, sem reduzir o processo a uma movimentação genérica de estoque. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarMovimentacao(UUID, RegistrarMovimentacaoRequest)` | O método altera o saldo de um único lote e registra uma movimentação. Não vincula contêiner, conjunto de lotes, planejamento, conferência, lacre, execução parcial, divergência ou fechamento atômico de uma operação de stuff/unstuff. | Implementar novo método sugerido: `criarOperacaoStuffUnstuff()`, além de comandos transacionais para iniciar, registrar execução parcial, concluir e cancelar, com bloqueio dos lotes envolvidos, validação de saldo/capacidade e histórico imutável. |
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | persistência em `LoteCarga` e `MovimentacaoCarga` | A localização e o saldo são atualizados diretamente no lote; não existe agregado persistido que permita recuperar a composição planejada e realizada de uma estufagem ou desova, nem compensar uma conclusão parcial. | Criar entidade e repositório da operação com itens por cargo lot, contêiner, armazém/posição, equipe ou recurso, quantidades planejadas/realizadas, lacres, avarias, timestamps, usuário e motivo de cancelamento; atualizar saldos e status na mesma transação. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `blankMovement()`, `registerMovement()` e formulário de movimentação | A interface oferece somente um formulário genérico de movimentação por lote. Não há seleção de contêiner, múltiplos lotes, sequência operacional, conferência, progresso, divergências ou encerramento da operação. | Criar tela operacional de stuff/unstuff com criação da ordem, seleção de contêiner e lotes, quantidades planejadas, execução por item, progresso, registro de avarias/divergências e comandos de conclusão ou cancelamento conforme o estado persistido. |
