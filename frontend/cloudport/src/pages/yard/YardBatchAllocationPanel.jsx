import { useState } from 'react';
import { request } from '../../api.js';
import { DataTable, Message, Section, StatusBadge } from '../../components.jsx';

const EXAMPLE = JSON.stringify([
  { id: 101, codigo: 'CONT001', etaPartida: '2026-07-24T08:00:00', tipoCarga: 'SECO', destino: 'BERCO-1' },
  { id: 102, codigo: 'CONT002', etaPartida: '2026-07-24T09:00:00', tipoCarga: 'REFRIGERADO', destino: 'BERCO-2' }
], null, 2);

export function YardBatchAllocationPanel({ enabled, onCompleted }) {
  const [payload, setPayload] = useState(EXAMPLE);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setResult(null);
    try {
      const batch = JSON.parse(payload);
      if (!Array.isArray(batch) || !batch.length) throw new Error('Informe um array JSON com pelo menos um contêiner.');
      setBusy(true);
      const response = await request('/api/patio/otimizacao/alocar-e-validar', { method: 'POST', body: batch });
      setResult(response);
      onCompleted?.();
    } catch (submissionError) {
      setError(submissionError.message || 'Não foi possível processar o lote.');
    } finally {
      setBusy(false);
    }
  }

  return <Section title="Alocação integrada em lote" description="O backend otimiza, valida e persiste os itens válidos em uma única transação. Rejeições operacionais são devolvidas com o motivo.">
    <details className="json-details">
      <summary>ⓘ Manual</summary>
      <div className="content-card">
        <h3>Finalidade da tela</h3><p>Enviar um lote de contêineres para otimização, validação e persistência coordenadas.</p>
        <h3>Fluxo operacional</h3><ol><li>Revise o JSON do lote.</li><li>Envie para processamento.</li><li>Consulte os alocados e rejeitados.</li><li>Atualize o mapa para conferir as posições persistidas.</li></ol>
        <h3>Explicação dos campos</h3><p>id e codigo identificam o contêiner; etaPartida define prioridade; tipoCarga e destino alimentam as validações operacionais.</p>
        <h3>Permissões necessárias</h3><p>ADMIN_PORTO ou PLANEJADOR.</p>
        <h3>Estados possíveis</h3><p>Pronto, processando, concluído com alocações, concluído com rejeições ou falha transacional.</p>
        <h3>Motivos de bloqueio</h3><p>Perfil sem permissão, JSON inválido, posição indisponível, incompatibilidade de carga ou indisponibilidade do banco.</p>
        <h3>Exemplo</h3><p>Um lote com dois itens pode retornar um ALLOCADO e um REJEITADO com o motivo “Berço incompatível”.</p>
        <h3>Atalhos</h3><p>Ctrl+A no editor seleciona todo o lote para substituição.</p>
        <p><a href="https://github.com/diogo2806/CloudPort/issues/725" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
      </div>
    </details>
    <Message type="error">{error}</Message>
    <form onSubmit={submit}>
      <label htmlFor="yard-batch-json">Lote JSON</label>
      <textarea id="yard-batch-json" rows="12" value={payload} onChange={(event) => setPayload(event.target.value)} disabled={!enabled || busy} />
      <button type="submit" disabled={!enabled || busy}>{busy ? 'Processando...' : 'Otimizar, validar e persistir'}</button>
    </form>
    {result && <>
      <div className="metrics-grid"><strong>Recebidos: {result.totalRecebido}</strong><strong>Alocados: {result.totalAlocado}</strong><strong>Rejeitados: {result.totalRejeitado}</strong></div>
      <DataTable rows={result.resultados ?? []} rowKey={(row, index) => `${row.containerId}-${index}`} columns={[
        { key: 'codigoContainer', label: 'Contêiner' },
        { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'linha', label: 'Linha' }, { key: 'coluna', label: 'Coluna' }, { key: 'nivel', label: 'Nível' }, { key: 'motivo', label: 'Motivo' }
      ]} emptyTitle="Nenhum resultado retornado" />
    </>}
  </Section>;
}
