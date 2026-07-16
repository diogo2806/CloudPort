# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-16 após implementação de ASYNC20 e INIT10.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Persistência e regras de negócio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS10 | Concluir a aplicação idempotente e compatível do plano otimizado no Yard. | A aplicação rejeita work queue incompatível com o bloco de destino e registra de forma única o `planoId`, retornando o resultado anterior em repetição segura sem reaplicar alterações ou duplicar auditoria. | 🟡 Em andamento |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/AplicacaoPlanoOtimizadoNavioPatioServico.java` | `replanejar()` e `gerarPlanoId()` | O fluxo aplica o plano real, calcula indicadores e compensa falhas, mas o identificador determinístico não é persistido como aplicação única. Uma repetição após resposta perdida pode aplicar novamente o mesmo plano. | Criar `novo método sugerido: buscarOuRegistrarAplicacaoPlano()` para reutilizar resultado concluído e impedir aplicação concorrente do mesmo plano. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/PlanoOtimizadoPatioServico.java` | `selecionarFila()` | Quando não encontra candidata com `blocoZona` compatível, retorna a primeira fila do equipamento e pode vincular a ordem a outro bloco. | Rejeitar o plano quando não houver fila compatível; não usar fallback para zona diferente. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/PlanoOtimizadoPatioServico.java` | `aplicar()` | `planoId` aparece somente nos detalhes do histórico e não diferencia primeira aplicação, repetição concluída ou execução em andamento. | Persistir identidade, visita, status e resultado na mesma transação das alterações do Yard, com unicidade por plano e visita. |

## 2. Interface e navegação operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Expor no frontend a edição de visita e item e a conclusão do plano de estiva. | Usuário autorizado edita pelos contratos existentes e conclui o plano após validação, sem atualizar a tela como sucesso quando a persistência falhar. | ⬜ Pendente |

### UI10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | objeto `api` | Não há clientes para `PUT /visitas-navio/{id}`, `PUT /visitas-navio/{id}/itens/{itemId}` ou `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`. | Adicionar chamadas aos contratos existentes e propagar usuário e correlação quando aplicável. |
| `frontend/servico-navio-siderurgico/src/Ui20ControlRoom.jsx` | fluxo de visita, itens e plano de estiva | A interface operacional permite ações do Yard e plano de guindastes, mas não oferece formulários de edição da visita e dos itens nem conclusão do plano de estiva. | Criar formulários com estado de carregamento, validação, confirmação de conclusão e recarga somente após resposta persistida. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/VisitaNavioControlador.java` | `atualizar()`, `atualizarItem()` e `concluirPlano()` | Os endpoints existem e são alcançáveis no backend, porém não possuem consumidor na interface operacional. | Preservar os contratos e conectá-los à interface sem criar rota concorrente. |
