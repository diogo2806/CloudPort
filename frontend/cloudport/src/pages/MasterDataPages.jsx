import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { masterDataApi } from '../masterDataApi.js';

function useRemote(loader, dependencies = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await loader();
      setData(response);
      return response;
    } catch (reason) {
      setData(null);
      setError(formatError(reason));
      return null;
    } finally {
      setLoading(false);
    }
  }, dependencies);
  useEffect(() => { reload(); }, [reload]);
  return { data, loading, error, reload };
}

function numberValue(value) {
  return value === '' || value === null || value === undefined ? null : Number(value);
}

function textValue(value) {
  return value === null || value === undefined ? '' : String(value);
}

const EMPTY_VESSEL = {
  nome: '', codigoImo: '', paisBandeira: '', empresaArmadora: '', capacidadeTeu: '',
  loaMetros: '', caladoMaximoMetros: '', callSign: ''
};

export function VesselRegistrationsPage({ session }) {
  const canEdit = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const canDelete = hasAnyRole(session, 'ADMIN_PORTO');
  const remote = useRemote(() => masterDataApi.listarNavios(), []);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY_VESSEL);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function selectVessel(row) {
    setSelectedId(row.identificador);
    setError('');
    try {
      const detail = await masterDataApi.obterNavio(row.identificador);
      setForm({
        nome: textValue(detail.nome),
        codigoImo: textValue(detail.codigoImo),
        paisBandeira: textValue(detail.paisBandeira),
        empresaArmadora: textValue(detail.empresaArmadora),
        capacidadeTeu: textValue(detail.capacidadeTeu),
        loaMetros: textValue(detail.loaMetros),
        caladoMaximoMetros: textValue(detail.caladoMaximoMetros),
        callSign: textValue(detail.callSign)
      });
    } catch (reason) {
      setError(formatError(reason));
    }
  }

  function clearForm() {
    setSelectedId(null);
    setForm(EMPTY_VESSEL);
    setError('');
    setMessage('');
  }

  async function submit(event) {
    event.preventDefault();
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    const payload = {
      ...form,
      capacidadeTeu: numberValue(form.capacidadeTeu),
      loaMetros: numberValue(form.loaMetros),
      caladoMaximoMetros: numberValue(form.caladoMaximoMetros)
    };
    try {
      if (selectedId) {
        await masterDataApi.atualizarNavio(selectedId, payload);
        setMessage('Navio atualizado com sucesso.');
      } else {
        await masterDataApi.criarNavio(payload);
        setMessage('Navio cadastrado com sucesso.');
      }
      await remote.reload();
      clearForm();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!selectedId || !canDelete || busy || !globalThis.confirm('Excluir o navio selecionado?')) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await masterDataApi.removerNavio(selectedId);
      await remote.reload();
      clearForm();
      setMessage('Navio excluído com sucesso.');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <PageHeader eyebrow="Cadastros · Navio" title="Navios" description="Cadastro mestre de embarcações utilizado pelas escalas, line-up, berços e planejamento de estiva." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error || error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil pode consultar os navios, mas não pode alterar o cadastro.</Message>}
    <Section title={selectedId ? 'Editar navio' : 'Cadastrar navio'} description="O código IMO deve seguir o padrão IMO9999999.">
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Nome</span><input required maxLength="120" value={form.nome} onChange={(event) => setForm((current) => ({ ...current, nome: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Código IMO</span><input required pattern="IMO[0-9]{7}" placeholder="IMO9999999" value={form.codigoImo} onChange={(event) => setForm((current) => ({ ...current, codigoImo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>País da bandeira</span><input required maxLength="60" value={form.paisBandeira} onChange={(event) => setForm((current) => ({ ...current, paisBandeira: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Empresa armadora</span><input required maxLength="80" value={form.empresaArmadora} onChange={(event) => setForm((current) => ({ ...current, empresaArmadora: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Capacidade (TEU)</span><input required type="number" min="1" value={form.capacidadeTeu} onChange={(event) => setForm((current) => ({ ...current, capacidadeTeu: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>LOA (m)</span><input type="number" min="0.01" step="0.01" value={form.loaMetros} onChange={(event) => setForm((current) => ({ ...current, loaMetros: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Calado máximo (m)</span><input type="number" min="0.01" step="0.01" value={form.caladoMaximoMetros} onChange={(event) => setForm((current) => ({ ...current, caladoMaximoMetros: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Call sign</span><input maxLength="15" value={form.callSign} onChange={(event) => setForm((current) => ({ ...current, callSign: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <div className="field"><span>Ações</span><div className="actions"><button type="submit" disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar navio' : 'Cadastrar navio'}</button><button type="button" className="secondary" onClick={clearForm}>Limpar</button>{selectedId && canDelete && <button type="button" className="danger" onClick={remove} disabled={busy}>Excluir</button>}</div></div>
      </form>
    </Section>
    <Section title="Navios cadastrados">
      {remote.loading ? <Loading /> : <DataTable rows={remote.data ?? []} rowKey="identificador" onRowClick={selectVessel} columns={[
        { key: 'codigoImo', label: 'IMO' }, { key: 'nome', label: 'Navio' }, { key: 'empresaArmadora', label: 'Armador' }, { key: 'capacidadeTeu', label: 'Capacidade (TEU)' }
      ]} emptyTitle="Nenhum navio cadastrado" />}
    </Section>
  </>;
}

const EMPTY_BERTH = {
  codigo: '', nome: '', comprimentoMetros: '', caladoMetros: '', guinchesPermanentes: '0',
  capacidadeToneladasDia: '', voltagem: '440V', aguaPotavel: true, energiaGenerica: true,
  iluminacaoNoturna: true, sistemaSeguranca: true, cobertura: false, compatContainer: true,
  compatBreakbulk: true, compatRoro: false, compatCargaGeral: true, compatReefer: true,
  compatPerigosa: false, compatGranel: false, zonaPrimaria: '', zonaSecundaria: '',
  distanciaZonaMetros: '0', tempoTransporteMinutos: '0', diasOperacao: 'SEG-DOM',
  ultimaManutencao: '', proximaManutencao: '', status: 'OPERACIONAL', observacoes: ''
};

const BERTH_BOOLEAN_FIELDS = [
  ['aguaPotavel', 'Água potável'], ['energiaGenerica', 'Energia'], ['iluminacaoNoturna', 'Iluminação noturna'],
  ['sistemaSeguranca', 'Sistema de segurança'], ['cobertura', 'Cobertura'], ['compatContainer', 'Contêiner'],
  ['compatBreakbulk', 'Breakbulk'], ['compatRoro', 'Ro-Ro'], ['compatCargaGeral', 'Carga geral'],
  ['compatReefer', 'Reefer'], ['compatPerigosa', 'Carga perigosa'], ['compatGranel', 'Granel']
];

export function BerthRegistrationsPage({ session }) {
  const canEdit = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const canDelete = hasAnyRole(session, 'ADMIN_PORTO');
  const remote = useRemote(() => masterDataApi.listarBercos(), []);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(EMPTY_BERTH);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  function selectBerth(row) {
    setSelectedId(row.id);
    setForm(Object.fromEntries(Object.keys(EMPTY_BERTH).map((key) => [key, typeof EMPTY_BERTH[key] === 'boolean' ? Boolean(row[key]) : textValue(row[key])])));
    setError(''); setMessage('');
  }

  function clearForm() {
    setSelectedId(null); setForm(EMPTY_BERTH); setError(''); setMessage('');
  }

  async function submit(event) {
    event.preventDefault();
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    const payload = {
      ...form,
      comprimentoMetros: numberValue(form.comprimentoMetros),
      caladoMetros: numberValue(form.caladoMetros),
      guinchesPermanentes: numberValue(form.guinchesPermanentes),
      capacidadeToneladasDia: numberValue(form.capacidadeToneladasDia),
      distanciaZonaMetros: numberValue(form.distanciaZonaMetros),
      tempoTransporteMinutos: numberValue(form.tempoTransporteMinutos),
      ultimaManutencao: form.ultimaManutencao || null,
      proximaManutencao: form.proximaManutencao || null
    };
    try {
      if (selectedId) {
        await masterDataApi.atualizarBerco(selectedId, payload);
        setMessage('Berço atualizado com sucesso.');
      } else {
        await masterDataApi.criarBerco(payload);
        setMessage('Berço cadastrado com sucesso.');
      }
      await remote.reload();
      clearForm();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!selectedId || !canDelete || busy || !globalThis.confirm('Excluir o berço selecionado?')) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await masterDataApi.removerBerco(selectedId);
      await remote.reload();
      clearForm();
      setMessage('Berço excluído com sucesso.');
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <PageHeader eyebrow="Cadastros · Infraestrutura" title="Berços portuários" description="Cadastro físico e operacional dos berços usados pelo line-up, reservas, manutenção e alocação de navios." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error || error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil pode consultar, mas não pode alterar berços.</Message>}
    <Section title={selectedId ? 'Editar berço' : 'Cadastrar berço'}>
      <form className="planner-selection-grid" onSubmit={submit}>
        <label className="field"><span>Código</span><input required maxLength="30" value={form.codigo} onChange={(event) => setForm((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Nome</span><input required maxLength="120" value={form.nome} onChange={(event) => setForm((current) => ({ ...current, nome: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Comprimento (m)</span><input required type="number" min="1" value={form.comprimentoMetros} onChange={(event) => setForm((current) => ({ ...current, comprimentoMetros: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Calado (m)</span><input required type="number" min="0.01" step="0.01" value={form.caladoMetros} onChange={(event) => setForm((current) => ({ ...current, caladoMetros: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Guindastes permanentes</span><input required type="number" min="0" value={form.guinchesPermanentes} onChange={(event) => setForm((current) => ({ ...current, guinchesPermanentes: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Capacidade (t/dia)</span><input required type="number" min="1" value={form.capacidadeToneladasDia} onChange={(event) => setForm((current) => ({ ...current, capacidadeToneladasDia: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Voltagem</span><input required maxLength="40" value={form.voltagem} onChange={(event) => setForm((current) => ({ ...current, voltagem: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Status</span><select value={form.status} onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))} disabled={!canEdit || busy}><option>OPERACIONAL</option><option>MANUTENCAO</option><option>BLOQUEADO</option></select></label>
        <label className="field"><span>Zona primária</span><input required maxLength="40" value={form.zonaPrimaria} onChange={(event) => setForm((current) => ({ ...current, zonaPrimaria: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Zona secundária</span><input maxLength="40" value={form.zonaSecundaria} onChange={(event) => setForm((current) => ({ ...current, zonaSecundaria: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Distância da zona (m)</span><input required type="number" min="0" value={form.distanciaZonaMetros} onChange={(event) => setForm((current) => ({ ...current, distanciaZonaMetros: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Tempo de transporte (min)</span><input required type="number" min="0" value={form.tempoTransporteMinutos} onChange={(event) => setForm((current) => ({ ...current, tempoTransporteMinutos: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Dias de operação</span><input required maxLength="80" value={form.diasOperacao} onChange={(event) => setForm((current) => ({ ...current, diasOperacao: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Última manutenção</span><input type="date" value={form.ultimaManutencao} onChange={(event) => setForm((current) => ({ ...current, ultimaManutencao: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Próxima manutenção</span><input type="date" value={form.proximaManutencao} onChange={(event) => setForm((current) => ({ ...current, proximaManutencao: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Observações</span><input maxLength="250" value={form.observacoes} onChange={(event) => setForm((current) => ({ ...current, observacoes: event.target.value }))} disabled={!canEdit || busy} /></label>
        {BERTH_BOOLEAN_FIELDS.map(([key, label]) => <label className="field" key={key}><span>{label}</span><span><input type="checkbox" checked={form[key]} onChange={(event) => setForm((current) => ({ ...current, [key]: event.target.checked }))} disabled={!canEdit || busy} /> Habilitado</span></label>)}
        <div className="field"><span>Ações</span><div className="actions"><button type="submit" disabled={!canEdit || busy}>{busy ? 'Salvando...' : selectedId ? 'Atualizar berço' : 'Cadastrar berço'}</button><button type="button" className="secondary" onClick={clearForm}>Limpar</button>{selectedId && canDelete && <button type="button" className="danger" onClick={remove} disabled={busy}>Excluir</button>}</div></div>
      </form>
    </Section>
    <Section title="Berços cadastrados">
      {remote.loading ? <Loading /> : <DataTable rows={remote.data ?? []} rowKey="id" onRowClick={selectBerth} columns={[
        { key: 'codigo', label: 'Código' }, { key: 'nome', label: 'Berço' }, { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'comprimentoMetros', label: 'Comprimento (m)' }, { key: 'caladoMetros', label: 'Calado (m)' }, { key: 'zonaPrimaria', label: 'Zona primária' }
      ]} emptyTitle="Nenhum berço cadastrado" />}
    </Section>
  </>;
}

export function GateInfrastructurePage({ session }) {
  const canEdit = hasAnyRole(session, 'ADMIN_PORTO');
  const remote = useRemote(() => masterDataApi.obterInfraestruturaGate(), []);
  const [facility, setFacility] = useState({ codigo: '', nome: '', fusoHorario: 'America/Sao_Paulo', ativo: true });
  const [gate, setGate] = useState({ facilityId: '', codigo: '', nome: '', ativo: true });
  const [lane, setLane] = useState({ gateId: '', codigo: '', nome: '', direcao: 'ENTRADA', status: 'ABERTA', capacidadeFila: '1', ocrHabilitado: false, balancaHabilitada: false, inspecaoHabilitada: false });
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const facilities = remote.data?.facilities ?? [];
  const gates = remote.data?.gates ?? [];
  const lanes = remote.data?.lanes ?? [];

  async function execute(action, success) {
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await action();
      setMessage(success);
      await remote.reload();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  return <>
    <PageHeader eyebrow="Cadastros · Gate" title="Instalações, gates e pistas" description="Configuração da infraestrutura física utilizada por agendamentos, truck visits, filas, OCR, balança e inspeção." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error || error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Somente ADMIN_PORTO pode alterar a infraestrutura do Gate.</Message>}
    <div className="split-grid">
      <Section title="Instalação">
        <form className="planner-selection-grid" onSubmit={(event) => { event.preventDefault(); execute(() => masterDataApi.salvarFacility(facility), 'Instalação salva com sucesso.'); }}>
          <label className="field"><span>Código</span><input required maxLength="40" value={facility.codigo} onChange={(event) => setFacility((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Nome</span><input required maxLength="120" value={facility.nome} onChange={(event) => setFacility((current) => ({ ...current, nome: event.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Fuso horário</span><input required maxLength="60" value={facility.fusoHorario} onChange={(event) => setFacility((current) => ({ ...current, fusoHorario: event.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Situação</span><span><input type="checkbox" checked={facility.ativo} onChange={(event) => setFacility((current) => ({ ...current, ativo: event.target.checked }))} disabled={!canEdit || busy} /> Ativa</span></label>
          <button disabled={!canEdit || busy}>Salvar instalação</button>
        </form>
      </Section>
      <Section title="Gate">
        <form className="planner-selection-grid" onSubmit={(event) => { event.preventDefault(); execute(() => masterDataApi.salvarGate({ ...gate, facilityId: Number(gate.facilityId) }), 'Gate salvo com sucesso.'); }}>
          <label className="field"><span>Instalação</span><select required value={gate.facilityId} onChange={(event) => setGate((current) => ({ ...current, facilityId: event.target.value }))} disabled={!canEdit || busy}><option value="">Selecione</option>{facilities.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.nome}</option>)}</select></label>
          <label className="field"><span>Código</span><input required maxLength="40" value={gate.codigo} onChange={(event) => setGate((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Nome</span><input required maxLength="120" value={gate.nome} onChange={(event) => setGate((current) => ({ ...current, nome: event.target.value }))} disabled={!canEdit || busy} /></label>
          <label className="field"><span>Situação</span><span><input type="checkbox" checked={gate.ativo} onChange={(event) => setGate((current) => ({ ...current, ativo: event.target.checked }))} disabled={!canEdit || busy} /> Ativo</span></label>
          <button disabled={!canEdit || busy || !gate.facilityId}>Salvar Gate</button>
        </form>
      </Section>
    </div>
    <Section title="Pista operacional">
      <form className="planner-selection-grid" onSubmit={(event) => { event.preventDefault(); execute(() => masterDataApi.salvarLane({ ...lane, gateId: Number(lane.gateId), capacidadeFila: Number(lane.capacidadeFila) }), 'Pista salva com sucesso.'); }}>
        <label className="field"><span>Gate</span><select required value={lane.gateId} onChange={(event) => setLane((current) => ({ ...current, gateId: event.target.value }))} disabled={!canEdit || busy}><option value="">Selecione</option>{gates.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {item.nome}</option>)}</select></label>
        <label className="field"><span>Código</span><input required maxLength="40" value={lane.codigo} onChange={(event) => setLane((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Nome</span><input required maxLength="120" value={lane.nome} onChange={(event) => setLane((current) => ({ ...current, nome: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Direção</span><select value={lane.direcao} onChange={(event) => setLane((current) => ({ ...current, direcao: event.target.value }))} disabled={!canEdit || busy}><option>ENTRADA</option><option>SAIDA</option><option>BIDIRECIONAL</option></select></label>
        <label className="field"><span>Status</span><select value={lane.status} onChange={(event) => setLane((current) => ({ ...current, status: event.target.value }))} disabled={!canEdit || busy}><option>ABERTA</option><option>FECHADA</option><option>MANUTENCAO</option></select></label>
        <label className="field"><span>Capacidade da fila</span><input required type="number" min="1" max="500" value={lane.capacidadeFila} onChange={(event) => setLane((current) => ({ ...current, capacidadeFila: event.target.value }))} disabled={!canEdit || busy} /></label>
        {[['ocrHabilitado', 'OCR'], ['balancaHabilitada', 'Balança'], ['inspecaoHabilitada', 'Inspeção']].map(([key, label]) => <label className="field" key={key}><span>{label}</span><span><input type="checkbox" checked={lane[key]} onChange={(event) => setLane((current) => ({ ...current, [key]: event.target.checked }))} disabled={!canEdit || busy} /> Habilitado</span></label>)}
        <button disabled={!canEdit || busy || !lane.gateId}>Salvar pista</button>
      </form>
    </Section>
    {remote.loading ? <Loading /> : <div className="split-grid">
      <Section title="Instalações e gates"><DataTable rows={facilities} rowKey="id" columns={[{ key: 'codigo', label: 'Código' }, { key: 'nome', label: 'Instalação' }, { key: 'fusoHorario', label: 'Fuso' }, { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVA' : 'INATIVA'} /> }]} emptyTitle="Nenhuma instalação cadastrada" /></Section>
      <Section title="Gates"><DataTable rows={gates} rowKey="id" columns={[{ key: 'codigo', label: 'Código' }, { key: 'nome', label: 'Gate' }, { key: 'facilityId', label: 'Instalação' }, { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> }]} emptyTitle="Nenhum Gate cadastrado" /></Section>
      <Section title="Pistas"><DataTable rows={lanes} rowKey="id" columns={[{ key: 'codigo', label: 'Código' }, { key: 'nome', label: 'Pista' }, { key: 'direcao', label: 'Direção' }, { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> }, { key: 'capacidadeFila', label: 'Capacidade' }]} emptyTitle="Nenhuma pista cadastrada" /></Section>
    </div>}
  </>;
}

export function EquipmentReferencesPage({ session }) {
  const canEdit = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const remote = useRemote(async () => {
    const [types, prefixes] = await Promise.all([masterDataApi.listarTiposEquipamento(), masterDataApi.listarPrefixosEquipamento()]);
    return { types: types ?? [], prefixes: prefixes ?? [] };
  }, []);
  const [typeForm, setTypeForm] = useState({ codigo: '', descricao: '', categoria: 'CONTEINER', codigoIso: '', comprimentoMm: '', larguraMm: '', alturaMm: '', taraKg: '', capacidadeKg: '', refrigerado: false, grupoEquivalencia: '' });
  const [prefixForm, setPrefixForm] = useState({ prefixo: '', proprietario: '', categoria: 'CONTEINER' });
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function execute(action, success) {
    if (!canEdit || busy) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await action();
      setMessage(success);
      await remote.reload();
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setBusy(false);
    }
  }

  const typeRows = remote.data?.types ?? [];
  const prefixRows = remote.data?.prefixes ?? [];
  const categories = ['CONTEINER', 'CHASSI', 'CARRETA', 'ACESSORIO'];

  return <>
    <PageHeader eyebrow="Cadastros · Equipamentos" title="Tipos e prefixos de equipamentos" description="Referências utilizadas no cadastro canônico de contêineres, chassis, carretas e acessórios." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error || error}</Message><Message type="success">{message}</Message>
    {!canEdit && <Message type="warning">Seu perfil pode consultar, mas não pode criar referências de equipamentos.</Message>}
    <Section title="Cadastrar tipo de equipamento" description="Informe dimensões em milímetros e pesos em quilogramas.">
      <form className="planner-selection-grid" onSubmit={(event) => { event.preventDefault(); execute(() => masterDataApi.criarTipoEquipamento({ ...typeForm, comprimentoMm: numberValue(typeForm.comprimentoMm), larguraMm: numberValue(typeForm.larguraMm), alturaMm: numberValue(typeForm.alturaMm), taraKg: numberValue(typeForm.taraKg), capacidadeKg: numberValue(typeForm.capacidadeKg) }), 'Tipo de equipamento cadastrado com sucesso.'); }}>
        <label className="field"><span>Código</span><input required maxLength="30" value={typeForm.codigo} onChange={(event) => setTypeForm((current) => ({ ...current, codigo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Descrição</span><input required maxLength="120" value={typeForm.descricao} onChange={(event) => setTypeForm((current) => ({ ...current, descricao: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Categoria</span><select value={typeForm.categoria} onChange={(event) => setTypeForm((current) => ({ ...current, categoria: event.target.value }))} disabled={!canEdit || busy}>{categories.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label className="field"><span>Código ISO</span><input maxLength="10" value={typeForm.codigoIso} onChange={(event) => setTypeForm((current) => ({ ...current, codigoIso: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Comprimento (mm)</span><input type="number" min="1" value={typeForm.comprimentoMm} onChange={(event) => setTypeForm((current) => ({ ...current, comprimentoMm: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Largura (mm)</span><input type="number" min="1" value={typeForm.larguraMm} onChange={(event) => setTypeForm((current) => ({ ...current, larguraMm: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Altura (mm)</span><input type="number" min="1" value={typeForm.alturaMm} onChange={(event) => setTypeForm((current) => ({ ...current, alturaMm: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Tara (kg)</span><input type="number" min="0" step="0.001" value={typeForm.taraKg} onChange={(event) => setTypeForm((current) => ({ ...current, taraKg: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Capacidade (kg)</span><input type="number" min="0" step="0.001" value={typeForm.capacidadeKg} onChange={(event) => setTypeForm((current) => ({ ...current, capacidadeKg: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Grupo de equivalência</span><input maxLength="60" value={typeForm.grupoEquivalencia} onChange={(event) => setTypeForm((current) => ({ ...current, grupoEquivalencia: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Reefer</span><span><input type="checkbox" checked={typeForm.refrigerado} onChange={(event) => setTypeForm((current) => ({ ...current, refrigerado: event.target.checked }))} disabled={!canEdit || busy} /> Refrigerado</span></label>
        <button disabled={!canEdit || busy}>Cadastrar tipo</button>
      </form>
    </Section>
    <Section title="Cadastrar prefixo">
      <form className="planner-selection-grid" onSubmit={(event) => { event.preventDefault(); execute(() => masterDataApi.criarPrefixoEquipamento(prefixForm), 'Prefixo cadastrado com sucesso.'); }}>
        <label className="field"><span>Prefixo</span><input required maxLength="12" value={prefixForm.prefixo} onChange={(event) => setPrefixForm((current) => ({ ...current, prefixo: event.target.value.toUpperCase() }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Proprietário</span><input maxLength="120" value={prefixForm.proprietario} onChange={(event) => setPrefixForm((current) => ({ ...current, proprietario: event.target.value }))} disabled={!canEdit || busy} /></label>
        <label className="field"><span>Categoria</span><select value={prefixForm.categoria} onChange={(event) => setPrefixForm((current) => ({ ...current, categoria: event.target.value }))} disabled={!canEdit || busy}>{categories.map((item) => <option key={item}>{item}</option>)}</select></label>
        <button disabled={!canEdit || busy}>Cadastrar prefixo</button>
      </form>
    </Section>
    {remote.loading ? <Loading /> : <div className="split-grid">
      <Section title={`Tipos (${typeRows.length})`}>{typeRows.length ? <DataTable rows={typeRows} rowKey="id" columns={[{ key: 'codigo', label: 'Código' }, { key: 'descricao', label: 'Descrição' }, { key: 'categoria', label: 'Categoria' }, { key: 'codigoIso', label: 'ISO' }, { key: 'grupoEquivalencia', label: 'Equivalência' }, { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> }]} /> : <EmptyState title="Nenhum tipo cadastrado" />}</Section>
      <Section title={`Prefixos (${prefixRows.length})`}>{prefixRows.length ? <DataTable rows={prefixRows} rowKey="id" columns={[{ key: 'prefixo', label: 'Prefixo' }, { key: 'proprietario', label: 'Proprietário' }, { key: 'categoria', label: 'Categoria' }, { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> }]} /> : <EmptyState title="Nenhum prefixo cadastrado" />}</Section>
    </div>}
  </>;
}
