# BUS1420 a BUS1470 — Dispatch e equipamentos

## Escopo implementado

### BUS1420 — Scheduler dinâmico por família de CHE

- Estratégias específicas para RTG, RMG, ASC, reach stacker, trator portuário, straddle carrier, guindaste ship-to-shore e equipamento ferroviário.
- Ranking por prioridade, atraso, distância, congestionamento e ajuste da família.
- Memória de cálculo persistida para auditoria.
- ETA composto por deslocamento, congestionamento, coleta e entrega.
- Bloqueio por rota, limite regional e telemetria atrasada no modo automático.

### BUS1430 — Modos manual, semiautomático e automático

- Configuração versionada do modo por escopo e família de CHE.
- Seis etapas persistidas por work instruction.
- Sincronização das etapas com aceite, início, falha e conclusão VMT.
- Reconciliador agendado e idempotente.
- Autodespacho da próxima instrução após encerramento quando o modo efetivo for automático.

### BUS1440 — Autodespacho acionado pelo operador

- Endpoint `POST /yard/patio/work-instructions/auto-dispatch`.
- Alias `POST /api/dispatch/auto-dispatch` para o portal React.
- Validação de fila, CHE, fase da visita, POW, pool, unidade, prioridade, hold, concorrência, planejamento, rota, telemetria e chave idempotente.
- Resposta com decisão, score, ETA, rota, auxiliar e etapas.

### BUS1450 — Configuração operacional versionada

- Escopos FILA, POOL, POW, BLOCO, PÁTIO e TERMINAL.
- Resolução hierárquica na ordem de maior especificidade.
- Pesos, tempos médios, velocidade, tolerância de telemetria, capacidade simultânea, limite regional, seleção de auxiliar e override.
- Criação em rascunho, ativação transacional, inativação da versão anterior e rollback que cria nova versão.
- Histórico de criação, ativação, inativação e rollback.

### BUS1460 — GPS, rotas, sentidos e congestionamento

- Consumo da telemetria persistida do CHE.
- Fallback para coordenadas de grade quando não houver telemetria.
- Cadastro versionado de segmentos de rota.
- Distância, sentido, congestionamento, interdição, vigência e limite regional de CHE.
- Bloqueio automático por telemetria vencida, interdição ou limite regional.
- ETA e justificativa da rota preservados na decisão.

### BUS1470 — Seleção de equipamentos auxiliares

- Seleção de chassis para trator portuário.
- Seleção de bomb cart para guindaste ship-to-shore.
- Seleção de cassette para carga siderúrgica ou bobinas.
- Seleção genérica de acessório quando a configuração exigir.
- Validação de categoria, condição operacional, estado, hold, armador e reserva ativa.
- Reserva concorrente protegida por índice único parcial.
- Movimentos de coleta, associação, desassociação e devolução persistidos.

## Persistência

A migration `V220__dispatch_dinamico_equipamentos.sql` cria:

- `configuracao_dispatch`;
- `historico_configuracao_dispatch`;
- `segmento_rota_dispatch`;
- `decisao_dispatch`;
- `etapa_work_instruction`;
- `reserva_equipamento_auxiliar_dispatch`;
- `movimento_equipamento_auxiliar_dispatch`;
- `gatilho_dispatch_processado`.

Também são criados perfis iniciais ativos para oito famílias de CHE.

## Backend

Pacote principal:

`backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/dispatch`

Componentes:

- `DispatchDinamicoServico`;
- `DispatchScheduler` e `DispatchSchedulerRegistry`;
- `ConfiguracaoDispatchServico`;
- `RoteamentoEquipamentoServico`;
- `SegmentoRotaDispatchServico`;
- `EtapaWorkInstructionServico`;
- `SelecaoEquipamentoAuxiliarServico`;
- `DispatchAutomaticoReconciliador`;
- `DispatchControlador`.

O dispatch manual existente permanece disponível e não foi substituído.

## Frontend

- Nova rota `/home/patio/dispatch-equipamentos`.
- Nova entrada na navegação dinâmica e na navegação de contingência.
- Aba `Dispatch` no conjunto de telas do pátio.
- Resumo operacional.
- Seleção de fila e CHE.
- Ranking auditável e autodespacho.
- Decisões recentes.
- Configurações, ativação e rollback.
- Cadastro e consulta de rotas.
- Manual contextual completo por F1 ou Shift + ?.

## Segurança

- Leitura: `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_PATIO`, `SERVICE_NAVIO`.
- Operação: `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_PATIO`.
- Configurações e rotas: `ADMIN_PORTO`, `PLANEJADOR`.

## Testes

`DispatchSchedulerTest` cobre:

- influência da prioridade de busca e geração da memória de cálculo;
- bloqueio do modo automático quando a telemetria está atrasada.

## Manual

[Manual completo de Dispatch e equipamentos](../manuais/patio-dispatch-equipamentos.md)
