import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildStacks,
  orderDestinationKey,
  positionKey
} from './pages/yard/yardModel.js';
import {
  buildYardMapLayout,
  DEFAULT_YARD_MAP_CONFIG,
  normalizeYardMapConfig
} from './pages/yard/yardOpenStreetMap.js';
import {
  buildPersistedYardMapLayout,
  buildResolvedYardMapLayout,
  geometryPath
} from './pages/yard/yardGeoJson.js';

const positions = [
  {
    id: 1,
    bloco: 'A1',
    linha: 1,
    coluna: 1,
    camadaOperacional: 'T1',
    ocupada: true,
    bloqueada: false,
    interditada: false,
    areaPermitida: true,
    codigoConteiner: 'ABCD1234567'
  },
  {
    id: 2,
    bloco: 'A1',
    linha: 1,
    coluna: 1,
    camadaOperacional: 'T2',
    ocupada: false,
    bloqueada: false,
    interditada: false,
    areaPermitida: true
  },
  {
    id: 3,
    bloco: 'A1',
    linha: 1,
    coluna: 2,
    camadaOperacional: 'T1',
    ocupada: false,
    bloqueada: false,
    interditada: false,
    areaPermitida: true
  }
];

const config = normalizeYardMapConfig({
  openStreetMap: {
    center: { lat: -22.93315, lng: -43.83731 },
    slotWidthMeters: 3.2,
    slotLengthMeters: 12.2,
    stackGapMeters: 1,
    blockGapMeters: 20,
    blockColumns: 2,
    rotationDegrees: 17
  }
});

test('associa a ordem não final à posição usando as coordenadas de destino oficiais', () => {
  const order = {
    id: 91,
    linhaDestino: 1,
    colunaDestino: 1,
    camadaDestino: 'T2',
    statusOrdem: 'PENDENTE'
  };

  assert.equal(positionKey(positions[1]), '1:1:T2');
  assert.equal(orderDestinationKey(order), '1:1:T2');

  const blocks = buildStacks(positions, [order]);
  const destination = blocks[0].stacks[0].layers.find((layer) => layer.camadaOperacional === 'T2');
  const occupied = blocks[0].stacks[0].layers.find((layer) => layer.camadaOperacional === 'T1');

  assert.equal(destination.plannedOrder.id, 91);
  assert.equal(destination.ocupada, false);
  assert.equal(occupied.ocupada, true);
  assert.equal(occupied.plannedOrder, null);
});

test('ignora ordens concluídas ou canceladas na sobreposição do mapa', () => {
  const blocks = buildStacks(positions, [{
    id: 92,
    linhaDestino: 1,
    colunaDestino: 2,
    camadaDestino: 'T1',
    statusOrdem: 'CONCLUIDA'
  }]);
  const layer = blocks[0].stacks.find((stack) => stack.coluna === 2).layers[0];
  assert.equal(layer.plannedOrder, null);
});

test('gera um polígono geográfico por pilha e preserva a situação operacional', () => {
  const blocks = buildStacks(positions, [{
    id: 93,
    linhaDestino: 1,
    colunaDestino: 1,
    camadaDestino: 'T2',
    statusOrdem: 'EM_EXECUCAO'
  }]);

  const layout = buildYardMapLayout(blocks, config);

  assert.equal(layout.length, 2);
  assert.equal(layout[0].path.length, 4);
  assert.equal(layout[0].state, 'reserved');
  assert.equal(layout[0].occupiedLayers, 1);
  assert.ok(layout.every((entry) => Number.isFinite(entry.center.lat) && Number.isFinite(entry.center.lng)));
  assert.notDeepEqual(layout[0].center, layout[1].center);
});

test('usa o polígono GeoJSON persistido e mantém a grade automática para pilhas restantes', () => {
  const blocks = buildStacks(positions, [{
    id: 94,
    linhaDestino: 1,
    colunaDestino: 1,
    camadaDestino: 'T2',
    statusOrdem: 'PENDENTE'
  }]);
  const geometry = {
    id: 10,
    codigo: 'PILHA-A1-1-1',
    tipo: 'PILHA',
    bloco: 'A1',
    linha: 1,
    coluna: 1,
    geoJson: {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [[
          [-43.83740, -22.93310],
          [-43.83735, -22.93310],
          [-43.83735, -22.93320],
          [-43.83740, -22.93320],
          [-43.83740, -22.93310]
        ]]
      }
    }
  };

  assert.equal(geometryPath(geometry).length, 4);
  const persisted = buildPersistedYardMapLayout(blocks, [geometry]);
  const resolved = buildResolvedYardMapLayout(blocks, config, [geometry]);

  assert.equal(persisted.length, 1);
  assert.equal(persisted[0].state, 'reserved');
  assert.equal(persisted[0].stack.coluna, 1);
  assert.equal(resolved.length, 2);
  assert.equal(resolved.filter((entry) => entry.geometry).length, 1);
  assert.equal(resolved.filter((entry) => !entry.geometry)[0].stack.coluna, 2);
});

test('normaliza limites geográficos e usa tiles gratuitos sem chave de API', () => {
  const normalizedConfig = normalizeYardMapConfig({
    openStreetMap: {
      tileUrl: 'http://servidor-inseguro/{z}/{x}/{y}.png',
      center: { lat: 999, lng: -999 },
      zoom: 99,
      minZoom: 0,
      maxZoom: 99,
      blockColumns: 0,
      rotationDegrees: 999
    }
  });

  assert.equal(normalizedConfig.tileUrl, DEFAULT_YARD_MAP_CONFIG.tileUrl);
  assert.equal(normalizedConfig.center.lat, 85);
  assert.equal(normalizedConfig.center.lng, -180);
  assert.equal(normalizedConfig.minZoom, 1);
  assert.equal(normalizedConfig.maxZoom, 22);
  assert.equal(normalizedConfig.zoom, 22);
  assert.equal(normalizedConfig.blockColumns, 1);
  assert.equal(normalizedConfig.rotationDegrees, 180);
  assert.equal('apiKey' in normalizedConfig, false);
});
