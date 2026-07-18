# 2026-07-18 — Vessel Planner gráfico, fase 2

Implementada a segunda fase do planejamento gráfico de navio:

- drag-and-drop entre load list e slots;
- pré-validação visual em todas as vistas;
- bloqueios por ocupação, restrição, suporte, peso, reefer, ISO, IMDG e OOG;
- peso atual e projetado por stack;
- inspector com motivos de bloqueio e avisos;
- sincronização dos estados persistidos das tampas de porão;
- seleção bidirecional entre tampa e slots;
- documentação e testes unitários das regras puras.

Validação executada com `node --test`: 6 testes aprovados para origem, peso VGM, restrições, suporte da pilha, limites de peso, reefer e estados das tampas.

A validação local é preventiva. O backend continua responsável pela confirmação definitiva da movimentação.
