import { useCallback, useEffect, useState } from 'react';
import { formatError } from '../api.js';
import { Message, PageHeader } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';
import { StuffUnstuffPanel } from './StuffUnstuffPanel.jsx';
import { StuffUnstuffSealPanel } from './StuffUnstuffSealPanel.jsx';
import { StuffUnstuffWeighingPanel } from './StuffUnstuffWeighingPanel.jsx';

const MANUAL_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/stuff-unstuff-pesagem-vgm.md';

export function StuffUnstuffPage() {
  const [lotes, setLotes] = useState([]);
  const [conteineres, setConteineres] = useState([]);
  const [error, setError] = useState('');

  const reloadData = useCallback(async () => {
    setError('');
    try {
      const [lotsResult, containersResult] = await Promise.all([
        generalCargoApi.listarLotes(),
        generalCargoApi.listarConteineresElegiveis()
      ]);
      setLotes(Array.isArray(lotsResult) ? lotsResult : []);
      setConteineres(Array.isArray(containersResult) ? containersResult : []);
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  useEffect(() => { reloadData(); }, [reloadData]);

  return <>
    <PageHeader
      eyebrow="Carga geral"
      title="Stuff e unstuff"
      description="Planejamento e execução persistida de estufagem e desova por contêiner canônico, cargo lot, local, equipe, lacres, divergências, avarias, pesagem e VGM."
      actions={<>
        <a className="secondary" href={MANUAL_URL} target="_blank" rel="noreferrer" aria-label="Abrir manual de stuff, unstuff, pesagem e VGM"><span aria-hidden="true">?</span> Manual</a>
        <button type="button" className="secondary" onClick={reloadData}>Atualizar dados</button>
      </>}
    />
    <Message type="error">{error}</Message>
    <StuffUnstuffPanel lotes={lotes} conteineres={conteineres} onChanged={reloadData} />
    <StuffUnstuffWeighingPanel onChanged={reloadData} />
    <StuffUnstuffSealPanel onChanged={reloadData} />
  </>;
}
