# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-16 após implementação de ASYNC20, INIT10 e BUS10.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Interface e navegação operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Expor no frontend a edição de visita e item e a conclusão do plano de estiva. | Usuário autorizado edita pelos contratos existentes e conclui o plano após validação, sem atualizar a tela como sucesso quando a persistência falhar. | ⬜ Pendente |

### UI10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | objeto `api` | Não há clientes para `PUT /visitas-navio/{id}`, `PUT /visitas-navio/{id}/itens/{itemId}` ou `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`. | Adicionar chamadas aos contratos existentes e propagar usuário e correlação quando aplicável. |
| `frontend/servico-navio-siderurgico/src/Ui20ControlRoom.jsx` | fluxo de visita, itens e plano de estiva | A interface operacional permite ações do Yard e plano de guindastes, mas não oferece formulários de edição da visita e dos itens nem conclusão do plano de estiva. | Criar formulários com estado de carregamento, validação, confirmação de conclusão e recarga somente após resposta persistida. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/VisitaNavioControlador.java` | `atualizar()`, `atualizarItem()` e `concluirPlano()` | Os endpoints existem e são alcançáveis no backend, porém não possuem consumidor na interface operacional. | Preservar os contratos e conectá-los à interface sem criar rota concorrente. |
