import { useCallback, useEffect, useState } from 'react';
import { dateTime, statusClass } from './ui20-model.js';
import { reconciliacaoBarcodeApi, TIPOS_DESINCRONIA } from './reconciliacao-barcode-api.js';

function formatError(error) {
  if (error?.status === 401) return 'A sessão expirou. Entre novamente.';
  if (error?.status === 403) return 'Sua conta não possui permissão para administrar reconciliações.';
  return error?.message || 'Não foi possível concluir a operação.';
}

export default function ReconciliacaoBarcodePage({ session, onBack, onLogout }) {
  const [items, setItems] = useState([]);
  const [type, setType] = useState('');
  const [resolutions, setResolutions] = useState({});
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = type
        ? await reconciliacaoBarcodeApi.listarPorTipo(type)
        : await reconciliacaoBarcodeApi.listarNaoResolvidas();
      setItems((data ?? []).filter((item) => !item.resolvidoEm));
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, [type]);

  useEffect(() => { load(); }, [load]);

  async function resolve(item) {
    const resolution = String(resolutions[item.id] ?? '').trim();
    if (!resolution) {
      setError('Informe a resolução aplicada antes de concluir a reconciliação.');
      return;
    }
    setBusyId(item.id);
    setError('');
    setSuccess('');
    try {
      await reconciliacaoBarcodeApi.resolver(item.id, resolution);
      setItems((current) => current.filter((currentItem) => currentItem.id !== item.id));
      setResolutions((current) => ({ ...current, [item.id]: '' }));
      setSuccess(`Reconciliação ${item.id} resolvida.`);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusyId(null);
    }
  }

  return <div className="reconciliation-app">
    <header className="reconciliation-topbar">
      <div><span className="eyebrow">CloudPort</span><h1>Reconciliação de Barcode</h1><p>Desincronias operacionais pendentes de tratamento.</p></div>
      <div className="reconciliation-actions"><span>{session.nome}</span><button className="secondary" onClick={onBack}>Control Room</button><button className="danger" onClick={onLogout}>Sair</button></div>
    </header>
    <main className="reconciliation-content">
      {error && <div className="message error">{error}</div>}
      {success && <div className="message success">{success}</div>}
      <section className="panel reconciliation-filter">
        <label>Tipo de desincronia<select value={type} onChange={(event) => setType(event.target.value)}><option value="">Todos os tipos</option>{TIPOS_DESINCRONIA.map((value) => <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>)}</select></label>
        <button className="secondary" onClick={load} disabled={loading}>{loading ? 'Carregando...' : 'Atualizar'}</button>
      </section>
      <section className="panel">
        <div className="section-head"><h2>Não resolvidas</h2><span>{items.length}</span></div>
        {loading ? <p className="empty">Carregando reconciliações...</p> : !items.length ? <p className="empty">Nenhuma reconciliação pendente para o filtro selecionado.</p> : <div className="table-wrap"><table className="reconciliation-table"><thead><tr><th>ID</th><th>Gate Pass</th><th>Tipo</th><th>Descrição</th><th>Barcode</th><th>Status</th><th>Detectado em</th><th>Resolução</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td>{item.id}</td><td>{item.codigoGatePass || item.gatePassId}</td><td><span className={statusClass(item.tipoDesinconia)}>{item.tipoDesinconia}</span></td><td>{item.descricao || '—'}</td><td><small>Esperado: {item.barcodeEsperado || '—'}<br />Recebido: {item.barcodeRecebido || '—'}</small></td><td><small>TOS: {item.statusTos || '—'}<br />Local: {item.statusLocal || '—'}<br />Alerta: {item.alertaEnviado ? 'enviado' : 'pendente'}</small></td><td>{dateTime(item.detectadoEm)}</td><td><textarea value={resolutions[item.id] ?? ''} onChange={(event) => setResolutions((current) => ({ ...current, [item.id]: event.target.value }))} maxLength={500} placeholder="Descreva a correção aplicada" /><button onClick={() => resolve(item)} disabled={busyId === item.id}>{busyId === item.id ? 'Salvando...' : 'Resolver'}</button></td></tr>)}</tbody></table></div>}
      </section>
    </main>
  </div>;
}
