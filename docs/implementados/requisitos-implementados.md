# Requisitos implementados — CloudPort

Status: atualizado em 2026-07-18 com base na branch `main`.

## Instruções obrigatórias para manutenção

Esta pasta deve manter um único arquivo: `docs/implementados/requisitos-implementados.md`.

Não criar documentos paralelos de evidência, logs, históricos ou rascunhos nesta pasta. Toda entrega concluída deve ser removida de `docs/requisitos/modulo-navios-back-front-gaps.md` e registrada aqui sem duplicação.

Este documento consolida o estado funcional e técnico entregue. As lacunas restantes estão em:

- `docs/requisitos/modulo-navios-back-front-gaps.md`;
- `docs/requisitos/requisito-tecnico.md`.

## 1. Arquitetura e runtime canônico

1. Definido `backend/cloudport-runtime` como ponto de entrada oficial do backend.
2. Consolidado um processo Spring Boot e uma origem de API.
3. Incorporados oito módulos de negócio:
   - Autenticação;
   - Carga Geral;
   - Gate;
   - Rail;
   - Visibilidade;
   - Yard e Inventário;
   - Navio;
   - Navio Siderúrgico.
4. Preservado `backend/cloudport-monolito-navio` somente como rollback intermediário.
5. Preservados os módulos `servico-*` como artefatos compiláveis para coexistência e última camada de retorno.
6. Criado `backend/cloudport-contracts` para paginação, erros, comandos motivados, eventos versionados e enums externos.
7. Centralizados segurança, CORS, Jackson, tratamento de erros, logs, métricas, tracing, OpenAPI, cache, jobs e clientes externos no runtime.
8. Impedidas por ArchUnit dependências cíclicas, acesso a repository de outro módulo e uso de adaptadores HTTP internos pelo runtime.
9. Implementadas portas locais para Navio, Yard e Autenticação, com adaptadores HTTP condicionais somente para rollback.
10. Implementados eventos internos idempotentes para sincronização seletiva entre módulos.

## 2. Persistência, Flyway e execução única

1. Configurada uma conexão PostgreSQL com oito schemas proprietários:
   - `cloudport_autenticacao`;
   - `cloudport_carga_geral`;
   - `cloudport_gate`;
   - `cloudport_rail`;
   - `cloudport_visibilidade`;
   - `cloudport_yard`;
   - `cloudport_navio`;
   - `cloudport_siderurgico`.
2. Criado um objeto Flyway e um `flyway_schema_history` por módulo.
3. Executadas migrações antes do `EntityManagerFactory`.
4. Habilitados `validateOnMigrate`, criação de schema e bloqueio de `clean`.
5. Adotado `expand and contract` para compatibilidade com rollback.
6. Controladas escritas por `cloudport.runtime.writes-enabled`.
7. Controlados jobs por `cloudport.runtime.jobs-enabled`.
8. Controlados consumidores por `cloudport.runtime.consumers-enabled` e `auto-startup` do RabbitMQ.
9. Serializados jobs críticos por `pg_try_advisory_xact_lock`.
10. Mantidos runtime canônico, rollback e standalone em modo fail-closed quando suas propriedades de execução não forem habilitadas explicitamente.

## 3. Build, Docker e implantação

1. Consolidado o reator Maven em `backend/cloudport-modules`.
2. Incluídos os oito módulos, `cloudport-contracts` e `cloudport-runtime` no build.
3. Produzido um único JAR executável e uma única imagem principal do backend.
4. Criado `backend/Dockerfile` para o contexto `/backend` usado pelo EasyPanel.
5. Mantido `backend/cloudport-runtime/Dockerfile` para build iniciado na raiz do repositório.
6. Criado `frontend/Dockerfile` multi-stage com Node 22 e Nginx.
7. Implementado fallback de SPA para `index.html`.
8. Implementados health checks:
   - backend: `/actuator/health/readiness` na porta `8080`;
   - frontend: `/health` na porta `80`.
9. Preparado diretório persistente de documentos no backend.
10. Atualizado workflow para validar os dois contextos de build Docker do backend.
11. Documentados os valores exatos de implantação no EasyPanel.

## 4. Navio e integração com Yard

1. Criadas visitas de navio.
2. Criados itens operacionais de embarque, descarga e restow.
3. Criados planos de estiva por visita.
4. Criados eventos e resumos operacionais.
5. Expostos endpoints `/visitas-navio` e contratos de integração com o pátio.
6. Criadas reservas reais vinculadas a visita, item e plano.
7. Criadas ordens de trabalho reais no Yard.
8. Impedidas ordens ativas duplicadas por visita e item.
9. Implementadas sincronização manual, automática e orientada a eventos.
10. Implementados replanejamento, prioridade, suspensão, retomada, reset e cancelamento.
11. Atualizados item, posição real e reserva conforme o estado da ordem.
12. Implementada compensação de reserva durante replanejamento.
13. Implementados cadastro canônico de navios e projeção siderúrgica vinculada por ID/IMO.
14. Implementados contratos de Quay Monitor, crane plan e produtividade de cais.
15. Validado crane plan contra visita, berço, porão, work queue, POW, pool, CHE, recurso de cais e work instructions elegíveis.

## 5. Vessel Planner gráfico

1. Implementadas vistas `profile`, `top`, `section` e `tier` sincronizadas.
2. Implementado modo multivisão e inspector lateral por slot.
3. Implementado drag-and-drop da load list para slots e entre slots.
4. Preservados atributos operacionais ao mover contêiner.
5. Implementadas legendas por POD, peso, IMO, reefer e operador quando presentes no contrato.
6. Representadas tampas de porão.
7. Calculado e exibido peso acumulado por stack.
8. Exibidas restrições, violações e alertas diretamente no slot.
9. Implementada segregação IMDG gráfica.
10. Implementadas visualização de restow e sequência por guindaste.
11. Implementados overlays de estabilidade, força estrutural e risco indicativo de lashing.
12. Reutilizada a validação de hard constraints do backend nas movimentações.
13. Implementada criação do plano vinculada obrigatoriamente à escala por `bayPlanId` e `visitaNavioId`.

## 6. BAPLIE, COPRAR, COARRI, VERMAS e EDI

1. Implementados BAPLIE, COPRAR, COARRI e VERMAS com validação, rejeição e auditoria persistente.
2. Validado código real do navio, viagem e operações obrigatórias.
3. Persistidos posição, operação, cheio/vazio, reefer, IMO, número ONU, grupo de embalagem, segregação, OOG e instruções de manuseio.
4. Preservados segmentos originais para auditoria.
5. Normalizados pesos em quilogramas e toneladas.
6. Atualizado VGM sem substituir indevidamente o peso bruto.
7. Implementada recepção HTTP assíncrona e idempotente por identificadores UNB/UNH e referência.
8. Reutilizada recepção quando identidade e conteúdo são iguais.
9. Rejeitada colisão da mesma identidade com conteúdo divergente.
10. Persistida recepção antes do retorno `202 Accepted`.
11. Retornado `X-EDI-Processing-Id`.
12. Implementados worker persistente, lock transacional, lote por ciclo, espera exponencial e recuperação de execução interrompida.
13. Implementada quarentena e reprocessamento motivado com limite de tentativas.
14. Mantidos efeito de negócio e conclusão na mesma transação.

## 7. Yard operacional

1. Implementado mapa georreferenciado com Google Maps e polígonos.
2. Implementadas vistas de bloco, seção lateral, scan e microvisão.
3. Implementadas camadas de situação, ocupação, dwell time e reefers.
4. Exibidos CHEs com telemetria e atualização operacional.
5. Destacadas pilhas bloqueadas, interditadas, cheias, reservadas e com notas.
6. Implementados workspaces salvos no navegador.
7. Implementada movimentação manual por arrastar e soltar.
8. Implementada simulação visual de origem e destino antes da confirmação.
9. Validado destino existente, livre, permitido e não reservado.
10. Implementada edição motivada de bloqueio, interdição, permissão e nota de pilha.
11. Persistida nota operacional por Flyway.
12. Implementadas rotas visuais entre origem e destino de work instruction.
13. Implementada telemetria reefer com temperatura, faixa, alimentação e instante.
14. Gerados alarmes `NORMAL`, `ATENCAO` e `CRITICO` para leitura desatualizada, desligamento e temperatura fora da faixa.
15. Implementado editor gráfico de allocations com posições elegíveis, pré-visualização e confirmação motivada.
16. Impedido replanejamento para o mesmo destino.
17. Implementados contratos e testes para movimentação, restrições, reefers e allocations.

## 8. Reservas, work queues e work instructions

1. Consultadas posições reais antes da reserva.
2. Selecionadas linha, coluna e camada reais.
3. Recusadas posição inexistente, ocupada, bloqueada, interditada ou incompatível.
4. Validados área, tipo de carga, peso, altura, camada e capacidade da pilha.
5. Implementadas validade, expiração e cancelamento motivado.
6. Criada reserva nova antes de cancelar a anterior no replanejamento.
7. Restaurado estado anterior em falha de compensação.
8. Implementadas auditorias de criação, consumo, cancelamento e expiração.
9. Listadas, criadas, ativadas e desativadas work queues.
10. Associados POW, pool, equipamento e ordens.
11. Expostas job lists e dispatch com limite real.
12. Implementados reset, cancelamento, bloqueio, suspensão, retomada e conclusão por matriz oficial de estados.
13. Validada fila ativa, plano de guindaste, recurso de cais e equipamento operacional antes do dispatch.
14. Auditado motivo, usuário, origem e `correlationId` nas mutações.
15. Implementados drill-down e job lists por equipamento no Control Room.

## 9. Inventário canônico

1. Implementado ciclo de vida completo da unidade.
2. Modelados contêiner, chassi, carreta e acessórios.
3. Modelados tipos ISO, dimensões, capacidades, prefixos e equivalências.
4. Implementados lacres e documentos.
5. Implementadas avarias, componentes e condições.
6. Implementada manutenção e reparo.
7. Implementados holds e permissions.
8. Implementados ownership e operador.
9. Implementadas montagem e desmontagem de equipamentos.
10. Implementado histórico de atributos.
11. Implementado controle reefer.
12. Implementados inventário físico e divergências.
13. Implementada importação/sincronização do inventário legado de `conteiner_patio`.
14. Exposta API `/yard/inventario/canonico`.
15. Implementado frontend unificado com filtros, indicadores, cadastro, inspector e ações rápidas.
16. Integrado o inventário ao `OperationalDataGrid`.

## 10. Gate visual e operacional

1. Implementado quadro visual de pistas e filas por estágio.
2. Implementado calendário de appointments com ocupação versus capacidade.
3. Implementada jornada do veículo com OCR, balança, inspeção e liberação.
4. Exibidos documentos, imagens, avarias e transações problemáticas.
5. Implementadas impressão e reimpressão de EIR.
6. Implementados cronômetro e classificação visual de SLA.
7. Criadas facilities, múltiplos Gates, pistas e lane monitor.
8. Criados estágios, transições e business tasks configuráveis.
9. Implementados bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos.
10. Implementados appointments com consumo transacional de capacidade.
11. Implementadas truck visits com múltiplas transações.
12. Implementadas trouble transactions, inspeções, fotografias, anexos, tickets e histórico de estágios.
13. Implementadas transferências entre instalações.
14. Implementadas regras de bloqueio/permissão para motorista, transportadora e veículo.
15. Publicado painel React em `/home/gate/operacao`.
16. Criadas migrations V300 a V303 do Gate e navegação correspondente.

## 11. Controle de entrada e saída de pessoas

1. Implementado cadastro canônico de pessoas para acesso ao terminal.
2. Implementados registros de entrada e saída.
3. Implementado estado atual dentro/fora.
4. Implementado histórico de movimentações.
5. Implementados documento normalizado, unicidade e versionamento otimista.
6. Publicados endpoints e telas operacionais correspondentes.

A serialização explícita de disputas concorrentes permanece registrada como `ERR10` em `requisito-tecnico.md`.

## 12. Billing e portal CAP

1. Criadas tabelas de tarifas, cobranças, faturas, itens e pagamentos.
2. Cadastradas tarifas por operação e vigência.
3. Geradas cobranças idempotentes para atendimentos concluídos.
4. Consolidadas cobranças pendentes em faturas.
5. Registrados pagamentos e quitação automática.
6. Isolados dados da transportadora pelos claims do JWT.
7. Criado resumo CAP com appointments, cobranças e faturas.
8. Criadas telas, rotas e navegação por perfil.
9. Criados testes dos contratos frontend.

As disputas concorrentes de faturamento e pagamento permanecem registradas como `ERR20`.

## 13. Carga Geral, projeto e break-bulk

1. Criado módulo `servico-carga-geral` incorporado ao runtime.
2. Implementados Bill of Lading e itens do conhecimento.
3. Implementados cargo lots de carga solta, projeto e break-bulk.
4. Cadastradas commodities, embalagens, produtos, códigos de armazenagem/manuseio, perigosos, temperatura e avarias.
5. Controlados quantidade, volume e peso previstos e em estoque.
6. Implementados recebimento, carga/descarga parcial, transferência, consolidação e desconsolidação.
7. Vinculados lotes a veículo, visita de navio, armazém e cliente.
8. Implementado bloqueio pessimista nas movimentações de estoque.
9. Impedido saldo negativo.
10. Exigidos número UN e classe IMDG para mercadoria perigosa.
11. Validada faixa de temperatura.
12. Criados dashboard e console React operacional.
13. Integrados Flyway, runtime, navegação e testes.

A proteção standalone e o tratamento de duplicidade concorrente permanecem registrados como `SEC80` e `ERR40`.

## 14. Ferrovia visual

1. Implementada composição gráfica do trem com locomotiva e vagões em sequência.
2. Associados contêineres de carga e descarga a cada vagão.
3. Exibido progresso individual por vagão.
4. Representadas linhas ferroviárias e ocupação.
5. Implementado planejamento visual por arrastar e soltar com alternativa acessível.
6. Indicados vagões bloqueados, incompatíveis e operações sem vagão válido.
7. Implementado cronograma de chegada, operação e partida.
8. Destacados conflitos de ocupação entre trens.
9. Preservados totalizadores quando a listagem resumida não retorna manifestos completos.

A persistência do replanejamento visual por vagão permanece como evolução funcional.

## 15. Control Room, equipamentos e telemetria

1. Criado painel Navio + Yard com filtros, movimentos, filas, reservas, ordens, alertas e exceções.
2. Implementadas ações de geração, sincronização, replanejamento, prioridade, suspensão e retomada.
3. Implementadas work queues persistentes e job lists expansíveis.
4. Implementados dispatch, reset e cancelamento.
5. Integrado o Control Room ao portal em rota autenticada.
6. Implementados SSO por `postMessage` restrito e login próprio como fallback.
7. Enviados JWT, usuário, origem e `X-Correlation-Id` nas ações.
8. Implementado SSE autenticado com snapshot inicial, heartbeat, reconexão e polling apenas como fallback.
9. Criada visão operacional de equipamentos com status, posição, conectividade, VMT e work instruction atual.
10. Mantido histórico de telemetria por equipamento.
11. Detectados telemetria atrasada, heartbeat ausente, falha de dispositivo e indisponibilidade.
12. Implementados reconhecimento e resolução de alarmes técnicos.
13. Implementado registro de indisponibilidades com início, encerramento, motivo e responsáveis.
14. Implementado ciclo de comandos remotos: criação, polling, envio, execução e confirmação.
15. Integrados dispositivos por heartbeat autenticado, firmware, protocolo, endereço e sequência.
16. Publicados painel React, filtros, indicadores, histórico, comandos, alarmes e dispositivos.
17. Implementado Quay Monitor com plano de guindastes persistido e produtividade.

## 16. Visibilidade e central global de alertas

1. Consolidado rastreamento e histórico de contêineres.
2. Persistidos eventos de Gate, Yard, Rail e Navio.
3. Implementadas projeções mesmo quando o evento chega antes do cadastro.
4. Implementada idempotência persistente por `eventId`/`messageId`, tipo e hash canônico.
5. Ignorado redelivery idêntico sem reaplicar efeito.
6. Rejeitada colisão de identidade com payload divergente.
7. Vinculado histórico ao evento externo com unicidade.
8. Publicado dashboard imediatamente após eventos e mantidos jobs somente como reparo.
9. Implementada central global de alertas no cabeçalho e em `/home/alertas`.
10. Implementados filtros, priorização, resumo por severidade, reconhecimento, resolução e roteamento ao módulo relacionado.
11. Implementada atualização periódica do frontend.
12. Padronizados erros e exigido motivo para resolução.

## 17. Grade operacional e ajuda contextual

1. Implementado `OperationalDataGrid` compartilhado.
2. Implementadas busca, ordenação, filtros, paginação, seleção múltipla e inspector.
3. Implementadas ocultação e reordenação de colunas.
4. Removido o limite de oito campos das páginas genéricas.
5. Inferidos todos os campos encontrados na resposta.
6. Implementadas exportações CSV e Excel.
7. Neutralizadas fórmulas iniciadas por `=`, `+`, `-` e `@`, inclusive após espaços e caracteres de controle.
8. Implementado SpreadsheetML com conteúdo textual e escape XML.
9. Implementada ajuda contextual em todos os cabeçalhos padrão.
10. Criado painel lateral responsivo e acessível.
11. Determinado conteúdo automaticamente por rota e módulo.
12. Implementada pesquisa sem diferenciação de acentos.
13. Exibidos papéis, fluxo, campos, permissões, estados, bloqueios, exemplos e atalhos.
14. Implementados atalhos `F1`, `Shift + ?` e `Esc`.

## 18. Segurança e contratos externos

1. Preservada senha digitada sem remoção de caracteres.
2. Removido armazenamento de senha no `localStorage`.
3. Protegidas integrações legadas por `X-CloudPort-Service-Key` com comparação em tempo constante.
4. Protegida API pública por cliente/aplicação e role `INTEGRACAO_EXTERNA`.
5. Implementada seleção segura de campos e whitelist de ordenação.
6. Padronizados erros com código, mensagem, detalhes, status, caminho, timestamp e `correlationId`.
7. Gerado e propagado `X-Correlation-Id`.
8. Criado envelope `EventoIntegracaoV1` com identidade, tipo, versão, instante, correlação, origem e dados.
9. Publicados SSE, WebSocket/STOMP e tópicos versionados para os contratos já protegidos.
10. Implementado OpenAPI consolidado com `operationId` único.

As pendências de sanitização dos logs do Gate, segurança standalone de Carga Geral e autorização dos WebSockets do Yard permanecem como `SEC70`, `SEC80` e `SEC90`.

## 19. Observabilidade, testes e operação

1. Implementados logs estruturados com módulo, operação, resultado, correlação e identificadores operacionais.
2. Propagados `X-Correlation-Id` e `traceparent` em integrações externas.
3. Publicadas métricas de contagem e duração com tags de baixa cardinalidade.
4. Expostos health, readiness, métricas e Prometheus.
5. Criados testes PostgreSQL/Testcontainers do runtime.
6. Validados Flyway e JPA reais.
7. Testados modo somente leitura, propriedades de jobs/consumidores e advisory locks.
8. Criados testes ArchUnit de limites modulares.
9. Criado smoke funcional do Compose.
10. Documentados corte, coexistência e rollback sem downgrade de banco.
11. Implementados jobs de Visibilidade e Navio Siderúrgico em modo fail-closed.
12. Mantidas reconciliações periódicas somente como reparo de eventos perdidos.

## 20. Requisitos técnicos concluídos registrados

- `ARCH10`: otimização do Yard por porta local;
- `DATA10`: validação de crane plan contra o Yard;
- `STATE10`: estado operacional oficial de work queues e work instructions;
- `UI20`: Quay Monitor operacional;
- `INIT10`: runtime canônico e rollback coerente;
- `ASYNC40`: agendamentos de Visibilidade condicionados;
- `INT20`: atributos operacionais e de segurança do BAPLIE;
- `BUS20`: estabilidade operacional versionada;
- `UI60`: criação do Vessel Planner vinculada à escala;
- `ASYNC80`: jobs do Navio Siderúrgico fail-closed;
- `SEC60`: exportação CSV/Excel protegida contra injeção de fórmula.

## 21. Principais contratos publicados

```text
GET   /assets/configuracao.json
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
GET   /yard/inventario/canonico
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
GET   /api/v1/visibilidade/dashboard
GET   /api/v1/visibilidade/alertas/filtrados
GET   /api/v1/visibilidade/alertas/resumo
PATCH /api/v1/visibilidade/alertas/{id}/reconhecer
PATCH /api/v1/visibilidade/alertas/{id}/resolver
GET   /api/carga-geral/conhecimentos
POST  /api/carga-geral/conhecimentos
```

## 22. Entregas consolidadas em 2026-07-18

1. Ajuda contextual em todas as páginas.
2. OperationalDataGrid com todas as colunas e exportação Excel segura.
3. Ferrovia visual e planejamento por vagão.
4. Control Room com telemetria e controle de equipamentos.
5. Yard operacional com vistas, movimentação, restrições, notas e workspaces.
6. Yard com rotas, telemetria reefer e editor de allocations.
7. Vessel Planner gráfico completo.
8. Inventory Management canônico.
9. Gate visual operacional.
10. Carga Geral e Break-Bulk.
11. Gate operacional completo inspirado nos fluxos do N4.
12. Billing/CAP e central global de alertas.
13. Implantação do frontend e backend no EasyPanel com Dockerfiles e health checks corretos.

## 23. Itens que não devem voltar como pendência principal

1. CRUD básico de visita, item e plano de navio.
2. Integração Navio + Yard.
3. Work queues, job lists e ações do Control Room.
4. Reserva em posição real e ciclo de expiração/cancelamento/compensação.
5. SSE do Control Room.
6. Quay Monitor e crane plan.
7. Monólito modular com oito módulos e oito schemas.
8. Contratos paginados e API pública protegida.
9. BAPLIE, COPRAR, COARRI e VERMAS com auditoria e reprocessamento.
10. Eventos internos idempotentes e reconciliação seletiva.
11. Grade operacional e ajuda contextual.
12. Ferrovia visual.
13. Yard operacional e telemetria reefer.
14. Vessel Planner multivisão.
15. Inventário canônico.
16. Gate visual e operacional.
17. Carga Geral e Break-Bulk.
18. Billing/CAP e alertas globais.
19. Dockerfiles do EasyPanel para backend e frontend.
