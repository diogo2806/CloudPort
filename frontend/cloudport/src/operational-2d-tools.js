export const FLOW_PATTERNS = Object.freeze([
  { value: 'STACK_WISE', label: 'Stack-wise' },
  { value: 'TIER_WISE', label: 'Tier-wise' }
]);

export const FILL_DIRECTIONS = Object.freeze([
  { value: 'FORWARD', label: 'Crescente' },
  { value: 'REVERSE', label: 'Decrescente' }
]);

function text(value) {
  return String(value ?? '').trim();
}

function numberValue(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function array(value) {
  return Array.isArray(value) ? value : [];
}

function identifier(value, fallback) {
  return text(value?.id ?? value?.codigo ?? value?.code ?? value?.codigoConteiner ?? value?.containerCode) || fallback;
}

function unitLength(unit) {
  const value = numberValue(unit?.comprimentoPes ?? unit?.lengthFeet ?? unit?.tamanho ?? unit?.isoLength, 0);
  if (value) return value;
  const iso = text(unit?.codigoIso ?? unit?.isoCode);
  return iso.startsWith('2') ? 20 : iso.startsWith('4') ? 40 : 0;
}

function destinationCoordinates(destination) {
  return {
    bay: numberValue(destination?.bay ?? destination?.bloco ?? destination?.linha),
    row: numberValue(destination?.row ?? destination?.rowBay ?? destination?.coluna),
    tier: numberValue(destination?.tier ?? destination?.camada ?? destination?.camadaOperacional)
  };
}

function compareCoordinates(left, right, pattern) {
  const a = destinationCoordinates(left);
  const b = destinationCoordinates(right);
  const fields = pattern === 'TIER_WISE' ? ['tier', 'bay', 'row'] : ['bay', 'row', 'tier'];
  for (const field of fields) {
    if (a[field] !== b[field]) return a[field] - b[field];
  }
  return text(identifier(left, '')).localeCompare(text(identifier(right, '')));
}

function alternateBayOrder(destinations) {
  const groups = new Map();
  destinations.forEach((destination) => {
    const bay = destinationCoordinates(destination).bay;
    const current = groups.get(bay) ?? [];
    current.push(destination);
    groups.set(bay, current);
  });
  const bays = [...groups.keys()].sort((left, right) => left - right);
  const result = [];
  let start = 0;
  let end = bays.length - 1;
  while (start <= end) {
    result.push(...(groups.get(bays[start]) ?? []));
    if (start !== end) result.push(...(groups.get(bays[end]) ?? []));
    start += 1;
    end -= 1;
  }
  return result;
}

export function buildFlowPreview({
  units = [],
  destinations = [],
  pattern = 'STACK_WISE',
  direction = 'FORWARD',
  paired20 = false,
  alternateBays = false
} = {}) {
  const normalizedUnits = array(units).filter(Boolean);
  let orderedDestinations = array(destinations).filter(Boolean).sort((left, right) => compareCoordinates(left, right, pattern));
  if (alternateBays) orderedDestinations = alternateBayOrder(orderedDestinations);
  if (direction === 'REVERSE') orderedDestinations.reverse();

  const errors = [];
  if (normalizedUnits.length === 0) errors.push('Selecione ao menos uma unidade.');
  if (orderedDestinations.length === 0) errors.push('Selecione ao menos um destino.');
  if (normalizedUnits.length > orderedDestinations.length) errors.push('A quantidade de destinos é menor que a quantidade de unidades.');

  const moves = normalizedUnits.slice(0, orderedDestinations.length).map((unit, index) => {
    const destination = orderedDestinations[index];
    const previous = index > 0 ? normalizedUnits[index - 1] : null;
    const pairEligible = paired20 && unitLength(unit) === 20 && unitLength(previous) === 20 && index % 2 === 1;
    return {
      sequence: index + 1,
      unitId: identifier(unit, `UNIDADE-${index + 1}`),
      unit,
      destinationId: identifier(destination, `DESTINO-${index + 1}`),
      destination,
      pairGroup: pairEligible ? `PAR-${Math.ceil((index + 1) / 2)}` : null,
      state: 'PROPOSTA'
    };
  });

  if (paired20 && normalizedUnits.some((unit) => unitLength(unit) !== 20)) {
    errors.push('Paired 20 somente pode ser aplicado a unidades de 20 pés.');
  }

  const destinationIds = new Set();
  moves.forEach((move) => {
    if (destinationIds.has(move.destinationId)) errors.push(`Destino duplicado: ${move.destinationId}.`);
    destinationIds.add(move.destinationId);
  });

  return {
    valid: errors.length === 0 && moves.length === normalizedUnits.length,
    errors: [...new Set(errors)],
    moves,
    summary: {
      units: normalizedUnits.length,
      destinations: orderedDestinations.length,
      moves: moves.length,
      pattern,
      direction,
      paired20,
      alternateBays
    }
  };
}

function operationDurationMinutes(operation, productivity) {
  const configured = numberValue(operation?.duracaoMinutos ?? operation?.durationMinutes, 0);
  if (configured > 0) return configured;
  const rate = Math.max(1, numberValue(operation?.produtividade ?? productivity, 25));
  return 60 / rate;
}

export function buildQuayQueueModel(operations = [], { productivity = 25, startAt = new Date().toISOString() } = {}) {
  const queues = new Map();
  array(operations).forEach((operation, index) => {
    const crane = text(operation?.guindasteId ?? operation?.craneId ?? operation?.equipamento) || 'SEM_GUINDASTE';
    const bay = text(operation?.bay ?? operation?.bayId) || 'SEM_BAY';
    const queueId = text(operation?.workQueueId ?? operation?.filaId) || `${crane}:${bay}`;
    const current = queues.get(queueId) ?? {
      id: queueId,
      crane,
      bay,
      state: text(operation?.estado ?? operation?.status) || 'PLANEJADA',
      blockedReason: text(operation?.motivoBloqueio),
      operations: []
    };
    current.operations.push({
      ...operation,
      id: identifier(operation, `${queueId}:${index + 1}`),
      sequence: numberValue(operation?.sequencia ?? operation?.sequence, current.operations.length + 1),
      durationMinutes: operationDurationMinutes(operation, productivity)
    });
    queues.set(queueId, current);
  });

  const startTimestamp = Date.parse(startAt);
  return [...queues.values()].map((queue) => {
    queue.operations.sort((left, right) => left.sequence - right.sequence);
    const totalMinutes = queue.operations.reduce((sum, operation) => sum + operation.durationMinutes, 0);
    return {
      ...queue,
      planned: queue.operations.length,
      remaining: queue.operations.filter((operation) => !['CONCLUIDO', 'CONCLUIDA'].includes(text(operation?.estado ?? operation?.status).toUpperCase())).length,
      totalMinutes,
      projectedEnd: Number.isFinite(startTimestamp) ? new Date(startTimestamp + totalMinutes * 60000).toISOString() : null
    };
  });
}

export function transferQuayQueue(queueModel = [], queueId, targetCrane, splitAt = 0) {
  const queues = array(queueModel).map((queue) => ({ ...queue, operations: array(queue.operations).map((item) => ({ ...item })) }));
  const source = queues.find((queue) => queue.id === queueId);
  if (!source) return { valid: false, error: 'Fila de origem não encontrada.', queues };
  if (!text(targetCrane)) return { valid: false, error: 'Guindaste de destino não informado.', queues };
  const cut = Math.max(0, Math.min(numberValue(splitAt, 0), source.operations.length));
  const movedOperations = cut > 0 ? source.operations.splice(cut) : source.operations.splice(0);
  if (movedOperations.length === 0) return { valid: false, error: 'Nenhuma operação foi selecionada para transferência.', queues };
  const targetId = `${targetCrane}:${source.bay}:${Date.now()}`;
  const target = {
    ...source,
    id: targetId,
    crane: targetCrane,
    operations: movedOperations.map((operation, index) => ({ ...operation, sequence: index + 1 }))
  };
  source.operations = source.operations.map((operation, index) => ({ ...operation, sequence: index + 1 }));
  return { valid: true, queues: [...queues.filter((queue) => queue.operations.length > 0), target], moved: movedOperations.length };
}

export function normalizeCheMap(telemetry = [], { now = Date.now(), staleAfterSeconds = 90 } = {}) {
  return array(telemetry).map((item, index) => {
    const timestamp = Date.parse(item?.instante ?? item?.timestamp ?? item?.atualizadoEm ?? item?.updatedAt);
    const ageSeconds = Number.isFinite(timestamp) ? Math.max(0, Math.floor((now - timestamp) / 1000)) : Number.POSITIVE_INFINITY;
    const x = numberValue(item?.x ?? item?.longitudeOperacional ?? item?.coluna, index * 12 + 8);
    const y = numberValue(item?.y ?? item?.latitudeOperacional ?? item?.linha, index * 9 + 8);
    return {
      ...item,
      id: identifier(item, `CHE-${index + 1}`),
      label: text(item?.equipamento ?? item?.identificador ?? item?.codigo) || `CHE ${index + 1}`,
      x,
      y,
      heading: numberValue(item?.heading ?? item?.rumo, 0),
      stale: ageSeconds > staleAfterSeconds,
      ageSeconds,
      connected: item?.conectado !== false && item?.connected !== false,
      state: text(item?.statusOperacional ?? item?.status) || 'DESCONHECIDO',
      carriedUnit: text(item?.codigoConteiner ?? item?.containerCode),
      currentJob: text(item?.workInstructionId ?? item?.ordemAtualId),
      nextJobs: array(item?.proximosJobs ?? item?.nextJobs),
      trail: array(item?.trilha ?? item?.trail),
      route: array(item?.rota ?? item?.route),
      range: numberValue(item?.alcanceMetros ?? item?.rangeMeters, 30)
    };
  });
}

export function summarizeEcConsole(workQueues = [], telemetry = []) {
  const queues = array(workQueues);
  const che = normalizeCheMap(telemetry);
  const jobs = queues.flatMap((queue) => array(queue?.jobs ?? queue?.operations ?? queue?.ordens));
  const completed = jobs.filter((job) => ['CONCLUIDO', 'CONCLUIDA'].includes(text(job?.estado ?? job?.status).toUpperCase())).length;
  const blocked = jobs.filter((job) => text(job?.motivoBloqueio) || ['BLOQUEADO', 'FALHA'].includes(text(job?.estado ?? job?.status).toUpperCase())).length;
  const activeChe = che.filter((item) => item.connected && !item.stale).length;
  return {
    powCount: new Set(queues.map((queue) => text(queue?.powId ?? queue?.pow)).filter(Boolean)).size,
    poolCount: new Set(queues.map((queue) => text(queue?.poolId ?? queue?.pool)).filter(Boolean)).size,
    queueCount: queues.length,
    jobCount: jobs.length,
    completed,
    blocked,
    activeChe,
    staleChe: che.filter((item) => item.stale).length,
    pushRate: jobs.length > 0 ? Math.round((completed / jobs.length) * 100) : 0
  };
}

export function applyOperationalFilter(elements = [], filters = {}) {
  const query = text(filters.query).toLowerCase();
  const state = text(filters.state).toUpperCase();
  const domain = text(filters.domain).toLowerCase();
  const equipment = text(filters.equipment).toLowerCase();
  return array(elements).map((element) => {
    const searchable = JSON.stringify(element).toLowerCase();
    const matches = (!query || searchable.includes(query))
      && (!state || text(element?.state ?? element?.status).toUpperCase() === state)
      && (!domain || text(element?.domain).toLowerCase() === domain)
      && (!equipment || text(element?.equipment ?? element?.che).toLowerCase().includes(equipment));
    return { ...element, highlighted: matches, dimmed: !matches };
  });
}

export function createWorkspacePayload(value = {}) {
  return {
    id: text(value.id) || null,
    name: text(value.name) || 'Workspace operacional',
    scope: text(value.scope) || 'INDIVIDUAL',
    role: text(value.role) || null,
    version: Math.max(1, numberValue(value.version, 1)),
    palette: { ...(value.palette ?? {}) },
    filters: { ...(value.filters ?? {}) },
    panels: array(value.panels).map((panel, index) => ({
      id: identifier(panel, `PAINEL-${index + 1}`),
      x: numberValue(panel?.x),
      y: numberValue(panel?.y),
      width: Math.max(1, numberValue(panel?.width, 4)),
      height: Math.max(1, numberValue(panel?.height, 3)),
      visible: panel?.visible !== false
    })),
    attributes: array(value.attributes).map(text).filter(Boolean),
    updatedAt: new Date().toISOString()
  };
}

export function validateWorkspacePayload(value) {
  const payload = createWorkspacePayload(value);
  const errors = [];
  if (!payload.name) errors.push('Nome do workspace é obrigatório.');
  if (!['INDIVIDUAL', 'EQUIPE', 'PAPEL', 'PADRAO'].includes(payload.scope)) errors.push('Escopo de workspace inválido.');
  if (payload.scope === 'PAPEL' && !payload.role) errors.push('O papel deve ser informado para workspace por papel.');
  return { valid: errors.length === 0, errors, payload };
}

export function validateGeometryDraft(geometry = {}) {
  const elements = array(geometry.elements);
  const errors = [];
  const ids = new Set();
  elements.forEach((element, index) => {
    const id = identifier(element, '');
    if (!id) errors.push(`Elemento ${index + 1} sem identificador.`);
    if (ids.has(id)) errors.push(`Identificador duplicado: ${id}.`);
    ids.add(id);
    if (!text(element?.type)) errors.push(`Elemento ${id || index + 1} sem tipo.`);
    if (numberValue(element?.width, 1) <= 0 || numberValue(element?.height, 1) <= 0) errors.push(`Elemento ${id || index + 1} possui dimensão inválida.`);
  });
  array(geometry.edges).forEach((edge) => {
    const from = text(edge?.from);
    const to = text(edge?.to);
    if (!ids.has(from) || !ids.has(to)) errors.push(`Ligação ${from} → ${to} referencia elemento inexistente.`);
  });
  return { valid: errors.length === 0, errors: [...new Set(errors)] };
}

export function shortestOperationalRoute(nodes = [], edges = [], startId, endId, blockedEdgeIds = []) {
  const nodeIds = new Set(array(nodes).map((node, index) => identifier(node, `NODE-${index + 1}`)));
  if (!nodeIds.has(startId) || !nodeIds.has(endId)) return { reachable: false, reason: 'Origem ou destino não existe no grafo.' };
  const blocked = new Set(array(blockedEdgeIds).map(text));
  const adjacency = new Map([...nodeIds].map((id) => [id, []]));
  array(edges).forEach((edge, index) => {
    const id = identifier(edge, `EDGE-${index + 1}`);
    if (blocked.has(id)) return;
    const from = text(edge?.from);
    const to = text(edge?.to);
    if (!adjacency.has(from) || !adjacency.has(to)) return;
    const congestion = Math.max(0, numberValue(edge?.congestion ?? edge?.congestionamento, 0));
    const weight = Math.max(0.001, numberValue(edge?.distance ?? edge?.distancia ?? edge?.weight, 1)) * (1 + congestion);
    adjacency.get(from).push({ id, to, weight });
    if (edge?.oneWay !== true && edge?.sentidoUnico !== true) adjacency.get(to).push({ id, to: from, weight });
  });

  const distance = new Map([...nodeIds].map((id) => [id, Number.POSITIVE_INFINITY]));
  const previous = new Map();
  const unvisited = new Set(nodeIds);
  distance.set(startId, 0);
  while (unvisited.size > 0) {
    let current = null;
    unvisited.forEach((id) => {
      if (current === null || distance.get(id) < distance.get(current)) current = id;
    });
    if (current === null || !Number.isFinite(distance.get(current))) break;
    unvisited.delete(current);
    if (current === endId) break;
    adjacency.get(current).forEach((edge) => {
      const candidate = distance.get(current) + edge.weight;
      if (candidate < distance.get(edge.to)) {
        distance.set(edge.to, candidate);
        previous.set(edge.to, { node: current, edge: edge.id });
      }
    });
  }
  if (!Number.isFinite(distance.get(endId))) return { reachable: false, reason: 'Não existe rota disponível.' };
  const path = [];
  const edgeIds = [];
  let cursor = endId;
  while (cursor) {
    path.unshift(cursor);
    const step = previous.get(cursor);
    if (!step) break;
    edgeIds.unshift(step.edge);
    cursor = step.node;
  }
  return { reachable: true, path, edgeIds, cost: distance.get(endId), etaMinutes: Math.ceil(distance.get(endId)) };
}

export function planRailYardAssignments(units = [], wagons = []) {
  const available = array(wagons).map((wagon, index) => ({
    ...wagon,
    id: identifier(wagon, `VAGAO-${index + 1}`),
    remaining: Math.max(0, numberValue(wagon?.capacidade ?? wagon?.capacity, 1) - array(wagon?.units ?? wagon?.unidades).length)
  }));
  const conflicts = [];
  const assignments = [];
  array(units).forEach((unit, index) => {
    const compatible = available.find((wagon) => wagon.remaining > 0
      && (!text(wagon?.tipoPermitido) || text(wagon.tipoPermitido) === text(unit?.tipo ?? unit?.type))
      && (!numberValue(wagon?.pesoMaximo ?? wagon?.maxWeight) || numberValue(unit?.peso ?? unit?.weight) <= numberValue(wagon?.pesoMaximo ?? wagon?.maxWeight)));
    if (!compatible) {
      conflicts.push(`Sem vagão compatível para ${identifier(unit, `UNIDADE-${index + 1}`)}.`);
      return;
    }
    compatible.remaining -= 1;
    assignments.push({
      sequence: assignments.length + 1,
      unitId: identifier(unit, `UNIDADE-${index + 1}`),
      wagonId: compatible.id,
      line: text(compatible?.linha ?? compatible?.line),
      position: text(compatible?.posicao ?? compatible?.position),
      equipment: text(unit?.equipamento ?? unit?.che),
      workInstruction: text(unit?.workInstructionId ?? unit?.ordemId),
      state: conflicts.length > 0 ? 'BLOQUEADO' : 'PROPOSTA'
    });
  });
  return { valid: conflicts.length === 0 && assignments.length === array(units).length, assignments, conflicts };
}
