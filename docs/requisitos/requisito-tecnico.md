# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após auditoria da branch `main` e comparação com a base documental operacional.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Fontes de verdade e dados de estiva

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ARCH20 | Vincular os planejadores de estiva ao navio e à visita canônicos. | Planos de contêineres e bobinas persistem os identificadores canônicos de navio e visita; IMO, nome e viagem não possuem cadastro concorrente, e perfis estruturais ficam vinculados e versionados sob a mesma identidade. | ⬜ Pendente |
| DATA30 | Substituir a malha artificial do Vessel Planner pela geometria real do navio. | O plano usa bays, rows, tiers, hatch covers, slots restritos, tomadas reefer e limites de pilha do perfil versionado; posições do Bay Plan são preservadas e a operação é bloqueada quando o perfil estiver ausente ou incompatível. | ⬜ Pendente |
| INT20 | Preservar os atributos operacionais e de segurança recebidos no BAPLIE. | Navio e viagem são validados sem identificadores sintéticos; pesos são normalizados por unidade; posição, operação, cheio/vazio, VGM quando presente, reefer, carga perigosa e OOG são persistidos em campos próprios e chegam ao planejador sem inferência textual. | ⬜ Pendente |

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

### INT20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/parser/BaplieParser.java` | `parse()`, `processarTdt()`, `processarEqd()`, `extrairPeso()` | Todo equipamento vira `DESCARGA`; ausência de código gera `NAVIO_<viagem>`; a unidade do peso é ignorada; `HAN` é reduzido a texto. | Validar o perfil BAPLIE aceito, normalizar unidades, mapear os qualificadores suportados e rejeitar identidade ou dados obrigatórios ausentes. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/modelo/BayPlanContainer.java` | contrato persistido | Não há campos estruturados para classe/ONU e segregação, parâmetros reefer, dimensões OOG, cheio/vazio e origem/status do VGM. | Criar migration aditiva e campos próprios, preservando o conteúdo original para auditoria. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/AutoStowageServico.java` | `sugerirEstivagem()` | Reefer é inferido por substring do ISO; perigoso por `statusOperacao.startsWith("IMO")`, embora o parser grave `MANUSEIO:*`; a classe IMO não chega ao slot. | Consumir os campos estruturados e aplicar compatibilidade, segregação e restrições sem inferência textual. |

## 2. Cálculo, aprovação e segurança operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS20 | Impedir aprovação com cálculos hidrostáticos ou estruturais sintéticos. | Planos operacionais usam dados e limites versionados do navio para calado, trim, banda, GM, força cortante e momento fletor; ausência de dados obrigatórios bloqueia a aprovação ou identifica o resultado como simulação não operacional. | ⬜ Pendente |
| BUS30 | Validar dunnage, empilhamento, calçamento e lashing de bobinas antes da aprovação. | Cada posição é validada contra geometria do porão, tank top, peso e dimensões, camadas, espaçamento, dunnage, calços, pontos/capacidade de amarração e sequência de descarga; valores estimados não aprovam o plano. | ⬜ Pendente |
| BUS40 | Tornar o reshuffling dependente da pilha e do mapa real, com destino reservado e ordem única. | O bloqueador é identificado pela posição e camada; o destino existe e respeita ocupação, reservas e regras de empilhamento; reserva e ordem são criadas atomicamente e de forma idempotente. | ⬜ Pendente |

### BUS20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/EstabilidadeNavioServico.java` | `calcular()` | Usa limites globais, altura fixa e coordenadas derivadas da malha; não calcula calado, força cortante ou momento fletor. | Usar dados hidrostáticos e de resistência longitudinal do navio, condição de pesos/lastro e limites da viagem. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/EstabilidadeEstruturalServico.java` | `calcular()` | Aplica 20 seções, distribuições uniformes, fatores e dimensões padrão; retorna trim `0`. | Substituir aproximações por curvas, limites e distribuição real versionados; falhar fechado quando faltarem entradas. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/servico/VesselPlannerServico.java` e `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/PlanoEstivaBulkServico.java` | `validarEAprovar()` | A aprovação depende dos cálculos simplificados e o fluxo bulk aceita GM padrão `1,5`. | Exigir validações completas e registrar versão das entradas, memória de cálculo e resultado da aprovação. |

### BUS30 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/PlanoEstivaBulkServico.java` | `posicionarBobina()`, `validarEAprovar()` | Espessura ausente vira 50 mm, lashing ausente vira `SEM_LASHING`; a aprovação consulta apenas a estabilidade estrutural. | Criar `novo método sugerido: validarPlanoCompleto()` e exigir tank top, empilhamento, dunnage, calçamento, lashing e estabilidade na mesma versão. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/servico/TacktopServico.java` | `calcularTacktop()` | O ângulo resulta sempre em 30 graus para diâmetro válido e são geradas quantidades fixas de correntes, cunhas e cintas, sem forças, atrito, pontos ou capacidade do material. | Calcular ou validar o securing a partir do navio, viagem, arranjo, bobina, materiais e pontos disponíveis; não mutar posições por estimativa. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/modelo/PosicaoBobina.java` e `MaterialLashingBulk.java` | parâmetros e materiais | Os registros não comprovam regra, capacidade, ponto de fixação, responsável ou versão da especificação usada. | Persistir parâmetros, referência da regra e resultado por posição e conjunto. |

### BUS40 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `verificarConteinerEmCima()`, `calcularNovaPositicao()` | O bloqueio é inferido por ordem pendente na mesma linha/coluna sem comparar camada; a lista de vizinhos é ignorada e o destino é `linha+5`, `coluna+5`, `CAMADA_1`. | Consultar a pilha real, identificar a unidade bloqueadora e selecionar posição existente e elegível pela fonte do Yard. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `executarReshuffflingConteiner()` | Chama `registrarOrdem()` sem reservar destino nem guardar identidade idempotente do plano. | Reservar a posição e criar/reutilizar a ordem na mesma transação; conflito deve cancelar a tentativa e recalcular. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/OrdemTrabalhoPatioServico.java` | `registrarOrdem()`, `validarDestinoPatio()` | Para o reshuffling, `exigirPosicaoReal=false` permite coordenada inexistente. | Exigir posição real e validação de placement para todo `REMANEJAMENTO`. |

## 3. Autorização operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC20 | Restringir criação, alteração e aprovação de planos de estiva a perfis autorizados. | Escritas do Vessel Planner e da estiva de bobinas exigem planejamento ou administração; leitura segue a matriz definida, usuário sem permissão recebe `403` e o backend protege o fluxo independentemente do menu. | ⬜ Pendente |

### SEC20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/controlador/VesselPlannerControlador.java` | `/api/vessel-planner/**` | Não possui `@PreAuthorize`; qualquer autenticado pode criar, autoestivar, realocar e aprovar. | Separar leitura e comandos e limitar mutações a `ADMIN_PORTO`, `PLANEJADOR` ou matriz equivalente. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/controlador/EstivaBulkControlador.java` | `/api/estivagem-bulk/**` | Registro, criação, posicionamento, cálculo que persiste materiais e aprovação exigem apenas autenticação genérica. | Proteger comandos por role e impedir alteração de plano aprovado sem comando administrativo explícito. |
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/configuracao/ConfiguracaoSegurancaRuntime.java` | `anyRequest().authenticated()` | A cadeia central autentica, mas não autoriza operações de domínio. | Manter a cadeia única e aplicar autorização nos controllers proprietários. |

## 4. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC50 | Subordinar o reshuffling noturno ao controle canônico de jobs. | `executarReshuffflingNoturno()` só é registrado com `cloudport.runtime.jobs-enabled=true`; deployments de rollback não analisam candidatos nem criam ordens, enquanto chamadas explícitas permanecem disponíveis. | ⬜ Pendente |

### ASYNC50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `executarReshuffflingNoturno()` | O `@Scheduled` está no serviço e não consulta `cloudport.runtime.jobs-enabled`; runtime e standalone podem executar o mesmo cron. | Criar `novo método sugerido: PredictiveReshuffflingJob.executar()`, condicionado pela propriedade canônica, e manter o caso de uso sem anotação. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/ServicoYardApplication.java` e `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | `@EnableScheduling` e component scan | Ambos carregam scheduling e o serviço do Yard. | Garantir que somente a instância com jobs habilitados registre o cron, sem criar flag concorrente. |

## 5. Interface operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI40 | Substituir o Steel Coil Planner estático por uma tela alcançável e persistida. | A rota abre a tela real, carrega navio, visita, carga e plano persistidos e exibe apenas validações do backend; não há dados hardcoded, destino aleatório, física simulada ou sucesso antes da persistência. | ⬜ Pendente |

### UI40 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-autenticacao/src/main/resources/db/migration/V11__inserir_navegacao_steel_coils.sql` e `frontend/cloudport/src/App.jsx` | rota `embarque/steel-coils`, `RouteContent()` | A navegação habilita a rota, mas o React não a trata e exibe 404. | Registrar um componente operacional e alinhar as roles ao backend. |
| `frontend/servico-navio-siderurgico/src/assets/steel-coil-planner.html` | `COIL_TYPES`, `HOLDS`, `PORTS`, `dropCoil()`, `generatePlan()`, `updateStats()` | Usa navio/carga fixos, destino aleatório, plano predefinido e cálculos locais marcados como simulados, mas declara estabilidade e sucesso. | Remover a simulação do fluxo operacional e consumir contratos persistentes; o navegador não pode aprovar nem produzir indicadores de segurança. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/controlador/EstivaBulkControlador.java` e `docs/steel-coil-planner-integration.md` | API real e API documentada | O backend possui contratos parciais em `/api/estivagem-bulk`, enquanto o documento promete `/api/steel-coils/plans`, importação e exportações inexistentes. | Definir um contrato único usado pela tela e alinhar a documentação somente após o fluxo real estar conectado. |
