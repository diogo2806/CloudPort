import assert from 'node:assert/strict';
import test from 'node:test';
import {
  COMPLETE_VESSEL_VIEW_MODES,
  buildVesselScanBays,
  overlayClassName,
  overlayTooltip,
  resolveControlledValue
} from './vessel-planner-complete.js';

test('inclui scan entre top e section sem remover as vistas existentes', () => {
  assert.deepEqual(
    COMPLETE_VESSEL_VIEW_MODES.map((item) => item.value),
    ['MULTI', 'PROFILE', 'TOP', 'SCAN', 'SECTION', 'TIER']
  );
});

test('resume ocupação, restrições, tampas, restow e guindastes por bay', () => {
  const slots = [
    { id: 1, bay: 1, rowBay: 1, tier: 82, codigoContainer: 'AAA0000001', pesoKg: 10000, codigoHatchCover: 'HC01' },
    { id: 2, bay: 1, rowBay: 1, tier: 84, restrito: true, codigoHatchCover: 'HC01' },
    { id: 3, bay: 3, rowBay: 2, tier: 82, codigoContainer: 'BBB0000002', pesoKg: 15000 }
  ];
  const summary = buildVesselScanBays(slots, {
    stackSummaries: { '1:1': { maxWeightKg: 40000 } },
    violationIndex: { '1': [{ descricao: 'Alerta' }], __global__: [] },
    restowIndex: { '1:1:82': { codigoContainer: 'AAA0000001' } },
    craneIndex: { '1:1:82': [{ guindasteId: 1 }, { guindasteId: 2 }] }
  });

  assert.equal(summary.length, 2);
  assert.equal(summary[0].bay, 1);
  assert.equal(summary[0].occupied, 1);
  assert.equal(summary[0].free, 1);
  assert.equal(summary[0].restricted, 1);
  assert.equal(summary[0].warnings, 1);
  assert.equal(summary[0].restows, 1);
  assert.equal(summary[0].craneOperations, 2);
  assert.equal(summary[0].weightKg, 10000);
  assert.deepEqual(summary[0].hatchCovers, ['HC01']);
});

test('converte risco técnico em classe e tooltip', () => {
  const descriptor = { risk: 'HIGH', label: 'Risco combinado', details: ['IMDG: HIGH', 'LASHING: MEDIUM'] };
  assert.equal(overlayClassName(descriptor), 'phase3-overlay overlay-high');
  assert.equal(overlayTooltip(descriptor), 'Risco combinado: IMDG: HIGH · LASHING: MEDIUM');
  assert.equal(overlayClassName({ risk: 'NONE' }), '');
});

test('prioriza valor controlado e mantém fallback local', () => {
  assert.equal(resolveControlledValue('IMDG', 'COMBINED'), 'IMDG');
  assert.equal(resolveControlledValue(undefined, 'COMBINED'), 'COMBINED');
  assert.equal(resolveControlledValue(null, 'PROFILE'), 'PROFILE');
});
