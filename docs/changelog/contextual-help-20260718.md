# 2026-07-18 — Ajuda contextual

A interface passou a disponibilizar ajuda contextual diretamente no cabeçalho das páginas. O conteúdo considera a rota e o módulo atual, apresenta orientações operacionais e permite abrir o processo relacionado.

Validação local executada:

- 4 testes unitários aprovados para normalização de rota, herança por módulo, fallback e pesquisa;
- sintaxe JSX validada para `ContextHelp.jsx` e `components.jsx`.

O build completo não foi executado localmente porque o ambiente não conseguiu resolver o domínio do GitHub para clonar as dependências do repositório.
