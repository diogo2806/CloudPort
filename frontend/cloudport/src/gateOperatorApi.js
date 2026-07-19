import { request } from './api.js';

function vehicleKey(vehicle) {
  return vehicle?.id ?? vehicle?.placa ?? vehicle?.documento ?? null;
}

function addQueueVehicles(target, queues, flow) {
  if (!Array.isArray(queues)) return;
  queues.forEach((queue) => {
    if (!Array.isArray(queue?.veiculos)) return;
    queue.veiculos.forEach((vehicle) => {
      const key = vehicleKey(vehicle);
      if (key === null) return;
      target.set(key, {
        ...vehicle,
        filaOperacional: queue.nome ?? queue.id ?? '—',
        fluxoOperacional: flow
      });
    });
  });
}

export function selectGateOperatorVehicles(panel) {
  const vehicles = new Map();
  addQueueVehicles(vehicles, panel?.filasEntrada, 'ENTRADA');
  addQueueVehicles(vehicles, panel?.filasSaida, 'SAÍDA');

  if (Array.isArray(panel?.veiculosAtendimento)) {
    panel.veiculosAtendimento.forEach((vehicle) => {
      const key = vehicleKey(vehicle);
      if (key === null) return;
      vehicles.set(key, {
        ...vehicle,
        filaOperacional: 'Atendimento',
        fluxoOperacional: 'ATENDIMENTO'
      });
    });
  }

  return Array.from(vehicles.values());
}

function requiredVehicleId(vehicleId) {
  const value = Number(vehicleId);
  if (!Number.isInteger(value) || value <= 0) throw new Error('O veículo é obrigatório para gerar o EIR.');
  return value;
}

export const gateOperatorApi = {
  obterPainel: () => request('/gate/operador/painel'),
  listarEventos: () => request('/gate/operador/eventos'),
  obterComprovante: async (vehicleId) => request(`/gate/operador/veiculos/${requiredVehicleId(vehicleId)}/comprovante`)
};
