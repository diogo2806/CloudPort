const STATUS_ORDER = {
  PENDENTE: 0,
  EM_EXECUCAO: 1,
  CONCLUIDA: 2
};

export function nextRailOrderStatus(status) {
  if (status === 'PENDENTE') return 'EM_EXECUCAO';
  if (status === 'EM_EXECUCAO') return 'CONCLUIDA';
  return null;
}

export function summarizeRailOrders(orders) {
  const safeOrders = Array.isArray(orders) ? orders : [];
  return safeOrders.reduce((summary, order) => {
    summary.total += 1;
    if (order?.statusMovimentacao === 'PENDENTE') summary.pendentes += 1;
    if (order?.statusMovimentacao === 'EM_EXECUCAO') summary.emExecucao += 1;
    if (order?.statusMovimentacao === 'CONCLUIDA') summary.concluidas += 1;
    return summary;
  }, { total: 0, pendentes: 0, emExecucao: 0, concluidas: 0 });
}

export function sortRailOrders(orders) {
  return [...(Array.isArray(orders) ? orders : [])].sort((left, right) => {
    const statusDifference = (STATUS_ORDER[left?.statusMovimentacao] ?? 99)
      - (STATUS_ORDER[right?.statusMovimentacao] ?? 99);
    if (statusDifference !== 0) return statusDifference;
    return String(left?.codigoConteiner ?? '').localeCompare(String(right?.codigoConteiner ?? ''), 'pt-BR');
  });
}
