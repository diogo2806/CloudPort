import { sanitizeText } from '../../api.js';
import { stackClass } from './yardModel.js';

const METERS_PER_DEGREE_LATITUDE = 111_320;
const LEAFLET_SCRIPT_ID = 'cloudport-leaflet-script';
const LEAFLET_STYLE_ID = 'cloudport-leaflet-style';
const LEAFLET_VERSION = '1.9.4';
const LEAFLET_SCRIPT_URL = `https://unpkg.com/leaflet@${LEAFLET_VERSION}/dist/leaflet.js`;
const LEAFLET_STYLE_URL = `https://unpkg.com/leaflet@${LEAFLET_VERSION}/dist/leaflet.css`;

export const OPEN_STREET_MAP_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap contributors</a>';

export const DEFAULT_YARD_MAP_CONFIG = Object.freeze({
  tileUrl: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
  center: Object.freeze({ lat: -22.93315, lng: -43.83731 }),
  zoom: 19,
  minZoom: 2,
  maxZoom: 19,
  slotWidthMeters: 3.2,
  slotLengthMeters: 12.2,
  stackGapMeters: 1,
  blockGapMeters: 24,
  blockColumns: 2,
  rotationDegrees: 0
});

export const YARD_MAP_STATE_LABELS = Object.freeze({
  available: 'Disponível',
  occupied: 'Ocupação parcial',
  full: 'Pilha completa',
  reserved: 'Destino planejado',
  blocked: 'Bloqueada',
  interdicted: 'Interditada'
});

export const YARD_MAP_STATE_COLORS = Object.freeze({
  available: '#16a34a',
  occupied: '#0284c7',
  full: '#475569',
  reserved: '#d97706',
  blocked: '#dc2626',
  interdicted: '#7c3aed'
});

let leafletPromise = null;

function finiteNumber(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function clamp(value, minimum, maximum, fallback) {
  return Math.min(maximum, Math.max(minimum, finiteNumber(value, fallback)));
}

function safeTileUrl(value) {
  const normalized = String(value ?? '').trim();
  return normalized.startsWith('https://') ? normalized : DEFAULT_YARD_MAP_CONFIG.tileUrl;
}

export function normalizeYardMapConfig(payload = {}) {
  const source = payload?.openStreetMap ?? payload?.yardMap ?? payload?.googleMaps ?? {};
  const center = source?.center ?? {};
  const minZoom = Math.round(clamp(source.minZoom, 1, 22, DEFAULT_YARD_MAP_CONFIG.minZoom));
  const maxZoom = Math.round(clamp(source.maxZoom, minZoom, 22, DEFAULT_YARD_MAP_CONFIG.maxZoom));
  return {
    tileUrl: safeTileUrl(source.tileUrl),
    center: {
      lat: clamp(center.lat, -85, 85, DEFAULT_YARD_MAP_CONFIG.center.lat),
      lng: clamp(center.lng, -180, 180, DEFAULT_YARD_MAP_CONFIG.center.lng)
    },
    zoom: Math.round(clamp(source.zoom, minZoom, maxZoom, DEFAULT_YARD_MAP_CONFIG.zoom)),
    minZoom,
    maxZoom,
    slotWidthMeters: clamp(source.slotWidthMeters, 1, 30, DEFAULT_YARD_MAP_CONFIG.slotWidthMeters),
    slotLengthMeters: clamp(source.slotLengthMeters, 2, 60, DEFAULT_YARD_MAP_CONFIG.slotLengthMeters),
    stackGapMeters: clamp(source.stackGapMeters, 0, 20, DEFAULT_YARD_MAP_CONFIG.stackGapMeters),
    blockGapMeters: clamp(source.blockGapMeters, 0, 250, DEFAULT_YARD_MAP_CONFIG.blockGapMeters),
    blockColumns: Math.round(clamp(source.blockColumns, 1, 8, DEFAULT_YARD_MAP_CONFIG.blockColumns)),
    rotationDegrees: clamp(source.rotationDegrees, -180, 180, DEFAULT_YARD_MAP_CONFIG.rotationDegrees)
  };
}

export async function loadYardMapConfig() {
  const response = await fetch('/assets/configuracao.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Não foi possível carregar a configuração do mapa do pátio (status ${response.status}).`);
  }
  return normalizeYardMapConfig(await response.json());
}

export function loadLeaflet() {
  if (globalThis.L?.map) return Promise.resolve(globalThis.L);
  if (typeof document === 'undefined') return Promise.reject(new Error('O mapa somente pode ser carregado no navegador.'));
  if (leafletPromise) return leafletPromise;

  leafletPromise = new Promise((resolve, reject) => {
    if (!document.getElementById(LEAFLET_STYLE_ID)) {
      const style = document.createElement('link');
      style.id = LEAFLET_STYLE_ID;
      style.rel = 'stylesheet';
      style.href = LEAFLET_STYLE_URL;
      style.crossOrigin = '';
      document.head.appendChild(style);
    }

    let script = document.getElementById(LEAFLET_SCRIPT_ID);
    const finish = () => {
      if (globalThis.L?.map) resolve(globalThis.L);
      else {
        leafletPromise = null;
        reject(new Error('A biblioteca do mapa foi carregada sem disponibilizar o Leaflet.'));
      }
    };
    const fail = () => {
      leafletPromise = null;
      reject(new Error('Não foi possível carregar a biblioteca gratuita do mapa.'));
    };

    if (!script) {
      script = document.createElement('script');
      script.id = LEAFLET_SCRIPT_ID;
      script.src = LEAFLET_SCRIPT_URL;
      script.async = true;
      script.crossOrigin = '';
      script.addEventListener('load', finish, { once: true });
      script.addEventListener('error', fail, { once: true });
      document.head.appendChild(script);
      return;
    }

    script.addEventListener('load', finish, { once: true });
    script.addEventListener('error', fail, { once: true });
    if (globalThis.L?.map) finish();
  });

  return leafletPromise;
}

function compareCoordinate(left, right) {
  const leftNumber = Number(left);
  const rightNumber = Number(right);
  if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) return leftNumber - rightNumber;
  return String(left).localeCompare(String(right), 'pt-BR', { numeric: true });
}

function uniqueCoordinates(stacks, property) {
  return Array.from(new Set(stacks.map((stack) => String(stack?.[property] ?? ''))))
    .sort(compareCoordinate);
}

function rotatePoint(point, radians) {
  const cosine = Math.cos(radians);
  const sine = Math.sin(radians);
  return {
    east: point.east * cosine - point.north * sine,
    north: point.east * sine + point.north * cosine
  };
}

function metersToLatLng(point, center) {
  const longitudeScale = METERS_PER_DEGREE_LATITUDE * Math.max(0.01, Math.cos(center.lat * Math.PI / 180));
  return {
    lat: center.lat + point.north / METERS_PER_DEGREE_LATITUDE,
    lng: center.lng + point.east / longitudeScale
  };
}

function stackLabel(stack, occupied, total) {
  return `${sanitizeText(stack.bloco)} · L${stack.linha} C${stack.coluna} · ${occupied}/${total}`;
}

export function buildYardMapLayout(blocks, inputConfig = DEFAULT_YARD_MAP_CONFIG) {
  const config = normalizeYardMapConfig({ openStreetMap: inputConfig });
  const metrics = (blocks ?? [])
    .filter((block) => Array.isArray(block?.stacks) && block.stacks.length)
    .map((block) => {
      const lines = uniqueCoordinates(block.stacks, 'linha');
      const columns = uniqueCoordinates(block.stacks, 'coluna');
      return {
        block,
        lines,
        columns,
        width: columns.length * config.slotWidthMeters + Math.max(0, columns.length - 1) * config.stackGapMeters,
        height: lines.length * config.slotLengthMeters + Math.max(0, lines.length - 1) * config.stackGapMeters
      };
    });

  if (!metrics.length) return [];

  const gridColumns = Math.min(config.blockColumns, metrics.length);
  const gridRows = Math.ceil(metrics.length / gridColumns);
  const maximumWidth = Math.max(...metrics.map((metric) => metric.width));
  const maximumHeight = Math.max(...metrics.map((metric) => metric.height));
  const totalWidth = gridColumns * maximumWidth + Math.max(0, gridColumns - 1) * config.blockGapMeters;
  const totalHeight = gridRows * maximumHeight + Math.max(0, gridRows - 1) * config.blockGapMeters;
  const rotation = config.rotationDegrees * Math.PI / 180;
  const layout = [];

  metrics.forEach((metric, blockIndex) => {
    const gridColumn = blockIndex % gridColumns;
    const gridRow = Math.floor(blockIndex / gridColumns);
    const blockLeft = -totalWidth / 2
      + gridColumn * (maximumWidth + config.blockGapMeters)
      + (maximumWidth - metric.width) / 2;
    const blockTop = totalHeight / 2
      - gridRow * (maximumHeight + config.blockGapMeters)
      - (maximumHeight - metric.height) / 2;

    metric.block.stacks.forEach((stack) => {
      const lineIndex = metric.lines.indexOf(String(stack.linha ?? ''));
      const columnIndex = metric.columns.indexOf(String(stack.coluna ?? ''));
      const east = blockLeft + columnIndex * (config.slotWidthMeters + config.stackGapMeters) + config.slotWidthMeters / 2;
      const north = blockTop - lineIndex * (config.slotLengthMeters + config.stackGapMeters) - config.slotLengthMeters / 2;
      const halfWidth = config.slotWidthMeters / 2;
      const halfLength = config.slotLengthMeters / 2;
      const localCorners = [
        { east: east - halfWidth, north: north + halfLength },
        { east: east + halfWidth, north: north + halfLength },
        { east: east + halfWidth, north: north - halfLength },
        { east: east - halfWidth, north: north - halfLength }
      ];
      const center = metersToLatLng(rotatePoint({ east, north }, rotation), config.center);
      const occupiedLayers = (stack.layers ?? []).filter((layer) => layer.ocupada).length;
      const totalLayers = stack.layers?.length ?? 0;

      layout.push({
        key: `${sanitizeText(stack.bloco)}:${stack.linha}:${stack.coluna}`,
        stack,
        state: stackClass(stack),
        center,
        path: localCorners.map((corner) => metersToLatLng(rotatePoint(corner, rotation), config.center)),
        occupiedLayers,
        totalLayers,
        label: stackLabel(stack, occupiedLayers, totalLayers)
      });
    });
  });

  return layout;
}
