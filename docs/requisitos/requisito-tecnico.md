# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-16 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Arquitetura e fluxos principais

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ARCH10 | Substituir a chamada HTTP interna do otimizador do Yard por uma porta local no runtime modular. | A otimização Navio + Pátio executa no `cloudport-runtime` por chamada local ao módulo Yard; o adaptador HTTP permanece condicionado somente ao modo de rollback. | ⬜ Pendente |
| ERR10 | Impedir que falhas obrigatórias do Yard sejam convertidas em respostas locais de sucesso. | Falhas na consulta de filas ou ordens sem cobertura retornam erro tipado e status coerente; cálculo local alternativo só é usado quando um modo de contingência explícito estiver habilitado. | ⬜ Pendente |
| BUS10 | Aplicar o resultado real do otimizador no replanejamento de pátio. | O replanejamento usa o plano calculado para atualizar reservas, posições, ordens e vínculos operacionais em uma transação, compensa o estado anterior em falha e calcula ganho e risco com dados do plano. | ⬜ Pendente |

### ARCH10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/cliente/OtimizacaoYardCliente.java` | `otimizar(Map<String, Object>)` | O componente sempre usa `RestTemplate` para chamar `/api/scheduler/gerar-plano`, mesmo quando Navio Siderúrgico e Yard executam no mesmo processo. | Transformar o cliente em porta e manter esta implementação como adaptador HTTP condicionado ao modo de rollback. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/scheduler/servico/PredictiveSchedulerService.java` | `gerarPlanoOperacional(...)` | O serviço real do otimizador já está incorporado ao runtime, mas não é chamado diretamente pelo módulo consumidor. | Criar adaptador local que converta o contrato de entrada e invoque o serviço do Yard sem atravessar HTTP. |
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | composição dos módulos | O runtime incorpora os dois módulos, porém não registra uma implementação local para o contrato de otimização. | Registrar a implementação local como padrão do runtime e impedir a ativação simultânea dos adaptadores local e HTTP. |

### ERR10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/IntegracaoNavioPatioServico.java` | `listarFilasOperacionaisDaVisita()` | Qualquer `RuntimeException` da integração é capturada e substituída por filas derivadas localmente, fazendo uma indisponibilidade parecer uma resposta válida. | Propagar exceção de integração tipada e retornar `503` quando o Yard obrigatório estiver indisponível; permitir derivação local somente sob configuração explícita de contingência. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/IntegracaoNavioPatioServico.java` | `listarOrdensSemCoberturaDaVisita()` | Em falha, o método devolve uma heurística baseada em prioridade, sequência e destino, sem comprovar o estado real de cobertura no Yard. | Remover o fallback implícito e consultar a fonte real; em contingência habilitada, identificar a resposta como degradada e não como estado operacional confirmado. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/cliente/OrdemPatioYardCliente.java` | contratos de filas e cobertura | O contrato não diferencia falha obrigatória, resposta vazia legítima e modo degradado. | Definir erros e resultado degradado de forma explícita, preservando a distinção até controller e frontend. |

### BUS10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/IntegracaoNavioPatioServico.java` | `replanejarPatioDaVisita()` | O fluxo apenas troca reservas item a item; não consome o plano do otimizador e retorna ganho fixo `12` e risco derivado somente da existência de falhas. | Consumir um resultado de otimização identificável, validar todas as posições e recursos e aplicar as mudanças de forma atômica. Calcular ganho e risco a partir do plano aplicado. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/OtimizacaoGlobalNavioPatioServico.java` | `otimizar()` | O método monta e devolve um plano em transação somente leitura, mas não possui comando para aplicar ou versionar o resultado. | Persistir uma proposta versionada ou devolver um identificador imutável; criar `novo método sugerido: aplicarPlanoOtimizado()` para efetivar reservas, ordens e work queues com validação de concorrência. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/dto/ResultadoReplanejamentoPatioNavioDTO.java` | ganho e risco | Os campos aceitam valores que não correspondem a uma memória de cálculo real. | Preencher os campos com indicadores calculados e rastreáveis do plano aplicado, sem constantes artificiais. |

## 2. Persistência, contratos e estado operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| DATA10 | Validar o plano de guindastes contra work queues reais do Yard antes de persistir. | Cada `workQueueId` pertence à mesma visita, é compatível com berço e porão, está operacionalmente disponível e possui cobertura e recursos válidos; um plano inválido não substitui o plano vigente. | ⬜ Pendente |
| STATE10 | Unificar as mutações de work queue e work instruction no fluxo que usa recursos reais e a matriz oficial de estados. | Controllers e frontend não conseguem despachar por fila com equipamento textual ou sem cobertura; todas as transições passam por uma única regra, auditam motivo e preservam os vínculos com equipamento, POW, pool e plano. | ⬜ Pendente |

### DATA10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/QuayBerthCraneServico.java` | `validarAlocacao()` | `workQueueId` é validado apenas como número positivo; existência, visita, status, cobertura, equipamento e compatibilidade não são consultados. | Injetar uma porta de consulta do Yard e validar a fila real antes de aceitar cada alocação. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/QuayBerthCraneServico.java` | `salvarPlano()` e `criarEntidade()` | O plano anterior é removido e o identificador informado é persistido sem vínculo comprovado com o Yard. | Validar o comando completo antes da substituição e persistir referências somente após confirmação da fonte proprietária. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueueOperacaoServico.java` | consulta de work queue e recursos | O Yard possui os vínculos reais, mas não há contrato local específico usado pelo plano de guindastes. | Expor uma porta de leitura que retorne visita, porão, status, cobertura, POW, pool, equipamento e job list necessários à validação. |

### STATE10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueuePatioServico.java` | `atualizarEquipamento()` e `despachar()` | O fluxo legado grava equipamento como texto e o dispatch apenas exige fila ativa antes de marcar ordens como `EM_EXECUCAO`. | Delegar as mutações ao fluxo operacional que resolve `EquipamentoPatio`, verifica cobertura e aplica a matriz de transições; rejeitar dispatch sem recursos reais válidos. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueueOperacaoServico.java` | `associarRecursos()`, `transicionar()` e `MATRIZ_ESTADOS` | A implementação nova valida equipamento e estados, mas concorre com endpoints legados ainda ativos. | Tornar este serviço a única fonte de mutação e incluir nele o comando de dispatch com as mesmas garantias. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/controlador/WorkQueuePatioControlador.java` | `/pow`, `/equipamento`, `/dispatch`, `/reset`, `/cancelar` | Os contratos antigos continuam acessíveis e contornam parte das validações do fluxo novo. | Fazer os contratos compatíveis delegarem ao serviço único ou removê-los após migração dos consumidores, sem manter dois caminhos de escrita. |
| `frontend/servico-navio-siderurgico/src/api.js` | métodos de work queue e work instruction | O frontend chama somente os contratos legados para recurso, dispatch, reset e cancelamento. | Migrar para `/recursos-operacionais`, transições da matriz, drill-down e job lists por equipamento. |

## 3. Autenticação e autorização

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC10 | Conectar a autenticação por cliente da API pública à cadeia de segurança do runtime geral. | Requisições válidas a `/api/public/v1/**` autenticam por `X-CloudPort-Client-Id` e `X-CloudPort-Client-Secret` no `cloudport-runtime`; credenciais inválidas são rejeitadas e os cabeçalhos necessários são aceitos pelo CORS. | ⬜ Pendente |

### SEC10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/configuracao/ConfiguracaoSegurancaRuntime.java` | `securityFilterChain()` | A cadeia adiciona somente `InternalServiceAuthenticationFilter`; o filtro de clientes externos não é registrado antes da decisão de autorização. | Injetar `PublicApiClientAuthenticationFilter`, posicioná-lo explicitamente antes do `BearerTokenAuthenticationFilter` e autorizar a role externa somente nas rotas públicas previstas. |
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/configuracao/ConfiguracaoSegurancaRuntime.java` | `corsConfigurationSource()` | Os cabeçalhos `X-CloudPort-Client-Id` e `X-CloudPort-Client-Secret` não estão na lista permitida. | Incluir os cabeçalhos de autenticação pública e expor apenas os cabeçalhos de resposta necessários. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/configuracao/PublicApiClientAuthenticationFilter.java` | `doFilterInternal()` | O filtro implementa validação e cria `ROLE_INTEGRACAO_EXTERNA`, mas sua existência como componente não garante execução antes da cadeia central. | Manter uma única instância controlada pela configuração central e limpar o contexto de segurança ao final ou em falha conforme o ciclo do filtro. |
| `backend/cloudport-runtime/src/main/java/br/com/cloudport/runtime/CloudPortRuntimeApplication.java` | filtros excluídos do scan | As configurações standalone são excluídas, portanto a proteção declarada no módulo não substitui a integração explícita com o runtime. | Preservar a exclusão das cadeias duplicadas e registrar somente o filtro necessário na cadeia única. |

## 4. Eventos, integrações e processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC10 | Tornar os consumidores da Visibilidade idempotentes por identidade de evento. | O mesmo `eventId` ou `messageId` processado novamente não duplica histórico nem reaplica projeções; deduplicação e gravação do efeito ocorrem na mesma transação. | ⬜ Pendente |
| ASYNC20 | Tornar o processamento EDI idempotente e desacoplado da requisição HTTP. | A combinação de tipo, identificadores `UNB`/`UNH` e referência possui unicidade; a recepção é persistida antes da confirmação e o processamento ocorre por worker com retentativa segura e quarentena. | ⬜ Pendente |
| ASYNC30 | Usar eventos internos como fluxo principal de atualização Yard → Navio e Navio canônico → projeção siderúrgica. | Alterações relevantes publicam eventos no processo e atualizam os consumidores imediatamente; os jobs periódicos permanecem apenas para reconciliação de perdas, sem varrer toda a base como mecanismo principal. | ⬜ Pendente |

### ASYNC10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/listener/YardEventListener.java` | `handleYardEvent()` | O listener recebe `Map<String, Object>`, não exige identidade do evento e chama os serviços novamente em cada redelivery. | Ler e validar `eventId` ou `messageId` do envelope versionado e chamar `novo método sugerido: processarUmaVez()`. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/listener/GateEventListener.java` | consumidores de eventos de Gate | O fluxo não compartilha uma barreira persistente de deduplicação com os demais listeners. | Aplicar o mesmo contrato idempotente antes de alterar localização e histórico. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/listener/RailEventListener.java` | consumidores de eventos ferroviários | Redelivery pode reaplicar a movimentação quando não há chave persistida do evento. | Persistir a identidade e confirmar a mensagem somente após a transação do efeito. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/listener/NavioEventListener.java` | consumidores de eventos de navio | Não há registro comum de eventos já processados. | Usar o mesmo mecanismo de deduplicação e rejeitar colisão de identidade com payload divergente. |
| `backend/servico-visibilidade/src/main/java/br/com/cloudport/visibilidade/service/MovimentoConteinerService.java` | `registrarMovimento()` | Toda chamada cria um novo `HistoricoMovimento`; não recebe uma chave externa capaz de impedir duplicidade. | Receber a identidade do evento e persistir deduplicação, projeção e histórico atomicamente, com restrição única no banco. |

### ASYNC20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/modelo/ProcessamentoEdi.java` | `referenciaMensagem`, `tentativa` | A entidade armazena referência, mas não possui chave idempotente nem identificadores `UNB` e `UNH` separados. | Persistir os identificadores normalizados e uma chave idempotente imutável. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/repositorio/ProcessamentoEdiRepositorio.java` | consultas do processamento | Não existe consulta ou trava por chave natural capaz de reutilizar uma recepção já registrada. | Adicionar busca e inserção atômica por chave idempotente e tratar conflito de unicidade como redelivery, não como novo processamento. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/edi/servico/EdiAuditoriaServico.java` | `processarInterno()` | Cada chamada cria um novo registro e executa a operação de negócio de forma síncrona na mesma requisição. | Separar recepção de execução: gravar `RECEBIDO`, publicar em outbox ou fila transacional, processar por worker e mover falhas esgotadas para quarentena. |
| `backend/servico-yard/src/main/resources/db/migration/V100__edi_processamento_auditoria.sql` | tabela `edi_processamento` | A estrutura não impede mensagens equivalentes de gerar processamentos paralelos ou repetidos. | Criar migration aditiva com colunas de identidade, índice único e estado necessário à retentativa assíncrona. |

### ASYNC30 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/ReconciliacaoNavioPatioJob.java` | `reconciliarVisitasAtivas()` | A cada ciclo, o job busca todas as visitas e consulta o estado do Yard para atualizar os itens. | Consumir eventos internos de criação e mudança de ordem, reserva e movimento; manter o job apenas para reparar eventos perdidos. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/servico/SincronizacaoCadastroCanonicoJob.java` | `sincronizarCadastroCanonico()` | A projeção siderúrgica é sincronizada periodicamente, mesmo com Navio incorporado ao mesmo processo. | Publicar evento interno quando o cadastro canônico mudar e atualizar somente a projeção afetada. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/servico/WorkQueueOperacaoServico.java` | `transicionar()` e mutações de recursos | As mudanças persistem e auditam, mas não formam o fluxo interno principal de atualização do módulo Navio Siderúrgico. | Publicar eventos internos após commit com identidade e versão; consumidores devem ser idempotentes. |
| `backend/servico-navio/src/main/java/br/com/cloudport/serviconavio` | atualização do cadastro canônico | Alterações do domínio não disparam um contrato interno consumido pela projeção siderúrgica. | Publicar evento interno versionado contendo o identificador canônico e os campos alterados. |

## 5. Interface e navegação operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Expor no frontend a edição de visita e item e a conclusão do plano de estiva. | Usuário autorizado consegue editar dados pelos contratos existentes e concluir o plano após validação, com retorno de erro exibido sem atualizar a tela como sucesso. | ⬜ Pendente |
| UI20 | Tornar o Quay Monitor e o painel de equipamentos operacionais, não apenas consultivos. | O frontend salva plano de guindastes com work queues validadas, exibe drill-down de work instruction e usa job lists por equipamento e transições oficiais. | ⬜ Pendente |

### UI10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | objeto `api` | Não há métodos para `PUT /visitas-navio/{id}`, `PUT /visitas-navio/{id}/itens/{itemId}` ou `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`. | Adicionar clientes tipados para os três contratos e propagar motivo, usuário e correlação quando aplicável. |
| `frontend/servico-navio-siderurgico/src/AdvancedApp.jsx` | fluxo de visita, itens e plano | A interface permite ações operacionais de pátio, mas não oferece formulários de edição nem publicação do plano. | Criar formulários com estado de carregamento, validação do contrato, confirmação de conclusão e recarga somente após resposta persistida. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/VisitaNavioControlador.java` | `atualizar()`, `atualizarItem()` e `concluirPlano()` | Os endpoints existem e são alcançáveis no backend, porém não possuem consumidor na interface operacional. | Preservar os contratos e garantir que o frontend use os identificadores retornados, sem criar rota concorrente. |

### UI20 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/servico-navio-siderurgico/src/api.js` | `obterQuayMonitor()` e métodos de work queue | O Quay Monitor é somente leitura e as operações de fila usam os endpoints legados. | Adicionar gravação de `POST /visitas-navio/{id}/crane-plan`, consulta do plano, recursos operacionais, drill-down, matriz de estados e job lists por equipamento. |
| `frontend/servico-navio-siderurgico/src/AdvancedApp.jsx` | `QuayMonitor`, `CheDetail` e `WorkQueue` | Os componentes exibem dados, mas não conectam o plano de guindastes nem os controles novos do Yard ao fluxo real. | Permitir selecionar work queue válida por alocação, salvar o plano, abrir drill-down e executar somente transições permitidas pelo backend. |
| `backend/servico-navio-siderurgico/src/main/java/br/com/cloudport/serviconaviosiderurgico/controlador/QuayBerthCraneControlador.java` | contratos de plano e monitor | O backend possui leitura e gravação, mas a gravação não é usada pela interface. | Manter uma única rota de gravação e retornar os dados necessários para atualização imediata do monitor. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/listatrabalho/controlador/WorkQueueOperacaoControlador.java` | `/recursos-operacionais`, `/drill-down`, `/matriz-estados` e `/equipamentos/job-lists` | Os contratos robustos existem, mas não são consumidos pelo Control Room. | Fazer a interface utilizar esses contratos como fonte de verdade para ações e detalhamento. |
