# Planejamento preditivo de pátio e Yard Impact

## Finalidade

O processo transforma as propostas calculadas pelo scheduler em planos de posição persistidos, versionados e auditáveis. Também projeta o impacto operacional por bloco e POW para antecipar saturação, rehandles, reservas, work instructions e falta de equipamentos de movimentação de contêineres, chamados de CHE.

## Fluxo operacional

1. O scheduler recebe navio, contêineres, posições candidatas, equipamentos, cutoff e pesos de critérios.
2. O otimizador gera atribuições reais de posição e uma assinatura idempotente da entrada.
3. Cada atribuição é persistida inicialmente como `TENTATIVO`, com horizonte, validade, origem e motivo.
4. O planejador revisa a proposta e pode convertê-la para `DEFINITIVO`.
5. Quando a execução se aproxima, o plano pode ser convertido para `IMINENTE`.
6. No dispatch, uma proposta ainda `TENTATIVO` é revalidada na mesma transação. O destino precisa coincidir e a validade não pode ter encerrado.
7. O Yard Impact consolida posições, contêineres atuais, reservas, planos, work queues, work instructions e equipamentos para projetar o horizonte selecionado.
8. O operador seleciona um bloco ou POW para chegar às unidades que geram o impacto.

## Campos

### Plano de posição

- **Unidade:** código do contêiner associado à proposta.
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

## Permissões

- `ADMIN_PORTO`: consulta completa e conversão dos estados dos planos.
- `PLANEJADOR`: consulta, confirmação, mudança para iminente e cancelamento.
- `OPERADOR_PATIO`: consulta dos planos e do Yard Impact; execução do dispatch conforme autorização operacional.
- `SERVICE_NAVIO`: geração do plano pelo endpoint do scheduler, conforme o contrato já existente.

## Estados possíveis

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

- Plano vencido.
- Destino da work instruction diferente da posição planejada.
- Transição de estado incompatível.
- Motivo ou operador ausente na conversão.
- Alteração concorrente da versão do plano.
- Posição sem capacidade cadastrada.
- POW ou pool operacional ausente.
- CHE real não associado ou indisponível.
- Déficit de CHE para o volume projetado.
- Perfil autenticado sem a permissão necessária.

## Exemplos

### Conversão no dispatch

A unidade `MSCU1000001` possui plano `TENTATIVO` válido para o bloco `A01`, linha `10`, coluna `4`, camada `2`. A work instruction informa o mesmo destino. Durante o dispatch, o sistema bloqueia o registro, revalida a validade e o destino, converte o plano para `DEFINITIVO`, grava o histórico e somente então inicia a instrução.

Se a posição da work instruction for diferente ou a validade estiver encerrada, o dispatch retorna conflito e exige novo cálculo.

### Saturação projetada

No horizonte de seis horas, o bloco `B02` alcança 88% de ocupação após considerar contêineres atuais, reservas, propostas e entradas líquidas. O painel marca o bloco como `SATURADO`. Ao selecioná-lo, o usuário vê as unidades e posições que formam a projeção.

### Déficit de CHE

O POW `POW-03` possui doze work instructions, demanda estimada de três CHE e apenas um equipamento operacional associado. O painel indica déficit de dois CHE e apresenta o motivo de bloqueio.

## Atalhos

- `F1`: abre o manual da tela atual.
- `Shift + ?`: abre o manual da tela atual.
- `Esc`: fecha o manual.
- **Atualizar:** recarrega planos ou recalcula a projeção.
- **Clique na linha do plano:** abre detalhes e auditoria.
- **Clique no bloco ou POW:** filtra o drill-down das unidades.

## Processo completo

1. Abra **Pátio > Planejamento de recebimento**.
2. Gere ou consulte os planos preditivos.
3. Revise posição, horizonte, validade, origem e justificativa.
4. Confirme ou cancele a proposta com motivo auditável.
5. Abra **Pátio > Yard Impact**.
6. Verifique saturação de blocos e cobertura por POW.
7. Use o drill-down para identificar as unidades responsáveis.
8. Corrija cobertura, posição ou recursos antes do dispatch.
9. Execute o dispatch; planos tentativos serão revalidados transacionalmente.

Rotas do processo:

- `/home/patio/planejamento-recebimento`
- `/home/patio/yard-impact`
- `/home/patio/lista-trabalho`
- `/home/patio/instrucoes`
