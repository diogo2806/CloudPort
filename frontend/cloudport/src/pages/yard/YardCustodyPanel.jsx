import { useMemo, useState } from 'react';
import { createCorrelationId, formatError, hasAnyRole, readSession, sanitizeText } from '../../api.js';
import { DataTable, EmptyState, Field, Loading, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { yardCustodyApi } from '../../yardCustodyApi.js';
import { displayValue, useRemote } from './YardShared.jsx';

function emptyCommand(operator = '') {
  return {
    codigoUnidade: '',
    area: '',
    posicao: '',
    equipamento: '',
    operador: operator,
    condicao: 'ÍNTEGRO',
    lacres: '',
    chaveIdempotencia: ''
  };
}

function CustodyFields({ value, onChange, disabled = false }) {
  function update(field, nextValue) {
    onChange({ ...value, [field]: nextValue });
  }

  return <div className="inline-form">
    <Field label="Unidade"><input value={value.codigoUnidade} onChange={(event) => update('codigoUnidade', event.target.value)} maxLength={40} disabled={disabled} required /></Field>
    <Field label="Exchange area"><input value={value.area} onChange={(event) => update('area', event.target.value)} maxLength={80} disabled={disabled} required /></Field>
    <Field label="Posição física"><input value={value.posicao} onChange={(event) => update('posicao', event.target.value)} maxLength={80} disabled={disabled} required /></Field>
    <Field label="Equipamento"><input value={value.equipamento} onChange={(event) => update('equipamento', event.target.value)} maxLength={80} disabled={disabled} required /></Field>
    <Field label="Operador"><input value={value.operador} onChange={(event) => update('operador', event.target.value)} maxLength={120} disabled={disabled} required /></Field>
    <Field label="Condição"><input value={value.condicao} onChange={(event) => update('condicao', event.target.value)} maxLength={120} disabled={disabled} required /></Field>
    <Field label="Lacres" hint="Separe múltiplos lacres por vírgula."><input value={value.lacres} onChange={(event) => update('lacres', event.target.value)} maxLength={500} disabled={disabled} required /></Field>
  </div>;
}

function ManualCustodia() {
  return <details className="json-details">
    <summary>Manual operacional do handoff de custódia</summary>
    <div className="content-card">
      <h3>Finalidade da tela</h3>
      <p>Registrar a entrega e o recebimento bilateral de uma unidade em exchange area, impedindo dupla transferência e bloqueando divergências físicas.</p>
      <h3>Fluxo operacional</h3>
      <ol>
        <li>O operador de origem registra unidade, área, posição, equipamento, condição e lacres.</li>
        <li>A custódia fica no estado ENTREGUE, aguardando conferência independente.</li>
        <li>O operador de destino seleciona o registro e repete a leitura física.</li>
        <li>Dados equivalentes concluem a transferência como RECEBIDA.</li>
        <li>Unidade, área, posição, condição ou lacres divergentes geram bloqueio DIVERGENTE.</li>
      </ol>
      <h3>Campos</h3>
      <ul>
        <li>Unidade: identificação física do contêiner ou unidade de carga.</li>
        <li>Exchange area e posição: local exato da transferência.</li>
        <li>Equipamento e operador: responsáveis por cada lado do handoff.</li>
        <li>Condição: estado físico conferido na entrega e no recebimento.</li>
        <li>Lacres: conjunto normalizado e comparado sem depender da ordem informada.</li>
      </ul>
      <h3>Permissões necessárias</h3>
      <ul><li>ADMIN_PORTO e OPERADOR_PATIO podem entregar e receber.</li><li>PLANEJADOR pode consultar o painel.</li></ul>
      <h3>Estados possíveis</h3>
      <ul><li>ENTREGUE: aguardando a segunda confirmação.</li><li>RECEBIDA: custódia transferida uma única vez.</li><li>DIVERGENTE: conferência incompatível e operação bloqueada.</li></ul>
      <h3>Motivos de bloqueio</h3>
      <ul><li>Outra custódia ativa para a unidade.</li><li>Registro já recebido ou já divergente.</li><li>Chave idempotente reutilizada com conteúdo diferente.</li><li>Divergência de unidade, área, posição, condição ou lacres.</li><li>Perfil sem permissão operacional.</li></ul>
      <h3>Exemplo</h3>
      <p>A unidade CONT-001 é entregue na EA-01/P-03 com os lacres L10 e L20. O recebimento informa os mesmos dados, ainda que os lacres sejam digitados em outra ordem, e a custódia passa para RECEBIDA.</p>
      <h3>Atalhos</h3>
      <ul><li>F1 ou Shift + ?: abrir a ajuda contextual.</li><li>Esc: fechar a ajuda.</li><li>Atualizar: recarregar o estado persistido.</li></ul>
      <p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/patio-handoff-custodia-exchange-area.md" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
    </div>
  </details>;
}

export function YardCustodyPanel() {
  const session = readSession() ?? {};
  const canOperate = hasAnyRole(session, 'ADMIN_PORTO', 'OPERADOR_PATIO');
  const remote = useRemote(() => yardCustodyApi.listar(), []);
  const [delivery, setDelivery] = useState(() => emptyCommand(session.nome));
  const [receipt, setReceipt] = useState(() => emptyCommand(session.nome));
  const [selectedId, setSelectedId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const rows = useMemo(() => remote.data ?? [], [remote.data]);
  const selected = rows.find((row) => row.id === selectedId) ?? null;
  const counts = useMemo(() => ({
    entregue: rows.filter((row) => row.status === 'ENTREGUE').length,
    recebida: rows.filter((row) => row.status === 'RECEBIDA').length,
    divergente: rows.filter((row) => row.status === 'DIVERGENTE').length,
    bloqueada: rows.filter((row) => row.bloqueada).length
  }), [rows]);

  function selectCustody(row) {
    setSelectedId(row.id);
    setReceipt({
      codigoUnidade: row.codigoUnidade ?? '',
      area: row.area ?? '',
      posicao: row.posicao ?? '',
      equipamento: '',
      operador: session.nome ?? '',
      condicao: row.condicaoEntrega ?? '',
      lacres: row.lacresEntrega ?? '',
      chaveIdempotencia: ''
    });
    setError('');
    setSuccess('');
  }

  async function submitDelivery(event) {
    event.preventDefault();
    if (!canOperate || busy) return;
    const payload = { ...delivery, chaveIdempotencia: delivery.chaveIdempotencia || createCorrelationId() };
    setDelivery(payload);
    setBusy(true); setError(''); setSuccess('');
    try {
      await yardCustodyApi.entregar(payload);
      setDelivery(emptyCommand(session.nome));
      setSuccess('Entrega de custódia registrada e aguardando recebimento.');
      await remote.reload();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function submitReceipt(event) {
    event.preventDefault();
    if (!canOperate || busy || !selected || selected.status !== 'ENTREGUE') return;
    const payload = { ...receipt, chaveIdempotencia: receipt.chaveIdempotencia || createCorrelationId() };
    setReceipt(payload);
    setBusy(true); setError(''); setSuccess('');
    try {
      const response = await yardCustodyApi.receber(selected.id, payload);
      setSuccess(response.status === 'DIVERGENTE'
        ? `Recebimento bloqueado: ${sanitizeText(response.motivoDivergencia)}`
        : 'Recebimento confirmado e custódia transferida.');
      await remote.reload();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <Message type="error">{remote.error || error}</Message>
    <Message type="success">{success}</Message>
    {!canOperate && <Message type="warning">Seu perfil pode consultar as custódias, mas não registrar entrega ou recebimento.</Message>}
    <div className="metrics-grid">
      <MetricCard label="Aguardando recebimento" value={counts.entregue} />
      <MetricCard label="Recebidas" value={counts.recebida} />
      <MetricCard label="Divergentes" value={counts.divergente} />
      <MetricCard label="Bloqueadas" value={counts.bloqueada} />
    </div>
    <ManualCustodia />
    <div className="split-grid">
      <Section title="Entregar na exchange area" description="A primeira confirmação cria uma custódia ativa e idempotente.">
        <form onSubmit={submitDelivery}>
          <CustodyFields value={delivery} onChange={setDelivery} disabled={!canOperate || busy} />
          <div className="actions"><button type="submit" disabled={!canOperate || busy}>{busy ? 'Registrando...' : 'Registrar entrega'}</button></div>
        </form>
      </Section>
      <Section title="Receber da exchange area" description="Selecione uma entrega e repita a conferência física.">
        {!selected ? <EmptyState title="Selecione uma custódia entregue" /> : <>
          <p>Registro <strong>#{selected.id}</strong> · <StatusBadge value={selected.status} /></p>
          <form onSubmit={submitReceipt}>
            <CustodyFields value={receipt} onChange={setReceipt} disabled={!canOperate || busy || selected.status !== 'ENTREGUE'} />
            <div className="actions"><button type="submit" disabled={!canOperate || busy || selected.status !== 'ENTREGUE'}>{busy ? 'Conferindo...' : 'Confirmar recebimento'}</button></div>
          </form>
        </>}
      </Section>
    </div>
    <Section title="Custódias registradas" actions={<button className="secondary" type="button" onClick={remote.reload}>Atualizar</button>}>
      {remote.loading ? <Loading label="Carregando handoffs de custódia..." /> : <DataTable rows={rows} rowKey="id" columns={[
        { key: 'codigoUnidade', label: 'Unidade' },
        { key: 'area', label: 'Área' },
        { key: 'posicao', label: 'Posição' },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'operadorEntrega', label: 'Entregue por' },
        { key: 'entregueEm', label: 'Entrega', render: (row) => displayValue(row.entregueEm) },
        { key: 'operadorRecebimento', label: 'Recebido por' },
        { key: 'motivoDivergencia', label: 'Bloqueio' },
        { key: 'actions', label: 'Ações', render: (row) => <button className="small secondary" type="button" onClick={() => selectCustody(row)}>{row.status === 'ENTREGUE' ? 'Receber' : 'Inspecionar'}</button> }
      ]} emptyTitle="Nenhuma custódia registrada" />}
    </Section>
  </>;
}
