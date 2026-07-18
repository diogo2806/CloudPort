import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import { DataTable, Loading, Message, PageHeader, Section, StatusBadge } from '../components.jsx';
import { generalCargoApi } from '../generalCargoApi.js';
import { GeneralCargoInventoryPanel } from './GeneralCargoInventoryPanel.jsx';

export function GeneralCargoPhysicalInventoryPage() {
  const [lots, setLots] = useState([]);
  const [selectedLotId, setSelectedLotId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showManual, setShowManual] = useState(false);

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
      actions={<>
        <button type="button" className="secondary" aria-label="Abrir manual da tela" title="Manual da tela" onClick={() => setShowManual((value) => !value)}>?</button>
        <button type="button" className="secondary" onClick={reload}>Atualizar lotes</button>
      </>}
    />
    <Message type="error">{error}</Message>
    {showManual && <Section title="Manual da tela" description="Orientações para identificação e reconciliação física de cargo lots.">
      <dl>
        <dt><strong>Finalidade</strong></dt><dd>Identificar lotes e embalagens por código de barras ou QR e reconciliar contagens físicas com o saldo lógico.</dd>
        <dt><strong>Fluxo operacional</strong></dt><dd>Selecione o lote, cadastre ou leia a identificação, abra uma sessão por posição, registre as contagens, trate divergências e conclua a sessão.</dd>
        <dt><strong>Campos</strong></dt><dd>Código e tipo identificam a etiqueta; embalagem detalha a unidade física; posição delimita a sessão; quantidade, volume e peso registram a contagem; motivo fundamenta a decisão.</dd>
        <dt><strong>Permissões</strong></dt><dd>ADMIN_PORTO, PLANEJADOR ou OPERADOR_GATE.</dd>
        <dt><strong>Estados possíveis</strong></dt><dd>Inventário ABERTO, EM_CONTAGEM, AGUARDANDO_APROVACAO ou CONCLUIDO; divergência SEM_DIVERGENCIA, PENDENTE, AJUSTADA ou REJEITADA.</dd>
        <dt><strong>Motivos de bloqueio</strong></dt><dd>Identificação inexistente ou vinculada a outro lote, divergência já resolvida, sessão concluída, motivo ausente ou divergências pendentes no encerramento.</dd>
        <dt><strong>Exemplo</strong></dt><dd>Leia o QR da embalagem, conte 98 unidades onde o saldo lógico indica 100 e aprove o ajuste com o motivo “duas unidades avariadas confirmadas na inspeção”.</dd>
        <dt><strong>Atalhos</strong></dt><dd>O leitor pode preencher o campo e enviar com Enter; use “Selecionar” para alternar rapidamente entre lote e sessão.</dd>
        <dt><strong>Processo completo</strong></dt><dd>Bill of Lading e cargo lots → identificação física → inventário por posição → contagem → decisão da divergência → conclusão.</dd>
      </dl>
    </Section>}
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
