# Vessel Planner gráfico completo

## Finalidade

O Vessel Planner gráfico permite planejar, revisar e acompanhar a estivagem de contêineres usando a geometria persistida do navio. O backend continua sendo a fonte definitiva para restrições, aprovação técnica, segregação IMDG e execução operacional.

## Vistas disponíveis

- **Multivisão**: profile, top, section, tier e scan no mesmo contexto.
- **Profile**: perfil lateral por bay e tier, incluindo tampas de porão.
- **Top**: ocupação e peso acumulado por stack.
- **Scan**: resumo navegável por bay com ocupação, peso, restrições, alertas, restow, guindastes e slots.
- **Section**: corte transversal do bay selecionado.
- **Tier**: plano longitudinal do tier selecionado.

A seleção de bay, row, tier e slot é compartilhada entre todas as vistas, o inspector, o mapa técnico, a segregação IMDG, o restow e a sequência dos guindastes.

## Fluxo operacional

1. Selecione a escala, o Bay Plan e abra ou crie o plano persistido.
2. Escolha a vista e a legenda desejada.
3. Selecione um slot para consultar posição, contêiner, peso, POD, IMO, reefer, operador, limites e restrições.
4. Arraste um contêiner da load list para um slot livre ou mova um contêiner entre slots.
5. Revise a validação visual antes de confirmar a movimentação.
6. Consulte tampas de porão e peso acumulado por stack.
7. Execute as análises de estabilidade, restow e sequência dos guindastes.
8. Ative a camada técnica necessária e valide o plano no backend.

## Legendas

Os slots podem ser coloridos por:

- POD;
- faixa de peso;
- classe IMO;
- reefer;
- operador marítimo.

## Drag-and-drop e validação

Durante o arraste, os destinos são classificados como:

- válido;
- atenção;
- bloqueado;
- restrito.

A validação considera slot ocupado, restrição cadastral, origem igual ao destino, limite individual, peso acumulado da stack, geometria, ISO, reefer, IMDG, OOG e estado da tampa de porão quando os dados estiverem disponíveis.

## Tampas de porão

A tampa vinculada ao slot é exibida no profile, no slot e no inspector. Estados fechados, removidos, posicionados ou em operação podem bloquear a movimentação conforme a regra retornada pelo backend.

## Peso acumulado por stack

O painel de stack apresenta peso atual, limite, percentual e projeção durante o arraste. Os níveis visuais são normal, atenção e excedido.

## Segregação IMDG

O painel IMDG exibe classe, número ONU, grupo de segregação, conflitos retornados pelo backend e zonas gráficas de atenção. A proximidade visual não substitui a validação normativa persistida.

## Sequência dos guindastes

As operações são agrupadas por guindaste e ordenadas conforme o sequenciamento calculado. O operador pode localizar o slot da operação, identificar bloqueios por tampa e acompanhar a execução planejado × realizado.

## Restow

O restow gráfico apresenta origem, destino, ordem, estado e motivo. Origem e destino podem ser selecionados para sincronizar todas as vistas.

## Camadas técnicas

As camadas disponíveis são:

- segregação IMDG;
- estabilidade;
- lashing;
- força estrutural;
- risco combinado.

A camada ativa é aplicada diretamente aos slots e células agregadas, ao inspector e ao dashboard técnico. O risco combinado usa o maior nível entre os componentes disponíveis.

## Permissões

- **ADMIN_PORTO** e **PLANEJADOR**: criação, alocação, movimentação, autoestivagem e validação do plano.
- Demais perfis: consulta em modo somente leitura.

Planos em estado final não aceitam novas movimentações.

## Estados e motivos de bloqueio

Os principais motivos são:

- plano finalizado ou bloqueado;
- slot ocupado ou restrito;
- origem igual ao destino;
- excesso de peso do slot ou da stack;
- vão livre na stack;
- incompatibilidade de ISO, reefer, IMDG ou OOG;
- tampa de porão incompatível com a operação;
- violação de estabilidade, lashing ou força estrutural.

## Atalhos operacionais

- Selecionar bay, row ou tier nos controles superiores reposiciona todas as vistas.
- Clicar em uma stack, operação de guindaste, restow ou carga IMDG seleciona o slot relacionado.
- A multivisão permite comparar profile, top, section, tier e scan sem trocar de contexto.
- O seletor de overlay altera simultaneamente os slots, o inspector e o dashboard técnico.
