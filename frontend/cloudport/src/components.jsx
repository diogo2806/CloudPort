import { ContextHelp } from './ContextHelp.jsx';
import { OperationalDataGrid } from './OperationalDataGrid.jsx';
import { OperatorModeLauncher } from './OperatorMode.jsx';
import { sanitizeText } from './api.js';

export { ContextHelp, OperationalDataGrid, OperatorModeLauncher };

export function PageHeader({ eyebrow, title, description, actions }) {
  return <header className="page-header">
    <div>
      {eyebrow && <span className="eyebrow">{eyebrow}</span>}
      <h1>{title}</h1>
      {description && <p>{description}</p>}
    </div>
    <div className="page-actions">{actions}<OperatorModeLauncher /><ContextHelp /></div>
  </header>;
}

export function Message({ type = 'info', children, onClose }) {
  if (!children) return null;
  return <div className={`message ${type}`} role={type === 'error' ? 'alert' : 'status'}>
    <span>{children}</span>
    {onClose && <button className="icon-button" type="button" aria-label="Fechar mensagem" onClick={onClose}>×</button>}
  </div>;
}

export function Loading({ label = 'Carregando...' }) {
  return <div className="loading" role="status"><span className="spinner" />{label}</div>;
}

export function EmptyState({ title = 'Nenhum registro encontrado', description }) {
  return <div className="empty-state"><strong>{title}</strong>{description && <span>{description}</span>}</div>;
}

export function StatusBadge({ value }) {
  const label = sanitizeText(value) || 'INDEFINIDO';
  const normalized = label.toLowerCase().replace(/[^a-z0-9]+/g, '-');
  return <span className={`status status-${normalized}`}>{label}</span>;
}

export function MetricCard({ label, value, detail }) {
  return <article className="metric-card"><span>{label}</span><strong>{value ?? '—'}</strong>{detail && <small>{detail}</small>}</article>;
}

export function DataTable(props) {
  return <OperationalDataGrid {...props} />;
}

export function JsonDetails({ value, title = 'Resposta da operação' }) {
  if (value === undefined || value === null) return null;
  return <details className="json-details"><summary>{title}</summary><pre>{JSON.stringify(value, null, 2)}</pre></details>;
}

export function Section({ title, description, actions, children }) {
  return <section className="panel">
    <header className="panel-header"><div><h2>{title}</h2>{description && <p>{description}</p>}</div>{actions && <div className="panel-actions">{actions}</div>}</header>
    {children}
  </section>;
}

export function Field({ label, children, hint }) {
  return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>;
}
