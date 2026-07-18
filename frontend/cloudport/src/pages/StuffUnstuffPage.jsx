import { useCallback, useEffect, useState } from 'react';
import { formatError } from '../api.js';
import { Message, PageHeader } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';
import { StuffUnstuffPanel } from './StuffUnstuffPanel.jsx';

export function StuffUnstuffPage() {
  const [lotes, setLotes] = useState([]);
  const [error, setError] = useState('');

  const reloadLots = useCallback(async () => {
    setError('');
    try {
      const result = await generalCargoApi.listarLotes();
      setLotes(Array.isArray(result) ? result : []);
    } catch (reason) {
      setError(formatError(reason));
    }
  }, []);

  useEffect(() => { reloadLots(); }, [reloadLots]);

  return <>
    <PageHeader
      eyebrow="Carga geral"
      title="Stuff e unstuff"
      description="Planejamento e execução persistida de estufagem e desova por contêiner, cargo lot, local, equipe, lacres, divergências e avarias."
      actions={<button type="button" className="secondary" onClick={reloadLots}>Atualizar lotes</button>}
    />
    <Message type="error">{error}</Message>
    <StuffUnstuffPanel lotes={lotes} onChanged={reloadLots} />
  </>;
}
