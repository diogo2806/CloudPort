# Requisitos implementados - CloudPort

Status: atualizado em 2026-07-18 com base nas entregas incorporadas à `main` até o PR #396.

## Instruções obrigatórias para agentes de IA

Este é o registro canônico das funcionalidades e requisitos já implementados no CloudPort.

Não criar novos arquivos de entrega para cada alteração. Atualizar este documento e remover do backlog correspondente apenas os itens efetivamente concluídos. Não reabrir como pendência principal o que estiver listado aqui, salvo quando houver regressão comprovada ou novo critério funcional claramente diferente.

## Arquitetura e runtime canônico

1. O backend oficial é o monólito modular `backend/cloudport-runtime`.
2. O runtime incorpora Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.
3. Os limites de domínio são preservados por módulos, pacotes, portas, eventos e schemas próprios.
4. `backend/cloudport-contracts` concentra contratos compartilhados sem compartilhar entidades JPA ou repositories.
5. As integrações internas principais usam adaptadores locais no mesmo processo.
6. Adaptadores HTTP permanecem condicionais para rollback ou integração externa.
7. `backend/cloudport-monolito-navio` permanece somente como runtime anterior de rollback.
8. Os diretórios `backend/servico-*` continuam compiláveis isoladamente durante a janela de retorno.
9. O runtime produz um único JAR executável e uma única imagem Docker.
10. O portal e o Control Room usam uma origem de API configurável.
11. Escritas, jobs e consumidores podem ser controlados separadamente para impedir execução duplicada durante o corte.
12. O runtime anterior exige `CLOUDPORT_ROLLBACK_ENABLED=true` e permanece fail-closed por padrão.

## Maven, módulos, schemas e Flyway

1. `backend/cloudport-modules` funciona como parent e reator Maven canônico.
2. `cloudport-contracts`, os oito módulos de domínio e o runtime participam do build consolidado.
3. Java, BOMs, versões, `dependencyManagement`, `pluginManagement` e regras do Maven Enforcer estão centralizados.
4. Os módulos podem produzir biblioteca para o monólito e aplicação standalone para rollback.
5. Não há inclusão direta de fontes de projetos irmãos.
6. Cada módulo publica suas próprias migrações.
7. O PostgreSQL é compartilhado com schemas proprietários por módulo.
8. Cada schema possui histórico Flyway independente.
9. Os Flyways são executados antes da criação do `EntityManagerFactory`.
10. `validateOnMigrate` está habilitado e `clean` permanece desabilitado.
11. O `search_path` inclui os schemas dos módulos e `public`.
12. Alterações de banco seguem estratégia aditiva e `expand and contract`, sem downgrade automático.
13. O módulo de Carga Geral está incluído no runtime, no reator e nos Dockerfiles.
14. O módulo `cloudport-contracts` está incluído no workflow e nas imagens standalone e consolidada.

Schemas atuais:

- `cloudport_autenticacao`;
- `cloudport_carga_geral`;
- `cloudport_gate`;
- `cloudport_rail`;
- `cloudport_visibilidade`;
- `cloudport_yard`;
- `cloudport_navio`;
- `cloudport_siderurgico`.

## Configuração do backend

O backend aceita as variáveis:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASS
DB_SCHEMA
SECURITY_JWT_SECRET
SECURITY_JWT_EXPIRATION_MS
ADMIN_EMAIL
ADMIN_PASSWORD
```

Foram implementados:

1. composição da conexão PostgreSQL a partir das variáveis `DB_*`;
2. configuração explícita do schema de execução;
3. segredo e expiração JWT por `SECURITY_JWT_*`;
4. criação do administrador inicial somente quando `ADMIN_EMAIL` e `ADMIN_PASSWORD` são fornecidos;
5. ausência de administrador funcional padrão inseguro;
6. migração para remoção do registro administrativo padrão conhecido;
7. arquivo `.env.example` do runtime consolidado;
8. documentação das variáveis no README principal e no runtime.

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
13. O portal invalida a sessão em respostas `401`, remove o token e redireciona com retorno interno seguro.

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

## Navio e Vessel Planner

1. Cadastro canônico de navios com resolução por ID ou IMO.
2. Criação e manutenção de visitas, itens operacionais e planos de estiva.
3. Integração Navio + Yard por reservas e ordens reais.
4. Sincronização do estado das ordens e das posições reais.
5. Consumo, cancelamento e compensação de reservas.
6. Line-up interno com ETA, ETB, ETD, berço, fase, progresso e conflitos.
7. Line-up vertical com berços em colunas e tempo no eixo vertical.
8. Line-up público anônimo em `/line-up` e `GET /public/line-up-navios`.
9. Contrato público sanitizado, sem IDs internos ou observações administrativas.
10. Quay Monitor, plano de guindastes e produtividade planejada versus realizada.
11. Validação de período, equipamento repetido e janelas sobrepostas no crane plan.
12. Vessel Planner vinculado à escala selecionada.
13. Vistas sincronizadas de profile, top, section e tier.
14. Modo multivisão e inspector lateral de slot.
15. Drag-and-drop da load list para slots e movimentação entre slots.
16. Legendas por POD, peso, IMO, reefer e operador.
17. Tampas de porão, peso por stack e restrições diretamente nos slots.
18. Segregação IMDG, restow e sequência visual por guindaste.
19. Overlays de estabilidade e força estrutural baseados em cálculos persistidos.
20. Overlay de lashing explicitamente indicativo e não certificado.
21. Dados hidrostáticos e de resistência longitudinal versionados.
22. Peso total, centros de gravidade, GM, calado, trim, banda, força cortante e momento fletor calculados a partir de dados persistidos.
23. Aprovações persistem versões de entrada, memória de cálculo e resultado.
24. Alterações no plano invalidam a aprovação anterior.
25. BAPLIE preserva atributos de posição, operação, VGM, reefer, perigosos e OOG.

## Yard, inventário e dispatch

1. Mapa georreferenciado com Google Maps, polígonos, blocos, pilhas e posições.
2. Vistas de bloco, seção lateral, scan e microvisão.
3. Camadas, heatmaps, workspaces, notas, bloqueios, interdições e permissões.
4. Reserva baseada em posição real com linha, coluna e camada.
5. Validação de existência, ocupação, bloqueio, área, carga, peso, altura, apoio e capacidade.
6. Expiração, cancelamento e compensação de reservas.
7. Simulação e confirmação de movimentos com validação do backend.
8. Telemetria de reefers e alarmes por temperatura, alimentação e atraso.
9. Rotas no mapa e editor gráfico de allocations.
10. Planejamento de recebimento e agrupamento operacional de contêineres.
11. Otimização global de posições por custo mínimo com algoritmo Húngaro.
12. Restrições de reefer, perigosos, reservas, apoio físico e capacidade consideradas no planejamento.
13. Custos de rehandle, distância, mistura, nova pilha e camada considerados na otimização.
14. Domínio canônico de unidade e equipamento.
15. Contêiner, chassi, carreta e acessórios.
16. Tipos, códigos ISO, dimensões, capacidades, prefixos e equivalências.
17. Lacres, documentos, avarias, manutenção, holds e permissions.
18. Ownership, operador, montagem, histórico, reefer e inventário físico.
19. API canônica em `/yard/inventario/canonico`.
20. Work queues, POW, pool, equipamento, job list e dispatch.
21. Reset, cancelamento, prioridade, suspensão, retomada, bloqueio e conclusão.
22. Motivo, usuário, origem e `correlationId` auditados.
23. Reshuffling baseado no mapa real, com reserva do destino e idempotência.

## Control Room e equipamentos

1. Painel Navio + Yard com movimentos, filas, reservas, ordens, alertas e exceções.
2. Ações operacionais protegidas e motivadas.
3. SSE autenticado como mecanismo principal de atualização.
4. Snapshot inicial, heartbeat, reconexão e polling como fallback.
5. Painel de equipamentos com status, posição, conectividade, VMT e work instruction atual.
6. Histórico persistido de telemetria.
7. Detecção de telemetria atrasada, heartbeat ausente, falha e indisponibilidade.
8. Reconhecimento e resolução de alarmes técnicos.
9. Registro de indisponibilidade com início, fim, motivo e responsáveis.
10. Ciclo de comandos remotos: criação, polling, envio, execução e confirmação.
11. Dispositivos integrados por heartbeat autenticado com firmware, protocolo, endereço e sequência.

## Gate operacional e visual

1. Facilities, múltiplos Gates, pistas, consoles, filas e monitor de lanes.
2. Estágios, transições e business tasks configuráveis.
3. Bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos.
4. Appointments com capacidade e consumo transacional da janela.
5. Truck visits com múltiplas transações.
6. Trouble transactions e inspeções.
7. Fotografias, documentos, tickets e EIR.
8. Impressão e reimpressão de EIR.
9. Transferências entre instalações.
10. Regras de bloqueio e permissão para motorista, transportadora e veículo.
11. Histórico de estágios e relatórios operacionais.
12. Quadro visual de pistas e filas por estágio.
13. Calendário de agendamentos com ocupação versus capacidade.
14. Jornada do veículo com OCR, balança, inspeção e liberação.
15. Painel de transações problemáticas e classificação de SLA.
16. Embarque de contêiner direto do Gate para o navio sem passagem pelo pátio.
17. Fechamento do Gate somente após confirmação do módulo Navio.
18. Saída direta de carga autopropelida descarregada do navio.

## Controle de entrada e saída de pessoas

1. Cadastro operacional da pessoa e situação `DENTRO` ou `FORA`.
2. Histórico com ponto de acesso, operador, origem, `correlationId` e permanência.
3. Bloqueio de entrada duplicada e saída sem entrada aberta.
4. Normalização de documento.
5. APIs de entrada, saída, presentes, resumo e histórico.
6. Tela `Gate > Controle de Pessoas`.
7. Autorização para perfis administrativos e operacionais.
8. Busca serializada por documento com `PESSIMISTIC_WRITE` e timeout imediato.
9. `saveAndFlush` usado para materializar conflitos dentro da operação.
10. Violações da constraint de documento, locks pessimistas e locks otimistas são traduzidos para HTTP `409`.
11. O contrato OpenAPI documenta `201` para sucesso e `409` para conflito concorrente.
12. Testes cobrem colisão de documento, lock pessimista e lock otimista.

O requisito técnico `ERR10` foi concluído no PR #396.

## Ferrovia

1. Visitas ferroviárias, manifestos, vagões, contêineres e ordens.
2. Lista de trabalho por visita com filtros e métricas.
3. Início, conclusão e partida conforme as transições do domínio.
4. Fase `CONCLUIDO` antes da partida.
5. Composição gráfica com locomotiva e vagões.
6. Associação visual de carga e descarga por vagão.
7. Progresso operacional e ocupação das linhas.
8. Indicação de vagões bloqueados ou incompatíveis.
9. Cronograma e conflitos entre trens e recursos.
10. Line-up ferroviário vertical.
11. Drag-and-drop e seletor acessível para simulação de replanejamento.
12. Locomotiva isolada tratada como a própria visita ferroviária.
13. Transferência de custódia, planejamento, checklist e embarque no navio.

A persistência do replanejamento visual por vagão permanece no backlog.

## Carga Geral, projeto e break-bulk

1. Módulo `servico-carga-geral` incorporado ao runtime.
2. Bill of Lading e itens do conhecimento.
3. Cargo lots para carga solta, projeto e break-bulk.
4. Commodities, embalagens e produtos.
5. Códigos de armazenagem e manuseio.
6. Mercadorias perigosas com número ONU e classe IMDG.
7. Faixas de temperatura e avarias.
8. Quantidade, volume e peso previstos e em estoque.
9. Recebimento, carga, descarga parcial e transferência.
10. Consolidação e desconsolidação.
11. Vínculos com veículo, visita de navio, armazém e cliente.
12. Bloqueio pessimista e saldo não negativo.
13. Dashboard e console React.
14. Flyway, testes, navegação e contratos integrados ao runtime.

## Carga siderúrgica

1. Planos persistidos por navio e visita.
2. Manifesto de bobinas, porões e setores de tank top.
3. Posicionamento, empilhamento, estabilidade e securing.
4. Relatório, validação completa e aprovação explícita.
5. Dimensões de apoio, geometria, camadas, espaçamento, dunnage, calços e sequência de descarga.
6. Materiais certificados, pontos de amarração, capacidade nominal e carga de trabalho segura.
7. Regras e versões da especificação persistidas.
8. Valores sintéticos não são usados para aprovar o plano.

## Billing e portal CAP

1. Tarifas, cobranças, faturas, itens e pagamentos.
2. Tarifas por operação e vigência.
3. Cobrança idempotente para atendimentos concluídos.
4. Consolidação de cobranças pendentes em faturas.
5. Registro de pagamentos e quitação automática.
6. Isolamento por transportadora com dados do JWT.
7. Resumo CAP de agendamentos, cobranças e faturas.
8. Telas, rotas e navegação por perfil.

## Visibilidade, alertas, EDI e eventos

1. Rastreamento e histórico de contêineres.
2. Eventos de Gate, Yard, Rail e Navio persistidos.
3. Projeções operacionais e reconciliação seletiva.
4. Alertas automáticos e resolução motivada.
5. Idempotência por `eventId` ou `messageId`.
6. Central global de alertas no cabeçalho e em `/home/alertas`.
7. Filtros, reconhecimento, resolução e navegação ao módulo de origem.
8. BAPLIE, COPRAR, COARRI e VERMAS suportados.
9. Auditoria persistente e reprocessamento motivado.
10. Recepção EDI assíncrona e idempotente.
11. Worker com reivindicação transacional, retentativa e quarentena.
12. Eventos internos publicados após persistência.
13. Reconciliação periódica mantida como reparo de divergências.

## Frontend compartilhado

1. `OperationalDataGrid` com busca, filtros, ordenação e paginação.
2. Ocultação, reordenação, congelamento e persistência de colunas.
3. Visões nomeadas, seleção múltipla e ações em lote.
4. Exportação CSV e Excel.
5. Neutralização de fórmulas nas exportações.
6. Inspector lateral e navegação por teclado.
7. Inferência de todos os campos retornados.
8. Ajuda contextual no `PageHeader`.
9. Painel acessível, conteúdo por rota, pesquisa e atalhos.

## Implantação e operação

1. Dockerfile multi-stage do frontend em `frontend/Dockerfile`.
2. Build com Node 22 e publicação por Nginx na porta 80.
3. Fallback da SPA e health check em `/health`.
4. Dockerfile do backend em `backend/Dockerfile` para o contexto `/backend` do EasyPanel.
5. Parent Maven instalado antes do empacotamento.
6. Build inclui contratos, Carga Geral e todos os módulos.
7. Diretório persistente de documentos preparado na imagem.
8. Health check em `/actuator/health/readiness`.
9. Workflow valida os dois contextos Docker do backend.
10. Frontend e backend possuem parâmetros documentados no EasyPanel.
11. O Compose utiliza as variáveis `DB_*`, `SECURITY_JWT_*`, `ADMIN_*` e dependências externas configuráveis.

## Entregas recentes de referência

| PR | Entrega |
| --- | --- |
| #396 | Concorrência no controle de acesso de pessoas e conclusão de `ERR10` |
| #395 | Novas variáveis de ambiente e remoção do administrador padrão inseguro |
| #394 | Consolidação da documentação funcional |
| #393 | Correção do backend no EasyPanel |
| #392 | Imagem do frontend para EasyPanel |
| #386 | Operação completa do Gate |
| #385 | Carga Geral e break-bulk |
| #384 | Reefers, rotas e allocations no Yard |
| #383 | Inventory Management canônico |
| #382 | Gate visual |
| #381 | Vessel Planner gráfico completo |
| #379 | Pátio operacional |
| #378 | Control Room, telemetria e equipamentos |
| #376 | Ferrovia visual |
| #375 | Exportação Excel e grade operacional |
| #373 | Ajuda contextual |
| #371 | Billing e CAP |
| #370 | Central global de alertas |
| #360 | Line-up público de navios |
| #357 | Controle de pessoas |
| #345 | Embarque direto do Gate para o navio |
| #340 | Otimização global de posições do Yard |
| #339 | Agrupamento para recebimento no pátio |
| #338 | Saída direta de carga autopropelida |

## Pendências não marcadas como implementadas

Permanecem no backlog técnico `ERR20`, `ERR30`, `ERR40`, `SEC70`, `SEC80` e `SEC90`, conforme `docs/requisitos/requisito-tecnico.md`.

Permanecem no backlog funcional a persistência do replanejamento ferroviário, a conclusão e edição integral de operações de navio, o corte operacional, as integrações reais de campo e as evoluções avançadas registradas em `docs/requisitos/modulo-navios-back-front-gaps.md`.