import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';

const apiSource = await readFile(new URL('./fleetApi.js', import.meta.url), 'utf8');
const pageSource = await readFile(new URL('./pages/FleetPage.jsx', import.meta.url), 'utf8');

function expectSource(source, pattern, message) {
  assert.match(source, pattern, message);
}

test('fleetApi cobre CRUD, status e consulta elegível', () => {
  expectSource(apiSource, /const BASE = '\/gate\/frota\/veiculos'/, 'deve usar o endpoint canônico da frota');
  expectSource(apiSource, /listarElegiveis:\s*\(transportadoraId\)/, 'deve consultar apenas veículos elegíveis');
  expectSource(apiSource, /method: 'POST'/, 'deve criar veículos');
  expectSource(apiSource, /method: 'PUT'/, 'deve atualizar veículos');
  expectSource(apiSource, /method: 'PATCH'/, 'deve alterar o status');
});

test('FleetPage possui manutenção, busca e manual contextual', () => {
  expectSource(pageSource, /Veículos e carretas de transportadoras/, 'deve identificar a tela');
  expectSource(pageSource, /fleetApi\.criar\(payload\)/, 'deve criar veículos');
  expectSource(pageSource, /fleetApi\.atualizar\(editingId, payload\)/, 'deve editar veículos');
  expectSource(pageSource, /fleetApi\.atualizarStatus\(row\.id, !row\.ativo\)/, 'deve ativar ou inativar');
  expectSource(pageSource, /Placas duplicadas são bloqueadas/, 'deve explicar o bloqueio principal');
  expectSource(pageSource, /placeholder="Placa, tipo ou transportadora"/, 'deve oferecer busca operacional');
});
