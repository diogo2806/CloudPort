import { sanitizeText } from '../../api.js';
import { stackClass } from './yardModel.js';

const METERS_PER_DEGREE_LATITUDE = 111_320;
const GOOGLE_MAPS_SCRIPT_ID = 'cloudport-google-maps-script';
const GOOGLE_MAPS_CALLBACK = '__cloudPortGoogleMapsReady';

export const DEFAULT_YARD_MAP_CONFIG = Object.freeze({
  apiKey: '',
  mapId: '',
  center: Object.freeze({ lat: -22.93315, lng: -43.83731 }),
  zoom: 19,
  mapTypeId: 'satellite',
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

let googleMapsPromise = null;
let loadingApiKey = '';

function finiteNumber(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function clamp(value, minimum, maximum, fallback) {
  return Math.min(maximum, Math.max(minimum, finiteNumber(value, fallback)));
}

function normalizeMapType(value) {
  const normalized = sanitizeText(value).toLowerCase();
  return ['roadmap', 'satellite', 'hybrid', 'terrain'].includes(normalized)
    ? normalized
    : DEFAULT_YARD_MAP_CONFIG.mapTypeId;
}

export function normalizeYardMapConfig(payload = {}) {
  const source = payload?.googleMaps ?? payload?.yardMap ?? {};
  const center = source?.center ?? {};
  return {
    apiKey: sanitizeText(source.apiKey ?? payload?.googleMapsApiKey),
    mapId: sanitizeText(source.mapId),
    center: {
      lat: clamp(center.lat, -85, 85, DEFAULT_YARD_MAP_CONFIG.center.lat),
      lng: clamp(center.lng, -180, 180, DEFAULT_YARD_MAP_CONFIG.center.lng)
    },
    zoom: Math.round(clamp(source.zoom, 1, 22, DEFAULT_YARD_MAP_CONFIG.zoom)),
    mapTypeId: normalizeMapType(source.mapTypeId),
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
    throw new Error(`Não foi possível carregar a configuração do Google Maps (status ${response.status}).`);
  }
  return normalizeYardMapConfig(await response.json());
}

export function hasGoogleMapsApiKey(config) {
  const key = sanitizeText(config?.apiKey);
  return Boolean(key) && !/^(YOUR_|SUA_|COLOQUE_|CHANGE_ME)/i.test(key);
}

export function loadGoogleMaps(apiKey) {
  const normalizedKey = sanitizeText(apiKey);
  if (!normalizedKey) return Promise.reject(new Error('A chave da API Google Maps não foi configurada.'));
  if (globalThis.google?.maps?.importLibrary) return Promise.resolve(globalThis.google.maps);
  if (typeof document === 'undefined') return Promise.reject(new Error('O Google Maps somente pode ser carregado no navegador.'));

  if (googleMapsPromise) {
    if (loadingApiKey !== normalizedKey) {
      return Promise.reject(new Error('A API Google Maps já está sendo carregada com outra chave.'));
    }
    return googleMapsPromise;
  }

  loadingApiKey = normalizedKey;
  googleMapsPromise = new Promise((resolve, reject) => {
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      if (globalThis.google?.maps?.importLibrary) {
        resolve(globalThis.google.maps);
      } else {
        googleMapsPromise = null;
        loadingApiKey = '';
        reject(new Error('A API Google Maps foi carregada sem disponibilizar a biblioteca de mapas.'));
      }
    };
    const fail = () => {
      if (settled) return;
      settled = true;
      googleMapsPromise = null;
      loadingApiKey = '';
      reject(new Error('Não foi possível carregar a API Google Maps. Verifique a chave, o faturamento e as restrições por referenciador.'));
    };

    globalThis[GOOGLE_MAPS_CALLBACK] = () => {
      delete globalThis[GOOGLE_MAPS_CALLBACK];
      finish();
    };

    let script = document.getElementById(GOOGLE_MAPS_SCRIPT_ID);
    if (!script) {
      const query = new URLSearchParams({
        key: normalizedKey,
        loading: 'async',
        callback: GOOGLE_MAPS_CALLBACK,
        v: 'quarterly',
        language: 'pt-BR',
        region: 'BR',
        auth_referrer_policy: 'origin'
      });
      script = document.createElement('script');
      script.id = GOOGLE_MAPS_SCRIPT_ID;
      script.async = true;
      script.src = `https://maps.googleapis.com/maps/api/js?${query}`;
      document.head.appendChild(script);
    }
    script.addEventListener('load', finish, { once: true });
    script.addEventListener('error', fail, { once: true });
  });

  return googleMapsPromise;
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
  const config = normalizeYardMapConfig({ googleMaps: inputConfig });
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
