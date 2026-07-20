import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../api.js';
import { DataTable, EmptyState, Loading, Message, MetricCard, Section, StatusBadge } from '../components.jsx';
import { railApi } from '../railApi.js';
import '../rail-operations.css';

function formatarDataHora(valor) {
  if (!valor) return '—';
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return '—';
  return data.toLocaleString('pt-BR');
}

function paraDateTimeLocal(data) {
  const valor = data instanceof Date ? data : new Date(data);
  const deslocamento = valor.getTimezoneOffset() * 60000;
  return new Date(valor.getTime() - deslocamento).toISOString().slice(0, 16);
}

function formularioManobraInicial(sequencia = 1, composicao = '') {
  const inicio = new Date();
  const fim = new Date(inicio.getTime() + 60 * 60 * 1000);
  return {
    sequencia,
    origem: '',
    destino: '',
    composicao,
    linha: 'LINHA 1',
    trecho: '',
    inicioPrevisto: paraDateTimeLocal(inicio),
    fimPrevisto: paraDateTimeLocal(fim)
  };
}

function formularioInspecaoInicial(identificadorVagao = '') {
  return {
    identificadorVagao,
    rodasAprovadas: true,
    freiosAprovados: true,
    engatesAprovados: true,
    estruturaAprovada: true,
    lacresAprovados: true,
    responsavel: '',
    observacao: '',
    codigoDefeito: '',
    descricaoDefeito: '',
    severidadeDefeito: 'BAIXA',
    evidenciaDefeito: ''
  };
}

function ManualFerrovia() {
  return <details className="json-details rail-operations-manual">
    <summary>ⓘ Manual</summary>
    <div className="content-card">
      <h3>Finalidade da tela</h3>
      <p>Planejar manobras ferroviárias com reserva de linhas e liberar vagões para carga ou descarga somente após inspeção física válida.</p>
      <h3>Fluxo operacional</h3>
      <ol>
        <li>Selecione a visita e registre as inspeções de todos os vagões.</li>
        <li>Corrija vagões reprovados ou aplique override autorizado e motivado.</li>
        <li>Cadastre as manobras na sequência operacional, informando origem, destino, linha, trecho e janela.</li>
        <li>Resolva conflitos, autorize a manobra, inicie a execução e conclua cada etapa.</li>
      </ol>
      <h3>Explicação dos campos</h3>
      <ul>
        <li>Sequência: ordem de execução da manobra dentro da visita.</li>
        <li>Linha e trecho: recurso físico reservado durante a janela prevista.</li>
        <li>Composição: locomotiva, vagões ou agrupamento movimentado.</li>
        <li>Checklist: rodas, freios, engates, estrutura e lacres.</li>
        <li>Defeito: código, descrição, severidade e evidência da não conformidade.</li>
      </ul>
      <h3>Permissões necessárias</h3>
      <p>Usuário autenticado no módulo ferroviário. Autorização de manobra e override devem ser executados por responsável operacional habilitado.</p>
      <h3>Estados possíveis</h3>
      <ul>
        <li>Manobra: planejada, bloqueada por conflito, autorizada, em execução, concluída ou cancelada.</li>
        <li>Inspeção: aprovada, reprovada ou liberada por override.</li>
      </ul>
      <h3>Motivos de bloqueio</h3>
      <ul>
        <li>Sobreposição de linha, trecho e horário com outra manobra ativa.</li>
        <li>Vagão sem inspeção, reprovado ou sem associação ao manifesto.</li>
        <li>Transição de estado fora da sequência ou cancelamento sem motivo.</li>
      </ul>
      <h3>Exemplo</h3>
      <p>A manobra 2 reserva a Linha 1, trecho Pátio A–Moega, das 14h às 15h. Uma segunda reserva no mesmo intervalo fica bloqueada até a primeira ser concluída ou cancelada.</p>
      <h3>Atalhos</h3>
      <ul><li>F1 ou Shift + ?: abrir a ajuda contextual.</li><li>Atualizar operação: recarregar manobras e inspeções.</li><li>Enter nos formulários: confirmar o registro.</li></ul>
      <p><a href="https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/ferrovia-manobras-inspecoes.md" target="_blank" rel="noreferrer">Abrir processo completo</a></p>
    </div>
  </details>;
}

function AcoesManobra({ manobra, onAlterar, processando }) {
  const botoes = [];
  if (['PLANEJADA', 'BLOQUEADA_CONFLITO'].includes(manobra.status)) {
    botoes.push(<button type="button" className="small" key="autorizar" disabled={processando} onClick={() => onAlterar(manobra, 'AUTORIZADA')}>Autorizar</button>);
  }
  if (manobra.status === 'AUTORIZADA') {
    botoes.push(<button type="button" className="small" key="iniciar" disabled={processando} onClick={() => onAlterar(manobra, 'EM_EXECUCAO')}>Iniciar</button>);
  }
  if (manobra.status === 'EM_EXECUCAO') {
    botoes.push(<button type="button" className="small" key="concluir" disabled={processando} onClick={() => onAlterar(manobra, 'CONCLUIDA')}>Concluir</button>);
  }
  if (['PLANEJADA', 'BLOQUEADA_CONFLITO', 'AUTORIZADA'].includes(manobra.status)) {
    botoes.push(<button type="button" className="secondary small" key="cancelar" disabled={processando} onClick={() => onAlterar(manobra, 'CANCELADA')}>Cancelar</button>);
  }
  return botoes.length ? <div className="rail-operation-actions">{botoes}</div> : '—';
}

export function RailOperationsPanels({ idVisita, composicao }) {
  const [manobras, setManobras] = useState([]);
  const [inspecoes, setInspecoes] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [processando, setProcessando] = useState(false);
  const [erro, setErro] = useState('');
  const [mensagem, setMensagem] = useState('');
  const composicaoTexto = useMemo(() => {
    const locomotiva = composicao?.locomotiva?.identificador ?? '';
    const quantidade = composicao?.vagoes?.length ?? 0;
    return [locomotiva, quantidade ? `${quantidade} vagão(ões)` : ''].filter(Boolean).join(' · ');
  }, [composicao]);
  const [formManobra, setFormManobra] = useState(() => formularioManobraInicial());
  const [formInspecao, setFormInspecao] = useState(() => formularioInspecaoInicial());

  const carregar = useCallback(async () => {
    if (!idVisita) {
      setManobras([]);
      setInspecoes([]);
      return;
    }
    setCarregando(true);
    setErro('');
    try {
      const [manobrasResposta, inspecoesResposta] = await Promise.all([
        railApi.listarManobras(idVisita),
        railApi.listarInspecoesVagoes(idVisita)
      ]);
      const proximasManobras = Array.isArray(manobrasResposta) ? manobrasResposta : [];
      setManobras(proximasManobras);
      setInspecoes(Array.isArray(inspecoesResposta) ? inspecoesResposta : []);
      setFormManobra((atual) => ({
        ...atual,
        sequencia: proximasManobras.reduce((maior, item) => Math.max(maior, Number(item.sequencia) || 0), 0) + 1,
        composicao: atual.composicao || composicaoTexto
      }));
      setFormInspecao((atual) => ({
        ...atual,
        identificadorVagao: atual.identificadorVagao || composicao?.vagoes?.[0]?.identificadorVagao || ''
      }));
    } catch (motivo) {
      setErro(formatError(motivo, 'Não foi possível carregar manobras e inspeções.'));
    } finally {
      setCarregando(false);
    }
  }, [composicao, composicaoTexto, idVisita]);

  useEffect(() => { carregar(); }, [carregar]);

  async function criarManobra(event) {
    event.preventDefault();
    setProcessando(true);
    setErro('');
    setMensagem('');
    try {
      const criada = await railApi.criarManobra(idVisita, formManobra);
      setMensagem(criada.status === 'BLOQUEADA_CONFLITO'
        ? `Manobra registrada com bloqueio: ${criada.conflitoDescricao}`
        : 'Manobra planejada e trecho reservado.');
      await carregar();
      setFormManobra(formularioManobraInicial(Number(formManobra.sequencia) + 1, composicaoTexto));
    } catch (motivo) {
      setErro(formatError(motivo, 'Não foi possível registrar a manobra.'));
    } finally {
      setProcessando(false);
    }
  }

  async function alterarStatusManobra(manobra, status) {
    let motivo = '';
    if (status === 'CANCELADA') {
      motivo = globalThis.prompt('Informe o motivo do cancelamento:') ?? '';
      if (!motivo.trim()) return;
    }
    setProcessando(true);
    setErro('');
    setMensagem('');
    try {
      const atualizada = await railApi.atualizarStatusManobra(idVisita, manobra.id, { status, motivo });
      setMensagem(atualizada.status === 'BLOQUEADA_CONFLITO'
        ? atualizada.conflitoDescricao
        : `Manobra ${atualizada.sequencia} atualizada para ${atualizada.status}.`);
      await carregar();
    } catch (motivoErro) {
      setErro(formatError(motivoErro, 'Não foi possível atualizar a manobra.'));
    } finally {
      setProcessando(false);
    }
  }

  async function registrarInspecao(event) {
    event.preventDefault();
    const defeitos = formInspecao.codigoDefeito.trim() ? [{
      codigo: formInspecao.codigoDefeito,
      descricao: formInspecao.descricaoDefeito,
      severidade: formInspecao.severidadeDefeito,
      evidencia: formInspecao.evidenciaDefeito
    }] : [];
    setProcessando(true);
    setErro('');
    setMensagem('');
    try {
      const registrada = await railApi.registrarInspecaoVagao(idVisita, { ...formInspecao, defeitos });
      setMensagem(`Inspeção do vagão ${registrada.identificadorVagao}: ${registrada.status}.`);
      await carregar();
      setFormInspecao(formularioInspecaoInicial(formInspecao.identificadorVagao));
    } catch (motivo) {
      setErro(formatError(motivo, 'Não foi possível registrar a inspeção.'));
    } finally {
      setProcessando(false);
    }
  }

  async function liberarOverride(inspecao) {
    const responsavel = globalThis.prompt('Responsável pela liberação excepcional:') ?? '';
    if (!responsavel.trim()) return;
    const motivo = globalThis.prompt('Motivo operacional do override:') ?? '';
    if (!motivo.trim()) return;
    setProcessando(true);
    setErro('');
    try {
      await railApi.liberarVagaoOverride(idVisita, inspecao.id, { responsavel, motivo });
      setMensagem(`Vagão ${inspecao.identificadorVagao} liberado por override.`);
      await carregar();
    } catch (motivoErro) {
      setErro(formatError(motivoErro, 'Não foi possível liberar o vagão.'));
    } finally {
      setProcessando(false);
    }
  }

  if (!idVisita) {
    return <Section title="Operação ferroviária persistida"><EmptyState title="Selecione uma visita" description="O plano de manobras e as inspeções serão habilitados após selecionar uma composição." /></Section>;
  }

  const aprovadas = inspecoes.filter((item) => ['APROVADA', 'LIBERADA_OVERRIDE'].includes(item.status)).length;
  const reprovadas = inspecoes.filter((item) => item.status === 'REPROVADA').length;
  const conflitos = manobras.filter((item) => item.status === 'BLOQUEADA_CONFLITO').length;

  return <>
    <ManualFerrovia />
    <Message type="error">{erro}</Message>
    <Message type="success">{mensagem}</Message>
    <div className="metrics-grid">
      <MetricCard label="Manobras" value={manobras.length} detail={`${conflitos} bloqueada(s) por conflito`} />
      <MetricCard label="Vagões liberados" value={aprovadas} />
      <MetricCard label="Vagões reprovados" value={reprovadas} detail="Fora da lista de trabalho" />
    </div>

    <Section title="Plano de manobra ferroviária" description="A reserva é persistida e impede sobreposição do mesmo trecho e linha no período.">
      <form className="rail-operation-form" onSubmit={criarManobra}>
        <label>Sequência<input type="number" min="1" required value={formManobra.sequencia} onChange={(event) => setFormManobra({ ...formManobra, sequencia: event.target.value })} /></label>
        <label>Origem<input required maxLength="120" value={formManobra.origem} onChange={(event) => setFormManobra({ ...formManobra, origem: event.target.value })} /></label>
        <label>Destino<input required maxLength="120" value={formManobra.destino} onChange={(event) => setFormManobra({ ...formManobra, destino: event.target.value })} /></label>
        <label>Composição<input required maxLength="200" value={formManobra.composicao} onChange={(event) => setFormManobra({ ...formManobra, composicao: event.target.value })} /></label>
        <label>Linha<input required maxLength="80" value={formManobra.linha} onChange={(event) => setFormManobra({ ...formManobra, linha: event.target.value })} /></label>
        <label>Trecho<input required maxLength="120" value={formManobra.trecho} onChange={(event) => setFormManobra({ ...formManobra, trecho: event.target.value })} /></label>
        <label>Início previsto<input type="datetime-local" required value={formManobra.inicioPrevisto} onChange={(event) => setFormManobra({ ...formManobra, inicioPrevisto: event.target.value })} /></label>
        <label>Fim previsto<input type="datetime-local" required value={formManobra.fimPrevisto} onChange={(event) => setFormManobra({ ...formManobra, fimPrevisto: event.target.value })} /></label>
        <button type="submit" disabled={processando}>Reservar trecho</button>
        <button type="button" className="secondary" disabled={carregando} onClick={carregar}>Atualizar operação</button>
      </form>
      {carregando ? <Loading label="Carregando plano operacional..." /> : <DataTable
        rows={manobras}
        rowKey="id"
        emptyTitle="Nenhuma manobra registrada"
        columns={[
          { key: 'sequencia', label: 'Seq.' },
          { key: 'movimento', label: 'Movimento', render: (item) => `${item.origem} → ${item.destino}` },
          { key: 'composicao', label: 'Composição' },
          { key: 'reserva', label: 'Reserva', render: (item) => `${item.linha} · ${item.trecho}` },
          { key: 'periodo', label: 'Período', render: (item) => `${formatarDataHora(item.inicioPrevisto)} até ${formatarDataHora(item.fimPrevisto)}` },
          { key: 'status', label: 'Status', render: (item) => <StatusBadge value={item.status} /> },
          { key: 'conflitoDescricao', label: 'Conflito', render: (item) => item.conflitoDescricao || '—' },
          { key: 'acoes', label: 'Ações', render: (item) => <AcoesManobra manobra={item} onAlterar={alterarStatusManobra} processando={processando} /> }
        ]}
      />}
    </Section>

    <Section title="Inspeção e liberação de vagões" description="Somente a inspeção mais recente aprovada ou liberada por override habilita carga e descarga.">
      <form className="rail-operation-form rail-inspection-form" onSubmit={registrarInspecao}>
        <label>Vagão<select required value={formInspecao.identificadorVagao} onChange={(event) => setFormInspecao({ ...formInspecao, identificadorVagao: event.target.value })}>
          <option value="">Selecione</option>
          {(composicao?.vagoes ?? []).map((vagao) => <option key={vagao.identificadorVagao} value={vagao.identificadorVagao}>{vagao.identificadorVagao}</option>)}
        </select></label>
        <label>Responsável<input required maxLength="120" value={formInspecao.responsavel} onChange={(event) => setFormInspecao({ ...formInspecao, responsavel: event.target.value })} /></label>
        <div className="rail-checklist">
          {[
            ['rodasAprovadas', 'Rodas'],
            ['freiosAprovados', 'Freios'],
            ['engatesAprovados', 'Engates'],
            ['estruturaAprovada', 'Estrutura'],
            ['lacresAprovados', 'Lacres']
          ].map(([campo, rotulo]) => <label key={campo}><input type="checkbox" checked={formInspecao[campo]} onChange={(event) => setFormInspecao({ ...formInspecao, [campo]: event.target.checked })} /> {rotulo} aprovado</label>)}
        </div>
        <label>Observação<textarea maxLength="1000" value={formInspecao.observacao} onChange={(event) => setFormInspecao({ ...formInspecao, observacao: event.target.value })} /></label>
        <label>Código do defeito<input maxLength="40" value={formInspecao.codigoDefeito} onChange={(event) => setFormInspecao({ ...formInspecao, codigoDefeito: event.target.value })} /></label>
        <label>Descrição do defeito<input maxLength="500" value={formInspecao.descricaoDefeito} onChange={(event) => setFormInspecao({ ...formInspecao, descricaoDefeito: event.target.value })} /></label>
        <label>Severidade<select value={formInspecao.severidadeDefeito} onChange={(event) => setFormInspecao({ ...formInspecao, severidadeDefeito: event.target.value })}>
          <option value="BAIXA">Baixa</option><option value="MEDIA">Média</option><option value="ALTA">Alta</option><option value="CRITICA">Crítica</option>
        </select></label>
        <label>Evidência<input maxLength="500" placeholder="URL, arquivo ou referência" value={formInspecao.evidenciaDefeito} onChange={(event) => setFormInspecao({ ...formInspecao, evidenciaDefeito: event.target.value })} /></label>
        <button type="submit" disabled={processando}>Registrar inspeção</button>
      </form>
      <DataTable
        rows={inspecoes}
        rowKey="id"
        emptyTitle="Nenhuma inspeção registrada"
        columns={[
          { key: 'identificadorVagao', label: 'Vagão' },
          { key: 'status', label: 'Status', render: (item) => <StatusBadge value={item.status} /> },
          { key: 'checklist', label: 'Checklist', render: (item) => [item.rodasAprovadas, item.freiosAprovados, item.engatesAprovados, item.estruturaAprovada, item.lacresAprovados].filter(Boolean).length + '/5' },
          { key: 'defeitos', label: 'Defeitos', render: (item) => item.defeitos?.length ?? 0 },
          { key: 'responsavel', label: 'Responsável' },
          { key: 'inspecionadoEm', label: 'Inspecionada em', render: (item) => formatarDataHora(item.inspecionadoEm) },
          { key: 'overrideMotivo', label: 'Liberação', render: (item) => item.overrideMotivo || '—' },
          { key: 'acoes', label: 'Ações', render: (item) => item.status === 'REPROVADA' ? <button type="button" className="secondary small" disabled={processando} onClick={() => liberarOverride(item)}>Liberar override</button> : '—' }
        ]}
      />
    </Section>
  </>;
}
