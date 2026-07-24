import test from 'node:test';
import assert from 'node:assert/strict';
import {
  BILL_COMPANY_ROLES,
  buildLinksPayload,
  companiesForRole,
  companyOptionLabel,
  selectionFromLinks
} from './generalCargoCompanyLinksModel.js';

test('filtra empresas ativas pelo papel e mantém vínculo inativo visível', () => {
  const companies = [
    { id: '1', codigo: 'CLI-1', razaoSocial: 'Cliente ativo', ativo: true, papeis: ['CLIENTE'] },
    { id: '2', codigo: 'CLI-2', razaoSocial: 'Cliente inativo', ativo: false, papeis: ['CLIENTE'] },
    { id: '3', codigo: 'OPE-1', razaoSocial: 'Operador', ativo: true, papeis: ['OPERADOR'] }
  ];

  assert.deepEqual(companiesForRole(companies, 'CLIENTE').map((item) => item.id), ['1']);
  assert.deepEqual(companiesForRole(companies, 'CLIENTE', '2').map((item) => item.id), ['1', '2']);
  assert.match(companyOptionLabel(companies[1]), /INATIVA/);
});

test('monta payload somente com papéis selecionados', () => {
  const selection = { CLIENTE: 'empresa-1', OPERADOR: '', EMBARCADOR: 'empresa-2' };
  const payload = buildLinksPayload(selection, BILL_COMPANY_ROLES);

  assert.deepEqual(payload, {
    vinculos: [
      { papel: 'CLIENTE', empresaId: 'empresa-1' },
      { papel: 'EMBARCADOR', empresaId: 'empresa-2' }
    ]
  });
});

test('converte resposta da API em seleção por papel', () => {
  assert.deepEqual(selectionFromLinks([
    { papel: 'CLIENTE', empresaId: 'empresa-1' },
    { papel: 'TRANSPORTADORA', empresaId: 'empresa-2' }
  ]), {
    CLIENTE: 'empresa-1',
    TRANSPORTADORA: 'empresa-2'
  });
});
