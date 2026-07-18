const COMPLETE_DOCUMENT_STATUSES = new Set(['VALIDADO', 'APROVADO', 'CONCLUIDO', 'CONCLUÍDO']);
const ERROR_DOCUMENT_STATUSES = new Set(['INVALIDO', 'INVÁLIDO', 'REJEITADO', 'ERRO', 'FALHA']);
const FINISHED_STATUSES = new Set(['CONCLUIDO', 'CONCLUÍDO', 'FINALIZADO', 'LIBERADO', 'SAIU']);

function arrayOf(value) {
  return Array.isArray(value) ? value : [];
}

function normalized(value) {
  return String(value ?? '').trim().toUpperCase();
}

function normalizedPlate(value) {
  return normalized(value).replace(/[^A-Z0-9]/g, '');
}

export function gateVehicleKey(vehicle) {
  const id = vehicle?.veiculoId ?? vehicle?.id;
  if (id !== undefined && id !== null && id !== '') return `id:${id}`;
  const plate = normalizedPlate(vehicle?.placaVeiculo ?? vehicle?.placa);
  return plate ? `plate:${plate}` : null;
}

function appointmentVehicle(appointment) {
  return {
    id: appointment?.veiculoId ?? null,
    veiculoId: appointment?.veiculoId ?? null,
    placa: appointment?.placaVeiculo ?? 'Sem placa',
    placaVeiculo: appointment?.placaVeiculo ?? 'Sem placa',
    motorista: appointment?.motoristaNome ?? '—',
    transportadora: appointment?.transportadoraNome ?? '—',
    status: appointment?.status,
    statusDescricao: appointment?.statusDescricao ?? appointment?.status,
    tempoFilaMinutos: 0,
    fluxoOperacional: 'AGENDADO',
    filaOperacional: 'Pré-gate',
    agendamento: appointment,
    documentos: arrayOf(appointment?.documentos),
    gatePass: appointment?.gatePass
  };
}

function appointmentIndex(appointments) {
  const index = new Map();
  arrayOf(appointments).forEach((appointment) => {
    const key = gateVehicleKey(appointment);
    if (key) index.set(key, appointment);
    const plate = normalizedPlate(appointment?.placaVeiculo);
    if (plate) index.set(`plate:${plate}`, appointment);
  });
  return index;
}

function enrichVehicle(vehicle, appointmentsByVehicle, lane) {
  const key = gateVehicleKey(vehicle);
  const plateKey = normalizedPlate(vehicle?.placa ?? vehicle?.placaVeiculo);
  const appointment = appointmentsByVehicle.get(key)
    ?? (plateKey ? appointmentsByVehicle.get(`plate:${plateKey}`) : null)
    ?? null;
  return {
    ...appointmentVehicle(appointment),
    ...vehicle,
    fluxoOperacional: lane.flow,
    filaOperacional: lane.name,
    agendamento: appointment,
    documentos: arrayOf(appointment?.documentos ?? vehicle?.documentos),
    gatePass: appointment?.gatePass ?? vehicle?.gatePass
  };
}

function queueLanes(queues, flow, stage, appointmentsByVehicle) {
  return arrayOf(queues).map((queue, index) => {
    const lane = {
      id: queue?.id ?? `${flow.toLowerCase()}-${index + 1}`,
      name: queue?.nome ?? `${stage} ${index + 1}`,
      flow,
      stage,
      averageMinutes: Number(queue?.tempoMedioMinutos ?? queue?.tempoMedioFilaMinutos ?? 0),
      declaredCount: Number(queue?.quantidade ?? 0),
      vehicles: []
    };
    lane.vehicles = arrayOf(queue?.veiculos).map((vehicle) => enrichVehicle(vehicle, appointmentsByVehicle, lane));
    return lane;
  });
}

export function buildGateLanes(panel, appointments = []) {
  const appointmentsByVehicle = appointmentIndex(appointments);
  const serviceLane = {
    id: 'atendimento',
    name: 'Atendimento em pista',
    flow: 'ATENDIMENTO',
    stage: 'PROCESSAMENTO',
    averageMinutes: 0,
    declaredCount: arrayOf(panel?.veiculosAtendimento).length,
    vehicles: []
  };
  serviceLane.vehicles = arrayOf(panel?.veiculosAtendimento)
    .map((vehicle) => enrichVehicle(vehicle, appointmentsByVehicle, serviceLane));

  const lanes = [
    ...queueLanes(panel?.filasEntrada, 'ENTRADA', 'FILA DE ENTRADA', appointmentsByVehicle),
    serviceLane,
    ...queueLanes(panel?.filasSaida, 'SAÍDA', 'FILA DE SAÍDA', appointmentsByVehicle)
  ];

  const serviceKeys = new Set(serviceLane.vehicles.map(gateVehicleKey).filter(Boolean));
  lanes.forEach((lane) => {
    if (lane.id !== serviceLane.id) {
      lane.vehicles = lane.vehicles.filter((vehicle) => !serviceKeys.has(gateVehicleKey(vehicle)));
    }
  });

  const knownKeys = new Set(lanes.flatMap((lane) => lane.vehicles.map(gateVehicleKey)).filter(Boolean));
  const scheduled = arrayOf(appointments)
    .filter((appointment) => !knownKeys.has(gateVehicleKey(appointment)))
    .filter((appointment) => !FINISHED_STATUSES.has(normalized(appointment?.status)))
    .map(appointmentVehicle);

  if (scheduled.length) {
    lanes.unshift({
      id: 'pre-gate',
      name: 'Pré-gate / agendados',
      flow: 'AGENDADO',
      stage: 'AGENDAMENTO',
      averageMinutes: 0,
      declaredCount: scheduled.length,
      vehicles: scheduled
    });
  }

  return lanes.filter((lane) => lane.vehicles.length || lane.id === 'atendimento');
}

export function flattenGateVehicles(lanes) {
  return arrayOf(lanes).flatMap((lane) => arrayOf(lane?.vehicles));
}

export function classifyGateSla(minutes, warningMinutes = 20, criticalMinutes = 30) {
  const value = Math.max(0, Number(minutes) || 0);
  if (value >= criticalMinutes) return { level: 'critical', label: `${value} min`, minutes: value };
  if (value >= warningMinutes) return { level: 'warning', label: `${value} min`, minutes: value };
  return { level: 'ok', label: `${value} min`, minutes: value };
}

export function getGateJourney(vehicle) {
  const appointment = vehicle?.agendamento ?? {};
  const status = normalized(vehicle?.status ?? appointment?.status);
  const documents = arrayOf(vehicle?.documentos ?? appointment?.documentos);
  const documentStatuses = documents.map((document) => normalized(document?.statusValidacao ?? document?.status));
  const documentError = documentStatuses.some((value) => ERROR_DOCUMENT_STATUSES.has(value));
  const documentComplete = documents.length > 0 && documentStatuses.every((value) => COMPLETE_DOCUMENT_STATUSES.has(value));
  const exceptions = arrayOf(vehicle?.excecoes);
  const hasGatePass = Boolean(vehicle?.gatePass ?? appointment?.gatePass ?? vehicle?.podeImprimirComprovante);
  const inService = ['EM_ATENDIMENTO', 'EM_EXECUCAO'].includes(status);
  const executionStarted = status === 'EM_EXECUCAO' || FINISHED_STATUSES.has(status);

  return [
    { key: 'appointment', label: 'Agendamento', state: 'complete', detail: appointment?.codigo ?? vehicle?.codigoAgendamento ?? 'Confirmado' },
    {
      key: 'ocr',
      label: 'OCR e documentos',
      state: documentError ? 'warning' : documentComplete ? 'complete' : documents.length ? 'active' : 'pending',
      detail: documentError ? 'Documento com erro' : documentComplete ? `${documents.length} validado(s)` : documents.length ? `${documents.length} em validação` : 'Sem documento'
    },
    {
      key: 'scale',
      label: 'Balança',
      state: executionStarted ? 'complete' : inService ? 'active' : 'pending',
      detail: executionStarted ? 'Pesagem concluída' : inService ? 'Em processamento' : 'Aguardando'
    },
    {
      key: 'inspection',
      label: 'Inspeção',
      state: exceptions.length ? 'warning' : executionStarted ? 'complete' : inService ? 'active' : 'pending',
      detail: exceptions.length ? `${exceptions.length} ocorrência(s)` : executionStarted ? 'Concluída' : inService ? 'Em andamento' : 'Aguardando'
    },
    {
      key: 'release',
      label: 'Liberação',
      state: hasGatePass || FINISHED_STATUSES.has(status) ? 'complete' : executionStarted ? 'active' : 'pending',
      detail: hasGatePass ? 'EIR disponível' : FINISHED_STATUSES.has(status) ? 'Veículo liberado' : executionStarted ? 'Aguardando autorização' : 'Pendente'
    }
  ];
}

export function isGateProblemVehicle(vehicle, criticalMinutes = 30) {
  const sla = classifyGateSla(vehicle?.tempoFilaMinutos, 20, criticalMinutes);
  const exceptions = arrayOf(vehicle?.excecoes);
  const documents = arrayOf(vehicle?.documentos ?? vehicle?.agendamento?.documentos);
  const documentError = documents.some((document) => ERROR_DOCUMENT_STATUSES.has(normalized(document?.statusValidacao ?? document?.status)));
  return sla.level === 'critical' || exceptions.length > 0 || documentError;
}

export function groupGateWindows(windows, appointments) {
  const appointmentsList = arrayOf(appointments);
  const grouped = new Map();

  arrayOf(windows).forEach((windowItem) => {
    const date = String(windowItem?.data ?? 'Sem data');
    const occupied = appointmentsList.filter((appointment) => {
      if (appointment?.janelaAtendimentoId !== undefined && windowItem?.id !== undefined) {
        return String(appointment.janelaAtendimentoId) === String(windowItem.id);
      }
      return String(appointment?.dataJanela ?? '') === date
        && String(appointment?.horaInicioJanela ?? '') === String(windowItem?.horaInicio ?? '');
    }).length;
    const capacity = Math.max(0, Number(windowItem?.capacidade) || 0);
    const percentage = capacity ? Math.min(100, Math.round((occupied / capacity) * 100)) : 0;
    const normalizedWindow = { ...windowItem, occupied, capacity, percentage };
    if (!grouped.has(date)) grouped.set(date, []);
    grouped.get(date).push(normalizedWindow);
  });

  return Array.from(grouped, ([date, items]) => ({
    date,
    windows: items.sort((left, right) => String(left?.horaInicio ?? '').localeCompare(String(right?.horaInicio ?? '')))
  })).sort((left, right) => left.date.localeCompare(right.date));
}
