import { useCallback, useEffect, useState } from 'react';
import { formatError } from '../api.js';
import { railApi } from '../railApi.js';
import { DataTable, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR');
}

export function RailVisitsPage({ navigate }) {
  const [days, setDays] = useState(7);
  const [visits, setVisits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await railApi.listarVisitas(days);
      setVisits(Array.isArray(response) ? response : []);
    } catch (reason) {
      setVisits([]);
      setError(formatError(reason, 'Não foi possível carregar as visitas ferroviárias.'));
    } finally {
      setLoading(false);
    }
  }, [days]);

  useEffect(() => { load(); }, [load]);

  return <>
    <PageHeader
      eyebrow="Ferrovia"
      title="Visitas de trem"
      description="Visitas previstas, composição do trem e volumes planejados de carga e descarga."
      actions={<div className="inline">
        <label className="compact-field">Janela
          <select value={days} onChange={(event) => setDays(Number(event.target.value))}>
            <option value="1">1 dia</option><option value="7">7 dias</option><option value="15">15 dias</option><option value="30">30 dias</option>
          </select>
        </label>
        <button className="secondary" onClick={load}>Atualizar</button>
        <button onClick={() => navigate('/home/ferrovia/lista-trabalho')}>Abrir lista de trabalho</button>
      </div>}
    />
    <Message type="error" onClose={() => setError('')}>{error}</Message>
    <Section title="Visitas">
      {loading ? <Loading label="Carregando visitas..." /> : <DataTable
        rows={visits}
        rowKey="id"
        emptyTitle="Nenhuma visita prevista"
        columns={[
          { key: 'identificadorTrem', label: 'Trem' },
          { key: 'operadoraFerroviaria', label: 'Operadora' },
          { key: 'statusVisita', label: 'Status', render: (row) => <StatusBadge value={row.statusVisita} /> },
          { key: 'horaChegadaPrevista', label: 'Chegada', render: (row) => formatDateTime(row.horaChegadaPrevista) },
          { key: 'horaPartidaPrevista', label: 'Partida', render: (row) => formatDateTime(row.horaPartidaPrevista) },
          { key: 'quantidadeVagoes', label: 'Vagões', render: (row) => row.quantidadeVagoes ?? row.listaVagoes?.length ?? 0 },
          { key: 'quantidadeDescarga', label: 'Descarga', render: (row) => `${row.quantidadeDescarga ?? row.listaDescarga?.length ?? 0} contêiner(es)` },
          { key: 'quantidadeCarga', label: 'Carga', render: (row) => `${row.quantidadeCarga ?? row.listaCarga?.length ?? 0} contêiner(es)` }
        ]}
      />}
    </Section>
  </>;
}
