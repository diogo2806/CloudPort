# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-16 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Interface e navegação operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Expor no Control Room a edição de visita e item e a conclusão do plano de estiva. | Usuário autorizado carrega os dados persistidos, edita visita e item pelos contratos existentes, valida e conclui o plano de estiva e só atualiza a interface como sucesso após resposta persistida; falhas permanecem visíveis sem substituir o estado anterior. | ⬜ Pendente |

### UI10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | `atualizarVisita()`, `atualizarItemVisita()`, `validarPlanoEstiva()` e `concluirPlanoEstiva()` | Os clientes dos contratos de atualização e conclusão já existem, validam identificadores e corpos básicos e propagam autenticação e correlação pelo fluxo comum de `request()`. | Preservar estes contratos como única integração do frontend e consumi-los no fluxo visual, sem criar rotas concorrentes ou duplicar regras do backend. |
| `frontend/servico-navio-siderurgico/src/Ui20ControlRoom.jsx` | carregamento da visita, itens e plano de estiva; ações da interface | O componente carrega visitas, resumo, integração de pátio, ordens, work queues, eventos, reservas, Quay Monitor e plano de guindastes, mas não carrega os itens da visita, não mantém formulários de edição e não chama `atualizarVisita()`, `atualizarItemVisita()`, `validarPlanoEstiva()` ou `concluirPlanoEstiva()`. | Adicionar carregamento dos itens e formulários vinculados ao estado persistido, validação antes do envio, confirmação explícita para conclusão e recarga somente depois de resposta bem-sucedida. Em erro, conservar o estado anterior e exibir `formatError()`. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/VisitaNavioControlador.java` | `atualizar()`, `atualizarItem()` e `concluirPlano()` | Os endpoints existem no backend, porém não são alcançados pelo fluxo operacional atual do Control Room. | Manter os contratos e autorizações atuais e fazer a interface usar os identificadores e representações retornados pelo backend, sem simular sucesso local. |
