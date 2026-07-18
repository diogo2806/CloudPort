# Vessel Planner gráfico — Fase 2

A fase 2 complementa as quatro vistas sincronizadas com interação operacional, pré-validação visual e acompanhamento das tampas de porão.

## Drag-and-drop

Os contêineres podem ser arrastados da load list ou de um slot ocupado para um destino livre. Durante o arraste, profile, top, section e tier views compartilham a mesma origem e calculam a situação de cada destino.

Estados apresentados:

- `VALID`: destino compatível com as regras locais;
- `WARNING`: operação permitida, mas com condição que exige atenção;
- `BLOCKED`: operação não é enviada ao backend.

A validação do backend continua sendo a decisão definitiva após o drop.

## Validações visuais

A pré-validação considera:

- slot ocupado ou restrito;
- origem igual ao destino;
- contêiner com carga acima;
- destino que deixaria vão na pilha;
- limite individual de peso do slot;
- limite acumulado de peso da stack;
- compatibilidade de ISO quando configurada;
- tomada para contêiner reefer;
- permissão para carga IMDG e OOG;
- avisos persistidos pelo backend;
- estado e tarefa operacional da tampa de porão.

As células usam bordas verdes, âmbar e vermelhas. O painel de validação apresenta os motivos e o peso projetado antes da confirmação.

## Peso acumulado por stack

O painel de stacks apresenta:

- bay e row;
- ocupação atual;
- peso atual ou projetado durante o drag;
- limite configurado;
- percentual de utilização.

Classificação:

- abaixo de 85%: normal;
- de 85% a 100%: atenção;
- acima de 100%: bloqueado.

## Tampas de porão

O estado persistido das tampas é carregado pelo endpoint do Vessel Planner e sincronizado com as vistas.

Regras visuais locais:

- tampa fechada bloqueia acesso aos slots do porão;
- tampa aberta ou removida bloqueia ocupação sobre a abertura;
- tampa posicionada bloqueia movimentações até a confirmação do fechamento;
- tarefa em execução bloqueia os slots relacionados;
- tarefa planejada gera aviso.

Selecionar uma tampa no painel posiciona as quatro vistas em um slot relacionado. Selecionar um slot destaca a tampa correspondente.

## Arquivos principais

- `frontend/cloudport/src/pages/VesselPlannerWorkspace.jsx`
- `frontend/cloudport/src/pages/VesselHatchCoverPanel.jsx`
- `frontend/cloudport/src/vessel-planner-phase2.js`
- `frontend/cloudport/src/vessel-planner-phase2.css`
- `frontend/cloudport/src/vessel-planner-phase2.test.js`
