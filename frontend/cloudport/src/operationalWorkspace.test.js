import assert from 'node:assert/strict';
import test from 'node:test';
import {
  getOperationalSelectionSnapshot,
  markOperationalSimulationReady,
  prepareOperationalSimulation,
  publishOperationalSelection,
  resetOperationalSelectionForTests
} from './operational-selection.js';
import {
  createOperationalViewport,
  drillOperationalViewport,
  operationalViewportBreadcrumbs,
  panOperationalViewport,
  returnOperationalViewport,
  selectViewportItems,
  zoomOperationalViewport
} from './operational-viewport.js';

test('sincroniza a unidade entre pátio, ferrovia e equipamento', () => {
  resetOperationalSelectionForTests();
  publishOperationalSelection({
    domain: 'yard',
    label: 'MSCU1234567',
    unitCode: 'MSCU1234567',
    stackKey: 'A1:2:3',
    location: { bloco: 'A1', linha: 2, coluna: 3 },
    related: [
      { domain: 'rail', label: 'Vagão 18', rail: '18', location: { linha: 'R1', slot: 4 } },
      { domain: 'equipment', label: 'RTG-07', equipment: 'RTG-07', location: { bloco: 'A1' } }
    ]
  });

  const snapshot = getOperationalSelectionSnapshot();
  assert.equal(snapshot.active.domain, 'yard');
  assert.equal(snapshot.contexts.rail.unitCode, 'MSCU1234567');
  assert.equal(snapshot.contexts.equipment.equipment, 'RTG-07');
});

test('bloqueia simulação com unidades divergentes e prepara a válida', () => {
  resetOperationalSelectionForTests();
  publishOperationalSelection({ domain: 'vessel', unitCode: 'MSCU1234567', label: 'B12/R02/T84', location: { bay: 12, row: 2, tier: 84 } });
  publishOperationalSelection({ domain: 'yard', unitCode: 'TCLU7654321', label: 'A1 L2/C3', location: { bloco: 'A1', linha: 2, coluna: 3 } });
  assert.equal(prepareOperationalSimulation('vessel', 'yard').valid, false);

  publishOperationalSelection({ domain: 'yard', unitCode: 'MSCU1234567', label: 'A1 L2/C3', location: { bloco: 'A1', linha: 2, coluna: 3 } });
  const simulation = prepareOperationalSimulation('vessel', 'yard');
  assert.equal(simulation.valid, true);
  assert.equal(markOperationalSimulationReady('Reposicionamento validado pelo planejador').ok, true);
  assert.equal(getOperationalSelectionSnapshot().simulation.status, 'READY_FOR_TRANSACTION');
});

test('controla zoom, pan, drill-down e retorno preservando contexto', () => {
  const initial = createOperationalViewport();
  const zoomed = zoomOperationalViewport(initial, 0.5, { x: 100, y: 60 });
  const panned = panOperationalViewport(zoomed, 20, -10);
  const block = drillOperationalViewport(panned, { bloco: 'A1' });
  const line = drillOperationalViewport(block, { linha: 2 });

  assert.equal(line.level, 'line');
  assert.equal(line.context.bloco, 'A1');
  assert.equal(line.context.linha, 2);
  assert.deepEqual(operationalViewportBreadcrumbs(line).map((item) => item.label), ['Visão geral', 'Bloco A1', 'Linha 2']);
  assert.equal(returnOperationalViewport(line, 'block').level, 'block');
});

test('lista somente elementos interceptados pela seleção retangular', () => {
  const selected = selectViewportItems([
    { value: 'A', bounds: { left: 10, right: 40, top: 10, bottom: 40 } },
    { value: 'B', bounds: { left: 70, right: 100, top: 70, bottom: 100 } }
  ], { x1: 0, y1: 0, x2: 50, y2: 50 });

  assert.deepEqual(selected.map((item) => item.value), ['A']);
});
