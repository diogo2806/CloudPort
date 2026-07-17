# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após implementação do mapa georreferenciado do Pátio e do requisito `UI50`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Planejamento de embarque

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI60 | Reabrir a criação de plano do Vessel Planner vinculando a escala selecionada. | `POST /api/vessel-planner/planos` recebe `bayPlanId` e `visitaNavioId` da escala selecionada; o plano é criado e persiste a identidade canônica da visita utilizada na tela. | ⬜ Pendente |

### UI60 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/pages/ContainerVesselPlannerPage.jsx` | `createPlan()` e `selectedScale` | A tela exige e exibe uma escala, mas cria o plano chamando a API somente com `bayPlan.id`; `selectedScale.id` não participa do comando. | Bloquear a criação sem escala válida e enviar `selectedScale.id` como `visitaNavioId` junto com `bayPlan.id`. |
| `frontend/cloudport/src/api.js` | `criarPlanoVesselPlanner()` | A assinatura aceita somente `bayPlanId` e produz `{ bayPlanId }`. | Alterar a assinatura para receber também `visitaNavioId` e produzir os dois campos obrigatórios. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/dto/CriarEstivagemPlanRequisicaoDto.java` | campos `bayPlanId` e `visitaNavioId` | Ambos são `@NotNull`; o corpo atual do portal sempre omite a visita e é rejeitado antes do serviço. | Manter o contrato obrigatório e corrigir o frontend para fornecer a identidade da escala selecionada. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/controlador/VesselPlannerControlador.java` | `criarPlano()` | O controller encaminha os dois identificadores ao serviço, mas o produtor React fornece somente um deles. | Manter o vínculo canônico existente no backend e tornar o fluxo React compatível com o contrato. |

## 2. Processamento assíncrono e jobs

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC40 | Reabrir o controle dos agendamentos de Visibilidade para falhar fechado sem habilitação explícita. | `VisibilidadeDashboardJob` só é registrado quando `cloudport.runtime.jobs-enabled=true`; a ausência da propriedade não publica dashboards nem detecta alertas automaticamente em serviços standalone, coexistência ou rollback. | ⬜ Pendente |
| ASYNC80 | Desabilitar por padrão os jobs do Navio Siderúrgico quando a flag canônica não estiver configurada. | Os jobs de expiração de reservas, reconciliação Navio × Pátio e sincronização do cadastro canônico só são registrados quando `cloudport.runtime.jobs-enabled=true`; a ausência da propriedade não inicia processamento real em serviços standalone, coexistência ou rollback. | ⬜ Pendente |

### ASYNC40 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/service/VisibilidadeDashboardJob.java` | `@ConditionalOnProperty` | O job foi separado do serviço, mas usa `matchIfMissing = true`. Quando `cloudport.runtime.jobs-enabled` está ausente, o bean continua registrado e executa `publicarDashboard()` e `detectarAlertasAutomaticos()`, contrariando o controle canônico que mantém legados sem jobs durante coexistência. | Alterar a condição para falhar fechada quando a propriedade estiver ausente, preservando `havingValue = "true"`; a execução standalone deve exigir habilitação explícita em sua configuração de implantação. |
| `docs/implementados/requisitos-implementados.md` | seção `ASYNC40 — agendamentos de visibilidade condicionados implementado` | O registro considera `matchIfMissing=true` parte da conclusão, embora isso habilite processamento real sem autorização explícita e mantenha a fonte de verdade concorrente. | Manter `ASYNC40` reaberto no backlog até a condição exigir explicitamente `cloudport.runtime.jobs-enabled=true`; após a correção comprovada, atualizar o histórico de implementação de forma coerente. |

### ASYNC80 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/ExpiracaoReservaPatioJob.java` | `@ConditionalOnProperty` e `expirarReservasVencidas()` | A condição usa `matchIfMissing = true`; a ausência da flag registra o job e permite expirar reservas reais, embora o runtime canônico deva ser o único executor por padrão. | Alterar a condição para falhar fechada quando a propriedade estiver ausente, preservando `havingValue = "true"` e a coordenação por `ExecucaoUnicaServico`. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/ReconciliacaoNavioPatioJob.java` | `@ConditionalOnProperty` e `reconciliarVisitasAtivas()` | A ausência da propriedade habilita o polling e a sincronização de visitas pendentes em qualquer aplicação que carregue o componente. | Registrar o bean somente com habilitação explícita e manter o bloqueio distribuído como proteção adicional, não como substituto do controle de implantação. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/SincronizacaoCadastroCanonicoJob.java` | `@ConditionalOnProperty` e `sincronizarCadastroCanonico()` | A condição também assume habilitação quando a propriedade não existe e executa reconciliação de projeções persistidas. | Exigir `cloudport.runtime.jobs-enabled=true` para registrar o job e impedir execução implícita em serviços standalone ou instâncias de rollback. |
