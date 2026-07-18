import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { EmptyState, Loading, Message, StatusBadge } from '../components.jsx';
import { gateOperationsApi } from '../gateOperationsApi.js';
import './GateOperationsTabs.css';

const TABS = [
  { id: 'trocas', label: 'Troca de Cavalo' },
  { id: 'chamados', label: 'Chamados' },
  { id: 'filas', label: 'Filas' }
];

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

export function GateOperationsTabs() {
  const [tab, setTab] = useState('trocas');
  const [trocas, setTrocas] = useState([]);
  const [chamados, setChamados] = useState([]);
  const [fila, setFila] = useState([]);
  const [sentido, setSentido] = useState('ENTRADA');
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [trocaForm, setTrocaForm] = useState({ cpfMotorista: '', numeroCnh: '', cavaloAtual: '', gatePassId: '' });
  const [callForm, setCallForm] = useState({ gatePassId: '', prioridade: 'NORMAL' });
  const [queueForm, setQueueForm] = useState({ gatePassId: '' });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [trocaResponse, callResponse, queueResponse] = await Promise.all([
        gateOperationsApi.listarTrocas(),
        gateOperationsApi.listarChamados(),
        gateOperationsApi.listarFila(sentido)
      ]);
      setTrocas(Array.isArray(trocaResponse) ? trocaResponse : []);
      setChamados(Array.isArray(callResponse) ? callResponse : []);
      setFila(Array.isArray(queueResponse) ? queueResponse : []);
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar as operações complementares do gate.'));
    } finally {
      setLoading(false);
    }
  }, [sentido]);

  useEffect(() => { load(); }, [load]);

  const filteredTrocas = useMemo(() => {
    const query = filter.trim().toLowerCase();
    return !query ? trocas : trocas.filter((item) => `${item.cpfMotorista} ${item.numeroCnh} ${item.cavaloAtual} ${item.status}`.toLowerCase().includes(query));
  }, [filter, trocas]);

  async function execute(action, message) {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await action();
      setSuccess(message);
      await load();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setSaving(false);
    }
  }

  function submitTroca(event) {
    event.preventDefault();
    execute(() => gateOperationsApi.abrirTroca({ ...trocaForm, gatePassId: Number(trocaForm.gatePassId) }), 'Sessão de troca de cavalo aberta.');
  }

  function closeTroca(item) {
    const cavaloAtual = window.prompt('Informe a placa do cavalo na saída:', item.cavaloAtual);
    if (!cavaloAtual) return;
    execute(() => gateOperationsApi.encerrarTroca(item.cpfMotorista, { cavaloAtual, gatePassId: item.gateInId }), 'Sessão encerrada.');
  }

  function submitCall(event) {
    event.preventDefault();
    execute(() => gateOperationsApi.chamarVeiculo({
      gatePassId: Number(callForm.gatePassId),
      prioridade: callForm.prioridade,
      operador: readSession()?.nome
    }), 'Veículo chamado.');
  }

  function cancelCall(item) {
    const justificativa = window.prompt('Justificativa do cancelamento:');
    if (!justificativa) return;
    execute(() => gateOperationsApi.cancelarChamado(item.id, justificativa), 'Chamado cancelado.');
  }

  function submitQueue(event) {
    event.preventDefault();
    execute(() => gateOperationsApi.adicionarFila({ gatePassId: Number(queueForm.gatePassId), sentido }), 'GatePass incluído na fila.');
  }

  function prioritize(item) {
    const prioridade = window.prompt('Prioridade: NORMAL, ALTA ou EMERGENCIAL', item.prioridade);
    if (!prioridade) return;
    const justificativa = prioridade === 'NORMAL' ? '' : window.prompt('Justificativa da prioridade:');
    if (prioridade !== 'NORMAL' && !justificativa) return;
    execute(() => gateOperationsApi.alterarPrioridadeFila(item.id, {
      prioridade: prioridade.toUpperCase(), justificativa, operador: readSession()?.nome
    }), 'Prioridade atualizada.');
  }

  function reorder(item) {
    const posicao = Number(window.prompt('Nova posição:', item.posicaoAtual));
    if (!Number.isInteger(posicao) || posicao < 1) return;
    const justificativa = window.prompt('Justificativa da reordenação:');
    if (!justificativa) return;
    execute(() => gateOperationsApi.alterarPosicaoFila(item.id, { posicao, justificativa, operador: readSession()?.nome }), 'Fila reordenada.');
  }

  return <div className="gate-operations">
    <div className="gate-operation-tabs" role="tablist" aria-label="Operações complementares do gate">
      {TABS.map((item) => <button key={item.id} type="button" role="tab" aria-selected={tab === item.id} className={tab === item.id ? 'active' : ''} onClick={() => setTab(item.id)}>{item.label}</button>)}
    </div>
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {loading ? <Loading label="Carregando operações do gate..." /> : <>
      {tab === 'trocas' && <div className="gate-operation-panel">
        <form className="gate-operation-form" onSubmit={submitTroca}>
          <label>CPF<input required value={trocaForm.cpfMotorista} onChange={(event) => setTrocaForm({ ...trocaForm, cpfMotorista: event.target.value })} /></label>
          <label>CNH<input required value={trocaForm.numeroCnh} onChange={(event) => setTrocaForm({ ...trocaForm, numeroCnh: event.target.value })} /></label>
          <label>Placa do cavalo<input required value={trocaForm.cavaloAtual} onChange={(event) => setTrocaForm({ ...trocaForm, cavaloAtual: event.target.value.toUpperCase() })} /></label>
          <label>GatePass ID<input required min="1" type="number" value={trocaForm.gatePassId} onChange={(event) => setTrocaForm({ ...trocaForm, gatePassId: event.target.value })} /></label>
          <button disabled={saving} type="submit">Abrir sessão</button>
        </form>
        <label className="gate-operation-filter">Pesquisar<input value={filter} onChange={(event) => setFilter(event.target.value)} placeholder="CPF, CNH, placa ou status" /></label>
        {filteredTrocas.length ? <div className="gate-operation-table"><table><thead><tr><th>CPF</th><th>CNH</th><th>Cavalo</th><th>Status</th><th>Abertura</th><th>Ações</th></tr></thead><tbody>{filteredTrocas.map((item) => <tr key={item.id}><td>{item.cpfMotorista}</td><td>{item.numeroCnh}</td><td>{item.cavaloAtual}</td><td><StatusBadge value={item.status} /></td><td>{dateTime(item.abertaEm)}</td><td>{item.status === 'ABERTA' && <button type="button" className="secondary" onClick={() => closeTroca(item)}>Encerrar</button>}</td></tr>)}</tbody></table></div> : <EmptyState title="Nenhuma sessão encontrada" />}
      </div>}
      {tab === 'chamados' && <div className="gate-operation-panel">
        <form className="gate-operation-form" onSubmit={submitCall}>
          <label>GatePass ID<input required min="1" type="number" value={callForm.gatePassId} onChange={(event) => setCallForm({ ...callForm, gatePassId: event.target.value })} /></label>
          <label>Prioridade<select value={callForm.prioridade} onChange={(event) => setCallForm({ ...callForm, prioridade: event.target.value })}><option>NORMAL</option><option>ALTA</option><option>EMERGENCIAL</option></select></label>
          <button disabled={saving} type="submit">Chamar veículo</button>
        </form>
        {chamados.length ? <div className="gate-operation-table"><table><thead><tr><th>GatePass</th><th>Prioridade</th><th>Status</th><th>Chamado</th><th>Ações</th></tr></thead><tbody>{chamados.map((item) => <tr key={item.id}><td>{item.codigoGatePass || item.gatePassId}</td><td><StatusBadge value={item.prioridade} /></td><td><StatusBadge value={item.status} /></td><td>{dateTime(item.chamadoEm)}</td><td className="gate-operation-actions">{item.status === 'CHAMADO' && <button type="button" onClick={() => execute(() => gateOperationsApi.iniciarChamado(item.id), 'Atendimento iniciado.')}>Iniciar</button>}{item.status === 'EM_ATENDIMENTO' && <button type="button" onClick={() => execute(() => gateOperationsApi.finalizarChamado(item.id), 'Atendimento finalizado.')}>Finalizar</button>}{['CHAMADO', 'EM_ATENDIMENTO'].includes(item.status) && <button type="button" className="secondary" onClick={() => cancelCall(item)}>Cancelar</button>}</td></tr>)}</tbody></table></div> : <EmptyState title="Nenhum chamado registrado" />}
      </div>}
      {tab === 'filas' && <div className="gate-operation-panel">
        <form className="gate-operation-form" onSubmit={submitQueue}>
          <label>Sentido<select value={sentido} onChange={(event) => setSentido(event.target.value)}><option>ENTRADA</option><option>SAIDA</option></select></label>
          <label>GatePass ID<input required min="1" type="number" value={queueForm.gatePassId} onChange={(event) => setQueueForm({ gatePassId: event.target.value })} /></label>
          <button disabled={saving} type="submit">Adicionar à fila</button>
        </form>
        {fila.length ? <div className="gate-operation-table"><table><thead><tr><th>Posição</th><th>GatePass</th><th>Prioridade</th><th>Status</th><th>Entrada</th><th>Ações</th></tr></thead><tbody>{fila.map((item) => <tr key={item.id}><td>{item.posicaoAtual}<small>Original: {item.posicaoOriginal}</small></td><td>{item.codigoGatePass || item.gatePassId}</td><td><StatusBadge value={item.prioridade} /></td><td><StatusBadge value={item.status} /></td><td>{dateTime(item.entrouEm)}</td><td className="gate-operation-actions"><button type="button" className="secondary" onClick={() => prioritize(item)}>Prioridade</button><button type="button" className="secondary" onClick={() => reorder(item)}>Reordenar</button></td></tr>)}</tbody></table></div> : <EmptyState title="Fila vazia" />}
      </div>}
    </>}
  </div>;
}
