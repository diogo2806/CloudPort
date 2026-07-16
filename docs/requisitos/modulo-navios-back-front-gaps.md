# Requisitos pendentes - CloudPort

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Não criar outros arquivos de requisito, relatórios de execução, históricos ou rascunhos nesta pasta. Itens concluídos devem sair deste arquivo e ser registrados em `docs/implementados/requisitos-implementados.md`.

Antes de desenvolver, ler este arquivo e `docs/implementados/requisitos-implementados.md`. Depois de desenvolver, remover daqui o que foi entregue, registrar a entrega no arquivo de implementados e acrescentar lacunas encontradas em APIs, telas, contratos, testes, observabilidade e memórias de cálculo.

## Diretriz arquitetural vigente

O backend alvo é um **monólito modular**. O runtime `backend/cloudport-monolito-navio` incorpora no código Navio, Navio Siderúrgico, Yard, Gate, Rail, Autenticação e Visibilidade. Não criar HTTP entre módulos incorporados, dependência cíclica nem acesso ao repository de outro módulo.

Deployments e credenciais legadas continuam disponíveis somente para corte e rollback até a validação operacional de paridade. As regras estão em `docs/arquitetura-monolito-modular.md` e `docs/operacao-corte-rollback-navio.md`.

## Pendências de integração Back x Front

1. Expor no frontend a conclusão/publicação do plano por `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`.
2. Completar edição de visita e item pelos contratos `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`.
3. Criar cancelamento administrativo diferenciado para visita e item.
4. Criar tela de diagnóstico dos contratos `/api/public/v1/*`.
5. Integrar o motor real de otimização ao endpoint de replanejamento da visita.
6. Separar em `/filas` e `/sem-cobertura` as causas sem fila, sem POW, sem equipamento e sem job list.
7. Evoluir o relatório integrado com produtividade, divergências detalhadas, planejado x realizado e exportação.

## Pendências do Control Room

1. Criar drill-down da work instruction com eventos, auditoria, divergências, reserva, item de navio e movimento de pátio.
2. Diferenciar visualmente sem fila, sem POW, sem equipamento, sem job list, posição inválida, reserva bloqueada e divergência Navio x Pátio.
3. Criar painel de CHE e job list por equipamento.
4. Integrar ao Quay Monitor os contratos persistidos de berth e crane.
5. Criar E2E de login/SSO, SSE, job list, dispatch, reset, cancelamento e indisponibilidade do Yard.

## Pendências de contratos compartilhados

1. Padronizar paginação para listas grandes.
2. Padronizar enums de visita, item, ordem, reserva, work queue, severidade e alerta.
3. Tornar `motivo` obrigatório nos comandos de cancelamento, suspensão, retomada, reset e alterações administrativas.
4. Gerar tipos TypeScript a partir do OpenAPI consolidado.
5. Renomear contratos internos que ainda carregam nomes históricos de transporte, como `WorkQueuePatioYardDTO`.
6. Separar DTO resumido de lista e DTO detalhado de job list.
7. Definir contrato versionado de evento para SSE, WebSocket e integrações externas.
8. Validar automaticamente o OpenAPI consolidado contra operação, rota e schema duplicados.

## Pendências do módulo de Visibilidade

1. Persistir `eventId` ou `messageId` e impedir duplicação de histórico em redelivery.
2. Substituir `repository.findAll()` e filtros em memória por consultas paginadas.
3. Publicar atualização do dashboard imediatamente após eventos de Gate, Yard, Rail e Navio, mantendo agendamento apenas como reconciliação.
4. Persistir quantidades reais de movimentos, produtividade, equipamentos alocados e previsão de saída antes de preencher esses campos.
5. Integrar Visibilidade ao contrato compartilhado de enums e eventos versionados.
6. Criar teste de contexto com PostgreSQL, RabbitMQ, Redis e mapeamentos Spring reais.
7. Garantir idempotência dos consumidores e comandos de projeção.

## Pendências operacionais do monólito modular

A incorporação estrutural dos sete módulos, as portas locais, o parent Maven, os sete schemas/Flyways, a infraestrutura transversal e os controles globais de execução única foram implementados no código.

Ainda falta concluir o corte operacional:

1. Executar o build e os testes completos em CI e corrigir incompatibilidades de contexto, bean, rota, JPA ou migração encontradas.
2. Validar paridade funcional de todos os endpoints consumidos pelo frontend e integrações externas.
3. Validar o OpenAPI consolidado e impedir rotas ou schemas duplicados no pipeline.
4. Executar smoke integrado de Navio, Yard, Gate, Rail, Autenticação e Visibilidade com PostgreSQL, RabbitMQ e Redis reais.
5. Comparar dados e históricos dos sete schemas antes e depois do corte.
6. Ensaiar rollback para os deployments legados usando os mesmos schemas.
7. Definir a janela de observação e os indicadores que autorizam retirar os legados.
8. Remover deployments, imagens, DNS, portas, variáveis, clientes HTTP e `X-CloudPort-Service-Key` somente em mudança posterior ao aceite do corte.
9. Renomear `cloudport-monolito-navio` e diretórios `servico-*` somente quando pipelines e rollback não dependerem mais dos nomes atuais.
10. Garantir idempotência persistente para todos os consumidores e comandos de escrita, além do controle global de ativação.

## P0 - Pendências obrigatórias restantes

### 1. Eventos Yard -> Navio

A reconciliação automática e idempotente por job atualiza item, posição real e reserva. Falta substituir a consulta periódica por evento interno no runtime e por evento externo versionado quando necessário.

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

1. Auditar suspensão, retomada, bloqueio e conclusão.
2. Associar work queue a porão, plano de guindaste e recurso de cais.
3. Associar fila a CHE real.
4. Auditar prioridade de fetch separadamente da prioridade operacional.
5. Criar matriz oficial de transição de work instruction.
6. Expor painel de job list por equipamento e drill-down completo.

### 3. Replanejamento real

Conectar o contrato do scheduler ao replanejamento da visita, considerando ETA, ETB, ETD, cutoff, mapa, bloqueios, capacidade, dual-cycling e rehandle.

### 4. Integração Quay, berth e crane

Os contratos backend já existem. Ainda falta:

1. integrar o Quay Monitor ao Control Room;
2. associar filas, work instructions, CHE e recursos de cais ao plano de guindastes;
3. registrar atrasos e produtividade em eventos internos e projeções de Visibilidade;
4. criar E2E do fluxo plano de guindaste -> execução -> produtividade.

### 5. Contratos externos e EDI

1. Proteger `/api/public/v1` por cliente ou aplicação.
2. Implementar filtros, paginação, campos selecionáveis e erro padronizado.
3. Implementar eventos externos versionados de visita, estiva, reserva, ordem, movimento e work queue.
4. Completar BAPLIE, COPRAR, COARRI e VERMAS com validação, rejeição, reprocessamento e auditoria.
5. Separar eventos internos do monólito dos eventos publicados externamente.

### 6. Testes e observabilidade

1. Testar portas locais e adaptadores HTTP legados dos cortes Navio, Yard e Autenticação.
2. Testar vínculo `workQueueId`, limite de dispatch, auditoria e autorização por perfil.
3. Criar E2E do fluxo operacional completo no runtime com os sete módulos.
4. Expandir logs e métricas com módulo, visita, item, reserva, ordem, work queue e equipamento sem labels de alta cardinalidade.
5. Validar tracing distribuído nas integrações externas.
6. Criar teste de contexto de Visibilidade com PostgreSQL, RabbitMQ e Redis.
7. Criar testes de idempotência por redelivery e retry.
8. Criar teste automático contra rotas e schemas OpenAPI duplicados.

## P1

1. Relatórios operacionais e exportação CSV/PDF.
2. Completar permissões e auditoria de reservas, ordens, replanejamento, sincronização e prioridades.
3. Padronizar status entre Navio, Yard, work queue e alertas.
4. Substituir a sincronização periódica da projeção siderúrgica por evento interno.
5. Formalizar matriz de dependências permitidas entre todos os módulos.
6. Concluir idempotência, paginação e publicação orientada a eventos na Visibilidade.

## P2

1. Integração EDI operacional atualizando reservas e ordens automaticamente.
2. Otimização global Navio + Yard + Equipamento.
3. Comparação automática entre estiva, pátio e execução.
4. Previsão de gargalos por berço, porão, bloco, fila e equipamento.
5. Control Room completo com yard view, vessel view, CHE detail, alerts e quay monitor.
6. Telemetria/VMT real.
7. Lashing, estabilidade, segregação e restrições estruturais.
8. EVP e event streaming versionado.

## Critérios de aceite pendentes

1. Replanejar usando mapa e otimização real.
2. Integrar o Control Room ao streaming sem polling de fallback permanente.
3. Integrar quay, berth e crane às filas e ordens.
4. Padronizar, versionar, paginar e proteger contratos externos.
5. Cobrir o fluxo por testes de service, controller, contrato e frontend.
6. Exigir motivo e usuário autenticado nas ações aplicáveis.
7. Diferenciar fila derivada, work queue persistente, work instruction, job list e exceção operacional.
8. Validar corte e rollback dos sete módulos sem dupla escrita, job ou consumidor.
9. Retirar deployments e credenciais legadas somente após paridade, dados, segurança, observabilidade e rollback validados.

## Fora do escopo deste corte

1. Telemetria real de equipamentos.
2. Dispatch direto para VMT real.
3. Motor matemático global multi-recurso.
4. Controle aduaneiro e documental completo.
5. Substituição integral de um TOS comercial.
