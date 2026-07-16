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
5. Conectar o resultado da otimização global ao fluxo transacional de replanejamento, substituindo reservas e ordens com compensação da posição anterior.
6. Separar em `/filas` e `/sem-cobertura` as causas sem fila, sem POW, sem equipamento e sem job list.
7. Criar drill-down completo da work instruction com eventos, auditoria, divergências, reserva, item de navio e movimento de pátio.
8. Permitir cadastrar e manter regras de segregação, lashing e limites estruturais por classe de navio, sem exigir parâmetros manuais em cada validação.

## Pendências do Control Room

1. Diferenciar visualmente sem fila, sem POW, sem equipamento, sem job list, posição inválida, reserva bloqueada e divergência Navio x Pátio em todos os componentes, não apenas nos painéis consolidados.
2. Associar o Quay Monitor a crane plan, atividade de guindaste, atrasos, hatch covers e produtividade por recurso de cais.
3. Expandir para os demais backends o contrato de erro com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp já aplicado em parte do sistema.
4. Criar e2e para login/SSO, eventos SSE, job list, dispatch, reset, cancelamento, relatórios, otimização e indisponibilidade do Yard.
5. Implementar replay durável do stream a partir de `Last-Event-ID`; a reconexão atual solicita novo snapshot, mas não mantém log de eventos para repetição histórica.

## Pendências de contratos compartilhados

1. Padronizar paginação para listas grandes.
2. Padronizar enums de visita, item, ordem, reserva, work queue, severidade e alerta.
3. Tornar `motivo` obrigatório nos comandos de cancelamento, suspensão, retomada, reset e alterações administrativas.
4. Gerar tipos TypeScript a partir de OpenAPI.
5. Centralizar conversão de `WorkQueuePatioYardDTO`.
6. Separar DTO resumido de lista e DTO detalhado de job list.
7. Publicar um OpenAPI consolidado no runtime monolítico sem duplicação de operação, schema ou configuração.
8. Formalizar schema registry, política de compatibilidade e retenção para eventos externos versionados.

## Pendências do módulo de Visibilidade

1. Persistir `eventId` ou `messageId` e impedir duplicação de histórico em redelivery do RabbitMQ.
2. Substituir buscas com `repository.findAll()` e filtros em memória por consultas paginadas no banco.
3. Publicar atualização do dashboard imediatamente após eventos de gate, pátio, rail e navio, mantendo o agendamento apenas como reconciliação.
4. Persistir quantidades reais de movimentos, produtividade, equipamentos alocados e previsão de saída antes de preencher esses campos nos contratos.
5. Integrar a Visibilidade ao contrato compartilhado de enums e eventos versionados.
6. Criar teste de contexto com PostgreSQL, RabbitMQ e mapeamentos Spring reais, além dos testes unitários atuais.
7. Preparar o módulo para incorporação ao runtime monolítico sem manter segurança, tratamento de erros e agendamento duplicados.

## Pendências da migração para monólito modular

O corte Navio + Navio Siderúrgico já possui paridade estrutural de controllers, segurança única, integração local do cadastro canônico, validação dos dados e históricos Flyway, bloqueio distribuído dos jobs, deployments legados em modo somente leitura, testes de arquitetura e runbook de corte/rollback.

Ainda falta:

1. Incorporar o Yard ao runtime e substituir a integração HTTP Navio -> Yard por portas locais, preservando os contratos REST externos.
2. Incorporar Gate e Rail como módulos, usando o Yard por interfaces internas e mantendo TOS, OCR, EDI e mensageria como adaptadores externos.
3. Incorporar Autenticação e Visibilidade, centralizando emissão de token, OpenAPI, erros, logs, métricas e tracing.
4. Definir ownership de tabelas e schemas para todos os módulos antes de consolidar a conexão PostgreSQL.
5. Centralizar versões e `pluginManagement` em um parent Maven compartilhado, sem criar dependências cíclicas.
6. Centralizar configurações de segurança, CORS, Jackson, tratamento de erros, observabilidade e agendamento no runtime geral.
7. Remover clientes HTTP, credenciais internas, imagens, deployments e variáveis legadas somente depois da conclusão de cada corte.
8. Evoluir `cloudport-monolito-navio` para um runtime geral do CloudPort antes de incorporar domínios não relacionados a Navio.

## P0 - Pendências obrigatórias restantes

### 1. Eventos Pátio -> Navio

O frontend e o módulo de Navio já recebem eventos operacionais e telemetria por SSE versionado. Ainda falta substituir a reconciliação periódica Pátio -> Navio por evento interno quando Yard estiver no monólito ou por evento externo idempotente enquanto permanecer separado.

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

Já entregue: vínculo persistente `workQueueId`, painel CHE/job list, endpoint de vínculo, auditoria básica e limite real no dispatch.

Ainda falta:

1. Auditar suspensão, retomada, bloqueio e conclusão.
2. Associar work queue a porão, plano de guindaste e recurso de cais.
3. Validar a associação da fila a CHE real contra cadastro e disponibilidade do equipamento.
4. Auditar prioridade de fetch/busca separadamente da prioridade operacional.
5. Criar matriz oficial de transição de work instruction.
6. Completar drill-down da work instruction.

### 4. Replanejamento transacional

A otimização global usa visita, janela, berço, reservas reais, itens de embarque/descarga e equipamentos das work queues, chamando o scheduler real do Yard. Falta aplicar o resultado como uma única operação transacional e idempotente, considerando mapa, bloqueios, capacidade, dual-cycling, rehandle, cancelamento compensatório e rollback.

### 5. Quay/berth/crane

Já entregue: `GET /visitas-navio/{id}/quay-monitor` com progresso, produtividade observada, filas, CHE, previsão de conclusão e risco.

Ainda falta:

```text
POST /visitas-navio/{id}/crane-plan
GET  /visitas-navio/{id}/produtividade-cais
```

O Quay Monitor deve passar a consumir crane plan e eventos reais de atividade/parada de guindaste.

### 6. Contratos externos, EVP e EDI

1. Proteger `/api/public/v1` por client/app.
2. Implementar filtros, paginação, campos selecionáveis, `correlationId`, erro padronizado e OpenAPI.
3. Publicar eventos externos versionados de visita, estiva, reserva, ordem, movimento, work queue e telemetria por outbox durável.
4. Implementar adaptador EVP/Kafka ou CDC com checkpoint, replay, idempotência, métricas e tratamento de indisponibilidade.
5. Completar BAPLIE, COPRAR, COARRI e VERMAS com validação, rejeição, reprocessamento e auditoria.
6. Separar eventos internos do monólito de eventos publicados para integrações externas.

### 7. Lashing, estabilidade e estrutura certificada

A validação operacional configurável já cobre peso por porão/camada, altura, porão interditado, lashing declarado, equilíbrio transversal e regras de segregação. Ainda falta:

1. Persistir parâmetros por classe/modelo de navio e versionar sua vigência.
2. Importar dados de ship model, arquivos de estabilidade, strength e lashing.
3. Calcular LCG, VCG, TCG, draft, trim, GM, shear force, bending moment e limites estruturais pelo modelo oficial do navio.
4. Validar IMDG/D&H e segregação por matriz regulamentar completa.
5. Registrar aprovação, override, motivo, responsável e evidência do plano.
6. Deixar explícito que a validação operacional não substitui software naval ou aprovação certificada.

### 8. Telemetria e VMT em produção

A API já persiste a última leitura real por CHE, rejeita sequência atrasada, transmite snapshot/atualizações por SSE e apresenta posição, VMT e work instruction no Control Room. Ainda falta:

1. Integrar adaptadores reais de GPS, PLC, OCR, ECS ou fornecedor VMT.
2. Persistir histórico temporal completo, além da última leitura.
3. Detectar perda de sinal, salto impossível, equipamento parado, geofence e divergência de posição.
4. Implementar dispatch e confirmação direta no VMT com ACK, timeout, retry e idempotência.
5. Definir retenção, volume, particionamento e monitoramento da telemetria.

### 9. Testes e observabilidade

1. Testar o proxy de work queues com sucesso, retorno vazio legítimo e falha do Yard convertida em `503` enquanto o Yard permanecer externo.
2. Criar testes de contrato entre Navio Siderúrgico, Yard e Navio, cobrindo chamada local e adaptadores HTTP legados.
3. Testar vínculo `workQueueId`, limite de dispatch, auditoria e autorização por perfil.
4. Testar reserva contra mapa real: inexistente, ocupada, já reservada e mapa vazio.
5. Criar e2e do fluxo operacional completo, incluindo SSE, relatórios, otimização, validação estrutural e VMT.
6. Adicionar logs estruturados, métricas e tracing com módulo, visita, item, reserva, ordem, work queue, equipamento, evento e `correlationId`.
7. Validar o OpenAPI consolidado e ausência de rotas duplicadas.
8. Criar testes de carga e reconexão para SSE, telemetria e geração de relatórios.
9. Testar migração `V102__telemetria_equipamentos_vmt.sql` com PostgreSQL real.

## P1

1. Completar permissões e auditoria de reservas, ordens, replanejamento, sincronização e prioridades.
2. Padronizar status entre Navio, Pátio, work queue e alertas.
3. Substituir a sincronização periódica da projeção siderúrgica do cadastro canônico por evento interno.
4. Criar matriz de dependências permitidas entre todos os módulos do monólito.
5. Concluir idempotência, consultas paginadas e publicação orientada a eventos no módulo de Visibilidade.
6. Criar filtros e templates configuráveis para relatórios CSV/PDF, geração assíncrona e armazenamento de arquivos grandes.

## P2

1. Integração EDI operacional atualizando reservas e ordens automaticamente.
2. Otimizador matemático multi-recurso com função objetivo configurável, restrições formais, explicabilidade e comparação de cenários.
3. Previsão estatística de gargalos treinada com histórico real; a previsão atual é determinística por regras operacionais.
4. Digital twin com replay histórico de navio, pátio, cais e equipamentos.

## Critérios de aceite pendentes

1. Impedir reserva bloqueada, sem capacidade ou incompatível com a carga.
2. Expirar e auditar reservas automaticamente.
3. Aplicar replanejamento real com compensação e rollback.
4. Integrar quay/berth/crane às filas e ordens.
5. Padronizar, versionar, paginar e proteger contratos externos.
6. Cobrir o fluxo por testes de service, controller, contrato, frontend e e2e.
7. Rastrear o fluxo por logs, métricas e tracing.
8. Exigir motivo e usuário autenticado nas ações aplicáveis.
9. Diferenciar fila derivada, work queue persistente, work instruction, job list e exceção operacional.
10. Manter uma única origem de API para o frontend após cada corte.
11. Garantir que módulos incorporados não realizem chamadas HTTP entre si em cada novo corte.
12. Retirar um deployment legado somente após paridade, dados, segurança, observabilidade e rollback validados.
13. Executar cada job, consumidor e comando de escrita em uma única instância durante cada novo corte.
14. Validar regras estruturais com dados oficiais do navio antes de tratar o resultado como certificado.
15. Garantir replay, idempotência e retenção para eventos externos e telemetria.

## Fora do escopo deste corte

1. Certificação naval do cálculo de estabilidade, strength e lashing.
2. Dispatch direto para hardware VMT real.
3. Substituição integral de um TOS comercial.
4. Controle aduaneiro/documental completo.
