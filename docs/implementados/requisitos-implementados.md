# Requisitos implementados - CloudPort

Status: atualizado em 2026-07-18 com base nas entregas incorporadas à `main` até o PR #406.

## Instruções obrigatórias para agentes de IA

Este é o registro canônico das funcionalidades e requisitos já implementados no CloudPort.

Não criar novos arquivos de entrega para cada alteração. Atualizar este documento e remover do backlog correspondente apenas os itens efetivamente concluídos. Não reabrir como pendência principal o que estiver listado aqui, salvo quando houver regressão comprovada ou novo critério funcional claramente diferente.

## Arquitetura e runtime canônico

1. O backend oficial é o monólito modular `backend/cloudport-runtime`.
2. O runtime incorpora Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.
3. `backend/cloudport-contracts` concentra contratos compartilhados sem compartilhar entidades JPA ou repositories.
4. Limites de domínio são preservados por módulos, pacotes, portas, eventos e schemas próprios.
5. Integrações internas principais usam adaptadores locais no mesmo processo.
6. HTTP e mensageria permanecem na borda externa ou no caminho temporário de rollback.
7. `backend/cloudport-monolito-navio` permanece somente como runtime anterior de rollback.
8. O runtime produz um único JAR executável e uma única imagem Docker.
9. Escritas, jobs e consumidores podem ser desabilitados separadamente durante o corte.
10. O runtime anterior exige `CLOUDPORT_ROLLBACK_ENABLED=true` e permanece fail-closed por padrão.

## Maven, schemas e Flyway

1. `backend/cloudport-modules` funciona como parent e reator Maven canônico.
2. O build inclui `cloudport-contracts`, os oito módulos de domínio e o runtime.
3. O parent Maven é instalado antes do empacotamento consolidado.
4. Cada módulo publica suas próprias migrações.
5. O PostgreSQL é compartilhado com ownership por schema.
6. Cada schema possui histórico Flyway independente.
7. Flyway executa antes da criação do `EntityManagerFactory`.
8. `validateOnMigrate` está habilitado e `clean` permanece desabilitado.
9. Alterações de banco seguem estratégia aditiva e `expand and contract`.

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

O backend usa as variáveis:

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

1. composição da conexão PostgreSQL pelas variáveis `DB_*`;
2. configuração explícita dos schemas do runtime;
3. segredo e expiração JWT por `SECURITY_JWT_*`;
4. criação do administrador inicial somente quando `ADMIN_EMAIL` e `ADMIN_PASSWORD` são fornecidos;
5. remoção do administrador funcional padrão inseguro;
6. migração para retirar o registro administrativo padrão conhecido;
7. `.env.example` para o runtime consolidado;
8. documentação das variáveis no README principal e no runtime.

## Infraestrutura transversal e segurança

1. Cadeia de segurança stateless centralizada.
2. Login e emissão de JWT do módulo Autenticação incorporados.
3. Roles, CORS e políticas de acesso centralizadas.
4. Jackson com Java Time, UTC e propriedades não nulas.
5. OpenAPI consolidado com os esquemas de autenticação.
6. Erro padronizado com código, mensagem, detalhes, status, caminho, timestamp e `correlationId`.
7. Propagação de `X-Correlation-Id`, `traceId` e `traceparent`.
8. Logs estruturados, métricas Prometheus, health, liveness e readiness.
9. Scheduler e tratamento de erros de jobs centralizados.
10. Cliente HTTP comum para integrações externas.
11. Conversor JSON do RabbitMQ centralizado.
12. Jobs críticos podem usar lock distribuído no PostgreSQL.
13. O portal encerra a sessão em respostas `401` e aceita somente retorno interno seguro.
14. APIs operacionais exigem JWT e autorização por perfil.
15. APIs públicas externas usam cliente e segredo configuráveis.

## Contratos compartilhados e API

1. Paginação, erro, comando motivado, envelope de evento e enums externos estão em `cloudport-contracts`.
2. DTOs resumidos e detalhados foram separados.
3. Filtros paginados são executados no banco nos fluxos migrados.
4. A API pública usa whitelist de campos e ordenação.
5. `operationId` duplicado é evitado no OpenAPI consolidado.
6. Tipos TypeScript podem ser gerados a partir do OpenAPI.
7. Eventos externos usam envelope versionado.
8. SSE e WebSocket/STOMP versionados foram publicados nos fluxos aplicáveis.
9. Correlação é gerada e propagada nas chamadas autenticadas.

## Navio e Vessel Planner

1. Cadastro canônico de navios e visitas operacionais.
2. Itens de embarque, descarga e restow.
3. Planos de estiva vinculados às visitas.
4. Integração Navio + Yard por reservas e ordens reais.
5. Sincronização de estado, posição e ciclo das reservas.
6. Line-up interno, vertical e público.
7. Contrato público sanitizado em `/line-up` e `/public/line-up-navios`.
8. Quay Monitor, crane plan e produtividade planejada versus realizada.
9. Vistas profile, top, section e tier sincronizadas.
10. Drag-and-drop, inspector de slot e movimentação validada pelo backend.
11. Legendas por POD, peso, IMO, reefer e operador.
12. Tampas de porão, peso por stack e restrições nos slots.
13. Segregação IMDG, restow e sequência visual de guindastes.
14. Overlays de estabilidade e força estrutural persistidos.
15. Overlay de lashing indicativo e não certificado.
16. Hidrostática e resistência longitudinal versionadas.
17. Cálculos de peso, centros de gravidade, GM, calado, trim, banda, força cortante e momento fletor.
18. Aprovações versionadas e invalidadas quando o plano muda.
19. BAPLIE com posição, operação, VGM, reefer, perigosos e OOG.

## Yard, inventário e dispatch

1. Google Maps com blocos, pilhas e posições georreferenciadas.
2. Vistas de bloco, seção, scan e microvisão.
3. Heatmaps, workspaces, notas, bloqueios, interdições e permissões.
4. Reservas em posições reais com validação de ocupação, peso, altura, apoio e capacidade.
5. Expiração, cancelamento e compensação de reservas.
6. Movimentação com simulação e confirmação validada no backend.
7. Telemetria reefer, alarmes, rotas e editor de allocations.
8. Planejamento de recebimento e agrupamento de contêineres.
9. Otimização global de posições por custo mínimo com algoritmo Húngaro.
10. Domínio canônico de unidade e equipamento.
11. Contêiner, chassi, carreta, acessórios, tipos, prefixos e equivalências.
12. Lacres, documentos, avarias, manutenção, holds, ownership, montagem, reefer e inventário físico.
13. API em `/yard/inventario/canonico`.
14. Work queues, POW, pool, equipamento, job list e dispatch.
15. Reset, cancelamento, prioridade, suspensão, retomada, bloqueio e conclusão.
16. Auditoria por usuário, motivo, origem e `correlationId`.
17. Reshuffling com posição real, reserva e idempotência.

## Control Room e Visibilidade

1. Painel Navio + Yard com movimentos, filas, reservas, ordens, alertas e exceções.
2. SSE autenticado, snapshot inicial, heartbeat, reconexão e fallback.
3. Equipamentos com status, posição, conectividade, VMT e work instruction atual.
4. Histórico persistido de telemetria.
5. Alarmes de atraso, heartbeat ausente, falha e indisponibilidade.
6. Reconhecimento e resolução de alarmes.
7. Ciclo de comandos remotos e heartbeat autenticado dos dispositivos.
8. Rastreamento e histórico de contêineres.
9. Projeções e eventos de Gate, Yard, Rail e Navio.
10. Idempotência por `eventId` ou `messageId`.
11. Central global de alertas com filtros, reconhecimento, resolução e navegação.

## Gate operacional e visual

1. Facilities, múltiplos Gates, pistas, consoles, filas e monitor de lanes.
2. Estágios, transições e business tasks configuráveis.
3. Bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos.
4. Appointments com capacidade transacional.
5. Truck visits com múltiplas transações.
6. Trouble transactions, inspeções, fotos, documentos, tickets e EIR.
7. Transferências entre instalações e regras de acesso.
8. Histórico de estágios e relatórios operacionais.
9. Quadro visual de pistas e filas.
10. Calendário de agendamentos e ocupação.
11. Jornada do veículo com OCR, balança, inspeção e liberação.
12. Painel de problemas e SLA.
13. Embarque de contêiner direto do Gate para o navio.
14. Saída direta de carga autopropelida descarregada do navio.

## Controle de pessoas — ERR10 concluído

1. Cadastro e situação `DENTRO` ou `FORA`.
2. Histórico com ponto de acesso, operador, origem, correlação e permanência.
3. Bloqueio de entrada duplicada e saída sem entrada aberta.
4. Busca serializada por documento com `PESSIMISTIC_WRITE` e timeout imediato.
5. `saveAndFlush` para materializar conflitos dentro da operação.
6. Violações da constraint de documento e disputas de lock são traduzidas para HTTP `409`.
7. OpenAPI declara `201` e `409`.
8. Testes cobrem constraint, lock pessimista e lock otimista.

O requisito `ERR10` foi concluído no PR #396.

## Truck visits — ERR30 concluído

1. A validação transacional no PostgreSQL permanece atômica.
2. SQLStates de domínio distinguem capacidade esgotada, bloqueio de motorista, transportadora ou veículo e referência indisponível.
3. O serviço captura somente rejeições operacionais conhecidas.
4. Capacidade esgotada e disputas retornam `409 Conflict`.
5. Dados inválidos ou referências indisponíveis retornam `422 Unprocessable Entity`.
6. Mensagens SQL e detalhes internos não são expostos.
7. A transação reverte visita, transações, eventos e consumo de capacidade quando a abertura é rejeitada.
8. Controller e testes documentam os contratos de erro.

O requisito `ERR30` foi concluído no PR #398.

## Ferrovia

1. Visitas, manifestos, vagões, contêineres e ordens.
2. Lista de trabalho, início, conclusão e partida.
3. Fase `CONCLUIDO` antes da partida.
4. Composição gráfica com locomotiva, vagões e contêineres.
5. Ocupação de linhas, progresso e conflitos.
6. Line-up ferroviário vertical.
7. Drag-and-drop e seletor acessível para simulação de replanejamento.
8. Locomotiva isolada tratada como a própria visita ferroviária.
9. Custódia, planejamento, checklist e embarque no navio.

A persistência do replanejamento visual por vagão permanece no backlog.

## Carga Geral e carga siderúrgica

1. Bill of Lading, itens e cargo lots.
2. Carga solta, projeto e break-bulk.
3. Commodities, embalagens, produtos, armazenagem e manuseio.
4. Perigosos, temperatura e avarias.
5. Quantidade, volume, peso e saldo.
6. Recebimento, carga, descarga parcial, transferência, consolidação e desconsolidação.
7. Vínculos com veículo, navio, armazém e cliente.
8. Bloqueio pessimista e saldo não negativo.
9. Planejamento de bobinas, porões, tank top e posicionamento.
10. Empilhamento, estabilidade, securing e aprovação baseada em evidências versionadas.
11. Materiais, pontos de amarração, capacidades e regras persistidas.
12. Valores sintéticos não aprovam o plano siderúrgico.

## Billing e portal CAP — ERR20 concluído

1. Tarifas, cobranças, faturas, itens e pagamentos.
2. Cobrança idempotente para atendimentos concluídos.
3. Consolidação de cobranças pendentes e quitação automática.
4. Isolamento por transportadora com dados do JWT.
5. Portal CAP com agendamentos, cobranças e faturas.
6. A geração de fatura bloqueia a transportadora e as cobranças elegíveis com `SELECT ... FOR UPDATE`.
7. O registro de pagamento bloqueia a linha da fatura antes de recalcular o saldo disponível.
8. Fatura, itens, status das cobranças e pagamentos permanecem na mesma transação.
9. Cobrança indisponível, saldo consumido por operação concorrente e falha de lock retornam `409 Conflict`.
10. A tradução de violação de integridade é restrita à constraint conhecida que impede uma cobrança em duas faturas.
11. O handler de domínio tem precedência para impedir que conflitos operacionais sejam convertidos em `500`.
12. Testes unitários cobrem aquisição dos locks e tradução funcional das disputas.

O requisito `ERR20` foi concluído no PR #406.

## EDI e eventos

1. BAPLIE, COPRAR, COARRI e VERMAS.
2. Auditoria persistente e reprocessamento motivado.
3. Recepção EDI assíncrona e idempotente.
4. Worker com reivindicação transacional, retentativa e quarentena.
5. Eventos internos publicados após persistência.
6. Reconciliação periódica mantida como reparo.

## Frontend compartilhado

1. `OperationalDataGrid` com busca, filtros, ordenação e paginação.
2. Colunas configuráveis, visões nomeadas, seleção múltipla e inspector.
3. Exportação CSV e Excel com neutralização de fórmulas.
4. Navegação por teclado e atributos de acessibilidade.
5. Ajuda contextual por rota, módulo, processo e perfil.
6. Central global de alertas no cabeçalho.
7. Configuração dinâmica da navegação.
8. Sessão JWT invalidada de forma consistente em respostas `401`.

## Implantação e operação

1. Frontend em `frontend/Dockerfile`, com Node 22, Nginx, porta 80 e `/health`.
2. Backend em `backend/Dockerfile`, com contexto `/backend`, porta 8080 e `/actuator/health/readiness`.
3. Dockerfile alternativo do backend para build a partir da raiz.
4. Parent Maven instalado antes do empacotamento.
5. Contratos, Carga Geral e todos os módulos incluídos na imagem.
6. Diretório persistente de documentos preparado.
7. Workflow valida os contextos Docker suportados.
8. Compose utiliza as variáveis `DB_*`, `SECURITY_JWT_*`, `ADMIN_*` e dependências externas.
9. Runbook documenta corte, smoke, execução única e rollback sem downgrade.

## Entregas recentes de referência

| PR | Entrega |
| --- | --- |
| #406 | Serialização de faturamento e pagamentos; conclusão de ERR20 |
| #404 | Consolidação do estado atual do CloudPort |
| #401 | Documentação após ERR10 e ERR30 |
| #399 | Arquitetura e runbook alinhados ao runtime atual |
| #398 | Respostas operacionais para rejeições de truck visits; conclusão de ERR30 |
| #396 | Concorrência no controle de pessoas; conclusão de ERR10 |
| #395 | Variáveis de ambiente e remoção do administrador padrão inseguro |
| #394 | Consolidação da documentação funcional |
| #393 | Backend no EasyPanel |
| #392 | Frontend no EasyPanel |
| #386 | Operação completa do Gate |
| #385 | Carga Geral e break-bulk |
| #384 | Reefers, rotas e allocations no Yard |
| #383 | Inventory Management canônico |
| #382 | Gate visual |
| #381 | Vessel Planner gráfico completo |
| #379 | Pátio operacional |
| #378 | Control Room, telemetria e equipamentos |
| #376 | Ferrovia visual |
| #375 | Grade operacional e Excel |
| #373 | Ajuda contextual |
| #371 | Billing e CAP |
| #370 | Central global de alertas |

## Pendências não marcadas como implementadas

Permanecem no backlog técnico `ERR40`, `SEC70`, `SEC80` e `SEC90`, conforme `docs/requisitos/requisito-tecnico.md`.

Permanecem no backlog funcional a persistência do replanejamento ferroviário, a conclusão e edição integral das operações de navio, o corte operacional e as evoluções registradas em `docs/requisitos/modulo-navios-back-front-gaps.md`.
