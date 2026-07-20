# BUS1610 e BUS1620 — Timeframes e estados operacionais 2D

## Situação

Implementado em 20 de julho de 2026.

## BUS1610 — horizontes gráficos

A interface gráfica do pátio e do Vessel Planner passa a representar os horizontes:

- `Current`: posição física e estados vigentes;
- `Future`: posição esperada após planos, restows e instruções ativas;
- `Stow`: plano de estivagem e destinos reservados;
- `Preplan`: propostas e planos tentativos ainda não definitivos;
- `Composite`: composição das fontes atual, planejada e iminente;
- `Imminent`: trabalhos prestes a iniciar ou em execução.

A comparação lado a lado preserva a mesma chave de seleção entre os horizontes. O inspector informa estado, fonte, instante de referência e detalhes operacionais, permitindo explicar diferenças sem substituir o dado físico pelo planejado.

## BUS1620 — estados operacionais completos

Os elementos gráficos usam símbolos, texto, classes visuais, legenda acessível, `aria-label` e tooltip para os estados:

- proposta `○`;
- tentativo `◐`;
- definitivo `●`;
- reservado `◇`;
- atribuído `◆`;
- despachado `➜`;
- em execução `▶`;
- bloqueado `⊘`;
- falha `!`;
- concluído `✓`.

A simbologia não depende somente de cor. Estados heterogêneos dos contratos existentes são normalizados sem alterar o valor persistido no backend.

## Fontes do pátio

O painel do pátio combina:

- inventário e restrições das posições reais;
- work instructions e allocations visíveis no mapa;
- planos preditivos do BUS1400;
- validade, origem, equipamento sugerido e horizonte dos planos.

API reutilizada:

- `GET /api/scheduler/planos-posicao`.

A integração foi adicionada ao fluxo gráfico de allocations do mapa operacional, sem alterar os comandos transacionais existentes.

## Fontes do navio

O painel do navio combina:

- slots do plano persistido;
- estado do plano de estivagem;
- movimentos de restow;
- sequenciamento e atribuição de guindastes;
- bloqueios operacionais retornados pelo backend.

APIs reutilizadas:

- `GET /api/vessel-planner/planos/{id}`;
- `GET /api/vessel-planner/planos/{id}/restow`;
- `GET /api/vessel-planner/planos/{id}/sequenciamento-guindastes`.

A integração foi adicionada ao workspace do Vessel Planner junto ao painel operacional de tampas de porão.

## Ajuda contextual

Os dois painéis possuem ícone de manual com:

- finalidade da tela;
- fluxo operacional;
- explicação dos campos;
- permissões necessárias;
- estados possíveis;
- motivos de bloqueio;
- exemplos;
- atalhos;
- link para este processo completo.

## Arquivos principais

- `frontend/cloudport/src/operational-timeframes.js`;
- `frontend/cloudport/src/components/OperationalTimeframes.jsx`;
- `frontend/cloudport/src/operational-timeframes.css`;
- `frontend/cloudport/src/pages/yard/YardAllocationEditor.jsx`;
- `frontend/cloudport/src/pages/VesselHatchCoverPanel.jsx`.

## Testes

`frontend/cloudport/src/operational-timeframes.test.js` cobre:

- normalização dos estados heterogêneos;
- projeção de plano tentativo no `Preplan` do pátio;
- prioridade de bloqueios sobre ocupação física;
- representação de sequenciamento iminente;
- destino reservado de restow no horizonte futuro;
- resumo dos estados apresentados no canvas.
