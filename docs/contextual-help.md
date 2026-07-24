# Ajuda contextual do portal

A interface React disponibiliza ajuda contextual nos cabeçalhos construídos com `PageHeader`.

O botão **Ajuda** resolve o conteúdo pela rota atual e apresenta:

- finalidade da página;
- fluxo recomendado;
- campos e informações principais;
- permissões esperadas e perfil autenticado;
- estados do processo;
- bloqueios e validações;
- exemplo operacional;
- atalhos de teclado;
- acesso ao processo relacionado e à documentação técnica.

## Resolução do conteúdo

O catálogo implementado em `frontend/cloudport/src/contextHelp.js` usa três níveis:

1. `PAGES`: conteúdo específico para uma rota exata;
2. `MODULES`: conteúdo compartilhado pelo prefixo do módulo, como Gate, Ferrovia, Pátio, Navio, Embarque, Billing e CAP;
3. `BASE`: conteúdo padrão quando não existe cadastro específico nem correspondência de módulo.

Assim, uma página com `PageHeader` continua exibindo ajuda mesmo quando ainda não possui uma entrada própria em `PAGES`. Para processos críticos, o cadastro específico deve informar finalidade, fluxo, campos, permissões, estados, bloqueios, exemplo, atalhos, rota do processo e URL da documentação correspondente.

## Atalhos

- `F1`: abre a ajuda da página atual;
- `Shift + ?`: abre a ajuda da página atual;
- `Esc`: fecha o painel e também é descrito no conteúdo padrão como atalho para fechar painéis e diálogos.

## Links

O conteúdo padrão aponta para o diretório `docs` da branch `main`. Entradas específicas podem substituir esse endereço por um manual de operação ou por uma seção exata da documentação.

## Manutenção

O catálogo fica em `frontend/cloudport/src/contextHelp.js`.

Ao criar uma nova tela:

- confirme que o cabeçalho utiliza `PageHeader`;
- adicione uma entrada em `PAGES` quando o processo exigir instruções próprias;
- use `MODULES` somente para orientações realmente comuns ao domínio;
- mantenha `processPath` e `processLabel` apontando para uma rota existente;
- mantenha `documentationUrl` apontando para um arquivo ou seção existente;
- descreva permissões e estados conforme os contratos efetivamente expostos pelo backend.
