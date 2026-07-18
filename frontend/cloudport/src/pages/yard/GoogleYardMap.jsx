import { useEffect, useMemo, useRef, useState } from 'react';
import { EmptyState, Loading, Message } from '../../components.jsx';
import {
  buildYardMapLayout,
  hasGoogleMapsApiKey,
  loadGoogleMaps,
  loadYardMapConfig,
  YARD_MAP_STATE_COLORS,
  YARD_MAP_STATE_LABELS
} from './yardGoogleMaps.js';
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
  title.textContent = `${entry.stack.bloco} · Linha ${entry.stack.linha} · Coluna ${entry.stack.coluna}`;
  const state = document.createElement('span');
  state.textContent = YARD_MAP_STATE_LABELS[entry.state] ?? entry.state;
  const occupation = document.createElement('small');
  occupation.textContent = `${entry.occupiedLayers}/${entry.totalLayers} camada(s) ocupada(s)`;
  root.append(title, state, occupation);
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

function createLabelOverlay(maps, map, entry, selected, onActivate) {
  let element = null;
  const overlay = new maps.OverlayView();
  overlay.onAdd = () => {
    element = document.createElement('button');
    element.type = 'button';
    element.className = `yard-google-stack-label ${entry.state}${selected ? ' selected' : ''}`;
    element.textContent = `L${entry.stack.linha} C${entry.stack.coluna}`;
    element.title = entry.label;
    element.setAttribute('aria-label', entry.label);
    element.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      onActivate();
    });
    overlay.getPanes().overlayMouseTarget.appendChild(element);
  };
  overlay.draw = () => {
    if (!element) return;
    const point = overlay.getProjection().fromLatLngToDivPixel(entry.center);
    if (!point) return;
    element.style.left = `${point.x}px`;
    element.style.top = `${point.y}px`;
  };
  overlay.onRemove = () => {
    element?.remove();
    element = null;
  };
  overlay.setMap(map);
  return overlay;
}

export function GoogleYardMap({ blocks, selectedStack, onSelectStack, routes = [], equipment = [] }) {
  const mapElementRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const fittedLayoutRef = useRef('');
  const [config, setConfig] = useState(null);
  const [configError, setConfigError] = useState('');
  const [mapError, setMapError] = useState('');
  const [mapContext, setMapContext] = useState(null);
  const layout = useMemo(() => config ? buildYardMapLayout(blocks, config) : [], [blocks, config]);
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
    if (!config || !hasGoogleMapsApiKey(config) || !mapElementRef.current) return undefined;
    let active = true;
    setMapError('');
    loadGoogleMaps(config.apiKey)
      .then(async (maps) => {
        const { Map } = await maps.importLibrary('maps');
        if (!active || !mapElementRef.current) return;
        const options = {
          center: config.center,
          zoom: config.zoom,
          mapTypeId: config.mapTypeId,
          gestureHandling: 'cooperative',
          streetViewControl: false,
          fullscreenControl: true,
          mapTypeControl: true,
          tilt: 0
        };
        if (config.mapId) options.mapId = config.mapId;
        const map = new Map(mapElementRef.current, options);
        mapInstanceRef.current = map;
        setMapContext({ map, maps });
      })
      .catch((reason) => {
        if (active) setMapError(reason instanceof Error ? reason.message : String(reason));
      });

    return () => {
      active = false;
      if (mapInstanceRef.current && globalThis.google?.maps?.event) {
        globalThis.google.maps.event.clearInstanceListeners(mapInstanceRef.current);
      }
      mapInstanceRef.current = null;
    };
  }, [config]);

  useEffect(() => {
    if (!mapContext || !layout.length) return undefined;
    const { map, maps } = mapContext;
    const bounds = new maps.LatLngBounds();
    const infoWindow = new maps.InfoWindow();
    const overlays = [];
    const listeners = [];
    const centers = new Map(layout.map((entry) => [routePositionKey(entry.stack), entry.center]));

    layout.forEach((entry) => {
      const selected = sameStack(entry.stack, selectedStack);
      const color = YARD_MAP_STATE_COLORS[entry.state] ?? '#334155';
      const polygon = new maps.Polygon({
        map,
        paths: entry.path,
        clickable: true,
        strokeColor: selected ? '#0f172a' : color,
        strokeOpacity: 1,
        strokeWeight: selected ? 4 : 2,
        fillColor: color,
        fillOpacity: selected ? 0.78 : 0.55,
        zIndex: selected ? 20 : 10
      });
      entry.path.forEach((point) => bounds.extend(point));

      const activate = () => {
        onSelectStack(entry.stack);
        infoWindow.setContent(createInfoContent(entry));
        infoWindow.setPosition(entry.center);
        infoWindow.open({ map });
      };
      listeners.push(polygon.addListener('click', activate));
      overlays.push(polygon, createLabelOverlay(maps, map, entry, selected, activate));
    });

    routes.forEach((route) => {
      const origin = centers.get(routePositionKey(route.origem));
      const destination = centers.get(routePositionKey(route.destino));
      if (!origin || !destination || origin === destination) return;
      const polyline = new maps.Polyline({
        map,
        path: [origin, destination],
        strokeColor: '#0f4c81',
        strokeOpacity: 0.9,
        strokeWeight: 4,
        zIndex: 40,
        icons: [{
          icon: { path: maps.SymbolPath.FORWARD_CLOSED_ARROW, scale: 3 },
          offset: '100%'
        }]
      });
      overlays.push(polyline);
    });

    equipmentEntries.forEach((entry) => {
      bounds.extend(entry.position);
      const marker = new maps.Marker({
        map,
        position: entry.position,
        title: `${entry.type} ${entry.id} · ${entry.status}`,
        label: { text: entry.id.slice(-4), color: '#ffffff', fontSize: '10px', fontWeight: '700' },
        icon: {
          path: maps.SymbolPath.CIRCLE,
          scale: 13,
          fillColor: entry.status.includes('FALHA') || entry.status.includes('PARADO') ? '#dc2626' : '#0f4c81',
          fillOpacity: 0.95,
          strokeColor: '#ffffff',
          strokeWeight: 3
        },
        animation: maps.Animation?.DROP,
        zIndex: 80
      });
      listeners.push(marker.addListener('click', () => {
        if (entry.nearestStack) onSelectStack(entry.nearestStack);
        infoWindow.setContent(createEquipmentInfoContent(entry));
        infoWindow.setPosition(entry.position);
        infoWindow.open({ map });
      }));
      overlays.push(marker);
    });

    const layoutSignature = `${layout.map((entry) => entry.key).join('|')}::${equipmentEntries.map((entry) => entry.id).join('|')}`;
    if (fittedLayoutRef.current !== layoutSignature) {
      fittedLayoutRef.current = layoutSignature;
      map.fitBounds(bounds, 48);
      listeners.push(maps.event.addListenerOnce(map, 'idle', () => {
        if ((map.getZoom() ?? config.zoom) > config.zoom) map.setZoom(config.zoom);
      }));
    }

    return () => {
      infoWindow.close();
      listeners.forEach((listener) => maps.event.removeListener(listener));
      overlays.forEach((overlay) => overlay.setMap(null));
    };
  }, [mapContext, layout, selectedStack, onSelectStack, config, routes, equipmentEntries]);

  if (configError) return <Message type="error">{configError}</Message>;
  if (!config) return <Loading label="Carregando configuração geográfica do pátio..." />;
  if (!layout.length) return <EmptyState title="Nenhuma pilha disponível para desenhar" description="As posições persistidas do Yard serão convertidas em polígonos quando estiverem disponíveis." />;
  if (!hasGoogleMapsApiKey(config)) {
    return <div className="yard-google-map-not-configured">
      <EmptyState title="Google Maps não configurado" description="Informe googleMaps.apiKey no arquivo assets/configuracao.json do ambiente. A grade operacional abaixo continua disponível sem a chave." />
      <code>googleMaps.apiKey</code>
    </div>;
  }

  return <div className="yard-google-map-shell">
    <Message type="error">{mapError}</Message>
    <div ref={mapElementRef} className="yard-google-map" aria-label="Mapa georreferenciado do pátio de contêineres" />
    {!mapContext && !mapError && <div className="yard-google-map-loading"><Loading label="Carregando Google Maps..." /></div>}
    <div className="yard-google-map-legend" aria-label="Legenda do mapa">
      {Object.entries(YARD_MAP_STATE_LABELS).map(([state, label]) => <span key={state}><i className={state} />{label}</span>)}
      {!!routes.length && <span><i className="route" />Rotas planejadas</span>}
      {!!equipmentEntries.length && <span><i className="equipment" />CHE em tempo real</span>}
    </div>
  </div>;
}
