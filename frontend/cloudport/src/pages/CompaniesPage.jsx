import { useCallback, useEffect, useState } from 'react';
import { formatError } from '../api.js';
import { DataTable, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { companiesApi } from '../companiesApi.js';

const MANUAL_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/empresas-clientes.md';
const PAPEIS = ['CLIENTE','EMBARCADOR','CONSIGNATARIO','IMPORTADOR','EXPORTADOR','DONO_CARGA','OPERADOR','AGENTE','TRANSPORTADORA'];
const vazio = () => ({ codigo:'', razaoSocial:'', nomeFantasia:'', documento:'', inscricaoEstadual:'', endereco:'', contato:'', email:'', telefone:'', pais:'BRASIL', observacoes:'', papeis:['CLIENTE'] });

export function CompaniesPage() {
  const [empresas,setEmpresas]=useState([]); const [form,setForm]=useState(vazio); const [busca,setBusca]=useState('');
  const [erro,setErro]=useState(''); const [sucesso,setSucesso]=useState(''); const [busy,setBusy]=useState(false);
  const carregar=useCallback(async()=>{try{setEmpresas(await companiesApi.listar({busca})||[]);}catch(e){setErro(formatError(e));}},[busca]);
  useEffect(()=>{carregar();},[carregar]);
  async function salvar(e){e.preventDefault();setBusy(true);setErro('');try{await companiesApi.criar(form);setForm(vazio());setSucesso('Empresa cadastrada.');await carregar();}catch(x){setErro(formatError(x));}finally{setBusy(false);}}
  function papel(nome,marcado){setForm(f=>({...f,papeis:marcado?[...new Set([...f.papeis,nome])]:f.papeis.filter(p=>p!==nome)}));}
  async function status(row){try{await companiesApi.atualizarStatus(row.id,!row.ativo);await carregar();}catch(e){setErro(formatError(e));}}
  return <>
    <PageHeader eyebrow="Cadastros" title="Empresas e clientes" description="Cadastro mestre de clientes e partes relacionadas à carga." actions={<a className="secondary" href={MANUAL_URL} target="_blank" rel="noreferrer" aria-label="Abrir manual da tela">Manual</a>} />
    <Message type="error">{erro}</Message><Message type="success" onClose={()=>setSucesso('')}>{sucesso}</Message>
    <Section title="Nova empresa" description="O documento e o código devem ser únicos."><form className="planner-selection-grid" onSubmit={salvar}>
      {['codigo','razaoSocial','nomeFantasia','documento','inscricaoEstadual','contato','email','telefone','pais','endereco'].map(c=><label className="field" key={c}><span>{c}</span><input required={['codigo','razaoSocial','documento','pais'].includes(c)} value={form[c]} onChange={e=>setForm(f=>({...f,[c]:e.target.value}))}/></label>)}
      <div className="field"><span>Papéis</span>{PAPEIS.map(p=><label key={p}><input type="checkbox" checked={form.papeis.includes(p)} onChange={e=>papel(p,e.target.checked)}/>{p}</label>)}</div>
      <label className="field"><span>Observações</span><textarea value={form.observacoes} onChange={e=>setForm(f=>({...f,observacoes:e.target.value}))}/></label>
      <div className="field"><span>Ação</span><button disabled={busy||!form.papeis.length}>{busy?'Salvando...':'Cadastrar empresa'}</button></div>
    </form></Section>
    <Section title="Empresas cadastradas"><label className="field"><span>Buscar</span><input value={busca} onChange={e=>setBusca(e.target.value)} placeholder="Nome, código ou documento"/></label>
      <DataTable gridId="companies" rows={empresas} rowKey="id" columns={[{key:'codigo',label:'Código'},{key:'razaoSocial',label:'Razão social'},{key:'documento',label:'Documento'},{key:'papeis',label:'Papéis',render:r=>r.papeis?.join(', ')},{key:'ativo',label:'Status',render:r=><StatusBadge value={r.ativo?'ATIVA':'INATIVA'}/>},{key:'acao',label:'Ação',render:r=><button className="secondary small" onClick={()=>status(r)}>{r.ativo?'Inativar':'Ativar'}</button>}]} />
    </Section>
  </>;
}
