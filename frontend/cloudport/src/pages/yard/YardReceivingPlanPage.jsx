import { useMemo, useState } from 'react';
import { formatError, request, sanitizeText } from '../../api.js';
import { DataTable, EmptyState, JsonDetails, Message, MetricCard, Section, StatusBadge } from '../../components.jsx';
import { displayValue, YardPageHeader } from './YardShared.jsx';

const SAMPLE_CONTAINERS = [
  {
    id: 1001,
    codigo: 'MSCU1000001',
    etaChegada: '2026-07-18T09:10:00',
    etaPartida: '2026-07-20T18:00:00',
    categoria: 'EXPORTACAO',
    armador: 'MSC',
    visitaSaida: 'MSC-2026-0718',
    destino: 'BERCO_1',
    comprimentoPes: 40,
    tipoEquipamento: 'DRY',
    estadoCarga: 'CHEIO',
    pesoToneladas: 24
  },
  {
    id: 1002,
    codigo: 'MSCU1000002',
    etaChegada: '2026-07-18T10:30:00',
    etaPartida: '2026-07-20T18:00:00',
    categoria: 'EXPORTACAO',
    armador: 'MSC',
    visitaSaida: 'MSC-2026-0718',
    destino: 'BERCO_1',
    comprimentoPes: 40,
    tipoEquipamento: 'DRY',
    estadoCarga: 'CHEIO',
    pesoToneladas: 26
  },
  {
    id: 1003,
    codigo: 'REFU1000001',
    etaChegada: '2026-07-18T10:50:00',
    etaPartida: '2026-07-20T18:00:00',
    categoria: 'EXPORTACAO',
    armador: 'MSC',
    visitaSaida: 'MSC-2026-0718',
    destino: 'BERCO_1',
    comprimentoPes: 40,
    tipoEquipamento: 'REEFER',
    estadoCarga: 'CHEIO',
    refrigerado: true,
    pesoToneladas: 25
  }
];

function prettyJson(value) {
  return JSON.stringify(value, null, 2);
}

export function YardReceivingPlanPage({ navigate }) {
  const [source, setSource] = useState(() => prettyJson(SAMPLE_CONTAINERS));
  const [plan, setPlan] = useState(null);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const groups = plan?.grupos ?? [];
  const selectedContainers = selectedGroup?.conteineres ?? [];
  const alerts = useMemo(() => [
    ...(plan?.alertas ?? []).map((message) => ({ scope: 'PLANO', message })),
    ...(selectedGroup?.alertas ?? []).map((message) => ({ scope: 'GRUPO', message }))
  ], [plan, selectedGroup]);

  async function generatePlan() {
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      const containers = JSON.parse(source);
      if (!Array.isArray(containers)) throw new Error('Informe um array JSON de contêineres.');
      const response = await request('/yard/patio/planejamento-recebimento', {
        method: 'POST',
        body: containers
      });
      setPlan(response);
      setSelectedGroup(response?.grupos?.[0] ?? null);
    } catch (reason) {
      setPlan(null);
      setSelectedGroup(null);
      setError(reason instanceof SyntaxError
        ? 'O JSON informado é inválido.'
        : formatError(reason, 'Não foi possível gerar o plano de recebimento.'));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <YardPageHeader
      path="/home/patio/planejamento-recebimento"
      navigate={navigate}
      title="Planejamento de recebimento"
      description="Agrupa contêineres por compatibilidade operacional e janela de chegada antes da reserva das posições do pátio."
      actions={<button type="button" disabled={busy} onClick={generatePlan}>{busy ? 'Planejando...' : 'Gerar agrupamento'}</button>}
    />
    <Message type="error">{error}</Message>
    <div className="split-grid">
      <Section title="Contêineres previstos" description="Cole o pré-aviso em JSON. O backend valida duplicidades e monta os grupos de alocação.">
        <label className="field">
          <span>Lista JSON</span>
          <textarea
            value={source}
            onChange={(event) => setSource(event.target.value)}
            rows={24}
            spellCheck="false"
            aria-label="Lista JSON de contêineres previstos"
          />
        </label>
      </Section>
      <Section title="Critérios aplicados" description="Contêineres incompatíveis não são misturados no mesmo grupo.">
        <div className="detail-grid">
          <div className="detail-row"><span>Fluxo</span><strong>Categoria, armador, visita e destino</strong></div>
          <div className="detail-row"><span>Equipamento</span><strong>Comprimento ISO, tipo e estado da carga</strong></div>
          <div className="detail-row"><span>Segregação</span><strong>Reefer, perigoso e classe IMO</strong></div>
          <div className="detail-row"><span>Empilhamento</span><strong>Faixa de peso</strong></div>
          <div className="detail-row"><span>Recebimento</span><strong>Janelas de quatro horas</strong></div>
        </div>
      </Section>
    </div>

    {plan && <>
      <div className="metrics-grid">
        <MetricCard label="Contêineres" value={plan.totalConteineres ?? 0} />
        <MetricCard label="Grupos" value={plan.totalGrupos ?? 0} />
        <MetricCard label="Capacidade" value={`${plan.totalTeus ?? 0} TEU`} />
        <MetricCard label="Peso previsto" value={`${Number(plan.pesoTotalToneladas ?? 0).toFixed(3)} t`} />
      </div>
      <Section title="Grupos para recebimento" description="A prioridade menor deve ser tratada primeiro pelo planejador.">
        <DataTable
          rows={groups}
          rowKey="chaveAgrupamento"
          onRowClick={setSelectedGroup}
          columns={[
            { key: 'prioridade', label: 'Prioridade' },
            { key: 'nome', label: 'Grupo', render: (row) => sanitizeText(row.nome) },
            { key: 'inicioJanelaRecebimento', label: 'Início', render: (row) => displayValue(row.inicioJanelaRecebimento) },
            { key: 'fimJanelaRecebimento', label: 'Fim', render: (row) => displayValue(row.fimJanelaRecebimento) },
            { key: 'quantidadeConteineres', label: 'Contêineres' },
            { key: 'teus', label: 'TEU' },
            { key: 'perigoso', label: 'Segregação', render: (row) => <StatusBadge value={row.perigoso ? `IMO_${row.classeImo}` : row.refrigerado ? 'REEFER' : 'PADRAO'} /> }
          ]}
          emptyTitle="Nenhum grupo foi gerado"
        />
      </Section>
      <div className="split-grid">
        <Section title="Contêineres do grupo selecionado">
          {!selectedGroup ? <EmptyState title="Selecione um grupo" /> : <DataTable
            rows={selectedContainers}
            rowKey="id"
            columns={[
              { key: 'codigo', label: 'Contêiner' },
              { key: 'etaChegada', label: 'Chegada', render: (row) => displayValue(row.etaChegada ?? row.etaPartida) },
              { key: 'comprimentoPes', label: 'Pés' },
              { key: 'tipoEquipamento', label: 'Tipo' },
              { key: 'pesoToneladas', label: 'Peso (t)' },
              { key: 'destino', label: 'Destino' }
            ]}
            emptyTitle="Grupo sem contêineres"
          />}
        </Section>
        <Section title="Alertas e contrato calculado">
          {!alerts.length ? <Message type="success">Nenhuma pendência crítica identificada.</Message> : <div className="card-list">
            {alerts.map((alert, index) => <article className="content-card" key={`${alert.scope}-${index}`}>
              <div className="card-meta"><StatusBadge value={alert.scope} /></div>
              <p>{sanitizeText(alert.message)}</p>
            </article>)}
          </div>}
          <JsonDetails value={selectedGroup ?? plan} title="Resultado completo do backend" />
        </Section>
      </div>
    </>}
  </>;
}
