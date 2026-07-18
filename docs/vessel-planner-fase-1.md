# Vessel Planner gráfico — Fase 1

A primeira fase do planejamento gráfico de navio está disponível na tela `/home/embarque/planejamento`.

## Vistas

- **Profile view**: perfil lateral organizado por bay e tier.
- **Top view**: vista superior organizada por bay e row.
- **Section view**: seção transversal do bay ativo.
- **Tier view**: distribuição longitudinal do tier ativo.
- **Multivisão**: apresenta as quatro vistas simultaneamente.

## Sincronização

A seleção mantém uma única coordenada `bay / row / tier` para todo o workspace. Ao selecionar uma célula ou alterar um dos seletores:

1. o slot correspondente, ou o mais próximo disponível, é localizado;
2. as quatro vistas atualizam o destaque;
3. section e tier passam a exibir a seção correta;
4. o inspector apresenta os dados do mesmo slot.

## Legendas

Os contêineres podem ser coloridos por:

- POD;
- faixa de peso;
- classe IMO;
- reefer ou dry;
- operador marítimo.

Nas células agregadas de profile e top, a cor representa a categoria predominante. Section e tier exibem a categoria individual de cada slot.

## Seleção e movimentação

- slots ocupados e livres são selecionáveis;
- o inspector mostra contêiner, POD, peso, IMO, reefer e operador;
- a load list e os slots ocupados permanecem arrastáveis para slots livres quando o usuário possui permissão;
- a seleção continua sincronizada após a movimentação.

## Arquivos principais

- `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx`
- `frontend/cloudport/src/vessel-planner-phase1.js`
- `frontend/cloudport/src/vessel-planner-phase1.css`
- `frontend/cloudport/src/vessel-planner-phase1.test.js`
