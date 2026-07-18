import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

const BLOCKED_STATUSES = new Set(['ABERTA', 'SEGREGADA', 'EM_TRATAMENTO', 'BLOQUEADA']);

function number(value, digits = 3) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: digits }).format(Number(value ?? 0));
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function emptyDamage(responsavel) {
  return {
    codigo: '', descricao: '', quantidadeAfetada: '', volumeAfetadoM3: '0', pesoAfetadoKg: '0',
    responsavel, evidenciaTipo: 'FOTO', evidenciaUri: '', evidenciaChecksum: ''
  };
}

function parseEvidence(value) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [parsed];
  } catch {
    return [{ tipo: 'REFERENCIA', uri: value, checksum: '', responsavel: '', registradoEm: '' }];
  }
}

function parseHistory(value) {
  if (!value) return [];
  return String(value).split('\n').filter(Boolean).map((line, index) => {
    const [ocorridoEm, usuario, evento, ...detalhe] = line.split('|');
    return { id: `${ocorridoEm}-${index}`, ocorridoEm, usuario, evento, detalhe: detalhe.join('|') };
  });
}

function sumBlocked(damages, field) {
  return damages
    .filter((damage) => BLOCKED_STATUSES.has(damage.status))
    .reduce((total, damage) => total + Number(damage[field] ?? 0), 0);
}

function available(total, blocked) {
  return Math.max(Number(total ?? 0) - blocked, 0);
}

export function GeneralCargoDamageInspector({ lot, onChanged }) {
  const defaultUser = readSession()?.nome || 'operador';
  const [damages, setDamages] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [damage, setDamage] = useState(() => emptyDamage(defaultUser));
  const [inspection, setInspection] = useState({ relatorio: '', usuario: defaultUser });
  const [closing, setClosing] = useState({ resultado: 'REINTEGRAR', observacao: '', usuario: defaultUser });
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const reload = useCallback(async () => {
    if (!lot?.id) {
      setDamages([]);
      setSelectedId('');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await generalCargoApi.listarAvarias(lot.id);
      const rows = Array.isArray(response) ? response : [];
      setDamages(rows);
      setSelectedId((current) => rows.some((item) => item.id === current) ? current : rows[0]?.id || '');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, [lot?.id]);

  useEffect(() => {
    setDamage(emptyDamage(defaultUser));
    setInspection({ relatorio: '', usuario: defaultUser });
    setClosing({ resultado: 'REINTEGRAR', observacao: '', usuario: defaultUser });
    reload();
  }, [defaultUser, lot?.id, reload]);

  const selected = useMemo(() => damages.find((item) => item.id === selectedId) ?? null, [damages, selectedId]);
  const evidenceRows = useMemo(() => parseEvidence(selected?.evidenciasJson), [selected?.evidenciasJson]);
  const historyRows = useMemo(() => parseHistory(selected?.historicoOperacional), [selected?.historicoOperacional]);
  const blockedQuantity = useMemo(() => sumBlocked(damages, 'quantidadeAfetada'), [damages]);
  const blockedVolume = useMemo(() => sumBlocked(damages, 'volumeAfetadoM3'), [damages]);
  const blockedWeight = useMemo(() => sumBlocked(damages, 'pesoAfetadoKg'), [damages]);

  async function execute(key, action, message) {
    setBusy(key);
    setError('');
    setSuccess('');
    try {
      const result = await action();
      setSuccess(message);
      if (result?.id) setSelectedId(result.id);
      await reload();
      await onChanged?.();
      return result;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusy('');
    }
  }

  async function openDamage(event) {
    event.preventDefault();
    if (!lot?.id) return;
    const evidence = [{
      tipo: damage.evidenciaTipo,
      uri: damage.evidenciaUri,
      checksum: damage.evidenciaChecksum || null,
      responsavel: damage.responsavel || defaultUser,
      registradoEm: new Date().toISOString()
    }];
    const created = await execute('open', () => generalCargoApi.registrarAvaria({
      loteId: lot.id,
      codigo: damage.codigo,
      descricao: damage.descricao,
      quantidadeAfetada: damage.quantidadeAfetada,
      volumeAfetadoM3: damage.volumeAfetadoM3,
      pesoAfetadoKg: damage.pesoAfetadoKg,
      responsavel: damage.responsavel || defaultUser,
      evidenciasJson: JSON.stringify(evidence)
    }), 'Avaria registrada e saldo segregado.');
    if (created?.id) setDamage(emptyDamage(defaultUser));
  }

  async function inspectDamage(event) {
    event.preventDefault();
    if (!selected) return;
    const updated = await execute('inspect', () => generalCargoApi.inspecionarAvaria(selected.id, inspection), 'Inspeção registrada.');
    if (updated?.id) setInspection((current) => ({ ...current, relatorio: '' }));
  }

  async function closeDamage(event) {
    event.preventDefault();
    if (!selected) return;
    const updated = await execute('close', () => generalCargoApi.encerrarAvaria(selected.id, closing),
      closing.resultado === 'REINTEGRAR' ? 'Saldo reparado e reintegrado.' : closing.resultado === 'BAIXAR' ? 'Saldo avariado baixado.' : 'Saldo mantido bloqueado.');
    if (updated?.id) setClosing((current) => ({ ...current, observacao: '' }));
  }

  return <Section
    title="Inspector de avarias"
    description={lot ? `Cargo lot ${lot.codigo}: evidências, saldo segregado, inspeção, reparo e baixa.` : 'Selecione um cargo lot para operar avarias.'}
  >
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    {lot && <div className="metrics-grid">
      <MetricCard label="Saldo total" value={`${number(lot.quantidadeSaldo)} ${lot.unidadeMedida ?? ''}`} />
      <MetricCard label="Saldo segregado" value={`${number(blockedQuantity)} ${lot.unidadeMedida ?? ''}`} />
      <MetricCard label="Saldo disponível" value={`${number(available(lot.quantidadeSaldo, blockedQuantity))} ${lot.unidadeMedida ?? ''}`} />
      <MetricCard label="Volume segregado" value={`${number(blockedVolume)} m³`} />
      <MetricCard label="Peso segregado" value={`${number(blockedWeight)} kg`} />
      <MetricCard label="Casos bloqueando saldo" value={damages.filter((item) => BLOCKED_STATUSES.has(item.status)).length} />
    </div>}

    <form className="planner-selection-grid" onSubmit={openDamage}>
      <label className="field"><span>Código da avaria</span><input required maxLength="80" value={damage.codigo} onChange={(event) => setDamage((current) => ({ ...current, codigo: event.target.value }))} /></label>
      <label className="field"><span>Descrição</span><input required maxLength="1000" value={damage.descricao} onChange={(event) => setDamage((current) => ({ ...current, descricao: event.target.value }))} /></label>
      <label className="field"><span>Quantidade afetada</span><input required type="number" min="0.001" step="0.001" value={damage.quantidadeAfetada} onChange={(event) => setDamage((current) => ({ ...current, quantidadeAfetada: event.target.value }))} /></label>
      <label className="field"><span>Volume afetado m³</span><input required type="number" min="0" step="0.001" value={damage.volumeAfetadoM3} onChange={(event) => setDamage((current) => ({ ...current, volumeAfetadoM3: event.target.value }))} /></label>
      <label className="field"><span>Peso afetado kg</span><input required type="number" min="0" step="0.001" value={damage.pesoAfetadoKg} onChange={(event) => setDamage((current) => ({ ...current, pesoAfetadoKg: event.target.value }))} /></label>
      <label className="field"><span>Responsável</span><input required maxLength="120" value={damage.responsavel} onChange={(event) => setDamage((current) => ({ ...current, responsavel: event.target.value }))} /></label>
      <label className="field"><span>Tipo da evidência</span><select value={damage.evidenciaTipo} onChange={(event) => setDamage((current) => ({ ...current, evidenciaTipo: event.target.value }))}><option>FOTO</option><option>VIDEO</option><option>LAUDO</option><option>DOCUMENTO</option><option>OUTRO</option></select></label>
      <label className="field"><span>URI da evidência</span><input required maxLength="1000" value={damage.evidenciaUri} onChange={(event) => setDamage((current) => ({ ...current, evidenciaUri: event.target.value }))} placeholder="https://... ou referência documental" /></label>
      <label className="field"><span>Checksum</span><input maxLength="128" value={damage.evidenciaChecksum} onChange={(event) => setDamage((current) => ({ ...current, evidenciaChecksum: event.target.value }))} /></label>
      <div className="field"><span>Ação</span><button type="submit" className="danger" disabled={!lot?.id || busy === 'open'}>{busy === 'open' ? 'Registrando...' : 'Registrar avaria'}</button></div>
    </form>

    {loading ? <Loading label="Carregando avarias..." /> : damages.length ? <DataTable
      gridId="general-cargo-damages"
      exportFileName="avarias-carga-geral"
      rows={damages}
      rowKey="id"
      columns={[
        { key: 'codigo', label: 'Código' },
        { key: 'status', label: 'Estado', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'quantidadeAfetada', label: 'Quantidade' },
        { key: 'volumeAfetadoM3', label: 'Volume m³' },
        { key: 'pesoAfetadoKg', label: 'Peso kg' },
        { key: 'responsavel', label: 'Responsável' },
        { key: 'resultadoTratamento', label: 'Resultado', render: (row) => row.resultadoTratamento ? <StatusBadge value={row.resultadoTratamento} /> : '—' },
        { key: 'criadoEm', label: 'Criada em', render: (row) => dateTime(row.criadoEm) },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedId(row.id)}>Inspecionar</button> }
      ]}
    /> : <EmptyState title={lot ? 'Nenhuma avaria registrada para o lote' : 'Selecione um cargo lot'} />}

    {selected && <>
      <div className="planner-selection-grid">
        <div className="field"><span>Avaria selecionada</span><strong>{selected.codigo}</strong></div>
        <div className="field"><span>Estado</span><StatusBadge value={selected.status} /></div>
        <div className="field"><span>Descrição</span><strong>{selected.descricao}</strong></div>
        <div className="field"><span>Relatório de inspeção</span><strong>{selected.relatorioInspecao || '—'}</strong></div>
        <div className="field"><span>Resultado</span><strong>{selected.resultadoTratamento || '—'}</strong></div>
        <div className="field"><span>Encerrada em</span><strong>{dateTime(selected.encerradoEm)}</strong></div>
      </div>

      {(selected.status === 'ABERTA' || selected.status === 'SEGREGADA') && <form className="planner-selection-grid" onSubmit={inspectDamage}>
        <label className="field"><span>Relatório de inspeção</span><input required maxLength="4000" value={inspection.relatorio} onChange={(event) => setInspection((current) => ({ ...current, relatorio: event.target.value }))} /></label>
        <label className="field"><span>Inspetor</span><input required maxLength="120" value={inspection.usuario} onChange={(event) => setInspection((current) => ({ ...current, usuario: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'inspect'}>{busy === 'inspect' ? 'Registrando...' : 'Registrar inspeção'}</button></div>
      </form>}

      {selected.status === 'EM_TRATAMENTO' && <form className="planner-selection-grid" onSubmit={closeDamage}>
        <label className="field"><span>Resultado do tratamento</span><select value={closing.resultado} onChange={(event) => setClosing((current) => ({ ...current, resultado: event.target.value }))}><option value="REINTEGRAR">Reparar e reintegrar</option><option value="BAIXAR">Baixar do estoque</option><option value="MANTER_BLOQUEADA">Manter bloqueada</option></select></label>
        <label className="field"><span>Observação de encerramento</span><input required maxLength="2000" value={closing.observacao} onChange={(event) => setClosing((current) => ({ ...current, observacao: event.target.value }))} /></label>
        <label className="field"><span>Responsável pela decisão</span><input required maxLength="120" value={closing.usuario} onChange={(event) => setClosing((current) => ({ ...current, usuario: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'close'}>{busy === 'close' ? 'Encerrando...' : 'Encerrar avaria'}</button></div>
      </form>}

      {evidenceRows.length ? <DataTable rows={evidenceRows} rowKey={(row, index) => `${row.registradoEm ?? 'evidencia'}-${index}`} columns={[
        { key: 'tipo', label: 'Tipo', render: (row) => <StatusBadge value={row.tipo} /> },
        { key: 'uri', label: 'Referência', render: (row) => /^https?:\/\//i.test(row.uri ?? '') ? <a href={row.uri} target="_blank" rel="noreferrer">Abrir evidência</a> : row.uri },
        { key: 'checksum', label: 'Checksum' },
        { key: 'responsavel', label: 'Responsável' },
        { key: 'registradoEm', label: 'Registrada em', render: (row) => dateTime(row.registradoEm) }
      ]} /> : <EmptyState title="Avaria sem evidência estruturada" />}

      {historyRows.length ? <DataTable rows={historyRows} rowKey="id" columns={[
        { key: 'ocorridoEm', label: 'Data', render: (row) => dateTime(row.ocorridoEm) },
        { key: 'evento', label: 'Evento', render: (row) => <StatusBadge value={row.evento} /> },
        { key: 'usuario', label: 'Usuário' },
        { key: 'detalhe', label: 'Detalhe' }
      ]} /> : null}
    </>}
  </Section>;
}
