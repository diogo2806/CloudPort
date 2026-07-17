import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, formatError, normalizePage, sanitizeText } from '../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';

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

function visitKey(visit, index) {
  return String(visit?.id ?? visit?.codigoVisita ?? visit?.codigo ?? `${visit?.codigoImo ?? 'navio'}-${visit?.viagemEntrada ?? visit?.codigoViagem ?? index}`);
}

function visitCode(visit) {
  return sanitizeText(visit?.viagemEntrada ?? visit?.codigoViagem ?? visit?.viagemSaida ?? visit?.codigoVisita);
}

function formatNumber(value, suffix = '') {
  if (value === undefined || value === null || value === '') return '—';
  const number = Number(value);
  return Number.isFinite(number) ? `${number.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}${suffix}` : '—';
}

function normalizeList(payload) {
  return Array.isArray(payload) ? payload : normalizePage(payload);
}

export function SteelCoilPlannerPage() {
  const [navios, setNavios] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [visitas, setVisitas] = useState([]);
  const [planos, setPlanos] = useState([]);
  const [plano, setPlano] = useState(null);
  const [navioId, setNavioId] = useState('');
  const [visitaSelecionada, setVisitaSelecionada] = useState('');
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
    () => visitas.find((item, index) => visitKey(item, index) === visitaSelecionada) ?? null,
    [visitas, visitaSelecionada]
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
      const [naviosResponse, templatesResponse, visitsResponse] = await Promise.all([
        api.listarNaviosEstivagemBulk(),
        api.listarTemplatesEstivagemBulk(),
        api.listarEscalasEmbarque(60)
      ]);
      setNavios(normalizeList(naviosResponse));
      setTemplates(normalizeList(templatesResponse));
      setVisitas(normalizeList(visitsResponse));
    } catch (reason) {
      setError(formatError(reason, 'Não foi possível carregar navios, visitas e templates do planejamento.'));
    } finally {
      setLoading(false);
    }
  }, []);

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

  const loadPlans = useCallback(async () => {
    if (!navioId || !selectedVisit) {
      setPlanos([]);
      return;
    }
    setLoadingPlanos(true);
    setError('');
    try {
      const response = await api.listarPlanosEstivagemBulk(Number(navioId), visitCode(selectedVisit));
      setPlanos(normalizeList(response));
    } catch (reason) {
      setPlanos([]);
      setError(formatError(reason, 'Não foi possível carregar os planos do navio e da visita selecionados.'));
    } finally {
      setLoadingPlanos(false);
    }
  }, [navioId, selectedVisit]);

  useEffect(() => {
    setPlano(null);
    resetAnalyses();
    loadPlans();
  }, [loadPlans, resetAnalyses]);

  const refreshPlan = useCallback(async (planId = plano?.id) => {
    if (!planId) return null;
    const response = await api.buscarPlanoEstivagemBulk(planId);
    setPlano(response);
    setNovaPosicao((current) => ({
      ...current,
      bobinaId: (response?.bobinas ?? []).find((item) => !item.posicionada)?.id
        ? String((response?.bobinas ?? []).find((item) => !item.posicionada).id)
        : ''
    }));
    return response;
  }, [plano?.id]);

  async function runAction(action, operation, message, reload = false) {
    if (busyAction) return null;
    setBusyAction(action);
    setError('');
    setSuccess('');
    try {
      const result = await operation();
      if (reload && plano?.id) await refreshPlan(plano.id);
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
    if (!navioId || !selectedVisit) return;
    const result = await runAction('create-plan', () => api.criarPlanoEstivagemBulk({
      navioId: Number(navioId),
      codigoViagem: sanitizeText(novoPlano.codigoViagem),
      portoCarga: sanitizeText(novoPlano.portoCarga),
      portoDescarga: sanitizeText(novoPlano.portoDescarga)
    }), 'Plano criado e recarregado a partir do backend.');
    if (result?.id) {
      setPlano(result);
      resetAnalyses();
      await loadPlans();
    }
  }

  async function openPlan(planId) {
    const result = await runAction('open-plan', () => api.buscarPlanoEstivagemBulk(planId));
    if (result) {
      setPlano(result);
      resetAnalyses();
      const firstAvailable = (result.bobinas ?? []).find((item) => !item.posicionada);
      setNovaPosicao((current) => ({ ...current, bobinaId: firstAvailable?.id ? String(firstAvailable.id) : '' }));
    }
  }

  async function addCoil(event) {
    event.preventDefault();
    if (!plano?.id || approved) return;
    const result = await runAction('add-coil', () => api.adicionarBobinaEstivagemBulk(plano.id, {
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
    const result = await runAction('place-coil', () => api.posicionarBobinaEstivagemBulk(plano.id, {
      bobinaId: Number(novaPosicao.bobinaId),
      poraoId: Number(novaPosicao.poraoId),
      setorId: Number(novaPosicao.setorId),
      camada: Number(novaPosicao.camada),
      posicaoX: Number(novaPosicao.posicaoX),
      posicaoY: Number(novaPosicao.posicaoY),
      espessuraDunnageMm: Number(novaPosicao.espessuraDunnageMm),
      tipoLashing: novaPosicao.tipoLashing
    }), 'Posição confirmada pelo backend e plano recarregado.');
    if (result) {
      await refreshPlan(plano.id);
      await loadPlans();
      resetAnalyses();
    }
  }

  async function loadTanktop() {
    const result = await runAction('tanktop', () => api.analisarTanktopEstivagemBulk(plano.id));
    if (result) setTanktop(normalizeList(result));
  }

  async function loadStacking() {
    const poraoId = novaPosicao.poraoId || selectedNavio?.poroes?.[0]?.id;
    if (!poraoId) {
      setError('Selecione um porão para analisar o empilhamento.');
      return;
    }
    const result = await runAction('stacking', () => api.analisarEmpilhamentoEstivagemBulk(plano.id, poraoId));
    if (result) setEmpilhamento(result);
  }

  async function loadStability() {
    const result = await runAction('stability', () => api.calcularEstabilidadeEstivagemBulk(plano.id));
    if (result) setEstabilidade(result);
  }

  async function loadSecuring() {
    const result = await runAction('securing', () => api.calcularSecuringEstivagemBulk(plano.id), 'Securing calculado e materiais persistidos.');
    if (result) {
      setSecuring(result);
      await refreshPlan(plano.id);
    }
  }

  async function validatePlan() {
    const result = await runAction('validate', () => api.validarPlanoEstivagemBulk(plano.id), 'Plano validado e status recarregado do backend.');
    if (result) {
      setPlano(result);
      setEstabilidade(result.estabilidade ?? null);
      await loadPlans();
    }
  }

  async function openReport() {
    const reportWindow = typeof window !== 'undefined' ? window.open('', '_blank') : null;
    const result = await runAction('report', () => api.obterRelatorioEstivagemBulk(plano.id));
    if (!result) {
      reportWindow?.close();
      return;
    }
    setRelatorio(result);
    if (reportWindow && typeof Blob !== 'undefined' && globalThis.URL?.createObjectURL) {
      const blob = new Blob([JSON.stringify(result, null, 2)], { type: 'application/json;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      reportWindow.location.href = url;
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    }
  }

  if (loading) return <><PageHeader eyebrow="Embarque" title="Planejamento de steel coils" description="Planejamento operacional persistido para navios siderúrgicos." /><Loading label="Carregando contexto operacional..." /></>;

  return <>
    <PageHeader
      eyebrow="Embarque"
      title="Planejamento de steel coils"
      description="Selecione o navio e a visita, mantenha o manifesto e execute somente cálculos e confirmações retornados pelo backend."
      actions={<button className="secondary" onClick={loadInitialData} disabled={!!busyAction}>Atualizar contexto</button>}
    />
    <Message type="error" onClose={() => setError('')}>{error}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <Section title="Contexto operacional" description="Navio, visita e plano são selecionados explicitamente antes de qualquer comando.">
      <div className="planner-context-grid">
        <label className="field"><span>Navio siderúrgico</span><select value={navioId} onChange={(event) => setNavioId(event.target.value)}><option value="">Selecione</option>{navios.map((item) => <option key={item.id} value={item.id}>{item.nome} {item.imo ? `· IMO ${item.imo}` : ''}</option>)}</select><small>{selectedNavio ? `${selectedNavio.totalPoroes ?? selectedNavio.poroes?.length ?? 0} porão(ões) configurado(s)` : `${templates.length} template(s) disponível(is)`}</small></label>
        <label className="field"><span>Visita / viagem</span><select value={visitaSelecionada} onChange={(event) => setVisitaSelecionada(event.target.value)}><option value="">Selecione</option>{visitas.map((item, index) => <option key={visitKey(item, index)} value={visitKey(item, index)}>{item.nomeNavio ?? item.navioNome ?? 'Navio'} · {visitCode(item) || 'viagem sem código'} · {item.fase ?? 'fase não informada'}</option>)}</select></label>
        <label className="field"><span>Plano persistido</span><select value={plano?.id ?? ''} onChange={(event) => event.target.value ? openPlan(Number(event.target.value)) : setPlano(null)} disabled={!navioId || !selectedVisit || loadingPlanos}><option value="">{loadingPlanos ? 'Carregando...' : 'Criar ou selecionar'}</option>{planos.map((item) => <option key={item.id} value={item.id}>#{item.id} · {item.status ?? 'RASCUNHO'} · {item.totalBobinas ?? 0} bobina(s)</option>)}</select></label>
      </div>
    </Section>

    {!plano && <Section title="Criar plano" description="O plano será criado para o navio e a visita selecionados e, em seguida, recarregado do servidor.">
      <form className="planner-form-grid" onSubmit={createPlan}>
        <label className="field"><span>Código da viagem</span><input value={novoPlano.codigoViagem} onChange={(event) => setNovoPlano((current) => ({ ...current, codigoViagem: event.target.value }))} maxLength={30} required /></label>
        <label className="field"><span>Porto de carga</span><input value={novoPlano.portoCarga} onChange={(event) => setNovoPlano((current) => ({ ...current, portoCarga: event.target.value }))} maxLength={10} required /></label>
        <label className="field"><span>Porto de descarga</span><input value={novoPlano.portoDescarga} onChange={(event) => setNovoPlano((current) => ({ ...current, portoDescarga: event.target.value }))} maxLength={10} required /></label>
        <button type="submit" disabled={!navioId || !selectedVisit || busyAction === 'create-plan'}>{busyAction === 'create-plan' ? 'Criando...' : 'Criar plano'}</button>
      </form>
      {!loadingPlanos && navioId && selectedVisit && !planos.length && <EmptyState title="Nenhum plano para esta visita" description="Preencha os portos e crie o primeiro plano persistido." />}
    </Section>}

    {plano && <>
      <div className="metrics-grid">
        <MetricCard label="Plano" value={`#${plano.id}`} detail={`${plano.nomeNavio ?? 'Navio'} · ${plano.codigoViagem ?? 'Viagem'}`} />
        <MetricCard label="Status" value={<StatusBadge value={plano.status} />} detail={approved ? 'Comandos de alteração bloqueados' : 'Plano editável'} />
        <MetricCard label="Manifesto" value={plano.totalBobinas ?? plano.bobinas?.length ?? 0} detail={`${unpositionedCoils.length} sem posição`} />
        <MetricCard label="Peso total" value={formatNumber(plano.pesoTotalToneladas, ' t')} detail={`${plano.portoCarga ?? '—'} → ${plano.portoDescarga ?? '—'}`} />
      </div>

      <Section title="Manifesto de bobinas" description="A inclusão é confirmada pelo backend; o plano é recarregado após a persistência.">
        <form className="planner-form-grid coil-form" onSubmit={addCoil}>
          <label className="field"><span>Código</span><input value={novaBobina.codigo} onChange={(event) => setNovaBobina((current) => ({ ...current, codigo: event.target.value }))} maxLength={30} required disabled={approved} /></label>
          <label className="field"><span>Peso (kg)</span><input type="number" min="1" step="0.01" value={novaBobina.pesoKg} onChange={(event) => setNovaBobina((current) => ({ ...current, pesoKg: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Diâmetro externo (mm)</span><input type="number" min="1" step="0.01" value={novaBobina.diametroExternoMm} onChange={(event) => setNovaBobina((current) => ({ ...current, diametroExternoMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Diâmetro interno (mm)</span><input type="number" min="0" step="0.01" value={novaBobina.diametroInternoMm} onChange={(event) => setNovaBobina((current) => ({ ...current, diametroInternoMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Largura (mm)</span><input type="number" min="1" step="0.01" value={novaBobina.larguraMm} onChange={(event) => setNovaBobina((current) => ({ ...current, larguraMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Grau do aço</span><input value={novaBobina.grauAco} onChange={(event) => setNovaBobina((current) => ({ ...current, grauAco: event.target.value }))} maxLength={20} required disabled={approved} /></label>
          <label className="field"><span>Porto de descarga</span><input value={novaBobina.portoDescarga} onChange={(event) => setNovaBobina((current) => ({ ...current, portoDescarga: event.target.value }))} maxLength={10} required disabled={approved} /></label>
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

      <Section title="Plano por porão" description="O frontend informa a intenção; tank top, restrições e aceite da posição são avaliados pelo serviço de domínio.">
        <form className="planner-form-grid position-form" onSubmit={placeCoil}>
          <label className="field"><span>Bobina pendente</span><select value={novaPosicao.bobinaId} onChange={(event) => setNovaPosicao((current) => ({ ...current, bobinaId: event.target.value }))} required disabled={approved}><option value="">Selecione</option>{unpositionedCoils.map((item) => <option key={item.id} value={item.id}>{item.codigo} · {formatNumber(item.pesoKg, ' kg')}</option>)}</select></label>
          <label className="field"><span>Porão</span><select value={novaPosicao.poraoId} onChange={(event) => setNovaPosicao((current) => ({ ...current, poraoId: event.target.value }))} required disabled={approved}><option value="">Selecione</option>{(selectedNavio?.poroes ?? []).map((item) => <option key={item.id} value={item.id}>Porão {item.numero} · {formatNumber(item.areaUtil, ' m²')}</option>)}</select></label>
          <label className="field"><span>Setor de tank top</span><select value={novaPosicao.setorId} onChange={(event) => setNovaPosicao((current) => ({ ...current, setorId: event.target.value }))} required disabled={approved}><option value="">Selecione</option>{(selectedHold?.setores ?? []).map((item) => <option key={item.id} value={item.id}>{item.nome} · limite {formatNumber(item.capacidadeTM2, ' t/m²')}</option>)}</select></label>
          <label className="field"><span>Camada</span><input type="number" min="1" step="1" value={novaPosicao.camada} onChange={(event) => setNovaPosicao((current) => ({ ...current, camada: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Posição X (m)</span><input type="number" step="0.01" value={novaPosicao.posicaoX} onChange={(event) => setNovaPosicao((current) => ({ ...current, posicaoX: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Posição Y (m)</span><input type="number" step="0.01" value={novaPosicao.posicaoY} onChange={(event) => setNovaPosicao((current) => ({ ...current, posicaoY: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Dunnage (mm)</span><input type="number" min="1" step="0.01" value={novaPosicao.espessuraDunnageMm} onChange={(event) => setNovaPosicao((current) => ({ ...current, espessuraDunnageMm: event.target.value }))} required disabled={approved} /></label>
          <label className="field"><span>Securing informado</span><select value={novaPosicao.tipoLashing} onChange={(event) => setNovaPosicao((current) => ({ ...current, tipoLashing: event.target.value }))} disabled={approved}><option value="SEM_LASHING">Sem lashing</option><option value="CORRENTE">Corrente</option><option value="CINTA_ACO">Cinta de aço</option><option value="CINTA_SINTETICA">Cinta sintética</option><option value="MADEIRA_CUNHA">Cunha de madeira</option><option value="CALCO_BORRACHA">Calço de borracha</option></select></label>
          <button type="submit" disabled={approved || !unpositionedCoils.length || busyAction === 'place-coil'}>{busyAction === 'place-coil' ? 'Validando...' : 'Posicionar bobina'}</button>
        </form>
        <DataTable rows={plano.posicoes ?? []} columns={[
          { key: 'codigoBobina', label: 'Bobina' },
          { key: 'poraoNumero', label: 'Porão', render: (row) => `Porão ${row.poraoNumero ?? '—'}` },
          { key: 'setorNome', label: 'Setor' },
          { key: 'camada', label: 'Camada' },
          { key: 'coordenada', label: 'Coordenada', render: (row) => `X ${formatNumber(row.posicaoX)} · Y ${formatNumber(row.posicaoY)}` },
          { key: 'dunnage', label: 'Dunnage', render: (row) => formatNumber(row.espessuraDunnageMm, ' mm') },
          { key: 'tipoLashing', label: 'Securing' },
          { key: 'alertaTanktop', label: 'Alerta', render: (row) => row.alertaTanktop || '—' }
        ]} emptyTitle="Nenhuma bobina posicionada" />
      </Section>

      <Section title="Análises e aprovação" description="Os valores exibidos são respostas do backend. A validação não é reproduzida no navegador.">
        <div className="planner-analysis-actions">
          <button className="secondary" onClick={loadTanktop} disabled={!!busyAction}>{busyAction === 'tanktop' ? 'Consultando...' : 'Tank top'}</button>
          <button className="secondary" onClick={loadStacking} disabled={!!busyAction}>{busyAction === 'stacking' ? 'Consultando...' : 'Empilhamento do porão'}</button>
          <button className="secondary" onClick={loadStability} disabled={!!busyAction}>{busyAction === 'stability' ? 'Calculando...' : 'Estabilidade'}</button>
          <button className="secondary" onClick={loadSecuring} disabled={!!busyAction || approved}>{busyAction === 'securing' ? 'Calculando...' : 'Securing / tacktop'}</button>
          <button onClick={validatePlan} disabled={!!busyAction || approved}>{busyAction === 'validate' ? 'Validando...' : approved ? 'Plano aprovado' : 'Validar e aprovar'}</button>
          <button className="secondary" onClick={openReport} disabled={!!busyAction}>{busyAction === 'report' ? 'Abrindo...' : 'Abrir relatório'}</button>
        </div>

        <div className="planner-analysis-grid">
          <article className="analysis-card"><span>Estabilidade</span>{estabilidade || plano.estabilidade ? <><strong><StatusBadge value={(estabilidade ?? plano.estabilidade)?.aprovado ? 'APROVADA' : 'REPROVADA'} /></strong><small>BM {formatNumber((estabilidade ?? plano.estabilidade)?.bmMaxKnm, ' kNm')} · SF {formatNumber((estabilidade ?? plano.estabilidade)?.sfMaxKn, ' kN')} · trim {formatNumber((estabilidade ?? plano.estabilidade)?.trimMetros, ' m')}</small></> : <small>Execute a análise.</small>}</article>
          <article className="analysis-card"><span>Empilhamento</span>{empilhamento ? <><strong>{empilhamento.totalCamadas} camada(s)</strong><small>{empilhamento.descricaoEmpilhamento || 'Sem descrição'} · corredor {empilhamento.corredorOperacaoLivre ? 'livre' : 'bloqueado'}</small></> : <small>Selecione o porão e consulte.</small>}</article>
          <article className="analysis-card"><span>Securing</span>{securing ? <><strong>{securing.numeroBobinasTopLayer} bobina(s) no topo</strong><small>{formatNumber(securing.pesoTotalLashingKg, ' kg')} de materiais · ângulo {formatNumber(securing.anguloInclinacaoGraus, '°')}</small></> : <small>Execute o cálculo do servidor.</small>}</article>
        </div>

        {tanktop && <div className="analysis-result"><h3>Pressão por setor</h3><DataTable rows={tanktop} columns={[
          { key: 'nomeSetor', label: 'Setor' },
          { key: 'pressaoCalculadaTM2', label: 'Pressão', render: (row) => formatNumber(row.pressaoCalculadaTM2, ' t/m²') },
          { key: 'capacidadeNominalTM2', label: 'Capacidade', render: (row) => formatNumber(row.capacidadeNominalTM2, ' t/m²') },
          { key: 'percentualOcupacao', label: 'Utilização', render: (row) => formatNumber(row.percentualOcupacao, '%') },
          { key: 'excedido', label: 'Resultado', render: (row) => <StatusBadge value={row.excedido ? 'EXCEDIDO' : 'DENTRO DO LIMITE'} /> }
        ]} /></div>}
        <JsonDetails value={empilhamento} title="Detalhes do empilhamento" />
        <JsonDetails value={securing} title="Materiais e observações de securing" />
        <JsonDetails value={relatorio} title="Relatório operacional retornado" />
      </Section>
    </>}
  </>;
}
