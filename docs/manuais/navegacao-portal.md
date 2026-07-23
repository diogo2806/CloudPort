# Portal CloudPort — navegação, busca, favoritos e recentes

## Finalidade

A navegação lateral permite localizar e abrir telas operacionais do CloudPort sem expor rotas incompatíveis com as permissões do usuário. Ela combina módulos, ícones, grupos recolhíveis, busca global, favoritos e histórico recente.

## Fluxo operacional

1. Abra o menu lateral pelo botão de menu em telas menores ou use a barra fixa no desktop.
2. Localize o módulo pelo ícone e pelo nome do grupo.
3. Expanda o grupo e selecione a tela desejada.
4. Use a busca global quando não souber em qual grupo a tela está.
5. Fixe telas frequentes com a estrela.
6. Reabra as últimas telas pela seção **Recentes**.
7. Confirme o contexto ativo no topo, onde são exibidos o título real da tela e o breadcrumb do módulo.

## Campos e ações

- **Buscar telas e comandos:** pesquisa por nome, módulo, sinônimo operacional e rota. A pesquisa não diferencia letras maiúsculas, minúsculas ou acentos.
- **Grupo:** conjunto de telas do mesmo domínio, como Gate, Ferrovia, Pátio ou Navio e embarque.
- **Contador do grupo:** quantidade de telas atualmente visíveis para o usuário naquele grupo.
- **Seta do grupo:** expande ou recolhe as telas. O estado é salvo por usuário.
- **Ícone do módulo:** referência visual do domínio operacional.
- **Ícone da tela:** referência visual da função principal.
- **Estrela vazia:** adiciona a tela aos favoritos.
- **Estrela preenchida:** remove a tela dos favoritos.
- **Favoritos:** telas fixadas pelo usuário e ainda autorizadas.
- **Recentes:** últimas telas abertas, sem repetir os favoritos.
- **Item ativo:** tela atual destacada e marcada semanticamente como página corrente.
- **Breadcrumb:** módulo e nome legível da tela atual.
- **Botão de menu:** abre ou fecha a navegação em tablet e celular.

## Permissões necessárias

A navegação é construída a partir das rotas padrão e da configuração dinâmica retornada pelo backend. Antes da exibição, cada item é filtrado pelos papéis da sessão.

Uma rota removida das permissões também é removida da busca, dos favoritos e dos recentes. Dados persistidos localmente não concedem acesso e são sanitizados quando o menu é reconstruído. O backend permanece responsável por autorizar cada API e operação.

## Estados possíveis

- **Grupo recolhido:** itens ocultos, cabeçalho disponível para expansão.
- **Grupo expandido:** itens visíveis.
- **Busca vazia:** navegação normal, com favoritos e recentes.
- **Busca ativa:** somente grupos e itens compatíveis com o termo são exibidos; os grupos encontrados permanecem abertos.
- **Sem resultado:** nenhuma tela autorizada corresponde ao termo.
- **Favorito:** caminho persistido para o usuário atual.
- **Recente:** caminho autorizado registrado entre os últimos acessos.
- **Menu móvel fechado:** conteúdo ocupa a largura disponível.
- **Menu móvel aberto:** barra lateral sobreposta com backdrop para fechamento.

## Motivos de bloqueio

- **Tela não aparece:** o papel atual não possui acesso ou a rota foi desabilitada na configuração dinâmica.
- **Favorito desapareceu:** a permissão foi removida, a rota deixou de existir ou a configuração dinâmica mudou.
- **Busca não retorna uma tela:** o item não está autorizado ou o termo não corresponde ao nome, módulo, sinônimo ou rota.
- **Persistência não é mantida:** o navegador bloqueou ou limpou o armazenamento local.
- **Grupo volta a abrir:** o grupo contém a tela ativa e precisa permanecer visível para indicar o contexto.
- **Atalho não funciona dentro de um campo:** atalhos globais são ignorados durante a edição para não interferir na digitação.

## Exemplos

### Localizar o inventário

Digite `contêiner`, `conteiner`, `inventário` ou parte da rota `/home/patio/inventario`. O resultado mostra apenas a tela autorizada de inventário do Pátio.

### Fixar a lista de trabalho ferroviária

Abra **Ferrovia > Lista de trabalho** e selecione a estrela. A tela passa a aparecer em **Favoritos** após o recarregamento, desde que a permissão continue válida.

### Reabrir uma tela recente

Após alternar entre Gate, Pátio e Navio, abra **Recentes** e selecione uma das últimas telas. Itens que já estão em Favoritos não são repetidos nessa seção.

## Atalhos

- `Ctrl + K` no Windows ou Linux: abre o menu e posiciona o foco na busca global.
- `Command + K` no macOS: abre o menu e posiciona o foco na busca global.
- `F1`: abre o manual contextual da tela ativa.
- `Shift + ?`: abre o manual contextual fora de campos de edição.
- `Esc`: fecha o manual contextual.
- `Tab` e `Shift + Tab`: percorrem grupos, telas, favoritos e ações.
- `Enter` ou `Espaço`: ativa o botão em foco.

## Acessibilidade e responsividade

- Grupos informam `aria-expanded` e a região controlada.
- A tela atual usa `aria-current="page"`.
- Favoritos informam `aria-pressed` e possuem ação nomeada.
- Ícones decorativos são ocultados de leitores de tela; o texto permanece como fonte do significado.
- O menu móvel informa se está aberto e possui backdrop nomeado.
- O foco possui contraste visual próprio.
- Animações são desabilitadas quando o sistema solicita movimento reduzido.

## Processo completo

A navegação é montada em quatro etapas: carregar as abas dinâmicas, mesclar com as rotas padrão, filtrar pela sessão e aplicar preferências válidas do usuário. Busca, favoritos e recentes operam somente sobre esse resultado autorizado. Ao abrir uma tela, o portal atualiza o histórico, expande o grupo ativo, fecha o menu móvel e apresenta o título e o breadcrumb correspondentes.