# Recebimento integrado Gate × Yard, planejamento preditivo e Yard Impact

## Finalidade

O processo usa a truck visit e as transações já registradas no Gate como origem operacional do planejamento de recebimento. A tela valida unidade, booking, Bill of Lading, ordem e pré-aviso antes de enviar somente as transações elegíveis ao agrupador do pátio.

A simulação organiza as unidades por compatibilidade e permite revisar os planos de posição. Ela não deve ser confundida com a confirmação operacional: posição, exchange area e work instruction somente ficam reservadas quando o fluxo transacional Gate × Yard executar a confirmação e as compensações previstas.

O mesmo processo transforma propostas do scheduler em planos de posição persistidos, versionados e auditáveis e projeta o impacto operacional por bloco e POW para antecipar saturação, rehandles, reservas, work instructions e falta de equipamentos de movimentação de contêineres, chamados de CHE.

## Fluxo operacional

### Recebimento integrado

1. Selecione a instalação do Gate.
2. Selecione uma truck visit ativa.
3. Confira veículo, transportadora, pista, estágio e status da visita.
4. Revise as transações vinculadas à visita.
5. O sistema procura a unidade na transação, no pré-aviso e na ordem.
6. O sistema relaciona booking, Bill of Lading, ordem e pré-aviso disponíveis no catálogo do Gate.
7. Transações sem unidade, com trouble, em estado final ou sem referência comercial exigida ficam bloqueadas.
8. Acione **Simular planejamento** para enviar somente as transações elegíveis ao agrupador do pátio.
9. Revise grupos, prioridades, segregações, peso, janela e alertas.
10. Abra o inspector para conferir unidade, operação, referência e as etapas ainda pendentes.
11. Revise os planos de posição preditivos.
12. Confirme posição, exchange area e work instruction somente no orquestrador transacional Gate × Yard.

### Planos de posição

1. O scheduler recebe navio, contêineres, posições candidatas, equipamentos, cutoff e pesos de critérios.
2. O otimizador gera atribuições reais de posição e uma assinatura idempotente da entrada.
3. Cada atribuição é persistida inicialmente como `TENTATIVO`, com horizonte, validade, origem e motivo.
4. O planejador revisa a proposta e pode convertê-la para `DEFINITIVO`.
5. Quando a execução se aproxima, o plano pode ser convertido para `IMINENTE`.
6. No dispatch, uma proposta ainda `TENTATIVO` é revalidada na mesma transação. O destino precisa coincidir e a validade não pode ter encerrado.

### Yard Impact

1. Escolha um horizonte entre seis e vinte e quatro horas.
2. O sistema consolida posições, contêineres atuais, reservas, planos, work queues, work instructions e equipamentos.
3. Revise entradas, saídas, rehandles e ocupação projetada.
4. Identifique blocos saturados no mapa de calor.
5. Confira demanda, cobertura e déficit de CHE por POW.
6. Selecione um bloco ou POW para chegar às unidades que geram o impacto.

## Explicação dos campos

### Contexto da truck visit

- **Instalação:** facility usada para carregar o painel operacional do Gate.
- **Truck visit:** visita ativa que fornece veículo, motorista, transportadora, pista, estágio e transações.
- **Veículo:** placa associada à visita.
- **Transportadora:** empresa responsável pelo transporte terrestre.
- **Pista:** lane atualmente atribuída à visita.
- **Estágio:** etapa atual do fluxo configurado do Gate.
- **Status:** situação operacional da visita.

### Transações e elegibilidade

- **Unidade:** código do contêiner obtido da transação, do pré-aviso ou da ordem.
- **Operação:** importação, exportação, vazio, transbordo ou outro tipo registrado no Gate.
- **Referência comercial:** booking, Bill of Lading, ordem ou pré-aviso vinculado.
- **Status da transação:** estado persistido no Gate.
- **Elegibilidade:** resultado da validação anterior ao planejamento.
- **Decisão:** bloqueios ou avisos que explicam por que a unidade foi incluída ou excluída.

### Grupo sugerido

- **Prioridade:** menor valor deve ser tratado primeiro.
- **Grupo:** combinação de fluxo, armador, viagem, destino, comprimento, equipamento, estado da carga, segregação e faixa de peso.
- **Janela:** período de quatro horas calculado a partir da ETA disponível.
- **Contêineres:** quantidade de unidades no grupo.
- **TEU:** capacidade estimada conforme o comprimento ISO.
- **Segregação:** indicação de reefer, perigoso ou padrão.

### Inspector da seleção

- **Unidade:** contêiner selecionado.
- **Operação:** tipo da transação de Gate.
- **Referência:** autorização comercial localizada.
- **Posição:** plano preditivo que deve ser revisado antes da confirmação.
- **Exchange area:** permanece pendente até a reserva transacional.
- **Work instruction:** permanece pendente até a confirmação operacional.
- **Estado:** `SIMULADA` enquanto não houver reserva confirmada.

### Plano de posição

- **Bloco, linha, coluna e camada:** posição operacional sugerida.
- **Equipamento:** CHE previsto pelo otimizador.
- **Estado:** situação atual do plano.
- **Horizonte:** início e fim da janela operacional.
- **Validade:** limite para confirmar ou revalidar a proposta.
- **Origem:** serviço que criou a proposta.
- **Assinatura de entrada:** chave usada para impedir duplicidade do mesmo cálculo.
- **Motivo:** justificativa da criação ou da última conversão.
- **Operador:** usuário responsável pela última alteração.
- **Versão:** controle otimista para impedir sobrescrita concorrente.

### Yard Impact

- **Horizonte:** período de seis a vinte e quatro horas.
- **Entradas, saídas e rehandles:** work instructions ainda operacionais classificadas por tipo de movimento.
- **Reservas:** posições com reserva ativa no início da projeção.
- **Ocupação projetada:** ocupação atual, reservas, planos e movimentos previstos em relação à capacidade cadastrada.
- **Saturação:** bloco com ocupação projetada igual ou superior a 85%.
- **Demanda de CHE:** estimativa baseada no volume de work instructions.
- **CHE disponível ou associado:** equipamento operacional disponível globalmente ou vinculado ao POW.
- **Déficit:** diferença positiva entre demanda e cobertura.
- **Drill-down:** unidades, posições, estados, equipamentos, validade, origem e motivo que compõem a projeção.

## Permissões necessárias

- `ADMIN_PORTO`: consulta completa, simulação e operações autorizadas nos módulos Gate e Yard.
- `PLANEJADOR`: consulta das visitas, simulação, revisão, confirmação, mudança para iminente e cancelamento dos planos.
- `OPERADOR_PATIO`: consulta dos planos e do Yard Impact e execução do dispatch conforme autorização operacional.
- `OPERADOR_GATE`: manutenção da visita, transações e referências conforme as permissões do Gate.
- `SERVICE_NAVIO`: geração do plano pelo endpoint do scheduler, conforme o contrato existente.

## Estados possíveis

### Recebimento

- `ELEGÍVEL`: transação apta a participar da simulação.
- `BLOQUEADA`: transação excluída por inconsistência ou ausência de autorização.
- `NÃO_SIMULADA`: nenhuma proposta foi calculada para a visita selecionada.
- `SIMULADA`: agrupamento calculado sem reserva de posição, exchange area ou work instruction.
- `RESERVADA`: estado futuro esperado quando o orquestrador confirmar as reservas de forma transacional.
- `COMPENSADA`: estado futuro esperado quando cancelamento ou falha desfizer as reservas relacionadas.

### Posição e impacto

- `TENTATIVO`: proposta calculada, válida e ainda não confirmada.
- `DEFINITIVO`: posição confirmada para uso operacional e dispatch.
- `IMINENTE`: posição confirmada com execução próxima.
- `EXPIRADO`: validade encerrada sem execução; exige novo cálculo.
- `CANCELADO`: proposta retirada por decisão operacional.
- `CONTROLADO`: bloco abaixo do limite de saturação.
- `SATURADO`: bloco com ocupação projetada a partir de 85%.
- `COBERTO`: POW com cobertura suficiente.
- `BLOQUEADO`: POW com ausência de dados, equipamento ou capacidade operacional.

## Motivos de bloqueio

### Gate × Yard

- Unidade não identificada na transação, ordem ou pré-aviso.
- Trouble ativo na transação.
- Transação cancelada, concluída ou finalizada.
- Importação sem Bill of Lading ou ordem vinculada.
- Exportação sem booking, ordem ou pré-aviso.
- Movimento de vazio sem booking, ordem ou pré-aviso.
- ISO type ausente; comprimento e tipo exigem validação antes da posição definitiva.
- Peso bruto ausente; o limite da pilha precisa ser validado.
- Perfil autenticado sem permissão.

### Posição e Yard Impact

- Plano vencido.
- Destino da work instruction diferente da posição planejada.
- Transição de estado incompatível.
- Motivo ou operador ausente na conversão.
- Alteração concorrente da versão do plano.
- Posição sem capacidade cadastrada.
- POW ou pool operacional ausente.
- CHE real não associado ou indisponível.
- Déficit de CHE para o volume projetado.

## Exemplos

### Exportação elegível

A truck visit `TV-000120` possui uma transação de exportação para a unidade `REFU1234567`. O booking `BK-2026-100` e o pré-aviso `PA-100` estão ativos. O pré-aviso informa ISO type reefer e peso bruto. A transação aparece como `ELEGÍVEL` e entra na simulação.

### Importação bloqueada

A unidade `MSCU7654321` está em uma transação de importação sem Bill of Lading nem ordem. A tela exibe `BLOQUEADA` e informa o motivo. A unidade não é enviada ao agrupador até que a referência seja corrigida no Gate.

### Trouble ativo

Uma transação possui unidade e booking válidos, mas está com trouble operacional aberto. Ela permanece visível para diagnóstico, porém não participa da simulação.

### Conversão no dispatch

A unidade `MSCU1000001` possui plano `TENTATIVO` válido para o bloco `A01`, linha `10`, coluna `4`, camada `2`. A work instruction informa o mesmo destino. Durante o dispatch, o sistema bloqueia o registro, revalida validade e destino, converte o plano para `DEFINITIVO`, grava o histórico e somente então inicia a instrução.

Se a posição da work instruction for diferente ou a validade estiver encerrada, o dispatch retorna conflito e exige novo cálculo.

### Saturação projetada

No horizonte de seis horas, o bloco `B02` alcança 88% de ocupação após considerar contêineres atuais, reservas, propostas e entradas líquidas. O painel marca o bloco como `SATURADO`. Ao selecioná-lo, o usuário vê as unidades e posições que formam a projeção.

### Déficit de CHE

O POW `POW-03` possui doze work instructions, demanda estimada de três CHE e apenas um equipamento operacional associado. O painel indica déficit de dois CHE e apresenta o motivo de bloqueio.

## Atalhos

- `F1`: abre o manual da tela atual.
- `Shift + ?`: abre o manual da tela atual.
- `Esc`: fecha o manual.
- **Atualizar:** recarrega instalações, visitas e referências do Gate.
- **Simular planejamento:** envia somente transações elegíveis ao agrupador.
- **Clique no grupo:** abre o inspector da seleção automática.
- **Clique na linha do plano:** abre detalhes e auditoria.
- **Clique no bloco ou POW:** filtra o drill-down das unidades.

## Processo completo

1. Abra **Gate > Operação completa** e crie ou localize a truck visit.
2. Confirme veículo, motorista, transportadora, pista e transações.
3. Vincule booking, Bill of Lading, ordem e pré-aviso conforme o tipo da operação.
4. Resolva troubles e inconsistências documentais.
5. Abra **Pátio > Planejamento de recebimento**.
6. Selecione instalação e truck visit.
7. Revise elegibilidade e motivos de bloqueio.
8. Simule o planejamento.
9. Revise grupos, alertas e planos de posição.
10. Confirme ou cancele a proposta de posição com motivo auditável.
11. Execute a reserva coordenada de posição, exchange area e work instruction pelo fluxo Gate × Yard.
12. Abra **Pátio > Yard Impact**.
13. Verifique saturação de blocos e cobertura por POW.
14. Corrija cobertura, posição ou recursos antes do dispatch.
15. Execute o dispatch; planos tentativos serão revalidados transacionalmente.

Rotas do processo:

- `/home/gate/operacao`
- `/home/patio/planejamento-recebimento`
- `/home/patio/yard-impact`
- `/home/patio/lista-trabalho`
- `/home/patio/instrucoes`
