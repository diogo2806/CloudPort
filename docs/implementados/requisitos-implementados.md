# Requisitos implementados - CloudPort

Status: atualizado em 2026-07-20 com a conclusão dos BUS1350 e BUS1360 no PR #612, dos BUS1400 e BUS1410 no PR #607, além dos BUS1320, BUS1310, BUS1300, BUS1290, BUS1380, BUS1390, BUS10, BUS1030, BUS1040 e BUS1070, da seção Navio e ferrovia e da prova automatizada do corte operacional do monólito modular.

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

## Prontidão de berço e eventos de guindaste — BUS1350 e BUS1360

1. Cada escala atracada possui checklist versionado de prontidão com berço, calado, defensas, amarração, acesso, recursos, restrições, liberações, observações, responsável e instante.
2. Cada nova confirmação cria uma versão imutável e preserva o histórico integral da escala.
3. A transição de `ATRACADO` para `OPERANDO` utiliza bloqueio pessimista e exige que a versão mais recente esteja integralmente pronta.
4. A resposta funcional informa exatamente os itens críticos pendentes e impede alteração parcial da fase.
5. Paralisações planejadas e operacionais são persistidas por execução e guindaste com início, fim, motivo, impacto, turno, pendências, observações e responsável.
6. Paralisações planejadas exigem fim; paralisações operacionais podem permanecer abertas até a liberação do equipamento.
7. Intervalos sobrepostos são rejeitados e uma restrição parcial permite somente uma paralisação aberta por execução e guindaste.
8. O início de movimento é bloqueado quando seu guindaste estiver em paralisação ativa no instante informado.
9. O encerramento da paralisação registra fim, responsável e observação sem sobrescrever o evento original.
10. O handover registra turno de origem, turno de destino, responsável que recebe, pendências e instante da passagem.
11. Eventos operacionais possuem controle otimista de versão e permanecem separados do plano aprovado, preservando a reconciliação entre planejado e realizado.
12. O Control Room reúne prontidão, bloqueios, execução, métricas, paralisações, handover e linha do tempo, mantendo disponível o monitor incorporado legado.
13. O console continua operacional quando a URL do iframe não está configurada e restringe comandos aos perfis autorizados.
14. A tela possui manual contextual com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos, atalhos e link para o processo completo.
15. As migrations `V4__prontidao_operacional_berco.sql` e `V220__paralisacoes_handover_guindastes.sql` criam tabelas, restrições, índices, histórico e controles de concorrência.
16. Testes unitários cobrem ausência e conclusão da prontidão, paralisação planejada, bloqueio de movimento por paralisação e handover com pendências.

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

## Planejamento preditivo de pátio — BUS1400 e BUS1410

1. O `PredictiveSchedulerService` registra propostas persistidas por unidade no agregado `PlanoPosicaoOperacional`, vinculando posição, equipamento sugerido, horizonte operacional, validade, origem e assinatura reproduzível da entrada.
2. Os planos utilizam os estados `TENTATIVO`, `DEFINITIVO`, `IMINENTE`, `EXPIRADO` e `CANCELADO`, com transições controladas e motivo obrigatório.
3. A versão otimista do plano impede sobrescrita concorrente, e cada conversão registra estado anterior, estado novo, motivo, operador, instante e versão no histórico.
4. Planos vencidos são expirados antes da consulta ou utilização operacional e não podem ser convertidos sem novo cálculo.
5. Durante o dispatch, uma posição tentativa é revalidada na mesma transação; a conversão para definitiva só ocorre quando o plano está válido e o destino da work instruction coincide integralmente com a posição planejada.
6. Expiração, divergência de destino ou estado incompatível retornam conflito funcional e impedem o dispatch.
7. A API autenticada permite listar planos, filtrar por estado ou bloco, consultar histórico e executar conversões motivadas.
8. O Yard Impact projeta horizontes configuráveis de seis a vinte e quatro horas com base em posições, capacidade, inventário, reservas, planos ativos, work queues, work instructions e equipamentos operacionais.
9. A projeção consolida entradas, saídas, rehandles, reservas, ocupação atual e futura, percentual projetado, saturação e motivos de bloqueio por bloco.
10. A demanda e a cobertura de CHE são calculadas por POW, indicando déficit quando a quantidade requerida supera a disponibilidade operacional.
11. O drill-down identifica unidade, posição, movimento, estado, POW, fila e equipamento responsáveis por cada impacto previsto.
12. As telas `Pátio > Planejamento de recebimento` e `Pátio > Yard Impact` apresentam filtros, badges de estado, conversão motivada, validade, horizonte temporal, comparação atual versus futuro, saturação, déficit e detalhamento operacional.
13. As telas possuem ícone de manual contextual com finalidade, fluxo operacional, explicação dos campos, permissões, estados, motivos de bloqueio, exemplos, atalhos e link para o processo completo.
14. A migration `V219__planejamento_preditivo_yard_impact.sql` cria as tabelas de plano e histórico, restrições, índices e controle de versão.
15. Testes unitários cobrem conversão transacional válida, expiração e divergência entre o destino da work instruction e a posição planejada.

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
15. O BUS1310 persiste o handoff bilateral de custódia em exchange areas com unidade, área, posição, equipamento, operador, condição, lacres e instante em cada lado da transferência.
16. A entrega cria uma custódia `ENTREGUE`; o recebimento usa bloqueio pessimista e controle otimista de versão para impedir mudança concorrente ou repetida.
17. Chaves idempotentes independentes preservam o resultado de entrega e recebimento e rejeitam reutilização com conteúdo diferente.
18. Uma restrição parcial no PostgreSQL permite somente uma custódia ativa, `ENTREGUE` ou `DIVERGENTE`, por unidade.
19. Unidade, área, posição, condição e conjunto normalizado de lacres são conferidos na segunda confirmação sem depender da ordem digitada dos lacres.
20. Conferência equivalente conclui a custódia como `RECEBIDA`; divergência persiste a segunda leitura, altera o estado para `DIVERGENTE` e mantém bloqueio com memória detalhada dos campos incompatíveis.
21. Eventos versionados distinguem entrega, recebimento e divergência de custódia na exchange area.
22. A API autenticada permite listar custódias, registrar entregas e confirmar recebimentos para os perfis operacionais autorizados.
23. O painel em Pátio > Indicadores mostra entregues, recebidas, divergentes e bloqueadas, preserva a chave idempotente em retentativas e possui manual completo com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos, atalhos e processo completo.
24. Testes unitários cobrem entrega, recebimento equivalente e divergência bloqueante.
25. O BUS1320 exige confirmação física de `GROUNDING` ou `UNGROUNDING` antes da conclusão de cada work instruction executada pelo VMT.
26. A confirmação valida a leitura da unidade, o CHE real associado à work queue, o estado operacional do equipamento, a origem, o destino, a posição física e a sequência da job list.
27. Eventos duplicados, timestamps fora de ordem, CHE divergente, posições incompatíveis e instruções anteriores abertas são rejeitados antes de qualquer alteração de inventário.
28. Grounding registra ou atualiza a unidade na posição prevista e libera a reserva correspondente; ungrounding remove a unidade da posição atual e atualiza seu destino e estado.
29. Inventário, evento VMT, histórico e conclusão da work instruction são persistidos na mesma transação.
30. A tela Pátio > Lista de trabalho substitui a conclusão genérica por formulário explícito de transferência física, restrito aos perfis operacionais autorizados.
31. A tela possui manual contextual e documentação completa com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos, atalhos e processo completo.

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
13. O BUS1290 persiste credenciais operacionais do motorista vinculadas à transportadora, com tipo, hash, salt, vigência, revogação e auditoria.
14. Documento, PIN ou credencial são conferidos contra motorista, transportadora e chave operacional da truck visit ou do agendamento.
15. Cada tentativa é persistida com método, resultado, motivo, operador e instante.
16. Três tentativas inválidas bloqueiam a verificação por 15 minutos; aprovações e overrides expiram após 30 minutos.
17. O avanço da truck visit e o processamento de entrada são interceptados antes da execução e rejeitados quando a verificação está pendente, bloqueada ou expirada.
18. O override é restrito a `ADMIN_PORTO`, exige motivo e preserva responsável, instante e histórico.
19. A tela Gate > Operação completa mostra estado, método, tentativas restantes, bloqueio e validade, desabilitando o avanço até a autorização.
20. A API autenticada permite consultar e validar por visita ou agendamento e cadastrar credenciais administrativas.
21. A tela possui ajuda contextual e manual com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos, atalhos e processo completo.
22. O BUS1300 integra a confirmação de chegada antecipada à fila persistida de pré-gate dentro da mesma transação operacional.
23. A inclusão reutiliza a fila ativa do GatePass e a restrição única do banco, impedindo duplicidade de visita ou posição ativa.
24. A ordenação preserva posição original, posição atual e prioridade normal, alta ou emergencial, com justificativa e operador nas mudanças manuais.
25. O ciclo da chamada persiste gate ou pista, validade, aceite, expiração, rechamada, cancelamento, início e conclusão do atendimento.
26. A expiração ou o cancelamento retorna o veículo ao estado de espera sem perder posição e prioridade; a rechamada mantém o histórico e a contagem de tentativas.
27. A entrada física remove a fila de entrada e cria a fila de saída de forma idempotente, sem criar outra visita ativa.
28. A central do Gate exibe posição, prioridade, chamada, pista, tempo restante, aceite, rechamadas e ações compatíveis com o estado.
29. A central possui ajuda contextual e manual específico da fila de pré-gate com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos, atalhos e processo completo.
30. Testes unitários cobrem a inclusão após chegada antecipada e o bloqueio de reinclusão quando a entrada física já foi registrada.

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
14. O BUS1030 integra o fluxo de entrada e saída do Gate às reservas parciais de carga geral por BL, delivery order e cargo lot.
15. A reserva é criada antes do processamento físico e carrega `commandId`, agendamento, lote, tipo de movimento, quantidade, volume e peso.
16. Entregas parciais são confirmadas após a entrada autorizada; retiradas parciais permanecem reservadas na entrada e são confirmadas somente após a saída autorizada.
17. Os modos HTTP e local implementam a mesma porta de reserva, confirmação e compensação.
18. Confirmações e compensações usam comandos determinísticos e idempotentes; falhas do fluxo local acionam compensação automática sem ocultar a exceção original.
19. O contrato legado com `reservaCargaGeralId` e `commandIdCargaGeral` permanece aceito, e a resposta informa o identificador, o estado e o estágio esperado da reserva.
20. O BUS1040 persiste allocation de cargo lot com origem, destino, recurso, prioridade, restrições, quantidades e vínculo com a reserva de capacidade do Yard.
21. A capacidade por posição considera simultaneamente o saldo confirmado e as reservas pendentes, evitando sobrealocação de quantidade, volume ou peso.
22. A confirmação física debita a posição de origem e credita a posição de destino na mesma transação do Yard, com bloqueio pessimista e rejeição de saldo insuficiente.
23. O saldo confirmado é persistido por cargo lot e posição, com carga inicial das reservas históricas já confirmadas.
24. A API expõe consulta dos saldos por posição e os adaptadores HTTP e local propagam a origem da allocation para a transferência correta.
25. O BUS1070 usa `AvariaOperacionalCarga` como agregado canônico para código, descrição, parcela afetada, responsável, evidência inicial, inspeção, decisão e histórico.
26. A abertura bloqueia somente a quantidade, o volume e o peso afetados, preservando o saldo total e expondo o saldo disponível após a segregação.
27. O encerramento exige relatório de inspeção e só é permitido no estado `EM_TRATAMENTO`.
28. O resultado `REINTEGRAR` libera o saldo reparado, `BAIXAR` remove a perda do estoque e `MANTER_BLOQUEADA` conserva a parcela indisponível.
29. O portal possui inspector com saldo total, segregado e disponível, evidência inicial, relatório, decisão e histórico operacional.
30. O endpoint simplificado `POST /api/carga-geral/lotes/{id}/avarias` retorna `410 Gone` e direciona ao contrato canônico `/api/carga-geral/intermodal/avarias`.
31. A tela possui ajuda contextual e manual operacional com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos, atalhos e referências do processo completo.
32. Testes de domínio cobrem o ciclo completo, a obrigatoriedade da inspeção e a manutenção do bloqueio.
33. O BUS1380 exige pesagem física após a execução integral do stuffing, persistindo método, tara, peso bruto, VGM, capacidade máxima, equipamento, responsável, instante e observação.
34. O método 1 reconcilia peso bruto e VGM; o método 2 reconcilia tara, carga executada e VGM com tolerância operacional de 1 kg.
35. VGM ausente, inconsistente ou superior à capacidade máxima impede a conclusão e mantém o contêiner bloqueado para embarque.
36. O BUS1390 persiste `ProgramacaoDocaCarga` com doca, área de espera, recurso, contêiner, janela operacional, cargo lots, auditoria e estados `RESERVADA`, `EM_USO`, `CONCLUIDA` e `CANCELADA`.
37. Sobreposições de doca, área de espera, recurso, contêiner ou cargo lot são rejeitadas no serviço e protegidas por restrições de exclusão `gist` no PostgreSQL.
38. O início exige plano liberado, programação reservada e janela aberta; apontamentos exigem programação em uso.
39. Alterações do plano são bloqueadas enquanto houver programação ativa, preservando a correspondência entre cargo lots planejados e reservados.
40. Conclusão ou cancelamento libera doca, área de espera, recurso, contêiner e cargo lots na mesma transação do fluxo operacional.
41. O portal possui agenda de docas e staging com estado da janela, conflitos explicados, reprogramação, cancelamento e manual contextual completo.
42. Testes cobrem o ciclo da programação, bloqueio antes da janela, cancelamento prévio e exigência de staging na orquestração.

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
6. Painel unificado de mensagens EDI no portal React (`/home/integracoes/edi`), com recepção, tentativas, erro, correlação e efeito produzido por mensagem, filtros por tipo e status, paginação do backend, inspeção detalhada e reprocessamento motivado via `GET/POST /api/edi/processamentos*`.

## Integrações e API pública

1. Tela de diagnóstico dos contratos `/api/public/v1/*` no portal React (`/home/integracoes/api-publica`), com catálogo dos contratos GET expostos, execução real autenticada pelos headers `X-CloudPort-Client-Id`/`X-CloudPort-Client-Secret`, validação de parâmetros obrigatórios, status, latência, correlationId e histórico da sessão.
2. Abas `INTEGRACOES` semeadas em `configuracoes_navegacao` (migração `V305`), restritas a `ROLE_ADMIN_PORTO` e `ROLE_PLANEJADOR` (painel EDI) e `ROLE_ADMIN_PORTO` (diagnóstico da API pública).

## Frontend compartilhado

1. `OperationalDataGrid` com busca, filtros, ordenação e paginação.
2. Colunas configuráveis, visões nomeadas, seleção múltipla e inspector.
3. Exportação CSV e Excel com neutralização de fórmulas.
4. Navegação por teclado e atributos de acessibilidade.
5. Ajuda contextual, central global de alertas e navegação dinâmica.
6. Sessão JWT invalidada de forma consistente em respostas `401`.
