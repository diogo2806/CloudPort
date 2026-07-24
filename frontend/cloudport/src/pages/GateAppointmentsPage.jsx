import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole, normalizePage } from '../api.js';
import { fleetApi } from '../fleetApi.js';
import { gateAppointmentsApi } from '../gateAppointmentsApi.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY_FORM = {
  codigo: '',
  tipoOperacao: 'ENTRADA',
  status: 'PENDENTE',
  transportadoraId: '',
  motoristaId: '',
  veiculoId: '',
  janelaAtendimentoId: '',
  horarioPrevistoChegada: '',
  horarioPrevistoSaida: '',
  observacoes: ''
};

function vehicleLabel(vehicle) {
  const trailer = vehicle.placaCarreta ? ` · carreta ${vehicle.placaCarreta}` : '';
  const model = vehicle.modelo || vehicle.tipo ? ` · ${vehicle.modelo || vehicle.tipo}` : '';
  return `${vehicle.placa}${trailer}${model}`;
}

function windowLabel(window) {
  return `${window.data ?? 'Data não informada'} · ${window.horaInicio ?? '--:--'}–${window.horaFim ?? '--:--'}`;
}

export function GateAppointmentsPage({ session }) {
  const [rows, setRows] = useState([]);
  const [carriers, setCarriers] = useState([]);
  const [windows, setWindows] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [vehiclesLoading, setVehiclesLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const canCreate = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [appointmentsResponse, windowsResponse, carriersResponse] = await Promise.all([
        gateAppointmentsApi.listar(),
        gateAppointmentsApi.listarJanelas(),
        canCreate ? gateAppointmentsApi.listarTransportadoras() : Promise.resolve([])
      ]);
      setRows(normalizePage(appointmentsResponse));
      setWindows(normalizePage(windowsResponse));
      setCarriers(Array.isArray(carriersResponse) ? carriersResponse : []);
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar os agendamentos.'));
    } finally {
      setLoading(false);
    }
  }, [canCreate]);

  useEffect(() => { reload(); }, [reload]);

  useEffect(() => {
    if (!form.transportadoraId) {
      setVehicles([]);
      setForm((current) => ({ ...current, veiculoId: '' }));
      return;
    }
    let active = true;
    setVehiclesLoading(true);
    fleetApi.listarElegiveis(Number(form.transportadoraId))
      .then((response) => {
        if (!active) return;
        setVehicles(Array.isArray(response) ? response : []);
        setForm((current) => ({
          ...current,
          veiculoId: response?.some((vehicle) => String(vehicle.id) === String(current.veiculoId)) ? current.veiculoId : ''
        }));
      })
      .catch((reason) => {
        if (active) {
          setVehicles([]);
          setError(formatError(reason, 'Não foi possível carregar os veículos elegíveis.'));
        }
      })
      .finally(() => { if (active) setVehiclesLoading(false); });
    return () => { active = false; };
  }, [form.transportadoraId]);

  const selectedVehicle = useMemo(
    () => vehicles.find((vehicle) => String(vehicle.id) === String(form.veiculoId)),
    [vehicles, form.veiculoId]
  );

  async function submit(event) {
    event.preventDefault();
    if (busy || !canCreate) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await gateAppointmentsApi.criar({
        ...form,
        transportadoraId: Number(form.transportadoraId),
        motoristaId: Number(form.motoristaId),
        veiculoId: Number(form.veiculoId),
        janelaAtendimentoId: Number(form.janelaAtendimentoId)
      });
      setSuccess(`Agendamento criado com o veículo ${selectedVehicle?.placa ?? ''}.`);
      setForm(EMPTY_FORM);
      setVehicles([]);
      await reload();
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível criar o agendamento.'));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <PageHeader
      eyebrow="Gate"
      title="Agendamentos"
      description="Consulte os agendamentos e, quando autorizado, crie um novo atendimento selecionando apenas veículos ativos da transportadora."
      actions={<button type="button" className="secondary" onClick={reload}>Atualizar</button>}
    />
    <Message type="error">{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    {canCreate && <Section title="Novo agendamento" description="A lista de veículos é filtrada pela transportadora. Veículos inativos ou de outra empresa são bloqueados novamente pelo backend.">
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Código</span><input required maxLength="40" value={form.codigo} onChange={(event) => setForm((current) => ({ ...current, codigo: event.target.value }))} placeholder="AG-2026-001" /></label>
        <label className="field"><span>Operação</span><select value={form.tipoOperacao} onChange={(event) => setForm((current) => ({ ...current, tipoOperacao: event.target.value }))}><option value="ENTRADA">Entrada</option><option value="SAIDA">Saída</option><option value="DEVOLUCAO">Devolução</option><option value="TRANSFERENCIA">Transferência</option></select></label>
        <label className="field"><span>Transportadora</span><select required value={form.transportadoraId} onChange={(event) => setForm((current) => ({ ...current, transportadoraId: event.target.value, veiculoId: '' }))}><option value="">Selecione</option>{carriers.map((carrier) => <option key={carrier.codigo} value={carrier.codigo}>{carrier.descricao}</option>)}</select></label>
        <label className="field"><span>Veículo elegível</span><select required disabled={!form.transportadoraId || vehiclesLoading} value={form.veiculoId} onChange={(event) => setForm((current) => ({ ...current, veiculoId: event.target.value }))}><option value="">{vehiclesLoading ? 'Carregando...' : vehicles.length ? 'Selecione' : 'Nenhum veículo ativo'}</option>{vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicleLabel(vehicle)}</option>)}</select></label>
        <label className="field"><span>Motorista ID</span><input required type="number" min="1" value={form.motoristaId} onChange={(event) => setForm((current) => ({ ...current, motoristaId: event.target.value }))} /></label>
        <label className="field"><span>Janela de atendimento</span><select required value={form.janelaAtendimentoId} onChange={(event) => setForm((current) => ({ ...current, janelaAtendimentoId: event.target.value }))}><option value="">Selecione</option>{windows.map((window) => <option key={window.id} value={window.id}>{windowLabel(window)}</option>)}</select></label>
        <label className="field"><span>Chegada prevista</span><input required type="datetime-local" value={form.horarioPrevistoChegada} onChange={(event) => setForm((current) => ({ ...current, horarioPrevistoChegada: event.target.value }))} /></label>
        <label className="field"><span>Saída prevista</span><input required type="datetime-local" value={form.horarioPrevistoSaida} onChange={(event) => setForm((current) => ({ ...current, horarioPrevistoSaida: event.target.value }))} /></label>
        <label className="field"><span>Observações</span><textarea maxLength="500" value={form.observacoes} onChange={(event) => setForm((current) => ({ ...current, observacoes: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={busy || vehiclesLoading || !form.veiculoId}>{busy ? 'Criando...' : 'Criar agendamento'}</button></div>
      </form>
    </Section>}

    <Section title="Agendamentos cadastrados">
      {loading ? <Loading label="Carregando agendamentos..." /> : rows.length ? <DataTable rows={rows} rowKey="id" columns={[
        { key: 'codigo', label: 'Código' },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> },
        { key: 'transportadoraNome', label: 'Transportadora' },
        { key: 'placaVeiculo', label: 'Veículo' },
        { key: 'horarioPrevistoChegada', label: 'Chegada prevista' }
      ]} /> : <EmptyState title="Nenhum agendamento encontrado" />}
    </Section>
  </>;
}
