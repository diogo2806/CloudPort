import { EmptyState, StatusBadge } from '../../components.jsx';
import { sanitizeText } from '../../api.js';

function temperature(value) {
  return value === undefined || value === null ? '—' : `${Number(value).toFixed(1)} °C`;
}

export function YardReeferPanel({ telemetry }) {
  const rows = telemetry ?? [];
  const critical = rows.filter((item) => item.statusAlarme === 'CRITICO').length;
  const warnings = rows.filter((item) => item.statusAlarme === 'ATENCAO').length;

  if (!rows.length) {
    return <EmptyState title="Nenhuma telemetria reefer recebida" description="As leituras dos sensores aparecerão aqui após o envio para /yard/patio/reefers/telemetria/{conteinerId}." />;
  }

  return <div className="yard-reefer-panel">
    <div className="yard-operational-metrics">
      <span><strong>{rows.length}</strong> reefers monitorados</span>
      <span><strong>{critical}</strong> alarmes críticos</span>
      <span><strong>{warnings}</strong> leituras desatualizadas</span>
    </div>
    <div className="yard-reefer-grid">{rows.map((item) => <article key={item.conteinerId} className={`yard-reefer-card ${String(item.statusAlarme).toLowerCase()}`}>
      <header><strong>{sanitizeText(item.codigoConteiner)}</strong><StatusBadge value={item.statusAlarme} /></header>
      <div><span>Atual</span><strong>{temperature(item.temperaturaAtualCelsius)}</strong></div>
      <small>Faixa: {temperature(item.temperaturaMinimaCelsius)} a {temperature(item.temperaturaMaximaCelsius)}</small>
      <small>{item.bloco ?? 'Sem bloco'} · L{item.linha ?? '—'}/C{item.coluna ?? '—'}/{item.camadaOperacional ?? '—'}</small>
      <p>{sanitizeText(item.mensagemAlarme)}</p>
      <small>Última leitura: {item.registradoEm ? new Date(item.registradoEm).toLocaleString('pt-BR') : '—'}</small>
    </article>)}</div>
  </div>;
}
