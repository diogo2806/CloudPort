# Requisitos pendentes - CloudPort

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Não criar outros arquivos de requisito, relatórios de execução, históricos ou rascunhos nesta pasta. Itens concluídos devem sair deste arquivo e ser registrados em `docs/implementados/requisitos-implementados.md`.

Antes de desenvolver, ler os dois arquivos. Depois de desenvolver, remover daqui o que foi entregue, registrar a entrega no arquivo de implementados e acrescentar novas lacunas encontradas em APIs, telas, contratos, testes, observabilidade e memórias de cálculo.

## Diretriz arquitetural vigente

O backend alvo do CloudPort é um **monólito modular**. Não criar novos microsserviços para funcionalidades internas nem ampliar chamadas HTTP entre módulos que já executam no mesmo processo.

O runtime geral `backend/cloudport-runtime` incorpora Autenticação, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico. O runtime `backend/cloudport-monolito-navio` permanece apenas como primeiro corte e alternativa de rollback durante a retirada controlada dos deployments antigos.

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

1. Consumir o SSE/WebSocket versionado já exposto pelo backend, remover o polling de 30 segundos como mecanismo principal e manter reconexão com fallback de reconciliação.
2. Criar drill-down da work instruction com eventos, auditoria, divergências, reserva, item de navio e movimento de pátio.
3. Diferenciar visualmente sem fila, sem POW, sem equipamento, sem job list, posição inválida, reserva bloqueada e divergência Navio x Pátio.
4. Criar painel de CHE/job list por equipamento.
5. Criar a tela de Quay Monitor consumindo os contratos de berth/crane, com linha do tempo, alertas, progresso, MPH e ETC por guindaste.
6. Expandir para os demais backends o contrato de erro com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp já aplicado no `servico-visibilidade` e no módulo Navio Siderúrgico.
7. Criar e2e para login/SSO, job list, dispatch, reset, cancelamento, motivo obrigatório e indisponibilidade do Yard.

## Pendências de contratos compartilhados

1. Adotar `backend/cloudport-contracts` nos módulos Gate, Rail, Autenticação, Visibilidade e Yard após a incorporação incremental ao monólito.
2. Substituir enums locais equivalentes pelos enums compartilhados sem quebrar compatibilidade dos contratos externos.
3. Gerar os tipos TypeScript no pipeline a partir do OpenAPI publicado, comparando o snapshot para detectar quebra de contrato.
4. Adicionar escopos, rotação de segredo, expiração, rate limit e auditoria por cliente ou aplicação da API pública.
5. Validar automaticamente no CI a ausência de `operationId`, rota e schema duplicados no OpenAPI consolidado.

## Pendências do módulo de Visibilidade

1. Persistir `eventId` ou `messageId` e impedir duplicação de histórico em redelivery do RabbitMQ.
2. Substituir buscas com `repository.findAll()` e filtros em memória por consultas paginadas no banco.
3. Publicar atualização do dashboard imediatamente após eventos de gate, pátio, rail e navio, mantendo o agendamento apenas como reconciliação.
4. Persistir quantidades reais de movimentos, produtividade, equipamentos alocados e previsão de saída antes de preencher esses campos nos contratos.
5. Integrar a Visibilidade ao contrato compartilhado de enums e eventos versionados.
6. Criar teste de contexto com RabbitMQ e Redis reais, incluindo redelivery, idempotência e todos os listeners, além do contexto PostgreSQL já coberto pelo runtime geral.

## Pendências da migração para monólito modular

O runtime geral já carrega os sete módulos no mesmo processo, preserva schemas e históricos Flyway independentes, centraliza segurança, CORS, cache, OpenAPI e conexão PostgreSQL e mantém TOS, OCR, EDI, RabbitMQ, Redis e storage como adaptadores externos. As integrações Navio Siderúrgico -> Navio, Navio -> Yard, Gate -> Autenticação e Gate -> status do Yard são locais no runtime geral.

Ainda falta:

1. Executar o corte operacional dos ambientes, mantendo uma única instância escritora e uma única instância de cada job e consumidor antes de desligar os deployments antigos.
2. Validar paridade e e2e de todos os endpoints de Gate, Rail, Autenticação, Visibilidade e Yard no runtime geral, incluindo falhas dos adaptadores externos.
3. Auditar e substituir qualquer chamada HTTP interna remanescente encontrada entre módulos incorporados, preservando HTTP somente na borda.
4. Centralizar tratamento de erros, Jackson, logs, métricas, tracing e políticas de agendamento que ainda permaneçam definidos nos módulos.
5. Centralizar versões e `pluginManagement` em um parent Maven compartilhado, sem criar dependências cíclicas.
6. Remover credenciais internas, imagens, deployments e variáveis legadas somente depois do corte e rollback do runtime geral serem testados no ambiente.
7. Renomear diretórios e artefatos `servico-*` somente quando não houver impacto em pipelines, imports ou rollback.
8. Criar smoke completo do runtime geral com PostgreSQL, RabbitMQ, Redis, autenticação, Gate, Rail, Visibilidade, Yard, Navio e integrações externas simuladas.

## P0 - Pendências obrigatórias restantes

### 1. Eventos Pátio -> Navio

A reconciliação automática e idempotente por job já atualiza item, posição real e reserva. Falta substituir a consulta periódica por evento interno no runtime geral e por evento externo versionado apenas quando atravessar a fronteira da aplicação.

Eventos alvo:

```text
OrdemPatioCriada
StatusOrdemPatioAlterado
ReservaPatioConsumida
ReservaPatioCancelada
MovimentoPatioConfirmado
DivergenciaNavioPatioDetectada
```

### 2. Work queues e cobertura operacional

Já entregue: vínculo persistente `workQueueId`, endpoint `PATCH /yard/patio/work-queues/{id}/ordens`, auditoria de criação/status/POW/equipamento/vínculo/dispatch/reset/cancelamento, motivo obrigatório nas alterações administrativas e limite real no dispatch. O plano de guindastes também persiste porão, recurso de cais e `workQueueId` por alocação.

Ainda falta:

1. Auditar bloqueio e conclusão.
2. Validar no Yard a existência, cobertura e compatibilidade da work queue informada no plano de guindastes.
3. Associar fila a CHE real.
4. Auditar prioridade de fetch/busca separadamente da prioridade operacional.
5. Criar matriz oficial de transição de work instruction.
6. Expor painel de job list por equipamento e drill-down completo.

### 3. Replanejamento real

O replanejamento já troca reservas usando outra posição real validada do Yard, cancela a reserva anterior e mantém o vínculo de compensação na mesma transação. Falta conectar o motor real de otimização ao contrato, considerando ETA, ETB, ETD, cutoff, mapa completo, dual-cycling, rehandle e disponibilidade de equipamentos.

### 4. Contratos externos e EDI

1. Publicar eventos versionados específicos de estiva, reserva, ordem, movimento e work queue; o envelope, os canais SSE/WebSocket e os eventos de visita já foram entregues.
2. Separar explicitamente eventos internos do monólito dos eventos publicados para integrações externas.
3. Tornar o processamento EDI idempotente por identificadores `UNB`, `UNH` e referência, evitando repostagem duplicada.
4. Adicionar fila de quarentena, retentativa assíncrona e outbox para EDI sem depender da requisição HTTP aberta.
5. Aplicar VERMAS também a reservas, ordens e validações de capacidade quando o Yard for incorporado ao runtime.

### 5. Testes e observabilidade

1. Validar os adaptadores HTTP legados de rollback com sucesso, retorno vazio legítimo, credencial interna, correlação e falha convertida em `503`.
2. Criar teste de contexto da Visibilidade com PostgreSQL, RabbitMQ, Redis e todos os listeners.
3. Criar testes de integração do crane plan com work queues reais do Yard e testes frontend do Quay Monitor.
4. Centralizar logs estruturados, métricas e tracing de Gate, Rail, Autenticação e Visibilidade na infraestrutura do runtime geral.
5. Criar teste de integração da reserva contra o endpoint real do Yard, cobrindo concorrência, expiração e restrições persistidas no PostgreSQL.
6. Criar smoke completo da imagem geral e comprovar que jobs, consumidores e escritas não duplicam durante o corte.
7. Testar vínculo `workQueueId`, limite de dispatch, auditoria, motivo obrigatório e autorização por perfil.
8. Validar o OpenAPI consolidado e ausência de rotas, schemas e `operationId` duplicados.
9. Testar rejeição, auditoria e limite de tentativas de BAPLIE, COPRAR, COARRI e VERMAS.

## P1

1. Relatórios operacionais e exportação CSV/PDF.
2. Completar permissões de reservas, replanejamento, sincronização e prioridades e a auditoria das ações operacionais ainda pendentes.
3. Adotar os enums compartilhados nos módulos ainda legados e remover conversões locais equivalentes.
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
8. EVP/event streaming versionado para todos os domínios operacionais.

## Critérios de aceite pendentes

1. Replanejar usando mapa e otimização real.
2. Atualizar o Control Room por eventos, sem polling como mecanismo principal.
3. Validar quay/berth/crane contra work queues, ordens e recursos reais do Yard.
4. Cobrir o fluxo por testes de service, controller, contrato e frontend.
5. Centralizar logs, métricas e tracing no runtime geral.
6. Exigir motivo e usuário autenticado nas ações aplicáveis dos módulos ainda não migrados.
7. Diferenciar fila derivada, work queue persistente, work instruction, job list e exceção operacional.
8. Manter uma única origem de API para o frontend.
9. Garantir que módulos incorporados não realizem chamadas HTTP entre si.
10. Retirar deployments legados somente após paridade, dados, segurança, observabilidade e rollback validados.
11. Executar cada job, consumidor e comando de escrita em uma única instância durante a transição.

## Fora do escopo deste corte

1. Telemetria real de equipamentos.
2. Dispatch direto para VMT real.
3. Motor matemático global multi-recurso.
4. Controle aduaneiro/documental completo.
5. Substituição integral de um TOS comercial.
