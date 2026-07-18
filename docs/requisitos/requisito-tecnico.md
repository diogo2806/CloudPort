# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após conclusão dos BUS10, BUS1000, BUS1010, BUS1020, BUS1030, BUS1040, BUS1050, BUS1060, BUS1080, BUS1090, BUS1100, BUS1110, BUS1120, BUS1150, BUS1160, BUS1170, DATA1180 e INT1140 na branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Operações intermodais de carga geral

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS1070 | Implementar ciclo completo de avaria da carga. | Avarias possuem quantidade afetada, evidências, responsável, bloqueio, inspeção, reparo ou baixa e saldo segregado. | ⬜ Pendente |

### BUS1070 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/CargaGeralServico.java` | `registrarAvaria()` | Sobrescreve um único código e descrição e muda todo o lote para avariado. | Criar agregado de avaria com quantidade afetada, anexos, responsável, estado, bloqueio e reparo. |
| `frontend/cloudport/src/pages/GeneralCargoPage.jsx` | `registerDamage()` | Envia somente código e descrição. | Criar inspector de avarias com evidências e segregação de saldo. |
