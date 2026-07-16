# Requisitos implementados - CloudPort

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/implementados/requisitos-implementados.md`.

Não criar outros documentos, arquivos de evidência, logs, históricos ou rascunhos nesta pasta. Toda entrega deve sair de `docs/requisitos/modulo-navios-back-front-gaps.md` e ser registrada aqui sem duplicação.

## Módulo Navio implementado

1. CRUD operacional básico de visita, item e plano de estiva.
2. Itens de embarque, descarga e restow.
3. Eventos, resumo operacional e relatório integrado por visita.
4. Integração básica em `/visitas-navio/{id}/integracao-patio`.
5. Campos de integração e rastreabilidade em `ItemOperacaoNavio`.
6. Cadastro canônico de navios no `servico-navio`, com projeção siderúrgica vinculada por `navioCadastroId`.
7. Integração local com o cadastro canônico no runtime monolítico e adaptador HTTP no deployment legado.

## Integração Navio + Pátio implementada

1. Reserva de pátio vinculada ao item de navio.
2. Ordem real no `servico-yard` por `/yard/patio/ordens/navio`.
3. Identificadores de visita, item e plano na ordem de pátio.
4. Bloqueio de ordem ativa duplicada por `visitaNavioId + itemOperacaoNavioId`.
5. Filas, work queues, job list e ordens sem cobertura por visita.
6. Geração de reservas e ordens, sincronização manual e reconciliação automática.
7. Atualização do item para `EM_MOVIMENTO`, `OPERADO`, `BLOQUEADO` ou `CANCELADO` conforme a ordem real.
8. Atualização da posição real, consumo da reserva na conclusão e cancelamento quando a ordem é cancelada.
9. Registro de eventos somente quando a reconciliação altera dados.
10. Alteração de prioridade, suspensão e retomada de ordens.
11. Replanejamento inicial e simulação operacional.

## Reserva contra mapa real implementada

1. Consulta de `GET /yard/patio/posicoes` antes de reservar.
2. Seleção somente de posição real com linha, coluna e camada.
3. Rejeição de mapa vazio, posição inexistente, posição ocupada e posição com reserva ativa.
4. Remoção de identificadores artificiais de posição.
5. Persistência do identificador e das coordenadas reais na reserva.
6. Dados suficientes para criação da ordem real no Yard.

## Work queues e Equipment Control implementados

1. Listar, criar, ativar e desativar work queue.
2. Associar POW, pool operacional e equipamento.
3. Vincular ordens explicitamente por `PATCH /yard/patio/work-queues/{id}/ordens`.
4. Vincular automaticamente somente quando existe uma única fila compatível.
5. Persistir `workQueueId` em `OrdemTrabalhoPatio`.
6. Expor job list e executar dispatch com limite real de ordens.
7. Resetar e cancelar work instruction.
8. Persistir auditoria de criação, ativação, desativação, POW/pool, equipamento, vínculo, dispatch, reset e cancelamento.
9. Restringir comandos a perfis autorizados e ao serviço interno de Navio.
10. Padronizar a resposta do dispatch com fila, totais, ordens e mensagem.

## Scheduler operacional implementado

1. Remoção de equipamentos, contêineres e coordenadas gerados aleatoriamente.
2. Entrada `SchedulerPlanoOperacionalRequisicaoDto` com navio, equipamentos e posições reais.
3. Validação das quantidades manifestadas e da janela de chegada/partida.
4. Conflito considerado somente entre navios do mesmo berço.
5. Preservação da duração quando o slot é deslocado.
6. Persistência da agenda em `vessel_schedule`.
7. Diagnóstico calculado pela quantidade real de movimentos.
8. API restrita a perfis autorizados.
9. Orquestração global pelo módulo de Navio usando visita, berço, janela, reservas reais, itens de embarque/descarga e equipamentos vinculados às work queues.
10. Resultado versionado com status total ou parcial e alertas para itens sem posição ou movimentos não cobertos pelo dual-cycle.

## Control Room implementado

1. Painel Navio + Pátio com filtros, movimentos iminentes, filas, reservas, ordens, alertas e exceções.
2. Work queues persistentes e job list expansível.
3. Ativação/desativação de fila, edição de POW/pool/equipamento, dispatch, reset e cancelamento de work instruction.
4. Geração de reservas/ordens, sincronização, replanejamento, prioridade, suspensão e retomada.
5. Integração ao portal pela rota autenticada `/home/navio/control-room`.
6. SSO por `postMessage` restrito a origens configuradas e login próprio como fallback.
7. JWT, usuário autenticado, origem da ação e `X-Correlation-Id` nas chamadas operacionais.
8. Feedback de loading, sucesso, erro e exibição de `codigo`, `mensagem`, `detalhes` e `correlationId`.
9. Snapshot paralelo, atômico e protegido contra atualizações sobrepostas.
10. Substituição do polling periódico por SSE autenticado com reconexão e `Last-Event-ID`.
11. Vessel view consolidada por porão, com itens, pesos, progresso e alertas.
12. Yard view consolidada por bloco, com reservas, ordens por status e divergências.
13. CHE detail com work queue, POW, pool, job list, VMT, posição, work instruction e telemetria.
14. Quay Monitor com berço, fase, movimentos, produtividade observada, filas, CHE, previsão de conclusão e risco operacional.
15. Comparação automática entre estiva planejada/real, posição de pátio planejada/real e ordem esperada/executada.
16. Previsão determinística de gargalos para carga bloqueada, ausência de fila, backlog, fila sem CHE, telemetria atrasada e risco de cutoff.
17. Painéis de gargalos e divergências com severidade e recomendação operacional.
18. Ação de otimização global de Navio, Pátio e equipamentos.

## Relatórios operacionais implementados

1. Relatório operacional integrado em JSON.
2. Exportação CSV UTF-8 com visita, navio, fase, item, movimento, produto, carga, peso, estiva, pátio, status e integração.
3. Exportação PDF paginada com resumo, itens, estiva, pátio, alertas e divergências.
4. Downloads autenticados no frontend.
5. CORS configurado para `Content-Disposition` quando aplicável.

## Eventos operacionais e streaming versionado implementados

1. Contrato `EventoOperacionalVersionadoDTO` com `schemaVersion`, sequência, `eventId`, agregado, tipo, visita, item, usuário, horário, `correlationId` e dados.
2. Stream SSE por visita em `GET /visitas-navio/{id}/eventos/stream`.
3. Nome de evento `cloudport.operacao.v1`, tempo de reconexão e snapshot obrigatório ao conectar.
4. Publicação automática após persistência do evento operacional.
5. Evento versionado após geração de plano de otimização global.
6. Cliente frontend SSE autenticado por `fetch`/`ReadableStream`, com parsing, reconexão e envio de `Last-Event-ID`.
7. CORS dos serviços e do runtime monolítico atualizado para `Last-Event-ID`.
8. Arquitetura preparada para posterior adaptador EVP/Kafka/CDC, sem afirmar integração externa durável já concluída.

## Telemetria e VMT implementados

1. Entidade `TelemetriaEquipamentoPatio` vinculada de forma única ao equipamento.
2. Persistência da última leitura com latitude/longitude ou X/Y, heading, posição mais próxima, distância, geofence, origem, operador/status VMT, work instruction, sequência e horários.
3. Validação de coordenada, sequência crescente e timestamp mais recente.
4. Atualização opcional da linha/coluna observada do equipamento.
5. APIs para registrar, listar e detalhar telemetria por equipamento.
6. Stream SSE `cloudport.telemetria.v1` com snapshot e atualizações.
7. Consumo da telemetria pelo Control Room e associação ao CHE detail.
8. Migração `V102__telemetria_equipamentos_vmt.sql` com constraints e índices.

## Validação de estiva, lashing, estabilidade e estrutura implementada

1. Contrato configurável de limites e regras por execução.
2. Validação de peso por porão com limite informado ou referência estimada a partir de DWT/quantidade de porões.
3. Validação de peso por camada, altura máxima e porão interditado.
4. Validação de lashing declarado a partir de camada configurada.
5. Verificação de equilíbrio transversal entre bombordo e boreste.
6. Regras configuráveis de segregação por tipo de carga, porão e distância entre colunas.
7. Resultado `VALIDO`, `VALIDO_COM_ALERTAS`, `INVALIDO` ou `SEM_PLANO`, com erros, alertas e verificações não configuradas.
8. Endpoint `POST /visitas-navio/{id}/validacoes-estruturais` e formulário no Control Room.
9. Separação explícita entre validação operacional e futura certificação naval por modelo oficial do navio.

## Autenticação e segurança implementadas

1. Senha preservada sem sanitização destrutiva antes do login e não armazenada em `localStorage`.
2. Credencial interna `X-CloudPort-Service-Key` entre Navio, Navio Siderúrgico e Yard, com comparação constante.
3. Roles de serviço separadas e manutenção do cadastro canônico restrita.
4. CORS para `Authorization`, `X-Correlation-Id`, credencial interna, `Last-Event-ID` e downloads.
5. Retorno `503 Service Unavailable` quando o proxy de work queues não consegue consultar o Yard.
6. Segurança centralizada no runtime consolidado do primeiro corte.

## Monólito modular de Navio implementado

1. Runtime `backend/cloudport-monolito-navio` carregando Navio e Navio Siderúrgico no mesmo processo Spring Boot.
2. Porta local `CadastroNavioPorta` e adaptador `CadastroNavioLocalAdapter` para remover HTTP entre módulos incorporados.
3. Artefatos Maven independentes com perfil `modulo-monolito` e reator `backend/cloudport-navio-modules`.
4. Dois schemas e históricos Flyway independentes, carregados dos respectivos artefatos Maven.
5. PostgreSQL compartilhado com `search_path` controlado e `ddl-auto=validate`.
6. Segurança, CORS, JWT, credencial interna e frontend React centralizados no runtime.
7. Configuração dinâmica do frontend por `GET /assets/configuracao.json`.
8. Dockerfile, imagem unificada e Compose com perfis `monolito` e `legado`.
9. Modo somente leitura e desativação de jobs nos deployments legados durante o corte.
10. Bloqueio distribuído por PostgreSQL para jobs concorrentes.
11. Testes ArchUnit contra ciclo, acesso direto a repository de outro módulo e cliente HTTP entre módulos incorporados.
12. Estratégia `expand and contract`, smoke, paridade e runbook de corte/rollback.

## Visibilidade operacional implementada

1. Remoção de mapeamentos MVC duplicados.
2. Rastreamento e histórico de contêineres consolidados.
3. Eventos reais de gate, pátio e rail e atualização de capacidade sem exigir `containerId`.
4. Preservação de status do navio quando muda somente o berço e criação da projeção quando o evento chega primeiro.
5. Remoção de totais, velocidades, previsões e tempos fictícios.
6. Throughput de gate somente com ciclos reais pareados.
7. Erro padronizado com código, mensagem, detalhes, correlationId e timestamp.
8. Motivo obrigatório para resolução de alertas.
9. Configurações externas de banco, RabbitMQ, Redis, JWT e metas.

## Contratos de API implementados neste corte

```text
GET  /visitas-navio/{id}/control-room
GET  /visitas-navio/{id}/quay-monitor
POST /visitas-navio/{id}/otimizacao-global
POST /visitas-navio/{id}/validacoes-estruturais
GET  /visitas-navio/{id}/eventos/stream
GET  /visitas-navio/{id}/relatorio-operacional-integrado.csv
GET  /visitas-navio/{id}/relatorio-operacional-integrado.pdf
GET  /yard/patio/equipamentos/telemetria
GET  /yard/patio/equipamentos/telemetria/{identificador}
POST /yard/patio/equipamentos/telemetria/{identificador}
GET  /yard/patio/equipamentos/telemetria/stream
```

## Testes e validações implementados

1. Testes de contrato do Control Room para work queues, job list, dispatch, reset e cancelamento.
2. Testes de componente para filtros, totalização, expansão e ações operacionais.
3. Testes do scheduler com dados reais, quantidades inconsistentes, conflito no mesmo berço e simultaneidade em berços diferentes.
4. Testes da porta local de cadastro canônico e da configuração Flyway/CORS do runtime unificado.
5. Teste de contexto com PostgreSQL/Testcontainers, migrações reais e exercício dos repositórios JPA.
6. Validação CI do Compose, da imagem completa e do build pelo reator Maven.
7. Testes de persistência/localização e listeners de Visibilidade.
8. Smoke do runtime com frontend, configuração dinâmica, segurança, persistência e integração autenticada com o Yard.
9. Testes ArchUnit, bloqueio distribuído, modo somente leitura e validação Flyway.
10. Testes frontend dos contratos do painel avançado, Quay Monitor, validação estrutural e otimização global.

## Itens que não devem voltar como pendência principal

1. CRUD básico de visita, item e plano de estiva.
2. Integração inicial Navio + Pátio e reserva em posição real livre.
3. Work queues, job list e ações básicas do Control Room.
4. Scheduler baseado em dados operacionais reais e agenda persistente.
5. Cadastro canônico e primeiro runtime monolítico Navio + Navio Siderúrgico.
6. Frontend incorporado, configuração dinâmica, segurança única e rollback documentado.
7. Relatórios operacionais CSV/PDF.
8. Vessel view, yard view, CHE detail, alertas consolidados e Quay Monitor inicial.
9. Comparação automática planejado x executado e previsão determinística de gargalos.
10. Ingestão, persistência e streaming da última telemetria/VMT por equipamento.
11. Validação operacional configurável de lashing, estabilidade transversal, segregação e limites estruturais.
12. SSE versionado para operação e telemetria, com reconexão autenticada no frontend.
13. Orquestração da otimização global com dados reais; permanece pendente apenas a aplicação transacional completa e o otimizador matemático avançado.
