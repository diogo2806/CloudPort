import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const apiSource = await readFile(new URL('./companiesApi.js', import.meta.url), 'utf8');
const pageSource = await readFile(new URL('./pages/CompaniesPage.jsx', import.meta.url), 'utf8');

function expectSource(source, pattern, message) {
  assert.match(source, pattern, message);
}

test('companiesApi mantém os contratos de consulta e manutenção', () => {
  expectSource(apiSource, /const BASE = '\/api\/carga-geral\/empresas'/, 'deve usar o endpoint canônico de empresas');
  expectSource(apiSource, /listar:\s*\(query\)\s*=>\s*request\(BASE,\s*\{ query \}\)/, 'deve encaminhar filtros na listagem');
  expectSource(apiSource, /criar:\s*\(body\)\s*=>\s*request\(BASE,\s*\{ method: 'POST', body \}\)/, 'deve criar por POST');
  expectSource(apiSource, /atualizar:\s*\(id, body\).*method: 'PUT'/s, 'deve atualizar por PUT');
  expectSource(apiSource, /encodeURIComponent\(id\)/, 'deve codificar o identificador na URL');
  expectSource(apiSource, /atualizarStatus:\s*\(id, ativo\).*method: 'PATCH'.*query: \{ ativo \}/s, 'deve alterar status por PATCH');
});

test('CompaniesPage cobre carregamento, vazio, erro e sucesso', () => {
  expectSource(pageSource, /setCarregando\(true\)/, 'deve iniciar o estado de carregamento');
  expectSource(pageSource, /<Loading label="Carregando empresas\.\.\." \/>/, 'deve renderizar carregamento explícito');
  expectSource(pageSource, /<EmptyState title="Nenhuma empresa encontrada"/, 'deve renderizar estado vazio');
  expectSource(pageSource, /<Message type="error">\{erro\}<\/Message>/, 'deve renderizar erros');
  expectSource(pageSource, /<Message type="success"[^>]*>\{sucesso\}<\/Message>/, 'deve renderizar sucesso');
});

test('CompaniesPage protege consulta e manutenção por papel', () => {
  expectSource(pageSource, /hasAnyRole\(session, 'ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE'\)/, 'deve restringir consulta aos papéis autorizados');
  expectSource(pageSource, /hasAnyRole\(session, 'ADMIN_PORTO'\)/, 'deve restringir manutenção ao administrador do porto');
  expectSource(pageSource, /Você não possui permissão para consultar empresas/, 'deve explicar a falta de permissão');
  expectSource(pageSource, /Modo consulta\. Somente ADMIN_PORTO/, 'deve informar o modo somente leitura');
  expectSource(pageSource, /\{podeManter && <Section title=\{tituloFormulario\}/, 'não deve expor o formulário sem permissão');
});

test('CompaniesPage implementa criação, edição e alteração de status', () => {
  expectSource(pageSource, /companiesApi\.criar\(form\)/, 'deve criar empresas');
  expectSource(pageSource, /companiesApi\.atualizar\(editandoId, form\)/, 'deve atualizar empresas');
  expectSource(pageSource, /companiesApi\.atualizarStatus\(row\.id, !row\.ativo\)/, 'deve ativar ou inativar empresas');
  expectSource(pageSource, /Empresa cadastrada\./, 'deve confirmar cadastro');
  expectSource(pageSource, /Empresa atualizada\./, 'deve confirmar atualização');
  expectSource(pageSource, /Empresa inativada\./, 'deve confirmar inativação');
  expectSource(pageSource, /Empresa ativada\./, 'deve confirmar ativação');
});

test('CompaniesPage mantém validações e recuperação de falhas', () => {
  expectSource(pageSource, /if \(!podeManter \|\| busy \|\| !form\.papeis\.length\) return;/, 'deve bloquear envio inválido ou duplicado');
  expectSource(pageSource, /required=\{\['codigo', 'razaoSocial', 'documento', 'pais'\]\.includes\(campo\)\}/, 'deve exigir os campos principais');
  expectSource(pageSource, /formatError\(reason, 'Não foi possível carregar as empresas\.'\)/, 'deve tratar erro de listagem');
  expectSource(pageSource, /formatError\(reason, 'Não foi possível salvar a empresa\.'\)/, 'deve tratar conflito e validação no salvamento');
  expectSource(pageSource, /formatError\(reason, 'Não foi possível alterar o status\.'\)/, 'deve tratar falha na alteração de status');
});

test('CompaniesPage mantém manual contextual e busca operacional', () => {
  expectSource(pageSource, /docs\/manuais\/empresas-clientes\.md/, 'deve apontar para o manual completo');
  expectSource(pageSource, /aria-label="Abrir manual da tela"/, 'deve manter o atalho de manual acessível');
  expectSource(pageSource, /companiesApi\.listar\(\{ busca \}\)/, 'deve consultar usando o termo informado');
  expectSource(pageSource, /placeholder="Nome, código ou documento"/, 'deve explicar os campos pesquisáveis');
});
