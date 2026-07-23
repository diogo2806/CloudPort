import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildOperatorTasks,
  createOperatorCommand,
  enqueueOperatorCommand,
  isValidIso6346,
  operatorCommandKey,
  operatorQueueStorageKey,
  permittedOperatorSources,
  readOperatorQueue,
  removeCompletedCommands,
  updateOperatorCommand,
  validateOperatorScan,
  writeOperatorQueue
} from './operatorMode.js';

test('carrega somente fontes permitidas por papel', () => {
  assert.deepEqual(permittedOperatorSources({ roles: ['ROLE_OPERADOR_GATE'] }).map((item) => item.key), ['gate', 'inventory']);
  assert.deepEqual(permittedOperatorSources({ perfil: 'OPERADOR_PATIO' }).map((item) => item.key), ['yard', 'rail', 'inventory']);
  assert.deepEqual(permittedOperatorSources({ roles: ['PLANEJADOR'] }).map((item) => item.key), ['gate', 'yard', 'rail', 'inventory']);
  assert.deepEqual(permittedOperatorSources({ roles: ['CONSULTA'] }), []);
});

test('monta e prioriza tarefas autorizadas por bloqueio, execução e prazo', () => {
  const tasks = buildOperatorTasks({
    gate: { payload: { agendamentos: [{ id: 1, placaVeiculo: 'ABC1D23', status: 'EM_FILA', tempoFilaMinutos: 20 }] } },
    yard: { payload: [
      { id: 20, numeroConteiner: 'MSCU6639870', status: 'PENDENTE', posicaoOrigem: 'A-01', posicaoDestino: 'B-02' },
      { id: 21, numeroConteiner: 'CSQU3054383', status: 'BLOQUEADA', posicaoOrigem: 'A-02', posicaoDestino: 'B-03' }
    ] },
    inventory: { status: 'rejected', error: new Error('offline') }
  });
  assert.equal(tasks.length, 3);
  assert.equal(tasks[0].source, 'yard');
  assert.equal(tasks[0].status, 'BLOQUEADA');
  assert.equal(tasks[1].source, 'gate');
  assert.equal(tasks[2].action, 'START_YARD_ORDER');
  assert.equal(tasks.some((item) => item.source === 'inventory'), false);
});

test('cria ações ferroviárias a partir de ordens aninhadas', () => {
  const tasks = buildOperatorTasks({ rail: { payload: [{
    id: 9,
    identificadorTrem: 'MRS-2048',
    ordens: [
      { id: 91, numeroConteiner: 'MSCU6639870', status: 'PENDENTE', identificadorVagao: 'VAG-01' },
      { id: 92, numeroConteiner: 'CSQU3054383', status: 'EM_EXECUCAO', identificadorVagao: 'VAG-02' }
    ]
  }] } });
  assert.equal(tasks.length, 2);
  assert.equal(tasks.find((item) => item.sourceId === 91).action, 'START_RAIL_ORDER');
  assert.equal(tasks.find((item) => item.sourceId === 92).action, 'COMPLETE_RAIL_ORDER');
  assert.equal(tasks[0].visitId, 9);
});

test('valida contêiner ISO 6346 e rejeita dígito incorreto com orientação', () => {
  assert.equal(isValidIso6346('MSCU6639870'), true);
  assert.equal(isValidIso6346('CSQU3054383'), true);
  assert.equal(isValidIso6346('MSCU6639871'), false);

  const valid = validateOperatorScan('MSCU 663987 0', ['CONTAINER']);
  assert.deepEqual(valid, { valid: true, type: 'CONTAINER', value: 'MSCU6639870', reason: '', correction: '' });

  const invalid = validateOperatorScan('MSCU6639871', ['CONTAINER']);
  assert.equal(invalid.valid, false);
  assert.equal(invalid.type, 'CONTAINER');
  assert.match(invalid.reason, /ISO 6346/);
  assert.match(invalid.correction, /dígito verificador/);
});

test('usa a mesma validação para texto prefixado, QR JSON e URL', () => {
  const prefixed = validateOperatorScan('CONTAINER: MSCU6639870');
  const json = validateOperatorScan('{"type":"CONTAINER","value":"MSCU6639870"}');
  const url = validateOperatorScan('https://terminal.local/read?container=MSCU6639870');
  assert.equal(prefixed.valid, true);
  assert.deepEqual(json, prefixed);
  assert.deepEqual(url, prefixed);
});

test('reconhece placas, posições e incompatibilidade de tipo esperado', () => {
  assert.equal(validateOperatorScan('ABC1D23', ['PLACA']).valid, true);
  assert.equal(validateOperatorScan('ABC1234', ['PLACA']).valid, true);
  assert.equal(validateOperatorScan('POSICAO:A-01-02', ['POSICAO']).valid, true);
  const mismatch = validateOperatorScan('ABC1D23', ['CONTAINER']);
  assert.equal(mismatch.valid, false);
  assert.match(mismatch.reason, /espera CONTAINER/);
  assert.match(mismatch.correction, /objeto físico/);
});

test('informa formato desconhecido e leitura vazia sem resposta genérica', () => {
  const empty = validateOperatorScan('');
  assert.equal(empty.valid, false);
  assert.match(empty.reason, /Nenhum código/);
  assert.ok(empty.correction.length > 20);

  const unknown = validateOperatorScan('@@@');
  assert.equal(unknown.valid, false);
  assert.equal(unknown.type, 'DESCONHECIDO');
  assert.match(unknown.correction, /contêiner ISO 6346/);
});

test('gera chave idempotente determinística e impede comando duplicado', () => {
  const task = { id: 'yard-20', source: 'yard', sourceId: 20, action: 'START_YARD_ORDER' };
  const scan = { valid: true, type: 'CONTAINER', value: 'MSCU6639870' };
  const first = createOperatorCommand(task, task.action, scan, { email: 'operador@terminal.local' });
  const second = createOperatorCommand(task, task.action, scan, { email: 'operador@terminal.local' });
  assert.equal(first.idempotencyKey, second.idempotencyKey);
  assert.equal(first.idempotencyKey, operatorCommandKey(task, task.action, scan.value));

  const inserted = enqueueOperatorCommand([], first);
  assert.equal(inserted.inserted, true);
  const duplicate = enqueueOperatorCommand(inserted.queue, second);
  assert.equal(duplicate.inserted, false);
  assert.equal(duplicate.queue.length, 1);
});

test('permite nova operação somente após conclusão ou descarte da anterior', () => {
  const task = { id: 'rail-9-91', source: 'rail', sourceId: 91, visitId: 9 };
  const scan = { type: 'CONTAINER', value: 'MSCU6639870' };
  const command = createOperatorCommand(task, 'START_RAIL_ORDER', scan);
  const completed = updateOperatorCommand([command], command.id, { status: 'CONCLUIDA' });
  assert.deepEqual(removeCompletedCommands(completed), []);
  assert.equal(enqueueOperatorCommand(completed, command).inserted, true);
});

test('fila local é separada por usuário e tolera armazenamento inválido', () => {
  assert.notEqual(operatorQueueStorageKey({ id: 1 }), operatorQueueStorageKey({ id: 2 }));
  const values = new Map();
  const storage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };
  const queue = [{ id: 'op-1', status: 'PENDENTE' }];
  assert.equal(writeOperatorQueue(storage, 'queue', queue), true);
  assert.deepEqual(readOperatorQueue(storage, 'queue'), queue);
  assert.deepEqual(readOperatorQueue({ getItem: () => '{inválido' }, 'queue'), []);
  assert.equal(writeOperatorQueue({ setItem: () => { throw new Error('quota'); } }, 'queue', []), false);
});
