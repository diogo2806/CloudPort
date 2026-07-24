# Veículos e carretas de transportadoras

## Finalidade da tela

Manter a frota rodoviária vinculada às transportadoras e disponibilizar apenas veículos ativos para novos agendamentos de Gate.

As rotas operacionais são:

- `/home/cadastros/frota`: manutenção administrativa da frota;
- `/home/cap/frota`: manutenção da própria frota no Portal da Transportadora;
- `/home/gate/agendamentos`: criação e consulta de agendamentos com seleção de veículo elegível.

## Fluxo operacional

1. Pesquise a frota existente por placa, tipo ou transportadora.
2. Informe a placa principal, a placa da carreta quando aplicável, o modelo e o tipo.
3. Vincule o veículo à transportadora responsável. No perfil `TRANSPORTADORA`, o vínculo é obtido da sessão autenticada.
4. Salve o cadastro e confirme a situação ativa.
5. Abra os agendamentos, selecione a transportadora e escolha o veículo pela placa na lista de elegíveis.
6. Informe motorista, janela e horários previstos para criar o agendamento.
7. Inative veículos indisponíveis sem apagar o histórico.

## Explicação dos campos

- **Placa do veículo:** identificação principal e única do cavalo, caminhão ou veículo.
- **Placa da carreta:** identificação opcional e única do semirreboque.
- **Modelo:** descrição comercial ou operacional.
- **Tipo:** caminhão, carreta, cavalo mecânico ou van.
- **Transportadora:** empresa proprietária ou responsável pela frota. Para o perfil `TRANSPORTADORA`, aparece como vínculo somente leitura.
- **Situação:** ativo ou inativo.
- **Veículo elegível no agendamento:** seleção amigável que mostra somente veículos ativos da transportadora escolhida.
- **Janela e horários previstos:** período autorizado para o atendimento do veículo.

## Permissões necessárias

- `ADMIN_PORTO`, `OPERADOR_GATE` e `PLANEJADOR`: consulta e manutenção de qualquer transportadora.
- `ADMIN_PORTO` e `PLANEJADOR`: criação de agendamentos.
- `TRANSPORTADORA`: consulta e manutenção somente da própria frota, conforme vínculo do JWT com o documento da transportadora.
- `OPERADOR_GATE` e `TRANSPORTADORA`: consulta de agendamentos conforme o escopo permitido.

## Estados possíveis

- **ATIVO:** disponível para seleção em novos agendamentos.
- **INATIVO:** preservado para histórico, mas bloqueado em novas operações.
- **PENDENTE:** agendamento criado e aguardando atendimento.
- **EM_ATENDIMENTO:** operação iniciada no Gate.
- **CONCLUÍDO:** atendimento encerrado.
- **CANCELADO:** agendamento encerrado sem execução.

## Motivos de bloqueio

- placa principal já cadastrada;
- placa de carreta já cadastrada;
- placa principal igual à placa da carreta;
- transportadora inexistente ou sem vínculo com a sessão autenticada;
- usuário de transportadora tentando acessar frota de outra empresa;
- veículo inativo selecionado para um novo agendamento;
- veículo pertencente a outra transportadora;
- janela sem capacidade ou horários fora da faixa;
- campos obrigatórios ausentes.

O frontend filtra a lista para facilitar a operação, mas o `AgendamentoService` repete as validações de situação e vínculo antes de gravar o agendamento.

## Exemplos

- Cavalo mecânico `ABC1D23`, carreta `DEF4G56`, modelo `R 450`, tipo `CAVALO_MECANICO`.
- Veículo antigo inativado permanece nos agendamentos históricos, mas não aparece em `/gate/frota/veiculos/elegiveis`.
- Ao selecionar a Transportadora Alfa em `/home/gate/agendamentos`, a lista apresenta somente as placas ativas vinculadas à empresa.

## Atalhos

- `F1` ou `Shift + ?`: abrir o manual contextual da tela.
- `Ctrl + K` ou `Command + K`: localizar a tela no menu do portal.
- Use a busca para localizar rapidamente uma placa.
- Use **Inativar** quando o veículo estiver em manutenção ou fora da frota.
- Use **Editar** para corrigir modelo, tipo ou placa da carreta.
- Use **Atualizar** para recarregar vínculo, frota, janelas e agendamentos.

## Processo completo

Cadastro da transportadora → cadastro da frota → seleção de veículo elegível → criação do agendamento → operação de Gate → histórico e faturamento.
