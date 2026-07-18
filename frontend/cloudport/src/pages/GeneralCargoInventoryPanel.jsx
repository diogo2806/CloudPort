import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Message, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

function newCommandId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `${Date.now()}-0000-4000-8000-${Math.random().toString(16).slice(2).padEnd(12, '0').slice(0, 12)}`;
}

function userName() {
  return readSession()?.nome || 'operador';
}

function blankIdentification() {
  return { codigo: '', tipo: 'CODIGO_BARRAS', embalagemReferencia: '' };
}

function blankCount() {
  return { identificacao: '', quantidadeContada: '', volumeContadoM3: '', pesoContadoKg: '', observacao: '' };
}

export function GeneralCargoInventoryPanel({ selectedLot, onLotSelected, onChanged }) {
  const [inventories, setInventories] = useState([]);
  const [selectedInventoryId, setSelectedInventoryId] = useState('');
  const [identification, setIdentification] = useState(blankIdentification);
  const [scanCode, setScanCode] = useState('');
  const [resolved, setResolved] = useState(null);
  const [position, setPosition] = useState('');
  const [count, setCount] = useState(blankCount);
  const [divergence, setDivergence] = useState({ commandId: '', ajustarSaldo: true, motivo: '' });
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const reloadInventories = useCallback(async () => {
    try {
      const result = await generalCargoApi.listarInventarios();
      const items = Array.isArray(result) ? result : [];
      setInventories(items);
      setSelectedInventoryId((current) => current || items.find((item) => item.status !== 'CONCLUIDO')?.id || items[0]?.id || '');
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  useEffect(() => { reloadInventories(); }, [reloadInventories]);

  const selectedInventory = useMemo(
    () => inventories.find((item) => item.id === selectedInventoryId) ?? null,
    [inventories, selectedInventoryId]
  );

  async function execute(key, action, message) {
    setBusy(key);
    setError('');
    setSuccess('');
    try {
      const result = await action();
      setSuccess(message);
      await reloadInventories();
      await onChanged?.();
      return result;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusy('');
    }
  }

  async function registerIdentification(event) {
    event.preventDefault();
    if (!selectedLot) return;
    const result = await execute('identification', () => generalCargoApi.registrarIdentificacao({
      ...identification,
      loteId: selectedLot.id,
      embalagemReferencia: identification.embalagemReferencia || null,
      usuario: userName()
    }), 'Identificação física registrada.');
    if (result) {
      setResolved(result);
      setScanCode(result.codigo);
      setCount((current) => ({ ...current, identificacao: result.codigo }));
      setIdentification(blankIdentification());
    }
  }

  async function resolveIdentification(event) {
    event.preventDefault();
    if (!scanCode.trim()) return;
    const result = await execute('scan', () => generalCargoApi.resolverIdentificacao(scanCode.trim()), 'Identificação localizada.');
    if (result) {
      setResolved(result);
      setCount((current) => ({ ...current, identificacao: result.codigo }));
      onLotSelected?.(result.loteId);
    }
  }

  async function openInventory(event) {
    event.preventDefault();
    const result = await execute('open', () => generalCargoApi.abrirInventario({
      commandId: newCommandId(),
      posicao: position.trim(),
      usuario: userName()
    }), 'Sessão de inventário aberta.');
    if (result) {
      setSelectedInventoryId(result.id);
      setPosition('');
    }
  }

  async function registerCount(event) {
    event.preventDefault();
    if (!selectedInventoryId || !count.identificacao.trim()) return;
    const physicalIdentification = await generalCargoApi.resolverIdentificacao(count.identificacao.trim());
    const result = await execute('count', () => generalCargoApi.registrarContagem(selectedInventoryId, {
      commandId: newCommandId(),
      loteId: physicalIdentification.loteId,
      identificacao: physicalIdentification.codigo,
      quantidadeContada: count.quantidadeContada,
      volumeContadoM3: count.volumeContadoM3,
      pesoContadoKg: count.pesoContadoKg,
      usuario: userName(),
      observacao: count.observacao || null
    }), 'Contagem física registrada sem alterar o saldo lógico.');
    if (result) {
      onLotSelected?.(physicalIdentification.loteId);
      setCount(blankCount());
    }
  }

  async function resolveDivergence(event) {
    event.preventDefault();
    if (!selectedInventoryId || !divergence.commandId) return;
    const result = await execute('divergence', () => generalCargoApi.resolverDivergencia(selectedInventoryId, {
      commandIdContagem: divergence.commandId,
      ajustarSaldo: divergence.ajustarSaldo,
      usuario: userName(),
      motivo: divergence.motivo.trim()
    }), divergence.ajustarSaldo ? 'Divergência aprovada e ajuste auditável aplicado.' : 'Divergência rejeitada sem alteração do saldo.');
    if (result) setDivergence({ commandId: '', ajustarSaldo: true, motivo: '' });
  }

  async function concludeInventory() {
    if (!selectedInventoryId) return;
    await execute('conclude', () => generalCargoApi.concluirInventario(selectedInventoryId, {
      usuario: userName(),
      motivo: 'Inventário físico concluído após tratamento das divergências.'
    }), 'Inventário físico concluído.');
  }

  const pendingCounts = selectedInventory?.contagens?.filter((item) => item.statusDivergencia === 'PENDENTE') ?? [];

  return <>
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <Section title="Identificação física" description={selectedLot ? `Cargo lot selecionado: ${selectedLot.codigo}. Cadastre ou leia código de barras e QR da embalagem.` : 'Selecione um cargo lot para cadastrar sua identificação física.'}>
      <form className="planner-selection-grid" onSubmit={registerIdentification}>
        <label className="field"><span>Código</span><input required maxLength="160" value={identification.codigo} onChange={(event) => setIdentification((current) => ({ ...current, codigo: event.target.value }))} /></label>
        <label className="field"><span>Tipo</span><select value={identification.tipo} onChange={(event) => setIdentification((current) => ({ ...current, tipo: event.target.value }))}><option value="CODIGO_BARRAS">Código de barras</option><option value="QR_CODE">QR Code</option></select></label>
        <label className="field"><span>Embalagem</span><input maxLength="160" value={identification.embalagemReferencia} onChange={(event) => setIdentification((current) => ({ ...current, embalagemReferencia: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!selectedLot || busy === 'identification'}>{busy === 'identification' ? 'Registrando...' : 'Registrar identificação'}</button></div>
      </form>
      <form className="planner-selection-grid" onSubmit={resolveIdentification}>
        <label className="field"><span>Leitor de código/QR</span><input required autoComplete="off" autoFocus maxLength="160" value={scanCode} onChange={(event) => setScanCode(event.target.value)} placeholder="Leia ou digite e pressione Enter" /></label>
        <div className="field"><span>Ação</span><button type="submit" className="secondary" disabled={busy === 'scan'}>{busy === 'scan' ? 'Localizando...' : 'Localizar cargo lot'}</button></div>
      </form>
      {resolved && <p><strong>{resolved.loteCodigo}</strong> identificado por <StatusBadge value={resolved.tipo} /> {resolved.embalagemReferencia ? `• embalagem ${resolved.embalagemReferencia}` : ''}</p>}
    </Section>

    <Section title="Inventário físico por posição" description="A contagem preserva o saldo lógico. Somente uma divergência aprovada e motivada gera movimentação de ajuste.">
      <form className="planner-selection-grid" onSubmit={openInventory}>
        <label className="field"><span>Posição</span><input required maxLength="120" value={position} onChange={(event) => setPosition(event.target.value)} placeholder="Armazém, rua, bloco ou posição" /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'open'}>{busy === 'open' ? 'Abrindo...' : 'Abrir inventário'}</button></div>
      </form>
      {inventories.length ? <DataTable rows={inventories} rowKey="id" columns={[
        { key: 'posicao', label: 'Posição' },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'abertoPor', label: 'Aberto por' },
        { key: 'abertoEm', label: 'Abertura', render: (row) => new Date(row.abertoEm).toLocaleString('pt-BR') },
        { key: 'contagens', label: 'Contagens', render: (row) => row.contagens?.length ?? 0 },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedInventoryId(row.id)}>Selecionar</button> }
      ]} /> : <EmptyState title="Nenhuma sessão de inventário" />}
    </Section>

    <Section title="Registrar contagem" description={selectedInventory ? `Sessão ${selectedInventory.posicao} • ${selectedInventory.status}` : 'Abra ou selecione uma sessão de inventário.'}>
      <form className="planner-selection-grid" onSubmit={registerCount}>
        <label className="field"><span>Código de barras/QR</span><input required maxLength="160" value={count.identificacao} onChange={(event) => setCount((current) => ({ ...current, identificacao: event.target.value }))} /></label>
        <label className="field"><span>Quantidade contada</span><input required type="number" min="0" step="0.001" value={count.quantidadeContada} onChange={(event) => setCount((current) => ({ ...current, quantidadeContada: event.target.value }))} /></label>
        <label className="field"><span>Volume contado m³</span><input required type="number" min="0" step="0.001" value={count.volumeContadoM3} onChange={(event) => setCount((current) => ({ ...current, volumeContadoM3: event.target.value }))} /></label>
        <label className="field"><span>Peso contado kg</span><input required type="number" min="0" step="0.001" value={count.pesoContadoKg} onChange={(event) => setCount((current) => ({ ...current, pesoContadoKg: event.target.value }))} /></label>
        <label className="field"><span>Observação</span><input maxLength="1000" value={count.observacao} onChange={(event) => setCount((current) => ({ ...current, observacao: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!selectedInventoryId || busy === 'count'}>{busy === 'count' ? 'Registrando...' : 'Registrar contagem'}</button></div>
      </form>
      {!!selectedInventory?.contagens?.length && <DataTable rows={selectedInventory.contagens} rowKey="commandId" columns={[
        { key: 'identificacao', label: 'Identificação' },
        { key: 'quantidadeLogica', label: 'Qtd. lógica' },
        { key: 'quantidadeContada', label: 'Qtd. contada' },
        { key: 'volumeLogicoM3', label: 'Volume lógico' },
        { key: 'volumeContadoM3', label: 'Volume contado' },
        { key: 'pesoLogicoKg', label: 'Peso lógico' },
        { key: 'pesoContadoKg', label: 'Peso contado' },
        { key: 'statusDivergencia', label: 'Divergência', render: (row) => <StatusBadge value={row.statusDivergencia} /> },
        { key: 'acao', label: 'Ação', exportable: false, render: (row) => row.statusDivergencia === 'PENDENTE' ? <button type="button" className="secondary small" onClick={() => setDivergence((current) => ({ ...current, commandId: row.commandId }))}>Tratar</button> : '—' }
      ]} />}
    </Section>

    <Section title="Tratamento da divergência" description={pendingCounts.length ? `${pendingCounts.length} divergência(s) aguardando decisão.` : 'Não há divergências pendentes na sessão selecionada.'}>
      <form className="planner-selection-grid" onSubmit={resolveDivergence}>
        <label className="field"><span>Contagem</span><select required value={divergence.commandId} onChange={(event) => setDivergence((current) => ({ ...current, commandId: event.target.value }))}><option value="">Selecione</option>{pendingCounts.map((item) => <option key={item.commandId} value={item.commandId}>{item.identificacao}</option>)}</select></label>
        <label className="field"><span>Decisão</span><select value={divergence.ajustarSaldo ? 'true' : 'false'} onChange={(event) => setDivergence((current) => ({ ...current, ajustarSaldo: event.target.value === 'true' }))}><option value="true">Aprovar ajuste</option><option value="false">Rejeitar divergência</option></select></label>
        <label className="field"><span>Motivo</span><input required maxLength="1000" value={divergence.motivo} onChange={(event) => setDivergence((current) => ({ ...current, motivo: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={!divergence.commandId || busy === 'divergence'}>{busy === 'divergence' ? 'Confirmando...' : 'Confirmar decisão'}</button></div>
        <div className="field"><span>Encerramento</span><button type="button" className="secondary" disabled={!selectedInventoryId || pendingCounts.length > 0 || busy === 'conclude'} onClick={concludeInventory}>{busy === 'conclude' ? 'Concluindo...' : 'Concluir inventário'}</button></div>
      </form>
    </Section>
  </>;
}
