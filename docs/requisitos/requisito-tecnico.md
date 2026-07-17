# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após implementação do requisito `BUS20`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Fontes de verdade e dados de estiva

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ARCH20 | Vincular os planejadores de estiva ao navio e à visita canônicos. | Planos de contêineres e bobinas persistem os identificadores canônicos de navio e visita; IMO, nome e viagem não possuem cadastro concorrente, e perfis estruturais ficam vinculados e versionados sob a mesma identidade. | ⬜ Pendente |
| DATA30 | Substituir a malha artificial do Vessel Planner pela geometria real do navio. | O plano usa bays, rows, tiers, hatch covers, slots restritos, tomadas reefer e limites de pilha do perfil versionado; posições do Bay Plan são preservadas e a operação é bloqueada quando o perfil estiver ausente ou incompatível. | ⬜ Pendente |

### ARCH20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio/navio/entidade/Navio.java` | entidade `Navio` | O módulo Navio mantém IMO único e os dados comuns do cadastro canônico. | Expor uma porta de consulta para os planejadores e manter esta entidade como proprietária da identidade. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/modelo/NavioGranel.java` | entidade `NavioGranel` | Mantém outro IMO, nome e classe editáveis, sem referência ao navio canônico. | Transformar o registro em perfil estrutural vinculado ao ID canônico e migrar os planos existentes sem perda. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/PlanoEstivaBulkServico.java` e `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/EstivagemPlan.java` | `registrarNavio()`, `criarPlano()`, `codigoNavio`, `codigoViagem` | Os planos usam navio local ou textos de navio/viagem e não validam a visita operacional. | Resolver navio e visita pela porta canônica, persistir IDs e versão da fonte e rejeitar combinações incompatíveis. |

### DATA30 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/VesselPlannerServico.java` | `criarPlanoDeBayPlan()` | Cria sempre 30 bays, 10 rows e 8 tiers, todos `NORMAL`, com limite uniforme de 30.000 kg; a posição importada não é usada. | Carregar o perfil real versionado, criar apenas slots existentes e mapear cada contêiner para a posição do Bay Plan. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/EstivagemPlan.java` | construtor | Injeta LPP `300`, boca `45`, calado `14`, deslocamento `90000`, GM `1,5`, TPC `75` e LCB `150`. | Remover defaults operacionais e exigir valores provenientes do perfil aprovado para a condição de carregamento. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/SlotNavio.java` | tipo e limites do slot | O fluxo não carrega hatch cover, restrições, comprimento admissível ou limites reais por slot e pilha. | Persistir e validar a geometria e as restrições reais necessárias à alocação. |

## 2. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC50 | Subordinar o reshuffling noturno ao controle canônico de jobs. | `executarReshuffflingNoturno()` só é registrado com `cloudport.runtime.jobs-enabled=true`; deployments de rollback não analisam candidatos nem criam ordens, enquanto chamadas explícitas permanecem disponíveis. | ⬜ Pendente |

### ASYNC50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `executarReshuffflingNoturno()` | O `@Scheduled` está no serviço e não consulta `cloudport.runtime.jobs-enabled`; runtime e standalone podem executar o mesmo cron. | Criar `novo método sugerido: PredictiveReshuffflingJob.executar()`, condicionado pela propriedade canônica, e manter o caso de uso sem anotação. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/ServicoYardApplication.java` e `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | `@EnableScheduling` e component scan | Ambos carregam scheduling e o serviço do Yard. | Garantir que somente a instância com jobs habilitados registre o cron, sem criar flag concorrente. |

## 3. Interface operacional React

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI50 | Transformar o módulo Pátio em telas React operacionais, e não apenas consultas genéricas. | Mapa, posições, lista de trabalho, movimentações, recursos, indicadores e automação possuem telas próprias; o operador navega pela estrutura real do pátio, consulta detalhes e executa somente comandos autorizados, com motivo quando exigido e sucesso apenas após confirmação persistida. | ⬜ Pendente |

### UI50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/App.jsx` e `frontend/cloudport/src/pages/OperationalPages.jsx` | rotas `/home/patio/**`, `DATASET_ROUTES`, `GenericDatasetPage` | Posições, movimentações, lista de trabalho, recursos, indicadores e automação são tabelas genéricas; lista de trabalho reutiliza a consulta de movimentações e indicadores/automação reutilizam o mapa. | Criar páginas React próprias para cada fluxo, com filtros, paginação, seleção, detalhe, estados vazios e ações coerentes com o domínio. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | `YardMapPage()` | O mapa atual exibe duas tabelas de contêineres e equipamentos; não representa blocos ou zonas, linhas, colunas, camadas, ocupação, reserva, bloqueio e interdição como estrutura navegável. | Implementar visualização operacional baseada nas posições reais, permitindo abrir pilha, unidade, equipamento, restrições e reservas sem inventar coordenadas. |
| `frontend/cloudport/src/api.js` | `obterMapaPatio()`, `listarPosicoesPatio()`, `listarMovimentacoesPatio()`, `listarConteineresPatio()`, `listarRecursosPatio()` | O portal principal expõe somente leituras simples do Yard e não possui contratos para work queues, work instructions, reservas, placement, remanejamento ou reshuffling. | Adicionar métodos para consultas e comandos do Pátio, enviar motivo e identidade operacional quando exigidos e recarregar o estado persistido depois de cada comando. |
| `frontend/servico-navio-siderurgico/src/Ui20ControlRoom.jsx` e `frontend/cloudport/src/pages/OperationalPages.jsx` | ações do Control Room e módulo Pátio | Parte das operações de Yard existe apenas dentro do Control Room incorporado por `iframe`, enquanto as rotas próprias do Pátio permanecem somente leitura. | Reutilizar contratos e componentes compartilháveis ou expor fluxos equivalentes nas páginas do Pátio, sem duplicar regra e sem manter comportamentos divergentes. |
