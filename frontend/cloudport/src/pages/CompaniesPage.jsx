import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError, hasAnyRole } from '../api.js';
import { DataTable, EmptyState, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { companiesApi } from '../companiesApi.js';

const MANUAL_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/empresas-clientes.md';
const PAPEIS = ['CLIENTE', 'EMBARCADOR', 'CONSIGNATARIO', 'IMPORTADOR', 'EXPORTADOR', 'DONO_CARGA', 'OPERADOR', 'AGENTE', 'TRANSPORTADORA'];
const vazio = () => ({ codigo: '', razaoSocial: '', nomeFantasia: '', documento: '', inscricaoEstadual: '', endereco: '', contato: '', email: '', telefone: '', pais: 'BRASIL', observacoes: '', papeis: ['CLIENTE'] });

export function CompaniesPage({ session }) {
  const podeConsultar = hasAnyRole(session, 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE');
  const podeManter = hasAnyRole(session, 'ADMIN_PORTO');
  const [empresas, setEmpresas] = useState([]);
  const [form, setForm] = useState(vazio);
  const [editandoId, setEditandoId] = useState('');
  const [busca, setBusca] = useState('');
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [carregando, setCarregando] = useState(false);
  const [busy, setBusy] = useState(false);

  const tituloFormulario = useMemo(() => editandoId ? 'Editar empresa' : 'Nova empresa', [editandoId]);

  const carregar = useCallback(async () => {
    if (!podeConsultar) return;
    setCarregando(true);
    setErro('');
    try {
      setEmpresas(await companiesApi.listar({ busca }) || []);
    } catch (reason) {
      setErro(formatError(reason, 'Não foi possível carregar as empresas.'));
    } finally {
      setCarregando(false);
    }
  }, [busca, podeConsultar]);

  useEffect(() => { carregar(); }, [carregar]);

  function limparFormulario() {
    setForm(vazio());
    setEditandoId('');
  }

  async function salvar(event) {
    event.preventDefault();
    if (!podeManter || busy || !form.papeis.length) return;
    setBusy(true);
    setErro('');
    setSucesso('');
    try {
      if (editandoId) {
        await companiesApi.atualizar(editandoId, form);
        setSucesso('Empresa atualizada.');
      } else {
        await companiesApi.criar(form);
        setSucesso('Empresa cadastrada.');
      }
      limparFormulario();
      await carregar();
    } catch (reason) {
      setErro(formatError(reason, 'Não foi possível salvar a empresa.'));
    } finally {
      setBusy(false);
    }
  }

  function papel(nome, marcado) {
    setForm((atual) => ({ ...atual, papeis: marcado ? [...new Set([...atual.papeis, nome])] : atual.papeis.filter((papelAtual) => papelAtual !== nome) }));
  }

  function editar(row) {
    if (!podeManter) return;
    setEditandoId(row.id);
    setForm({ ...vazio(), ...row, papeis: row.papeis || [] });
    globalThis.scrollTo?.({ top: 0, behavior: 'smooth' });
  }

  async function status(row) {
    if (!podeManter) return;
    setErro('');
    try {
      await companiesApi.atualizarStatus(row.id, !row.ativo);
      setSucesso(row.ativo ? 'Empresa inativada.' : 'Empresa ativada.');
      await carregar();
    } catch (reason) {
      setErro(formatError(reason, 'Não foi possível alterar o status.'));
    }
  }

  if (!podeConsultar) {
    return <>
      <PageHeader eyebrow="Cadastros" title="Empresas e clientes" description="Cadastro mestre de clientes e partes relacionadas à carga." actions={<a className="secondary" href={MANUAL_URL} target="_blank" rel="noreferrer" title="Abrir manual da tela" aria-label="Abrir manual da tela">?</a>} />
      <Message type="error">Você não possui permissão para consultar empresas. Solicite um dos perfis: ADMIN_PORTO, PLANEJADOR ou OPERADOR_GATE.</Message>
    </>;
  }

  return <>
    <PageHeader eyebrow="Cadastros" title="Empresas e clientes" description="Cadastro mestre de clientes e partes relacionadas à carga." actions={<a className="secondary" href={MANUAL_URL} target="_blank" rel="noreferrer" title="Abrir manual da tela" aria-label="Abrir manual da tela">?</a>} />
    <Message type="error">{erro}</Message>
    <Message type="success" onClose={() => setSucesso('')}>{sucesso}</Message>
    {!podeManter && <Message type="info">Modo consulta. Somente ADMIN_PORTO pode criar, editar, ativar ou inativar empresas.</Message>}

    {podeManter && <Section title={tituloFormulario} description="O documento e o código devem ser únicos.">
      <form className="planner-selection-grid" onSubmit={salvar}>
        {['codigo', 'razaoSocial', 'nomeFantasia', 'documento', 'inscricaoEstadual', 'contato', 'email', 'telefone', 'pais', 'endereco'].map((campo) => <label className="field" key={campo}><span>{campo}</span><input required={['codigo', 'razaoSocial', 'documento', 'pais'].includes(campo)} value={form[campo] || ''} onChange={(event) => setForm((atual) => ({ ...atual, [campo]: event.target.value }))} /></label>)}
        <div className="field"><span>Papéis</span>{PAPEIS.map((nome) => <label key={nome}><input type="checkbox" checked={form.papeis.includes(nome)} onChange={(event) => papel(nome, event.target.checked)} />{nome}</label>)}</div>
        <label className="field"><span>Observações</span><textarea value={form.observacoes || ''} onChange={(event) => setForm((atual) => ({ ...atual, observacoes: event.target.value }))} /></label>
        <div className="field"><span>Ação</span><button disabled={busy || !form.papeis.length}>{busy ? 'Salvando...' : editandoId ? 'Salvar alterações' : 'Cadastrar empresa'}</button>{editandoId && <button type="button" className="secondary" onClick={limparFormulario}>Cancelar edição</button>}</div>
      </form>
    </Section>}

    <Section title="Empresas cadastradas"><label className="field"><span>Buscar</span><input value={busca} onChange={(event) => setBusca(event.target.value)} placeholder="Nome, código ou documento" /></label>
      {carregando ? <Loading label="Carregando empresas..." /> : empresas.length ? <DataTable gridId="companies" rows={empresas} rowKey="id" columns={[{ key: 'codigo', label: 'Código' }, { key: 'razaoSocial', label: 'Razão social' }, { key: 'documento', label: 'Documento' }, { key: 'papeis', label: 'Papéis', render: (row) => row.papeis?.join(', ') }, { key: 'ativo', label: 'Status', render: (row) => <StatusBadge value={row.ativo ? 'ATIVA' : 'INATIVA'} /> }, ...(podeManter ? [{ key: 'acao', label: 'Ações', render: (row) => <><button className="secondary small" onClick={() => editar(row)}>Editar</button><button className="secondary small" onClick={() => status(row)}>{row.ativo ? 'Inativar' : 'Ativar'}</button></> }] : [])]} /> : <EmptyState title="Nenhuma empresa encontrada" description={busca ? 'Revise o termo pesquisado.' : 'Cadastre a primeira empresa para utilizá-la nos fluxos operacionais.'} />}
    </Section>
  </>;
}
