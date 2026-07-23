export const VISIT_DATE_FIELDS = [
  'eta',
  'ata',
  'etb',
  'atb',
  'inicioOperacao',
  'fimOperacao',
  'etd',
  'atd',
  'janelaRecebimentoInicio',
  'janelaRecebimentoFim',
  'cutoffOperacional'
];

export const VISIT_MILESTONE_GROUPS = [
  {
    key: 'previstos',
    title: 'Marcos previstos',
    description: 'Datas planejadas e mantidas pelo planejador antes e durante a escala.',
    fields: [
      { name: 'eta', label: 'ETA', help: 'Chegada prevista do navio.', editable: true, source: 'Planejamento da visita' },
      { name: 'etb', label: 'ETB', help: 'Atracação prevista.', editable: true, source: 'Planejamento da visita' },
      { name: 'etd', label: 'ETD', help: 'Partida prevista.', editable: true, source: 'Planejamento da visita' }
    ]
  },
  {
    key: 'realizados',
    title: 'Marcos realizados',
    description: 'Horários efetivos. Os campos automáticos são gravados pelas transições operacionais.',
    fields: [
      { name: 'ata', label: 'ATA', help: 'Chegada efetiva. Informada pelo operador quando confirmada.', editable: true, source: 'Confirmação operacional' },
      { name: 'atb', label: 'ATB', help: 'Atracação efetiva.', editable: false, source: 'Transição para ATRACADA' },
      { name: 'inicioOperacao', label: 'Início da operação', help: 'Início efetivo da operação.', editable: false, source: 'Transição para OPERANDO' },
      { name: 'fimOperacao', label: 'Fim da operação', help: 'Conclusão efetiva da operação.', editable: false, source: 'Transição para OPERACAO_CONCLUIDA' },
      { name: 'atd', label: 'ATD', help: 'Partida efetiva.', editable: false, source: 'Transição para PARTIU' }
    ]
  },
  {
    key: 'recebimento',
    title: 'Janela de recebimento e cutoff',
    description: 'Período autorizado para recebimento e limite operacional da escala.',
    fields: [
      { name: 'janelaRecebimentoInicio', label: 'Início da janela de recebimento', help: 'Início do período autorizado para recebimento.', editable: true, source: 'Planejamento da visita' },
      { name: 'janelaRecebimentoFim', label: 'Fim da janela de recebimento', help: 'Fim do período autorizado para recebimento.', editable: true, source: 'Planejamento da visita' },
      { name: 'cutoffOperacional', label: 'Cutoff operacional', help: 'Limite para aceite operacional associado à visita.', editable: true, source: 'Planejamento da visita' }
    ]
  }
];

const DATE_COMPARISONS = [
  ['eta', 'ata', 'ATA não pode ser anterior ao ETA.'],
  ['eta', 'etb', 'ETB não pode ser anterior ao ETA.'],
  ['etb', 'atb', 'ATB não pode ser anterior ao ETB.'],
  ['atb', 'inicioOperacao', 'Início da operação não pode ser anterior ao ATB.'],
  ['inicioOperacao', 'fimOperacao', 'Fim da operação não pode ser anterior ao início.'],
  ['eta', 'etd', 'ETD não pode ser anterior ao ETA.'],
  ['etd', 'atd', 'ATD não pode ser anterior ao ETD.'],
  ['janelaRecebimentoInicio', 'janelaRecebimentoFim', 'Fim da janela de recebimento não pode ser anterior ao início.']
];

function timestamp(value) {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? Number.NaN : parsed.getTime();
}

export function validateVisitMilestones(draft = {}) {
  const errors = [];

  for (const field of VISIT_DATE_FIELDS) {
    const value = draft[field];
    if (value && Number.isNaN(timestamp(value))) {
      errors.push(`O campo ${field} contém uma data inválida.`);
    }
  }

  for (const [startField, endField, message] of DATE_COMPARISONS) {
    const start = timestamp(draft[startField]);
    const end = timestamp(draft[endField]);
    if (start !== null && end !== null && !Number.isNaN(start) && !Number.isNaN(end) && end < start) {
      errors.push(message);
    }
  }

  return [...new Set(errors)];
}

export function isAutomaticVisitMilestone(fieldName) {
  return VISIT_MILESTONE_GROUPS
    .flatMap((group) => group.fields)
    .some((field) => field.name === fieldName && !field.editable);
}