import { useMemo, useState } from 'react';
import { readSession, sanitizeText } from '../../api.js';
import { DataTable, EmptyState, Loading, Message, Section, StatusBadge } from '../../components.jsx';
import { yardPredictiveApi } from '../../yardPredictiveApi.js';
import { CommandPanel, displayValue, FilterField, useCommand, useRemote } from './YardShared.jsx';

const TRANSITIONS = {
  TENTATIVO: ['DEFINITIVO', 'CANCELADO'],
  DEFINITIVO: ['IMINENTE', 'CANCELADO'],
  IMINENTE: ['CANCELADO']
};

function remaining(value) {
  const seconds = Number(value ?? 0);
  if (seconds <= 0) return 'Vencido';
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${hours}h ${minutes}min`;
}

export function PredictivePositionPlansPanel() {
  const [estado, setEstado] = useState('');
  const [bloco, setBloco] = useState('');
  const [selected, setSelected] = useState(null);
  const [history, setHistory] = useState([]);
  const [historyError, setHistoryError] = useState('');
  const remote = useRemote(() => yardPredictiveApi.listarPlanos({ estado, bloco }), [estado, bloco]);
  const command = useCommand(remote);
  const plans = Array.isArray(remote.data) ? remote.data : [];
  const session = readSession() ?? {};

  const actions = useMemo(() => TRANSITIONS[selected?.estado] ?? [], [selected]);

  async function selectPlan(plan) {
    setSelected(plan);
    setHistoryError('');
    try {
      setHistory(await yardPredictiveApi.listarHistorico(plan.id));
    } catch (reason) {
      setHistory([]);
      setHistoryError(reason?.message ?? 'Não foi possível carregar a auditoria do plano.');
    }
  }

  function scheduleTransition(target) {
    command.setCommand({
      title: `Converter plano para ${target}`,
      description: `A alteração será persistida e auditada para a unidade ${selected.codigoContainer}.`,
      success: `Plano convertido para ${target}.`,
      run: (reason) => yardPredictiveApi.alterarEstado(selected.id, target, reason, session.nome)
    });
  }

  return <Section
    title="Planos de posição preditivos"
    description="Posições tentativas possuem validade. O dispatch converte uma posição tentativa somente após revalidar destino e expiração na mesma transação."
    actions={<button className="secondary" type="button" onClick={remote.reload}>Atualizar</button>}
  >
    <Message type="error">{remote.error || command.error || historyError}</Message>
    <Message type="success">{command.success}</Message>
    <div className="filter-bar">
      <FilterField label="Estado">
        <select value={estado} onChange={(event) => setEstado(event.target.value)}>
          <option value="">Todos</option>
          <option value="TENTATIVO">Tentativo</option>
          <option value="DEFINITIVO">Definitivo</option>
          <option value="IMINENTE">Iminente</option>
          <option value="EXPIRADO">Expirado</option>
          <option value="CANCELADO">Cancelado</option>
        </select>
      </FilterField>
      <FilterField label="Bloco">
        <input value={bloco} onChange={(event) => setBloco(event.target.value)} maxLength={40} placeholder="Ex.: A01" />
      </FilterField>
    </div>
    {remote.loading ? <Loading label="Carregando planos preditivos..." /> : <DataTable
      rows={plans}
      rowKey="id"
      onRowClick={selectPlan}
      columns={[
        { key: 'codigoContainer', label: 'Unidade' },
        { key: 'bloco', label: 'Bloco' },
        { key: 'posicao', label: 'Posição', render: (row) => `${row.linha ?? '—'} / ${row.coluna ?? '—'} / ${sanitizeText(row.camada) || '—'}` },
        { key: 'estado', label: 'Estado', render: (row) => <StatusBadge value={row.estado} /> },
        { key: 'equipamentoId', label: 'CHE' },
        { key: 'validoAte', label: 'Válido até', render: (row) => displayValue(row.validoAte) },
        { key: 'segundosAteExpiracao', label: 'Tempo restante', render: (row) => remaining(row.segundosAteExpiracao) },
        { key: 'origem', label: 'Origem' },
        { key: 'versao', label: 'Versão' }
      ]}
      emptyTitle="Nenhum plano preditivo encontrado"
    />}

    {selected && <div className="split-grid">
      <Section title={`Plano ${selected.codigoContainer}`} description={sanitizeText(selected.motivo)} actions={<div className="actions">
        {actions.map((target) => <button key={target} type="button" className={target === 'CANCELADO' ? 'secondary' : ''} onClick={() => scheduleTransition(target)}>{target}</button>)}
      </div>}>
        <div className="detail-grid">
          <div className="detail-row"><span>Horizonte</span><strong>{displayValue(selected.horizonteInicio)} até {displayValue(selected.horizonteFim)}</strong></div>
          <div className="detail-row"><span>Validade</span><strong>{displayValue(selected.validoAte)}</strong></div>
          <div className="detail-row"><span>Alterado por</span><strong>{sanitizeText(selected.alteradoPor) || '—'}</strong></div>
          <div className="detail-row"><span>Assinatura da entrada</span><strong>{sanitizeText(selected.assinaturaEntrada) || '—'}</strong></div>
        </div>
      </Section>
      <Section title="Auditoria das conversões">
        {!history.length ? <EmptyState title="Sem conversões registradas" /> : <DataTable
          rows={history}
          rowKey="id"
          columns={[
            { key: 'ocorridoEm', label: 'Data', render: (row) => displayValue(row.ocorridoEm) },
            { key: 'estadoAnterior', label: 'Anterior', render: (row) => <StatusBadge value={row.estadoAnterior ?? 'CRIADO'} /> },
            { key: 'estadoNovo', label: 'Novo', render: (row) => <StatusBadge value={row.estadoNovo} /> },
            { key: 'operador', label: 'Operador' },
            { key: 'motivo', label: 'Motivo' },
            { key: 'versaoPlano', label: 'Versão' }
          ]}
        />}
      </Section>
    </div>}
    <CommandPanel command={command.command} busy={command.busy} onCancel={() => command.setCommand(null)} onConfirm={command.confirm} />
  </Section>;
}
