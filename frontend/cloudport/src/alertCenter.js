import { sanitizeText } from './api.js';

const SEVERITY_ORDER = { critica: 0, alta: 1, media: 2, baixa: 3 };

export function normalizeSeverity(value) {
  const normalized = sanitizeText(value).toLowerCase();
  return Object.hasOwn(SEVERITY_ORDER, normalized) ? normalized : 'baixa';
}

export function severityLabel(value) {
  const normalized = normalizeSeverity(value);
  return normalized === 'critica' ? 'Crítica' : normalized === 'alta' ? 'Alta' : normalized === 'media' ? 'Média' : 'Baixa';
}

export function normalizeAlert(alert) {
  const source = alert ?? {};
  return {
    id: source.id,
    tipo: sanitizeText(source.tipo) || 'ALERTA_OPERACIONAL',
    severidade: normalizeSeverity(source.severidade),
    entidadeId: sanitizeText(source.entidadeId),
    descricao: sanitizeText(source.descricao) || 'Alerta operacional sem descrição.',
    acaoSugerida: sanitizeText(source.acaoSugerida),
    dataGerada: source.dataGerada ?? null,
    dataReconhecimento: source.dataReconhecimento ?? null,
    reconhecidoPor: sanitizeText(source.reconhecidoPor),
    dataResolucao: source.dataResolucao ?? null,
    resolvidoPor: sanitizeText(source.resolvidoPor),
    status: sanitizeText(source.status).toLowerCase() || 'ativo'
  };
}

export function normalizeAlertPage(payload) {
  const content = Array.isArray(payload) ? payload : Array.isArray(payload?.content) ? payload.content : [];
  const alerts = content.map(normalizeAlert).sort((left, right) => {
    const severity = SEVERITY_ORDER[left.severidade] - SEVERITY_ORDER[right.severidade];
    if (severity !== 0) return severity;
    return Date.parse(right.dataGerada ?? 0) - Date.parse(left.dataGerada ?? 0);
  });
  return {
    alerts,
    total: Number(payload?.totalElements ?? alerts.length) || 0,
    totalPages: Number(payload?.totalPages ?? 1) || 1,
    page: Number(payload?.number ?? 0) || 0
  };
}

export function summarizeAlerts(alerts, backendSummary) {
  if (backendSummary && typeof backendSummary === 'object') {
    return {
      totalAtivos: Number(backendSummary.totalAtivos) || 0,
      criticos: Number(backendSummary.criticos) || 0,
      altos: Number(backendSummary.altos) || 0,
      medios: Number(backendSummary.medios) || 0,
      baixos: Number(backendSummary.baixos) || 0,
      naoReconhecidos: Number(backendSummary.naoReconhecidos) || 0
    };
  }
  const active = (Array.isArray(alerts) ? alerts : []).map(normalizeAlert).filter((alert) => alert.status === 'ativo');
  return active.reduce((summary, alert) => {
    summary.totalAtivos += 1;
    summary[alert.severidade === 'critica' ? 'criticos' : alert.severidade === 'alta' ? 'altos' : alert.severidade === 'media' ? 'medios' : 'baixos'] += 1;
    if (!alert.dataReconhecimento) summary.naoReconhecidos += 1;
    return summary;
  }, { totalAtivos: 0, criticos: 0, altos: 0, medios: 0, baixos: 0, naoReconhecidos: 0 });
}

export function routeForAlert(alert) {
  const type = sanitizeText(alert?.tipo).toUpperCase();
  const entity = sanitizeText(alert?.entidadeId).toUpperCase();
  const value = `${type} ${entity}`;
  if (/GATE|PORTARIA|AGENDAMENTO/.test(value)) return '/home/gate/dashboard';
  if (/RAIL|FERROVIA|TREM|VAGAO|LOCOMOTIVA/.test(value)) return '/home/ferrovia/visitas';
  if (/YARD|PATIO|QUADRA|BLOCO|STACK/.test(value)) return '/home/patio/mapa';
  if (/NAVIO|VESSEL|BERCO|ATRAC/.test(value)) return '/home/navio/line-up';
  if (/ESTIVA|BAPLIE|EMBARQUE/.test(value)) return '/home/embarque/planejamento';
  return '/home/dashboard';
}

export function moduleForAlert(alert) {
  const route = routeForAlert(alert);
  if (route.includes('/gate/')) return 'Gate';
  if (route.includes('/ferrovia/')) return 'Ferrovia';
  if (route.includes('/patio/')) return 'Pátio';
  if (route.includes('/navio/')) return 'Navio';
  if (route.includes('/embarque/')) return 'Embarque';
  return 'Operação';
}

export function replaceAlert(alerts, updated) {
  const normalized = normalizeAlert(updated);
  return (Array.isArray(alerts) ? alerts : []).map((alert) => String(alert.id) === String(normalized.id) ? normalized : alert);
}
