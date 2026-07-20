export const TIMEFRAME_MODES = Object.freeze([
  { value: 'CURRENT', label: 'Current', description: 'Posição física e estados vigentes.' },
  { value: 'FUTURE', label: 'Future', description: 'Posição esperada após planos e instruções ativas.' },
  { value: 'STOW', label: 'Stow', description: 'Plano de estivagem e destinos reservados.' },
  { value: 'PREPLAN', label: 'Preplan', description: 'Propostas e tentativas ainda não definitivas.' },
  { value: 'COMPOSITE', label: 'Composite', description: 'Composição do atual, planejado e iminente.' },
  { value: 'IMMINENT', label: 'Imminent', description: 'Movimentos prestes a iniciar ou em execução.' }
]);

export const OPERATIONAL_STATES = Object.freeze([
  { value: 'PROPOSTA', label: 'Proposta', symbol: '○', description: 'Sugestão ainda não reservada.' },
  { value: 'TENTATIVO', label: 'Tentativo', symbol: '◐', description: 'Plano persistido sujeito a conversão.' },
  { value: 'DEFINITIVO', label: 'Definitivo', symbol: '●', description: 'Plano confirmado e vigente.' },
  { value: 'RESERVADO', label: 'Reservado', symbol: '◇', description: 'Posição ou recurso reservado.' },
  { value: 'ATRIBUIDO', label: 'Atribuído', symbol: '◆', description: 'Trabalho associado a um recurso.' },
  { value: 'DESPACHADO', label: 'Despachado', symbol: '➜', description: 'Instrução enviada para execução.' },
  { value: 'EM_EXECUCAO', label: 'Em execução', symbol: '▶', description: 'Movimento iniciado fisicamente.' },
  { value: 'BLOQUEADO', label: 'Bloqueado', symbol: '⊘', description: 'Movimento impedido por regra ou restrição.' },
  { value: 'FALHA', label: 'Falha', symbol: '!', description: 'Execução rejeitada ou encerrada com erro.' },
  { value: 'CONCLUIDO', label: 'Concluído', symbol: '✓', description: 'Estado físico confirmado.' }
]);

const STATE_ALIASES = Object.freeze({
  PROPOSTA: 'PROPOSTA',
  PROPOSTO: 'PROPOSTA',
  CRIADA: 'PROPOSTA',
  CRIADO: 'PROPOSTA',
  PLANEJADA: 'PROPOSTA',
  PLANEJADO: 'PROPOSTA',
  PENDENTE: 'PROPOSTA',
  TENTATIVO: 'TENTATIVO',
  TENTATIVA: 'TENTATIVO',
  DEFINITIVO: 'DEFINITIVO',
  DEFINITIVA: 'DEFINITIVO',
  APROVADO: 'DEFINITIVO',
  APROVADA: 'DEFINITIVO',
  VALIDADO: 'DEFINITIVO',
  VALIDADA: 'DEFINITIVO',
  RESERVADO: 'RESERVADO',
  RESERVADA: 'RESERVADO',
  ATRIBUIDO: 'ATRIBUIDO',
  ATRIBUIDA: 'ATRIBUIDO',
  ASSIGNADO: 'ATRIBUIDO',
  ASSIGNED: 'ATRIBUIDO',
  DESPACHADO: 'DESPACHADO',
  DESPACHADA: 'DESPACHADO',
  IMINENTE: 'DESPACHADO',
  DISPATCHED: 'DESPACHADO',
  EM_EXECUCAO: 'EM_EXECUCAO',
  EXECUCAO: 'EM_EXECUCAO',
  INICIADO: 'EM_EXECUCAO',
  INICIADA: 'EM_EXECUCAO',
  RUNNING: 'EM_EXECUCAO',
  BLOQUEADO: 'BLOQUEADO',
  BLOQUEADA: 'BLOQUEADO',
  INTERDITADO: 'BLOQUEADO',
  INTERDITADA: 'BLOQUEADO',
  SUSPENSO: 'BLOQUEADO',
  SUSPENSA: 'BLOQUEADO',
  BLOCKED: 'BLOQUEADO',
  FALHA: 'FALHA',
  ERRO: 'FALHA',
  REJEITADO: 'FALHA',
  REJEITADA: 'FALHA',
  CANCELADO: 'FALHA',
  CANCELADA: 'FALHA',
  FAILED: 'FALHA',
  CONCLUIDO: 'CONCLUIDO',
  CONCLUIDA: 'CONCLUIDO',
  FINALIZADO: 'CONCLUIDO',
  FINALIZADA: 'CONCLUIDO',
  OCUPADO: 'CONCLUIDO',
  OCUPADA: 'CONCLUIDO',
  POSICIONADO: 'CONCLUIDO',
  POSICIONADA: 'CONCLUIDO',
  COMPLETED: 'CONCLUIDO'
});

const STATE_PRIORITY = Object.freeze({
  FALHA: 100,
  BLOQUEADO: 90,
  EM_EXECUCAO: 80,
  DESPACHADO: 70,
  ATRIBUIDO: 60,
  DEFINITIVO: 50,
  RESERVADO: 40,
  TENTATIVO: 30,
  PROPOSTA: 20,
  CONCLUIDO: 10
});

function normalized(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function numberValue(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function timestampValue(...values) {
  for (const value of values) {
    const timestamp = Date.parse(value);
    if (Number.isFinite(timestamp)) return new Date(timestamp).toISOString();
  }
  return null;
}

export function normalizeOperationalState(value, fallback = 'PROPOSTA') {
  return STATE_ALIASES[normalized(value)] ?? fallback;
}

export function operationalStateDescriptor(value) {
  const state = normalizeOperationalState(value);
  return OPERATIONAL_STATES.find((item) => item.value === state) ?? OPERATIONAL_STATES[0];
}

export function timeframeDescriptor(value) {
  const normalizedValue = normalized(value) || 'CURRENT';
  return TIMEFRAME_MODES.find((item) => item.value === normalizedValue) ?? TIMEFRAME_MODES[0];
}

export function yardPositionKey(value) {
  return `${String(value?.bloco ?? 'SEM_BLOCO')}:${numberValue(value?.linha)}:${numberValue(value?.coluna)}:${String(value?.camadaOperacional ?? value?.camada ?? '')}`;
}

export function vesselPositionKey(value) {
  return `${numberValue(value?.bay)}:${numberValue(value?.rowBay ?? value?.row)}:${numberValue(value?.tier)}`;
}

function descriptor(state, options = {}) {
  const metadata = operationalStateDescriptor(state);
  return {
    state: metadata.value,
    label: metadata.label,
    symbol: metadata.symbol,
    description: metadata.description,
    origin: options.origin ?? 'Fonte operacional',
    reference: options.reference ?? null,
    detail: options.detail ?? '',
    rawState: options.rawState ?? state,
    containerCode: options.containerCode ?? '',
    priority: options.priority ?? STATE_PRIORITY[metadata.value] ?? 0,
    dimmed: Boolean(options.dimmed)
  };
}

function currentYardDescriptor(layer) {
  if (layer?.interditada || layer?.bloqueada || layer?.areaPermitida === false) {
    return descriptor('BLOQUEADO', {
      origin: 'Cadastro da posição do pátio',
      reference: timestampValue(layer?.atualizadoEm, layer?.alteradoEm),
      detail: layer?.notaOperacional || 'Posição indisponível para alocação.',
      containerCode: layer?.codigoConteiner
    });
  }
  if (layer?.plannedOrder) {
    return descriptor(layer.plannedOrder.statusOrdem ?? layer.plannedOrder.status ?? 'RESERVADO', {
      origin: `Work instruction #${layer.plannedOrder.id ?? 'N/I'}`,
      reference: timestampValue(layer.plannedOrder.atualizadoEm, layer.plannedOrder.criadoEm),
      detail: layer.plannedOrder.motivoBloqueio ?? layer.plannedOrder.observacao ?? '',
      containerCode: layer.plannedOrder.codigoConteiner ?? layer.codigoConteiner
    });
  }
  if (layer?.ocupada || layer?.codigoConteiner) {
    return descriptor(layer?.statusConteiner ?? 'CONCLUIDO', {
      origin: 'Inventário físico do pátio',
      reference: timestampValue(layer?.atualizadoEm, layer?.registradoEm),
      detail: layer?.notaOperacional ?? 'Posição física confirmada.',
      containerCode: layer?.codigoConteiner
    });
  }
  return null;
}

function yardPlanDescriptor(plan) {
  return descriptor(plan?.estado ?? 'PROPOSTA', {
    origin: plan?.origem ? `Plano preditivo · ${plan.origem}` : `Plano preditivo #${plan?.id ?? 'N/I'}`,
    reference: timestampValue(plan?.atualizadoEm, plan?.convertidoEm, plan?.criadoEm),
    detail: [
      plan?.motivo,
      plan?.equipamentoId ? `CHE ${plan.equipamentoId}` : '',
      plan?.horizonteInicio && plan?.horizonteFim ? `Horizonte ${plan.horizonteInicio} a ${plan.horizonteFim}` : ''
    ].filter(Boolean).join(' · '),
    rawState: plan?.estado,
    containerCode: plan?.codigoContainer,
    priority: (STATE_PRIORITY[normalizeOperationalState(plan?.estado)] ?? 0) + 5
  });
}

function planMatchesTimeframe(plan, timeframe) {
  const state = normalized(plan?.estado);
  if (['CANCELADO', 'EXPIRADO'].includes(state)) return timeframe === 'COMPOSITE';
  if (timeframe === 'PREPLAN') return state === 'TENTATIVO';
  if (timeframe === 'STOW') return ['TENTATIVO', 'DEFINITIVO'].includes(state);
  if (timeframe === 'IMMINENT') return state === 'IMINENTE' || numberValue(plan?.segundosAteExpiracao, Number.POSITIVE_INFINITY) <= 3600;
  if (timeframe === 'FUTURE') return ['TENTATIVO', 'DEFINITIVO', 'IMINENTE'].includes(state);
  if (timeframe === 'COMPOSITE') return true;
  return false;
}

function orderMatchesTimeframe(order, timeframe) {
  if (!order) return false;
  const state = normalizeOperationalState(order.statusOrdem ?? order.status);
  if (timeframe === 'STOW') return ['PROPOSTA', 'RESERVADO', 'ATRIBUIDO', 'DESPACHADO'].includes(state);
  if (timeframe === 'PREPLAN') return ['PROPOSTA', 'TENTATIVO'].includes(state);
  if (timeframe === 'IMMINENT') return ['DESPACHADO', 'EM_EXECUCAO', 'BLOQUEADO'].includes(state);
  return ['FUTURE', 'COMPOSITE'].includes(timeframe);
}

function sortDescriptors(items) {
  return items.filter(Boolean).sort((left, right) => right.priority - left.priority);
}

export function buildYardTimeframeScene(blocks = [], plans = [], timeframe = 'CURRENT') {
  const mode = timeframeDescriptor(timeframe).value;
  const planIndex = new Map();
  (Array.isArray(plans) ? plans : []).forEach((plan) => {
    if (!planMatchesTimeframe(plan, mode)) return;
    const key = yardPositionKey(plan);
    const current = planIndex.get(key) ?? [];
    current.push(plan);
    planIndex.set(key, current);
  });

  const mappedBlocks = (Array.isArray(blocks) ? blocks : []).map((block) => ({
    bloco: block?.bloco ?? 'SEM_BLOCO',
    stacks: (block?.stacks ?? []).map((stack) => ({
      key: `${block?.bloco ?? 'SEM_BLOCO'}:${stack?.linha ?? ''}:${stack?.coluna ?? ''}`,
      bloco: block?.bloco ?? 'SEM_BLOCO',
      linha: stack?.linha,
      coluna: stack?.coluna,
      layers: (stack?.layers ?? []).map((layer) => {
        const currentDescriptor = currentYardDescriptor(layer);
        const planDescriptors = (planIndex.get(yardPositionKey({ ...layer, bloco: block?.bloco })) ?? []).map(yardPlanDescriptor);
        const orderDescriptor = orderMatchesTimeframe(layer?.plannedOrder, mode)
          ? currentYardDescriptor({ ...layer, ocupada: false })
          : null;
        let descriptors;
        if (mode === 'CURRENT') descriptors = [currentDescriptor];
        else if (mode === 'COMPOSITE') descriptors = [currentDescriptor, orderDescriptor, ...planDescriptors];
        else descriptors = [orderDescriptor, ...planDescriptors, currentDescriptor ? { ...currentDescriptor, dimmed: true, priority: currentDescriptor.priority - 15 } : null];
        const sorted = sortDescriptors(descriptors);
        const top = sorted[0] ?? null;
        return {
          key: yardPositionKey({ ...layer, bloco: block?.bloco }),
          id: layer?.id,
          label: String(layer?.camadaOperacional ?? layer?.camada ?? '—'),
          containerCode: top?.containerCode || layer?.codigoConteiner || '',
          state: top?.state ?? '',
          stateLabel: top?.label ?? 'Livre',
          symbol: top?.symbol ?? '·',
          origin: top?.origin ?? 'Posição livre',
          reference: top?.reference,
          detail: top?.detail ?? '',
          dimmed: Boolean(top?.dimmed),
          descriptors: sorted,
          restricted: Boolean(layer?.interditada || layer?.bloqueada || layer?.areaPermitida === false)
        };
      })
    }))
  }));

  const references = (Array.isArray(plans) ? plans : []).map((plan) => timestampValue(plan?.atualizadoEm, plan?.criadoEm)).filter(Boolean).sort();
  return {
    timeframe: mode,
    referenceTime: references.at(-1) ?? new Date().toISOString(),
    blocks: mappedBlocks
  };
}

function sequencePosition(operation) {
  return vesselPositionKey({
    bay: operation?.bay,
    rowBay: operation?.rowBay ?? operation?.row,
    tier: operation?.tier
  });
}

function restowPositions(movement) {
  return {
    source: vesselPositionKey({
      bay: movement?.bayAtual ?? movement?.bayOrigem,
      rowBay: movement?.rowAtual ?? movement?.rowOrigem,
      tier: movement?.tierAtual ?? movement?.tierOrigem
    }),
    destination: vesselPositionKey({
      bay: movement?.bayDestino,
      rowBay: movement?.rowDestino,
      tier: movement?.tierDestino
    })
  };
}

function vesselPlanState(plan) {
  const state = normalized(plan?.status);
  if (['APROVADO', 'VALIDADO', 'FINALIZADO', 'CONCLUIDO', 'TRANSMITIDO'].includes(state)) return 'DEFINITIVO';
  if (['RASCUNHO', 'PLANEJAMENTO', 'EM_PLANEJAMENTO'].includes(state)) return 'TENTATIVO';
  return 'PROPOSTA';
}

function operationMatchesTimeframe(operation, timeframe, index) {
  const state = normalizeOperationalState(operation?.status ?? (operation?.guindasteId ? 'ATRIBUIDO' : 'PROPOSTA'));
  if (timeframe === 'IMMINENT') return ['DESPACHADO', 'EM_EXECUCAO', 'BLOQUEADO'].includes(state) || index < 8;
  if (timeframe === 'PREPLAN') return ['PROPOSTA', 'TENTATIVO'].includes(state);
  return ['FUTURE', 'STOW', 'COMPOSITE'].includes(timeframe);
}

export function buildVesselTimeframeScene(plan = {}, restow = {}, sequencing = {}, timeframe = 'CURRENT') {
  const mode = timeframeDescriptor(timeframe).value;
  const operations = Array.isArray(sequencing?.sequencia) ? sequencing.sequencia : [];
  const movements = Array.isArray(restow?.movimentos) ? restow.movimentos : [];
  const operationIndex = new Map();
  operations.forEach((operation, index) => {
    if (!operationMatchesTimeframe(operation, mode, index)) return;
    const key = sequencePosition(operation);
    const current = operationIndex.get(key) ?? [];
    current.push({ operation, index });
    operationIndex.set(key, current);
  });
  const restowIndex = new Map();
  movements.forEach((movement) => {
    const positions = restowPositions(movement);
    const source = restowIndex.get(positions.source) ?? [];
    source.push({ movement, role: 'source' });
    restowIndex.set(positions.source, source);
    const destination = restowIndex.get(positions.destination) ?? [];
    destination.push({ movement, role: 'destination' });
    restowIndex.set(positions.destination, destination);
  });

  const planState = vesselPlanState(plan);
  const slots = (Array.isArray(plan?.slots) ? plan.slots : []).map((slot) => {
    const key = vesselPositionKey(slot);
    const current = slot?.codigoContainer
      ? descriptor('CONCLUIDO', {
        origin: `Plano persistido #${plan?.id ?? 'N/I'}`,
        reference: timestampValue(plan?.atualizadoEm, plan?.criadoEm),
        detail: `Posição atual B${slot?.bay} R${slot?.rowBay} T${slot?.tier}.`,
        containerCode: slot?.codigoContainer
      })
      : null;
    const stow = slot?.codigoContainer
      ? descriptor(planState, {
        origin: `Stow plan #${plan?.id ?? 'N/I'}`,
        reference: timestampValue(plan?.atualizadoEm, plan?.criadoEm),
        detail: `Plano ${plan?.status ?? 'sem status'} para ${slot?.codigoContainer}.`,
        containerCode: slot?.codigoContainer,
        priority: (STATE_PRIORITY[planState] ?? 0) + 2
      })
      : null;
    const operationDescriptors = (operationIndex.get(key) ?? []).map(({ operation, index }) => descriptor(
      operation?.status ?? (operation?.bloqueadoPorTampa ? 'BLOQUEADO' : operation?.guindasteId ? 'ATRIBUIDO' : 'PROPOSTA'),
      {
        origin: `Sequenciamento de guindastes · Q${operation?.guindasteId ?? 'N/I'}`,
        reference: timestampValue(operation?.atualizadoEm, sequencing?.geradoEm, plan?.atualizadoEm),
        detail: [
          `Ordem ${operation?.ordem ?? operation?.ordemPlanejada ?? index + 1}`,
          operation?.motivoBloqueioTampa,
          operation?.codigoContainer
        ].filter(Boolean).join(' · '),
        containerCode: operation?.codigoContainer ?? slot?.codigoContainer,
        priority: (STATE_PRIORITY[normalizeOperationalState(operation?.status ?? 'ATRIBUIDO')] ?? 0) + 4
      }
    ));
    const restowDescriptors = (restowIndex.get(key) ?? []).map(({ movement, role }) => descriptor(
      role === 'destination' ? 'RESERVADO' : movement?.status ?? 'EM_EXECUCAO',
      {
        origin: `Restow · ${role === 'destination' ? 'destino' : 'origem'}`,
        reference: timestampValue(movement?.atualizadoEm, restow?.geradoEm, plan?.atualizadoEm),
        detail: movement?.motivo ?? movement?.descricao ?? 'Reposicionamento operacional.',
        containerCode: movement?.codigoContainer ?? slot?.codigoContainer,
        priority: role === 'destination' ? 65 : 75
      }
    ));

    let descriptors;
    if (mode === 'CURRENT') descriptors = [current];
    else if (mode === 'PREPLAN') descriptors = [stow, ...operationDescriptors, current ? { ...current, dimmed: true, priority: 1 } : null];
    else if (mode === 'STOW') descriptors = [stow, ...restowDescriptors, ...operationDescriptors];
    else if (mode === 'IMMINENT') descriptors = [...operationDescriptors, ...restowDescriptors, current ? { ...current, dimmed: true, priority: 1 } : null];
    else if (mode === 'FUTURE') descriptors = [...restowDescriptors, ...operationDescriptors, stow, current ? { ...current, dimmed: true, priority: 1 } : null];
    else descriptors = [current, stow, ...operationDescriptors, ...restowDescriptors];
    const sorted = sortDescriptors(descriptors);
    const top = sorted[0] ?? null;
    return {
      key,
      id: slot?.id,
      bay: numberValue(slot?.bay),
      rowBay: numberValue(slot?.rowBay ?? slot?.row),
      tier: numberValue(slot?.tier),
      containerCode: top?.containerCode || slot?.codigoContainer || '',
      state: top?.state ?? '',
      stateLabel: top?.label ?? 'Livre',
      symbol: top?.symbol ?? '·',
      origin: top?.origin ?? 'Slot livre',
      reference: top?.reference,
      detail: top?.detail ?? '',
      dimmed: Boolean(top?.dimmed),
      descriptors: sorted,
      restricted: Boolean(slot?.restrito)
    };
  });

  return {
    timeframe: mode,
    referenceTime: timestampValue(sequencing?.geradoEm, restow?.geradoEm, plan?.atualizadoEm, plan?.criadoEm) ?? new Date().toISOString(),
    slots
  };
}

export function operationalSceneSummary(scene) {
  const elements = scene?.blocks
    ? scene.blocks.flatMap((block) => block.stacks.flatMap((stack) => stack.layers))
    : scene?.slots ?? [];
  return elements.reduce((summary, item) => {
    if (!item?.state) return summary;
    summary[item.state] = (summary[item.state] ?? 0) + 1;
    return summary;
  }, {});
}
