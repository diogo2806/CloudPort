const FLEET_DOCUMENTATION = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/veiculos-carretas-transportadoras.md';

const FLEET_HELP = {
  module: 'Gate · Frota',
  title: 'Veículos e carretas de transportadoras',
  purpose: 'Cadastrar e manter a frota rodoviária vinculada à transportadora que será usada nos agendamentos do Gate.',
  flow: [
    'Confirme a transportadora vinculada ou selecione a empresa quando seu perfil puder administrar todas.',
    'Informe placa do veículo, placa da carreta quando aplicável, modelo, tipo e situação.',
    'Salve o cadastro e confira a frota listada.',
    'Inative veículos indisponíveis para impedir novos agendamentos.',
    'No agendamento, selecione somente um veículo ativo apresentado pela lista elegível.'
  ],
  fields: [
    'Placa do veículo: identificação principal e única da unidade tratora.',
    'Placa da carreta: identificação opcional e única do semirreboque.',
    'Modelo e tipo: caracterizam o veículo para a operação.',
    'Transportadora: vínculo obrigatório; para o perfil TRANSPORTADORA é obtido da sessão autenticada.',
    'Situação: ATIVO permite novos agendamentos; INATIVO mantém o histórico e bloqueia novas utilizações.'
  ],
  permissions: [
    'TRANSPORTADORA: consulta e mantém somente a própria frota.',
    'ADMIN_PORTO, PLANEJADOR e OPERADOR_GATE: consultam e mantêm frotas conforme autorização operacional.'
  ],
  states: [
    'ATIVO: disponível na seleção de novos agendamentos.',
    'INATIVO: preservado para histórico, mas indisponível para novos agendamentos.',
    'CARREGANDO: vínculo ou frota sendo consultado.',
    'SEM REGISTROS: nenhuma unidade encontrada para os filtros.'
  ],
  blockers: [
    'Placa principal ou placa de carreta já cadastrada.',
    'Placa principal igual à placa da carreta.',
    'Transportadora autenticada sem vínculo cadastral válido.',
    'Tentativa de acessar ou alterar a frota de outra transportadora.',
    'Campo obrigatório ausente ou sessão sem permissão.'
  ],
  example: 'Cadastre o cavalo ABC1D23 com a carreta DEF4G56 na Transportadora Alfa. Ao inativá-lo, ele deixa de aparecer na criação de agendamentos, mas continua no histórico da frota.',
  shortcuts: ['F1 ou Shift + ?: abrir esta ajuda.', 'Ctrl + K ou Command + K: buscar a tela.', 'Atualizar: recarregar vínculo e frota.', 'Editar: carregar o veículo no formulário.'],
  processPath: '/home/gate/agendamentos',
  processLabel: 'Abrir agendamentos do Gate',
  documentationUrl: FLEET_DOCUMENTATION
};

const APPOINTMENT_HELP = {
  module: 'Gate · Agendamentos',
  title: 'Agendamento com veículo elegível',
  purpose: 'Criar e consultar reservas de atendimento usando um veículo ativo e pertencente à transportadora selecionada.',
  flow: [
    'Informe código e tipo de operação.',
    'Selecione a transportadora.',
    'Aguarde a consulta e escolha um veículo elegível pela placa.',
    'Informe motorista, janela, horários previstos e observações.',
    'Crie o agendamento e confira o registro na lista.'
  ],
  fields: [
    'Código: identificador único do agendamento.',
    'Transportadora: empresa responsável pelo atendimento.',
    'Veículo elegível: seleção filtrada por transportadora e situação ATIVO.',
    'Motorista: identificador do condutor cadastrado.',
    'Janela: faixa de atendimento com capacidade disponível.',
    'Chegada e saída previstas: horários futuros e compatíveis com a janela.'
  ],
  permissions: [
    'ADMIN_PORTO e PLANEJADOR: criam agendamentos.',
    'OPERADOR_GATE e TRANSPORTADORA: consultam os registros permitidos ao perfil.',
    'TRANSPORTADORA visualiza somente seus próprios agendamentos conforme o vínculo autenticado.'
  ],
  states: ['PENDENTE: reserva criada e aguardando atendimento.', 'CONFIRMADO: dados validados.', 'EM_ATENDIMENTO: operação iniciada.', 'CONCLUIDO: atendimento encerrado.', 'CANCELADO: reserva encerrada sem execução.'],
  blockers: [
    'Nenhum veículo ativo cadastrado para a transportadora.',
    'Veículo inativo ou pertencente a outra transportadora.',
    'Código já utilizado.',
    'Janela sem capacidade ou horários fora da faixa.',
    'Motorista, transportadora, veículo ou janela inexistente.',
    'Perfil sem permissão para criação.'
  ],
  example: 'Selecione a Transportadora Alfa e o veículo ABC1D23. O backend confirma que ele está ativo e vinculado à empresa antes de gravar o agendamento.',
  shortcuts: ['F1 ou Shift + ?: abrir esta ajuda.', 'Ctrl + K ou Command + K: buscar outra tela.', 'Atualizar: recarregar agendamentos, janelas e transportadoras.'],
  processPath: '/home/cadastros/frota',
  processLabel: 'Abrir cadastro de frota',
  documentationUrl: FLEET_DOCUMENTATION
};

export function resolveFleetContextHelp(path, baseHelp) {
  const cleanPath = String(path ?? '').split(/[?#]/, 1)[0].replace(/\/$/, '');
  if (cleanPath === '/home/gate/agendamentos') return { ...baseHelp, ...APPOINTMENT_HELP };
  if (cleanPath === '/home/cadastros/frota' || cleanPath === '/home/cap/frota') return { ...baseHelp, ...FLEET_HELP };
  return null;
}
