export function selectGateAppointments(payload) {
  return Array.isArray(payload?.agendamentos) ? payload.agendamentos : [];
}
