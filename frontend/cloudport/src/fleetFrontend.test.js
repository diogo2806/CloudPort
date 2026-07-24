import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';

const apiSource = await readFile(new URL('./fleetApi.js', import.meta.url), 'utf8');
const pageSource = await readFile(new URL('./pages/FleetPage.jsx', import.meta.url), 'utf8');
const appointmentsSource = await readFile(new URL('./pages/GateAppointmentsPage.jsx', import.meta.url), 'utf8');
const routesSource = await readFile(new URL('./pages/OperationalPages.jsx', import.meta.url), 'utf8');
const helpSource = await readFile(new URL('./fleetContextHelp.js', import.meta.url), 'utf8');
const contextHelpSource = await readFile(new URL('./ContextHelp.jsx', import.meta.url), 'utf8');

function expectSource(source, pattern, message) {
  assert.match(source, pattern, message);
}

test('fleetApi cobre CRUD, vínculo, status e consulta elegível', () => {
  expectSource(apiSource, /const BASE = '\/gate\/frota\/veiculos'/, 'deve usar o endpoint canônico da frota');
  expectSource(apiSource, /obterMinhaTransportadora/, 'deve consultar o vínculo da sessão');
  expectSource(apiSource, /listarElegiveis:\s*\(transportadoraId\)/, 'deve consultar apenas veículos elegíveis');
  expectSource(apiSource, /method: 'POST'/, 'deve criar veículos');
  expectSource(apiSource, /method: 'PUT'/, 'deve atualizar veículos');
  expectSource(apiSource, /method: 'PATCH'/, 'deve alterar o status');
});

test('FleetPage usa a transportadora vinculada no portal do cliente', () => {
  expectSource(pageSource, /Veículos e carretas de transportadoras/, 'deve identificar a tela');
  expectSource(pageSource, /fleetApi\.obterMinhaTransportadora\(\)/, 'deve carregar o vínculo autenticado');
  expectSource(pageSource, /Transportadora vinculada/, 'deve exibir a empresa sem solicitar ID ao cliente');
  expectSource(pageSource, /fleetApi\.atualizarStatus\(row\.id, !row\.ativo\)/, 'deve ativar ou inativar');
  expectSource(pageSource, /Placas duplicadas são bloqueadas/, 'deve explicar o bloqueio principal');
});

test('Agendamento seleciona somente veículo elegível', () => {
  expectSource(appointmentsSource, /fleetApi\.listarElegiveis\(Number\(form\.transportadoraId\)\)/, 'deve carregar a frota ativa da transportadora');
  expectSource(appointmentsSource, /<span>Veículo elegível<\/span><select/, 'deve usar uma seleção amigável');
  assert.doesNotMatch(appointmentsSource, /<span>Veículo ID<\/span>/, 'não deve solicitar ID livre de veículo');
  expectSource(appointmentsSource, /Veículos inativos ou de outra empresa são bloqueados novamente pelo backend/, 'deve explicar a validação defensiva');
});

test('Rotas expõem cadastro administrativo e Minha frota no CAP', () => {
  expectSource(routesSource, /'\/home\/cadastros\/frota': \{ component: 'fleet' \}/, 'deve registrar a rota administrativa');
  expectSource(routesSource, /'\/home\/cap\/frota': \{ component: 'fleet' \}/, 'deve registrar a rota da transportadora');
  expectSource(routesSource, /'\/home\/gate\/agendamentos': \{ component: 'gateAppointments' \}/, 'deve usar a tela especializada de agendamento');
});

test('Manual contextual cobre todos os tópicos obrigatórios', () => {
  for (const field of ['purpose', 'flow', 'fields', 'permissions', 'states', 'blockers', 'example', 'shortcuts', 'processPath', 'documentationUrl']) {
    expectSource(helpSource, new RegExp(`${field}:`), `deve informar ${field}`);
  }
  expectSource(helpSource, /\/home\/cadastros\/frota/, 'deve cobrir a rota administrativa');
  expectSource(helpSource, /\/home\/cap\/frota/, 'deve cobrir Minha frota');
  expectSource(helpSource, /\/home\/gate\/agendamentos/, 'deve cobrir agendamentos');
  expectSource(contextHelpSource, /resolveFleetContextHelp\(activePath, baseHelp\)/, 'deve integrar o manual ao ícone de ajuda');
});
