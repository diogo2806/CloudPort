import { useCallback, useEffect, useState } from 'react';
import { formatError, sanitizeText } from '../../api.js';
import { EmptyState, PageHeader } from '../../components.jsx';
import './YardPages.css';

export { buildStacks, FINAL_ORDER_STATUSES, orderDestinationKey, positionKey, stackClass } from './yardModel.js';

export const PAGE_SIZE = 20;
const YARD_ROUTES = [
  ['/home/patio/mapa', 'Mapa'],
  ['/home/patio/planejamento-recebimento', 'Recebimento'],
  ['/home/patio/yard-impact', 'Yard Impact'],
  ['/home/patio/posicoes', 'Posições'],
  ['/home/patio/lista-trabalho', 'Lista de trabalho'],
  ['/home/patio/instrucoes', 'Instruções'],
  ['/home/patio/movimentacoes', 'Movimentações'],
  ['/home/patio/recursos', 'Recursos'],
  ['/home/patio/dashboard-kpi', 'Indicadores'],
  ['/home/patio/automacao', 'Automação'],
  ['/home/patio/dispatch-equipamentos', 'Dispatch']
];

export function displayValue(value) {
  if (value === undefined || value === null || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'Sim' : 'Não';
  if (Array.isArray(value)) return value.length ? value.join(', ') : '—';
  if (typeof value === 'object') return JSON.stringify(value);
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) return date.toLocaleString('pt-BR');
  }
  return String(value);
}

export function normalized(value) {
  return sanitizeText(value).toLocaleLowerCase('pt-BR');
}

export function useRemote(loader, dependencies = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const reload = useCallback(async (options) => {
    const silent = options?.silent === true;
    if (!silent) setLoading(true);
    setError('');
    try {
      const response = await loader();
      setData(response);
      return response;
    } catch (reason) {
      setError(formatError(reason));
      return undefined;
    } finally {
      if (!silent) setLoading(false);
    }
  }, dependencies);
  useEffect(() => { reload(); }, [reload]);
  return { data, loading, error, setError, reload };
}

export function usePagination(rows, pageSize = PAGE_SIZE) {
  const [page, setPage] = useState(0);
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  useEffect(() => {
    if (page >= totalPages) setPage(totalPages - 1);
  }, [page, totalPages]);
  return {
    page,
    setPage,
    totalPages,
    rows: rows.slice(page * pageSize, page * pageSize + pageSize)
  };
}

export function Pagination({ page, totalPages, totalRows, onChange }) {
  if (totalRows <= PAGE_SIZE) return null;
  return <div className="pager" aria-label="Paginação">
    <button className="secondary small" disabled={page === 0} onClick={() => onChange(page - 1)}>Anterior</button>
    <span>Página {page + 1} de {totalPages} · {totalRows} registro(s)</span>
    <button className="secondary small" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Próxima</button>
  </div>;
}

function YardTabs({ activePath, navigate }) {
  return <nav className="yard-tabs" aria-label="Módulos do pátio">
    {YARD_ROUTES.map(([path, label]) => <button key={path} className={activePath === path ? 'active' : 'secondary'} onClick={() => navigate(path)}>{label}</button>)}
  </nav>;
}

export function FilterField({ label, children }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

export function DetailGrid({ value, fields }) {
  if (!value) return <EmptyState title="Selecione um registro" description="Os detalhes persistidos serão exibidos aqui." />;
  return <div className="detail-grid">
    {fields.map(([key, label, render]) => <div className="detail-row" key={key}><span>{label}</span><strong>{render ? render(value) : displayValue(value?.[key])}</strong></div>)}
  </div>;
}

export function CommandPanel({ command, busy, onCancel, onConfirm }) {
  const [reason, setReason] = useState('');
  useEffect(() => { setReason(''); }, [command]);
  if (!command) return null;
  return <section className="command-panel" aria-label="Confirmação de comando">
    <div><strong>{command.title}</strong><p>{command.description}</p></div>
    <label className="field command-reason"><span>Motivo operacional</span><textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} rows={3} placeholder="Informe o motivo que será enviado e auditado pelo backend." /></label>
    <div className="actions"><button className="secondary" type="button" disabled={busy} onClick={onCancel}>Cancelar</button><button type="button" disabled={busy || !reason.trim()} onClick={() => onConfirm(reason.trim())}>{busy ? 'Confirmando...' : 'Confirmar comando'}</button></div>
  </section>;
}

export function useCommand(remote) {
  const [command, setCommand] = useState(null);
  const [busy, setBusy] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  async function confirm(reason) {
    if (!command || busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const result = await command.run(reason);
      if (result === undefined) throw new Error('O backend não confirmou a operação.');
      const refreshed = await remote.reload();
      if (refreshed === undefined) throw new Error('A operação foi enviada, mas o estado persistido não pôde ser recarregado.');
      setSuccess(command.success);
      setCommand(null);
    } catch (reasonError) {
      setError(formatError(reasonError));
    } finally {
      setBusy(false);
    }
  }

  return { command, setCommand, busy, success, setSuccess, error, setError, confirm };
}

export function YardPageHeader({ path, navigate, title, description, actions }) {
  return <>
    <PageHeader eyebrow="Pátio" title={title} description={description} actions={actions} />
    <YardTabs activePath={path} navigate={navigate} />
  </>;
}
