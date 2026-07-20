import { useEffect, useMemo, useState } from 'react';
import { Message } from '../../components.jsx';

const GEOMETRY_TYPES = [
  ['PILHA', 'Pilha'],
  ['BLOCO', 'Bloco'],
  ['VIA', 'Via operacional'],
  ['AREA_BLOQUEADA', 'Área bloqueada'],
  ['AREA_INTERDITADA', 'Área interditada'],
  ['EQUIPAMENTO', 'Área/ponto de equipamento']
];

const EMPTY_FORM = Object.freeze({
  codigo: '',
  tipo: 'PILHA',
  bloco: '',
  linha: '',
  coluna: '',
  motivo: ''
});

function geometryNode(geometry) {
  const geoJson = geometry?.geoJson;
  return geoJson?.type === 'Feature' ? geoJson.geometry : geoJson;
}

function geometryVertices(geometry) {
  const coordinates = geometryNode(geometry)?.coordinates?.[0];
  if (!Array.isArray(coordinates)) return [];
  const vertices = coordinates
    .filter((coordinate) => Array.isArray(coordinate) && coordinate.length >= 2)
    .map(([lng, lat]) => ({ lat: Number(lat), lng: Number(lng) }))
    .filter((point) => Number.isFinite(point.lat) && Number.isFinite(point.lng));
  if (vertices.length > 1) {
    const first = vertices[0];
    const last = vertices[vertices.length - 1];
    if (first.lat === last.lat && first.lng === last.lng) vertices.pop();
  }
  return vertices;
}

function buildGeoJson(form, vertices) {
  const coordinates = vertices.map((point) => [point.lng, point.lat]);
  coordinates.push([...coordinates[0]]);
  return {
    type: 'Feature',
    properties: {
      codigo: form.codigo.trim(),
      tipo: form.tipo,
      bloco: form.bloco.trim() || null,
      linha: form.linha === '' ? null : Number(form.linha),
      coluna: form.coluna === '' ? null : Number(form.coluna)
    },
    geometry: {
      type: 'Polygon',
      coordinates: [coordinates]
    }
  };
}

function geometryLabel(geometry) {
  const position = geometry.tipo === 'PILHA'
    ? ` · ${geometry.bloco ?? '—'} L${geometry.linha ?? '—'} C${geometry.coluna ?? '—'}`
    : '';
  return `${geometry.codigo} · ${geometry.tipo}${position}`;
}

export function YardGeometryEditor({
  mapContext,
  geometries = [],
  canEdit,
  onSave,
  onDelete,
  onSelectGeometry
}) {
  const [selectedId, setSelectedId] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);
  const [vertices, setVertices] = useState([]);
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const selectedGeometry = useMemo(
    () => geometries.find((geometry) => String(geometry.id) === String(selectedId)) ?? null,
    [geometries, selectedId]
  );

  useEffect(() => {
    if (selectedId && !selectedGeometry) setSelectedId('');
  }, [selectedId, selectedGeometry]);

  useEffect(() => {
    if (!mapContext || !editing) return undefined;
    const { map } = mapContext;
    const addVertex = (event) => {
      if (!event.latlng) return;
      setVertices((current) => [...current, { lat: event.latlng.lat, lng: event.latlng.lng }]);
    };
    map.on('click', addVertex);
    return () => map.off('click', addVertex);
  }, [mapContext, editing]);

  useEffect(() => {
    if (!mapContext || !editing || !vertices.length) return undefined;
    const { map, leaflet } = mapContext;
    const group = leaflet.layerGroup().addTo(map);
    const latLngs = vertices.map((point) => [point.lat, point.lng]);
    const polygon = leaflet.polygon(latLngs, {
      color: '#0f172a',
      opacity: 1,
      weight: 3,
      fillColor: '#f59e0b',
      fillOpacity: 0.32,
      interactive: false
    }).addTo(group);

    const vertexIcon = leaflet.divIcon({
      className: 'yard-geometry-vertex-icon',
      html: '<span aria-hidden="true"></span>',
      iconSize: [18, 18],
      iconAnchor: [9, 9]
    });

    vertices.forEach((point, index) => {
      const marker = leaflet.marker([point.lat, point.lng], {
        draggable: true,
        icon: vertexIcon,
        keyboard: true,
        title: `Vértice ${index + 1}`
      }).addTo(group);
      marker.on('drag', () => {
        const next = latLngs.map((value, currentIndex) => currentIndex === index
          ? [marker.getLatLng().lat, marker.getLatLng().lng]
          : value);
        polygon.setLatLngs(next);
      });
      marker.on('dragend', () => {
        const position = marker.getLatLng();
        setVertices((current) => current.map((value, currentIndex) => currentIndex === index
          ? { lat: position.lat, lng: position.lng }
          : value));
      });
    });

    return () => group.remove();
  }, [mapContext, editing, vertices]);

  if (!canEdit) return null;

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function resetEditor() {
    setEditing(false);
    setVertices([]);
    setForm(EMPTY_FORM);
    setError('');
  }

  function startNew() {
    setSelectedId('');
    setForm(EMPTY_FORM);
    setVertices([]);
    setEditing(true);
    setError('');
    setSuccess('');
    onSelectGeometry?.(null);
  }

  function startEdit() {
    if (!selectedGeometry) {
      setError('Selecione uma geometria para editar.');
      return;
    }
    setForm({
      codigo: selectedGeometry.codigo ?? '',
      tipo: selectedGeometry.tipo ?? 'PILHA',
      bloco: selectedGeometry.bloco ?? '',
      linha: selectedGeometry.linha ?? '',
      coluna: selectedGeometry.coluna ?? '',
      motivo: ''
    });
    setVertices(geometryVertices(selectedGeometry));
    setEditing(true);
    setError('');
    setSuccess('');
    onSelectGeometry?.(selectedGeometry);
  }

  async function save() {
    setError('');
    setSuccess('');
    if (!form.codigo.trim()) {
      setError('Informe o código da geometria.');
      return;
    }
    if (!form.motivo.trim()) {
      setError('Informe o motivo da alteração.');
      return;
    }
    if (form.tipo === 'PILHA' && (!form.bloco.trim() || form.linha === '' || form.coluna === '')) {
      setError('Uma pilha precisa estar vinculada a bloco, linha e coluna.');
      return;
    }
    if (vertices.length < 3) {
      setError('Clique no mapa para informar pelo menos três vértices.');
      return;
    }

    setBusy(true);
    try {
      const payload = {
        id: selectedGeometry?.id,
        codigo: form.codigo.trim(),
        tipo: form.tipo,
        bloco: form.bloco.trim() || null,
        linha: form.linha === '' ? null : Number(form.linha),
        coluna: form.coluna === '' ? null : Number(form.coluna),
        motivo: form.motivo.trim(),
        geoJson: buildGeoJson(form, vertices)
      };
      const saved = await onSave(payload);
      setSuccess(`Geometria ${saved?.codigo ?? payload.codigo} salva.`);
      setSelectedId(saved?.id ? String(saved.id) : '');
      setEditing(false);
      setVertices([]);
      setForm(EMPTY_FORM);
      onSelectGeometry?.(saved ?? null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!selectedGeometry) {
      setError('Selecione uma geometria para excluir.');
      return;
    }
    if (!form.motivo.trim()) {
      setError('Informe o motivo da exclusão no campo de motivo.');
      return;
    }
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await onDelete(selectedGeometry.id, form.motivo.trim());
      setSuccess(`Geometria ${selectedGeometry.codigo} removida do mapa.`);
      setSelectedId('');
      resetEditor();
      onSelectGeometry?.(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    } finally {
      setBusy(false);
    }
  }

  return <aside className="yard-geometry-editor" aria-label="Editor georreferenciado do pátio">
    <div className="yard-geometry-editor-header">
      <strong>Editor do pátio</strong>
      <span>{editing ? `${vertices.length} vértice(s)` : `${geometries.length} geometria(s)`}</span>
    </div>
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {!editing ? <>
      <label>
        <span>Geometria cadastrada</span>
        <select value={selectedId} onChange={(event) => {
          const value = event.target.value;
          setSelectedId(value);
          const selected = geometries.find((geometry) => String(geometry.id) === value) ?? null;
          onSelectGeometry?.(selected);
        }}>
          <option value="">Selecione</option>
          {geometries.map((geometry) => <option key={geometry.id} value={geometry.id}>{geometryLabel(geometry)}</option>)}
        </select>
      </label>
      <label>
        <span>Motivo para exclusão</span>
        <input value={form.motivo} onChange={(event) => updateField('motivo', event.target.value)} maxLength={500} placeholder="Obrigatório ao excluir" />
      </label>
      <div className="yard-geometry-editor-actions">
        <button type="button" onClick={startNew} disabled={!mapContext || busy}>Novo polígono</button>
        <button type="button" className="secondary" onClick={startEdit} disabled={!selectedGeometry || !mapContext || busy}>Editar</button>
        <button type="button" className="secondary danger" onClick={remove} disabled={!selectedGeometry || busy}>Excluir</button>
      </div>
    </> : <>
      <div className="yard-geometry-editor-grid">
        <label><span>Código</span><input value={form.codigo} onChange={(event) => updateField('codigo', event.target.value)} maxLength={80} /></label>
        <label><span>Tipo</span><select value={form.tipo} onChange={(event) => updateField('tipo', event.target.value)}>{GEOMETRY_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label><span>Bloco</span><input value={form.bloco} onChange={(event) => updateField('bloco', event.target.value)} maxLength={40} /></label>
        <label><span>Linha</span><input type="number" value={form.linha} onChange={(event) => updateField('linha', event.target.value)} /></label>
        <label><span>Coluna</span><input type="number" value={form.coluna} onChange={(event) => updateField('coluna', event.target.value)} /></label>
      </div>
      <label><span>Motivo da alteração</span><input value={form.motivo} onChange={(event) => updateField('motivo', event.target.value)} maxLength={500} /></label>
      <p className="yard-geometry-editor-hint">Clique no mapa para adicionar pontos. Com três ou mais pontos, arraste os marcadores dos vértices para ajustar o desenho.</p>
      <div className="yard-geometry-editor-actions">
        <button type="button" className="secondary" onClick={() => setVertices((current) => current.slice(0, -1))} disabled={!vertices.length || busy}>Desfazer ponto</button>
        <button type="button" onClick={save} disabled={busy || vertices.length < 3}>{busy ? 'Salvando...' : 'Salvar polígono'}</button>
        <button type="button" className="secondary" onClick={resetEditor} disabled={busy}>Cancelar</button>
      </div>
    </>}
  </aside>;
}
