import { useMemo, useState } from 'react';
import { api, createCorrelationId, formatError, hasAnyRole, request } from '../../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, Section, StatusBadge } from '../../components.jsx';
import { CommandPanel, DetailGrid, displayValue, FilterField, normalized, Pagination, useCommand, usePagination, useRemote, YardPageHeader } from './YardShared.jsx';

function expectedPhysicalAction(order) {
  return ['REMOCAO', 'LIBERACAO'].includes(order?.tipoMovimento) ? 'UNGROUNDING' : 'GROUNDING';
}

function numberOrNull(value) {
  if (value === '' || value === undefined || value === null) return null;
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

function localTimestamp() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 19);
}

function physicalForm(order, detail) {
  return {
    tipoAcaoFisica: expectedPhysicalAction(order),
    codigoUnidadeLido: order?.codigoConteiner ?? '',
    equipamentoPatioId: detail?.equipamentoPatioId ?? '',
    equipamentoIdentificador: detail?.equipamentoIdentificador ?? '',
    origem: order?.tipoOrigem ?? '',
    destino: order?.destino ?? order?.tipoDestino ?? '',
    linhaOrigem: '',
    colunaOrigem: '',
    camadaOrigem: '',
    linhaDestino: order?.linhaDestino ?? '',
    colunaDestino: order?.colunaDestino ?? '',
    camadaDestino: order?.camadaDestino ?? '',
    sequenciaOperacional: order?.sequenciaNavio ?? ''
  };
}

function ManualConfirmacaoFisica() {
  return <details className="json-details">
    <summary>📘 Manual — confirmação física de grounding e ungrounding</summary>
    <div className="content-card">
      <h3>Finalidade da tela</h3>
      <p>Confirmar a retirada ou a colocação física de uma unidade somente após conferir a work instruction, o CHE, a origem, o destino, a posição e a sequência operacional.</p>
      <h3>Fluxo operacional</h3>
      <ol>
        <li>Selecione uma work instruction despachada e com o ciclo VMT em execução.</li>
        <li>Abra a confirmação física e faça nova leitura da unidade e do CHE.</li>
        <li>Confirme origem, destino, coordenadas e sequência.</li>
        <li>O backend valida a job list, o equipamento e o inventário sob bloqueio transacional.</li>
        <li>Somente uma confirmação válida conclui a instrução e altera o inventário.</li>
      </ol>
      <h3>Explicação dos campos</h3>
      <ul>
        <li>Ação física: GROUNDING coloca a unidade na posição; UNGROUNDING retira a unidade da posição atual.</li>
        <li>Unidade lida: identificação física conferida novamente pelo operador.</li>
        <li>CHE: ID e identificador do equipamento associado à work queue.</li>
        <li>Origem e destino: áreas operacionais envolvidas na transferência.</li>
        <li>Linha, coluna e camada: posição física utilizada para validar inventário e work instruction.</li>
        <li>Sequência: ordem oficial da instrução na job list.</li>
      </ul>
      <h3>Permissões necessárias</h3>
      <p>ADMIN_PORTO, PLANEJADOR ou OPERADOR_PATIO podem confirmar. Outros perfis permanecem em consulta.</p>
      <h3>Estados possíveis</h3>
      <ul><li>PENDENTE, ACEITA e EM_EXECUCAO no ciclo VMT.</li><li>CONCLUIDA após confirmação física válida.</li><li>BLOQUEADA quando uma falha operacional é registrada.</li></ul>
      <h3>Motivos de bloqueio</h3>
      <ul><li>Evento repetido ou timestamp fora de ordem.</li><li>Unidade ou CHE divergente.</li><li>CHE indisponível ou não associado à fila.</li><li>Origem ou destino incompatível com o inventário e a instrução.</li><li>Instrução anterior ainda aberta.</li><li>Ação física incompatível com o tipo de movimento.</li></ul>
      <h3>Exemplo</h3>
      <p>Para a unidade CONT-001 destinada ao bloco A, o operador lê CONT-001 e RTG-01, confirma GROUNDING na posição linha 10, coluna 3, camada T2 e a sequência 4. O inventário é atualizado e a instrução é concluída na mesma transação.</p>
      <h3>Atalhos</h3>
      <ul><li>F1 ou Shift + ?: abrir a ajuda contextual.</li><li>Esc: cancelar a confirmação sem enviar dados.</li><li>Atualizar: recarregar o estado persistido.</li></ul>
      <p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/patio-confirmacao-grounding-ungrounding.md" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
    </div>
  </details>;
}

function PhysicalConfirmationPanel({ order, detail, form, onChange, busy, onCancel, onConfirm }) {
  if (!order) return null;
  const ungrounding = form.tipoAcaoFisica === 'UNGROUNDING';
  function update(field, value) {
    onChange({ ...form, [field]: value });
  }
  const ready = order.statusOrdem === 'EM_EXECUCAO'
    && order.statusConfirmacaoVmt === 'EM_EXECUCAO'
    && form.codigoUnidadeLido.trim()
    && form.equipamentoPatioId !== ''
    && form.equipamentoIdentificador.trim()
    && form.origem.trim()
    && form.destino.trim()
    && form.sequenciaOperacional !== ''
    && (!ungrounding || (form.linhaOrigem !== '' && form.colunaOrigem !== '' && form.camadaOrigem.trim()))
    && (ungrounding || (form.linhaDestino !== '' && form.colunaDestino !== '' && form.camadaDestino.trim()));

  return <Section title={`Confirmação física da work instruction #${order.id}`} description="A instrução somente será concluída após o backend validar a transferência e atualizar o inventário.">
    <Message type="warning">Estado VMT atual: {order.statusConfirmacaoVmt ?? 'PENDENTE'}. A confirmação exige EM_EXECUCAO.</Message>
    {!detail?.equipamentoPatioId && <Message type="warning">A work queue não retornou um CHE real associado. Reassocie os recursos antes de confirmar.</Message>}
    <div className="inline-form">
      <label className="field"><span>Ação física</span><select value={form.tipoAcaoFisica} onChange={(event) => update('tipoAcaoFisica', event.target.value)} disabled={busy}><option value="GROUNDING">GROUNDING — colocar</option><option value="UNGROUNDING">UNGROUNDING — retirar</option></select></label>
      <label className="field"><span>Unidade lida</span><input value={form.codigoUnidadeLido} onChange={(event) => update('codigoUnidadeLido', event.target.value)} maxLength={40} disabled={busy} required /></label>
      <label className="field"><span>ID do CHE</span><input type="number" min="1" value={form.equipamentoPatioId} onChange={(event) => update('equipamentoPatioId', event.target.value)} disabled={busy} required /></label>
      <label className="field"><span>Identificador do CHE</span><input value={form.equipamentoIdentificador} onChange={(event) => update('equipamentoIdentificador', event.target.value)} maxLength={80} disabled={busy} required /></label>
      <label className="field"><span>Origem</span><input value={form.origem} onChange={(event) => update('origem', event.target.value)} maxLength={120} disabled={busy} required /></label>
      <label className="field"><span>Destino</span><input value={form.destino} onChange={(event) => update('destino', event.target.value)} maxLength={120} disabled={busy} required /></label>
      <label className="field"><span>Linha de origem</span><input type="number" min="0" value={form.linhaOrigem} onChange={(event) => update('linhaOrigem', event.target.value)} disabled={busy} required={ungrounding} /></label>
      <label className="field"><span>Coluna de origem</span><input type="number" min="0" value={form.colunaOrigem} onChange={(event) => update('colunaOrigem', event.target.value)} disabled={busy} required={ungrounding} /></label>
      <label className="field"><span>Camada de origem</span><input value={form.camadaOrigem} onChange={(event) => update('camadaOrigem', event.target.value)} maxLength={40} disabled={busy} required={ungrounding} /></label>
      <label className="field"><span>Linha de destino</span><input type="number" min="0" value={form.linhaDestino} onChange={(event) => update('linhaDestino', event.target.value)} disabled={busy} required={!ungrounding} /></label>
      <label className="field"><span>Coluna de destino</span><input type="number" min="0" value={form.colunaDestino} onChange={(event) => update('colunaDestino', event.target.value)} disabled={busy} required={!ungrounding} /></label>
      <label className="field"><span>Camada de destino</span><input value={form.camadaDestino} onChange={(event) => update('camadaDestino', event.target.value)} maxLength={40} disabled={busy} required={!ungrounding} /></label>
      <label className="field"><span>Sequência operacional</span><input type="number" min="0" value={form.sequenciaOperacional} onChange={(event) => update('sequenciaOperacional', event.target.value)} disabled={busy} required /></label>
    </div>
    <div className="actions"><button className="secondary" type="button" disabled={busy} onClick={onCancel}>Cancelar</button><button type="button" disabled={busy || !ready} onClick={onConfirm}>{busy ? 'Confirmando...' : 'Confirmar transferência física'}</button></div>
  </Section>;
}

export function YardWorkListPage({ navigate, session }) {
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [selectedQueue, setSelectedQueue] = useState(null);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [detail, setDetail] = useState(null);
  const [physicalOrder, setPhysicalOrder] = useState(null);
  const [physicalData, setPhysicalData] = useState(() => physicalForm(null, null));
  const [physicalBusy, setPhysicalBusy] = useState(false);
  const canAdminister = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR');
  const canOperate = canAdminister || hasAnyRole(session, 'OPERADOR_PATIO');
  const remote = useRemote(async () => {
    const [queues, orders, matrix] = await Promise.all([
      api.listarWorkQueuesPatio(),
      api.listarOrdensPatio(),
      api.obterMatrizEstadosWorkInstructionPatio()
    ]);
    return { queues: queues ?? [], orders: orders ?? [], matrix: matrix ?? {} };
  }, []);
  const commands = useCommand(remote);
  const queues = useMemo(() => (remote.data?.queues ?? []).filter((queue) =>
    (!status || queue.status === status)
    && (!search || normalized(`${queue.identificador} ${queue.blocoZona} ${queue.pow} ${queue.equipamento}`).includes(normalized(search)))
  ), [remote.data, status, search]);
  const orders = useMemo(() => (remote.data?.orders ?? []).filter((order) =>
    (!status || order.statusOrdem === status)
    && (!search || normalized(`${order.codigoConteiner} ${order.tipoMovimento} ${order.destino}`).includes(normalized(search)))
  ), [remote.data, status, search]);
  const queuePage = usePagination(queues);
  const orderPage = usePagination(orders);

  async function openOrder(order) {
    setSelectedOrder(order);
    setDetail(null);
    setPhysicalOrder(null);
    try {
      setDetail(await api.obterDrillDownWorkInstructionPatio(order.id));
    } catch (reason) {
      commands.setError(formatError(reason));
    }
  }

  function queueCommand(queue, type) {
    if ((type === 'dispatch' && !canOperate) || (type !== 'dispatch' && !canAdminister)) return;
    const config = type === 'dispatch'
      ? {
          title: `Despachar ${queue.identificador}`,
          description: 'O backend selecionará e persistirá as work instructions despachadas.',
          success: 'Dispatch confirmado e estado persistido recarregado.',
          run: (reason) => api.despacharWorkQueuePatio(queue.id, { limiteOrdens: null, observacao: reason }, reason)
        }
      : type === 'activate'
        ? {
            title: `Ativar ${queue.identificador}`,
            description: 'A fila ficará disponível para dispatch após confirmação do backend.',
            success: 'Work queue ativada e estado persistido recarregado.',
            run: (reason) => api.ativarWorkQueuePatio(queue.id, reason)
          }
        : {
            title: `Desativar ${queue.identificador}`,
            description: 'A fila deixará de receber dispatch após confirmação do backend.',
            success: 'Work queue desativada e estado persistido recarregado.',
            run: (reason) => api.desativarWorkQueuePatio(queue.id, reason)
          };
    commands.setCommand(config);
  }

  function instructionCommand(order, type) {
    if (!canOperate) return;
    const actions = {
      suspend: ['Suspender', api.suspenderWorkInstructionPatio],
      resume: ['Retomar', api.retomarWorkInstructionPatio],
      block: ['Bloquear', api.bloquearWorkInstructionPatio],
      reset: ['Resetar', api.resetarWorkInstructionPatio],
      cancel: ['Cancelar', api.cancelarWorkInstructionPatio]
    };
    const [label, operation] = actions[type];
    const successMessages = {
      suspend: 'Work instruction suspensa',
      resume: 'Work instruction retomada',
      block: 'Work instruction bloqueada',
      reset: 'Work instruction resetada',
      cancel: 'Work instruction cancelada'
    };
    commands.setCommand({
      title: `${label} work instruction #${order.id}`,
      description: `Unidade ${order.codigoConteiner}. O sucesso só será exibido após recarga do registro persistido.`,
      success: `${successMessages[type]} e estado persistido recarregado.`,
      run: (reason) => operation(order.id, reason)
    });
  }

  function openPhysicalConfirmation(order) {
    if (!canOperate) return;
    setPhysicalOrder(order);
    setPhysicalData(physicalForm(order, detail));
    commands.setError('');
    commands.setSuccess('');
  }

  async function confirmPhysicalTransfer() {
    if (!physicalOrder || physicalBusy) return;
    setPhysicalBusy(true);
    commands.setError('');
    commands.setSuccess('');
    try {
      const body = {
        eventId: createCorrelationId(),
        tipoEvento: 'CONCLUSAO',
        statusEsperado: physicalOrder.statusConfirmacaoVmt,
        timestamp: localTimestamp(),
        resultado: 'TRANSFERENCIA_FISICA_CONFIRMADA_NO_PORTAL',
        operador: session?.nome ?? 'operador',
        tipoAcaoFisica: physicalData.tipoAcaoFisica,
        codigoUnidadeLido: physicalData.codigoUnidadeLido.trim(),
        equipamentoPatioId: numberOrNull(physicalData.equipamentoPatioId),
        equipamentoIdentificador: physicalData.equipamentoIdentificador.trim(),
        origem: physicalData.origem.trim(),
        destino: physicalData.destino.trim(),
        linhaOrigem: numberOrNull(physicalData.linhaOrigem),
        colunaOrigem: numberOrNull(physicalData.colunaOrigem),
        camadaOrigem: physicalData.camadaOrigem.trim() || null,
        linhaDestino: numberOrNull(physicalData.linhaDestino),
        colunaDestino: numberOrNull(physicalData.colunaDestino),
        camadaDestino: physicalData.camadaDestino.trim() || null,
        sequenciaOperacional: numberOrNull(physicalData.sequenciaOperacional)
      };
      const response = await request(`/yard/patio/work-instructions/${physicalOrder.id}/vmt-events`, { method: 'POST', body });
      const refreshed = await remote.reload();
      if (refreshed === undefined) throw new Error('A transferência foi confirmada, mas o estado persistido não pôde ser recarregado.');
      const persisted = response?.instrucao ?? physicalOrder;
      setSelectedOrder(persisted);
      setDetail(await api.obterDrillDownWorkInstructionPatio(physicalOrder.id));
      setPhysicalOrder(null);
      commands.setSuccess('Transferência física confirmada, inventário atualizado e work instruction concluída.');
    } catch (reason) {
      commands.setError(formatError(reason));
    } finally {
      setPhysicalBusy(false);
    }
  }

  return <>
    <YardPageHeader path="/home/patio/lista-trabalho" navigate={navigate} title="Lista de trabalho" description="Work queues e work instructions persistidas, com confirmação física, drill-down e comandos motivados conforme o perfil autenticado." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error || commands.error}</Message>
    <Message type="success">{commands.success}</Message>
    {!canAdminister && !canOperate && <Message type="warning">Seu perfil possui acesso de consulta. Os comandos operacionais permanecem ocultos e o backend continua aplicando a autorização.</Message>}
    <ManualConfirmacaoFisica />
    <Section title="Filtros"><div className="filter-grid">
      <FilterField label="Busca"><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Fila, zona, equipamento ou contêiner" /></FilterField>
      <FilterField label="Status"><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Todos</option>{['ATIVA', 'INATIVA', 'PENDENTE', 'EM_EXECUCAO', 'BLOQUEADA', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA'].map((value) => <option key={value}>{value}</option>)}</select></FilterField>
    </div></Section>
    <CommandPanel command={commands.command} busy={commands.busy} onCancel={() => commands.setCommand(null)} onConfirm={commands.confirm} />
    <PhysicalConfirmationPanel order={physicalOrder} detail={detail} form={physicalData} onChange={setPhysicalData} busy={physicalBusy} onCancel={() => setPhysicalOrder(null)} onConfirm={confirmPhysicalTransfer} />
    {remote.loading ? <Loading label="Carregando filas e instruções..." /> : <>
      <Section title={`Work queues (${queues.length})`} description="Selecione uma fila para abrir sua job list e os comandos permitidos.">
        <DataTable rows={queuePage.rows} rowKey={(row) => row.id ?? row.identificador} onRowClick={setSelectedQueue} columns={[
          { key: 'identificador', label: 'Fila' },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'blocoZona', label: 'Bloco/zona' },
          { key: 'pow', label: 'POW' },
          { key: 'equipamento', label: 'Equipamento' },
          { key: 'prioridadeOperacional', label: 'Prioridade' },
          { key: 'totalOrdens', label: 'Ordens' }
        ]} emptyTitle="Nenhuma work queue encontrada" />
        <Pagination page={queuePage.page} totalPages={queuePage.totalPages} totalRows={queues.length} onChange={queuePage.setPage} />
        {selectedQueue && <div className="selection-panel">
          <DetailGrid value={selectedQueue} fields={[
            ['identificador', 'Fila'], ['status', 'Status'], ['blocoZona', 'Bloco/zona'], ['pow', 'POW'],
            ['poolOperacional', 'Pool'], ['equipamento', 'Equipamento'], ['visitaNavioId', 'Visita'], ['totalOrdens', 'Job list']
          ]} />
          {canOperate && <div className="actions">
            {canAdminister && <><button className="small" onClick={() => queueCommand(selectedQueue, 'activate')}>Ativar</button><button className="warning small" onClick={() => queueCommand(selectedQueue, 'deactivate')}>Desativar</button></>}
            <button className="small" onClick={() => queueCommand(selectedQueue, 'dispatch')}>Dispatch</button>
          </div>}
          <DataTable rows={selectedQueue.jobList ?? []} rowKey={(row) => row.id} columns={[
            { key: 'codigoConteiner', label: 'Unidade' },
            { key: 'tipoMovimento', label: 'Movimento' },
            { key: 'statusOrdem', label: 'Status', render: (row) => <StatusBadge value={row.statusOrdem} /> },
            { key: 'sequenciaNavio', label: 'Sequência' }
          ]} emptyTitle="Fila sem work instructions" />
        </div>}
      </Section>
      <div className="split-grid">
        <Section title={`Work instructions (${orders.length})`}>
          <DataTable rows={orderPage.rows} rowKey={(row) => row.id} onRowClick={openOrder} columns={[
            { key: 'id', label: 'ID' },
            { key: 'codigoConteiner', label: 'Unidade' },
            { key: 'tipoMovimento', label: 'Movimento' },
            { key: 'statusOrdem', label: 'Status', render: (row) => <StatusBadge value={row.statusOrdem} /> },
            { key: 'destino', label: 'Destino' },
            { key: 'prioridadeOperacional', label: 'Prioridade' },
            { key: 'workQueueId', label: 'Fila' }
          ]} emptyTitle="Nenhuma work instruction encontrada" />
          <Pagination page={orderPage.page} totalPages={orderPage.totalPages} totalRows={orders.length} onChange={orderPage.setPage} />
        </Section>
        <Section title="Drill-down e comandos">
          {!selectedOrder ? <EmptyState title="Selecione uma work instruction" /> : <>
            <DetailGrid value={selectedOrder} fields={[
              ['id', 'ID'], ['codigoConteiner', 'Unidade'], ['tipoMovimento', 'Movimento'], ['statusOrdem', 'Status'],
              ['statusConfirmacaoVmt', 'Estado VMT'], ['tipoOrigem', 'Origem'], ['tipoDestino', 'Destino'], ['linhaDestino', 'Linha'], ['colunaDestino', 'Coluna'],
              ['camadaDestino', 'Camada'], ['sequenciaNavio', 'Sequência'], ['prioridadeOperacional', 'Prioridade']
            ]} />
            {canOperate && <div className="actions"><button className="small" onClick={() => instructionCommand(selectedOrder, 'suspend')}>Suspender</button><button className="small" onClick={() => instructionCommand(selectedOrder, 'resume')}>Retomar</button><button className="warning small" onClick={() => instructionCommand(selectedOrder, 'block')}>Bloquear</button><button className="small" disabled={selectedOrder.statusOrdem !== 'EM_EXECUCAO' || selectedOrder.statusConfirmacaoVmt !== 'EM_EXECUCAO'} onClick={() => openPhysicalConfirmation(selectedOrder)}>Confirmar transferência física</button><button className="secondary small" onClick={() => instructionCommand(selectedOrder, 'reset')}>Resetar</button><button className="danger small" onClick={() => instructionCommand(selectedOrder, 'cancel')}>Cancelar</button></div>}
            <JsonDetails value={{ drillDown: detail, matrizEstados: remote.data?.matrix?.[selectedOrder.statusOrdem] ?? [] }} title="Drill-down persistido e transições oficiais" />
          </>}
        </Section>
      </div>
    </>}
  </>;
}

export function YardMovementsPage({ navigate }) {
  const [search, setSearch] = useState('');
  const [movementType, setMovementType] = useState('');
  const [selected, setSelected] = useState(null);
  const remote = useRemote(() => api.listarMovimentacoesPatio(), []);
  const rows = useMemo(() => (remote.data ?? []).filter((movement) =>
    (!movementType || movement.tipoMovimento === movementType)
    && (!search || normalized(`${movement.codigoConteiner} ${movement.descricao} ${movement.destino}`).includes(normalized(search)))
  ), [remote.data, search, movementType]);
  const types = useMemo(() => Array.from(new Set((remote.data ?? []).map((movement) => movement.tipoMovimento).filter(Boolean))).sort(), [remote.data]);
  const paged = usePagination(rows);
  return <>
    <YardPageHeader path="/home/patio/movimentacoes" navigate={navigate} title="Movimentações" description="Histórico persistido de movimentos no pátio, com origem operacional, destino e posição registrada." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Filtros"><div className="filter-grid">
      <FilterField label="Busca"><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Contêiner, descrição ou destino" /></FilterField>
      <FilterField label="Tipo"><select value={movementType} onChange={(event) => setMovementType(event.target.value)}><option value="">Todos</option>{types.map((value) => <option key={value}>{value}</option>)}</select></FilterField>
    </div></Section>
    <div className="split-grid">
      <Section title={`Movimentos (${rows.length})`}>
        {remote.loading ? <Loading /> : <><DataTable rows={paged.rows} rowKey={(row) => row.id} onRowClick={setSelected} columns={[
          { key: 'codigoConteiner', label: 'Contêiner' },
          { key: 'tipoMovimento', label: 'Tipo', render: (row) => <StatusBadge value={row.tipoMovimento} /> },
          { key: 'descricao', label: 'Descrição' },
          { key: 'destino', label: 'Destino' },
          { key: 'linha', label: 'Linha' },
          { key: 'coluna', label: 'Coluna' },
          { key: 'camadaOperacional', label: 'Camada' },
          { key: 'registradoEm', label: 'Registrado em', render: (row) => displayValue(row.registradoEm) }
        ]} emptyTitle="Nenhum movimento encontrado" /><Pagination page={paged.page} totalPages={paged.totalPages} totalRows={rows.length} onChange={paged.setPage} /></>}
      </Section>
      <Section title="Detalhe do movimento"><DetailGrid value={selected} fields={[
        ['id', 'ID'], ['codigoConteiner', 'Contêiner'], ['tipoMovimento', 'Tipo'], ['descricao', 'Descrição'],
        ['destino', 'Destino'], ['linha', 'Linha'], ['coluna', 'Coluna'], ['camadaOperacional', 'Camada'], ['registradoEm', 'Registrado em']
      ]} /></Section>
    </div>
  </>;
}

export function YardResourcesPage({ navigate }) {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [selected, setSelected] = useState(null);
  const remote = useRemote(async () => {
    const [map, telemetry, jobLists] = await Promise.all([
      api.obterMapaPatio({}),
      api.listarTelemetriaEquipamentosPatio(),
      api.listarJobListsEquipamentoPatio()
    ]);
    const resources = map?.equipamentos ?? [];
    const telemetryById = new Map((telemetry ?? []).map((item) => [item.equipamento, item]));
    const jobsByEquipment = new Map((jobLists ?? []).map((item) => [item.equipamento ?? item.identificador, item]));
    return resources.map((resource) => ({
      ...resource,
      telemetria: telemetryById.get(resource.identificador) ?? null,
      jobList: jobsByEquipment.get(resource.identificador) ?? null
    }));
  }, []);
  const rows = useMemo(() => (remote.data ?? []).filter((resource) => {
    const currentStatus = resource.statusOperacional ?? resource.status ?? resource.telemetria?.statusOperacional;
    return (!status || currentStatus === status)
      && (!search || normalized(`${resource.identificador} ${resource.tipoEquipamento ?? resource.tipo} ${resource.localizacao ?? ''}`).includes(normalized(search)));
  }), [remote.data, search, status]);
  const statuses = useMemo(() => Array.from(new Set((remote.data ?? []).map((item) => item.statusOperacional ?? item.status ?? item.telemetria?.statusOperacional).filter(Boolean))).sort(), [remote.data]);
  const paged = usePagination(rows);
  return <>
    <YardPageHeader path="/home/patio/recursos" navigate={navigate} title="Recursos operacionais" description="Equipamentos do Yard, posição, telemetria confirmada e job list vinculada." actions={<button className="secondary" onClick={remote.reload}>Atualizar</button>} />
    <Message type="error">{remote.error}</Message>
    <Section title="Filtros"><div className="filter-grid">
      <FilterField label="Busca"><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Identificador, tipo ou localização" /></FilterField>
      <FilterField label="Status"><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Todos</option>{statuses.map((value) => <option key={value}>{value}</option>)}</select></FilterField>
    </div></Section>
    <div className="split-grid">
      <Section title={`Equipamentos (${rows.length})`}>
        {remote.loading ? <Loading /> : <><DataTable rows={paged.rows} rowKey={(row, index) => row.id ?? row.identificador ?? index} onRowClick={setSelected} columns={[
          { key: 'identificador', label: 'Equipamento' },
          { key: 'tipoEquipamento', label: 'Tipo', render: (row) => displayValue(row.tipoEquipamento ?? row.tipo) },
          { key: 'statusOperacional', label: 'Status', render: (row) => <StatusBadge value={row.statusOperacional ?? row.status ?? row.telemetria?.statusOperacional} /> },
          { key: 'linha', label: 'Linha' },
          { key: 'coluna', label: 'Coluna' },
          { key: 'posicaoMaisProxima', label: 'Posição', render: (row) => displayValue(row.telemetria?.posicaoMaisProxima ?? row.localizacao) },
          { key: 'workInstructionAtualId', label: 'WI atual', render: (row) => displayValue(row.telemetria?.workInstructionAtualId) }
        ]} emptyTitle="Nenhum recurso encontrado" /><Pagination page={paged.page} totalPages={paged.totalPages} totalRows={rows.length} onChange={paged.setPage} /></>}
      </Section>
      <Section title="Detalhes do recurso">
        <DetailGrid value={selected} fields={[
          ['identificador', 'Equipamento'],
          ['tipoEquipamento', 'Tipo', (value) => displayValue(value.tipoEquipamento ?? value.tipo)],
          ['statusOperacional', 'Status', (value) => displayValue(value.statusOperacional ?? value.status ?? value.telemetria?.statusOperacional)],
          ['linha', 'Linha'],
          ['coluna', 'Coluna'],
          ['telemetria', 'Telemetria', (value) => value.telemetria ? `Sequência ${value.telemetria.sequencia ?? '—'} · ${displayValue(value.telemetria.recebidoEm)}` : 'Sem telemetria'],
          ['jobList', 'Job list', (value) => `${value.jobList?.ordens?.length ?? value.jobList?.jobList?.length ?? 0} instrução(ões)`]
        ]} />
        <JsonDetails value={selected ? { telemetria: selected.telemetria, jobList: selected.jobList } : null} title="Telemetria e job list persistidas" />
      </Section>
    </div>
  </>;
}
