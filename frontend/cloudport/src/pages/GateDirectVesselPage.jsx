import { useState } from 'react';
import { formatError, hasAnyRole, request } from '../api.js';
import { JsonDetails, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';

export function GateDirectVesselPage({ session }) {
  const [agendamentoId, setAgendamentoId] = useState('');
  const [atribuicaoEstivaId, setAtribuicaoEstivaId] = useState('');
  const [horarioEmbarque, setHorarioEmbarque] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const authorized = hasAnyRole(session, 'ADMIN_PORTO', 'OPERADOR_GATE');

  async function submit(event) {
    event.preventDefault();
    if (!authorized || busy || !agendamentoId || !atribuicaoEstivaId) return;
    setBusy(true);
    setError('');
    setResult(null);
    try {
      const body = {
        agendamentoId: Number(agendamentoId),
        atribuicaoEstivaId: Number(atribuicaoEstivaId)
      };
      if (horarioEmbarque) body.horarioEmbarque = horarioEmbarque;
      setResult(await request('/gate/embarques-diretos/navio', { method: 'POST', body }));
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <PageHeader
      eyebrow="Gate + Navio"
      title="Embarque direto pelo cais"
      description="Finalize a carreta e confirme o contêiner no plano de estiva sem gerar posição, ordem ou permanência no pátio."
    />
    {!authorized && <Message type="error">Seu perfil não possui permissão para confirmar embarques diretos.</Message>}
    <Message type="error">{error}</Message>
    <Message type="success">{result ? 'Embarque direto confirmado e gate finalizado.' : ''}</Message>

    <Section title="Identificação operacional">
      <form className="upload-form" onSubmit={submit}>
        <label className="field">
          <span>ID do agendamento no gate</span>
          <input type="number" min="1" value={agendamentoId} onChange={(event) => setAgendamentoId(event.target.value)} required />
        </label>
        <label className="field">
          <span>ID da atribuição no plano de estiva</span>
          <input type="number" min="1" value={atribuicaoEstivaId} onChange={(event) => setAtribuicaoEstivaId(event.target.value)} required />
        </label>
        <label className="field">
          <span>Horário efetivo do embarque</span>
          <input type="datetime-local" value={horarioEmbarque} onChange={(event) => setHorarioEmbarque(event.target.value)} />
        </label>
        <button type="submit" disabled={!authorized || busy || !agendamentoId || !atribuicaoEstivaId}>
          {busy ? 'Confirmando...' : 'Confirmar gate → navio'}
        </button>
      </form>
    </Section>

    {result && <>
      <div className="metrics-grid">
        <MetricCard label="Contêiner" value={result.codigoConteiner} />
        <MetricCard label="Gate" value={<StatusBadge value={result.statusGate} />} />
        <MetricCard label="Posição no navio" value={`${result.baia}-${result.fileira}-${result.camada}`} />
        <MetricCard label="Passou pelo pátio" value={result.passouPeloPatio ? 'Sim' : 'Não'} />
      </div>
      <JsonDetails value={result} title="Confirmação operacional" />
    </>}
  </>;
}
