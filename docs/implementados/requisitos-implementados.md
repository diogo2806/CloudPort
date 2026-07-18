# Requisitos implementados - CloudPort

Status: atualizado em 2026-07-18 com a conclusão do BUS10, da seção Navio e ferrovia e a prova automatizada do corte operacional do monólito modular.

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
8. Escritas, jobs e consumidores podem ser desabilitados separadamente durante o corte.
9. O runtime anterior exige `CLOUDPORT_ROLLBACK_ENABLED=true` e permanece fail-closed por padrão.
10. O controle de corte do runtime canônico inicia fail-closed e somente libera comandos de escrita quando `cloudport.runtime.cutover-writes-enabled=true` é informado explicitamente.
11. Instâncias sem permissão de escrita rejeitam `POST`, `PUT`, `PATCH` e `DELETE` com `503` e código `RUNTIME_SOMENTE_LEITURA`.
12. `GET /operacao/corte` expõe, de forma autenticada, identidade da instância, revisão, papel, controles de escrita, jobs e consumidores, adaptadores internos e schemas incorporados.
13. `deploy/cloudport-runtime/provar-corte.sh` executa uma prova reproduzível com instância canônica, observador somente leitura, oito schemas, Flyway, Redis, RabbitMQ, TOS, OpenAPI, persistência após reinício, documentos e rollback sobre o mesmo banco.
14. A prova interrompe escritor, jobs e consumidores canônicos antes de iniciar o runtime anterior, valida leitura sem downgrade e bloqueia escrita durante o ensaio de rollback.
15. Após o ensaio, o runtime canônico é restaurado e o dado criado antes do rollback é novamente consultado.
16. O workflow `Validar CloudPort` publica o relatório, os estados das instâncias, as contagens Flyway e os diagnósticos da prova como artefato.

## Maven, banco e configuração

1. `backend/cloudport-modules` funciona como reator Maven canônico.
2. Cada módulo publica migrações e mantém ownership do próprio schema.
3. Flyway executa de forma independente por schema, com validação e estratégia aditiva.
4. O backend usa `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` e `DB_SCHEMA`.
5. JWT usa `SECURITY_JWT_SECRET` e `SECURITY_JWT_EXPIRATION_MS`.
6. O administrador inicial exige `ADMIN_EMAIL` e `ADMIN_PASSWORD` explícitos.
7. O administrador funcional padrão inseguro foi removido.
8. Frontend e backend possuem Dockerfiles e configuração para EasyPanel.

## Infraestrutura transversal e segurança

1. Segurança stateless, JWT, roles, CORS e autorização por perfil.
2. Erro padronizado com código, mensagem, status, caminho, timestamp e `correlationId`.
3. Propagação de `X-Correlation-Id`, `traceId` e `traceparent`.
4. Logs estruturados, métricas, health, liveness e readiness.
5. Scheduler, tratamento de erros e locks distribuídos para jobs críticos.
6. APIs públicas protegidas por cliente e segredo configuráveis.
7. SSE e WebSocket/STOMP autenticados e autorizados.
8. Logs e exceções da integração TOS são sanitizados.
9. A execução standalone de Carga Geral é protegida por JWT e roles.
10. Jobs e consumidores falham fechados quando não são habilitados explicitamente.
11. O job de recuperação OCR do Gate só é registrado no serviço standalone e no runtime consolidado quando `cloudport.runtime.jobs-enabled=true`; sem a propriedade, não há polling, consulta, reivindicação nem submissão de documentos.

## Contratos compartilhados e API

1. Paginação, erro, comando motivado, envelope de evento e enums externos estão em `cloudport-contracts`.
2. DTOs resumidos e detalhados foram separados.
3. Filtros paginados são executados no banco nos fluxos migrados.
4. A API pública usa whitelist de campos, ordenação e contratos versionados.
5. Tipos TypeScript podem ser gerados a partir do OpenAPI.
6. Eventos externos usam envelope versionado e correlação.

## Navio, visitas e Vessel Planner

1. Cadastro canônico de navios e visitas operacionais.
2. Itens de embarque, descarga e restow.
3. Planos de estiva vinculados às visitas.
4. Line-up interno, vertical e público.
5. Quay Monitor, crane plan e produtividade planejada versus realizada.
6. Vistas profile, top, section e tier sincronizadas.
7. Drag-and-drop, inspector de slot e movimentação validada pelo backend.
8. Legendas por POD, peso, IMO, reefer e operador.
9. Tampas de porão, peso por stack, segregação IMDG e restrições nos slots.
10. Restow, sequência visual de guindastes e overlays técnicos.
11. Hidrostática e resistência longitudinal versionadas.
12. Peso, centros de gravidade, GM, calado, trim, banda, força cortante e momento fletor.
13. Aprovações versionadas e invalidadas quando o plano muda.
14. BAPLIE com posição, operação, VGM, reefer, perigosos e OOG.

## Execução operacional do Vessel Planner — BUS1150, BUS1160 e DATA1180

1. A sequência de guindastes possui execução persistida por movimento, com guindaste, ordem, janela, início, conclusão, falha, replanejamento, reconciliação e quantidade realizada.
2. A linha do tempo operacional apresenta progresso, exceções e resultado real sem substituir o plano aprovado.
3. Tampas de porão possuem entidades de tampa, posição, tarefa e dependência com operações de abrir, remover, posicionar e fechar.
4. Dependências incompletas e estados incompatíveis impedem movimentos de guindaste relacionados.
5. A reconciliação compara BAPLIE, plano aprovado, inventário e execução física, incluindo posição e peso confirmados pelo COARRI.
6. Divergências são persistidas por contêiner e fonte, mantendo separados os valores de BAPLIE, plano, inventário e execução.
7. Divergências críticas abertas bloqueiam aprovação, publicação e conclusão do plano.
8. A resolução registra decisão, justificativa, usuário e instante sem sobrescrever silenciosamente os dados de origem.
9. Divergências são reabertas quando as fontes mudam e resolvidas automaticamente quando voltam a ficar consistentes.
10. O Vessel Planner disponibiliza fila visual e endpoints autenticados para consulta, reconciliação e resolução.

## Operações administrativas de Navio

1. A visita e seus itens podem ser editados no portal pelos contratos `PUT` existentes.
2. Plano validado pode ser concluído e publicado por comando motivado.
3. Plano validado ou publicado pode ser invalidado, preservando a versão e o histórico.
4. Plano sem execução física pode ser cancelado administrativamente.
5. Após conclusão, invalidação ou cancelamento, o operador pode criar uma nova versão copiando as posições anteriores.
6. Visita pode ser cancelada antes da partida, com cancelamento dos itens não operados.
7. Item não operado pode ser cancelado individualmente.
8. Reservas ligadas ao item ou à visita são canceladas pelos serviços de domínio existentes.
9. Ordens de pátio não concluídas podem ser canceladas por endpoint motivado.
10. Cancelamento de visita ou item tenta compensar as ordens associadas no Yard.
11. Falha de compensação gera evento `CANCELAMENTO_ORDEM_PATIO_PENDENTE` para reprocessamento operacional.
12. Publicação, invalidação, cancelamento e nova aprovação aparecem no histórico do inspector.
13. Motivo, usuário, origem e `correlationId` acompanham os comandos administrativos.
14. Os adaptadores locais executam comandos de Yard no mesmo processo nos runtimes consolidados.
15. O frontend bloqueia edição e comandos incompatíveis com estados terminais.

## Replanejamento operacional com otimização real

1. O endpoint `POST /visitas-navio/{id}/integracao-patio/replanejar` suporta simulação e aplicação pelo mesmo contrato autenticado e motivado.
2. O motor recebe ETA, ETB/ATB, ETD/ATD, cutoff e sequência operacional da visita.
3. O mapa real do Yard fornece bloco, linha, coluna, camada, ocupação, bloqueio, interdição, área permitida, capacidade, peso, altura e tipos de carga aceitos.
4. Reservas concorrentes de outras visitas são retiradas do conjunto de posições elegíveis.
5. A carga considera movimento, tipo, peso, altura, operador, destino, dwell time e indicadores IMO, reefer e OOG.
6. Work queues fornecem CHE, disponibilidade, produtividade estimada, prioridade, carga atual e conflitos de recurso.
7. O motor combina alocação de posição, dual-cycling e otimização de rotas de equipamentos.
8. A pontuação considera distância, ocupação, rehandles, destino, equipamento, sequência, dwell time e urgência do cutoff.
9. A proposta é determinística e recebe assinatura SHA-256 reproduzível.
10. O resultado expõe posição atual e proposta, CHE, sequência, score, rehandles, memória de cálculo e justificativas.
11. O Control Room permite simular sem persistir e somente depois confirmar a aplicação.
12. A aplicação reutiliza o fluxo idempotente existente e revalida transacionalmente ocupação, reservas, restrições, peso, altura, ordem e posição antes de persistir.
13. Conflitos encontrados na confirmação impedem alteração parcial e retornam erro funcional.
14. A interface recarrega reservas, ordens, filas e plano após uma aplicação confirmada.

## Yard, inventário e dispatch

1. Google Maps com blocos, pilhas e posições georreferenciadas.
2. Vistas de bloco, seção, scan, microvisão, heatmaps e workspaces.
3. Notas, bloqueios, interdições, permissões, rotas, reefers e allocations.
4. Reservas reais com validação de ocupação, peso, altura, apoio e capacidade.
5. Expiração, cancelamento e compensação de reservas.
6. Movimentação com simulação e confirmação validada no backend.
7. Planejamento de recebimento e otimização de posições por custo mínimo.
8. Domínio canônico de unidade e equipamento.
9. Contêiner, chassi, carreta, acessórios, tipos, prefixos e equivalências.
10. Lacres, documentos, avarias, manutenção, holds, ownership, montagem, reefer e inventário físico.
11. Work queues, POW, pool, equipamento, job list e dispatch.
12. Reset, cancelamento, prioridade, suspensão, retomada, bloqueio e conclusão.
13. Auditoria por usuário, motivo, origem e `correlationId`.
14. Reshuffling com posição real, reserva e idempotência.

## Control Room e Visibilidade

1. Painel Navio + Yard com movimentos, filas, reservas, ordens, alertas e exceções.
2. SSE autenticado, snapshot inicial, heartbeat, reconexão e fallback.
3. Equipamentos com status, posição, conectividade, VMT e work instruction atual.
4. Histórico persistido de telemetria, alarmes e indisponibilidades.
5. Reconhecimento e resolução de alarmes.
6. Ciclo de comandos remotos e heartbeat dos dispositivos.
7. Rastreamento e histórico de contêineres.
8. Projeções e eventos de Gate, Yard, Rail e Navio.
9. Central global de alertas com filtros, reconhecimento, resolução e navegação.

## Gate operacional e visual

1. Facilities, múltiplos Gates, pistas, consoles, filas e monitor de lanes.
2. Estágios, transições e business tasks configuráveis.
3. Bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos.
4. Appointments com capacidade transacional.
5. Truck visits com múltiplas transações.
6. Trouble transactions, inspeções, fotos, documentos, tickets e EIR.
7. Transferências entre instalações e regras de acesso.
8. Histórico de estágios e relatórios operacionais.
9. Quadro visual, calendário, jornada do veículo, OCR, balança, inspeção, liberação e SLA.
10. Embarque direto Gate → navio e saída direta de carga autopropelida.
11. Controle de entrada e saída de pessoas serializado por documento.
12. Rejeições transacionais conhecidas retornam `409` ou `422`, sem exposição de SQL.

## Ferrovia

1. Visitas, manifestos, vagões, contêineres e ordens.
2. Lista de trabalho, início, conclusão e partida.
3. Composição gráfica, ocupação de linhas, progresso e conflitos.
4. Line-up ferroviário vertical.
5. Locomotiva isolada tratada como visita e embarcada após custódia e checklist.
6. Replanejamento visual persistido de contêineres entre vagões com confirmação motivada no frontend.
7. Manifesto, posição do vagão e ordem ferroviária são atualizados na mesma transação com bloqueio pessimista e controle otimista de versão.
8. Capacidade, compatibilidade, estado da operação e concorrência são revalidados antes da confirmação.
9. Cada replanejamento registra origem, destino, posições, ordem do manifesto, usuário, motivo e versões anterior e atual.
10. Movimentos internos persistem visita, origem, destino, janela planejada e estados `PLANEJADO`, `AUTORIZADO`, `EM_EXECUCAO`, `CONCLUIDO` e `CANCELADO`.
11. Rotas, linhas, trechos e switches são reservados na autorização e liberados atomicamente na conclusão ou no cancelamento.
12. Sobreposições de visita ou recurso retornam conflito funcional e são protegidas por restrições de exclusão no PostgreSQL.
13. A conclusão atualiza a posição ferroviária corrente da visita.
14. A API permite planejar, autorizar, iniciar, concluir, cancelar, consultar e listar o histórico por visita.

## Carga Geral e carga siderúrgica

1. Bill of Lading, itens, cargo lots, carga solta, projeto e break-bulk.
2. Commodities, embalagens, produtos, armazenagem e manuseio.
3. Perigosos, temperatura, avarias, quantidade, volume, peso e saldo.
4. Recebimento, carga, descarga parcial, transferência, consolidação e desconsolidação.
5. Vínculos com veículo, navio, armazém e cliente.
6. Bloqueio pessimista, saldo não negativo e conflitos de unicidade traduzidos para `409`.
7. Planejamento de bobinas, porões, tank top, empilhamento e posicionamento.
8. Estabilidade, securing e aprovação baseada em evidências versionadas.
9. Materiais, pontos de amarração, capacidades e regras persistidas.
10. O BUS10 implementa operações persistidas de stuff e unstuff com bloqueio dos cargo lots, validação de saldo ou capacidade e compensação transacional no cancelamento.
11. O contêiner é validado e reservado no inventário canônico, com bloqueio pessimista, verificação de condição, estado, manutenção e holds, e liberação na conclusão ou no cancelamento.
12. Cada apontamento exige `commandId`, persiste hash do conteúdo e reutiliza o resultado aplicado em repetições equivalentes, rejeitando reutilização incompatível.
13. O portal lista apenas contêineres elegíveis do inventário canônico e preserva o mesmo `commandId` para retentativa após falha de comunicação.

## Billing e portal CAP

1. Tarifas, cobranças, faturas, itens e pagamentos.
2. Cobrança idempotente para atendimentos concluídos.
3. Consolidação de cobranças pendentes e quitação automática.
4. Isolamento por transportadora com dados do JWT.
5. Portal CAP com agendamentos, cobranças e faturas.
6. Locks transacionais impedem cobrança duplicada e pagamento acima do saldo.
7. Disputas previsíveis retornam `409 Conflict`.

## EDI e eventos

1. BAPLIE, COPRAR, COARRI e VERMAS.
2. Auditoria persistente e reprocessamento motivado.
3. Recepção assíncrona e idempotente.
4. Worker com reivindicação transacional, retentativa e quarentena.
5. Eventos internos publicados após persistência.

## Frontend compartilhado

1. `OperationalDataGrid` com busca, filtros, ordenação e paginação.
2. Colunas configuráveis, visões nomeadas, seleção múltipla e inspector.
3. Exportação CSV e Excel com neutralização de fórmulas.
4. Navegação por teclado e atributos de acessibilidade.
5. Ajuda contextual, central global de alertas e navegação dinâmica.
6. Sessão JWT invalidada de forma consistente em respostas `401`.
