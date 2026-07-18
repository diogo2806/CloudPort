# Requisitos implementados — CloudPort

Status: atualizado em 2026-07-18 com base no código incorporado à `main`.

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/implementados/requisitos-implementados.md`.

Não criar documentos paralelos, logs, históricos ou rascunhos nesta pasta. Toda entrega concluída deve sair de `docs/requisitos/modulo-navios-back-front-gaps.md` e ser registrada aqui sem duplicação.

As pendências técnicas comprovadas permanecem em `docs/requisitos/requisito-tecnico.md`.

## Arquitetura e runtime canônico

1. Definir `backend/cloudport-runtime` como ponto de entrada oficial do backend.
2. Incorporar no mesmo processo Spring Boot os módulos de Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.
3. Preservar `backend/cloudport-monolito-navio` exclusivamente como rollback intermediário.
4. Manter os diretórios `backend/servico-*` compiláveis isoladamente durante a janela de compatibilidade.
5. Usar portas, serviços de aplicação e eventos internos para comunicação entre módulos incorporados.
6. Manter HTTP, RabbitMQ, Redis, TOS, OCR, EDI, storage e webhooks somente na borda.
7. Impedir por regras ArchUnit ciclos, dependência de módulo para o runtime, repository de outro módulo e adaptador HTTP interno.
8. Centralizar segurança, CORS, Jackson, OpenAPI, tratamento de erros, correlação, métricas, tracing, clientes HTTP e agendamento no runtime.
9. Produzir um único JAR executável e uma única imagem backend canônica.
10. Incorporar `cloudport-contracts` ao reator Maven, workflows e imagens Docker.

## Persistência, schemas e Flyway

1. Usar uma conexão PostgreSQL compartilhada.
2. Preservar oito schemas proprietários:
   - `cloudport_autenticacao`;
   - `cloudport_carga_geral`;
   - `cloudport_gate`;
   - `cloudport_rail`;
   - `cloudport_visibilidade`;
   - `cloudport_yard`;
   - `cloudport_navio`;
   - `cloudport_siderurgico`.
3. Criar um objeto Flyway e um `flyway_schema_history` por módulo.
4. Executar migrações antes do `EntityManagerFactory`.
5. Validar nomes de schema, `validateOnMigrate` e ausência de migração pendente.
6. Usar estratégia `expand and contract` durante a janela de rollback.
7. Preservar ownership das estruturas pelo módulo que publica a migração.

## Build, Docker e implantação

1. Usar `backend/cloudport-modules` como reator canônico.
2. Instalar o parent `backend/cloudport-navio-modules/pom.xml` antes do empacotamento do runtime.
3. Incluir os oito módulos e `cloudport-contracts` no build.
4. Manter `backend/Dockerfile` para o contexto `/backend` do EasyPanel.
5. Manter `backend/cloudport-runtime/Dockerfile` para build iniciado na raiz.
6. Criar `frontend/Dockerfile` multi-stage com Node 22 e Nginx.
7. Publicar `frontend/cloudport/dist/cloudport` com fallback de SPA.
8. Expor health backend em `/actuator/health/readiness` e frontend em `/health`.
9. Preparar o diretório persistente de documentos no backend.
10. Validar em workflow as duas imagens backend e a imagem frontend.

Configuração implementada no EasyPanel:

| Serviço | Contexto | Dockerfile | Porta | Health |
| --- | --- | --- | --- | --- |
| Backend | `/backend` | `Dockerfile` | `8080` | `/actuator/health/readiness` |
| Frontend | `/frontend` | `Dockerfile` | `80` | `/health` |

## Navio e integração com Yard

1. Criar cadastro canônico de navios, escalas e visitas.
2. Criar itens operacionais de embarque, descarga e restow.
3. Criar e versionar planos de estiva.
4. Criar reservas de pátio vinculadas à visita, item e plano.
5. Gerar ordens reais no Yard e impedir ordem ativa duplicada.
6. Sincronizar status, posição real, reserva e execução entre Navio e Yard.
7. Alterar prioridade, suspender, retomar, resetar e cancelar ordens com motivo e auditoria.
8. Replanejar usando posição real validada e compensação transacional da reserva anterior.
9. Expor filas, ordens, reservas, exceções e itens sem cobertura por visita.
10. Publicar eventos internos e manter jobs periódicos somente como reparo de divergência.
11. Implementar portas locais para cadastro, posições, ordens, work queues, otimização e aplicação de plano.

## Work queues, work instructions e Control Room de Navio

1. Listar, criar, ativar e desativar work queues.
2. Associar POW, pool, recurso de cais e equipamento real.
3. Persistir `workQueueId` em ordens do pátio e planos de guindaste.
4. Expor job list, dispatch, reset e cancelamento.
5. Aplicar limite real de ordens no dispatch.
6. Concentrar transições em serviço operacional com matriz oficial de estados.
7. Auditar criação, status, POW, pool, equipamento, vínculo, dispatch, reset, cancelamento e motivo.
8. Restringir comandos por perfil.
9. Criar drill-down e job lists por equipamento nos fluxos já integrados.
10. Integrar o Control Room ao portal pela rota autenticada.
11. Implementar SSO por `postMessage` com origens configuradas e login próprio como fallback.
12. Propagar JWT, usuário, origem, `X-Correlation-Id` e `traceparent`.
13. Carregar snapshots em paralelo e aplicar resultado atomicamente.
14. Substituir polling principal por SSE, com reconexão e fallback controlado.

## Quay Monitor, berth e crane plan

1. Expor monitor de cais, plano de guindastes e produtividade.
2. Persistir alocações por berço, guindaste, porão, work queue, sequência, janela e movimentos.
3. Validar visita, período, sobreposição, equipamento repetido e compatibilidade com o Yard.
4. Validar fila ativa, POW, pool, CHE, recurso de cais e work instructions elegíveis.
5. Calcular produtividade planejada e realizada.
6. Consumir os contratos no frontend do Quay Monitor.

## Vessel Planner gráfico

1. Implementar vistas `profile`, `top`, `section` e `tier` sincronizadas.
2. Criar modo multivisão e inspector lateral por slot.
3. Permitir drag-and-drop da load list para slots e movimentação entre slots.
4. Preservar atributos operacionais ao movimentar contêiner.
5. Exibir legendas por POD, peso, IMO, reefer e operador.
6. Representar tampas de porão, peso acumulado por stack e limites visuais.
7. Mostrar restrições, violações e alertas diretamente nos slots.
8. Exibir segregação IMDG, restow e sequência visual dos guindastes.
9. Exibir overlays de estabilidade, lashing indicativo e força estrutural.
10. Reutilizar validações de hard constraints do backend.

## Bay Plan, EDI e atributos operacionais

1. Implementar BAPLIE, COPRAR, COARRI e VERMAS.
2. Validar tipo, navio, viagem e operações suportadas.
3. Normalizar peso em quilogramas e preservar VGM separadamente do peso bruto.
4. Persistir posição, operação, cheio/vazio, reefer, IMO, ONU, grupo de embalagem, segregação, OOG e instruções.
5. Preservar segmentos originais para auditoria.
6. Persistir status `RECEBIDO`, `PROCESSANDO`, `CONCLUIDO`, `REJEITADO` e `QUARENTENA`.
7. Tornar a recepção HTTP assíncrona e idempotente por `UNB`, `UNH` e referência.
8. Rejeitar reutilização de identidade com conteúdo divergente.
9. Reivindicar mensagens pendentes com trava transacional e retentativa exponencial.
10. Permitir reprocessamento motivado com limite de tentativas.
11. Retornar `X-EDI-Processing-Id` para mensagens aceitas.
12. Aplicar compatibilidade de slots reefer, segregação de perigosos e reserva adjacente para OOG.

## Estabilidade e resistência

1. Remover valores hidrostáticos sintéticos dos fluxos classificados como operacionais.
2. Exigir versões dos dados hidrostáticos e de resistência longitudinal.
3. Calcular peso, LCG, TCG, VCG, GM, calado, trim e banda a partir da condição real.
4. Calcular força cortante e momento fletor por seções.
5. Usar coordenadas físicas persistidas dos slots e cargas.
6. Bloquear aprovação quando entradas obrigatórias estiverem ausentes.
7. Persistir versões de entrada, memória de cálculo, resultados e instante de aprovação.
8. Invalidar aprovação anterior quando o plano for alterado.

## Yard gráfico operacional

1. Manter mapa georreferenciado em Google Maps.
2. Implementar vistas de bloco, seção lateral, scan e microvisão da pilha.
3. Implementar camadas de situação, ocupação, dwell time e reefers.
4. Exibir CHEs com telemetria e atualização automática.
5. Destacar pilhas bloqueadas, interditadas, cheias, reservadas e com notas.
6. Salvar, restaurar e excluir workspaces no navegador.
7. Permitir arrastar contêiner para posição livre.
8. Simular origem e destino antes da confirmação.
9. Validar no backend destino existente, livre, permitido e não reservado.
10. Editar bloqueio, interdição, permissão e nota operacional da pilha.
11. Desenhar rotas entre a posição atual e o destino da work instruction.
12. Implementar editor gráfico de allocations com posições elegíveis e confirmação motivada.

## Telemetria reefer

1. Persistir temperatura atual, faixa permitida, alimentação elétrica e instante da leitura.
2. Gerar estados `NORMAL`, `ATENCAO` e `CRITICO`.
3. Alertar leitura desatualizada, reefer desligado e temperatura fora da faixa.
4. Exibir painel de reefers no mapa operacional.
5. Restringir gravação de telemetria e edição de allocation aos perfis autorizados.

## Inventário canônico

1. Unificar contêiner, chassi, carreta e acessório em `unidade_inventario`.
2. Implementar ciclo de vida completo e matriz de transições.
3. Cadastrar tipos, ISO, dimensões, capacidades, prefixos e equivalências.
4. Controlar lacres, documentos, avarias, componentes e condições.
5. Controlar manutenção, reparo, holds e permissions.
6. Controlar proprietário, operador, posição real e planejada.
7. Montar e desmontar equipamentos por papéis operacionais.
8. Manter histórico de atributos e registros reefer.
9. Executar inventário físico e classificar divergências.
10. Importar e sincronizar o inventário legado de `conteiner_patio`.
11. Expor API canônica em `/yard/inventario/canonico`.
12. Criar interface com filtros, indicadores, grade operacional, cadastro e inspector unificado.
13. Bloquear liberação ou despacho com hold ativo.

## Gate operacional completo

1. Cadastrar facilities, gates, pistas, consoles e áreas atendidas.
2. Configurar estágios, transições e business tasks.
3. Manter bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos.
4. Criar appointments com capacidade transacional.
5. Abrir truck visits com múltiplas transações.
6. Processar trouble transactions e inspeções.
7. Armazenar fotografias, documentos, tickets e EIR.
8. Implementar transferências entre instalações.
9. Aplicar regras de bloqueio e permissão a motorista, transportadora e veículo.
10. Manter histórico de estágios.
11. Expor painel React em `/home/gate/operacao`.

## Gate visual

1. Exibir quadro visual de pistas e filas por estágio.
2. Exibir calendário de agendamentos e ocupação versus capacidade.
3. Mostrar jornada do veículo com OCR, balança, inspeção e liberação.
4. Exibir documentos, imagens e avarias.
5. Manter painel de transações problemáticas.
6. Imprimir e reimprimir EIR.
7. Exibir cronômetro e classificação de SLA.
8. Separar a rota de saída direta do navio.

## Controle de entrada e saída de pessoas

1. Cadastrar pessoas por documento normalizado.
2. Registrar entrada e saída com situação atual e histórico de movimentações.
3. Aplicar unicidade de documento e versão otimista.
4. Expor endpoints operacionais no Gate.

A serialização de requisições concorrentes permanece registrada como `ERR10` em `docs/requisitos/requisito-tecnico.md`.

## Carga Geral, carga de projeto e break-bulk

1. Criar módulo `servico-carga-geral` e incorporá-lo ao runtime.
2. Implementar Bill of Lading e itens do conhecimento.
3. Implementar cargo lots para carga solta, projeto e break-bulk.
4. Cadastrar commodities, embalagens, produtos, códigos de armazenagem e manuseio.
5. Cadastrar mercadorias perigosas, temperatura e avarias.
6. Controlar quantidade, volume e peso previstos e em estoque.
7. Registrar recebimento, carga, descarga parcial, transferência, consolidação e desconsolidação.
8. Vincular lote a veículo, visita de navio, armazém e cliente.
9. Validar saldo não negativo, dados IMDG e faixa de temperatura.
10. Usar bloqueio pessimista nas movimentações de estoque.
11. Criar dashboard e console React operacional.

A segurança standalone e o tratamento de duplicidade concorrente permanecem registrados como `SEC80` e `ERR40`.

## Rail visual

1. Exibir locomotiva e vagões em sequência.
2. Associar contêineres de carga e descarga a cada vagão.
3. Exibir progresso individual por vagão.
4. Representar linhas ferroviárias e ocupação.
5. Permitir replanejamento visual por drag-and-drop e seletor acessível.
6. Indicar vagão bloqueado, incompatível ou sem operação válida.
7. Exibir cronograma de chegada, operação e partida.
8. Destacar conflitos de ocupação entre trens.

A persistência do replanejamento de vagões permanece como evolução funcional.

## Control Room de equipamentos e telemetria

1. Exibir status, posição, conectividade, VMT e work instruction atual.
2. Persistir telemetria atual e histórico de leituras.
3. Atualizar o painel quase em tempo real por SSE.
4. Detectar telemetria atrasada, heartbeat ausente, falha de dispositivo e indisponibilidade.
5. Reconhecer e resolver alarmes técnicos.
6. Abrir e encerrar indisponibilidades com motivo e responsáveis.
7. Criar, entregar, executar e confirmar comandos remotos.
8. Integrar dispositivos por heartbeat autenticado.
9. Manter firmware, protocolo, endereço e sequência do dispositivo.
10. Expor interface em `/home/control-room`.

Comandos suportados: `DISPONIBILIZAR`, `INDISPONIBILIZAR`, `ENVIAR_MENSAGEM`, `MOVER_PARA_POSICAO`, `SINCRONIZAR_TELEMETRIA` e `RESETAR_POSICAO`.

## Grade operacional compartilhada

1. Implementar ordenação, busca, paginação e filtros.
2. Permitir ocultação, congelamento e reordenação de colunas.
3. Permitir seleção múltipla e ações em lote.
4. Exibir inspector lateral.
5. Exportar CSV e Excel.
6. Neutralizar valores iniciados por `=`, `+`, `-` ou `@` para evitar fórmula maliciosa.
7. Inferir todos os campos retornados nas páginas genéricas, sem limite de oito colunas.
8. Preservar ordem preferencial e identificadores estáveis de exportação.

## Visibilidade e eventos

1. Consolidar rastreamento e histórico de contêineres.
2. Processar eventos de Gate, Yard, Rail e Navio.
3. Criar projeção quando o evento chega antes do cadastro.
4. Calcular throughput do Gate por ciclos reais.
5. Resolver alertas automaticamente quando a condição é regularizada.
6. Exigir motivo para resolução manual.
7. Persistir identidade e hash do evento antes de aplicar o efeito.
8. Ignorar redelivery idêntico e rejeitar colisão de identidade divergente.
9. Executar deduplicação, projeção e histórico na mesma transação.
10. Publicar dashboard por job condicionado a `cloudport.runtime.jobs-enabled=true`.

## API pública, contratos e segurança implementada

1. Padronizar paginação com `conteudo`, `pagina`, `tamanho`, `totalElementos`, `totalPaginas`, `primeira` e `ultima`.
2. Padronizar erro com código, mensagem, detalhes, status, caminho, timestamp e `correlationId`.
3. Criar comando motivado, envelope de evento versionado e enums externos em `cloudport-contracts`.
4. Implementar filtros no banco e whitelist de ordenação.
5. Proteger `/api/public/v1/**` por cliente configurado.
6. Comparar segredos em tempo constante.
7. Gerar e propagar `X-Correlation-Id`.
8. Publicar eventos por SSE, WebSocket/STOMP e eventos Spring internos.
9. Expor `GET /api/public/v1/events/stream` e streams por visita.
10. Manter integração legada por `X-CloudPort-Service-Key` somente onde autorizada.
11. Não armazenar senha no `localStorage` e preservar integralmente a senha digitada.

## Testes e proteção arquitetural

1. Criar teste de contexto com PostgreSQL 16 em Testcontainers.
2. Validar os oito schemas e históricos Flyway.
3. Validar ausência de migrações pendentes.
4. Validar uma única cadeia de segurança.
5. Validar controllers incorporados no mesmo contexto.
6. Testar modo somente leitura com `503`.
7. Testar propriedades de consumidores RabbitMQ.
8. Testar advisory lock de jobs críticos.
9. Criar testes ArchUnit de ciclos, repositories e adaptadores HTTP.
10. Cobrir contratos frontend de grade, Gate, Rail, Yard, inventário, Vessel Planner e Control Room.
11. Criar smoke do Compose e validar JWT, persistência e portas locais.

## Itens que não devem voltar como pendência principal

1. CRUD básico de visita, item e plano.
2. Integração Navio + Yard e reserva em posição real.
3. Work queues, job list, dispatch e transições operacionais.
4. Streaming SSE do Control Room.
5. Quay Monitor e plano de guindastes persistido.
6. Runtime canônico com oito módulos.
7. Oito schemas e históricos Flyway independentes.
8. Carga Geral e break-bulk.
9. Gate operacional e Gate visual.
10. Inventário canônico.
11. Yard gráfico com reefers, rotas e allocations.
12. Vessel Planner multivisão.
13. Rail visual.
14. Control Room de equipamentos e telemetria.
15. Grade operacional e exportação Excel.
16. Imagens Docker para EasyPanel nos contextos `/backend` e `/frontend`.
