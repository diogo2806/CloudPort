# Requisitos funcionais pendentes — CloudPort

Status: atualizado em 2026-07-18 com base no código incorporado à `main`.

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter este arquivo como fonte funcional consolidada de lacunas entre backend, frontend e operação.

Antes de desenvolver, ler também:

- `docs/requisitos/requisito-tecnico.md`;
- `docs/implementados/requisitos-implementados.md`;
- `docs/arquitetura-monolito-modular.md`.

Depois de implementar uma entrega, remover a lacuna correspondente daqui e registrar a capacidade em `docs/implementados/requisitos-implementados.md`.

## Diretriz arquitetural vigente

O backend alvo é o monólito modular `backend/cloudport-runtime`, com Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico no mesmo processo.

Não criar novos microsserviços para funcionalidades internas nem ampliar chamadas HTTP entre módulos incorporados. HTTP, RabbitMQ, Redis, storage, EDI, OCR, TOS e webhooks permanecem na borda.

## Capacidades retiradas das pendências

As seguintes capacidades já foram entregues e não devem voltar como lacuna principal:

- `OperationalDataGrid` com busca, filtros, paginação, seleção, colunas, inspector e exportação CSV/Excel;
- composição ferroviária visual com locomotivas, vagões, contêineres, progresso, linhas, cronograma e conflitos;
- Control Room de equipamentos com telemetria, SSE, heartbeat, alarmes, indisponibilidades e comandos;
- Yard com vistas de bloco, seção, scan e microvisão, heatmaps, workspaces, drag-and-drop, simulação, restrições e notas;
- telemetria reefer, alarmes de temperatura, rotas e editor gráfico de allocations;
- inventário canônico de contêineres, chassis, carretas e acessórios;
- Vessel Planner com `profile`, `top`, `section`, `tier`, drag-and-drop, overlays, restow e sequência de guindastes;
- Gate visual com pistas, filas, calendário, jornada do veículo, documentos, avarias, EIR e SLA;
- domínio de Carga Geral, carga de projeto e break-bulk;
- Gate operacional com facilities, configurações, orders, appointments, truck visits, troubles, inspeções, documentos e transferências;
- build e implantação backend pelo contexto `/backend` e frontend pelo contexto `/frontend` no EasyPanel.

## P0 — segurança, concorrência e consistência

As pendências técnicas detalhadas permanecem em `docs/requisitos/requisito-tecnico.md`:

1. `ERR10`: serializar entrada e saída concorrentes de pessoas.
2. `ERR20`: impedir faturamento e pagamento concorrentes inconsistentes.
3. `ERR30`: traduzir rejeições transacionais na abertura de truck visits.
4. `ERR40`: tratar disputas concorrentes nos cadastros únicos de Carga Geral.
5. `SEC70`: sanitizar logs e exceções da integração TOS.
6. `SEC80`: proteger a execução standalone de Carga Geral.
7. `SEC90`: autenticar e autorizar os canais WebSocket operacionais do Yard.

Esses itens bloqueiam a classificação dos fluxos correspondentes como concluídos para produção, mesmo quando a capacidade funcional já existe.

## P0 — integração Back x Front ainda pendente

1. Expor no frontend a conclusão/publicação do plano por `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`.
2. Completar edição de visita e item pelos contratos `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`.
3. Criar cancelamento administrativo diferenciado para visita e item, com motivo, autorização e auditoria.
4. Criar tela de diagnóstico dos contratos `/api/public/v1/*`, incluindo credencial, escopos, filtros e stream.
5. Persistir no backend o replanejamento de vagões realizado na interface ferroviária; a entrega atual é visual.
6. Separar nas exceções operacionais as causas sem fila, sem POW, sem equipamento, sem job list, posição inválida e reserva bloqueada.
7. Evoluir relatórios integrados com planejado versus realizado, produtividade, divergências detalhadas e exportação PDF.
8. Completar telas administrativas para referências de Carga Geral e Gate quando a operação ainda depender somente dos endpoints.

## P0 — otimização operacional

1. Conectar o motor real de otimização ao replanejamento de visita, considerando ETA, ETB, ETD, cutoff, mapa completo, capacidade, rehandle e disponibilidade de equipamentos.
2. Persistir decisões de replanejamento ferroviário e validar compatibilidade de vagão no backend.
3. Evoluir a escolha de posição do Yard para otimização multiobjetivo com distância, ocupação, dwell, restrições, reefer, peso, sequência de navio e custo de rehandle.
4. Comparar automaticamente estiva planejada, posição no Yard e execução real, produzindo divergência acionável.

## P0 — corte operacional

1. Executar o corte dos ambientes para `cloudport-runtime`, mantendo um único escritor, scheduler e consumidor por fila.
2. Validar os oito schemas e históricos Flyway no ambiente alvo.
3. Executar smoke integrado de Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.
4. Validar as duas imagens backend e a imagem frontend com os mesmos contextos usados no EasyPanel.
5. Ensaiar rollback para `cloudport-monolito-navio` e serviços isolados sem downgrade de banco.
6. Remover deployments, imagens e credenciais legadas somente após paridade, observação e retorno comprovados.

## P1 — contratos e integrações

1. Adotar `cloudport-contracts` nos contratos locais equivalentes ainda duplicados.
2. Substituir enums locais equivalentes sem quebrar contratos externos.
3. Gerar tipos TypeScript no pipeline a partir do OpenAPI consolidado e comparar o snapshot.
4. Adicionar escopos, rotação de segredo, expiração, rate limit e auditoria por cliente da API pública.
5. Aplicar VERMAS também às reservas, ordens e validações de capacidade dependentes de peso.
6. Publicar eventos externos específicos de estiva, reserva, ordem, movimento, work queue, Gate, Rail, Carga Geral e inventário.
7. Manter eventos internos do monólito separados dos eventos publicados para terceiros.

## P1 — Control Room e observabilidade

1. Completar drill-down de work instruction com eventos, auditoria, divergências, reserva, item de navio e movimento de pátio.
2. Consolidar no mesmo painel a visão de CHE, job list, alertas, Yard, Vessel e Quay Monitor conforme o perfil do usuário.
3. Centralizar os tratamentos de erro, logs, métricas e tracing que ainda permaneçam definidos localmente nos módulos.
4. Garantir que corpos externos, mensagens SQL, credenciais e dados pessoais não sejam registrados.
5. Proteger SSE e WebSocket por autenticação, origem e autorização por destino.

## P1 — planejamento técnico certificado

1. Substituir o overlay indicativo de lashing por cálculo ou integração certificada quando necessário para decisão operacional.
2. Validar estabilidade e força estrutural com dados hidrostáticos e limites versionados de cada navio.
3. Manter bloqueio de aprovação quando entradas obrigatórias estiverem ausentes ou a memória de cálculo for somente simulada.
4. Evoluir segregação IMDG, OOG, reefers e restrições estruturais para regras configuráveis por modelo de navio e terminal.

## P2 — evolução do produto

1. Otimização global Navio + Yard + Gate + Rail + equipamentos.
2. Previsão de gargalos por berço, porão, bloco, pista, linha, fila e equipamento.
3. EVP/event streaming versionado para todos os domínios operacionais.
4. Telemetria integrada a equipamentos e dispositivos reais de produção.
5. Controle aduaneiro e documental ampliado conforme integrações do terminal.

## Critérios de aceite gerais

1. Persistir cada comando operacional no módulo proprietário.
2. Validar autorização antes de qualquer alteração.
3. Exigir motivo e usuário nas ações administrativas e de exceção.
4. Retornar erro funcional estável para conflito e regra de negócio, sem `500` técnico.
5. Manter uma única origem de API para o frontend.
6. Impedir chamadas HTTP internas entre módulos incorporados.
7. Garantir idempotência em consumidores, EDI e comandos sujeitos a retry.
8. Manter compatibilidade de rollback durante a janela definida.
9. Atualizar este arquivo e o registro de implementados na mesma entrega.
