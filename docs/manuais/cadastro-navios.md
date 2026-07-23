# Cadastro de navios

## Finalidade da tela

A tela **Navio e embarque > Navios** mantém o cadastro mestre das embarcações utilizadas pelo line-up, pelas escalas, pela alocação de berços e pelo planejamento de estiva.

Rota do portal: `/home/navio/cadastros`.

## Fluxo operacional

1. Acesse **Navio e embarque > Navios**.
2. Consulte a grade para verificar se a embarcação já está cadastrada.
3. Para criar um registro, preencha os campos obrigatórios e selecione **Cadastrar navio**.
4. Para alterar, selecione uma linha da grade, revise os dados e selecione **Atualizar navio**.
5. Para excluir, selecione um navio e use **Excluir**. A exclusão somente deve ser feita quando o cadastro não possuir dependências operacionais.
6. Use **Atualizar** para recarregar a lista persistida no backend.

## Explicação dos campos

- **Nome**: nome oficial ou operacional da embarcação.
- **Código IMO**: identificador internacional no formato `IMO` seguido de sete dígitos, por exemplo `IMO9319466`.
- **País da bandeira**: país de registro da embarcação.
- **Empresa armadora**: empresa responsável pela operação comercial do navio.
- **Capacidade (TEU)**: capacidade nominal em unidades equivalentes a contêineres de vinte pés.
- **LOA (m)**: comprimento total da embarcação em metros.
- **Calado máximo (m)**: maior calado operacional informado para validações de berço.
- **Call sign**: indicativo internacional de chamada por rádio.

## Permissões necessárias

- **ADMIN_PORTO**: consulta, criação, alteração e exclusão.
- **PLANEJADOR**: consulta, criação e alteração.
- **OPERADOR_GATE**: consulta, sem acesso aos comandos de criação, alteração e exclusão.
- A configuração dinâmica de navegação pode autorizar outros perfis somente para consulta; os comandos permanecem bloqueados pelas regras da tela.

## Estados possíveis

- **Carregando**: a lista está sendo consultada.
- **Sem registros**: não existem navios retornados pela API.
- **Consulta**: nenhum registro está selecionado para edição.
- **Cadastro**: o formulário está preparado para um novo navio.
- **Edição**: um navio da grade foi selecionado.
- **Salvando**: criação ou atualização em andamento.
- **Sucesso**: operação confirmada pelo backend.
- **Erro**: validação, autorização, conflito ou indisponibilidade impediu a operação.

## Motivos de bloqueio

- Perfil sem permissão para criar, alterar ou excluir.
- Nome, código IMO, país da bandeira, empresa armadora ou capacidade não preenchidos.
- Código IMO fora do padrão `IMO9999999`.
- Capacidade menor que um TEU.
- LOA ou calado máximo informados com valor inválido.
- Código IMO já cadastrado.
- Navio vinculado a escala, visita, line-up, plano de estiva ou outro registro que impeça exclusão.
- Sessão expirada ou falha de comunicação com a API.

## Exemplo

Para cadastrar o navio **Cloud Atlantic**, informe `IMO9319466`, bandeira `Brasil`, armador `Cloud Shipping`, capacidade `5000`, LOA `280`, calado máximo `13.5` e call sign `PPCA`. Revise os valores e selecione **Cadastrar navio**.

## Atalhos

- O cadastro fica no mesmo módulo do **Line-up de navios**, em **Navio e embarque > Navios**.
- **Atualizar**: recarrega a grade.
- Selecionar uma linha: carrega o registro para edição ou consulta.
- **Limpar**: abandona a seleção e volta ao modo de cadastro.

## Processo completo

Após o cadastro, o navio pode ser utilizado no [line-up de navios](../../frontend/cloudport/src/pages/VesselLineUpPage.jsx), no planejamento de berços e no [planejamento de estiva](../../frontend/cloudport/src/pages/ContainerVesselPlannerPage.jsx). A tela operacional do processo fica em `/home/navio/line-up`.
