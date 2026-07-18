# 2026-07-18 — Vessel Planner gráfico, fase 3

Implementada a terceira fase do planejamento gráfico de navio:

- mapa e seção técnica com overlays de estabilidade, lashing, força estrutural, IMDG e risco combinado;
- segregação IMDG gráfica com distinção entre conflito retornado pelo backend e zona visual de atenção;
- sequência de operações agrupada em faixas por guindaste;
- destaque de operações bloqueadas e seleção do slot relacionado;
- fluxo gráfico de restow com origem, destino, ordem, estado e motivo;
- inspector da camada técnica ativa;
- atualização independente das análises de estabilidade e restow;
- documentação funcional e seis testes unitários das regras puras.

Os overlays auxiliam a leitura operacional. A aprovação técnica e normativa permanece sob responsabilidade das validações persistidas no backend.
