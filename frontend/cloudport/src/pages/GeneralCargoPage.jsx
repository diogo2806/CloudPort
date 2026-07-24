import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, readSession } from '../api.js';
import { companiesApi } from '../companiesApi.js';
import {
  DataTable,
  EmptyState,
  Loading,
  Message,
  MetricCard,
  PageHeader,
  Section,
  StatusBadge
} from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';
import { generalCargoCompanyLinksApi } from '../generalCargoCompanyLinksApi.js';
import {
  BILL_COMPANY_ROLES,
  COMPANY_ROLE_LABELS,
  LOT_COMPANY_ROLES,
  buildLinksPayload,
  companiesForRole,
  companyName,
  companyOptionLabel,
  selectedCompany
} from '../generalCargoCompanyLinksModel.js';
import { GeneralCargoCompanyLinks } from './GeneralCargoCompanyLinks.jsx';
import { GeneralCargoDamageInspector } from './GeneralCargoDamageInspector.jsx';

const EMPTY_DASHBOARD = {
  conhecimentosAbertos: 0,
  lotesNoTerminal: 0,
  lotesBreakBulk: 0,
  lotesAvariados: 0,
  quantidadeEmEstoque: 0,
  volumeEmEstoqueM3: 0,
  pesoEmEstoqueKg: 0,
  ultimasMovimentacoes: []
};

const GENERAL_CARGO_MANUAL_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/carga-geral-empresas-operacionais.md';

function number(value, digits = 3) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: digits }).format(Number(value ?? 0));
}

function dateTime(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString('pt-BR');
}

function blankKnowledge() {
  return {
    numero: '', tipoOperacao: 'IMPORTACAO', embarcadorEmpresaId: '', consignatarioEmpresaId: '',
    clienteEmpresaId: '', operadorEmpresaId: '', importadorEmpresaId: '', exportadorEmpresaId: '',
    donoCargaEmpresaId: '', agenteEmpresaId: '', transportadoraEmpresaId: '', visitaNavioId: '',
    visitaVeiculoId: '', armazemId: '', portoOrigem: '', portoDestino: '', observacoes: ''
  };
}

function blankItem() {
  return {
    sequencia: 1, descricao: '', commodityCodigo: '', tipoProdutoCodigo: '', tipoEmbalagemCodigo: '',
    quantidadeManifestada: '', volumeM3: '', pesoKg: '', codigoArmazenagem: '', codigoManuseio: '',
    mercadoriaPerigosa: false, numeroUn: '', classeImdg: '', temperaturaMinima: '', temperaturaMaxima: ''
  };
}

function blankLot() {
  return {
    codigo: '', natureza: 'CARGA_SOLTA', quantidadePrevista: '', volumePrevistoM3: '', pesoPrevistoKg: '',
    unidadeMedida: 'UN', marcasEmbalagem: '', armazemId: '', posicaoArmazenagem: '', veiculoId: '',
    visitaNavioId: '', lotePaiId: '', clienteEmpresaId: '', donoCargaEmpresaId: '', operadorEmpresaId: '',
    transportadoraEmpresaId: ''
  };
}

function blankMovement() {
  return {
    tipo: 'RECEBIMENTO', quantidade: '', volumeM3: '', pesoKg: '', loteRelacionadoId: '',
    origemTipo: '', origemId: '', destinoTipo: '', destinoId: '', veiculoId: '', visitaNavioId: '',
    armazemId: '', clienteId: '', observacao: ''
  };
}

function blankReference() {
  return { categoria: 'COMMODITY', codigo: '', descricao: '', atributosJson: '', ativo: true };
}

function knowledgeSelection(value) {
  return {
    CLIENTE: value.clienteEmpresaId,
    EMBARCADOR: value.embarcadorEmpresaId,
    CONSIGNATARIO: value.consignatarioEmpresaId,
    IMPORTADOR: value.importadorEmpresaId,
    EXPORTADOR: value.exportadorEmpresaId,
    DONO_CARGA: value.donoCargaEmpresaId,
    OPERADOR: value.operadorEmpresaId,
    AGENTE: value.agenteEmpresaId,
    TRANSPORTADORA: value.transportadoraEmpresaId
  };
}

function lotSelection(value) {
  return {
    CLIENTE: value.clienteEmpresaId,
    DONO_CARGA: value.donoCargaEmpresaId,
    OPERADOR: value.operadorEmpresaId,
    TRANSPORTADORA: value.transportadoraEmpresaId
  };
}

function knowledgePayload(value, companies) {
  return {
    numero: value.numero,
    tipoOperacao: value.tipoOperacao,
    embarcador: companyName(selectedCompany(companies, value.embarcadorEmpresaId)),
    consignatario: companyName(selectedCompany(companies, value.consignatarioEmpresaId)),
    clienteId: value.clienteEmpresaId || '',
    operadorId: value.operadorEmpresaId || '',
    visitaNavioId: value.visitaNavioId,
    visitaVeiculoId: value.visitaVeiculoId,
    armazemId: value.armazemId,
    portoOrigem: value.portoOrigem,
    portoDestino: value.portoDestino,
    observacoes: value.observacoes
  };
}

function lotPayload(value) {
  return {
    codigo: value.codigo,
    natureza: value.natureza,
    quantidadePrevista: value.quantidadePrevista,
    volumePrevistoM3: value.volumePrevistoM3,
    pesoPrevistoKg: value.pesoPrevistoKg,
    unidadeMedida: value.unidadeMedida,
    marcasEmbalagem: value.marcasEmbalagem,
    armazemId: value.armazemId,
    posicaoArmazenagem: value.posicaoArmazenagem,
    veiculoId: value.veiculoId,
    visitaNavioId: value.visitaNavioId,
    clienteId: value.clienteEmpresaId || '',
    lotePaiId: value.lotePaiId || null
  };
}

function CompanySelectField({ role, value, companies, required = false, onChange }) {
  const options = companiesForRole(companies, role, value);
  return <label className="field">
    <span>{COMPANY_ROLE_LABELS[role] || role}</span>
    <select required={required} value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">{required ? 'Selecione' : 'Não informado'}</option>
      {options.map((company) => <option key={company.id} value={company.id}>{companyOptionLabel(company)}</option>)}
    </select>
    {!options.length && <small>Nenhuma empresa ativa com este papel.</small>}
  </label>;
}

export function GeneralCargoPage() {
  const [remote, setRemote] = useState({ dashboard: EMPTY_DASHBOARD, conhecimentos: [], lotes: [], referencias: [], empresas: [] });
  const [selectedKnowledgeId, setSelectedKnowledgeId] = useState('');
  const [knowledgeDetail, setKnowledgeDetail] = useState(null);
  const [selectedItemId, setSelectedItemId] = useState('');
  const [selectedLotId, setSelectedLotId] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [companyError, setCompanyError] = useState('');
  const [success, setSuccess] = useState('');
  const [knowledge, setKnowledge] = useState(blankKnowledge);
  const [item, setItem] = useState(blankItem);
  const [lot, setLot] = useState(blankLot);
  const [movement, setMovement] = useState(blankMovement);
  const [reference, setReference] = useState(blankReference);

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    setCompanyError('');
    try {
      const [dashboard, conhecimentos, lotes, referencias] = await Promise.all([
        generalCargoApi.dashboard(),
        generalCargoApi.listarConhecimentos(),
        generalCargoApi.listarLotes(),
        generalCargoApi.listarReferencias()
      ]);
      let empresas = [];
      try {
        const response = await companiesApi.listar({});
        empresas = Array.isArray(response) ? response : [];
      } catch (reason) {
        setCompanyError(formatError(reason));
      }
      setRemote({
        dashboard: dashboard ?? EMPTY_DASHBOARD,
        conhecimentos: Array.isArray(conhecimentos) ? conhecimentos : [],
        lotes: Array.isArray(lotes) ? lotes : [],
        referencias: Array.isArray(referencias) ? referencias : [],
        empresas
      });
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { reload(); }, [reload]);

  useEffect(() => {
    if (!selectedKnowledgeId) {
      setKnowledgeDetail(null);
      setSelectedItemId('');
      return;
    }
    generalCargoApi.obterConhecimento(selectedKnowledgeId)
      .then((detail) => {
        setKnowledgeDetail(detail);
        setSelectedItemId((current) => current || detail?.itens?.[0]?.id || '');
      })
      .catch((reason) => setError(formatError(reason)));
  }, [selectedKnowledgeId]);

  const selectedLot = useMemo(
    () => remote.lotes.find((candidate) => candidate.id === selectedLotId) ?? null,
    [remote.lotes, selectedLotId]
  );

  async function execute(key, action, message) {
    setBusy(key);
    setError('');
    setSuccess('');
    try {
      const result = await action();
      setSuccess(message);
      await reload();
      if (selectedKnowledgeId) {
        const detail = await generalCargoApi.obterConhecimento(selectedKnowledgeId);
        setKnowledgeDetail(detail);
      }
      return result;
    } catch (reason) {
      setError(formatError(reason));
      return null;
    } finally {
      setBusy('');
    }
  }

  async function createKnowledge(event) {
    event.preventDefault();
    let createdId = '';
    const created = await execute('knowledge', async () => {
      const result = await generalCargoApi.criarConhecimento(knowledgePayload(knowledge, remote.empresas));
      createdId = result?.id || '';
      if (createdId) {
        setSelectedKnowledgeId(createdId);
        await generalCargoCompanyLinksApi.salvar(
          'CONHECIMENTO',
          createdId,
          buildLinksPayload(knowledgeSelection(knowledge), BILL_COMPANY_ROLES)
        );
      }
      return result;
    }, 'Bill of Lading criado com empresas vinculadas.');
    if (created?.id) {
      setSelectedKnowledgeId(created.id);
      setKnowledge(blankKnowledge());
    } else if (createdId) {
      setSelectedKnowledgeId(createdId);
    }
  }

  async function createItem(event) {
    event.preventDefault();
    if (!selectedKnowledgeId) return;
    const created = await execute('item', () => generalCargoApi.adicionarItem(selectedKnowledgeId, item), 'Item do conhecimento criado.');
    if (created?.id) {
      setSelectedItemId(created.id);
      setItem(blankItem());
    }
  }

  async function createLot(event) {
    event.preventDefault();
    if (!selectedItemId) return;
    let createdId = '';
    const created = await execute('lot', async () => {
      const result = await generalCargoApi.adicionarLote(selectedItemId, lotPayload(lot));
      createdId = result?.id || '';
      if (createdId) {
        setSelectedLotId(createdId);
        await generalCargoCompanyLinksApi.salvar(
          'LOTE',
          createdId,
          buildLinksPayload(lotSelection(lot), LOT_COMPANY_ROLES)
        );
      }
      return result;
    }, 'Cargo lot criado com empresas vinculadas.');
    if (created?.id) {
      setSelectedLotId(created.id);
      setLot(blankLot());
    } else if (createdId) {
      setSelectedLotId(createdId);
    }
  }

  async function registerMovement(event) {
    event.preventDefault();
    if (!selectedLotId) return;
    const session = readSession();
    await execute('movement', () => generalCargoApi.registrarMovimentacao(selectedLotId, {
      ...movement,
      loteRelacionadoId: movement.loteRelacionadoId || null,
      usuario: session?.nome || 'operador'
    }), 'Movimentação registrada.');
    setMovement(blankMovement());
  }

  async function createReference(event) {
    event.preventDefault();
    await execute('reference', () => generalCargoApi.criarReferencia(reference), 'Referência criada.');
    setReference(blankReference());
  }

  return <>
    <PageHeader
      eyebrow="Cargo"
      title="Carga geral e break-bulk"
      description="Controle canônico de Bill of Lading, itens, cargo lots, estoque físico, carga parcial, descarga, consolidação, avarias e referências operacionais."
      actions={<>
        <a className="secondary" href={GENERAL_CARGO_MANUAL_URL} target="_blank" rel="noreferrer" title="Abrir manual da tela" aria-label="Abrir manual da tela"><span aria-hidden="true">?</span> Manual</a>
        <button type="button" className="secondary" onClick={reload}>Atualizar</button>
      </>}
    />
    <Message type="error">{error}</Message>
    <Message type="error">{companyError && `Cadastro de empresas indisponível: ${companyError}`}</Message>
    <Message type="success" onClose={() => setSuccess('')}>{success}</Message>

    <div className="metrics-grid">
      <MetricCard label="Conhecimentos abertos" value={remote.dashboard.conhecimentosAbertos} />
      <MetricCard label="Lotes no terminal" value={remote.dashboard.lotesNoTerminal} />
      <MetricCard label="Break-bulk" value={remote.dashboard.lotesBreakBulk} />
      <MetricCard label="Lotes avariados" value={remote.dashboard.lotesAvariados} />
      <MetricCard label="Quantidade em estoque" value={number(remote.dashboard.quantidadeEmEstoque)} />
      <MetricCard label="Volume em estoque" value={`${number(remote.dashboard.volumeEmEstoqueM3)} m³`} />
      <MetricCard label="Peso em estoque" value={`${number(remote.dashboard.pesoEmEstoqueKg)} kg`} />
    </div>

    <Section title="Novo Bill of Lading" description="Selecione empresas ativas por papel. Embarcador e consignatário são obrigatórios e os nomes ficam registrados como fotografia operacional.">
      <form className="planner-selection-grid" onSubmit={createKnowledge}>
        <label className="field"><span>Número</span><input required maxLength="80" value={knowledge.numero} onChange={(event) => setKnowledge((current) => ({ ...current, numero: event.target.value }))} /></label>
        <label className="field"><span>Operação</span><select value={knowledge.tipoOperacao} onChange={(event) => setKnowledge((current) => ({ ...current, tipoOperacao: event.target.value }))}><option>IMPORTACAO</option><option>EXPORTACAO</option><option>CABOTAGEM</option><option>TRANSBORDO</option></select></label>
        <CompanySelectField role="EMBARCADOR" required value={knowledge.embarcadorEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, embarcadorEmpresaId: value }))} />
        <CompanySelectField role="CONSIGNATARIO" required value={knowledge.consignatarioEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, consignatarioEmpresaId: value }))} />
        <CompanySelectField role="CLIENTE" value={knowledge.clienteEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, clienteEmpresaId: value }))} />
        <CompanySelectField role="IMPORTADOR" value={knowledge.importadorEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, importadorEmpresaId: value }))} />
        <CompanySelectField role="EXPORTADOR" value={knowledge.exportadorEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, exportadorEmpresaId: value }))} />
        <CompanySelectField role="DONO_CARGA" value={knowledge.donoCargaEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, donoCargaEmpresaId: value }))} />
        <CompanySelectField role="OPERADOR" value={knowledge.operadorEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, operadorEmpresaId: value }))} />
        <CompanySelectField role="AGENTE" value={knowledge.agenteEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, agenteEmpresaId: value }))} />
        <CompanySelectField role="TRANSPORTADORA" value={knowledge.transportadoraEmpresaId} companies={remote.empresas} onChange={(value) => setKnowledge((current) => ({ ...current, transportadoraEmpresaId: value }))} />
        <label className="field"><span>Visita do navio</span><input maxLength="80" value={knowledge.visitaNavioId} onChange={(event) => setKnowledge((current) => ({ ...current, visitaNavioId: event.target.value }))} /></label>
        <label className="field"><span>Visita do veículo</span><input maxLength="80" value={knowledge.visitaVeiculoId} onChange={(event) => setKnowledge((current) => ({ ...current, visitaVeiculoId: event.target.value }))} /></label>
        <label className="field"><span>Armazém</span><input maxLength="80" value={knowledge.armazemId} onChange={(event) => setKnowledge((current) => ({ ...current, armazemId: event.target.value }))} /></label>
        <label className="field"><span>Porto de origem</span><input maxLength="120" value={knowledge.portoOrigem} onChange={(event) => setKnowledge((current) => ({ ...current, portoOrigem: event.target.value }))} /></label>
        <label className="field"><span>Porto de destino</span><input maxLength="120" value={knowledge.portoDestino} onChange={(event) => setKnowledge((current) => ({ ...current, portoDestino: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'knowledge' || !!companyError}>{busy === 'knowledge' ? 'Salvando...' : 'Criar conhecimento'}</button></div>
      </form>
    </Section>

    {loading ? <Loading label="Carregando carga geral..." /> : <>
      <Section title="Bills of Lading" description="Selecione um conhecimento para cadastrar itens, cargo lots e manter suas empresas.">
        <DataTable
          gridId="general-cargo-bills"
          exportFileName="bills-of-lading"
          rows={remote.conhecimentos}
          rowKey="id"
          columns={[
            { key: 'numero', label: 'Bill of Lading' },
            { key: 'tipoOperacao', label: 'Operação', render: (row) => <StatusBadge value={row.tipoOperacao} /> },
            { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
            { key: 'embarcador', label: 'Embarcador' },
            { key: 'consignatario', label: 'Consignatário' },
            { key: 'visitaNavioId', label: 'Navio' },
            { key: 'visitaVeiculoId', label: 'Veículo' },
            { key: 'armazemId', label: 'Armazém' },
            { key: 'quantidadeItens', label: 'Itens' },
            { key: 'atualizadoEm', label: 'Atualizado', render: (row) => dateTime(row.atualizadoEm) },
            { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedKnowledgeId(row.id)}>Selecionar</button> }
          ]}
        />
      </Section>

      <GeneralCargoCompanyLinks
        resourceType="CONHECIMENTO"
        resourceId={selectedKnowledgeId}
        resourceLabel={knowledgeDetail?.numero}
        companies={remote.empresas}
        roles={BILL_COMPANY_ROLES}
      />

      <Section title="Item do conhecimento" description={selectedKnowledgeId ? `Conhecimento selecionado: ${knowledgeDetail?.numero ?? selectedKnowledgeId}` : 'Selecione um Bill of Lading.'}>
        <form className="planner-selection-grid" onSubmit={createItem}>
          <label className="field"><span>Sequência</span><input required type="number" min="1" value={item.sequencia} onChange={(event) => setItem((current) => ({ ...current, sequencia: Number(event.target.value) }))} /></label>
          <label className="field"><span>Descrição</span><input required maxLength="300" value={item.descricao} onChange={(event) => setItem((current) => ({ ...current, descricao: event.target.value }))} /></label>
          <label className="field"><span>Commodity</span><input required maxLength="80" value={item.commodityCodigo} onChange={(event) => setItem((current) => ({ ...current, commodityCodigo: event.target.value }))} /></label>
          <label className="field"><span>Tipo de produto</span><input required maxLength="80" value={item.tipoProdutoCodigo} onChange={(event) => setItem((current) => ({ ...current, tipoProdutoCodigo: event.target.value }))} /></label>
          <label className="field"><span>Embalagem</span><input required maxLength="80" value={item.tipoEmbalagemCodigo} onChange={(event) => setItem((current) => ({ ...current, tipoEmbalagemCodigo: event.target.value }))} /></label>
          <label className="field"><span>Quantidade</span><input required type="number" min="0.001" step="0.001" value={item.quantidadeManifestada} onChange={(event) => setItem((current) => ({ ...current, quantidadeManifestada: event.target.value }))} /></label>
          <label className="field"><span>Volume m³</span><input required type="number" min="0" step="0.001" value={item.volumeM3} onChange={(event) => setItem((current) => ({ ...current, volumeM3: event.target.value }))} /></label>
          <label className="field"><span>Peso kg</span><input required type="number" min="0" step="0.001" value={item.pesoKg} onChange={(event) => setItem((current) => ({ ...current, pesoKg: event.target.value }))} /></label>
          <label className="field"><span>Cód. armazenagem</span><input maxLength="80" value={item.codigoArmazenagem} onChange={(event) => setItem((current) => ({ ...current, codigoArmazenagem: event.target.value }))} /></label>
          <label className="field"><span>Cód. manuseio</span><input maxLength="80" value={item.codigoManuseio} onChange={(event) => setItem((current) => ({ ...current, codigoManuseio: event.target.value }))} /></label>
          <label className="field"><span>Perigosa</span><select value={item.mercadoriaPerigosa ? 'true' : 'false'} onChange={(event) => setItem((current) => ({ ...current, mercadoriaPerigosa: event.target.value === 'true' }))}><option value="false">Não</option><option value="true">Sim</option></select></label>
          <label className="field"><span>Número UN</span><input maxLength="20" value={item.numeroUn} onChange={(event) => setItem((current) => ({ ...current, numeroUn: event.target.value }))} /></label>
          <label className="field"><span>Classe IMDG</span><input maxLength="20" value={item.classeImdg} onChange={(event) => setItem((current) => ({ ...current, classeImdg: event.target.value }))} /></label>
          <label className="field"><span>Temp. mínima</span><input type="number" step="0.01" value={item.temperaturaMinima} onChange={(event) => setItem((current) => ({ ...current, temperaturaMinima: event.target.value || null }))} /></label>
          <label className="field"><span>Temp. máxima</span><input type="number" step="0.01" value={item.temperaturaMaxima} onChange={(event) => setItem((current) => ({ ...current, temperaturaMaxima: event.target.value || null }))} /></label>
          <div className="field"><span>Ação</span><button type="submit" disabled={!selectedKnowledgeId || busy === 'item'}>{busy === 'item' ? 'Salvando...' : 'Adicionar item'}</button></div>
        </form>
        {!!knowledgeDetail?.itens?.length && <DataTable rows={knowledgeDetail.itens} rowKey="id" columns={[
          { key: 'sequencia', label: 'Seq.' }, { key: 'descricao', label: 'Descrição' }, { key: 'commodityCodigo', label: 'Commodity' },
          { key: 'tipoProdutoCodigo', label: 'Produto' }, { key: 'tipoEmbalagemCodigo', label: 'Embalagem' },
          { key: 'quantidadeManifestada', label: 'Quantidade' }, { key: 'pesoKg', label: 'Peso kg' },
          { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedItemId(row.id)}>Usar no lote</button> }
        ]} />}
      </Section>

      <Section title="Novo cargo lot" description={selectedItemId ? `Item selecionado: ${selectedItemId}` : 'Selecione um item do conhecimento.'}>
        <form className="planner-selection-grid" onSubmit={createLot}>
          <label className="field"><span>Código do lote</span><input required maxLength="100" value={lot.codigo} onChange={(event) => setLot((current) => ({ ...current, codigo: event.target.value }))} /></label>
          <label className="field"><span>Natureza</span><select value={lot.natureza} onChange={(event) => setLot((current) => ({ ...current, natureza: event.target.value }))}><option>CARGA_SOLTA</option><option>CARGA_PROJETO</option><option>BREAK_BULK</option></select></label>
          <label className="field"><span>Quantidade prevista</span><input required type="number" min="0.001" step="0.001" value={lot.quantidadePrevista} onChange={(event) => setLot((current) => ({ ...current, quantidadePrevista: event.target.value }))} /></label>
          <label className="field"><span>Volume previsto m³</span><input required type="number" min="0" step="0.001" value={lot.volumePrevistoM3} onChange={(event) => setLot((current) => ({ ...current, volumePrevistoM3: event.target.value }))} /></label>
          <label className="field"><span>Peso previsto kg</span><input required type="number" min="0" step="0.001" value={lot.pesoPrevistoKg} onChange={(event) => setLot((current) => ({ ...current, pesoPrevistoKg: event.target.value }))} /></label>
          <label className="field"><span>Unidade</span><input required maxLength="20" value={lot.unidadeMedida} onChange={(event) => setLot((current) => ({ ...current, unidadeMedida: event.target.value }))} /></label>
          <CompanySelectField role="CLIENTE" value={lot.clienteEmpresaId} companies={remote.empresas} onChange={(value) => setLot((current) => ({ ...current, clienteEmpresaId: value }))} />
          <CompanySelectField role="DONO_CARGA" value={lot.donoCargaEmpresaId} companies={remote.empresas} onChange={(value) => setLot((current) => ({ ...current, donoCargaEmpresaId: value }))} />
          <CompanySelectField role="OPERADOR" value={lot.operadorEmpresaId} companies={remote.empresas} onChange={(value) => setLot((current) => ({ ...current, operadorEmpresaId: value }))} />
          <CompanySelectField role="TRANSPORTADORA" value={lot.transportadoraEmpresaId} companies={remote.empresas} onChange={(value) => setLot((current) => ({ ...current, transportadoraEmpresaId: value }))} />
          <label className="field"><span>Armazém</span><input maxLength="80" value={lot.armazemId} onChange={(event) => setLot((current) => ({ ...current, armazemId: event.target.value }))} /></label>
          <label className="field"><span>Posição</span><input maxLength="120" value={lot.posicaoArmazenagem} onChange={(event) => setLot((current) => ({ ...current, posicaoArmazenagem: event.target.value }))} /></label>
          <label className="field"><span>Veículo</span><input maxLength="80" value={lot.veiculoId} onChange={(event) => setLot((current) => ({ ...current, veiculoId: event.target.value }))} /></label>
          <label className="field"><span>Visita do navio</span><input maxLength="80" value={lot.visitaNavioId} onChange={(event) => setLot((current) => ({ ...current, visitaNavioId: event.target.value }))} /></label>
          <label className="field"><span>Lote pai</span><select value={lot.lotePaiId} onChange={(event) => setLot((current) => ({ ...current, lotePaiId: event.target.value }))}><option value="">Nenhum</option>{remote.lotes.map((candidate) => <option key={candidate.id} value={candidate.id}>{candidate.codigo}</option>)}</select></label>
          <div className="field"><span>Ação</span><button type="submit" disabled={!selectedItemId || busy === 'lot' || !!companyError}>{busy === 'lot' ? 'Salvando...' : 'Criar lote'}</button></div>
        </form>
      </Section>

      <Section title="Inventário de cargo lots" description="A grade reúne lote, conhecimento, item, natureza, saldo, localização e vínculos operacionais.">
        <DataTable
          gridId="general-cargo-lots"
          exportFileName="cargo-lots"
          rows={remote.lotes}
          rowKey="id"
          columns={[
            { key: 'codigo', label: 'Cargo lot' }, { key: 'conhecimentoNumero', label: 'B/L' },
            { key: 'descricaoItem', label: 'Item' }, { key: 'natureza', label: 'Natureza', render: (row) => <StatusBadge value={row.natureza} /> },
            { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
            { key: 'quantidadeSaldo', label: 'Saldo' }, { key: 'volumeSaldoM3', label: 'Volume m³' },
            { key: 'pesoSaldoKg', label: 'Peso kg' }, { key: 'armazemId', label: 'Armazém' },
            { key: 'posicaoArmazenagem', label: 'Posição' }, { key: 'veiculoId', label: 'Veículo' },
            { key: 'visitaNavioId', label: 'Navio' }, { key: 'clienteId', label: 'Cliente' },
            { key: 'codigoAvaria', label: 'Última avaria' },
            { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedLotId(row.id)}>Operar</button> }
          ]}
        />
      </Section>

      <GeneralCargoCompanyLinks
        resourceType="LOTE"
        resourceId={selectedLotId}
        resourceLabel={selectedLot?.codigo}
        companies={remote.empresas}
        roles={LOT_COMPANY_ROLES}
      />

      <Section title="Movimentação parcial" description={selectedLot ? `Lote selecionado: ${selectedLot.codigo} | saldo ${number(selectedLot.quantidadeSaldo)} ${selectedLot.unidadeMedida}` : 'Selecione um cargo lot.'}>
        <form className="planner-selection-grid" onSubmit={registerMovement}>
          <label className="field"><span>Tipo</span><select value={movement.tipo} onChange={(event) => setMovement((current) => ({ ...current, tipo: event.target.value }))}><option>RECEBIMENTO</option><option>DESCARGA_PARCIAL</option><option>ARMAZENAGEM</option><option>TRANSFERENCIA</option><option>CARGA_PARCIAL</option><option>ENTREGA</option><option>CONSOLIDACAO</option><option>DESCONSOLIDACAO</option><option>AJUSTE_INVENTARIO</option></select></label>
          <label className="field"><span>Quantidade</span><input required type="number" min="0" step="0.001" value={movement.quantidade} onChange={(event) => setMovement((current) => ({ ...current, quantidade: event.target.value }))} /></label>
          <label className="field"><span>Volume m³</span><input required type="number" min="0" step="0.001" value={movement.volumeM3} onChange={(event) => setMovement((current) => ({ ...current, volumeM3: event.target.value }))} /></label>
          <label className="field"><span>Peso kg</span><input required type="number" min="0" step="0.001" value={movement.pesoKg} onChange={(event) => setMovement((current) => ({ ...current, pesoKg: event.target.value }))} /></label>
          <label className="field"><span>Lote relacionado</span><select value={movement.loteRelacionadoId} onChange={(event) => setMovement((current) => ({ ...current, loteRelacionadoId: event.target.value }))}><option value="">Nenhum</option>{remote.lotes.filter((candidate) => candidate.id !== selectedLotId).map((candidate) => <option key={candidate.id} value={candidate.id}>{candidate.codigo}</option>)}</select></label>
          <label className="field"><span>Origem</span><input maxLength="120" value={movement.origemId} onChange={(event) => setMovement((current) => ({ ...current, origemId: event.target.value }))} /></label>
          <label className="field"><span>Destino</span><input maxLength="120" value={movement.destinoId} onChange={(event) => setMovement((current) => ({ ...current, destinoId: event.target.value }))} /></label>
          <label className="field"><span>Armazém</span><input maxLength="80" value={movement.armazemId} onChange={(event) => setMovement((current) => ({ ...current, armazemId: event.target.value }))} /></label>
          <label className="field"><span>Veículo</span><input maxLength="80" value={movement.veiculoId} onChange={(event) => setMovement((current) => ({ ...current, veiculoId: event.target.value }))} /></label>
          <label className="field"><span>Visita do navio</span><input maxLength="80" value={movement.visitaNavioId} onChange={(event) => setMovement((current) => ({ ...current, visitaNavioId: event.target.value }))} /></label>
          <label className="field"><span>Observação</span><input maxLength="1000" value={movement.observacao} onChange={(event) => setMovement((current) => ({ ...current, observacao: event.target.value }))} /></label>
          <div className="field"><span>Ação</span><button type="submit" disabled={!selectedLotId || busy === 'movement'}>{busy === 'movement' ? 'Registrando...' : 'Registrar movimento'}</button></div>
        </form>
      </Section>

      <GeneralCargoDamageInspector lot={selectedLot} onChanged={reload} />

      <Section title="Referências de carga" description="Catálogo único de commodities, embalagens, produtos, armazenagem, manuseio, perigosos, temperatura e avarias.">
        <form className="planner-selection-grid" onSubmit={createReference}>
          <label className="field"><span>Categoria</span><select value={reference.categoria} onChange={(event) => setReference((current) => ({ ...current, categoria: event.target.value }))}><option>COMMODITY</option><option>TIPO_EMBALAGEM</option><option>TIPO_PRODUTO</option><option>CODIGO_ARMAZENAGEM</option><option>CODIGO_MANUSEIO</option><option>MERCADORIA_PERIGOSA</option><option>FAIXA_TEMPERATURA</option><option>TIPO_AVARIA</option></select></label>
          <label className="field"><span>Código</span><input required maxLength="80" value={reference.codigo} onChange={(event) => setReference((current) => ({ ...current, codigo: event.target.value }))} /></label>
          <label className="field"><span>Descrição</span><input required maxLength="240" value={reference.descricao} onChange={(event) => setReference((current) => ({ ...current, descricao: event.target.value }))} /></label>
          <label className="field"><span>Atributos JSON</span><input maxLength="4000" value={reference.atributosJson} onChange={(event) => setReference((current) => ({ ...current, atributosJson: event.target.value }))} placeholder='{"limite":"exemplo"}' /></label>
          <div className="field"><span>Ação</span><button type="submit" disabled={busy === 'reference'}>{busy === 'reference' ? 'Salvando...' : 'Criar referência'}</button></div>
        </form>
        {remote.referencias.length ? <DataTable rows={remote.referencias} rowKey="id" columns={[
          { key: 'categoria', label: 'Categoria', render: (row) => <StatusBadge value={row.categoria} /> },
          { key: 'codigo', label: 'Código' }, { key: 'descricao', label: 'Descrição' },
          { key: 'ativo', label: 'Situação', render: (row) => <StatusBadge value={row.ativo ? 'ATIVO' : 'INATIVO'} /> },
          { key: 'atualizadoEm', label: 'Atualizado', render: (row) => dateTime(row.atualizadoEm) }
        ]} /> : <EmptyState title="Nenhuma referência cadastrada" />}
      </Section>

      <Section title="Últimas movimentações">
        <DataTable rows={remote.dashboard.ultimasMovimentacoes ?? []} rowKey="id" columns={[
          { key: 'ocorridoEm', label: 'Data', render: (row) => dateTime(row.ocorridoEm) },
          { key: 'loteCodigo', label: 'Lote' }, { key: 'tipo', label: 'Tipo', render: (row) => <StatusBadge value={row.tipo} /> },
          { key: 'quantidade', label: 'Quantidade' }, { key: 'volumeM3', label: 'Volume m³' },
          { key: 'pesoKg', label: 'Peso kg' }, { key: 'origemId', label: 'Origem' },
          { key: 'destinoId', label: 'Destino' }, { key: 'usuario', label: 'Usuário' }
        ]} emptyTitle="Nenhuma movimentação registrada" />
      </Section>
    </>}
  </>;
}
