# Ajuda contextual do portal

A interface React disponibiliza ajuda contextual em todos os cabeçalhos construídos com `PageHeader`.

O botão **Ajuda** identifica a rota atual e apresenta:

- finalidade da página;
- fluxo recomendado;
- campos e informações principais;
- permissões esperadas e perfil autenticado;
- estados do processo;
- bloqueios e validações;
- exemplo operacional;
- atalhos de teclado;
- acesso ao processo relacionado e à documentação técnica.

## Atalhos

- `F1`: abre a ajuda da página atual;
- `Shift + ?`: abre a ajuda da página atual;
- `Esc`: fecha o painel.

## Manutenção

O catálogo fica em `frontend/cloudport/src/contextHelp.js`. Novas rotas podem receber uma entrada específica em `PAGES`; rotas não cadastradas utilizam automaticamente a ajuda do módulo correspondente e, por último, o conteúdo padrão do CloudPort.
