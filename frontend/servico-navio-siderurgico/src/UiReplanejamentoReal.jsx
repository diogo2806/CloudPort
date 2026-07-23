import { useEffect, useMemo, useState } from 'react';
import { formatError } from './api.js';
import { replanejamentoRealApi } from './replanejamentoRealApi.js';

function number(value, digits = 2) {
  return Number(value ?? 0).toLocaleString('pt-BR', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
}

function position(line, column, layer) {
  return [line, column, layer].filter((value) => value !== null && value !== undefined && value !== '').join('-') || 'não informada';
}

export default function UiReplanejamentoReal({ visitId, onApplied }) {
  const [proposal, setProposal] = useState(null);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [reason, setReason] = useState('Replanejamento operacional com otimização real');
  const [rehandleLimit, setRehandleLimit] = useState('');

  useEffect(() => {
    setProposal(null);
    setError('');
    setSuccess('');
  }, [visitId]);

  const blockingAlerts = proposal?.alertasImpeditivos ?? [];
  const assignments = proposal?.atribuicoesPropostas ?? [];
  const canApply = Boolean(proposal?.planoOtimizacaoId)
    && assignments.length > 0
    && blockingAlerts.length === 0
    && !busy;

  const memory = useMemo(
    () => Object.entries(proposal?.memoriaCalculo ?? {}).sort(([left], [right]) => left.localeCompare(right)),
    [proposal]
  );

  function options() {
    const parsedLimit = rehandleLimit === '' ? null : Number(rehandleLimit);
    return {
      limiteRehandleAceitavel: Number.isInteger(parsedLimit) && parsedLimit >= 0 ? parsedLimit : null,
      pesosCriterios: {}
    };
  }

  async function simulate() {
    setBusy('simulate');
    setError('');
    setSuccess('');
    try {
      const result = await replanejamentoRealApi.simular(visitId, reason, options());
      setProposal(result);
      setSuccess('Proposta calculada. Nenhuma posição foi alterada.');
    } catch (cause) {
      setError(formatError(cause));
    } finally {
      setBusy('');
    }
  }

  async function apply() {
    if (!canApply) return;
    const confirmed = typeof window === 'undefined'
      || typeof window.confirm !== 'function'
      || window.confirm(`Aplicar o plano ${proposal.planoOtimizacaoId} após nova validação transacional?`);
    if (!confirmed) return;
    setBusy('apply');
    setError('');
    setSuccess('');
    try {
      const result = await replanejamentoRealApi.aplicar(visitId, reason, options());
      setProposal(result);
      setSuccess('Plano aplicado após revalidação das posições, reservas e ordens reais.');
      await onApplied?.(result);
    } catch (cause) {
      setError(formatError(cause));
    } finally {
      setBusy('');
    }
  }

  function rejectProposal() {
    if (!proposal || busy) return;
    const confirmed = typeof window === 'undefined'
      || typeof window.confirm !== 'function'
      || window.confirm('Rejeitar e descartar esta sugestão de replanejamento?');
    if (!confirmed) return;
    setProposal(null);
    setError('');
    setSuccess('Sugestão rejeitada. Nenhuma posição foi alterada.');
  }

  return <section className="panel">
    <div className="section-head">
      <div>
        <span className="eyebrow">Navio + Yard + CHE</span>
        <h2>Replanejamento com otimização real</h2>
      </div>
      <div className="actions">
        <details>
          <summary aria-label="Abrir manual da tela" title="Manual da tela">?</summary>
          <div className="editor-block" role="note">
            <h3>Manual da tela</h3>
            <p><strong>Finalidade:</strong> simular, revisar, aprovar ou rejeitar sugestões de replanejamento do pátio sem alterar posições durante a simulação.</p>
            <p><strong>Fluxo operacional:</strong> informe o motivo e o limite de rehandles, simule, revise alertas e posições propostas e então aplique ou rejeite a sugestão.</p>
            <p><strong>Campos:</strong> Motivo registra a justificativa operacional. Limite de rehandles define o máximo aceitável para a proposta.</p>
            <p><strong>Permissões:</strong> exige sessão autenticada e permissão de replanejamento da visita de navio.</p>
            <p><strong>Estados:</strong> sem proposta, calculando, proposta disponível, bloqueada, aplicando, aplicada ou rejeitada.</p>
            <p><strong>Motivos de bloqueio:</strong> alertas impeditivos, ausência de posições elegíveis, conflito de reserva, equipamento indisponível ou proposta sem atribuições.</p>
            <p><strong>Exemplo:</strong> simular uma nova distribuição após alteração de ETA e aplicar somente quando não houver alertas impeditivos.</p>
            <p><strong>Atalhos:</strong> Tab navega pelos controles; Enter ativa o botão em foco; Esc fecha a confirmação do navegador.</p>
            <p><strong>Processo completo:</strong> consulte o manual operacional de Yard Planning na documentação do sistema.</p>
          </div>
        </details>
        {proposal?.assinaturaEntrada && <small>Assinatura {proposal.assinaturaEntrada}</small>}
      </div>
    </div>

    {error && <div className="message error" role="alert">{error}</div>}
    {success && <div className="message success" role="status">{success}</div>}

    <div className="filters">
      <label className="span-2">Motivo
        <input value={reason} maxLength="500" onChange={(event) => setReason(event.target.value)} />
      </label>
      <label>Limite de rehandles
        <input type="number" min="0" value={rehandleLimit} onChange={(event) => setRehandleLimit(event.target.value)} />
      </label>
    </div>
    <div className="actions">
      <button type="button" className="secondary" disabled={!visitId || !reason.trim() || Boolean(busy)} onClick={simulate}>
        {busy === 'simulate' ? 'Calculando...' : 'Simular replanejamento real'}
      </button>
      <button type="button" className="warning" disabled={!canApply} onClick={apply}>
        {busy === 'apply' ? 'Aplicando...' : 'Aprovar e aplicar plano'}
      </button>
      <button type="button" className="secondary" disabled={!proposal || Boolean(busy)} onClick={rejectProposal}>
        Rejeitar sugestão
      </button>
    </div>

    {!proposal ? <p className="empty">Simule para comparar o plano atual com a proposta antes de confirmar alterações.</p> : <>
      <section className="metrics">
        <div className="metric"><span>Economia</span><strong>{number(proposal.economiaEstimadaDistanciaPercentual)}%</strong><small>distância estimada</small></div>
        <div className="metric"><span>Distância</span><strong>{proposal.distanciaOriginal ?? 0} → {proposal.distanciaOtimizada ?? 0}</strong><small>atual versus proposta</small></div>
        <div className="metric"><span>Risco</span><strong>{proposal.riscoRehandle || 'INDEFINIDO'}</strong><small>{proposal.itensNaoReplanejados?.length ?? 0} itens não replanejados</small></div>
        <div className="metric"><span>Pontuação</span><strong>{number(proposal.pontuacaoTotal, 3)}</strong><small>menor é melhor</small></div>
      </section>

      {blockingAlerts.length > 0 && <div className="message error" role="alert">
        <strong>A aplicação está bloqueada por {blockingAlerts.length} alerta(s).</strong>
        {blockingAlerts.map((alert) => <p key={alert}>{alert}</p>)}
      </div>}

      <div className="table-wrap"><table>
        <thead><tr><th>Seq.</th><th>Carga</th><th>Movimento</th><th>Atual</th><th>Proposta</th><th>Bloco</th><th>CHE</th><th>Score</th><th>Rehandles</th></tr></thead>
        <tbody>{assignments.map((item) => <tr key={`${item.codigoCarga}-${item.sequenciaPlano}`}>
          <td>{item.sequenciaPlano}</td>
          <td><strong>{item.codigoCarga}</strong></td>
          <td>{item.movimento}</td>
          <td>{position(item.linhaOriginal, item.colunaOriginal, item.camadaOriginal)}</td>
          <td>{position(item.linhaProposta, item.colunaProposta, item.camadaProposta)}</td>
          <td>{item.blocoProposto || '—'}</td>
          <td>{item.equipamento || '—'}</td>
          <td>{number(item.scoreTotal, 3)}</td>
          <td>{item.rehandlesEstimados ?? 0}</td>
        </tr>)}</tbody>
      </table></div>

      <div className="columns">
        <section className="editor-block">
          <h3>Memória de cálculo</h3>
          {!memory.length ? <p className="empty">Sem memória de cálculo.</p> : <div className="list">
            {memory.map(([key, value]) => <article key={key}><div><strong>{key}</strong><span>{number(value, 3)}</span></div></article>)}
          </div>}
        </section>
        <section className="editor-block">
          <h3>Justificativas</h3>
          <div className="list">{(proposal.justificativas ?? []).map((text, index) => <article key={`${index}-${text}`}><p>{text}</p></article>)}</div>
        </section>
      </div>
    </>}
  </section>;
}
