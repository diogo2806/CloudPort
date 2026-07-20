import assert from 'node:assert/strict';
import test from 'node:test';
import { buildReceivingRows, countReceivingRows } from './pages/yard/yardReceivingPlanModel.js';

const visit = {
  id: 10,
  codigo: 'TV-10',
  checkinEm: '2026-07-20T10:00:00',
  transacoes: []
};

test('monta contêiner elegível a partir de pré-aviso e booking', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 101,
      tipoOperacao: 'EXPORTACAO',
      status: 'EM_PROCESSAMENTO',
      unidadeReferencia: 'REFU1234567',
      bookingId: 1,
      preadviceId: 2,
      troubleAtivo: false
    }]
  }, {
    bookings: [{ id: 1, codigo: 'BK-1', armador: 'MSC', viagem: 'MSC-001', validadeFim: '2026-07-21T12:00:00' }],
    preAvisos: [{ id: 2, codigo: 'PA-2', tipo: 'EXPORTACAO', bookingId: 1, unidadeReferencia: 'REFU1234567', isoType: '45R1', pesoBrutoKg: 25000 }]
  });

  assert.equal(rows.length, 1);
  assert.equal(rows[0].eligible, true);
  assert.equal(rows[0].referenceLabel, 'Booking BK-1 · Pré-aviso PA-2');
  assert.deepEqual(rows[0].container, {
    id: 101,
    codigo: 'REFU1234567',
    etaChegada: '2026-07-20T10:00:00',
    etaPartida: '2026-07-21T12:00:00',
    pesoToneladas: 25,
    tipoCarga: 'EXPORTACAO',
    destino: 'A_DEFINIR_NO_DISPATCH',
    restricoes: '',
    categoria: 'EXPORTACAO',
    armador: 'MSC',
    visitaSaida: 'MSC-001',
    comprimentoPes: 40,
    tipoEquipamento: 'REEFER',
    estadoCarga: 'CHEIO',
    refrigerado: true,
    perigoso: false,
    classeImo: null,
    numeroOnu: null
  });
});

test('bloqueia importação sem BL ou ordem comercial', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 102,
      tipoOperacao: 'IMPORTACAO',
      status: 'PENDENTE',
      unidadeReferencia: 'MSCU1234567',
      troubleAtivo: false
    }]
  });

  assert.equal(rows[0].eligible, false);
  assert.match(rows[0].blockers.join(' '), /Bill of Lading/);
});

test('bloqueia transação finalizada e com trouble', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 103,
      tipoOperacao: 'EXPORTACAO',
      status: 'CONCLUIDA',
      unidadeReferencia: 'MSCU7654321',
      bookingId: 1,
      troubleAtivo: true
    }]
  }, {
    bookings: [{ id: 1, codigo: 'BK-1' }]
  });

  assert.equal(rows[0].eligible, false);
  assert.equal(rows[0].blockers.length, 2);
});

test('resume elegíveis, bloqueadas e avisos', () => {
  const summary = countReceivingRows([
    { eligible: true, warnings: [] },
    { eligible: true, warnings: ['Peso ausente'] },
    { eligible: false, warnings: [] }
  ]);

  assert.deepEqual(summary, { total: 3, eligible: 2, blocked: 1, withWarnings: 1 });
});
