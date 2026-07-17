# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-17 após implementação do requisito `SEC20`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Fontes de verdade e dados de estiva

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ARCH20 | Vincular os planejadores de estiva ao navio e à visita canônicos. | Planos de contêineres e bobinas persistem os identificadores canônicos de navio e visita; IMO, nome e viagem não possuem cadastro concorrente, e perfis estruturais ficam vinculados e versionados sob a mesma identidade. | ⬜ Pendente |
| DATA30 | Substituir a malha artificial do Vessel Planner pela geometria real do navio. | O plano usa bays, rows, tiers, hatch covers, slots restritos, tomadas reefer e limites de pilha do perfil versionado; posições do Bay Plan são preservadas e a operação é bloqueada quando o perfil estiver ausente ou incompatível. | ⬜ Pendente |
| INT20 | Preservar os atributos operacionais e de segurança recebidos no BAPLIE. | Navio e viagem são validados sem identificadores sintéticos; pesos são normalizados por unidade; posição, operação, cheio/vazio, VGM, reefer, carga perigosa e OOG são persistidos em campos próprios e chegam ao planejador sem inferência textual. | ⬜ Pendente |

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

## 3. Autenticação e autorização

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC30 | Eliminar credenciais funcionais e segredos criptográficos padrão dos serviços standalone. | Serviços que validam JWT ou autenticam clientes públicos falham na inicialização quando os segredos obrigatórios não forem fornecidos; nenhuma credencial conhecida do repositório habilita acesso, assinatura ou validação em runtime. | ⬜ Pendente |

### SEC30 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/resources/application.properties` | `cloudport.security.jwt.secret` | O serviço standalone usa por padrão `chave-local-para-desenvolvimento-123456`, que possui tamanho aceito pelo decoder e é conhecido por qualquer pessoa com acesso ao repositório. | Remover o fallback e exigir segredo externo com validação de presença e tamanho antes de expor endpoints autenticados. |
| `backend/servico-navio-siderurgico/src/main/resources/application.properties` e `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/configuracao/PublicApiClientAuthenticationFilter.java` | `cloudport.security.public-api.clients`, `carregarClientes()` | A configuração padrão `cloudport-local:troque-esta-chave-publica` é carregada como cliente válido e concede `ROLE_INTEGRACAO_EXTERNA` para `/api/public/v1/**`. | Remover o cliente funcional padrão, exigir configuração externa e falhar fechado sem credenciais válidas. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/configuracao/ConfiguracaoSeguranca.java` | `jwtDecoder()` e cadeia `/api/public/v1/**` | O decoder valida apenas presença e 32 bytes; portanto aceita o segredo conhecido, enquanto o filtro autentica o cliente padrão antes do `BearerTokenAuthenticationFilter`. | Rejeitar valores sentinela de desenvolvimento e garantir que o profile operacional não inicialize com credenciais documentadas ou defaults reutilizáveis. |

## 4. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC50 | Subordinar o reshuffling noturno ao controle canônico de jobs. | `executarReshuffflingNoturno()` só é registrado com `cloudport.runtime.jobs-enabled=true`; deployments de rollback não analisam candidatos nem criam ordens, enquanto chamadas explícitas permanecem disponíveis. | ⬜ Pendente |

### ASYNC50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/otimizacao/PredictiveReshuffflingServico.java` | `executarReshuffflingNoturno()` | O `@Scheduled` está no serviço e não consulta `cloudport.runtime.jobs-enabled`; runtime e standalone podem executar o mesmo cron. | Criar `novo método sugerido: PredictiveReshuffflingJob.executar()`, condicionado pela propriedade canônica, e manter o caso de uso sem anotação. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/ServicoYardApplication.java` e `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | `@EnableScheduling` e component scan | Ambos carregam scheduling e o serviço do Yard. | Garantir que somente a instância com jobs habilitados registre o cron, sem criar flag concorrente. |

## 5. Interface operacional React

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI40 | Implementar o planejamento operacional React do navio siderúrgico. | A rota de steel coils permite selecionar navio e visita, criar ou carregar plano, manter manifesto de bobinas, posicionar carga por porão, consultar tank top, empilhamento, estabilidade e securing, validar e abrir relatório; toda confirmação vem do backend e o estado é recarregado após persistência. | ⬜ Pendente |
| UI50 | Transformar o módulo Pátio em telas React operacionais, e não apenas consultas genéricas. | Mapa, posições, lista de trabalho, movimentações, recursos, indicadores e automação possuem telas próprias; o operador navega pela estrutura real do pátio, consulta detalhes e executa somente comandos autorizados, com motivo quando exigido e sucesso apenas após confirmação persistida. | ⬜ Pendente |

### UI40 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/App.jsx` e `frontend/cloudport/src/pages/OperationalPages.jsx` | rota `/home/embarque/steel-coils`, `DATASET_ROUTES` | A rota é alcançável, mas abre somente `GenericDatasetPage` com a mesma listagem de escalas do embarque; não existe planejador siderúrgico no portal principal. | Criar `novo componente sugerido: SteelCoilPlannerPage`, registrar a rota antes do fallback genérico e manter seleção explícita de navio, visita e plano. |
| `frontend/cloudport/src/api.js` | `listarEscalasEmbarque()` e objeto `api` | O portal não possui chamadas para `/api/estivagem-bulk`; não cria, carrega ou altera plano e não consulta as análises existentes. | Adicionar contratos para navios e templates, planos, manifesto, posicionamento, tank top, empilhamento, estabilidade, tacktop, validação e relatório, preservando erro e `correlationId`. |
| `frontend/servico-navio-siderurgico/src/assets/steel-coil-planner.html` | `COIL_TYPES`, `HOLDS`, `PORTS`, `dropCoil()`, `generatePlan()`, `updateStats()` | O HTML legado mantém dados fixos, destino aleatório, plano predefinido e cálculos simulados no navegador. | Retirar o HTML do fluxo operacional; a interface React deve renderizar somente dados persistidos e resultados calculados pelo servidor. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/controlador/EstivaBulkControlador.java` | `/api/estivagem-bulk/**` | Existem contratos parciais para criar e consultar plano, adicionar e posicionar bobina, analisar e validar, mas nenhum deles é consumido pelo portal React. | Definir DTOs estáveis para a tela, completar as consultas necessárias e impedir que o frontend reproduza regra de domínio ou cálculo de segurança. |

### UI50 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/cloudport/src/App.jsx` e `frontend/cloudport/src/pages/OperationalPages.jsx` | rotas `/home/patio/**`, `DATASET_ROUTES`, `GenericDatasetPage` | Posições, movimentações, lista de trabalho, recursos, indicadores e automação são tabelas genéricas; lista de trabalho reutiliza a consulta de movimentações e indicadores/automação reutilizam o mapa. | Criar páginas React próprias para cada fluxo, com filtros, paginação, seleção, detalhe, estados vazios e ações coerentes com o domínio. |
| `frontend/cloudport/src/pages/OperationalPages.jsx` | `YardMapPage()` | O mapa atual exibe duas tabelas de contêineres e equipamentos; não representa blocos ou zonas, linhas, colunas, camadas, ocupação, reserva, bloqueio e interdição como estrutura navegável. | Implementar visualização operacional baseada nas posições reais, permitindo abrir pilha, unidade, equipamento, restrições e reservas sem inventar coordenadas. |
| `frontend/cloudport/src/api.js` | `obterMapaPatio()`, `listarPosicoesPatio()`, `listarMovimentacoesPatio()`, `listarConteineresPatio()`, `listarRecursosPatio()` | O portal principal expõe somente leituras simples do Yard e não possui contratos para work queues, work instructions, reservas, placement, remanejamento ou reshuffling. | Adicionar métodos para consultas e comandos do Pátio, enviar motivo e identidade operacional quando exigidos e recarregar o estado persistido depois de cada comando. |
| `frontend/servico-navio-siderurgico/src/Ui20ControlRoom.jsx` e `frontend/cloudport/src/pages/OperationalPages.jsx` | ações do Control Room e módulo Pátio | Parte das operações de Yard existe apenas dentro do Control Room incorporado por `iframe`, enquanto as rotas próprias do Pátio permanecem somente leitura. | Reutilizar contratos e componentes compartilháveis ou expor fluxos equivalentes nas páginas do Pátio, sem duplicar regra e sem manter comportamentos divergentes. |
