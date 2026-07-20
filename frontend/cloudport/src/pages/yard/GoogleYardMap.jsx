import { useEffect, useMemo, useRef, useState } from 'react';
import { Loading, Message } from '../../components.jsx';
import {
  loadLeaflet,
  loadYardMapConfig,
  OPEN_STREET_MAP_ATTRIBUTION,
  YARD_MAP_STATE_COLORS,
  YARD_MAP_STATE_LABELS
} from './yardOpenStreetMap.js';
import { buildResolvedYardMapLayout } from './yardGeoJson.js';
import { YardGeometryEditor } from './YardGeometryEditor.jsx';
import { buildEquipmentMapEntries } from './yardLiveMap.js';
import './GoogleYardMap.css';

function sameStack(left, right) {
  return Boolean(left && right)
    && String(left.bloco) === String(right.bloco)
    && String(left.linha) === String(right.linha)
    && String(left.coluna) === String(right.coluna);
}

function routePositionKey(position) {
  return `${position?.linha ?? ''}:${position?.coluna ?? ''}`;
}

function createInfoContent(entry) {
  const root = document.createElement('div');
  root.className = 'yard-google-info';
  const title = document.createElement('strong');
  const state = document.createElement('span');
  const detail = document.createElement('small');

  if (entry.stack) {
    title.textContent = `${entry.stack.bloco} · Linha ${entry.stack.linha} · Coluna ${entry.stack.coluna}`;
    state.textContent = YARD_MAP_STATE_LABELS[entry.state] ?? entry.state;
    detail.textContent = `${entry.occupiedLayers}/${entry.totalLayers} camada(s) ocupada(s)`;
  } else {
    title.textContent = entry.geometry?.codigo ?? 'Geometria do pátio';
    state.textContent = entry.geometry?.tipo ?? 'POLÍGONO';
    detail.textContent = entry.geometry?.bloco
      ? `${entry.geometry.bloco} · L${entry.geometry.linha ?? '—'} C${entry.geometry.coluna ?? '—'}`
      : 'Geometria georreferenciada persistida';
  }

  root.append(title, state, detail);
  return root;
}

function createEquipmentInfoContent(entry) {
  const root = document.createElement('div');
  root.className = 'yard-google-info yard-google-equipment-info';
  const title = document.createElement('strong');
  title.textContent = `${entry.type} · ${entry.id}`;
  const state = document.createElement('span');
  state.textContent = entry.status;
  const position = document.createElement('small');
  position.textContent = entry.nearestStack
    ? `${entry.nearestStack.bloco} · L${entry.nearestStack.linha}/C${entry.nearestStack.coluna}`
    : 'Posição GPS';
  root.append(title, state, position);
  if (entry.operator) {
    const operator = document.createElement('small');
    operator.textContent = `Operador: ${entry.operator}`;
    root.append(operator);
  }
  if (entry.updatedAt) {
    const updatedAt = document.createElement('small');
    const date = new Date(entry.updatedAt);
    updatedAt.textContent = `Atualização: ${Number.isNaN(date.getTime()) ? entry.updatedAt : date.toLocaleString('pt-BR')}`;
    root.append(updatedAt);
  }
  return root;
}

function createStackLabel(leaflet, entry, selected) {
  const element = document.createElement('span');
  element.className = `yard-google-stack-label ${entry.state}${selected ? ' selected' : ''}`;
  element.textContent = entry.stack
    ? `L${entry.stack.linha} C${entry.stack.coluna}`
    : entry.geometry?.codigo ?? 'Área';
  element.title = entry.label;
  return leaflet.divIcon({
    className: 'yard-leaflet-label-icon',
    html: element,
    iconSize: [0, 0],
    iconAnchor: [0, 0]
  });
}

export function OpenStreetMapYardMap({
  blocks,
  selectedStack,
  onSelectStack,
  routes = [],
  equipment = [],
  geometries = [],
  canEditGeometry = false,
  onSaveGeometry,
  onDeleteGeometry
}) {
  const mapElementRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const fittedLayoutRef = useRef('');
  const [config, setConfig] = useState(null);
  const [configError, setConfigError] = useState('');
  const [mapError, setMapError] = useState('');
  const [mapContext, setMapContext] = useState(null);
  const [selectedGeometry, setSelectedGeometry] = useState(null);
  const layout = useMemo(
    () => config ? buildResolvedYardMapLayout(blocks, config, geometries) : [],
    [blocks, config, geometries]
  );
  const equipmentEntries = useMemo(() => buildEquipmentMapEntries(equipment, layout), [equipment, layout]);

  useEffect(() => {
    let active = true;
    loadYardMapConfig()
      .then((loaded) => {
        if (active) setConfig(loaded);
      })
      .catch((reason) => {
        if (active) setConfigError(reason instanceof Error ? reason.message : String(reason));
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!config || !mapElementRef.current) return undefined;
    let active = true;
    setMapError('');
    loadLeaflet()
      .then((leaflet) => {
        if (!active || !mapElementRef.current) return;
        const map = leaflet.map(mapElementRef.current, {
          center: [config.center.lat, config.center.lng],
          zoom: config.zoom,
          minZoom: config.minZoom,
          maxZoom: config.maxZoom,
          zoomControl: true,
          attributionControl: true
        });
        leaflet.tileLayer(config.tileUrl, {
          minZoom: config.minZoom,
          maxZoom: config.maxZoom,
          attribution: OPEN_STREET_MAP_ATTRIBUTION
        }).addTo(map);
        mapInstanceRef.current = map;
        setMapContext({ map, leaflet });
      })
      .catch((reason) => {
        if (active) setMapError(reason instanceof Error ? reason.message : String(reason));
      });

    return () => {
      active = false;
      setMapContext(null);
      mapInstanceRef.current?.remove();
      mapInstanceRef.current = null;
    };
  }, [config]);

  useEffect(() => {
    if (!mapContext) return undefined;
    const { map, leaflet } = mapContext;
    const overlays = leaflet.layerGroup().addTo(map);
    const bounds = leaflet.latLngBounds([]);
    const centers = new Map(layout
      .filter((entry) => entry.stack)
      .map((entry) => [routePositionKey(entry.stack), entry.center]));

    layout.forEach((entry) => {
      const selected = entry.stack
        ? sameStack(entry.stack, selectedStack)
        : Boolean(entry.geometry && String(entry.geometry.id) === String(selectedGeometry?.id));
      const color = entry.color ?? YARD_MAP_STATE_COLORS[entry.state] ?? '#334155';
      const path = entry.path.map((point) => [point.lat, point.lng]);
      const polygon = leaflet.polygon(path, {
        color: selected ? '#0f172a' : color,
        opacity: 1,
        weight: selected ? 4 : 2,
        fillColor: color,
        fillOpacity: selected ? 0.78 : 0.48
      }).addTo(overlays);
      path.forEach((point) => bounds.extend(point));

      const activate = () => {
        if (entry.stack) onSelectStack(entry.stack);
        if (entry.geometry) setSelectedGeometry(entry.geometry);
      };
      polygon.on('click', activate);
      polygon.bindPopup(createInfoContent(entry));

      const label = leaflet.marker([entry.center.lat, entry.center.lng], {
        icon: createStackLabel(leaflet, entry, selected),
        keyboard: true,
        riseOnHover: true
      }).addTo(overlays);
      label.on('click', activate);
      label.bindPopup(createInfoContent(entry));
    });

    routes.forEach((route) => {
      const origin = centers.get(routePositionKey(route.origem));
      const destination = centers.get(routePositionKey(route.destino));
      if (!origin || !destination || origin === destination) return;
      leaflet.polyline([
        [origin.lat, origin.lng],
        [destination.lat, destination.lng]
      ], {
        color: '#0f4c81',
        opacity: 0.9,
        weight: 4,
        dashArray: '10 7'
      }).addTo(overlays);
    });

    equipmentEntries.forEach((entry) => {
      bounds.extend([entry.position.lat, entry.position.lng]);
      const failure = entry.status.includes('FALHA') || entry.status.includes('PARADO');
      const marker = leaflet.circleMarker([entry.position.lat, entry.position.lng], {
        radius: 11,
        color: '#ffffff',
        weight: 3,
        fillColor: failure ? '#dc2626' : '#0f4c81',
        fillOpacity: 0.95
      }).addTo(overlays);
      marker.on('click', () => {
        if (entry.nearestStack) onSelectStack(entry.nearestStack);
      });
      marker.bindPopup(createEquipmentInfoContent(entry));
      marker.bindTooltip(entry.id.slice(-4), { permanent: true, direction: 'center', className: 'yard-equipment-label' });
    });

    const layoutSignature = `${layout.map((entry) => entry.key).join('|')}::${equipmentEntries.map((entry) => entry.id).join('|')}`;
    if (bounds.isValid() && fittedLayoutRef.current !== layoutSignature) {
      fittedLayoutRef.current = layoutSignature;
      map.fitBounds(bounds, { padding: [48, 48], maxZoom: config.zoom });
    }

    return () => overlays.remove();
  }, [mapContext, layout, selectedStack, selectedGeometry, onSelectStack, config, routes, equipmentEntries]);

  if (configError) return <Message type="error">{configError}</Message>;
  if (!config) return <Loading label="Carregando configuração geográfica do pátio..." />;

  return <div className="yard-google-map-shell">
    <Message type="error">{mapError}</Message>
    <div ref={mapElementRef} className="yard-google-map" aria-label="Mapa gratuito OpenStreetMap do pátio de contêineres" />
    {!mapContext && !mapError && <div className="yard-google-map-loading"><Loading label="Carregando OpenStreetMap..." /></div>}
    {!layout.length && mapContext && <div className="yard-google-map-empty">Nenhuma geometria cadastrada. Use “Novo polígono” para desenhar o pátio diretamente no mapa.</div>}
    <YardGeometryEditor
      mapContext={mapContext}
      geometries={geometries}
      canEdit={canEditGeometry}
      onSave={onSaveGeometry}
      onDelete={onDeleteGeometry}
      onSelectGeometry={setSelectedGeometry}
    />
    <div className="yard-google-map-legend" aria-label="Legenda do mapa">
      {Object.entries(YARD_MAP_STATE_LABELS).map(([state, label]) => <span key={state}><i className={state} />{label}</span>)}
      {!!geometries.length && <span><i className="geometry" />Geometria persistida</span>}
      {!!routes.length && <span><i className="route" />Rotas planejadas</span>}
      {!!equipmentEntries.length && <span><i className="equipment" />CHE em tempo real</span>}
      <span className="yard-map-provider">Mapa: OpenStreetMap</span>
    </div>
  </div>;
}
