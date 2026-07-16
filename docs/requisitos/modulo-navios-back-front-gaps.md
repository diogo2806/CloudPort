# Requisitos pendentes - CloudPort

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Não criar outros arquivos de requisito, relatórios de execução, históricos ou rascunhos nesta pasta. Itens concluídos devem sair deste arquivo e ser registrados em `docs/implementados/requisitos-implementados.md`.

Antes de desenvolver, ler os dois arquivos. Depois de desenvolver, remover daqui o que foi entregue, registrar a entrega no arquivo de implementados e acrescentar novas lacunas encontradas em APIs, telas, contratos, testes, observabilidade e memórias de cálculo.

## Diretriz arquitetural vigente

O backend alvo do CloudPort é um **monólito modular**. Não criar novos microsserviços para funcionalidades internas nem ampliar chamadas HTTP entre módulos que já executam no mesmo processo.

O runtime `backend/cloudport-monolito-navio` é o primeiro corte consolidado e atualmente incorpora Navio e Navio Siderúrgico. Yard, Gate, Rail, Autenticação e Visibilidade continuam como deployments legados durante a migração incremental.

As regras, fases, critérios de corte e rollback estão em `docs/arquitetura-monolito-modular.md` e `docs/operacao-corte-rollback-navio.md`.

## Pendências de integração Back x Front

1. Expor no frontend a conclusão/publicação do plano por `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`.
2. Completar edição de visita e item pelos contratos `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`.
3. Criar cancelamento administrativo diferenciado para visita e item.
4. Criar tela de diagnóstico dos contratos `/api/public/v1/*`.
5. Integrar o motor real de otimização ao endpoint de replanejamento da visita.
6. Separar em `/filas` e `/sem-cobertura` as causas sem fila, sem POW, sem equipamento e sem job list.
7. Evoluir o relatório integrado com produtividade, divergências detalhadas, planejado x realizado e exportação.
8. Consumir no frontend os contratos de quay/berth/crane, permitindo consultar o monitor, editar/publicar o plano e acompanhar a produtividade do cais.

## Pendências do Control Room

1. Substituir o polling de 30 segundos por SSE ou WebSocket. O carregamento atual já é paralelo, atômico e protegido contra sobreposição, mas continua baseado em polling.
2. Criar drill-down da work instruction com eventos, auditoria, divergências, reserva, item de navio e movimento de pátio.
3. Diferenciar visualmente sem fila, sem POW, sem equipamento, sem job list, posição inválida, reserva bloqueada e divergência Navio x Pátio.
4. Criar painel de CHE/job list por equipamento.
5. Criar a tela de Quay Monitor consumindo os contratos de berth/crane, com linha do tempo, alertas, progresso, MPH e ETC por guindaste.
6. Expandir para os demais backends o contrato de erro com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp já aplicado no `servico-visibilidade`.
7. Criar e2e para login/SSO, job list, dispatch, reset, cancelamento e indisponibilidade do Yard.

## Pendências de contratos compartilhados

1. Padronizar paginação para listas grandes.
2. Padronizar enums de visita, item, ordem, reserva, work queue, severidade e alerta.
3. Tornar `motivo` obrigatório nos comandos de cancelamento, suspensão, retomada, reset e alterações administrativas. A resolução de alerta da Visibilidade já exige motivo.
4. Gerar tipos TypeScript a partir de OpenAPI.
5. Centralizar conversão de `WorkQueuePatioYardDTO`.
6. Separar DTO resumido de lista e DTO detalhado de job list.
7. Definir contrato versionado de evento para SSE/WebSocket.
8. Publicar um OpenAPI consolidado no runtime monolítico sem duplicação de operação, schema ou configuração.

## Pendências do módulo de Visibilidade

1. Persistir `eventId` ou `messageId` e impedir duplicação de histórico em redelivery do RabbitMQ.
2. Substituir buscas com `repository.findAll()` e filtros em memória por consultas paginadas no banco.
3. Publicar atualização do dashboard imediatamente após eventos de gate, pátio, rail e navio, mantendo o agendamento apenas como reconciliação.
4. Persistir quantidades reais de movimentos, produtividade, equipamentos alocados e previsão de saída antes de preencher esses campos nos contratos.
5. Integrar a Visibilidade ao contrato compartilhado de enums e eventos versionados.
6. Criar teste de contexto com PostgreSQL, RabbitMQ e mapeamentos Spring reais, além dos testes unitários atuais.
7. Preparar o módulo para incorporação ao runtime monolítico sem manter segurança, tratamento de erros e agendamento duplicados.

## Pendências da migração para monólito modular

O corte Navio + Navio Siderúrgico já possui paridade estrutural de controllers, segurança única, integração local do cadastro canônico, validação dos dados e históricos Flyway, bloqueio distribuído dos jobs, deployments legados em modo somente leitura, testes de arquitetura e runbook de corte/rollback. O smoke cobre inicialização, frontend, configuração dinâmica, autenticação, persistência e integração autenticada com o Yard.

Ainda falta:

1. Incorporar o Yard ao runtime e substituir a integração HTTP Navio -> Yard por portas locais, preservando os contratos REST externos.
2. Incorporar Gate e Rail como módulos, usando o Yard por interfaces internas e mantendo TOS, OCR, EDI e mensageria como adaptadores externos.
3. Incorporar Autenticação e Visibilidade, centralizando emissão de token, OpenAPI, erros, logs, métricas e tracing.
4. Definir ownership de tabelas e schemas para todos os módulos antes de consolidar a conexão PostgreSQL.
5. Centralizar versões e `pluginManagement` em um parent Maven compartilhado, sem criar dependências cíclicas.
6. Centralizar configurações de segurança, CORS, Jackson, tratamento de erros, observabilidade e agendamento no runtime geral.
7. Remover clientes HTTP, credenciais internas, imagens, deployments e variáveis legadas somente depois da conclusão de cada corte.
8. Renomear diretórios e artefatos `servico-*` somente quando não houver impacto em pipelines, imagens, imports ou rollback.
9. Evoluir `cloudport-monolito-navio` para um runtime geral do CloudPort ou criar o runtime geral antes de incorporar domínios não relacionados a Navio.

## P0 - Pendências obrigatórias restantes

### 1. Eventos Pátio -> Navio

A reconciliação automática e idempotente por job já atualiza item, posição real e reserva. Falta substituir a consulta periódica por callback, evento interno ou fila externa conforme o estágio da migração, reduzindo latência sem introduzir novo microsserviço.

Eventos alvo:

```text
OrdemPatioCriada
StatusOrdemPatioAlterado
ReservaPatioConsumida
ReservaPatioCancelada
MovimentoPatioConfirmado
DivergenciaNavioPatioDetectada
```

### 2. Fechar o ciclo de reserva no mapa real

Já entregue: consulta de posições reais, rejeição de posição inexistente ou ocupada, rejeição de reserva ativa duplicada, remoção de posição textual artificial, consumo ao concluir ordem e cancelamento ao cancelar ordem.

Ainda falta:

1. Validar bloqueio, interdição e área permitida.
2. Validar tipo de carga, peso, altura/camada e capacidade da pilha.
3. Expirar reserva por prazo configurável.
4. Cancelar reserva ao cancelar visita ou replanejar item.
5. Persistir auditoria específica de reserva criada, consumida, cancelada e expirada.
6. Aplicar replanejamento com reserva nova e compensação da reserva anterior.

### 3. Work queues e cobertura operacional

Já entregue: vínculo persistente `workQueueId`, endpoint `PATCH /yard/patio/work-queues/{id}/ordens`, auditoria de criação/status/POW/equipamento/vínculo/dispatch/reset/cancelamento e limite real no dispatch. O plano de guindastes também persiste porão, recurso de cais e `workQueueId` por alocação.

Ainda falta:

1. Auditar suspensão, retomada, bloqueio e conclusão.
2. Validar no Yard a existência, cobertura e compatibilidade da work queue informada no plano de guindastes.
3. Associar fila a CHE real.
4. Auditar prioridade de fetch/busca separadamente da prioridade operacional.
5. Criar matriz oficial de transição de work instruction.
6. Expor painel de job list por equipamento e drill-down completo.

### 4. Replanejamento real

O scheduler não gera mais equipamentos, contêineres ou coordenadas aleatórias e exige dados operacionais reais. Falta conectar esse contrato ao replanejamento da visita, considerando ETA, ETB, ETD, cutoff, mapa, bloqueios, capacidade, dual-cycling e rehandle.

### 5. Contratos externos e EDI

1. Proteger `/api/public/v1` por client/app.
2. Implementar filtros, paginação, campos selecionáveis, `correlationId`, erro padronizado e OpenAPI.
3. Implementar eventos externos versionados de visita, estiva, reserva, ordem, movimento e work queue.
4. Completar BAPLIE, COPRAR, COARRI e VERMAS com validação, rejeição, reprocessamento e auditoria.
5. Separar eventos internos do monólito de eventos publicados para integrações externas.

### 6. Testes e observabilidade

1. Testar o proxy de work queues com sucesso, retorno vazio legítimo e falha do Yard convertida em `503` enquanto o Yard permanecer externo.
2. Criar testes de contrato entre os módulos Navio Siderúrgico, Yard e Navio, cobrindo chamada local no monólito e adaptadores HTTP legados com `X-CloudPort-Service-Key`.
3. Testar vínculo `workQueueId`, limite de dispatch, auditoria e autorização por perfil.
4. Testar reserva contra mapa real: inexistente, ocupada, já reservada e mapa vazio.
5. Criar e2e do fluxo operacional completo.
6. Adicionar logs estruturados, métricas e tracing com módulo, visita, item, reserva, ordem, work queue e `correlationId`.
7. Validar o OpenAPI consolidado e ausência de rotas duplicadas.
8. Criar teste de contexto da Visibilidade com PostgreSQL, RabbitMQ e todos os mapeamentos de controller.
9. Criar testes de integração do crane plan com work queues reais do Yard e testes frontend do Quay Monitor.

## P1

1. Relatórios operacionais e exportação CSV/PDF.
2. Completar permissões e auditoria de reservas, ordens, replanejamento, sincronização e prioridades.
3. Padronizar status entre Navio, Pátio, work queue e alertas.
4. Substituir a sincronização periódica da projeção siderúrgica do cadastro canônico por evento interno.
5. Criar matriz de dependências permitidas entre todos os módulos do monólito.
6. Concluir idempotência, consultas paginadas e publicação orientada a eventos no módulo de Visibilidade.

## P2

1. Integração EDI operacional atualizando reservas e ordens automaticamente.
2. Otimização global Navio + Pátio + Equipamento.
3. Comparação automática entre estiva, pátio e execução.
4. Previsão de gargalos por berço, porão, bloco, fila e equipamento.
5. Control Room completo com yard view, vessel view, CHE detail, alerts e quay monitor.
6. Telemetria/VMT real.
7. Lashing, estabilidade, segregação e restrições estruturais.
8. EVP/event streaming versionado.

## Critérios de aceite pendentes

1. Impedir reserva bloqueada, sem capacidade ou incompatível com a carga.
2. Expirar e auditar reservas automaticamente.
3. Replanejar usando mapa e otimização real.
4. Atualizar o Control Room por eventos, sem polling.
5. Validar quay/berth/crane contra work queues, ordens e recursos reais do Yard.
6. Padronizar, versionar, paginar e proteger contratos externos.
7. Cobrir o fluxo por testes de service, controller, contrato e frontend.
8. Rastrear o fluxo por logs, métricas e tracing.
9. Exigir motivo e usuário autenticado nas ações aplicáveis.
10. Diferenciar fila derivada, work queue persistente, work instruction, job list e exceção operacional.
11. Manter uma única origem de API para o frontend após cada corte.
12. Garantir que módulos incorporados não realizem chamadas HTTP entre si em cada novo corte.
13. Retirar um deployment legado somente após paridade, dados, segurança, observabilidade e rollback validados.
14. Executar cada job, consumidor e comando de escrita em uma única instância durante cada novo corte.

## Fora do escopo deste corte

1. Telemetria real de equipamentos.
2. Dispatch direto para VMT real.
3. Motor matemático global multi-recurso.
4. Controle aduaneiro/documental completo.
5. Substituição integral de um TOS comercial.
