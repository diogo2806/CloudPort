import assert from 'node:assert/strict';
import test from 'node:test';
import { buildReceivingRows, countReceivingRows } from './pages/yard/yardReceivingPlanModel.js';

const NOW = '2026-07-20T12:00:00-03:00';
const visit = {
  id: 10,
  codigo: 'TV-10',
  gateId: 5,
  transportadoraId: 20,
  motoristaId: 30,
  veiculoId: 40,
  checkinEm: '2026-07-20T10:00:00',
  transacoes: []
};

const activeBooking = {
  id: 1,
  codigo: 'BK-1',
  armador: 'MSC',
  viagem: 'MSC-001',
  transportadoraId: 20,
  quantidadeTotal: 10,
  quantidadeUtilizada: 2,
  status: 'ABERTO',
  validadeInicio: '2026-07-19T00:00:00-03:00',
  validadeFim: '2026-07-21T12:00:00-03:00'
};

const activePreadvice = {
  id: 2,
  codigo: 'PA-2',
  tipo: 'EXPORTACAO',
  bookingId: 1,
  unidadeReferencia: 'REFU1234567',
  isoType: '45R1',
  pesoBrutoKg: 25000,
  status: 'ATIVO'
};

test('monta contêiner elegível a partir de pré-aviso e booking válidos', () => {
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
    bookings: [activeBooking],
    preAvisos: [activePreadvice]
  }, {}, { now: NOW });

  assert.equal(rows.length, 1);
  assert.equal(rows[0].eligible, true);
  assert.equal(rows[0].referenceLabel, 'Booking BK-1 · Pré-aviso PA-2');
  assert.deepEqual(rows[0].container, {
    id: 101,
    codigo: 'REFU1234567',
    etaChegada: '2026-07-20T10:00:00',
    etaPartida: '2026-07-21T12:00:00-03:00',
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
  }, {}, {}, { now: NOW });

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
    bookings: [activeBooking]
  }, {}, { now: NOW });

  assert.equal(rows[0].eligible, false);
  assert.equal(rows[0].blockers.length, 2);
});

test('bloqueia referências expiradas, inativas ou sem saldo', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 104,
      tipoOperacao: 'EXPORTACAO',
      status: 'EM_PROCESSAMENTO',
      unidadeReferencia: 'REFU1234567',
      bookingId: 1,
      preadviceId: 2,
      troubleAtivo: false
    }]
  }, {
    bookings: [{
      ...activeBooking,
      status: 'UTILIZADO',
      quantidadeUtilizada: 10,
      validadeFim: '2026-07-19T23:59:59-03:00'
    }],
    preAvisos: [{ ...activePreadvice, status: 'UTILIZADO' }]
  }, {}, { now: NOW });

  assert.equal(rows[0].eligible, false);
  assert.match(rows[0].blockers.join(' '), /status UTILIZADO/);
  assert.match(rows[0].blockers.join(' '), /expirado/);
  assert.match(rows[0].blockers.join(' '), /sem saldo disponível/);
});

test('bloqueia Bill of Lading indisponível para importação', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 105,
      tipoOperacao: 'IMPORTACAO',
      status: 'EM_PROCESSAMENTO',
      unidadeReferencia: 'MSCU5555555',
      billOfLadingId: 50,
      troubleAtivo: false
    }]
  }, {}, {
    billsOfLading: [{
      id: 50,
      numero: 'BL-50',
      status: 'LIBERADO',
      quantidadeTotal: 1,
      quantidadeLiberada: 1,
      validadeInicio: '2026-07-01T00:00:00-03:00',
      validadeFim: '2026-07-31T23:59:59-03:00'
    }]
  }, { now: NOW });

  assert.equal(rows[0].eligible, false);
  assert.match(rows[0].blockers.join(' '), /Bill of Lading em status LIBERADO/);
  assert.match(rows[0].blockers.join(' '), /sem saldo disponível/);
});

test('bloqueia regra de acesso vigente da transportadora', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 106,
      tipoOperacao: 'EXPORTACAO',
      status: 'EM_PROCESSAMENTO',
      unidadeReferencia: 'REFU1234567',
      bookingId: 1,
      preadviceId: 2,
      troubleAtivo: false
    }]
  }, {
    bookings: [activeBooking],
    preAvisos: [activePreadvice]
  }, {
    regrasAcesso: [{
      gateId: 5,
      escopo: 'TRANSPORTADORA',
      referenciaId: 20,
      tipo: 'BLOQUEIO',
      motivo: 'Documentação cadastral vencida',
      inicioVigencia: '2026-07-20T00:00:00-03:00',
      fimVigencia: '2026-07-20T23:59:59-03:00',
      ativo: true
    }]
  }, { now: NOW });

  assert.equal(rows[0].eligible, false);
  assert.match(rows[0].blockers.join(' '), /Documentação cadastral vencida/);
});

test('ignora regra de acesso expirada ou de outro Gate', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 107,
      tipoOperacao: 'EXPORTACAO',
      status: 'EM_PROCESSAMENTO',
      unidadeReferencia: 'REFU1234567',
      bookingId: 1,
      preadviceId: 2,
      troubleAtivo: false
    }]
  }, {
    bookings: [activeBooking],
    preAvisos: [activePreadvice]
  }, {
    regrasAcesso: [
      {
        gateId: 5,
        escopo: 'TRANSPORTADORA',
        referenciaId: 20,
        tipo: 'BLOQUEIO',
        motivo: 'Bloqueio expirado',
        fimVigencia: '2026-07-19T23:59:59-03:00',
        ativo: true
      },
      {
        gateId: 99,
        escopo: 'TRANSPORTADORA',
        referenciaId: 20,
        tipo: 'BLOQUEIO',
        motivo: 'Outro Gate',
        ativo: true
      }
    ]
  }, { now: NOW });

  assert.equal(rows[0].eligible, true);
});

test('bloqueia booking de outra transportadora', () => {
  const rows = buildReceivingRows({
    ...visit,
    transacoes: [{
      id: 108,
      tipoOperacao: 'EXPORTACAO',
      status: 'EM_PROCESSAMENTO',
      unidadeReferencia: 'REFU1234567',
      bookingId: 1,
      preadviceId: 2,
      troubleAtivo: false
    }]
  }, {
    bookings: [{ ...activeBooking, transportadoraId: 999 }],
    preAvisos: [activePreadvice]
  }, {}, { now: NOW });

  assert.equal(rows[0].eligible, false);
  assert.match(rows[0].blockers.join(' '), /outra transportadora/);
});

test('resume elegíveis, bloqueadas e avisos', () => {
  const summary = countReceivingRows([
    { eligible: true, warnings: [] },
    { eligible: true, warnings: ['Peso ausente'] },
    { eligible: false, warnings: [] }
  ]);

  assert.deepEqual(summary, { total: 3, eligible: 2, blocked: 1, withWarnings: 1 });
});
