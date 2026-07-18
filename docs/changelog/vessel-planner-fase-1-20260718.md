# 2026-07-18 — Vessel Planner gráfico, fase 1

Implementada a consolidação das quatro vistas gráficas do planejamento de navio:

- profile;
- top;
- section;
- tier;
- multivisão.

A seleção de slot passou a manter bay, row e tier sincronizados entre todas as vistas e seletores. As células agregadas de profile e top agora aplicam a legenda predominante, enquanto section e tier usam a categoria individual do slot.

As legendas disponíveis são POD, peso, IMO, reefer e operador. Também foi mantida a movimentação por drag-and-drop para usuários autorizados.

Validação local:

- sintaxe JSX processada com esbuild;
- 4 testes unitários aprovados para sincronização de coordenadas, busca do slot mais próximo, legenda predominante e normalização da seleção.
