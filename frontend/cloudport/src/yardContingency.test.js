import assert from 'node:assert/strict';
import test from 'node:test';
import {
  createYardSnapshot,
  fingerprintYardSnapshot,
  isYardSnapshotExpired,
  readYardSnapshot,
  reconcileYardSnapshots,
  writeYardSnapshot,
  YARD_SNAPSHOT_MAX_AGE_MS
} from './pages/yard/yardContingency.js';

function memoryStorage() {
  const values = new Map();
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };
}

test('persiste e recupera a fotografia operacional versionada', () => {
  const storage = memoryStorage();
  const data = { positions: [{ id: 1, bloco: 'A1' }], orders: [] };
  const synchronizedAt = '2026-07-23T10:00:00.000Z';

  const written = writeYardSnapshot(data, storage, synchronizedAt);
  const recovered = readYardSnapshot(storage);

  assert.equal(written.synchronizedAt, synchronizedAt);
  assert.deepEqual(recovered.data, data);
  assert.equal(recovered.fingerprint, fingerprintYardSnapshot(data));
});

test('identifica fotografia expirada sem descartar os dados de contingência', () => {
  const synchronizedAt = '2026-07-23T10:00:00.000Z';
  const snapshot = createYardSnapshot({ positions: [] }, synchronizedAt);
  const now = Date.parse(synchronizedAt) + YARD_SNAPSHOT_MAX_AGE_MS + 1;

  assert.equal(isYardSnapshotExpired(snapshot, now), true);
  assert.deepEqual(snapshot.data, { positions: [] });
});

test('detecta divergência após reconexão e não sinaliza estados equivalentes', () => {
  const local = createYardSnapshot({ positions: [{ id: 1, status: 'OCUPADA' }] });

  assert.equal(reconcileYardSnapshots(local, { positions: [{ id: 1, status: 'OCUPADA' }] }).diverged, false);
  assert.equal(reconcileYardSnapshots(local, { positions: [{ id: 1, status: 'LIVRE' }] }).diverged, true);
});

test('ignora conteúdo local inválido ou de versão desconhecida', () => {
  const invalidJson = { getItem: () => '{', setItem: () => undefined };
  const unknownVersion = {
    getItem: () => JSON.stringify({ version: 999, data: { positions: [] } }),
    setItem: () => undefined
  };

  assert.equal(readYardSnapshot(invalidJson), null);
  assert.equal(readYardSnapshot(unknownVersion), null);
});
