import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import { DataTable, Loading, Message, PageHeader, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';
import { GeneralCargoInventoryPanel } from './GeneralCargoInventoryPanel.jsx';

export function GeneralCargoPhysicalInventoryPage() {
  const [lots, setLots] = useState([]);
  const [selectedLotId, setSelectedLotId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await generalCargoApi.listarLotes();
      setLots(Array.isArray(result) ? result : []);
    } catch (reason) {
      setError(formatError(reason));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { reload(); }, [reload]);

  const selectedLot = useMemo(
    () => lots.find((candidate) => candidate.id === selectedLotId) ?? null,
    [lots, selectedLotId]
  );

  return <>
    <PageHeader
      eyebrow="Carga geral"
      title="Identificação e inventário físico"
      description="Leitura de código de barras ou QR, contagem por posição, divergência motivada e ajuste auditável do cargo lot."
      actions={<button type="button" className="secondary" onClick={reload}>Atualizar lotes</button>}
    />
    <Message type="error">{error}</Message>
    {loading ? <Loading label="Carregando cargo lots..." /> : <>
      <DataTable
        gridId="general-cargo-physical-inventory-lots"
        exportFileName="cargo-lots-inventario-fisico"
        rows={lots}
        rowKey="id"
        columns={[
          { key: 'codigo', label: 'Cargo lot' },
          { key: 'conhecimentoNumero', label: 'B/L' },
          { key: 'descricaoItem', label: 'Item' },
          { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
          { key: 'quantidadeSaldo', label: 'Saldo lógico' },
          { key: 'unidadeMedida', label: 'Unidade' },
          { key: 'armazemId', label: 'Armazém' },
          { key: 'posicaoArmazenagem', label: 'Posição' },
          { key: 'acao', label: 'Ação', exportable: false, render: (row) => <button type="button" className="secondary small" onClick={() => setSelectedLotId(row.id)}>Selecionar</button> }
        ]}
        emptyTitle="Nenhum cargo lot cadastrado"
      />
      <GeneralCargoInventoryPanel selectedLot={selectedLot} onLotSelected={setSelectedLotId} onChanged={reload} />
    </>}
  </>;
}
