# Requisitos pendentes - CloudPort

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Não criar outros arquivos de requisito, relatórios de execução, históricos ou rascunhos nesta pasta. Itens concluídos devem sair deste arquivo e ser registrados em `docs/implementados/requisitos-implementados.md`.

Antes de desenvolver, ler os dois arquivos. Depois de desenvolver, remover daqui o que foi entregue, registrar a entrega no arquivo de implementados e acrescentar novas lacunas encontradas em APIs, telas, contratos, testes, observabilidade e memórias de cálculo.

## Diretriz arquitetural vigente

O backend alvo do CloudPort é um **monólito modular**. Não criar novos microsserviços para funcionalidades internas nem ampliar chamadas HTTP entre módulos que já executam no mesmo processo.

O runtime `backend/cloudport-monolito-navio` é o primeiro corte consolidado e atualmente incorpora Navio e Navio Siderúrgico. Yard, Gate, Rail, Autenticação e Visibilidade continuam como deployments legados durante a migração incremental.

As regras, fases, critérios de corte e rollback estão em `docs/arquitetura-monolito-modular.md`.

## Pendências de integração Back x Front

1. Expor no frontend a conclusão/publicação do plano por `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`.
2. Completar edição de visita e item pelos contratos `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`.
3. Criar cancelamento administrativo diferenciado para visita e item.
4. Criar tela de diagnóstico dos contratos `/api/public/v1/*`.
5. Integrar o motor real de otimização ao endpoint de replanejamento da visita.
6. Separar em `/filas` e `/sem-cobertura` as causas sem fila, sem POW, sem equipamento e sem job list.
7. Evoluir o relatório integrado com produtividade, divergências detalhadas, planejado x realizado e exportação.

## Pendências do Control Room

1. Substituir o polling de 30 segundos por SSE ou WebSocket. O carregamento atual já é paralelo, atômico e protegido contra sobreposição, mas continua baseado em polling.
2. Criar drill-down da work instruction com eventos, auditoria, divergências, reserva, item de navio e movimento de pátio.
3. Diferenciar visualmente sem fila, sem POW, sem equipamento, sem job list, posição inválida, reserva bloqueada e divergência Navio x Pátio.
4. Criar painel de CHE/job list por equipamento.
5. Criar Quay Monitor quando os contratos de berth/crane estiverem disponíveis.
6. Padronizar no backend os erros com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp. O frontend já exibe esses campos quando recebidos.
7. Criar e2e para login/SSO, job list, dispatch, reset, cancelamento e indisponibilidade do Yard.

## Pendências de contratos compartilhados

1. Padronizar paginação para listas grandes.
2. Padronizar enums de visita, item, ordem, reserva, work queue, severidade e alerta.
3. Tornar `motivo` obrigatório nos comandos de cancelamento, suspensão, retomada, reset e alterações administrativas.
4. Gerar tipos TypeScript a partir de OpenAPI.
5. Centralizar conversão de `WorkQueuePatioYardDTO`.
6. Separar DTO resumido de lista e DTO detalhado de job list.
7. Definir contrato versionado de evento para SSE/WebSocket.
8. Publicar um OpenAPI consolidado no runtime monolítico sem duplicação de operação, schema ou configuração.

## Pendências da migração para monólito modular

O runtime `backend/cloudport-monolito-navio` já executa `servico-navio` e `servico-navio-siderurgico` no mesmo processo, consome os dois como módulos Maven reais, carrega as migrações publicadas pelos próprios artefatos, usa a porta local do cadastro canônico, migra separadamente os dois schemas, possui segurança centralizada e inicia com PostgreSQL real em teste cobrindo todos os repositórios JPA.

Ainda falta:

1. Atualizar Docker Compose, configurações de ambiente, proxy e roteamento para substituir os deployments de Navio e Navio Siderúrgico pelo runtime unificado.
2. Validar paridade de endpoints, jobs agendados, segurança, frontend, integrações e dados antes de retirar os deployments legados.
3. Garantir que somente uma instância execute cada job, consumidor e caminho de escrita durante o corte.
4. Substituir as chamadas HTTP restantes entre Navio e Navio Siderúrgico por portas ou eventos internos.
5. Criar testes de arquitetura que impeçam dependências cíclicas, acesso direto a repositories/entidades de outro módulo e clientes HTTP entre módulos incorporados.
6. Incorporar o Yard ao runtime e substituir a integração HTTP Navio -> Yard por portas locais, preservando os contratos REST externos.
7. Incorporar Gate e Rail como módulos, usando o Yard por interfaces internas e mantendo TOS, OCR, EDI e mensageria como adaptadores externos.
8. Incorporar Autenticação e Visibilidade, centralizando emissão de token, OpenAPI, erros, logs, métricas e tracing.
9. Definir ownership de tabelas e schemas para todos os módulos antes de consolidar a conexão PostgreSQL.
10. Definir estratégia de rollback e compatibilidade dos históricos Flyway antes de apontar ambientes existentes para cada novo runtime consolidado.
11. Centralizar versões e `pluginManagement` em um parent Maven compartilhado, sem criar dependências cíclicas.
12. Centralizar configurações de segurança, CORS, Jackson, tratamento de erros, observabilidade e agendamento no runtime.
13. Remover clientes HTTP, credenciais internas, imagens, deployments e variáveis legadas somente depois da conclusão de cada corte.
14. Renomear diretórios e artefatos `servico-*` somente quando não houver impacto em pipelines, imagens, imports ou rollback.
15. Evoluir `cloudport-monolito-navio` para um runtime geral do CloudPort ou criar o runtime geral antes de incorporar domínios não relacionados a Navio.

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

Já entregue: vínculo persistente `workQueueId`, endpoint `PATCH /yard/patio/work-queues/{id}/ordens`, auditoria de criação/status/POW/equipamento/vínculo/dispatch/reset/cancelamento e limite real no dispatch.

Ainda falta:

1. Auditar suspensão, retomada, bloqueio e conclusão.
2. Associar work queue a porão, plano de guindaste e recurso de cais.
3. Associar fila a CHE real.
4. Auditar prioridade de fetch/busca separadamente da prioridade operacional.
5. Criar matriz oficial de transição de work instruction.
6. Expor painel de job list por equipamento e drill-down completo.

### 4. Replanejamento real

O scheduler não gera mais equipamentos, contêineres ou coordenadas aleatórias e exige dados operacionais reais. Falta conectar esse contrato ao replanejamento da visita, considerando ETA, ETB, ETD, cutoff, mapa, bloqueios, capacidade, dual-cycling e rehandle.

### 5. Quay/berth/crane

```text
GET  /visitas-navio/{id}/quay-monitor
POST /visitas-navio/{id}/crane-plan
GET  /visitas-navio/{id}/produtividade-cais
```

### 6. Contratos externos e EDI

1. Proteger `/api/public/v1` por client/app.
2. Implementar filtros, paginação, campos selecionáveis, `correlationId`, erro padronizado e OpenAPI.
3. Implementar eventos externos versionados de visita, estiva, reserva, ordem, movimento e work queue.
4. Completar BAPLIE, COPRAR, COARRI e VERMAS com validação, rejeição, reprocessamento e auditoria.
5. Separar eventos internos do monólito de eventos publicados para integrações externas.

### 7. Testes e observabilidade

1. Testar o proxy de work queues com sucesso, retorno vazio legítimo e falha do Yard convertida em `503` enquanto o Yard permanecer externo.
2. Criar testes de contrato entre os módulos Navio Siderúrgico, Yard e Navio, cobrindo chamada local no monólito e adaptadores HTTP legados com `X-CloudPort-Service-Key`.
3. Testar vínculo `workQueueId`, limite de dispatch, auditoria e autorização por perfil.
4. Testar reserva contra mapa real: inexistente, ocupada, já reservada e mapa vazio.
5. Criar e2e do fluxo operacional completo.
6. Adicionar logs estruturados, métricas e tracing com módulo, visita, item, reserva, ordem, work queue e `correlationId`.
7. Criar testes que comprovem que jobs e consumidores não executam em duplicidade durante o corte para o monólito.
8. Validar o OpenAPI consolidado e ausência de rotas duplicadas.

## P1

1. Relatórios operacionais e exportação CSV/PDF.
2. Completar permissões e auditoria de reservas, ordens, replanejamento, sincronização e prioridades.
3. Padronizar status entre Navio, Pátio, work queue e alertas.
4. Substituir a sincronização periódica da projeção siderúrgica do cadastro canônico por evento interno.
5. Criar matriz de dependências permitidas entre todos os módulos do monólito.

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
5. Integrar quay/berth/crane às filas e ordens.
6. Padronizar, versionar, paginar e proteger contratos externos.
7. Cobrir o fluxo por testes de service, controller, contrato e frontend.
8. Rastrear o fluxo por logs, métricas e tracing.
9. Exigir motivo e usuário autenticado nas ações aplicáveis.
10. Diferenciar fila derivada, work queue persistente, work instruction, job list e exceção operacional.
11. Manter uma única origem de API para o frontend após cada corte.
12. Garantir que módulos incorporados não realizem chamadas HTTP entre si.
13. Retirar um deployment legado somente após paridade, dados, segurança, observabilidade e rollback validados.
14. Executar cada job, consumidor e comando de escrita em uma única instância durante a transição.

## Fora do escopo deste corte

1. Telemetria real de equipamentos.
2. Dispatch direto para VMT real.
3. Motor matemático global multi-recurso.
4. Controle aduaneiro/documental completo.
5. Substituição integral de um TOS comercial.
