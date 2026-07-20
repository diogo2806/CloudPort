# BUS1400 e BUS1410 — Planejamento preditivo de pátio

## Situação

Implementado em 20 de julho de 2026.

## BUS1400 — estados do planejamento

A implementação adiciona o agregado persistido `PlanoPosicaoOperacional`, com os estados:

- `TENTATIVO`;
- `DEFINITIVO`;
- `IMINENTE`;
- `EXPIRADO`;
- `CANCELADO`.

Cada plano mantém unidade, posição, equipamento sugerido, horizonte, validade, origem, assinatura idempotente, motivo, operador, versão e datas de criação e alteração.

As conversões são registradas em `historico_plano_posicao_operacional`. A versão otimista impede sobrescrita concorrente. Durante o dispatch, um plano tentativo somente é convertido quando permanece válido e o destino da work instruction coincide com a posição planejada. Divergência ou expiração retorna conflito e impede o dispatch.

## BUS1410 — Yard Impact

A projeção aceita horizonte de seis a vinte e quatro horas e consolida dados persistidos de:

- posições e capacidade por bloco;
- contêineres atualmente posicionados;
- reservas ativas;
- planos de posição ativos;
- work queues;
- work instructions;
- equipamentos operacionais.

O resultado apresenta entradas, saídas, rehandles, reservas, ocupação projetada, saturação por bloco, demanda de CHE, cobertura e déficit por POW, motivos de bloqueio e drill-down até as unidades responsáveis.

## APIs

- `GET /api/scheduler/planos-posicao`
- `GET /api/scheduler/planos-posicao/{planoId}/historico`
- `POST /api/scheduler/planos-posicao/{planoId}/estado`
- `GET /api/scheduler/yard-impact?horizonteHoras=6`

## Telas

- `/home/patio/planejamento-recebimento`
- `/home/patio/yard-impact`

As telas possuem ajuda contextual com finalidade, fluxo operacional, campos, permissões, estados, bloqueios, exemplos, atalhos e acesso ao manual completo.

## Persistência

Migration: `V219__planejamento_preditivo_yard_impact.sql`.

Tabelas:

- `plano_posicao_operacional`;
- `historico_plano_posicao_operacional`.

## Testes adicionados

`PlanoPosicaoOperacionalServicoTest` cobre:

- conversão transacional de plano tentativo válido durante o dispatch;
- bloqueio de plano expirado;
- bloqueio de destino divergente.
