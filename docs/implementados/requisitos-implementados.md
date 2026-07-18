# Requisitos implementados - CloudPort

Atualizado em 2026-07-18 com base nas entregas incorporadas à branch `main`.

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/implementados/requisitos-implementados.md`.

Não criar documentos de evidência, logs, históricos ou rascunhos nesta pasta. Toda entrega concluída deve ser removida de `docs/requisitos/modulo-navios-back-front-gaps.md` e registrada aqui sem duplicação.

## 1. Arquitetura e runtime canônico

1. O ponto de entrada oficial do backend é `backend/cloudport-runtime`.
2. O runtime incorpora Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.
3. `backend/cloudport-monolito-navio` permanece somente como rollback intermediário.
4. O build produz um único JAR executável e uma única imagem do backend.
5. `backend/cloudport-contracts` fornece paginação, erros, comandos motivados, eventos versionados e enums compartilhados.
6. O runtime usa uma única conexão PostgreSQL e oito schemas com históricos Flyway independentes.
7. Segurança, CORS, Jackson, erros, correlação, métricas, tracing, jobs e conversores comuns são centralizados.
8. A comunicação interna entre módulos incorporados usa portas e adaptadores locais.
9. Adaptadores HTTP internos permanecem condicionados ao modo de rollback.
10. Escritas e jobs podem ser controlados por propriedades do runtime.
11. Jobs críticos usam exclusão distribuída no PostgreSQL quando aplicável.
12. Testes ArchUnit impedem ciclos, acesso direto a repositories de outro módulo e uso indevido de adaptadores HTTP pelo runtime.
13. O runtime publica health, métricas e Prometheus.
14. O OpenAPI consolidado usa `operationId` único e segurança comum.

## 2. Build, Docker e implantação

1. O reator Maven inclui `cloudport-contracts`, os módulos operacionais e o runtime.
2. O `backend/Dockerfile` suporta o contexto `/backend` usado pelo EasyPanel.
3. O Dockerfile instala o parent Maven antes de compilar o reator.
4. A imagem inclui Carga Geral, contratos, migrações e recursos dos módulos.
5. O diretório `/var/lib/cloudport/documents` é preparado para persistência de documentos.
6. O health check do backend usa `/actuator/health/readiness` e possui período inicial ampliado.
7. O `frontend/Dockerfile` suporta o contexto `/frontend`.
8. O frontend é compilado com Node 22 e publicado pelo Nginx.
9. O Nginx possui fallback para SPA e endpoint `/health`.
10. O Compose consolidado inclui PostgreSQL, RabbitMQ e Redis.

## 3. Portal React e componentes compartilhados

1. O portal principal usa React 19 e Vite 8.
2. O runtime Angular foi removido.
3. O portal preserva autenticação JWT, papéis, navegação dinâmica e menu de contingência.
4. O cliente HTTP comum propaga JWT e `X-Correlation-Id`.
5. A central global de alertas fica disponível no cabeçalho e em `/home/alertas`.
6. Alertas podem ser filtrados, reconhecidos, resolvidos e direcionados ao módulo de origem.
7. A ajuda contextual é apresentada pelo `PageHeader`, por rota e módulo.
8. A ajuda possui pesquisa, papéis, fluxo, campos, permissões, estados, bloqueios, exemplos e atalhos.
9. Os atalhos `F1`, `Shift + ?` e `Esc` são suportados.
10. Rotas novas recebem conteúdo padrão do módulo quando não existe ajuda específica.

## 4. OperationalDataGrid

1. O componente compartilhado substitui tabelas genéricas simples.
2. Implementa busca rápida sem diferenciação de maiúsculas, minúsculas ou acentos.
3. Implementa filtros combináveis por coluna.
4. Implementa ordenação de texto, número e data.
5. Implementa paginação local e contrato opcional para backend.
6. Permite ocultar, exibir, reordenar e congelar colunas.
7. Persiste layouts e visões nomeadas no navegador.
8. Permite seleção múltipla e ações em lote.
9. Exibe inspector lateral do registro.
10. Exporta CSV e Excel.
11. Neutraliza fórmulas iniciadas por `=`, `+`, `-` ou `@`, inclusive após espaços e caracteres de controle.
12. Remove o limite de oito colunas das páginas genéricas.
13. Preserva renderizações específicas, teclado e atributos de acessibilidade.

## 5. Navio e line-up

1. Criar, editar e consultar cadastro canônico de navio.
2. Criar visita de navio e controlar fases operacionais.
3. Criar itens de embarque, descarga e restow.
4. Criar plano de estiva por visita.
5. Criar eventos e resumo operacional.
6. Integrar visita, itens, reservas, ordens e posições reais do Yard.
7. Expor relatório operacional integrado básico.
8. Persistir agenda de navios e validar janelas e conflitos por berço.
9. Implementar line-up interno vertical com berços em colunas e tempo no eixo vertical.
10. Representar ETA, ETB, ETD, fase, progresso e conflitos por sobreposição.
11. Manter controles de simulação, pausa e avanço do horário.
12. Disponibilizar line-up público de navios para clientes conforme contratos do portal.

## 6. Vessel Planner

1. Persistir Bay Plan vinculado à visita canônica.
2. Exibir profile, top, section e tier views sincronizadas.
3. Permitir modo multivisão e inspector lateral por slot.
4. Permitir drag-and-drop da load list para slots.
5. Permitir movimentação entre slots.
6. Exibir legendas por POD, peso, IMO, reefer e operador.
7. Representar tampas de porão.
8. Calcular peso acumulado por stack.
9. Exibir restrições e erros diretamente no slot.
10. Representar sequência visual dos guindastes.
11. Exibir restow e overlays de estabilidade, lashing e força estrutural.
12. Validar slots dedicados, reefer, perigosos, OOG e segregação conservadora.
13. Persistir atributos BAPLIE estruturados, incluindo posição, operação, VGM, reefer, IMO, ONU, grupo de embalagem e OOG.
14. Criar plano informando `bayPlanId` e `visitaNavioId`.
15. Bloquear criação sem visita canônica válida.

## 7. Identidade canônica dos planejadores de estiva - ARCH20

1. Os planejadores de contêineres e bobinas usam Navio e VisitaNavio como fontes canônicas.
2. Cadastro e visita são versionados com controle otimista.
3. Uma porta de consulta é compartilhada por standalone, runtime e rollback.
4. Perfis estruturais de graneleiros são vinculados ao identificador canônico e versionados separadamente.
5. Planos persistem navio, visita, código da visita e versões das fontes utilizadas.
6. A compatibilidade entre navio, visita e viagem é validada.
7. Comandos são bloqueados quando a fonte canônica ou o perfil estrutural foi alterado após a criação do plano.
8. A migração `V106__identidade_canonica_planejadores_estiva.sql` registra a estrutura aditiva.

## 8. Quay Monitor e crane plan

1. Expor `GET /visitas-navio/{id}/quay-monitor`.
2. Expor `POST /visitas-navio/{id}/crane-plan`.
3. Expor `GET /visitas-navio/{id}/produtividade-cais`.
4. Persistir alocações de guindastes por visita.
5. Validar visita, período, equipamento repetido e janelas sobrepostas.
6. Validar berço, porão, work queue, POW, pool, CHE, recurso de cais e work instructions elegíveis contra o Yard.
7. Impedir reutilização da mesma work queue em duas alocações.
8. Consolidar work queues, porão, equipamento, status e movimentos.
9. Calcular produtividade planejada e realizada.
10. Usar ordens concluídas como produção realizada.
11. Permitir criar e editar alocações no frontend.
12. Consumir recursos operacionais, matriz de estados, drill-down e job lists por equipamento.

## 9. Integração Navio e Yard

1. Criar reserva de pátio vinculada ao item de navio.
2. Criar ordem real no Yard.
3. Impedir ordem ativa duplicada por visita e item.
4. Expor filas e ordens sem cobertura.
5. Sincronizar status manualmente e por evento.
6. Gerar reservas e ordens.
7. Replanejar usando posição real validada.
8. Alterar prioridade, suspender e retomar ordens.
9. Atualizar item conforme o estado real da ordem.
10. Preencher posição real, consumir reserva ao concluir e cancelar ao cancelar ordem.
11. Compensar a reserva anterior durante replanejamento.
12. Usar eventos internos versionados para Yard para Navio.
13. Manter jobs periódicos somente como reparo de divergência ou evento perdido.
14. Implementar portas locais de otimização, aplicação e compensação de plano.

## 10. Yard operacional

1. Manter mapa georreferenciado com Google Maps e grade de contingência.
2. Exibir vistas de bloco, seção lateral, scan e microvisão da pilha.
3. Exibir camadas de situação, ocupação, dwell time e reefers.
4. Exibir heatmaps de ocupação e dwell time.
5. Exibir CHEs com telemetria e atualização automática.
6. Destacar pilhas bloqueadas, interditadas, cheias, reservadas e com notas.
7. Permitir movimentação manual gráfica.
8. Permitir criar, editar e remover restrições e notas de pilha.
9. Permitir simular a movimentação antes da confirmação.
10. Persistir telemetria reefer, faixa permitida, alimentação e horário da leitura.
11. Gerar alarmes para temperatura, alimentação e leitura desatualizada.
12. Desenhar rotas entre posição atual e destino da work instruction.
13. Implementar editor gráfico de allocations com pré-visualização e confirmação motivada.
14. Persistir e publicar as alterações operacionais.

## 11. Reservas e posições reais

1. Consultar posições reais antes de reservar.
2. Armazenar bloco, linha, coluna, camada e identificador real.
3. Recusar mapa vazio, posição inexistente, ocupada ou reservada.
4. Validar bloqueio, interdição, área permitida, carga, peso, altura, camada e capacidade.
5. Persistir validade e motivo de cancelamento.
6. Expirar reservas por job protegido por lock PostgreSQL.
7. Cancelar reservas ao cancelar visita ou replanejar item.
8. Criar a nova reserva antes de cancelar a anterior.
9. Restaurar o estado anterior quando o replanejamento falha.
10. Impedir uso de reserva expirada.
11. Auditar criação, consumo, cancelamento e expiração.
12. Expor histórico em `/yard/patio/reservas/auditoria`.

## 12. Inventory Management canônico

1. Implementar ciclo de vida completo da unidade.
2. Unificar contêiner, chassi, carreta e acessórios.
3. Cadastrar tipos ISO, dimensões, capacidades, prefixos e equivalências.
4. Controlar lacres e documentos.
5. Controlar avarias, componentes, condições e histórico.
6. Controlar manutenção e reparo.
7. Controlar holds e permissions.
8. Controlar ownership, operador e vínculos.
9. Controlar montagem e desmontagem de equipamentos.
10. Manter histórico de atributos.
11. Controlar reefer e registros de temperatura.
12. Executar inventário físico e registrar divergências.
13. Expor API canônica, migrações, interface React e testes.

## 13. Work queues e Equipment Control

1. Listar, criar, ativar e desativar work queue.
2. Associar POW, pool, CHE e ordens.
3. Expor job list e executar dispatch.
4. Aplicar limite real de ordens no dispatch.
5. Resetar, cancelar, suspender, retomar, bloquear e concluir work instruction conforme matriz oficial.
6. Validar fila ativa, POW, pool, plano de guindaste, recurso de cais e equipamento operacional.
7. Resolver o equipamento real por ID ou identificador.
8. Auditar motivo, usuário, origem e `correlationId`.
9. Expor drill-down e job lists por equipamento.
10. Impedir vínculo automático quando existe mais de uma fila compatível.
11. Publicar eventos após a persistência dos comandos.

## 14. Control Room e telemetria

1. Exibir painel Navio, Yard e Quay com filtros, movimentos, filas, reservas, ordens, alertas e exceções.
2. Executar reservas, ordens, sincronização, replanejamento, prioridade, suspensão e retomada.
3. Exibir work queues e job lists expansíveis.
4. Executar dispatch e transições oficiais.
5. Solicitar motivo nas mutações operacionais.
6. Integrar ao portal por SSO com origem restrita.
7. Manter login próprio como fallback.
8. Propagar JWT, usuário, origem, correlação e tracing.
9. Atualizar o estado principal por SSE autenticado.
10. Enviar snapshot inicial, eventos versionados e heartbeat.
11. Reconectar com backoff e usar polling somente como contingência.
12. Exibir equipamentos, status, posição, conectividade, VMT e work instruction atual.
13. Manter histórico de telemetria por equipamento.
14. Detectar telemetria atrasada, heartbeat ausente, falha de dispositivo e indisponibilidade.
15. Reconhecer e resolver alarmes técnicos.
16. Registrar indisponibilidade, motivo, início, encerramento e responsáveis.

## 15. Gate operacional

1. Implementar facilities, múltiplos Gates, pistas e lane monitor.
2. Configurar estágios, transições e business tasks.
3. Implementar bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos.
4. Implementar appointments e capacidade por janela.
5. Implementar truck visits com múltiplas transações.
6. Implementar troubles, inspeções, anexos, tickets, EIR e transferências.
7. Aplicar regras de acesso de motorista, transportadora e veículo.
8. Integrar OCR, balança, inspeção e liberação.
9. Controlar entrada e saída de pessoas com histórico de movimentações.
10. Suportar retirada direta de veículo desembarcado do navio pelo Gate.

## 16. Gate visual

1. Exibir quadro visual de pistas e filas por estágio.
2. Exibir calendário de agendamentos.
3. Comparar ocupação e capacidade por janela.
4. Exibir jornada gráfica do veículo.
5. Indicar OCR, balança, inspeção e liberação.
6. Exibir documentos, imagens e avarias.
7. Imprimir e reimprimir EIR.
8. Exibir painel de transações com problema.
9. Exibir cronômetro e SLA por atendimento.
10. Integrar a central visual ao dashboard do Gate.

## 17. Rail operacional e visual

1. Criar visitas ferroviárias e controlar chegada, operação e partida.
2. Representar locomotivas e vagões em sequência.
3. Associar contêineres de carga e descarga aos vagões.
4. Exibir progresso por vagão.
5. Representar linhas ferroviárias e ocupação.
6. Permitir replanejamento visual por drag-and-drop e seletor acessível.
7. Indicar vagão bloqueado ou incompatível.
8. Exibir cronograma e conflitos entre trens e recursos.
9. Implementar line-up ferroviário vertical com etapas de recepção, operação e expedição.
10. Suportar locomotiva recebida pela malha, desacoplada da tripulação e posteriormente embarcada em navio.

## 18. Carga geral, projeto e break-bulk

1. Incorporar `servico-carga-geral` ao runtime modular.
2. Implementar Bill of Lading e itens do conhecimento.
3. Implementar cargo lots para carga solta, projeto e break-bulk.
4. Cadastrar commodities, embalagens, produtos, códigos de armazenagem e manuseio.
5. Cadastrar mercadorias perigosas, faixas de temperatura e tipos de avaria.
6. Controlar quantidade, volume e peso previsto e em estoque.
7. Registrar carga e descarga parcial.
8. Registrar movimentações e avarias.
9. Implementar consolidação e desconsolidação.
10. Vincular lote, veículo, navio, armazém e cliente.
11. Expor API, Flyway, runtime e console React.

## 19. Billing e CAP

1. Cadastrar tarifas por operação e vigência.
2. Gerar cobrança idempotente para atendimentos concluídos.
3. Consolidar cobranças pendentes em faturas.
4. Persistir faturas, itens e pagamentos.
5. Quitar faturas automaticamente quando o total é pago.
6. Isolar dados da transportadora pelas informações do JWT.
7. Expor resumo CAP com agendamentos, cobranças e faturas.
8. Adicionar telas, rotas e navegação por perfil.

## 20. Visibilidade e alertas

1. Consolidar rastreamento e histórico de contêineres.
2. Persistir eventos de Gate, Yard, Rail e Navio.
3. Criar projeção quando o evento chega antes do cadastro.
4. Resolver alertas de atraso após confirmação de chegada.
5. Calcular throughput do Gate por ciclos reais.
6. Exigir motivo para resolver alertas.
7. Expor dashboard, navios, ocupação do Yard, throughput, alertas e track de contêiner.
8. Consultar alertas com paginação e filtros.
9. Expor resumo agregado por severidade.
10. Reconhecer e resolver alertas com data e usuário.
11. Impedir reaplicação de eventos por `eventId` ou `messageId`.
12. Rejeitar colisão de identidade com payload divergente.
13. Executar deduplicação, projeção e histórico na mesma transação.
14. Restringir jobs de dashboard a `cloudport.runtime.jobs-enabled=true`.

## 21. EDI e integrações externas

1. Processar BAPLIE, COPRAR, COARRI e VERMAS.
2. Validar tipo, identidade, conteúdo e atributos obrigatórios.
3. Persistir status `RECEBIDO`, `PROCESSANDO`, `CONCLUIDO`, `REJEITADO` e `QUARENTENA`.
4. Expor consulta paginada e detalhamento da auditoria.
5. Permitir reprocessamento motivado.
6. Retornar `X-EDI-Processing-Id` após recepção aceita.
7. Extrair identificadores `UNB` e `UNH`.
8. Derivar chave idempotente por tipo, intercâmbio e referência.
9. Reutilizar recepção com mesma identidade e conteúdo.
10. Rejeitar identidade reutilizada com conteúdo divergente.
11. Persistir antes de retornar `202 Accepted`.
12. Executar processamento por worker persistente fora da requisição.
13. Reivindicar mensagens com trava transacional e lote limitado.
14. Aplicar retentativa exponencial e recuperação de execução interrompida.
15. Enviar falhas esgotadas para quarentena.
16. Atualizar VGM separadamente do peso bruto do contêiner.
17. Publicar API pública protegida por cliente e segredo.
18. Publicar SSE e WebSocket versionados para integrações autorizadas.

## 22. Segurança, erros e observabilidade

1. Não armazenar senha no `localStorage`.
2. Preservar integralmente a senha digitada durante autenticação.
3. Usar cadeia stateless com JWT e autorização por papéis.
4. Comparar chaves internas e segredos externos em tempo constante.
5. Proteger `/api/public/v1/**` por cliente ou aplicação.
6. Padronizar erro com código, mensagem, detalhes, status, caminho, timestamp e `correlationId`.
7. Gerar e propagar `X-Correlation-Id` e `traceparent`.
8. Registrar logs estruturados por módulo, operação e resultado.
9. Publicar métricas de contagem e duração com tags de baixa cardinalidade.
10. Retornar `503` quando uma integração obrigatória está indisponível.
11. Desabilitar Open Session in View.
12. Exigir motivo e usuário autenticado nas mutações operacionais aplicáveis.

## 23. Estabilidade operacional - BUS20

1. Remover valores hidrostáticos sintéticos e GM padrão.
2. Exigir versões identificáveis dos dados hidrostáticos e de resistência.
3. Calcular peso total, LCG, TCG, VCG, GM, calado, trim e banda com dados reais.
4. Calcular força cortante e momento fletor por seções.
5. Usar coordenadas físicas persistidas.
6. Marcar cálculo incompleto como simulação não operacional.
7. Bloquear aprovação quando faltam entradas obrigatórias.
8. Persistir versões, memória de cálculo, resultados e aprovação.
9. Invalidar aprovação quando o plano ou a distribuição muda.

## 24. Requisitos técnicos concluídos identificados

- `ARCH10`: otimização Yard por porta local;
- `ARCH20`: identidade canônica dos planejadores de estiva;
- `DATA10`: crane plan validado contra a fonte real do Yard;
- `STATE10`: estado operacional oficial de work queues e work instructions;
- `UI20`: Quay Monitor operacional;
- `INIT10`: runtime canônico e rollback coerente;
- `ASYNC10`: idempotência dos consumidores de Visibilidade;
- `ASYNC20`: recepção HTTP EDI assíncrona e idempotente;
- `ASYNC30`: eventos internos e reconciliação seletiva;
- `ASYNC40`: agendamentos de Visibilidade condicionados;
- `ASYNC80`: jobs do Navio Siderúrgico fail-closed;
- `INT20`: atributos operacionais e de segurança do BAPLIE;
- `BUS20`: estabilidade operacional versionada;
- `UI60`: criação do Vessel Planner vinculada à escala.

## 25. Itens que não devem voltar como pendência principal

1. Runtime modular canônico e oito schemas Flyway.
2. Portas locais entre módulos incorporados.
3. Grade operacional compartilhada.
4. Central global de alertas e ajuda contextual.
5. Line-up vertical de navios e trens.
6. Vessel Planner em múltiplas vistas.
7. Quay Monitor e crane plan persistido.
8. Yard visual com vistas, heatmaps, telemetria, reefers, rotas e allocations.
9. Gate operacional e Gate visual.
10. Rail visual e composição do trem.
11. Inventory Management canônico.
12. Carga geral e break-bulk.
13. Billing e CAP.
14. Control Room por SSE com Equipment Control e telemetria.
15. EDI assíncrono, idempotente, auditável e com quarentena.
16. Idempotência de eventos da Visibilidade.
17. Dockerfiles de backend e frontend compatíveis com EasyPanel.

As pendências atuais devem ser consultadas em `docs/requisitos/requisito-tecnico.md` e `docs/requisitos/modulo-navios-back-front-gaps.md`.
