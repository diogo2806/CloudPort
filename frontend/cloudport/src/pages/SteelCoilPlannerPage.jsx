import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, normalizePage, sanitizeText } from '../api.js';
import { steelCoilApi } from '../steelCoilApi.js';
import { DataTable, EmptyState, JsonDetails, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';

const EMPTY_COIL = {
  codigo: '',
  pesoKg: '',
  diametroExternoMm: '',
  diametroInternoMm: '',
  larguraMm: '',
  grauAco: '',
  portoDescarga: ''
};

const EMPTY_POSITION = {
  bobinaId: '',
  poraoId: '',
  setorId: '',
  camada: 1,
  posicaoX: 0,
  posicaoY: 0,
  espessuraDunnageMm: 50,
  tipoLashing: 'SEM_LASHING'
};

function normalizeList(payload) {
  return Array.isArray(payload) ? payload : normalizePage(payload);
}

function visitIdentifier(visit) {
  return visit?.id ?? visit?.visitaNavioId ?? visit?.identificador ?? '';
}

function visitCode(visit) {
  return sanitizeText(visit?.viagemEntrada ?? visit?.codigoViagem ?? visit?.viagemSaida ?? visit?.codigoVisita);
}

function formatNumber(value, suffix = '') {
  if (value === undefined || value === null || value === '') return '—';
  const number = Number(value);
  return Number.isFinite(number)
    ? `${number.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}${suffix}`
    : '—';
}

export function SteelCoilPlannerPage() {
  const [navios, setNavios] = useState([]);
  const [visitas, setVisitas] = useState([]);
  const [planos, setPlanos] = useState([]);
  const [navioId, setNavioId] = useState('');
  const [visitaNavioId, setVisitaNavioId] = useState('');
  const [plano, setPlano] = useState(null);
  const [novoPlano, setNovoPlano] = useState({ codigoViagem: '', portoCarga: '', portoDescarga: '' });
  const [novaBobina, setNovaBobina] = useState(EMPTY_COIL);
  const [novaPosicao, setNovaPosicao] = useState(EMPTY_POSITION);
  const [tanktop, setTanktop] = useState(null);
  const [empilhamento, setEmpilhamento] = useState(null);
  const [estabilidade, setEstabilidade] = useState(null);
  const [securing, setSecuring] = useState(null);
  const [relatorio, setRelatorio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadingPlanos, setLoadingPlanos] = useState(false);
  const [busyAction, setBusyAction] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const selectedNavio = useMemo(
    () => navios.find((item) => String(item.id) === String(navioId)) ?? null,
    [navios, navioId]
  );

  const selectedVisit = useMemo(
    () => visitas.find((item) => String(visitIdentifier(item)) === String(visitaNavioId)) ?? null,
    [visitas, visitaNavioId]
  );

  const selectedHold = useMemo(
    () => selectedNavio?.poroes?.find((item) => String(item.id) === String(novaPosicao.poraoId)) ?? null,
    [selectedNavio, novaPosicao.poraoId]
  );

  const unpositionedCoils = useMemo(
    () => (plano?.bobinas ?? []).filter((item) => !item.posicionada),
    [plano]
  );

  const approved = plano?.status === 'APROVADO';

  const resetAnalyses = useCallback(() => {
    setTanktop(null);
    setEmpilhamento(null);
    setEstabilidade(null);
    setSecuring(null);
    setRelatorio(null);
  }, []);

  const loadInitialData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [naviosResponse, visitasResponse] = await Promise.all([
        steelCoilApi.listarNavios(),
        steelCoilApi.listarEscalas(60)
      ]);
      setNavios(normalizeList(naviosResponse));
      setVisitas(normalizeList(visitasResponse).filter((item) => visitIdentifier(item)));
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar navios e visitas do planejamento.'));
    } finally {
      setLoading(false);
    }
  }, []);

  const loadPlans = useCallback(async () => {
    if (!navioId || !visitaNavioId) {
      setPlanos([]);
      return;
    }
    setLoadingPlanos(true);
    setError('');
    try {
      const response = await steelCoilApi.listarPlanos(Number(navioId), Number(visitaNavioId));
      setPlanos(normalizeList(response));
    } catch (reason) {
      setPlanos([]);
      setError(formatError(reason, 'Não foi possível carregar os planos persistidos da visita.'));
    } finally {
      setLoadingPlanos(false);
    }
  }, [navioId, visitaNavioId]);

  const refreshPlan = useCallback(async (planId = plano?.id) => {
    if (!planId) return null;
    const response = await steelCoilApi.buscarPlano(planId);
    setPlano(response);
    const firstAvailable = (response?.bobinas ?? []).find((item) => !item.posicionada);
    setNovaPosicao((current) => ({
      ...current,
      bobinaId: firstAvailable?.id ? String(firstAvailable.id) : ''
    }));
    return response;
  }, [plano?.id]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  useEffect(() => {
    const code = visitCode(selectedVisit);
    setNovoPlano((current) => ({
      ...current,
      codigoViagem: code || current.codigoViagem,
      portoCarga: sanitizeText(selectedVisit?.portoOrigem ?? selectedVisit?.portoCarga) || current.portoCarga,
      portoDescarga: sanitizeText(selectedVisit?.portoDestino ?? selectedVisit?.portoDescarga) || current.portoDescarga
    }));
  }, [selectedVisit]);

  useEffect(() => {
    const firstHold = selectedNavio?.poroes?.[0];
    const firstSector = firstHold?.setores?.[0];
    setNovaPosicao((current) => ({
      ...current,
      poraoId: firstHold?.id ? String(firstHold.id) : '',
      setorId: firstSector?.id ? String(firstSector.id) : ''
    }));
  }, [selectedNavio]);

  useEffect(() => {
    const firstSector = selectedHold?.setores?.[0];
    setNovaPosicao((current) => ({
      ...current,
      setorId: firstSector?.id ? String(firstSector.id) : ''
    }));
  }, [selectedHold]);

  useEffect(() => {
    setPlano(null);
    resetAnalyses();
    loadPlans();
  }, [loadPlans, resetAnalyses]);

  async function runAction(action, operation, message) {
    if (busyAction) return null;
    setBusyAction(action);
    setError('');
    setSuccess('');
    try {
      const result = await operation();
      if (message) setSuccess(message);
      return result;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusyAction('');
    }
  }

  async function createPlan(event) {
    event.preventDefault();
    if (!navioId || !visitaNavioId) return;
    const result = await runAction('create-plan', () => steelCoilApi.criarPlano({
      navioId: Number(navioId),
      visitaNavioId: Number(visitaNavioId),
      codigoViagem: visitCode(selectedVisit) || sanitizeText(novoPlano.codigoViagem),
      portoCarga: sanitizeText(novoPlano.portoCarga),
      portoDescarga: sanitizeText(novoPlano.portoDescarga)
    }), 'Plano criado e vinculado à visita selecionada.');
    if (result?.id) {
      await loadPlans();
      await refreshPlan(result.id);
      resetAnalyses();
    }
  }

  async function openPlan(planId) {
    if (!planId) {
      setPlano(null);
      resetAnalyses();
      return;
    }
    const result = await runAction('open-plan', () => steelCoilApi.buscarPlano(planId));
    if (result) {
      setPlano(result);
      const firstAvailable = (result.bobinas ?? []).find((item) => !item.posicionada);
      setNovaPosicao((current) => ({ ...current, bobinaId: firstAvailable?.id ? String(firstAvailable.id) : '' }));
      resetAnalyses();
    }
  }

  async function addCoil(event) {
    event.preventDefault();
    if (!plano?.id || approved) return;
    const result = await runAction('add-coil', () => steelCoilApi.adicionarBobina(plano.id, {
      codigo: sanitizeText(novaBobina.codigo),
      pesoKg: Number(novaBobina.pesoKg),
      diametroExternoMm: Number(novaBobina.diametroExternoMm),
      diametroInternoMm: Number(novaBobina.diametroInternoMm),
      larguraMm: Number(novaBobina.larguraMm),
      grauAco: sanitizeText(novaBobina.grauAco),
      portoDescarga: sanitizeText(novaBobina.portoDescarga)
    }), 'Bobina adicionada ao manifesto persistido.');
    if (result) {
      setNovaBobina({ ...EMPTY_COIL, portoDescarga: plano.portoDescarga ?? '' });
      await refreshPlan(plano.id);
      await loadPlans();
      resetAnalyses();
    }
  }

  async function placeCoil(event) {
    event.preventDefault();
    if (!plano?.id || approved) return;
    const result = await runAction('place-coil', () => steelCoilApi.posicionarBobina(plano.id, {
      bobinaId: Number(novaPosicao.bobinaId),
      poraoId: Number(novaPosicao.poraoId),
      setorId: Number(novaPosicao.setorId),
      camada: Number(novaPosicao.camada),
      posicaoX: Number(novaPosicao.posicaoX),
      posicaoY: Number(novaPosicao.posicaoY),
      espessuraDunnageMm: Number(novaPosicao.espessuraDunnageMm),
      tipoLashing: novaPosicao.tipoLashing
    }), 'Posição validada e persistida pelo backend.');
    if (result) {
      await refreshPlan(plano.id);
      await loadPlans();
      resetAnalyses();
    }
  }

  async function loadTanktop() {
    const result = await runAction('tanktop', () => steelCoilApi.analisarTanktop(plano.id));
    if (result) setTanktop(normalizeList(result));
  }

  async function loadStacking() {
    const poraoId = novaPosicao.poraoId || selectedNavio?.poroes?.[0]?.id;
    if (!poraoId) {
      setError('Selecione um porão para analisar o empilhamento.');
      return;
    }
    const result = await runAction('stacking', () => steelCoilApi.analisarEmpilhamento(plano.id, poraoId));
    if (result) setEmpilhamento(result);
  }

  async function loadStability() {
    const result = await runAction('stability', () => steelCoilApi.calcularEstabilidade(plano.id));
    if (result) setEstabilidade(result);
  }

  async function loadSecuring() {
    const result = await runAction('securing', () => steelCoilApi.calcularSecuring(plano.id));
    if (result) setSecuring(result);
  }

  async function validatePlan() {
    const result = await runAction('validate', () => steelCoilApi.validarPlano(plano.id), 'Plano validado e recarregado.');
    if (result) {
      setPlano(result);
      setEstabilidade(result.estabilidade ?? null);
      await loadPlans();
    }
  }

  async function loadReport() {
    const result = await runAction('report', () => steelCoilApi.obterRelatorio(plano.id));
    if (result) setRelatorio(result);
  }

  if (loading) {
    return <>
      <PageHeader eyebrow="Embarque" title="Planejamento de steel coils" description="Carregando contexto operacional persistido." />
      <p>Carregando navios e visitas...</p>
    </>;
  }

  return <>
    <PageHeader
      eyebrow="Embarque"
      title="Planejamento de steel coils"
      description="O plano é carregado por navio e visita canônica. Criação, posicionamento e cálculos usam os contratos publicados pelo backend."
      actions={<button className="secondary" onClick={loadInitialData} disabled={!!busyAction}>Atualizar contexto</button>}
    />
    <Message type="error" onClose={() => setError('')}>{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <Section title="Contexto operacional" description="Selecione o perfil estrutural do navio e a visita antes de criar ou abrir um plano.">
      <div className="planner-context-grid">
        <label className="field"><span>Navio siderúrgico</span><select value={navioId} onChange={(event) => setNavioId(event.target.value)}><option value="">Selecione</option>{navios.map((item) => <option key={item.id} value={item.id}>{item.nome} {item.imo ? `· IMO ${item.imo}` : ''}</option>)}</select></label>
        <label className="field"><span>Visita / viagem</span><select value={visitaNavioId} onChange={(event) => setVisitaNavioId(event.target.value)}><option value="">Selecione</option>{visitas.map((item) => <option key={visitIdentifier(item)} value={visitIdentifier(item)}>{item.nomeNavio ?? item.navioNome ?? 'Navio'} · {visitCode(item) || item.codigoVisita || `visita ${visitIdentifier(item)}`}</option>)}</select></label>
        <label className="field"><span>Plano persistido</span><select value={plano?.id ?? ''} onChange={(event) => openPlan(Number(event.target.value))} disabled={!navioId || !visitaNavioId || loadingPlanos}><option value="">{loadingPlanos ? 'Carregando...' : 'Criar ou selecionar'}</option>{planos.map((item) => <option key={item.id} value={item.id}>#{item.id} · {item.status ?? 'RASCUNHO'} · {item.totalBobinas ?? item.bobinas?.length ?? 0} bobina(s)</option>)}</select></label>
      </div>
    </Section>

    {!plano && <Section title="Criar plano" description="A identidade da visita selecionada será enviada obrigatoriamente ao backend.">
      <form className="planner-form-grid" onSubmit={createPlan}>
        <label className="field"><span>Código da viagem</span><input value={novoPlano.codigoViagem} onChange={(event) => setNovoPlano((current) => ({ ...current, codigoViagem: event.target.value }))} maxLength={30} required /></label>
        <label className="field"><span>Porto de carga</span><input value={novoPlano.portoCarga} onChange={(event) => setNovoPlano((current) => ({ ...current, portoCarga: event.target.value }))} maxLength={10} required /></label>
        <label className="field"><span>Porto de descarga</span><input value={novoPlano.portoDescarga} onChange={(event) => setNovoPlano((current) => ({ ...current, portoDescarga: event.target.value }))} maxLength={10} required /></label>
        <button type="submit" disabled={!navioId || !visitaNavioId || busyAction === 'create-plan'}>{busyAction === 'create-plan' ? 'Criando...' : 'Criar plano'}</button>
      </form>
      {!loadingPlanos && navioId && visitaNavioId && !planos.length && <EmptyState title="Nenhum plano para esta visita" description="Crie o primeiro plano persistido para o contexto selecionado." />}
    </Section>}

    {plano && <>
      <div className="metrics-grid">
        <MetricCard label="Plano" value={`#${plano.id}`} detail={`${plano.nomeNavio ?? selectedNavio?.nome ?? 'Navio'} · ${plano.codigoViagem ?? visitCode(selectedVisit) ?? 'Viagem'}`} />
        <MetricCard label="Visita" value={plano.codigoVisita ?? visitaNavioId} detail={`Identificador ${plano.visitaNavioId ?? visitaNavioId}`} />
        <MetricCard label="Status" value={<StatusBadge value={plano.status} />} detail={approved ? 'Alterações bloqueadas' : 'Plano editável'} />
        <MetricCard label="Manifesto" value={plano.totalBobinas ?? plano.bobinas?.length ?? 0} detail={`${unpositionedCoils.length} sem posição`} />
      </div>

      <Section title="Manifesto de bobinas" description="As inclusões são persistidas e o plano é recarregado após cada comando.">
        <form className="planner-form-grid coil-form" onSubmit={addCoil}>
          <label className="field"><span>Código</span><input value={novaBobina.codigo} onChange={(event) => setNovaBobina((current) => ({ ...current, codigo: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Peso (kg)</span><input type="number" min="1" step="0.01" value={novaBobina.pesoKg} onChange={(event) => setNovaBobina((current) => ({ ...current, pesoKg: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Diâmetro externo (mm)</span><input type="number" min="1" step="0.01" value={novaBobina.diametroExternoMm} onChange={(event) => setNovaBobina((current) => ({ ...current, diametroExternoMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Diâmetro interno (mm)</span><input type="number" min="0" step="0.01" value={novaBobina.diametroInternoMm} onChange={(event) => setNovaBobina((current) => ({ ...current, diametroInternoMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Largura (mm)</span><input type="number" min="1" step="0.01" value={novaBobina.larguraMm} onChange={(event) => setNovaBobina((current) => ({ ...current, larguraMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Grau do aço</span><input value={novaBobina.grauAco} onChange={(event) => setNovaBobina((current) => ({ ...current, grauAco: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Porto de descarga</span><input value={novaBobina.portoDescarga} onChange={(event) => setNovaBobina((current) => ({ ...current, portoDescarga: event.target.value }))} required disabled={approved} /></label>
          <button type="submit" disabled={approved || busyAction === 'add-coil'}>{busyAction === 'add-coil' ? 'Salvando...' : 'Adicionar bobina'}</button>
        </form>
        <DataTable rows={plano.bobinas ?? []} columns={[
          { key: 'codigo', label: 'Bobina' },
          { key: 'pesoKg', label: 'Peso', render: (row) => formatNumber(row.pesoKg, ' kg') },
          { key: 'dimensoes', label: 'Dimensões', render: (row) => `${formatNumber(row.diametroExternoMm)} × ${formatNumber(row.larguraMm)} mm` },
          { key: 'grauAco', label: 'Grau' },
          { key: 'portoDescarga', label: 'Descarga' },
          { key: 'posicionada', label: 'Situação', render: (row) => <StatusBadge value={row.posicionada ? 'POSICIONADA' : 'PENDENTE'} /> }
        ]} emptyTitle="Manifesto sem bobinas" />
      </Section>

      <Section title="Plano por porão" description="O serviço valida tank top, setor, camada, dunnage e securing antes de aceitar a posição.">
        <form className="planner-form-grid position-form" onSubmit={placeCoil}>
          <label className="field"><span>Bobina pendente</span><select value={novaPosicao.bobinaId} onChange={(event) => setNovaPosicao((current) => ({ ...current, bobinaId: event.target.value }))} required disabled={approved}><option value="">Selecione</option>{unpositionedCoils.map((item) => <option key={item.id} value={item.id}>{item.codigo}</option>)}</select></label>
          <label className="field"><span>Porão</span><select value={novaPosicao.poraoId} onChange={(event) => setNovaPosicao((current) => ({ ...current, poraoId: event.target.value }))} required disabled={approved}><option value="">Selecione</option>{(selectedNavio?.poroes ?? []).map((item) => <option key={item.id} value={item.id}>Porão {item.numero}</option>)}</select></label>
          <label className="field"><span>Setor</span><select value={novaPosicao.setorId} onChange={(event) => setNovaPosicao((current) => ({ ...current, setorId: event.target.value }))} required disabled={approved}><option value="">Selecione</option>{(selectedHold?.setores ?? []).map((item) => <option key={item.id} value={item.id}>{item.nome}</option>)}</select></label>
          <label className="field"><span>Camada</span><input type="number" min="1" value={novaPosicao.camada} onChange={(event) => setNovaPosicao((current) => ({ ...current, camada: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Posição X (m)</span><input type="number" step="0.01" value={novaPosicao.posicaoX} onChange={(event) => setNovaPosicao((current) => ({ ...current, posicaoX: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Posição Y (m)</span><input type="number" step="0.01" value={novaPosicao.posicaoY} onChange={(event) => setNovaPosicao((current) => ({ ...current, posicaoY: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Dunnage (mm)</span><input type="number" min="1" step="0.01" value={novaPosicao.espessuraDunnageMm} onChange={(event) => setNovaPosicao((current) => ({ ...current, espessuraDunnageMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Securing</span><select value={novaPosicao.tipoLashing} onChange={(event) => setNovaPosicao((current) => ({ ...current, tipoLashing: event.target.value }))} disabled={approved}><option value="SEM_LASHING">Sem lashing</option><option value="CORRENTE">Corrente</option><option value="CINTA_ACO">Cinta de aço</option><option value="CINTA_SINTETICA">Cinta sintética</option><option value="MADEIRA_CUNHA">Cunha de madeira</option><option value="CALCO_BORRACHA">Calço de borracha</option></select></label>
          <button type="submit" disabled={approved || !unpositionedCoils.length || busyAction === 'place-coil'}>{busyAction === 'place-coil' ? 'Validando...' : 'Posicionar bobina'}</button>
        </form>
        <DataTable rows={plano.posicoes ?? []} columns={[
          { key: 'codigoBobina', label: 'Bobina' },
          { key: 'poraoNumero', label: 'Porão', render: (row) => `Porão ${row.poraoNumero ?? '—'}` },
          { key: 'setorNome', label: 'Setor' },
          { key: 'camada', label: 'Camada' },
          { key: 'coordenada', label: 'Coordenada', render: (row) => `X ${formatNumber(row.posicaoX)} · Y ${formatNumber(row.posicaoY)}` },
          { key: 'tipoLashing', label: 'Securing' },
          { key: 'alertaTanktop', label: 'Alerta', render: (row) => row.alertaTanktop || '—' }
        ]} emptyTitle="Nenhuma bobina posicionada" />
      </Section>

      <Section title="Análises e aprovação" description="Todas as análises abaixo são respostas do backend; tacktop é consultado por GET.">
        <div className="planner-analysis-actions">
          <button className="secondary" onClick={loadTanktop} disabled={!!busyAction}>Tank top</button>
          <button className="secondary" onClick={loadStacking} disabled={!!busyAction}>Empilhamento</button>
          <button className="secondary" onClick={loadStability} disabled={!!busyAction}>Estabilidade</button>
          <button className="secondary" onClick={loadSecuring} disabled={!!busyAction}>Securing / tacktop</button>
          <button onClick={validatePlan} disabled={!!busyAction || approved}>{approved ? 'Plano aprovado' : 'Validar e aprovar'}</button>
          <button className="secondary" onClick={loadReport} disabled={!!busyAction}>Carregar relatório</button>
        </div>
        <div className="planner-analysis-grid">
          <article className="analysis-card"><span>Estabilidade</span>{estabilidade || plano.estabilidade ? <><strong><StatusBadge value={(estabilidade ?? plano.estabilidade)?.aprovado ? 'APROVADA' : 'REPROVADA'} /></strong><small>BM {formatNumber((estabilidade ?? plano.estabilidade)?.bmMaxKnm, ' kNm')} · SF {formatNumber((estabilidade ?? plano.estabilidade)?.sfMaxKn, ' kN')}</small></> : <small>Execute a análise.</small>}</article>
          <article className="analysis-card"><span>Empilhamento</span>{empilhamento ? <><strong>{empilhamento.totalCamadas ?? '—'} camada(s)</strong><small>{empilhamento.descricaoEmpilhamento || 'Sem descrição'}</small></> : <small>Selecione o porão e consulte.</small>}</article>
          <article className="analysis-card"><span>Securing</span>{securing ? <><strong>{securing.numeroBobinasTopLayer ?? '—'} bobina(s) no topo</strong><small>{formatNumber(securing.pesoTotalLashingKg, ' kg')} de materiais</small></> : <small>Consulte o tacktop.</small>}</article>
        </div>
        {tanktop && <DataTable rows={tanktop} columns={[
          { key: 'nomeSetor', label: 'Setor' },
          { key: 'pressaoCalculadaTM2', label: 'Pressão', render: (row) => formatNumber(row.pressaoCalculadaTM2, ' t/m²') },
          { key: 'capacidadeNominalTM2', label: 'Capacidade', render: (row) => formatNumber(row.capacidadeNominalTM2, ' t/m²') },
          { key: 'excedido', label: 'Resultado', render: (row) => <StatusBadge value={row.excedido ? 'EXCEDIDO' : 'DENTRO DO LIMITE'} /> }
        ]} />}
        <JsonDetails value={empilhamento} title="Detalhes do empilhamento" />
        <JsonDetails value={securing} title="Materiais e observações de securing" />
        <JsonDetails value={relatorio} title="Relatório operacional retornado" />
      </Section>
    </>}
  </>;
}
