# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após implementação do requisito `DATA30`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Fontes de verdade e dados de estiva

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ARCH20 | Vincular os planejadores de estiva ao navio e à visita canônicos. | Planos de contêineres e bobinas persistem os identificadores canônicos de navio e visita; IMO, nome e viagem não possuem cadastro concorrente, e perfis estruturais ficam vinculados e versionados sob a mesma identidade. | ⬜ Pendente |

### ARCH20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio/navio/entidade/Navio.java` | entidade `Navio` | O módulo Navio mantém IMO único e os dados comuns do cadastro canônico. | Expor uma porta de consulta para os planejadores e manter esta entidade como proprietária da identidade. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/modelo/NavioGranel.java` | entidade `NavioGranel` | Mantém outro IMO, nome e classe editáveis, sem referência ao navio canônico. | Transformar o registro em perfil estrutural vinculado ao ID canônico e migrar os planos existentes sem perda. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/PlanoEstivaBulkServico.java` e `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/EstivagemPlan.java` | `registrarNavio()`, `criarPlano()`, `codigoNavio`, `codigoViagem` | Os planos usam navio local ou textos de navio/viagem e não validam a visita operacional. | Resolver navio e visita pela porta canônica, persistir IDs e versão da fonte e rejeitar combinações incompatíveis. |

## 2. Cálculo, aprovação e segurança operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS20 | Impedir aprovação com cálculos hidrostáticos ou estruturais sintéticos. | Planos operacionais usam dados e limites versionados do navio para calado, trim, banda, GM, força cortante e momento fletor; ausência de dados obrigatórios bloqueia a aprovação ou identifica o resultado como simulação não operacional. | ⬜ Pendente |

### BUS20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/EstabilidadeNavioServico.java` | `calcular()` | Usa limites globais, altura fixa e coordenadas derivadas da malha; não calcula calado, força cortante ou momento fletor. | Usar dados hidrostáticos e de resistência longitudinal do navio, condição de pesos/lastro e limites da viagem. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/EstabilidadeEstruturalServico.java` | `calcular()` | Aplica 20 seções, distribuições uniformes, fatores e dimensões padrão; retorna trim `0`. | Substituir aproximações por curvas, limites e distribuição real versionados; falhar fechado quando faltarem entradas. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/VesselPlannerServico.java` e `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/PlanoEstivaBulkServico.java` | `validarEAprovar()` | A aprovação depende dos cálculos simplificados e o fluxo bulk aceita GM padrão `1,5`. | Exigir validações completas e registrar versão das entradas, memória de cálculo e resultado da aprovação. |

## 3. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC50 | Subordinar o reshuffling noturno ao controle canônico de jobs. | `executarReshuffflingNoturno()` só é registrado com `cloudport.runtime.jobs-enabled=true`; deployments de rollback não analisam candidatos nem criam ordens, enquanto chamadas explícitas permanecem disponíveis. | ⬜ Pendente |

### ASYNC50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `executarReshuffflingNoturno()` | O `@Scheduled` está no serviço e não consulta `cloudport.runtime.jobs-enabled`; runtime e standalone podem executar o mesmo cron. | Criar `novo método sugerido: PredictiveReshuffflingJob.executar()`, condicionado pela propriedade canônica, e manter o caso de uso sem anotação. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/ServicoYardApplication.java` e `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | `@EnableScheduling` e component scan | Ambos carregam scheduling e o serviço do Yard. | Garantir que somente a instância com jobs habilitados registre o cron, sem criar flag concorrente. |
