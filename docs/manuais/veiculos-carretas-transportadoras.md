# Veículos e carretas de transportadoras

## Finalidade da tela

Manter a frota rodoviária vinculada às transportadoras e disponibilizar apenas veículos ativos para novos agendamentos de Gate.

## Fluxo operacional

1. Pesquise a frota existente por placa, tipo ou transportadora.
2. Informe a placa principal, a placa da carreta quando aplicável, o modelo e o tipo.
3. Vincule o veículo à transportadora responsável.
4. Salve o cadastro e confirme a situação ativa.
5. Use a consulta de veículos elegíveis no agendamento.
6. Inative veículos indisponíveis sem apagar o histórico.

## Explicação dos campos

- **Placa do veículo:** identificação principal e única do cavalo, caminhão ou veículo.
- **Placa da carreta:** identificação opcional e única do semirreboque.
- **Modelo:** descrição comercial ou operacional.
- **Tipo:** caminhão, carreta, cavalo mecânico ou van.
- **Transportadora:** empresa proprietária ou responsável pela frota.
- **Situação:** ativo ou inativo.

## Permissões necessárias

- `ADMIN_PORTO`, `OPERADOR_GATE` e `PLANEJADOR`: consulta e manutenção de qualquer transportadora.
- `TRANSPORTADORA`: consulta e manutenção somente da própria frota, conforme vínculo do JWT com o documento da transportadora.

## Estados possíveis

- **ATIVO:** disponível para seleção em novos agendamentos.
- **INATIVO:** preservado para histórico, mas bloqueado em novas operações.

## Motivos de bloqueio

- placa principal já cadastrada;
- placa de carreta já cadastrada;
- placa principal igual à placa da carreta;
- transportadora inexistente;
- usuário de transportadora tentando acessar frota de outra empresa;
- campos obrigatórios ausentes.

## Exemplos

- Cavalo mecânico `ABC1D23`, carreta `DEF4G56`, modelo `R 450`, tipo `CAVALO_MECANICO`.
- Veículo antigo inativado permanece nos agendamentos históricos, mas não aparece em `/gate/frota/veiculos/elegiveis`.

## Atalhos

- Use a busca para localizar rapidamente uma placa.
- Use **Inativar** quando o veículo estiver em manutenção ou fora da frota.
- Use **Editar** para corrigir modelo, tipo ou placa da carreta.

## Processo completo

Cadastro da transportadora → cadastro da frota → seleção de veículo elegível → criação do agendamento → operação de Gate → histórico e faturamento.
