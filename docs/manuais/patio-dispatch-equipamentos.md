# Dispatch e equipamentos

## Finalidade da tela

A tela **Dispatch e equipamentos** centraliza a seleção, o sequenciamento e o acompanhamento de work instructions de pátio. O motor considera a família do CHE, a prioridade operacional, o atraso acumulado, a distância, o congestionamento, a atualização da telemetria, a capacidade simultânea, os limites regionais, os holds da unidade e a disponibilidade de equipamentos auxiliares.

A tela também permite consultar a memória de cálculo de cada decisão, acompanhar as seis etapas persistidas da instrução, manter configurações versionadas em runtime, executar rollback e cadastrar versões de rotas, interdições e congestionamentos.

## Fluxo operacional

1. Selecione uma work queue ativa.
2. Confirme se a fila possui POW, pool operacional, visita, plano de guindaste, recurso de cais e CHE real associado.
3. Calcule o ranking.
4. Revise score, ETA, rota, telemetria e motivos de bloqueio.
5. Selecione uma work instruction elegível.
6. Confirme o autodespacho.
7. O backend revalida o planejamento de posição, o estado da unidade, a capacidade concorrente, a rota e a configuração vigente.
8. Quando necessário, o sistema seleciona e reserva chassis, bomb cart, cassette ou acessório.
9. A decisão é persistida com memória de cálculo, configuração utilizada, rota, telemetria e chave de idempotência.
10. As etapas deslocamento até a origem, chegada, coleta, transporte, entrega e confirmação física são criadas.
11. Eventos VMT atualizam as etapas e encerram a decisão.
12. Em modo automático, conclusão ou falha aciona a seleção da próxima work instruction elegível.
13. O auxiliar é desassociado e devolvido ao pool após o encerramento.

## Estratégias por família de CHE

| Família | Estratégia principal |
| --- | --- |
| RTG | Operação vertical de bloco, com distância e camada de destino. |
| RMG | Operação vertical guiada, com sequência e cobertura de bloco. |
| ASC | Operação automática, exigindo telemetria dentro da tolerância. |
| Reach Stacker | Operação vertical móvel, com maior peso para deslocamento e congestionamento. |
| Trator portuário | Transporte horizontal, com seleção de chassis quando configurada. |
| Straddle Carrier | Transporte horizontal com coleta e entrega integradas. |
| Guindaste ship-to-shore | Sequência de cais e navio, com possibilidade de bomb cart. |
| Equipamento ferroviário | Movimentos cuja origem ou destino pertence ao domínio ferroviário. |

## Explicação dos campos

### Seleção e ranking

- **Work queue**: fila que reúne as instruções e a cobertura operacional.
- **CHE associado**: equipamento real vinculado à fila.
- **POW**: ponto ou área de trabalho utilizada na cobertura operacional.
- **Pool**: agrupamento de recursos aptos ao atendimento.
- **Posição do ranking**: ordem calculada pelo scheduler.
- **Score**: resultado da soma de prioridade, atraso e ajuste da família, descontando distância e congestionamento.
- **ETA**: tempo estimado de deslocamento, coleta e entrega.
- **Elegibilidade**: indica se a instrução pode ser despachada.
- **Bloqueios**: lista objetiva dos impedimentos encontrados.
- **Memória de cálculo**: detalhamento dos componentes usados no score.

### Decisão

- **Chave de idempotência**: impede criação duplicada da mesma decisão.
- **Configuração e versão**: regra efetivamente aplicada pelo scheduler.
- **Modo**: manual, semiautomático ou automático.
- **Rota**: origem, destino, distância, congestionamento, ETA e estado da telemetria.
- **Auxiliar**: equipamento de apoio reservado para a instrução.
- **Correlation ID**: identificador para rastreamento entre portal, backend e integrações.

### Configuração

- **Escopo**: FILA, POOL, POW, BLOCO, PÁTIO ou TERMINAL.
- **Valor do escopo**: identificador ao qual a regra se aplica.
- **Família de CHE**: equipamento atendido pela configuração.
- **Versão**: número crescente dentro do escopo e família.
- **Pesos**: influência de prioridade, distância, atraso e congestionamento.
- **Velocidade média**: base do cálculo de ETA.
- **Tempos de coleta e entrega**: parcelas fixas adicionadas ao ETA.
- **Tolerância da telemetria**: idade máxima aceita no modo automático.
- **Capacidade simultânea**: quantidade de instruções em execução para o mesmo CHE.
- **Limite regional**: quantidade máxima de CHE ativos na região da rota.
- **Selecionar auxiliar**: ativa a reserva automática de equipamento de apoio.
- **Permitir override**: registra se o perfil aceita intervenção autorizada.
- **Vigência**: intervalo em que a versão pode ser resolvida.

A resolução usa a ordem: **FILA > POOL > POW > BLOCO > PÁTIO > TERMINAL**.

### Rotas

- **Origem e destino**: posições operacionais normalizadas.
- **Distância**: comprimento configurado do segmento.
- **Sentido**: restrição de circulação do trecho.
- **Congestionamento**: percentual aplicado ao tempo de deslocamento.
- **Interdição**: bloqueio operacional do segmento.
- **Motivo**: justificativa da interdição.
- **Limite regional de CHE**: quantidade máxima de decisões ativas na origem.
- **Versão e vigência**: histórico temporal do segmento.

## Permissões necessárias

| Perfil | Permissões |
| --- | --- |
| `ADMIN_PORTO` | Consulta, ranking, autodespacho, avanço de etapas, criação e ativação de configurações, rollback e manutenção de rotas. |
| `PLANEJADOR` | Consulta, ranking, autodespacho, avanço de etapas, configuração, rollback e rotas. |
| `OPERADOR_PATIO` | Consulta, ranking, autodespacho e avanço de etapas. |
| `SERVICE_NAVIO` | Consulta das decisões, ranking, configurações e etapas para integração. |

## Estados possíveis

### Decisão de dispatch

- **RECOMENDADA**: decisão calculada e persistida antes da atribuição.
- **ATRIBUÍDA**: work instruction despachada e etapas inicializadas.
- **REJEITADA**: recomendação recusada por regra ou intervenção.
- **CANCELADA**: decisão retirada antes da conclusão.
- **CONCLUÍDA**: confirmação física recebida.
- **FALHA**: evento VMT encerrou a instrução com falha.

### Etapas da work instruction

- **PENDENTE**: etapa ainda não iniciada.
- **EM_EXECUÇÃO**: etapa corrente.
- **CONCLUÍDA**: etapa encerrada com sucesso.
- **FALHA**: etapa corrente encerrada com erro.
- **IGNORADA**: etapa dispensada por processo autorizado.

### Configuração

- **RASCUNHO**: versão criada e ainda não aplicada.
- **ATIVA**: versão vigente para resolução.
- **INATIVA**: versão substituída, mantida para auditoria e rollback.

### Auxiliar

- **RESERVADO**: recurso separado para a decisão.
- **ASSOCIADO**: recurso vinculado ao CHE e à work instruction.
- **DEVOLVIDO**: recurso liberado após o encerramento.
- **CANCELADO**: reserva desfeita antes da associação.

## Motivos de bloqueio

- Work queue inativa.
- POW, pool, visita, plano de guindaste ou recurso de cais ausente.
- CHE real não associado à fila.
- CHE inexistente, indisponível ou divergente da fila.
- Fase da visita fora do período operacional.
- Unidade com hold ativo.
- Work instruction fora dos estados pendente ou suspensa.
- Instrução concorrente acima da capacidade simultânea.
- Planejamento preditivo vencido ou divergente.
- Telemetria ausente ou atrasada no modo automático.
- Rota interditada.
- Limite regional de CHE atingido.
- Configuração ativa inexistente ou fora da vigência.
- Chave idempotente já utilizada por outra operação.
- Chassis, bomb cart, cassette ou acessório obrigatório indisponível.
- Etapa anterior ainda não concluída.
- Perfil sem permissão.

## Exemplos

### Trator portuário com chassis

Uma fila de trator portuário está em modo automático. O scheduler classifica as instruções, seleciona a unidade sem hold com melhor score e reserva um chassis operacional sem reserva ativa. A decisão registra a memória de cálculo e cria as seis etapas. Após a conclusão VMT, o chassis é devolvido e a próxima instrução elegível é selecionada.

### ASC com telemetria atrasada

Uma fila de ASC está em modo automático, mas a última telemetria ultrapassou a tolerância de 120 segundos. O ranking continua exibindo a instrução, porém marca a decisão como bloqueada e apresenta o motivo. Após nova telemetria, o ranking pode ser recalculado.

### Interdição de rota

O planejador cria uma nova versão do segmento `BLOCO-A` para `CAIS-2`, marca-o como bloqueado e informa o motivo. Novos rankings passam a bloquear decisões que dependam desse segmento. A versão anterior permanece disponível para auditoria.

### Rollback de configuração

Uma versão ativa aumentou excessivamente o peso de distância. O planejador escolhe uma versão anterior e executa rollback com justificativa. O backend cria uma nova versão com os valores históricos, desativa a configuração atual e ativa a versão de rollback.

## Atalhos

- **F1**: abrir o manual contextual.
- **Shift + ?**: abrir o manual contextual.
- **Esc**: fechar o manual ou painel aberto.
- **Atualizar**: recarregar resumo, decisões, configurações e rotas.
- **Calcular ranking**: reavaliar as instruções com dados atuais.
- **Clique na linha do ranking**: selecionar a work instruction elegível.
- **Despachar selecionada**: executar validações e atribuir a instrução.

## Integrações e endpoints

- `GET /api/dispatch/resumo`
- `GET /api/dispatch/decisoes`
- `GET /api/dispatch/work-queues/{id}/ranking`
- `POST /api/dispatch/auto-dispatch`
- `POST /yard/patio/work-instructions/auto-dispatch`
- `GET /api/dispatch/work-instructions/{id}/etapas`
- `POST /api/dispatch/work-instructions/{id}/etapas/{tipo}`
- `GET /api/dispatch/configuracoes`
- `POST /api/dispatch/configuracoes`
- `POST /api/dispatch/configuracoes/{id}/ativar`
- `POST /api/dispatch/configuracoes/{id}/rollback`
- `GET /api/dispatch/rotas`
- `POST /api/dispatch/rotas`

## Processo completo

1. [Lista de trabalho do pátio](../../frontend/cloudport/src/pages/yard/YardWorkListPage.jsx)
2. [Tela Dispatch e equipamentos](../../frontend/cloudport/src/pages/yard/DispatchEquipamentosPage.jsx)
3. [Motor de dispatch dinâmico](../../backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/dispatch/DispatchDinamicoServico.java)
4. [Ciclo persistido de etapas](../../backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/dispatch/EtapaWorkInstructionServico.java)
5. [Reconciliador automático VMT](../../backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/dispatch/DispatchAutomaticoReconciliador.java)
6. [Seleção de auxiliares](../../backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/dispatch/SelecaoEquipamentoAuxiliarServico.java)
7. [Migration V220](../../backend/servico-yard/src/main/resources/db/migration/V220__dispatch_dinamico_equipamentos.sql)
