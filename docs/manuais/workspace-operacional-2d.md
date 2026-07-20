# Workspace operacional 2D integrado

## Finalidade

O workspace operacional 2D mantém a mesma unidade sincronizada entre as vistas de Navio, Pátio, Ferrovia e equipamentos. A seleção compartilhada reúne posição física, origem, destino, work instruction, CHE e referências ferroviárias sem substituir os comandos transacionais de cada domínio.

## Fluxo operacional

1. Abra o Vessel Planner ou o mapa operacional do pátio.
2. Selecione um slot, contêiner ou pilha.
3. Confira os cartões Navio, Pátio, Ferrovia e Equipamento.
4. Use zoom, pan, breadcrumbs ou duplo clique para aprofundar da visão geral até bloco, linha, pilha, tier e slot.
5. Para uma análise entre domínios, arraste o cartão de origem sobre o cartão de destino.
6. Revise unidade, posições, WI, CHE e bloqueios apresentados na simulação.
7. Informe o motivo operacional e encaminhe a proposta para confirmação.
8. Conclua a persistência no comando autorizado do domínio de destino, que continua responsável pela validação definitiva e pela transação.

## Campos e elementos

- Unidade: código do contêiner ou carga selecionada.
- Localização: coordenadas do navio, pátio, ferrovia ou equipamento.
- WI: work instruction ou ordem operacional vinculada.
- CHE: equipamento atual, atribuído ou sugerido.
- Origem e destino: posições usadas para compor a simulação.
- Nível do viewport: visão geral, bloco, linha, pilha, tier ou slot.
- Ferramenta: ponteiro, mover, lupa, informação, seleção múltipla ou marcação de área.
- Estado da simulação: SIMULATED, BLOCKED ou READY_FOR_TRANSACTION.

## Permissões

A seleção e a navegação respeitam a autorização de leitura da tela aberta. Movimentações e alterações somente podem ser confirmadas por perfis autorizados pelo backend, como ADMIN_PORTO, PLANEJADOR ou OPERADOR_PATIO, conforme o contrato específico.

## Estados possíveis

- Sem seleção: nenhum elemento do domínio foi escolhido.
- Sincronizado: existe um contexto selecionado e compartilhado.
- Simulado: origem e destino foram comparados sem persistência.
- Bloqueado: a proposta possui inconsistência funcional.
- Pronto para confirmação transacional: a proposta foi justificada e deve seguir ao comando do domínio responsável.

## Motivos de bloqueio

- Origem ou destino ausente.
- Origem e destino pertencem ao mesmo domínio.
- Unidades diferentes nos contextos comparados.
- Origem sem identificação da unidade.
- Destino sem posição física.
- Pilha bloqueada, interditada, reservada ou incompatível.
- Permissão insuficiente.
- Rejeição na validação definitiva do backend.

## Exemplos

### Navio para pátio

Selecione MSCU1234567 no bay 12, row 02, tier 84. Em seguida, selecione sua pilha proposta no bloco A1, linha 2, coluna 3. Arraste Navio sobre Pátio, confira a WI e o CHE e encaminhe a proposta ao comando de movimentação do Yard.

### Pátio para ferrovia

Selecione uma unidade na pilha, abra o contexto ferroviário associado à work instruction e simule a transferência para o vagão e posição previstos. A operação somente é persistida pelo fluxo ferroviário autorizado.

## Atalhos

- Duplo clique: aprofundar um nível hierárquico.
- Backspace: retornar ao nível anterior preservando seleção, filtros e posição.
- + ou =: aumentar zoom.
- -: reduzir zoom.
- 0: restaurar zoom e deslocamento.
- Esc: cancelar retângulo ou seleção múltipla.
- Marcar área: desenhar um retângulo e listar as pilhas interceptadas.

## Processo completo

A seleção compartilhada é apenas a composição visual e a simulação. A confirmação transacional permanece no serviço responsável pelo destino, preservando validações de concorrência, autorização, idempotência, restrições físicas, auditoria e rollback definidos pelo backend.
