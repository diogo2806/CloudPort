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

const GEOMETRY_TYPE_HELP = {
  PILHA: {
    title: 'Pilha',
    purpose: 'Representa uma posição operacional onde contêineres são armazenados.',
    example: 'Ex.: bloco A, linha 1, coluna 3.',
    fields: 'Exige bloco, linha e coluna para identificar a posição.'
  },
  BLOCO: {
    title: 'Bloco',
    purpose: 'Delimita uma área maior do pátio que agrupa várias pilhas ou posições.',
    example: 'Ex.: Bloco A ou Bloco Refrigerado.',
    fields: 'Use um código único. Bloco, linha e coluna não são necessários.'
  },
  VIA: {
    title: 'Via operacional',
    purpose: 'Marca corredores usados para circulação de caminhões e equipamentos.',
    example: 'Ex.: Via principal norte.',
    fields: 'Desenhe a faixa operacional como um polígono estreito.'
  },
  AREA_BLOQUEADA: {
    title: 'Área bloqueada',
    purpose: 'Indica uma área temporariamente indisponível para uso operacional.',
    example: 'Ex.: manutenção, obra ou obstáculo temporário.',
    fields: 'Informe no motivo por que a área está bloqueada.'
  },
  AREA_INTERDITADA: {
    title: 'Área interditada',
    purpose: 'Indica uma área proibida para operação por decisão de segurança ou autoridade.',
    example: 'Ex.: risco estrutural ou interdição oficial.',
    fields: 'Informe no motivo a causa e a referência da interdição.'
  },
  EQUIPAMENTO: {
    title: 'Área/ponto de equipamento',
    purpose: 'Registra a posição ou a área de atuação de um equipamento do pátio.',
    example: 'Ex.: RTG-01, reach stacker ou balança.',
    fields: 'Use um código que permita reconhecer o equipamento.'
  }
};

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
      bloco: form.tipo === 'PILHA' ? form.bloco.trim() || null : null,
      linha: form.tipo === 'PILHA' && form.linha !== '' ? Number(form.linha) : null,
      coluna: form.tipo === 'PILHA' && form.coluna !== '' ? Number(form.coluna) : null
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
  const [choosingType, setChoosingType] = useState(false);
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const selectedGeometry = useMemo(
    () => geometries.find((geometry) => String(geometry.id) === String(selectedId)) ?? null,
    [geometries, selectedId]
  );
  const typeHelp = GEOMETRY_TYPE_HELP[form.tipo];

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
    setChoosingType(false);
    setEditing(false);
    setVertices([]);
    setForm(EMPTY_FORM);
    setError('');
  }

  function startNew() {
    setSelectedId('');
    setForm(EMPTY_FORM);
    setVertices([]);
    setChoosingType(true);
    setEditing(false);
    setError('');
    setSuccess('');
    onSelectGeometry?.(null);
  }

  function chooseType(tipo) {
    setForm({ ...EMPTY_FORM, tipo });
    setChoosingType(false);
    setEditing(true);
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
    setChoosingType(false);
    setEditing(true);
    setError('');
    setSuccess('');
    onSelectGeometry?.(selectedGeometry);
  }

  async function save() {
    setError('');
    setSuccess('');
    if (!form.codigo.trim()) {
      setError('Informe um código que identifique a geometria.');
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
        bloco: form.tipo === 'PILHA' ? form.bloco.trim() || null : null,
        linha: form.tipo === 'PILHA' && form.linha !== '' ? Number(form.linha) : null,
        coluna: form.tipo === 'PILHA' && form.coluna !== '' ? Number(form.coluna) : null,
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
      <div>
        <strong>Editor do pátio</strong>
        <small>{choosingType ? 'Etapa 1 de 2 · escolha o que será criado' : editing ? 'Etapa 2 de 2 · preencha e desenhe' : 'Selecione ou crie uma geometria'}</small>
      </div>
      <span>{editing ? `${vertices.length} vértice(s)` : `${geometries.length} geometria(s)`}</span>
    </div>
    <Message type="error">{error}</Message>
    <Message type="success">{success}</Message>
    {choosingType ? <>
      <div className="yard-geometry-intro">
        <strong>O que você quer representar no mapa?</strong>
        <p>Escolha o tipo antes de desenhar. O sistema mostrará somente os campos necessários.</p>
      </div>
      <div className="yard-geometry-type-list">
        {GEOMETRY_TYPES.map(([value]) => {
          const help = GEOMETRY_TYPE_HELP[value];
          return <button type="button" key={value} className="yard-geometry-type-option" onClick={() => chooseType(value)}>
            <strong>{help.title}</strong>
            <span>{help.purpose}</span>
            <small>{help.example}</small>
          </button>;
        })}
      </div>
      <div className="yard-geometry-editor-actions">
        <button type="button" className="secondary" onClick={resetEditor}>Cancelar</button>
      </div>
    </> : !editing ? <>
      <div className="yard-geometry-intro">
        <strong>Como usar</strong>
        <p>Crie blocos para dividir o pátio, pilhas para posições de armazenagem, vias para circulação e áreas especiais para restrições ou equipamentos.</p>
      </div>
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
        <button type="button" onClick={startNew} disabled={!mapContext || busy}>Criar geometria</button>
        <button type="button" className="secondary" onClick={startEdit} disabled={!selectedGeometry || !mapContext || busy}>Editar selecionada</button>
        <button type="button" className="secondary danger" onClick={remove} disabled={!selectedGeometry || busy}>Excluir</button>
      </div>
    </> : <>
      <div className="yard-geometry-type-summary">
        <strong>{typeHelp.title}</strong>
        <p>{typeHelp.purpose}</p>
        <small>{typeHelp.fields}</small>
        {!selectedGeometry && <button type="button" className="link-button" onClick={() => { setEditing(false); setChoosingType(true); setVertices([]); }}>Trocar tipo</button>}
      </div>
      <div className="yard-geometry-editor-grid">
        <label><span>Código</span><input value={form.codigo} onChange={(event) => updateField('codigo', event.target.value)} maxLength={80} placeholder={typeHelp.example.replace('Ex.: ', '')} /></label>
        <label><span>Tipo</span><input value={typeHelp.title} disabled /></label>
        {form.tipo === 'PILHA' && <>
          <label><span>Bloco</span><input value={form.bloco} onChange={(event) => updateField('bloco', event.target.value)} maxLength={40} placeholder="Ex.: A" /></label>
          <label><span>Linha</span><input type="number" min="1" value={form.linha} onChange={(event) => updateField('linha', event.target.value)} placeholder="Ex.: 1" /></label>
          <label><span>Coluna</span><input type="number" min="1" value={form.coluna} onChange={(event) => updateField('coluna', event.target.value)} placeholder="Ex.: 3" /></label>
        </>}
      </div>
      <label><span>Motivo da alteração</span><input value={form.motivo} onChange={(event) => updateField('motivo', event.target.value)} maxLength={500} placeholder="Ex.: criação do layout inicial do pátio" /></label>
      <div className="yard-geometry-draw-instructions">
        <strong>Desenho no mapa</strong>
        <ol>
          <li>Clique no mapa para marcar os cantos da área.</li>
          <li>Use pelo menos três pontos.</li>
          <li>Arraste os marcadores para ajustar o contorno.</li>
          <li>Revise os dados e salve.</li>
        </ol>
      </div>
      <div className="yard-geometry-editor-actions">
        <button type="button" className="secondary" onClick={() => setVertices((current) => current.slice(0, -1))} disabled={!vertices.length || busy}>Desfazer último ponto</button>
        <button type="button" onClick={save} disabled={busy || vertices.length < 3}>{busy ? 'Salvando...' : `Salvar ${typeHelp.title.toLowerCase()}`}</button>
        <button type="button" className="secondary" onClick={resetEditor} disabled={busy}>Cancelar</button>
      </div>
    </>}
  </aside>;
}
