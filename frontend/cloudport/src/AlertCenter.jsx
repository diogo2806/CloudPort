import { useCallback, useEffect, useMemo, useState } from 'react';
import { alertCenterApi } from './alertCenterApi.js';
import {
  moduleForAlert,
  normalizeAlertPage,
  replaceAlert,
  routeForAlert,
  severityLabel,
  summarizeAlerts
} from './alertCenter.js';
import { DataTable, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from './components.jsx';
import { formatError } from './api.js';
import './alert-center.css';

const SEVERITIES = ['', 'critica', 'alta', 'media', 'baixa'];

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}

function SeverityBadge({ value }) {
  const severity = String(value || 'baixa').toLowerCase();
  return <span className={`alert-severity alert-severity-${severity}`}>{severityLabel(severity)}</span>;
}

function AlertCard({ alert, busy, onAcknowledge, onResolve, onNavigate }) {
  return <article className={`alert-center-card severity-${alert.severidade} ${alert.dataReconhecimento ? 'acknowledged' : 'unacknowledged'}`}>
    <header><div><SeverityBadge value={alert.severidade} /><span>{moduleForAlert(alert)}</span></div><time>{formatDateTime(alert.dataGerada)}</time></header>
    <strong>{alert.descricao}</strong>
    <p>{alert.acaoSugerida || 'Sem ação sugerida registrada.'}</p>
    <footer>
      <button type="button" className="secondary small" onClick={() => onNavigate(alert)}>Abrir módulo</button>
      {!alert.dataReconhecimento && alert.status === 'ativo' && <button type="button" className="secondary small" disabled={busy} onClick={() => onAcknowledge(alert)}>{busy ? 'Atualizando...' : 'Reconhecer'}</button>}
      {alert.status === 'ativo' && <button type="button" className="small" disabled={busy} onClick={() => onResolve(alert)}>{busy ? 'Atualizando...' : 'Resolver'}</button>}
      {alert.dataReconhecimento && <span className="alert-center-ack">Reconhecido por {alert.reconhecidoPor || 'operador'}</span>}
    </footer>
  </article>;
}

function useAlertCenter({ status = 'ativo', severity = '', session, polling = false } = {}) {
  const [alerts, setAlerts] = useState([]);
  const [summary, setSummary] = useState(() => summarizeAlerts([]));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [updatedAt, setUpdatedAt] = useState(null);

  const load = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    setError('');
    try {
      const [page, backendSummary] = await Promise.all([
        alertCenterApi.listar({ status, severidade: severity ? [severity] : [], size: 100 }),
        status === 'ativo' ? alertCenterApi.resumo().catch(() => null) : Promise.resolve(null)
      ]);
      const normalized = normalizeAlertPage(page);
      setAlerts(normalized.alerts);
      setSummary(summarizeAlerts(normalized.alerts, backendSummary));
      setUpdatedAt(new Date());
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar os alertas operacionais.'));
    } finally {
      if (!silent) setLoading(false);
    }
  }, [status, severity]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    if (!polling || status !== 'ativo') return undefined;
    const timer = window.setInterval(() => load({ silent: true }), 30000);
    return () => window.clearInterval(timer);
  }, [polling, status, load]);

  const act = useCallback(async (alert, operation) => {
    setBusyId(alert.id);
    setError('');
    try {
      const updated = operation === 'acknowledge'
        ? await alertCenterApi.reconhecer(alert.id, session?.nome)
        : await alertCenterApi.resolver(alert.id, session?.nome);
      if (operation === 'resolve' && status === 'ativo') {
        setAlerts((current) => current.filter((item) => String(item.id) !== String(alert.id)));
      } else {
        setAlerts((current) => replaceAlert(current, updated));
      }
      await load({ silent: true });
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível atualizar o alerta.'));
    } finally {
      setBusyId(null);
    }
  }, [load, session?.nome, status]);

  return { alerts, summary, loading, error, busyId, updatedAt, load, acknowledge: (alert) => act(alert, 'acknowledge'), resolve: (alert) => act(alert, 'resolve') };
}

export function GlobalAlertCenter({ navigate, session }) {
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState('ativo');
  const [severity, setSeverity] = useState('');
  const center = useAlertCenter({ status, severity, session, polling: true });
  const badge = center.summary.naoReconhecidos || center.summary.totalAtivos;

  function navigateToAlert(alert) {
    setOpen(false);
    navigate(routeForAlert(alert));
  }

  return <div className="global-alert-center">
    <button type="button" className={`alert-center-trigger ${center.summary.criticos ? 'has-critical' : ''}`} aria-label={`Central de alertas. ${badge} alerta(s) pendente(s).`} aria-expanded={open} onClick={() => setOpen((value) => !value)}>
      <span aria-hidden="true">!</span><strong>Alertas</strong>{badge > 0 && <b>{badge > 99 ? '99+' : badge}</b>}
    </button>
    {open && <>
      <button type="button" className="alert-center-backdrop" aria-label="Fechar central de alertas" onClick={() => setOpen(false)} />
      <aside className="alert-center-drawer" role="dialog" aria-modal="true" aria-labelledby="global-alert-center-title">
        <header><div><span>Operação em tempo real</span><h2 id="global-alert-center-title">Central de alertas</h2></div><button type="button" className="icon-button" aria-label="Fechar central" onClick={() => setOpen(false)}>×</button></header>
        <div className="alert-center-summary"><strong>{center.summary.totalAtivos}</strong><span>ativos</span><strong>{center.summary.criticos}</strong><span>críticos</span><strong>{center.summary.naoReconhecidos}</strong><span>não reconhecidos</span></div>
        <div className="alert-center-controls">
          <div className="alert-center-tabs"><button type="button" className={status === 'ativo' ? 'active' : 'secondary'} onClick={() => setStatus('ativo')}>Ativos</button><button type="button" className={status === 'resolvido' ? 'active' : 'secondary'} onClick={() => setStatus('resolvido')}>Resolvidos</button></div>
          <label><span>Severidade</span><select value={severity} onChange={(event) => setSeverity(event.target.value)}>{SEVERITIES.map((value) => <option key={value || 'all'} value={value}>{value ? severityLabel(value) : 'Todas'}</option>)}</select></label>
        </div>
        <Message type="error">{center.error}</Message>
        <div className="alert-center-list">{center.loading ? <Loading label="Carregando alertas..." /> : center.alerts.length ? center.alerts.map((alert) => <AlertCard key={alert.id} alert={alert} busy={center.busyId === alert.id} onAcknowledge={center.acknowledge} onResolve={center.resolve} onNavigate={navigateToAlert} />) : <div className="empty-state"><strong>Nenhum alerta encontrado</strong><span>A operação não possui alertas para os filtros selecionados.</span></div>}</div>
        <footer><span>{center.updatedAt ? `Atualizado em ${formatDateTime(center.updatedAt)}` : 'Aguardando atualização'}</span><div><button type="button" className="secondary small" onClick={() => center.load()}>Atualizar</button><button type="button" className="small" onClick={() => { setOpen(false); navigate('/home/alertas'); }}>Abrir central completa</button></div></footer>
      </aside>
    </>}
  </div>;
}

export function AlertCenterPage({ navigate, session }) {
  const [status, setStatus] = useState('ativo');
  const [severity, setSeverity] = useState('');
  const center = useAlertCenter({ status, severity, session, polling: true });
  const columns = useMemo(() => [
    { key: 'severidade', label: 'Severidade', render: (alert) => <SeverityBadge value={alert.severidade} /> },
    { key: 'tipo', label: 'Tipo' },
    { key: 'modulo', label: 'Módulo', value: moduleForAlert },
    { key: 'entidadeId', label: 'Entidade' },
    { key: 'descricao', label: 'Descrição' },
    { key: 'dataGerada', label: 'Gerado em', render: (alert) => formatDateTime(alert.dataGerada), sortValue: (alert) => alert.dataGerada },
    { key: 'status', label: 'Status', render: (alert) => <StatusBadge value={alert.status} /> },
    { key: 'actions', label: 'Ações', sortable: false, searchable: false, exportable: false, render: (alert) => <div className="actions compact"><button type="button" className="secondary small" onClick={() => navigate(routeForAlert(alert))}>Abrir módulo</button>{!alert.dataReconhecimento && alert.status === 'ativo' && <button type="button" className="secondary small" disabled={center.busyId === alert.id} onClick={() => center.acknowledge(alert)}>Reconhecer</button>}{alert.status === 'ativo' && <button type="button" className="small" disabled={center.busyId === alert.id} onClick={() => center.resolve(alert)}>Resolver</button>}</div> }
  ], [center.busyId, center.acknowledge, center.resolve, navigate]);

  return <>
    <PageHeader eyebrow="Visibilidade operacional" title="Central global de alertas" description="Consolide ocorrências de Gate, ferrovia, pátio, navio e embarque, reconheça responsabilidades e acompanhe a resolução." actions={<button type="button" className="secondary" onClick={() => center.load()}>Atualizar</button>} />
    <Message type="error">{center.error}</Message>
    <div className="metrics-grid alert-center-metrics"><MetricCard label="Alertas ativos" value={center.summary.totalAtivos} detail={`${center.summary.naoReconhecidos} não reconhecido(s)`} /><MetricCard label="Críticos" value={center.summary.criticos} detail="Ação imediata" /><MetricCard label="Alta severidade" value={center.summary.altos} detail="Prioridade operacional" /><MetricCard label="Média e baixa" value={center.summary.medios + center.summary.baixos} detail="Monitoramento" /></div>
    <Section title="Filtros operacionais" description="A lista é atualizada automaticamente a cada 30 segundos."><div className="inline-form"><label className="field"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="ativo">Ativos</option><option value="resolvido">Resolvidos</option></select></label><label className="field"><span>Severidade</span><select value={severity} onChange={(event) => setSeverity(event.target.value)}>{SEVERITIES.map((value) => <option key={value || 'all'} value={value}>{value ? severityLabel(value) : 'Todas'}</option>)}</select></label></div></Section>
    <Section title={status === 'ativo' ? 'Alertas em aberto' : 'Histórico de alertas'} description={center.updatedAt ? `Última atualização: ${formatDateTime(center.updatedAt)}` : undefined}>{center.loading ? <Loading /> : <DataTable gridId="global-alert-center" rows={center.alerts} columns={columns} rowKey="id" inspectable selectable emptyTitle="Nenhum alerta encontrado" />}</Section>
  </>;
}
