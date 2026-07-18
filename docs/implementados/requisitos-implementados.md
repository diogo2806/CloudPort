# Requisitos implementados - CloudPort

Status: atualizado em 2026-07-18 após a configuração do backend e as implementações de ERR10 e ERR30.

## Instruções obrigatórias para agentes de IA

Este é o registro canônico das funcionalidades e requisitos já implementados no CloudPort.

Não criar novos arquivos de entrega para cada alteração. Atualizar este documento e remover do backlog correspondente apenas os itens efetivamente concluídos. Não reabrir como pendência principal o que estiver listado aqui, salvo quando houver regressão comprovada ou novo critério funcional claramente diferente.

## Arquitetura e runtime canônico

1. O backend oficial é o monólito modular `backend/cloudport-runtime`.
2. O runtime incorpora Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.
3. Os limites de domínio são preservados por módulos, pacotes, portas, eventos e schemas próprios.
4. As integrações internas principais usam adaptadores locais no mesmo processo.
5. Adaptadores HTTP permanecem condicionais para rollback ou integração externa.
6. `backend/cloudport-monolito-navio` permanece somente como runtime anterior de rollback.
7. Os diretórios `backend/servico-*` continuam compiláveis isoladamente durante a janela de retorno.
8. O runtime produz um único JAR executável e uma única imagem Docker.
9. O portal e o Control Room usam uma origem de API configurável.
10. O Control Room React pode ser incorporado ao JAR do runtime.
11. `GET /assets/configuracao.json` fornece configuração dinâmica ao frontend incorporado.
12. Escritas são controladas por `cloudport.runtime.writes-enabled`.
13. Jobs são controlados por `cloudport.runtime.jobs-enabled`.
14. Consumidores são controlados por `cloudport.runtime.consumers-enabled` e pelas propriedades de inicialização do RabbitMQ.
15. O runtime anterior exige `CLOUDPORT_ROLLBACK_ENABLED=true` e permanece fail-closed por padrão.

## Maven, módulos, schemas e Flyway

1. `backend/cloudport-modules` funciona como parent e reator Maven canônico.
2. Java, BOMs, versões, `dependencyManagement`, `pluginManagement` e regras do Maven Enforcer estão centralizados.
3. `cloudport-contracts`, os oito módulos de domínio e o runtime participam do build consolidado.
4. Os módulos podem produzir biblioteca para o monólito e aplicação standalone para rollback.
5. Não há inclusão direta de fontes de projetos irmãos.
6. Cada módulo publica suas próprias migrações no artefato.
7. O PostgreSQL é compartilhado com schemas proprietários por módulo.
8. Cada schema possui histórico Flyway independente.
9. Os Flyways são executados antes da criação do `EntityManagerFactory`.
10. `validateOnMigrate` está habilitado e `clean` permanece desabilitado.
11. O `search_path` inclui os schemas dos módulos e `public`.
12. Alterações de banco seguem estratégia aditiva e `expand and contract`, sem downgrade automático.
13. O módulo de Carga Geral foi incluído no runtime, no reator e nos Dockerfiles consolidados.
14. O módulo `cloudport-contracts` foi incluído no workflow e nas imagens standalone e consolidada.

## Infraestrutura transversal

1. Cadeia de segurança stateless centralizada no runtime.
2. Login e emissão de JWT do módulo Autenticação incorporados.
3. Roles, CORS, credenciais internas e políticas de acesso centralizadas.
4. Jackson centralizado com Java Time, UTC e propriedades não nulas.
5. OpenAPI consolidado com autenticação JWT e credenciais de cliente externo.
6. Tratamento de erros padronizado com código, mensagem, detalhes, status, caminho, timestamp e `correlationId`.
7. Filtro de `X-Correlation-Id`, `traceId` e propagação de `traceparent`.
8. Logs estruturados com contexto de módulo e operação.
9. Métricas HTTP e operacionais com exportação Prometheus.
10. Health, liveness e readiness publicados pelo Actuator.
11. Scheduler e tratamento de erro de jobs centralizados.
12. Cliente HTTP comum para integrações externas.
13. Conversor JSON principal do RabbitMQ centralizado.
14. Jobs críticos podem usar `pg_try_advisory_xact_lock` para exclusão mútua.
15. PostgreSQL, RabbitMQ e Redis fazem parte do Compose consolidado.

## Segurança e autenticação

1. A senha digitada é preservada sem remoção de caracteres.
2. Senhas não são armazenadas no `localStorage`.
3. Somente dados seguros da sessão são mantidos no navegador.
4. Integrações legadas podem usar `X-CloudPort-Service-Key`.
5. Credenciais internas são comparadas em tempo constante.
6. Roles de serviço são separadas das roles humanas.
7. A manutenção de cadastros canônicos é restrita por perfil.
8. Cabeçalhos de correlação são liberados pelo CORS.
9. Falhas de integração obrigatória retornam `503`, sem serem mascaradas como lista vazia.
10. `/api/public/v1/**` é protegido por cliente ou aplicação com `X-CloudPort-Client-Id` e `X-CloudPort-Client-Secret`.
11. Clientes externos recebem a role `INTEGRACAO_EXTERNA` após validação segura do segredo.
12. APIs operacionais do runtime exigem JWT e autorização por perfil.
13. O administrador inicial é criado somente com `ADMIN_EMAIL` e `ADMIN_PASSWORD` informados pela implantação.
14. A credencial padrão insegura `gitpod/gitpod` foi removida por migração Flyway.

## Contratos compartilhados e API

1. `backend/cloudport-contracts` contém paginação, erro padronizado, comando motivado, envelope de evento e enums externos.
2. Respostas paginadas usam `conteudo`, `pagina`, `tamanho`, `totalElementos`, `totalPaginas`, `primeira` e `ultima`.
3. DTOs resumidos e detalhados de visitas e work queues foram separados.
4. Conversões de work queue foram centralizadas.
5. Filtros de visita são executados no banco.
6. A API pública usa whitelist de campos e ordenação.
7. `operationId` duplicado é evitado no OpenAPI consolidado.
8. O comando `npm run generate:api-types` gera tipos TypeScript a partir do OpenAPI.
9. Eventos externos usam envelope versionado com `eventId`, `eventType`, `eventVersion`, `occurredAt`, `correlationId`, `source` e `data`.
10. SSE e WebSocket/STOMP versionados foram publicados para eventos de visita e integrações.
11. `X-Correlation-Id` é gerado e propagado nas chamadas autenticadas.

## Módulo Navio

1. Cadastro canônico de navios com resolução por ID ou IMO.
2. Criação e manutenção de visitas de navio.
3. Itens operacionais de embarque, descarga e restow.
4. Plano de estiva vinculado à visita.
5. Eventos e resumo operacional da visita.
6. Integração Navio + Yard por reservas e ordens reais.
7. Preenchimento de posição real e sincronização do estado da ordem.
8. Consumo, cancelamento e compensação de reservas conforme o ciclo da operação.
9. Atualização do item somente quando a reconciliação altera dados.
10. Line-up operacional interno com ETA, ETB, ETD, berço, fase, progresso e conflitos.
11. Line-up vertical com berços em colunas e tempo no eixo vertical.
12. Distribuição visual de escalas sobrepostas dentro do mesmo berço.
13. Line-up público anônimo em `/line-up` e `GET /public/line-up-navios`.
14. A API pública não expõe IDs internos, observações administrativas ou dados sensíveis.
15. Cache curto e atualização automática do line-up público.
16. Quay Monitor por visita.
17. Plano de guindastes persistido e validado contra recursos operacionais do Yard.
18. Produtividade planejada e realizada por cais e guindaste.
19. Validação de período, equipamento repetido e janelas sobrepostas no crane plan.
20. Criação do Vessel Planner vinculada à escala selecionada por `bayPlanId` e `visitaNavioId`.

## Vessel Planner gráfico

1. Vistas sincronizadas de profile, top, section e tier.
2. Modo multivisão e inspector lateral de slot.
3. Drag-and-drop da load list para slots.
4. Movimentação entre slots usando a validação do backend.
5. Preservação de atributos operacionais durante a movimentação.
6. Legendas por POD, peso, IMO, reefer e operador.
7. Representação de tampas de porão.
8. Peso acumulado por stack e limites visuais.
9. Restrições e alertas exibidos diretamente nos slots.
10. Segregação IMDG gráfica.
11. Visualização de restow.
12. Sequência visual por guindaste.
13. Overlays de estabilidade e força estrutural baseados em cálculos persistidos.
14. Overlay indicativo de risco de lashing, explicitamente não certificado.
15. Planejamento de contêineres separado da estiva especializada de bobinas de aço.

## Estabilidade e atributos de estiva

1. Dados hidrostáticos sintéticos foram removidos dos cálculos operacionais.
2. Dados hidrostáticos e de resistência longitudinal são versionados.
3. Peso total, LCG, TCG, VCG, GM, calado, trim e banda usam condição real de peso leve, lastro e carga.
4. Força cortante e momento fletor são calculados por seções e limites versionados.
5. Coordenadas físicas persistidas são usadas nos cálculos.
6. Planos incompletos são marcados como simulação não operacional.
7. Aprovações persistem versões de entrada, memória de cálculo, resultado e instante.
8. Alterações no plano invalidam a aprovação anterior.
9. BAPLIE preserva posição, operação, cheio/vazio, reefer, IMO, ONU, grupo de embalagem, segregação, OOG e instruções.
10. VGM é mantido separado do peso bruto e usado como peso operacional quando disponível.
11. Slots dedicados, segregação conservadora e reserva adjacente para OOG são considerados.

## Yard e planejamento de pátio

1. Mapa georreferenciado com Google Maps, polígonos, blocos, pilhas e posições.
2. Reserva baseada em posição real com linha, coluna e camada.
3. Validação de existência, ocupação, bloqueio, interdição, área permitida, carga, peso, altura, camada e capacidade.
4. Expiração configurável de reservas.
5. Cancelamento de reservas por cancelamento de visita ou replanejamento.
6. Compensação transacional durante a troca de posição.
7. Auditoria de criação, consumo, cancelamento e expiração.
8. Workspaces de pátio salvos e restaurados no navegador.
9. Vistas de bloco, seção lateral, scan e microvisão da pilha.
10. Camadas de situação, ocupação, dwell time e reefers.
11. Heatmaps de ocupação e dwell time.
12. Destaque de pilhas bloqueadas, interditadas, cheias, reservadas ou com notas.
13. Edição motivada de bloqueio, interdição, permissão e nota de pilha.
14. Simulação de origem e destino antes da confirmação de movimento.
15. Arrastar contêiner para posição livre com validação no backend.
16. Telemetria persistida de reefers.
17. Alarmes de reefer por temperatura, alimentação e atraso da leitura.
18. Rotas desenhadas entre posição atual e destino da work instruction.
19. Editor gráfico de allocations com posições elegíveis, pré-visualização e confirmação motivada.
20. Replanejamento rejeita destino inexistente, ocupado, proibido, reservado ou igual à posição atual.
21. Planejamento de recebimento e agrupamento operacional de contêineres.

## Inventário canônico

1. Ciclo de vida completo da unidade.
2. Contêiner, chassi, carreta e acessórios no mesmo domínio.
3. Tipos, códigos ISO, dimensões, capacidades, prefixos e equivalências.
4. Lacres e documentos da unidade.
5. Avarias, componentes, condições e graus.
6. Manutenção, reparo e status de M&R.
7. Holds e permissions.
8. Ownership, operador e pools.
9. Montagem e desmontagem de equipamentos.
10. Histórico de atributos.
11. Controle reefer.
12. Inventário físico e divergências.
13. Importação e sincronização do inventário legado do pátio.
14. API canônica em `/yard/inventario/canonico`.
15. Tela unificada com filtros, indicadores, cadastro, inspector e ações rápidas.
16. Integração com `OperationalDataGrid` para pesquisa, filtros, paginação e exportação.
17. Relatório operacional de inventário com totais, retenções, avarias, reefers, perigosos, unidades sem posição e peso.

## Work queues, work instructions e dispatch

1. Criação, listagem, ativação e desativação de work queues.
2. Associação de POW, pool e equipamento.
3. Job list expansível por fila.
4. Vínculo persistente de `workQueueId` na ordem de pátio.
5. Atualização da job list por endpoint dedicado.
6. Dispatch respeitando o limite de ordens.
7. Reset e cancelamento de work instruction.
8. Alteração de prioridade, suspensão, retomada, bloqueio e conclusão conforme matriz oficial.
9. Validação de fila ativa, POW, pool, plano de guindaste, recurso de cais e equipamento operacional.
10. Auditoria de motivo, usuário, origem e `correlationId`.
11. Drill-down operacional e job lists por equipamento no Control Room.
12. Eventos internos publicados após comandos do Yard.
13. Reconciliação periódica mantida apenas como reparo de divergências.

## Control Room e equipamentos

1. Painel integrado Navio + Yard com filtros, movimentos iminentes, filas, reservas, ordens, alertas e exceções.
2. Ações de reserva, sincronização, replanejamento, prioridade, suspensão e retomada.
3. Ações de work queue e work instruction com motivo obrigatório.
4. Rota autenticada `/home/navio/control-room`.
5. SSO por `postMessage` restrito a origens configuradas.
6. Login próprio como fallback.
7. Snapshot carregado em paralelo e aplicado atomicamente.
8. SSE autenticado como mecanismo principal de atualização.
9. Snapshot inicial, heartbeat, reconexão com backoff e polling somente como fallback.
10. Quay Monitor operacional com plano de guindastes, progresso, produtividade e alertas.
11. Painel de equipamentos com status, posição, conectividade, VMT e work instruction atual.
12. Histórico persistido de telemetria e atualização quase em tempo real.
13. Detecção de telemetria atrasada, heartbeat ausente, falha de dispositivo e indisponibilidade.
14. Reconhecimento e resolução de alarmes técnicos.
15. Registro de indisponibilidade com início, encerramento, motivo e responsáveis.
16. Ciclo de comandos remotos: criação, polling, envio, execução e confirmação.
17. Dispositivos integrados por heartbeat autenticado com firmware, protocolo, endereço e sequência.
18. Navegação e autorização específicas do Control Room de equipamentos.

## Gate operacional

1. Facilities e múltiplos gates.
2. Pistas, consoles, filas e monitor de lanes.
3. Estágios, transições e business tasks configuráveis.
4. Bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos.
5. Appointments com capacidade e consumo transacional da janela.
6. Truck visits com múltiplas transações.
7. Trouble transactions.
8. Inspeções rodoviárias e operacionais.
9. Fotografias, documentos, tickets e EIR.
10. Impressão e reimpressão de EIR.
11. Transferências entre instalações.
12. Regras de bloqueio e permissão para motorista, transportadora e veículo.
13. Histórico de estágios.
14. Relatórios persistidos com período, operação, transportadora, pontualidade, no-show, ocupação, abandono e turnaround.
15. Quadro visual de pistas e filas por estágio.
16. Calendário de agendamentos com ocupação versus capacidade.
17. Jornada do veículo com OCR, balança, inspeção e liberação.
18. Painel de transações problemáticas.
19. Cronômetro e classificação visual de SLA.
20. Operação de embarque de contêiner direto do gate para o navio, sem passagem pelo pátio.
21. Fechamento do gate somente após confirmação do módulo Navio.
22. Idempotência e auditoria do embarque direto.
23. Rejeições operacionais do trigger de abertura de truck visits usam SQLStates de domínio identificáveis.
24. Capacidade esgotada e conflitos operacionais retornam `409 Conflict`; dados inválidos ou referências indisponíveis retornam `422 Unprocessable Entity`.
25. Mensagens SQL, nomes de constraints e detalhes internos não são expostos ao cliente.
26. A rejeição da abertura reverte integralmente visita, transações, eventos, consumo do agendamento e capacidade da janela.
27. Falhas técnicas não reconhecidas continuam retornando erro interno neutro, sem serem mascaradas como conflito funcional.

## Controle de entrada e saída de pessoas

1. Cadastro operacional da pessoa e situação `DENTRO` ou `FORA`.
2. Histórico com ponto de acesso, operador, origem, `correlationId` e permanência.
3. Bloqueio de entrada duplicada e saída sem entrada aberta.
4. Normalização de documento.
5. APIs de entrada, saída, presentes, resumo e histórico.
6. Tela `Gate > Controle de Pessoas`.
7. Autorização para perfis administrativos e operacionais definidos.
8. Entrada e saída são serializadas por documento normalizado com lock pessimista sem espera.
9. O caso de uso executa `flush` dentro da transação para capturar colisões antes do commit.
10. Constraint única, optimistic locking e disputa de lock são convertidos em `409 Conflict` estável.
11. A operação perdedora sofre rollback integral e não persiste movimentação duplicada.
12. O contrato `409` está documentado no OpenAPI e coberto por testes de concorrência.

## Ferrovia

1. Visitas ferroviárias, manifestos, vagões, contêineres e ordens de trabalho.
2. Lista de trabalho por visita com filtros, manifesto e métricas.
3. Início e conclusão de movimentações conforme as transições do domínio.
4. Fase `CONCLUIDO` entre processamento e partida.
5. Conclusão automática da visita quando todas as operações terminam.
6. Registro de partida somente após conclusão integral.
7. Composição gráfica do trem com locomotiva e vagões em sequência.
8. Associação visual de carga e descarga por vagão.
9. Progresso operacional por vagão.
10. Representação das linhas ferroviárias e sua ocupação.
11. Indicação de vagões bloqueados, incompatíveis e operações sem vagão válido.
12. Cronograma de chegada, operação e partida.
13. Detecção visual de conflitos entre trens e recursos.
14. Line-up ferroviário vertical por linha e etapa operacional.
15. Drag-and-drop e seletor acessível para simulação de replanejamento no frontend.
16. Locomotiva isolada tratada como a própria visita ferroviária.
17. Transferência de custódia, planejamento, checklist e embarque da locomotiva no navio.
18. Ao confirmar o embarque, a própria visita da locomotiva é encerrada.

## Carga geral, projeto e break-bulk

1. Módulo `servico-carga-geral` incorporado ao runtime modular.
2. Bill of Lading e itens do conhecimento.
3. Cargo lots para carga solta, carga de projeto e break-bulk.
4. Commodities, embalagens e produtos.
5. Códigos de armazenagem e manuseio.
6. Mercadorias perigosas com número ONU e classe IMDG.
7. Faixas de temperatura.
8. Tipos e registros de avaria.
9. Quantidade, volume e peso previstos e em estoque.
10. Recebimento, carga, descarga parcial e transferência.
11. Consolidação e desconsolidação.
12. Vínculo de lote com veículo, visita de navio, armazém e cliente.
13. Bloqueio pessimista nas movimentações de estoque.
14. Validação de saldo não negativo.
15. Dashboard e console React operacional.
16. Flyway, testes, navegação e contratos integrados ao runtime.

## Billing e portal CAP

1. Tabelas de tarifas, cobranças, faturas, itens e pagamentos.
2. Tarifas por operação e vigência.
3. Cobrança idempotente para atendimentos concluídos.
4. Consolidação de cobranças pendentes em faturas.
5. Registro de pagamentos e quitação automática.
6. Isolamento dos dados da transportadora pelos dados do JWT.
7. Resumo CAP de agendamentos, cobranças e faturas.
8. Telas, rotas e navegação por perfil.
9. Integração das telas com contratos reais do backend.

## Visibilidade e alertas

1. Rastreamento e histórico de contêineres consolidados.
2. Eventos de Gate, Yard, Rail e Navio persistidos.
3. Projeção criada quando o evento chega antes do cadastro.
4. Capacidade do Yard processada sem exigir `containerId`.
5. Status do navio preservado quando o evento altera somente o berço.
6. Throughput do Gate calculado por ciclos reais.
7. Alertas automáticos e resolução motivada.
8. Idempotência por `eventId` ou `messageId` nos consumidores.
9. Hash canônico do payload e rejeição de colisões divergentes.
10. Deduplicação, efeito e histórico executados na mesma transação.
11. Central global de alertas disponível em todas as telas autenticadas.
12. Contagem de alertas ativos e não reconhecidos no cabeçalho.
13. Filtros por status e severidade.
14. Reconhecimento e resolução com usuário e data.
15. Navegação para o módulo relacionado.
16. Página completa `/home/alertas` com indicadores e grade operacional.
17. Atualização automática e layout acessível.

## EDI e processamento assíncrono

1. BAPLIE, COPRAR, COARRI e VERMAS suportados.
2. Validação, rejeição e auditoria persistente por processamento.
3. Status `RECEBIDO`, `PROCESSANDO`, `CONCLUIDO` e `REJEITADO`.
4. Consulta paginada e detalhamento da auditoria.
5. Reprocessamento motivado com encadeamento e limite de tentativas.
6. `X-EDI-Processing-Id` retornado nas recepções aceitas.
7. Identificadores `UNB` e `UNH` persistidos.
8. Chave idempotente derivada do intercâmbio e referência da mensagem.
9. Reenvio idêntico reutiliza a recepção existente.
10. Reutilização de identidade com conteúdo divergente retorna conflito.
11. Worker persistente executa o processamento fora da requisição HTTP.
12. Mensagens são reivindicadas com trava transacional e lote limitado.
13. Retentativa exponencial e recuperação de execução interrompida.
14. Falhas esgotadas são movidas para `QUARENTENA`.
15. Processamento, efeito e conclusão permanecem na mesma transação.

## Eventos internos e reconciliação seletiva

1. Eventos de operação de pátio e cadastro de navio possuem contratos versionados.
2. Eventos de work queue e work instruction são publicados após persistência.
3. Alterações do cadastro canônico de navio publicam eventos internos.
4. Navio Siderúrgico sincroniza somente a visita afetada por evento do Yard.
5. A projeção siderúrgica atualiza somente o navio afetado.
6. Eventos internos processados são persistidos para impedir reaplicação.
7. Remoção do cadastro canônico cancela a projeção correspondente.
8. Jobs de reconciliação consultam somente registros pendentes, com erro ou desatualizados.
9. Jobs periódicos permanecem como reparo para divergências ou eventos perdidos.

## Frontend compartilhado

1. `OperationalDataGrid` substitui a tabela genérica simples.
2. Busca rápida sem diferenciação de acentos.
3. Filtros combináveis por coluna.
4. Ordenação de texto, número e data.
5. Paginação local e contrato opcional para paginação no backend.
6. Ocultação, exibição, reordenação e congelamento de coluna.
7. Persistência de preferências e visões nomeadas.
8. Seleção múltipla e ações em lote extensíveis.
9. Exportação CSV da grade ou da seleção.
10. Exportação Excel em SpreadsheetML.
11. Neutralização de valores que poderiam ser interpretados como fórmula.
12. Inspector lateral do registro.
13. Navegação por teclado e `aria-sort`.
14. Páginas genéricas inferem todos os campos retornados, sem limite de oito colunas.
15. Ajuda contextual no `PageHeader` para páginas atuais e futuras.
16. Painel de ajuda responsivo e acessível.
17. Conteúdo por rota e módulo, pesquisa sem acentos e exibição dos papéis do usuário.
18. Atalhos `F1`, `Shift + ?` e `Esc`.
19. Conteúdo específico para Gate, Rail, Yard, Navio, Embarque, Billing, CAP, Administração, Alertas e Painéis.

## Implantação e operação

1. Dockerfile multi-stage do frontend em `frontend/Dockerfile`.
2. Build do portal `frontend/cloudport` com Node 22.
3. Publicação do frontend por Nginx na porta 80.
4. Fallback de SPA para `index.html`.
5. Health check do frontend em `/health`.
6. Dockerfile do backend em `backend/Dockerfile` compatível com o contexto `/backend` do EasyPanel.
7. Parent Maven instalado antes do empacotamento dos módulos.
8. Build consolidado inclui contratos, Carga Geral e todos os módulos do runtime.
9. Diretório persistente de documentos preparado na imagem.
10. Health check do backend em `/actuator/health/readiness`.
11. Workflow valida a imagem pelo contexto da raiz e pelo contexto usado no EasyPanel.
12. Configuração documentada para frontend na porta 80 e backend na porta 8080.
13. Banco configurável por `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` e `DB_SCHEMA`.
14. JWT configurável por `SECURITY_JWT_SECRET` e `SECURITY_JWT_EXPIRATION_MS`.
15. Bootstrap administrativo configurável por `ADMIN_EMAIL` e `ADMIN_PASSWORD`.
16. Arquivo de exemplo de ambiente e Docker Compose foram alinhados às variáveis canônicas.

## Contratos de API de referência

```text
GET   /assets/configuracao.json
GET   /public/line-up-navios
GET   /yard/inventario
GET   /yard/inventario/canonico
GET   /yard/patio/work-queues
POST  /yard/patio/work-queues
PATCH /yard/patio/work-queues/{id}/ativar
PATCH /yard/patio/work-queues/{id}/desativar
PATCH /yard/patio/work-queues/{id}/pow
PATCH /yard/patio/work-queues/{id}/equipamento
PATCH /yard/patio/work-queues/{id}/ordens
GET   /yard/patio/work-queues/{id}/job-list
POST  /yard/patio/work-queues/{id}/dispatch
POST  /yard/patio/work-instructions/{id}/reset
POST  /yard/patio/work-instructions/{id}/cancelar
GET   /yard/patio/reservas/posicoes
GET   /yard/patio/reservas/auditoria
GET   /visitas-navio/{id}/integracao-patio/stream
GET   /visitas-navio/{id}/quay-monitor
POST  /visitas-navio/{id}/crane-plan
GET   /visitas-navio/{id}/produtividade-cais
GET   /api/public/v1/vessel-visits
GET   /api/public/v1/events/stream
POST  /api/edi/baplie/upload
POST  /api/edi/baplie/texto
POST  /api/edi/coprar
POST  /api/edi/coarri
POST  /api/edi/vermas
GET   /api/edi/processamentos
POST  /api/edi/processamentos/{id}/reprocessar
GET   /api/v1/visibilidade/alertas/filtrados
GET   /api/v1/visibilidade/alertas/resumo
PATCH /api/v1/visibilidade/alertas/{id}/reconhecer
PATCH /api/v1/visibilidade/alertas/{id}/resolver
GET   /api/v1/visibilidade/conteiners/{containerId}/track
GET   /api/v1/visibilidade/conteiners/{containerId}/historico
POST  /gate/embarques-diretos/navio
PATCH /rail/ferrovia/visitas/{id}/partida
GET   /rail/ferrovia/visitas/{id}/locomotiva
```

## Itens que não devem voltar como pendência principal

1. CRUD básico de visita, item e plano de navio.
2. Integração inicial e avançada entre Navio e Yard.
3. Work queues, job lists, dispatch e matriz de transições.
4. Reserva contra posição real e ciclo de expiração, cancelamento e compensação.
5. Control Room com SSE, Quay Monitor e painel de equipamentos.
6. Cadastro canônico de navio.
7. Runtime modular com build único, schemas e Flyways independentes.
8. Portas locais para integrações entre módulos incorporados.
9. Segurança, CORS, erros, logs, métricas, tracing e agendamento centralizados no runtime.
10. Controles de escrita, jobs e consumidores para coexistência.
11. API pública de Navio protegida, paginada e versionada.
12. BAPLIE, COPRAR, COARRI e VERMAS com auditoria e reprocessamento.
13. Idempotência dos consumidores da Visibilidade.
14. Recepção EDI assíncrona e idempotente.
15. Eventos internos e reconciliação seletiva.
16. Validação do crane plan contra work queues reais do Yard.
17. Estado operacional oficial de work queues e work instructions.
18. Vessel Planner gráfico multivisão.
19. Pátio gráfico, reefers, rotas e allocations.
20. Inventário canônico completo.
21. Gate operacional e Gate visual.
22. Controle de entrada e saída de pessoas com tratamento concorrente e `409 Conflict`.
23. Ferrovia operacional, visual e transferência de locomotiva.
24. Carga geral e break-bulk.
25. Billing e portal CAP.
26. Central global de alertas.
27. Grade operacional, exportação Excel e ajuda contextual.
28. Line-up interno, ferroviário e público.
29. Dockerfiles e parâmetros do EasyPanel.
30. Configuração canônica do backend por `DB_*`, `SECURITY_*` e `ADMIN_*`.
31. Tradução das rejeições transacionais de truck visits para `409` e `422` com rollback integral.
