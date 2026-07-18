import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

const ACTIVE_DAMAGE_STATUSES = new Set(['ABERTA', 'BLOQUEADA', 'EM_INSPECAO', 'EM_REPARO']);

function createCommandId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (token) => {
    const random = Math.floor(Math.random() * 16);
    const value = token === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

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
    codigo: '',
    descricao: '',
    quantidadeAfetada: '',
    volumeAfetadoM3: '0',
    pesoAfetadoKg: '0',
    responsavel
  };
}

function emptyEvidence(responsavel) {
  return { tipo: 'FOTO', uri: '', checksum: '', responsavel };
}

function transitionActions(status) {
  if (status === 'BLOQUEADA' || status === 'ABERTA') return ['INSPECIONAR', 'BAIXAR'];
  if (status === 'EM_INSPECAO') return ['REPARAR', 'BAIXAR'];
  if (status === 'EM_REPARO') return ['CONCLUIR_REPARO', 'BAIXAR'];
  return [];
}

function sumActive(damages, field) {
  return damages
    .filter((damage) => ACTIVE_DAMAGE_STATUSES.has(damage.status))
    .reduce((total, damage) => total + Number(damage[field] ?? 0), 0);
}

function available(total, blocked) {
  return Math.max(Number(total ?? 0) - blocked, 0);
}

export function GeneralCargoDamageInspector({ lot, onChanged }) {
  const usuarioPadrao = readSession()?.nome || 'operador';
  const [damages, setDamages] = useState([]);
  const [selectedDamageId, setSelectedDamageId] = useState('');
  const [damage, setDamage] = useState(() => emptyDamage(usuarioPadrao));
  const [evidence, setEvidence] = useState(() => emptyEvidence(usuarioPadrao));
  const [transition, setTransition] = useState({ acao: '', observacao: '', usuario: usuarioPadrao });
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const reload = useCallback(async () => {
    if (!lot?.id) {
      setDamages([]);
      setSelectedDamageId('');
      return [];
    }
    setLoading(true);
    setError('');
    try {
      const response = await generalCargoApi.listarAvarias(lot.id);
      const rows = Array.isArray(response) ? response : [];
      setDamages(rows);
      setSelectedDamageId((current) => rows.some((item) => item.id === current) ? current : rows[0]?.id || '');
      return rows;
    } catch (reason) {
      setError(formatError(reason));
      return [];
    } finally {
      setLoading(false);
    }
  }, [lot?.id]);

  useEffect(() => {
    setDamage(emptyDamage(usuarioPadrao));
    setEvidence(emptyEvidence(usuarioPadrao));
    setTransition({ acao: '', observacao: '', usuario: usuarioPadrao });
    reload();
  }, [lot?.id, reload, usuarioPadrao]);

  const selectedDamage = useMemo(
    () => damages.find((item) => item.id === selectedDamageId) ?? null,
    [damages, selectedDamageId]
  );

  const actions = useMemo(() => transitionActions(selectedDamage?.status), [selectedDamage?.status]);

  useEffect(() => {
    setTransition((current) => ({
      ...current,
      acao: actions.includes(current.acao) ? current.acao : actions[0] || ''
    }));
  }, [actions]);

  const blockedQuantity = useMemo(() => sumActive(damages, 'quantidadeAfetada'), [damages]);
  const blockedVolume = useMemo(() => sumActive(damages, 'volumeAfetadoM3'), [damages]);
  const blockedWeight = useMemo(() => sumActive(damages, 'pesoAfetadoKg'), [damages]);

  async function execute(key, action, message) {
    setBusy(key);
    setError('');
    setSuccess('');
    try {
      const result = await action();
      setSuccess(message);
      if (result?.id) setSelectedDamageId(result.id);
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
    const created = await execute('open', () => generalCargoApi.abrirAvaria({
      commandId: createCommandId(),
      loteId: lot.id,
      codigo: damage.codigo,
      descricao: damage.descricao,
      quantidadeAfetada: damage.quantidadeAfetada,
      volumeAfetadoM3: damage.volumeAfetadoM3,
      pesoAfetadoKg: damage.pesoAfetadoKg,
      responsavel: damage.responsavel || usuarioPadrao
    }), 'Avaria aberta e saldo segregado.');
    if (created?.id) setDamage(emptyDamage(usuarioPadrao));
  }

  async function addEvidence(event) {
    event.preventDefault();
    if (!selectedDamage) return;
    const updated = await execute('evidence', () => generalCargoApi.adicionarEvidenciaAvaria(selectedDamage.id, {
      ...evidence,
      checksum: evidence.checksum || null,
      responsavel: evidence.responsavel || usuarioPadrao
    }), 'Evidência anexada à avaria.');
    if (updated?.id) setEvidence(emptyEvidence(usuarioPadrao));
  }

  async function transitionDamage(event) {
    event.preventDefault();
    if (!selectedDamage || !transition.acao) return;
    const updated = await execute('transition', () => generalCargoApi.transicionarAvaria(selectedDamage.id, {
      acao: transition.acao,
      usuario: transition.usuario || usuarioPadrao,
      observacao: transition.observacao || null
    }), transition.acao === 'BAIXAR' ? 'Avaria baixada e saldo removido.' : 'Estado da avaria atualizado.');
    if (updated?.id) setTransition((current) => ({ ...current, observacao: '' }));
  }

  return <Section
    title="Inspector de avarias"
    description={lot ? `Cargo lot ${lot.codigo}: abertura, evidências, inspeção, reparo e baixa com saldo segregado.` : 'Selecione um cargo lot para operar avarias.'}
  >
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    {lot && <div className="metrics-grid">
      <MetricCard label="Saldo total" value={`${number(lot.quantidadeSaldo)} ${lot.unidadeMedida ?? ''}`} />
      <MetricCard label="Saldo segregado" value={`${number(blockedQuantity)} ${lot.unidadeMedida ?? ''}`} />
      <MetricCard label="Saldo disponível" value={`${number(available(lot.quantidadeSaldo, blockedQuantity))} ${lot.unidadeMedida ?? ''}`} />
      <MetricCard label="Volume segregado" value={`${number(blockedVolume)} m³`} />
      <MetricCard label="Peso segregado" value={`${number(blockedWeight)} kg`} />
      <MetricCard label="Casos ativos" value={damages.filter((item) => ACTIVE_DAMAGE_STATUSES.has(item.status)).length} />
    </div>}

    <form className="planner-selection-grid" onSubmit={openDamage}>
      <label className="field"><span>Código da avaria</span><input required maxLength="80" value={damage.codigo} onChange={(event) => setDamage((current) => ({ ...current, codigo: event.target.value }))} /></label>
      <label className="field"><span>Descrição</span><input required maxLength="1000" value={damage.descricao} onChange={(event) => setDamage((current) => ({ ...current, descricao: event.target.value }))} /></label>
      <label className="field"><span>Quantidade afetada</span><input required type="number" min="0.001" step="0.001" value={damage.quantidadeAfetada} onChange={(event) => setDamage((current) => ({ ...current, quantidadeAfetada: event.target.value }))} /></label>
      <label className="field"><span>Volume afetado m³</span><input required type="number" min="0" step="0.001" value={damage.volumeAfetadoM3} onChange={(event) => setDamage((current) => ({ ...current, volumeAfetadoM3: event.target.value }))} /></label>
      <label className="field"><span>Peso afetado kg</span><input required type="number" min="0" step="0.001" value={damage.pesoAfetadoKg} onChange={(event) => setDamage((current) => ({ ...current, pesoAfetadoKg: event.target.value }))} /></label>
      <label className="field"><span>Responsável</span><input required maxLength="120" value={damage.responsavel} onChange={(event) => setDamage((current) => ({ ...current, responsavel: event.target.value }))} /></label>
      <div className="field"><span>Ação</span><button type="submit" className="danger" disabled={!lot?.id || busy === 'open'}>{busy === 'open' ? 'Abrindo...' : 'Abrir avaria'}</button></div>
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
        { key: 'evidencias', label: 'Evidências', render: (row) => row.evidencias?.length ?? 0 },
        { key: 'atualizadoEm', label: 'Atualizado', render: (row) => dateTime(row.atualizadoEm) },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedDamageId(row.id)}>Inspecionar</button> }
      ]}
    /> : <EmptyState title={lot ? 'Nenhuma avaria registrada para o lote' : 'Selecione um cargo lot'} />}

    {selectedDamage && <>
      <div className="planner-selection-grid">
        <div className="field"><span>Avaria selecionada</span><strong>{selectedDamage.codigo}</strong></div>
        <div className="field"><span>Estado</span><StatusBadge value={selectedDamage.status} /></div>
        <div className="field"><span>Descrição</span><strong>{selectedDamage.descricao}</strong></div>
        <div className="field"><span>Inspecionado por</span><strong>{selectedDamage.inspecionadoPor || '—'}</strong></div>
        <div className="field"><span>Reparado por</span><strong>{selectedDamage.reparadoPor || '—'}</strong></div>
        <div className="field"><span>Observações</span><strong>{selectedDamage.observacoes || '—'}</strong></div>
      </div>

      <form className="planner-selection-grid" onSubmit={addEvidence}>
        <label className="field"><span>Tipo de evidência</span><select value={evidence.tipo} onChange={(event) => setEvidence((current) => ({ ...current, tipo: event.target.value }))}><option>FOTO</option><option>VIDEO</option><option>LAUDO</option><option>DOCUMENTO</option><option>OUTRO</option></select></label>
        <label className="field"><span>URI da evidência</span><input required maxLength="1000" value={evidence.uri} onChange={(event) => setEvidence((current) => ({ ...current, uri: event.target.value }))} placeholder="https://... ou referência documental" /></label>
        <label className="field"><span>Checksum</span><input maxLength="128" value={evidence.checksum} onChange={(event) => setEvidence((current) => ({ ...current, checksum: event.target.value }))} /></label>
        <label className="field"><span>Responsável</span><input required maxLength="120" value={evidence.responsavel} onChange={(event) => setEvidence((current) => ({ ...current, responsavel: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'evidence'}>{busy === 'evidence' ? 'Anexando...' : 'Adicionar evidência'}</button></div>
      </form>

      <form className="planner-selection-grid" onSubmit={transitionDamage}>
        <label className="field"><span>Próxima ação</span><select required value={transition.acao} disabled={!actions.length} onChange={(event) => setTransition((current) => ({ ...current, acao: event.target.value }))}>{actions.map((action) => <option key={action}>{action}</option>)}</select></label>
        <label className="field"><span>Usuário</span><input required maxLength="120" value={transition.usuario} onChange={(event) => setTransition((current) => ({ ...current, usuario: event.target.value }))} /></label>
        <label className="field"><span>Observação</span><input maxLength="1000" value={transition.observacao} onChange={(event) => setTransition((current) => ({ ...current, observacao: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!actions.length || busy === 'transition'}>{busy === 'transition' ? 'Atualizando...' : 'Aplicar transição'}</button></div>
      </form>

      {selectedDamage.evidencias?.length ? <DataTable
        rows={selectedDamage.evidencias}
        rowKey={(row, index) => `${row.registradoEm ?? 'evidencia'}-${index}`}
        columns={[
          { key: 'tipo', label: 'Tipo', render: (row) => <StatusBadge value={row.tipo} /> },
          { key: 'uri', label: 'Referência', render: (row) => /^https?:\/\//i.test(row.uri ?? '') ? <a href={row.uri} target="_blank" rel="noreferrer">Abrir evidência</a> : row.uri },
          { key: 'checksum', label: 'Checksum' },
          { key: 'responsavel', label: 'Responsável' },
          { key: 'registradoEm', label: 'Registrada em', render: (row) => dateTime(row.registradoEm) }
        ]}
      /> : <EmptyState title="Avaria sem evidências" />}
    </>}
  </Section>;
}
