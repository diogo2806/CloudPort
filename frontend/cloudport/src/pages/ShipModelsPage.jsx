import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, normalizePage, sanitizeText } from '../api.js';
import { DataTable, EmptyState, JsonDetails, Loading, Message, MetricCard, PageHeader, Section, StatusBadge } from '../components.jsx';
import { steelCoilApi } from '../steelCoilApi.js';

function normalizeList(payload) {
  return Array.isArray(payload) ? payload : normalizePage(payload);
}

function formatNumber(value, suffix = '') {
  if (value === undefined || value === null || value === '') return '—';
  const number = Number(value);
  return Number.isFinite(number)
    ? `${number.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}${suffix}`
    : '—';
}

function modelKey(model) {
  return `${model.isTemplate ? 'modelo' : 'perfil'}-${model.id}`;
}

function ManualModelosNavio() {
  return <details className="json-details">
    <summary>ⓘ Manual</summary>
    <div className="content-card">
      <h3>Finalidade da tela</h3>
      <p>Consultar, comparar e inspecionar os modelos estruturais e os perfis versionados de navios já mantidos pelo backend de estiva bulk.</p>
      <h3>Fluxo operacional</h3>
      <ol>
        <li>Escolha modelos, perfis operacionais ou todos os registros.</li>
        <li>Pesquise por nome, IMO ou classe.</li>
        <li>Selecione um registro para conferir dimensões, limites, versões, porões e setores.</li>
        <li>Use o perfil operacional compatível ao iniciar o planejamento de steel coils.</li>
      </ol>
      <h3>Explicação dos campos</h3>
      <ul>
        <li>Modelo: estrutura reutilizável mantida como template no backend.</li>
        <li>Perfil operacional: versão vinculada ao cadastro canônico de um navio.</li>
        <li>Versão do perfil e versão canônica: garantem rastreabilidade dos dados usados no plano.</li>
        <li>LPP, boca e calado: dimensões principais em metros.</li>
        <li>GM, BM e SF: parâmetros e limites de estabilidade e esforço estrutural.</li>
        <li>Porões e setores: geometria disponível para posicionamento e validação das cargas.</li>
      </ul>
      <h3>Permissões necessárias</h3>
      <ul><li>ADMIN_PORTO e PLANEJADOR podem consultar o catálogo completo.</li><li>Demais perfis dependem da política de leitura de estiva publicada pelo backend.</li></ul>
      <h3>Estados possíveis</h3>
      <ul><li>Modelo: registro reutilizável.</li><li>Perfil operacional: versão aplicável a uma visita.</li><li>Sem geometria: registro sem porões expostos.</li><li>Completo: dimensões, limites e geometria disponíveis.</li></ul>
      <h3>Motivos de bloqueio</h3>
      <ul><li>Sessão expirada ou perfil sem permissão.</li><li>Modelo sem geometria ou parâmetros estruturais suficientes.</li><li>Perfil vinculado a versão anterior do cadastro canônico.</li><li>Falha na consulta dos contratos de estiva bulk.</li></ul>
      <h3>Exemplo</h3>
      <p>Filtre por PANAMAX, selecione o modelo desejado e confira a quantidade de porões, os setores de tanktop e os limites BM/SF antes de usar um perfil operacional no planejamento.</p>
      <h3>Atalhos</h3>
      <ul><li>F1 ou Shift + ?: abrir a ajuda contextual.</li><li>Atualizar: recarregar modelos e perfis.</li><li>Enter no botão Ver detalhes: abrir o inspector do registro.</li></ul>
      <p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/catalogo-modelos-navio.md" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
    </div>
  </details>;
}

function ModelDetails({ model }) {
  if (!model) return <EmptyState title="Selecione um modelo ou perfil" description="Use a ação Ver detalhes na tabela para abrir todas as informações publicadas pelo backend." />;
  const holds = Array.isArray(model.poroes) ? model.poroes : [];
  const sectors = holds.flatMap((hold) => (hold.setores ?? []).map((sector) => ({
    ...sector,
    porao: hold.numero
  })));
  return <>
    <div className="metrics-grid">
      <MetricCard label="Tipo" value={model.isTemplate ? 'Modelo' : 'Perfil operacional'} />
      <MetricCard label="Versão do perfil" value={model.versaoPerfil ?? '—'} />
      <MetricCard label="Porões" value={model.totalPoroes ?? holds.length} />
      <MetricCard label="Setores" value={sectors.length} />
    </div>
    <Section title="Identidade e dimensões">
      <DataTable
        rows={[model]}
        gridId="modelo-navio-identidade"
        rowKey={(row) => row.id}
        columns={[
          { key: 'nome', label: 'Nome' },
          { key: 'imo', label: 'IMO' },
          { key: 'classe', label: 'Classe' },
          { key: 'lpp', label: 'LPP', render: (row) => formatNumber(row.lpp, ' m') },
          { key: 'boca', label: 'Boca', render: (row) => formatNumber(row.boca, ' m') },
          { key: 'calado', label: 'Calado', render: (row) => formatNumber(row.calado, ' m') },
          { key: 'deslocamento', label: 'Deslocamento', render: (row) => formatNumber(row.deslocamento, ' t') }
        ]}
      />
    </Section>
    <Section title="Estabilidade, limites e versões">
      <DataTable
        rows={[model]}
        gridId="modelo-navio-estabilidade"
        rowKey={(row) => row.id}
        columns={[
          { key: 'gm', label: 'GM', render: (row) => formatNumber(row.gm, ' m') },
          { key: 'gmMinimo', label: 'GM mínimo', render: (row) => formatNumber(row.gmMinimo, ' m') },
          { key: 'bmMaxPermitido', label: 'BM máximo', render: (row) => formatNumber(row.bmMaxPermitido) },
          { key: 'sfMaxPermitido', label: 'SF máximo', render: (row) => formatNumber(row.sfMaxPermitido) },
          { key: 'versaoDadosHidrostaticos', label: 'Dados hidrostáticos' },
          { key: 'versaoDadosEstruturais', label: 'Dados estruturais' },
          { key: 'versaoNavioCanonico', label: 'Versão canônica' }
        ]}
      />
    </Section>
    <Section title="Porões" description="Geometria ordenada pelo número do porão.">
      {holds.length ? <DataTable
        rows={holds}
        gridId="modelo-navio-poroes"
        rowKey={(row) => row.id ?? row.numero}
        columns={[
          { key: 'numero', label: 'Porão' },
          { key: 'comprimento', label: 'Comprimento', render: (row) => formatNumber(row.comprimento, ' m') },
          { key: 'largura', label: 'Largura', render: (row) => formatNumber(row.largura, ' m') },
          { key: 'alturaUtil', label: 'Altura útil', render: (row) => formatNumber(row.alturaUtil, ' m') },
          { key: 'areaUtil', label: 'Área útil', render: (row) => formatNumber(row.areaUtil, ' m²') },
          { key: 'setores', label: 'Setores', render: (row) => row.setores?.length ?? 0 }
        ]}
      /> : <EmptyState title="Geometria de porões não disponível" />}
    </Section>
    <Section title="Setores de tanktop" description="Capacidade e limites físicos de cada setor publicado pelo backend.">
      {sectors.length ? <DataTable
        rows={sectors}
        gridId="modelo-navio-setores"
        rowKey={(row, index) => row.id ?? `${row.porao}-${row.nome}-${index}`}
        columns={[
          { key: 'porao', label: 'Porão' },
          { key: 'nome', label: 'Setor' },
          { key: 'capacidadeTM2', label: 'Capacidade', render: (row) => formatNumber(row.capacidadeTM2, ' t/m²') },
          { key: 'areaM2', label: 'Área', render: (row) => formatNumber(row.areaM2, ' m²') },
          { key: 'posLongInicio', label: 'Longitudinal inicial', render: (row) => formatNumber(row.posLongInicio, ' m') },
          { key: 'posLongFim', label: 'Longitudinal final', render: (row) => formatNumber(row.posLongFim, ' m') }
        ]}
      /> : <EmptyState title="Setores não informados" />}
    </Section>
    <JsonDetails value={model} title="Contrato completo do modelo" />
  </>;
}

export function ShipModelsPage() {
  const [models, setModels] = useState([]);
  const [profiles, setProfiles] = useState([]);
  const [scope, setScope] = useState('TODOS');
  const [query, setQuery] = useState('');
  const [selectedKey, setSelectedKey] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [modelsResponse, profilesResponse] = await Promise.all([
        steelCoilApi.listarTemplates(),
        steelCoilApi.listarNavios()
      ]);
      const nextModels = normalizeList(modelsResponse).map((item) => ({ ...item, isTemplate: true }));
      const nextProfiles = normalizeList(profilesResponse).map((item) => ({ ...item, isTemplate: false }));
      setModels(nextModels);
      setProfiles(nextProfiles);
      const available = [...nextModels, ...nextProfiles];
      setSelectedKey((current) => available.some((item) => modelKey(item) === current) ? current : '');
    } catch (reason) {
      setModels([]);
      setProfiles([]);
      setSelectedKey('');
      setError(formatError(reason, 'Não foi possível carregar os modelos de navio.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const allRows = useMemo(() => [...models, ...profiles], [models, profiles]);
  const rows = useMemo(() => {
    const term = sanitizeText(query).toLocaleLowerCase('pt-BR');
    return allRows.filter((item) => {
      if (scope === 'MODELOS' && !item.isTemplate) return false;
      if (scope === 'PERFIS' && item.isTemplate) return false;
      if (!term) return true;
      return [item.nome, item.imo, item.classe, item.versaoDadosHidrostaticos, item.versaoDadosEstruturais]
        .some((value) => sanitizeText(value).toLocaleLowerCase('pt-BR').includes(term));
    });
  }, [allRows, query, scope]);
  const selected = allRows.find((item) => modelKey(item) === selectedKey) ?? null;
  const completeCount = allRows.filter((item) => (item.poroes?.length ?? item.totalPoroes ?? 0) > 0).length;

  return <>
    <PageHeader
      eyebrow="Cadastros"
      title="Modelos de navio"
      description="Catálogo unificado dos modelos estruturais e perfis operacionais já publicados pelo backend de estiva bulk."
      actions={<><button className="secondary" type="button" onClick={reload} disabled={loading}>Atualizar</button><ManualModelosNavio /></>}
    />
    <Message type="error" onClose={() => setError('')}>{error}</Message>
    <div className="metrics-grid">
      <MetricCard label="Modelos" value={models.length} />
      <MetricCard label="Perfis operacionais" value={profiles.length} />
      <MetricCard label="Com geometria" value={completeCount} />
      <MetricCard label="Exibidos" value={rows.length} />
    </div>
    <Section
      title="Catálogo"
      description="Os registros permanecem separados por finalidade, mas são pesquisados e comparados em uma única tela."
      actions={<div className="actions">
        <select aria-label="Tipo de registro" value={scope} onChange={(event) => setScope(event.target.value)}>
          <option value="TODOS">Todos</option>
          <option value="MODELOS">Somente modelos</option>
          <option value="PERFIS">Somente perfis operacionais</option>
        </select>
        <input type="search" aria-label="Pesquisar modelos" placeholder="Nome, IMO, classe ou versão" value={query} onChange={(event) => setQuery(event.target.value)} />
      </div>}
    >
      {loading ? <Loading label="Carregando modelos e perfis..." /> : rows.length ? <DataTable
        rows={rows}
        gridId="catalogo-modelos-navio"
        exportFileName="modelos-navio"
        rowKey={modelKey}
        columns={[
          { key: 'tipo', label: 'Tipo', render: (row) => <StatusBadge value={row.isTemplate ? 'MODELO' : 'PERFIL'} /> },
          { key: 'nome', label: 'Nome' },
          { key: 'imo', label: 'IMO' },
          { key: 'classe', label: 'Classe' },
          { key: 'versaoPerfil', label: 'Versão' },
          { key: 'totalPoroes', label: 'Porões', render: (row) => row.totalPoroes ?? row.poroes?.length ?? 0 },
          { key: 'lpp', label: 'LPP', render: (row) => formatNumber(row.lpp, ' m') },
          { key: 'acao', label: 'Ação', render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedKey(modelKey(row))}>Ver detalhes</button> }
        ]}
      /> : <EmptyState title="Nenhum registro encontrado" description="Altere o filtro ou confirme se os modelos estão cadastrados no backend." />}
    </Section>
    <Section title="Detalhes do registro" description="Inspector completo do contrato retornado pelo backend.">
      <ModelDetails model={selected} />
    </Section>
  </>;
}
