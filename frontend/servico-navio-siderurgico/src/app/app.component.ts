import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  AlertaIntegracaoNavioPatio,
  EventoVisitaNavio,
  FaseVisita,
  FilaPatioDaVisita,
  ItemOperacaoNavio,
  NavioSiderurgico,
  OrdemPatioDaVisita,
  RelatorioOperacionalIntegrado,
  ReservaPatioNavio,
  ResultadoGeracaoOrdensPatio,
  ResultadoReplanejamentoPatioNavio,
  ResumoIntegracaoNavioPatio,
  ResumoOperacionalNavio,
  SiderurgicoApiService,
  VisitaNavio,
  WorkQueuePatioDaVisita
} from './siderurgico-api.service';

interface EdicaoWorkQueuePatio {
  pow: string;
  poolOperacional: string;
  equipamento: string;
  limiteDispatch: number | null;
}

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  navios: NavioSiderurgico[] = [];
  visitas: VisitaNavio[] = [];
  itens: ItemOperacaoNavio[] = [];
  eventos: EventoVisitaNavio[] = [];
  reservasPatio: ReservaPatioNavio[] = [];
  ordensPatio: OrdemPatioDaVisita[] = [];
  filasPatio: FilaPatioDaVisita[] = [];
  workQueuesPatio: WorkQueuePatioDaVisita[] = [];
  ordensSemCobertura: OrdemPatioDaVisita[] = [];
  alertasIntegracao: AlertaIntegracaoNavioPatio[] = [];
  visitaSelecionada?: VisitaNavio;
  resumo: ResumoOperacionalNavio = this.resumoVazio();
  resumoIntegracao: ResumoIntegracaoNavioPatio = this.resumoIntegracaoVazio();
  resultadoOrdens?: ResultadoGeracaoOrdensPatio;
  resultadoReplanejamento?: ResultadoReplanejamentoPatioNavio;
  relatorioIntegrado?: RelatorioOperacionalIntegrado;
  carregando = false;
  erro = '';
  sucesso = '';
  statusOrdemFiltro = '';
  blocoZonaFiltro = '';
  severidadeFiltro = '';
  atualizacaoAutomatica = true;
  ultimaAtualizacaoControlRoom?: Date;
  prioridadesOrdens: Record<number, number> = {};
  workQueuesExpandidas: Record<number, boolean> = {};
  edicoesWorkQueue: Record<number, EdicaoWorkQueuePatio> = {};
  acaoOperacionalEmExecucao = '';
  statusOrdens = ['PENDENTE', 'EM_EXECUCAO', 'BLOQUEADA', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA'];
  severidades = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'];
  private atualizacaoTimer?: ReturnType<typeof setInterval>;
  private atualizacaoEmAndamento?: Promise<void>;

  constructor(private readonly api: SiderurgicoApiService) {}

  ngOnInit(): void {
    void this.carregarTudo();
    this.atualizacaoTimer = setInterval(() => {
      if (this.atualizacaoAutomatica && this.visitaSelecionada?.id) {
        void this.atualizarControlRoomSilencioso().catch(erro => {
          this.erro = this.extrairErro(erro, 'Falha na atualização automática do Control Room.');
        });
      }
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.atualizacaoTimer) {
      clearInterval(this.atualizacaoTimer);
    }
  }

  async carregarTudo(): Promise<void> {
    this.carregando = true;
    try {
      this.limparMensagens();
      await this.api.carregarConfiguracao();
      const [navios, visitas] = await Promise.all([
        this.api.listarNavios(),
        this.api.listarVisitas()
      ]);
      this.navios = navios;
      this.visitas = visitas;
      if (this.visitas.length) {
        await this.selecionarVisita(this.visitas[0]);
      }
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel carregar os dados operacionais.');
    } finally {
      this.carregando = false;
    }
  }

  async selecionarVisita(visita: VisitaNavio): Promise<void> {
    this.visitaSelecionada = visita;
    this.atualizacaoEmAndamento = undefined;
    await this.atualizarControlRoomSilencioso();
  }

  async atualizarControlRoom(): Promise<void> {
    try {
      this.limparMensagens();
      await this.atualizarControlRoomSilencioso();
      this.sucesso = 'Control Room atualizado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel atualizar a visao operacional.');
    }
  }

  alternarAtualizacaoAutomatica(): void {
    this.atualizacaoAutomatica = !this.atualizacaoAutomatica;
    if (this.atualizacaoAutomatica) {
      void this.atualizarControlRoom();
    }
  }

  async gerarReservasPatio(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) return;
    try {
      this.limparMensagens();
      this.reservasPatio = await this.api.gerarReservasPatio(visitaId);
      await this.carregarIntegracaoPatio();
      this.sucesso = 'Reservas de patio geradas.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel gerar as reservas de patio.');
    }
  }

  async gerarOrdensPatio(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) return;
    try {
      this.limparMensagens();
      this.resultadoOrdens = await this.api.gerarOrdensPatio(visitaId);
      await this.carregarIntegracaoPatio();
      this.sucesso = `${this.resultadoOrdens.totalOrdensCriadas} ordem(ns) de patio gerada(s).`;
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel gerar as ordens de patio.');
    }
  }

  async sincronizarPatio(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) return;
    try {
      this.limparMensagens();
      this.resumoIntegracao = await this.api.sincronizarStatusPatio(visitaId);
      await this.atualizarControlRoomSilencioso();
      this.sucesso = 'Status do patio sincronizado com a visita.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel sincronizar o patio.');
    }
  }

  async replanejarPatio(aplicar: boolean): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) return;
    try {
      this.limparMensagens();
      this.resultadoReplanejamento = await this.api.replanejarPatioVisita(visitaId, aplicar);
      await this.atualizarControlRoomSilencioso();
      this.sucesso = aplicar ? 'Replanejamento aplicado.' : 'Replanejamento simulado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel replanejar o patio.');
    }
  }

  async carregarRelatorioIntegrado(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) return;
    try {
      this.limparMensagens();
      this.relatorioIntegrado = await this.api.obterRelatorioOperacionalIntegrado(visitaId);
      this.sucesso = 'Relatorio integrado atualizado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel carregar o relatorio integrado.');
    }
  }

  async avancarFase(fase: FaseVisita): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) return;
    try {
      this.limparMensagens();
      const atualizada = await this.api.alterarFaseVisita(visitaId, fase);
      this.visitaSelecionada = atualizada;
      this.visitas = this.visitas.map(visita => visita.id === atualizada.id ? atualizada : visita);
      await this.atualizarControlRoomSilencioso();
      this.sucesso = 'Fase da visita atualizada.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel avancar a fase.');
    }
  }

  faseSeguinte(fase?: FaseVisita): FaseVisita | undefined {
    switch (fase) {
      case 'PREVISTA': return 'FUNDEADA';
      case 'FUNDEADA': return 'ATRACADA';
      case 'ATRACADA': return 'OPERANDO';
      case 'OPERANDO': return 'OPERACAO_CONCLUIDA';
      case 'OPERACAO_CONCLUIDA': return 'PARTIU';
      default: return undefined;
    }
  }

  nomeItem(itemId: number): string {
    const item = this.itens.find(atual => atual.id === itemId);
    return item ? `${item.codigoLote} - ${item.produto}` : String(itemId);
  }

  filasPatioFiltradas(): FilaPatioDaVisita[] {
    return this.filasPatio.filter(fila => this.correspondeFiltroStatus(fila.status) && this.correspondeFiltroBloco(fila.blocoZona || fila.berco));
  }

  workQueuesPatioFiltradas(): WorkQueuePatioDaVisita[] {
    return this.workQueuesPatio.filter(workQueue => {
      const alvoFiltro = [workQueue.identificador, workQueue.berco, workQueue.blocoZona, workQueue.pow, workQueue.poolOperacional, workQueue.equipamento].filter(Boolean).join(' ');
      return this.correspondeFiltroStatus(workQueue.status) && this.correspondeFiltroBloco(alvoFiltro);
    });
  }

  totalJobListWorkQueuesPatio(): number {
    return this.workQueuesPatio.reduce((total, workQueue) => total + (workQueue.jobList?.length || workQueue.totalOrdens || 0), 0);
  }

  ordensPatioFiltradas(): OrdemPatioDaVisita[] {
    return this.ordensPatio.filter(ordem => this.correspondeFiltroStatus(ordem.statusOrdem) && this.correspondeFiltroBloco(`${ordem.destino || ''} ${ordem.posicaoPlanejada || ''} ${ordem.origem || ''}`));
  }

  alertasIntegracaoFiltrados(): AlertaIntegracaoNavioPatio[] {
    return this.alertasIntegracao.filter(alerta => !this.severidadeFiltro || alerta.severidade === this.severidadeFiltro);
  }

  movimentosIminentes(): OrdemPatioDaVisita[] {
    return this.ordensPatio
      .filter(ordem => ['PENDENTE', 'EM_EXECUCAO'].includes(ordem.statusOrdem))
      .sort((a, b) => (a.sequenciaNavio ?? 999999) - (b.sequenciaNavio ?? 999999))
      .slice(0, 5);
  }

  prioridadeOrdem(ordem: OrdemPatioDaVisita): number {
    if (!ordem.id) {
      return ordem.prioridadeOperacional ?? ordem.sequenciaNavio ?? 0;
    }
    return this.prioridadesOrdens[ordem.id] ?? ordem.prioridadeOperacional ?? ordem.sequenciaNavio ?? 0;
  }

  definirPrioridadeOrdem(ordem: OrdemPatioDaVisita, prioridade: string | number): void {
    if (!ordem.id) return;
    const normalizada = Number(prioridade);
    this.prioridadesOrdens[ordem.id] = Number.isFinite(normalizada) && normalizada >= 0 ? normalizada : 0;
  }

  workQueueExpandida(workQueue: WorkQueuePatioDaVisita): boolean {
    return !!workQueue.id && !!this.workQueuesExpandidas[workQueue.id];
  }

  alternarWorkQueue(workQueue: WorkQueuePatioDaVisita): void {
    if (!workQueue.id) return;
    this.workQueuesExpandidas[workQueue.id] = !this.workQueuesExpandidas[workQueue.id];
    this.edicaoWorkQueue(workQueue);
    this.sincronizarPrioridadesOrdens();
  }

  edicaoWorkQueue(workQueue: WorkQueuePatioDaVisita): EdicaoWorkQueuePatio {
    const chave = workQueue.id || 0;
    if (!this.edicoesWorkQueue[chave]) {
      this.edicoesWorkQueue[chave] = {
        pow: workQueue.pow || '',
        poolOperacional: workQueue.poolOperacional || '',
        equipamento: workQueue.equipamento || '',
        limiteDispatch: null
      };
    }
    return this.edicoesWorkQueue[chave];
  }

  acaoEmExecucao(chave: string): boolean {
    return this.acaoOperacionalEmExecucao === chave;
  }

  async atualizarPrioridadeOrdem(ordem: OrdemPatioDaVisita): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId || !ordem.id) return;
    const prioridade = this.prioridadeOrdem(ordem);
    if (!Number.isFinite(prioridade) || prioridade < 0) {
      this.erro = 'Informe uma prioridade operacional valida.';
      return;
    }
    try {
      this.limparMensagens();
      const atualizada = await this.api.atualizarPrioridadeOrdemPatio(visitaId, ordem.id, prioridade);
      this.substituirOrdemPatio(atualizada);
      await this.carregarIntegracaoPatio();
      this.sucesso = 'Prioridade da ordem atualizada no yard.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel atualizar a prioridade da ordem.');
    }
  }

  async suspenderOrdemPatio(ordem: OrdemPatioDaVisita): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId || !ordem.id) return;
    try {
      this.limparMensagens();
      const atualizada = await this.api.suspenderOrdemPatio(visitaId, ordem.id);
      this.substituirOrdemPatio(atualizada);
      await this.carregarIntegracaoPatio();
      this.sucesso = 'Ordem suspensa no yard.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel suspender a ordem.');
    }
  }

  async retomarOrdemPatio(ordem: OrdemPatioDaVisita): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId || !ordem.id) return;
    try {
      this.limparMensagens();
      const atualizada = await this.api.retomarOrdemPatio(visitaId, ordem.id);
      this.substituirOrdemPatio(atualizada);
      await this.carregarIntegracaoPatio();
      this.sucesso = 'Ordem retomada no yard.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel retomar a ordem.');
    }
  }

  async ativarWorkQueue(workQueue: WorkQueuePatioDaVisita): Promise<void> {
    if (!workQueue.id) return;
    await this.executarAcaoWorkQueue(`ativar-${workQueue.id}`, async () => {
      this.substituirWorkQueuePatio(await this.api.ativarWorkQueuePatio(workQueue.id as number));
      this.sucesso = 'Work queue ativada.';
    }, 'Nao foi possivel ativar a work queue.');
  }

  async desativarWorkQueue(workQueue: WorkQueuePatioDaVisita): Promise<void> {
    if (!workQueue.id) return;
    await this.executarAcaoWorkQueue(`desativar-${workQueue.id}`, async () => {
      this.substituirWorkQueuePatio(await this.api.desativarWorkQueuePatio(workQueue.id as number));
      this.sucesso = 'Work queue desativada.';
    }, 'Nao foi possivel desativar a work queue.');
  }

  async salvarPowWorkQueue(workQueue: WorkQueuePatioDaVisita): Promise<void> {
    if (!workQueue.id) return;
    const edicao = this.edicaoWorkQueue(workQueue);
    await this.executarAcaoWorkQueue(`pow-${workQueue.id}`, async () => {
      this.substituirWorkQueuePatio(await this.api.atualizarPowWorkQueuePatio(workQueue.id as number, { pow: edicao.pow || null, poolOperacional: edicao.poolOperacional || null }));
      this.sucesso = 'POW/pool da work queue atualizado.';
    }, 'Nao foi possivel atualizar POW/pool da work queue.');
  }

  async salvarEquipamentoWorkQueue(workQueue: WorkQueuePatioDaVisita): Promise<void> {
    if (!workQueue.id) return;
    const edicao = this.edicaoWorkQueue(workQueue);
    await this.executarAcaoWorkQueue(`equipamento-${workQueue.id}`, async () => {
      this.substituirWorkQueuePatio(await this.api.atualizarEquipamentoWorkQueuePatio(workQueue.id as number, { equipamento: edicao.equipamento || null }));
      this.sucesso = 'Equipamento da work queue atualizado.';
    }, 'Nao foi possivel atualizar o equipamento da work queue.');
  }

  async despacharWorkQueue(workQueue: WorkQueuePatioDaVisita): Promise<void> {
    if (!workQueue.id) return;
    const edicao = this.edicaoWorkQueue(workQueue);
    await this.executarAcaoWorkQueue(`dispatch-${workQueue.id}`, async () => {
      const resultado = await this.api.despacharWorkQueuePatio(workQueue.id as number, {
        limiteOrdens: edicao.limiteDispatch || null,
        observacao: 'Dispatch acionado pela tela Control Room'
      });
      const total = resultado.totalOrdensDespachadas ?? resultado.ordens?.length ?? 0;
      await this.carregarIntegracaoPatio();
      this.sucesso = `${total} ordem(ns) despachada(s) na work queue.`;
    }, 'Nao foi possivel despachar a work queue.');
  }

  async resetarWorkInstruction(ordem: OrdemPatioDaVisita): Promise<void> {
    if (!ordem.id) return;
    await this.executarAcaoWorkQueue(`reset-${ordem.id}`, async () => {
      this.substituirOrdemPatio(await this.api.resetarWorkInstructionPatio(ordem.id as number));
      await this.carregarIntegracaoPatio();
      this.sucesso = 'Work instruction resetada.';
    }, 'Nao foi possivel resetar a work instruction.');
  }

  async cancelarWorkInstruction(ordem: OrdemPatioDaVisita): Promise<void> {
    if (!ordem.id) return;
    await this.executarAcaoWorkQueue(`cancelar-${ordem.id}`, async () => {
      this.substituirOrdemPatio(await this.api.cancelarWorkInstructionPatio(ordem.id as number));
      await this.carregarIntegracaoPatio();
      this.sucesso = 'Work instruction cancelada.';
    }, 'Nao foi possivel cancelar a work instruction.');
  }

  private atualizarControlRoomSilencioso(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) return Promise.resolve();
    if (this.atualizacaoEmAndamento) {
      return this.atualizacaoEmAndamento;
    }

    const atualizacao = this.carregarSnapshotControlRoom(visitaId)
      .finally(() => {
        if (this.atualizacaoEmAndamento === atualizacao) {
          this.atualizacaoEmAndamento = undefined;
        }
      });
    this.atualizacaoEmAndamento = atualizacao;
    return atualizacao;
  }

  private async carregarSnapshotControlRoom(visitaId: number): Promise<void> {
    const [
      itens,
      resumo,
      eventos,
      resumoIntegracao,
      reservasPatio,
      ordensPatio,
      filasPatio,
      workQueuesPatio,
      ordensSemCobertura,
      alertasIntegracao
    ] = await Promise.all([
      this.api.listarItensVisita(visitaId),
      this.api.obterResumo(visitaId),
      this.api.listarEventos(visitaId),
      this.api.obterResumoIntegracaoPatio(visitaId),
      this.api.listarReservasPatio(visitaId),
      this.api.listarOrdensPatio(visitaId),
      this.api.listarFilasPatio(visitaId),
      this.api.listarWorkQueuesPatio(visitaId),
      this.api.listarOrdensSemCoberturaPatio(visitaId),
      this.api.listarAlertasIntegracaoPatio(visitaId)
    ]);

    if (this.visitaSelecionada?.id !== visitaId) {
      return;
    }
    this.itens = itens;
    this.resumo = resumo;
    this.eventos = eventos;
    this.resumoIntegracao = resumoIntegracao;
    this.reservasPatio = reservasPatio;
    this.ordensPatio = ordensPatio;
    this.filasPatio = filasPatio;
    this.workQueuesPatio = workQueuesPatio;
    this.ordensSemCobertura = ordensSemCobertura;
    this.alertasIntegracao = alertasIntegracao;
    this.sincronizarEdicoesWorkQueue();
    this.sincronizarPrioridadesOrdens();
    this.ultimaAtualizacaoControlRoom = new Date();
  }

  private async carregarIntegracaoPatio(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) {
      this.resumoIntegracao = this.resumoIntegracaoVazio();
      this.reservasPatio = [];
      this.ordensPatio = [];
      this.filasPatio = [];
      this.workQueuesPatio = [];
      this.ordensSemCobertura = [];
      this.alertasIntegracao = [];
      this.prioridadesOrdens = {};
      this.workQueuesExpandidas = {};
      this.edicoesWorkQueue = {};
      return;
    }

    const [resumo, reservas, ordens, filas, workQueues, semCobertura, alertas] = await Promise.all([
      this.api.obterResumoIntegracaoPatio(visitaId),
      this.api.listarReservasPatio(visitaId),
      this.api.listarOrdensPatio(visitaId),
      this.api.listarFilasPatio(visitaId),
      this.api.listarWorkQueuesPatio(visitaId),
      this.api.listarOrdensSemCoberturaPatio(visitaId),
      this.api.listarAlertasIntegracaoPatio(visitaId)
    ]);

    if (this.visitaSelecionada?.id !== visitaId) {
      return;
    }
    this.resumoIntegracao = resumo;
    this.reservasPatio = reservas;
    this.ordensPatio = ordens;
    this.filasPatio = filas;
    this.workQueuesPatio = workQueues;
    this.ordensSemCobertura = semCobertura;
    this.alertasIntegracao = alertas;
    this.sincronizarEdicoesWorkQueue();
    this.sincronizarPrioridadesOrdens();
    this.ultimaAtualizacaoControlRoom = new Date();
  }

  private substituirOrdemPatio(ordemAtualizada: OrdemPatioDaVisita): void {
    this.ordensPatio = this.ordensPatio.map(ordem => ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem);
    this.ordensSemCobertura = this.ordensSemCobertura.map(ordem => ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem);
    this.filasPatio = this.filasPatio.map(fila => ({ ...fila, ordens: fila.ordens.map(ordem => ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem) }));
    this.workQueuesPatio = this.workQueuesPatio.map(workQueue => ({ ...workQueue, jobList: (workQueue.jobList || []).map(ordem => ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem) }));
    this.sincronizarPrioridadesOrdens();
  }

  private substituirWorkQueuePatio(workQueueAtualizada: WorkQueuePatioDaVisita): void {
    this.workQueuesPatio = this.workQueuesPatio.map(workQueue => workQueue.id === workQueueAtualizada.id ? workQueueAtualizada : workQueue);
    this.edicoesWorkQueue[workQueueAtualizada.id || 0] = {
      pow: workQueueAtualizada.pow || '',
      poolOperacional: workQueueAtualizada.poolOperacional || '',
      equipamento: workQueueAtualizada.equipamento || '',
      limiteDispatch: this.edicoesWorkQueue[workQueueAtualizada.id || 0]?.limiteDispatch ?? null
    };
    this.sincronizarPrioridadesOrdens();
  }

  private sincronizarPrioridadesOrdens(): void {
    const ordensDasWorkQueues = this.workQueuesPatio.flatMap(workQueue => workQueue.jobList || []);
    const todasOrdens = [...this.ordensPatio, ...ordensDasWorkQueues];
    this.prioridadesOrdens = todasOrdens.reduce<Record<number, number>>((acc, ordem) => {
      if (ordem.id) {
        acc[ordem.id] = ordem.prioridadeOperacional ?? ordem.sequenciaNavio ?? 0;
      }
      return acc;
    }, { ...this.prioridadesOrdens });
  }

  private sincronizarEdicoesWorkQueue(): void {
    this.workQueuesPatio.forEach(workQueue => {
      if (!workQueue.id || this.edicoesWorkQueue[workQueue.id]) return;
      this.edicoesWorkQueue[workQueue.id] = {
        pow: workQueue.pow || '',
        poolOperacional: workQueue.poolOperacional || '',
        equipamento: workQueue.equipamento || '',
        limiteDispatch: null
      };
    });
  }

  private async executarAcaoWorkQueue(chave: string, acao: () => Promise<void>, mensagemErro: string): Promise<void> {
    try {
      this.limparMensagens();
      this.acaoOperacionalEmExecucao = chave;
      await acao();
    } catch (erro) {
      this.erro = this.extrairErro(erro, mensagemErro);
    } finally {
      this.acaoOperacionalEmExecucao = '';
    }
  }

  private correspondeFiltroStatus(status?: string | null): boolean {
    return !this.statusOrdemFiltro || status === this.statusOrdemFiltro;
  }

  private correspondeFiltroBloco(valor?: string | null): boolean {
    return !this.blocoZonaFiltro || (valor || '').toUpperCase().includes(this.blocoZonaFiltro.trim().toUpperCase());
  }

  private resumoVazio(): ResumoOperacionalNavio {
    return { totalItensPlanejados: 0, totalItensOperados: 0, pesoPlanejado: 0, pesoOperado: 0, percentualProgresso: 0, divergenciasPoraoPosicao: 0, itensBloqueados: 0, tempoOperacaoMinutos: null };
  }

  private resumoIntegracaoVazio(): ResumoIntegracaoNavioPatio {
    return { visitaNavioId: 0, totalItens: 0, itensComReserva: 0, itensComOrdem: 0, itensSemReserva: 0, itensSemOrdem: 0, ordensEmExecucao: 0, ordensConcluidas: 0, totalAlertas: 0, statusPredominante: 'NAO_GERADO' };
  }

  private limparMensagens(): void {
    this.erro = '';
    this.sucesso = '';
  }

  private extrairErro(erro: unknown, fallback: string): string {
    const resposta = erro as {
      error?: {
        codigo?: string;
        erro?: string;
        mensagem?: string;
        message?: string;
        detalhes?: string;
        correlationId?: string;
      };
    };
    const mensagem = resposta?.error?.mensagem
      || resposta?.error?.erro
      || resposta?.error?.message
      || fallback;
    const codigo = resposta?.error?.codigo ? ` [${resposta.error.codigo}]` : '';
    const correlationId = resposta?.error?.correlationId ? ` (correlationId: ${resposta.error.correlationId})` : '';
    const detalhes = resposta?.error?.detalhes ? ` - ${resposta.error.detalhes}` : '';
    return `${mensagem}${codigo}${detalhes}${correlationId}`;
  }
}
