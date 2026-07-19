import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { DataTable, EmptyState, Message, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';

function newCommandId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const random = Math.floor(Math.random() * 16);
    const value = character === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

function currentUser() {
  const session = readSession();
  return session?.nome || session?.email || 'operador';
}

function blankSealEvent() {
  return {
    commandId: newCommandId(), tipoEvento: 'CONFERIDO', numeroLacre: '',
    numeroLacreSubstituido: '', motivo: '', divergencia: false, overrideAutorizado: false
  };
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

export function StuffUnstuffSealPanel({ onChanged }) {
  const [operations, setOperations] = useState([]);
  const [operationId, setOperationId] = useState('');
  const [seals, setSeals] = useState([]);
  const [event, setEvent] = useState(blankSealEvent);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadOperations = useCallback(async () => {
    const result = await generalCargoApi.listarOperacoesStuffUnstuff();
    const values = Array.isArray(result) ? result : [];
    setOperations(values);
    setOperationId((current) => current || values[0]?.id || '');
  }, []);

  const loadSeals = useCallback(async (id) => {
    if (!id) {
      setSeals([]);
      return;
    }
    const result = await generalCargoApi.listarLacresStuffUnstuff(id);
    setSeals(Array.isArray(result) ? result : []);
  }, []);

  useEffect(() => {
    loadOperations().catch((reason) => setError(formatError(reason)));
  }, [loadOperations]);

  useEffect(() => {
    loadSeals(operationId).catch((reason) => setError(formatError(reason)));
  }, [loadSeals, operationId]);

  const selectedOperation = useMemo(
    () => operations.find((operation) => operation.id === operationId),
    [operationId, operations]
  );
  const divergenceOpen = seals.some((seal) => seal.divergenciaAberta && !seal.overrideAutorizado);
  const terminal = ['CONCLUIDA', 'CANCELADA'].includes(selectedOperation?.status);

  async function submit(eventSubmit) {
    eventSubmit.preventDefault();
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await generalCargoApi.registrarLacreStuffUnstuff(operationId, {
        ...event,
        numeroLacreSubstituido: event.numeroLacreSubstituido || null,
        motivo: event.motivo || null,
        operador: currentUser()
      });
      setEvent(blankSealEvent());
      await Promise.all([loadSeals(operationId), loadOperations()]);
      if (onChanged) await onChanged();
      setSuccess('Evento de lacre registrado com rastreabilidade e idempotência.');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <Section
    title="Ciclo operacional de lacres"
    description="Registre lacres previstos, aplicados, rompidos, substituídos e conferidos. Divergências abertas bloqueiam a conclusão da operação."
  >
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <label className="field"><span>Operação</span><select value={operationId} onChange={(change) => setOperationId(change.target.value)}>
      <option value="">Selecione</option>
      {operations.map((operation) => <option key={operation.id} value={operation.id}>
        {operation.tipo} | {operation.conteinerId} | {operation.status}
      </option>)}
    </select></label>

    {selectedOperation && <div className="summary-grid">
      <div><small>Contêiner</small><strong>{selectedOperation.conteinerId}</strong></div>
      <div><small>Estado</small><StatusBadge status={selectedOperation.status} /></div>
      <div><small>Lacre inicial</small><strong>{selectedOperation.lacreInicial || '—'}</strong></div>
      <div><small>Conferência</small><StatusBadge status={divergenceOpen ? 'DIVERGENTE' : 'SEM_PENDENCIA'} /></div>
    </div>}

    <form className="planner-selection-grid" onSubmit={submit}>
      <label className="field"><span>Evento</span><select disabled={!operationId || terminal} value={event.tipoEvento} onChange={(change) => setEvent((current) => ({ ...current, tipoEvento: change.target.value }))}>
        <option value="PREVISTO">Previsto</option>
        <option value="APLICADO">Aplicado</option>
        <option value="ROMPIDO">Rompido</option>
        <option value="SUBSTITUIDO">Substituído</option>
        <option value="CONFERIDO">Conferido</option>
      </select></label>
      <label className="field"><span>Número do lacre</span><input required maxLength="80" disabled={!operationId || terminal} value={event.numeroLacre} onChange={(change) => setEvent((current) => ({ ...current, numeroLacre: change.target.value }))} /></label>
      <label className="field"><span>Lacre substituído</span><input maxLength="80" disabled={!operationId || terminal || event.tipoEvento !== 'SUBSTITUIDO'} value={event.numeroLacreSubstituido} onChange={(change) => setEvent((current) => ({ ...current, numeroLacreSubstituido: change.target.value }))} /></label>
      <label className="field"><span>Motivo</span><input maxLength="1000" disabled={!operationId || terminal} value={event.motivo} onChange={(change) => setEvent((current) => ({ ...current, motivo: change.target.value }))} /></label>
      <label className="field checkbox-field"><input type="checkbox" disabled={!operationId || terminal} checked={event.divergencia} onChange={(change) => setEvent((current) => ({ ...current, divergencia: change.target.checked }))} /><span>Registrar divergência</span></label>
      <label className="field checkbox-field"><input type="checkbox" disabled={!operationId || terminal} checked={event.overrideAutorizado} onChange={(change) => setEvent((current) => ({ ...current, overrideAutorizado: change.target.checked }))} /><span>Override autorizado</span></label>
      <button type="submit" disabled={!operationId || terminal || busy}>{busy ? 'Registrando...' : 'Registrar evento de lacre'}</button>
    </form>

    {!seals.length ? <EmptyState title="Nenhum evento de lacre" description="Registre o lacre previsto ou a primeira conferência física." /> : <DataTable
      columns={[
        { key: 'ocorridoEm', label: 'Instante', render: (row) => dateTime(row.ocorridoEm) },
        { key: 'tipoEvento', label: 'Evento' },
        { key: 'numeroLacre', label: 'Lacre' },
        { key: 'numeroLacreSubstituido', label: 'Substituído' },
        { key: 'status', label: 'Estado', render: (row) => <StatusBadge status={row.status} /> },
        { key: 'operador', label: 'Operador' },
        { key: 'motivo', label: 'Motivo' }
      ]}
      rows={seals}
      rowKey={(row) => row.id || row.commandId}
    />}
  </Section>;
}
