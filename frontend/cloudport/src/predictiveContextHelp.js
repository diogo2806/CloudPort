const MANUAL_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/patio-planejamento-preditivo-yard-impact.md';

const HELP_BY_PATH = {
  '/home/patio/planejamento-recebimento': {
    title: 'Planejamento preditivo de posições',
    purpose: 'Agrupar recebimentos e controlar posições tentativas, definitivas e iminentes antes da execução operacional.',
    flow: [
      'Gere ou consulte as propostas calculadas pelo scheduler.',
      'Confira unidade, bloco, posição, horizonte, validade, origem e motivo.',
      'Converta a proposta para DEFINITIVO quando o plano estiver confirmado.',
      'Converta para IMINENTE quando a execução estiver próxima.',
      'No dispatch, permita que o sistema revalide transacionalmente propostas ainda TENTATIVAS.'
    ],
    fields: [
      'Estado: TENTATIVO, DEFINITIVO, IMINENTE, EXPIRADO ou CANCELADO.',
      'Horizonte: intervalo operacional previsto para uso da posição.',
      'Validade e tempo restante: prazo antes de exigir novo cálculo.',
      'Origem e assinatura: fonte do cálculo e chave de idempotência.',
      'Motivo, operador e versão: trilha auditável de cada conversão.'
    ],
    permissions: [
      'ADMIN_PORTO: consulta e conversão de estados.',
      'PLANEJADOR: consulta, confirmação, iminência e cancelamento.',
      'OPERADOR_PATIO: consulta; o dispatch revalida conforme a autorização operacional.'
    ],
    states: [
      'TENTATIVO: proposta calculada e ainda não confirmada.',
      'DEFINITIVO: posição confirmada para dispatch.',
      'IMINENTE: execução próxima e posição reservada operacionalmente.',
      'EXPIRADO: validade encerrada; exige novo cálculo.',
      'CANCELADO: proposta retirada por decisão operacional.'
    ],
    blockers: [
      'Validade encerrada.',
      'Destino da work instruction divergente da posição planejada.',
      'Transição de estado não permitida.',
      'Motivo ou operador ausente.',
      'Versão alterada concorrentemente.',
      'Perfil sem permissão.'
    ],
    example: 'Uma posição TENTATIVA válida é revalidada no dispatch. Se o destino coincidir, o sistema a converte para DEFINITIVO e registra a auditoria na mesma transação.',
    shortcuts: ['F1 ou Shift + ?: abrir esta ajuda.', 'Esc: fechar a ajuda.', 'Atualizar: recarregar planos.', 'Clique na linha: abrir detalhes e auditoria.'],
    processPath: '/home/patio/yard-impact',
    processLabel: 'Abrir Yard Impact',
    documentationUrl: MANUAL_URL
  },
  '/home/patio/yard-impact': {
    title: 'Yard Impact',
    purpose: 'Projetar por bloco e POW o impacto operacional das próximas seis a vinte e quatro horas.',
    flow: [
      'Escolha o horizonte, respeitando o mínimo de seis horas.',
      'Revise entradas, saídas, rehandles, reservas e work instructions.',
      'Identifique blocos saturados no mapa de calor.',
      'Confira demanda, cobertura e déficit de CHE por POW.',
      'Selecione bloco ou POW e desça até as unidades que geram o impacto.'
    ],
    fields: [
      'Horizonte em horas: período da projeção.',
      'Ocupação projetada: contêineres atuais, reservas, planos, entradas e saídas.',
      'Saturação: ocupação projetada igual ou superior a 85%.',
      'Demanda de CHE: quantidade estimada para as work instructions.',
      'Déficit: diferença entre demanda e equipamentos operacionais associados.',
      'Drill-down: unidade, posição, estado, equipamento, validade, origem e motivo.'
    ],
    permissions: [
      'ADMIN_PORTO: consulta completa.',
      'PLANEJADOR: consulta e uso no planejamento.',
      'OPERADOR_PATIO: consulta para preparação e execução.'
    ],
    states: ['CONTROLADO: bloco abaixo do limite de saturação.', 'SATURADO: bloco com ocupação projetada a partir de 85%.', 'COBERTO: POW com recursos suficientes.', 'BLOQUEADO: POW com ausência de cobertura ou déficit de CHE.'],
    blockers: ['Bloco sem capacidade cadastrada.', 'POW ou pool operacional ausente.', 'CHE real não associado.', 'Equipamento associado indisponível.', 'Déficit de CHE no horizonte.', 'Plano vencido ou sem posição válida.'],
    example: 'Ao projetar seis horas, um bloco com 88% aparece SATURADO. Selecione-o para listar as unidades, reservas e posições responsáveis pela pressão operacional.',
    shortcuts: ['F1 ou Shift + ?: abrir esta ajuda.', 'Esc: fechar a ajuda.', 'Atualizar projeção: recalcular com dados persistidos.', 'Clique no bloco ou POW: filtrar o drill-down.'],
    processPath: '/home/patio/planejamento-recebimento',
    processLabel: 'Abrir planejamento preditivo',
    documentationUrl: MANUAL_URL
  }
};

export function applyPredictiveContextHelp(path, baseHelp) {
  const override = HELP_BY_PATH[path];
  if (!override) return baseHelp;
  return { ...baseHelp, ...override, path: baseHelp.path, currentRoles: baseHelp.currentRoles };
}
