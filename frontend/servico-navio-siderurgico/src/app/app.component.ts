import { Component, OnInit } from '@angular/core';
import {
  BordoEstiva,
  EventoVisitaNavio,
  FaseVisita,
  ItemOperacaoNavio,
  NavioSiderurgico,
  PlanoEstivaNavio,
  PosicaoEstivaNavio,
  ResumoOperacionalNavio,
  SiderurgicoApiService,
  StatusItem,
  TipoCarga,
  TipoMovimento,
  ValidacaoPlanoEstiva,
  VisitaNavio
} from './siderurgico-api.service';

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  navios: NavioSiderurgico[] = [];
  visitas: VisitaNavio[] = [];
  itens: ItemOperacaoNavio[] = [];
  eventos: EventoVisitaNavio[] = [];
  visitaSelecionada?: VisitaNavio;
  plano?: PlanoEstivaNavio;
  validacaoPlano?: ValidacaoPlanoEstiva;
  resumo: ResumoOperacionalNavio = this.resumoVazio();
  carregando = false;
  erro = '';
  sucesso = '';

  fases: FaseVisita[] = ['PREVISTA', 'FUNDEADA', 'ATRACADA', 'OPERANDO', 'OPERACAO_CONCLUIDA', 'PARTIU', 'CANCELADA'];
  tiposMovimento: TipoMovimento[] = ['EMBARQUE', 'DESCARGA', 'RESTOW'];
  tiposCarga: TipoCarga[] = ['BOBINA', 'CHAPA', 'TARUGO', 'PLACA', 'PERFIL', 'VERGALHAO', 'OUTROS'];
  statusItens: StatusItem[] = ['PLANEJADO', 'LIBERADO', 'EM_MOVIMENTO', 'OPERADO', 'BLOQUEADO', 'CANCELADO'];
  bordos: BordoEstiva[] = ['BB', 'BE', 'CENTRO'];

  novoNavio: NavioSiderurgico = this.criarNavioVazio();
  novaVisita: VisitaNavio = this.criarVisitaVazia();
  novoItem: ItemOperacaoNavio = this.criarItemVazio();
  novaPosicao: PosicaoEstivaNavio = this.criarPosicaoVazia();
  motivoBloqueio = '';

  constructor(private readonly api: SiderurgicoApiService) {}

  ngOnInit(): void {
    void this.carregarTudo();
  }

  async carregarTudo(): Promise<void> {
    this.carregando = true;
    try {
      this.limparMensagens();
      await this.api.carregarConfiguracao();
      this.navios = await this.api.listarNavios();
      this.visitas = await this.api.listarVisitas();
      if (this.visitas.length > 0) {
        await this.selecionarVisita(this.visitas[0]);
      }
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel carregar os dados operacionais.');
    } finally {
      this.carregando = false;
    }
  }

  async cadastrarNavio(): Promise<void> {
    try {
      this.limparMensagens();
      const criado = await this.api.criarNavio(this.novoNavio);
      this.navios = [criado, ...this.navios];
      this.novoNavio = this.criarNavioVazio();
      this.sucesso = 'Navio cadastrado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel cadastrar o navio.');
    }
  }

  async cadastrarVisita(): Promise<void> {
    try {
      this.limparMensagens();
      const payload: VisitaNavio = {
        ...this.novaVisita,
        eta: this.normalizarData(this.novaVisita.eta),
        etb: this.normalizarData(this.novaVisita.etb),
        etd: this.normalizarData(this.novaVisita.etd),
        janelaRecebimentoInicio: this.normalizarData(this.novaVisita.janelaRecebimentoInicio),
        janelaRecebimentoFim: this.normalizarData(this.novaVisita.janelaRecebimentoFim),
        cutoffOperacional: this.normalizarData(this.novaVisita.cutoffOperacional)
      };
      const criada = await this.api.criarVisita(payload);
      this.visitas = [criada, ...this.visitas];
      this.novaVisita = this.criarVisitaVazia();
      await this.selecionarVisita(criada);
      this.sucesso = 'Visita de navio criada.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel cadastrar a visita.');
    }
  }

  async selecionarVisita(visita: VisitaNavio): Promise<void> {
    this.visitaSelecionada = visita;
    this.itens = visita.id ? await this.api.listarItensVisita(visita.id) : [];
    this.resumo = visita.id ? await this.api.obterResumo(visita.id) : this.resumoVazio();
    this.eventos = visita.id ? await this.api.listarEventos(visita.id) : [];
    await this.carregarPlanoSelecionado();
    this.novoItem = this.criarItemVazio();
    this.novaPosicao = this.criarPosicaoVazia();
  }

  async cadastrarItem(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) {
      this.erro = 'Selecione uma visita antes de cadastrar itens.';
      return;
    }
    try {
      this.limparMensagens();
      const criado = await this.api.criarItemVisita(visitaId, this.novoItem);
      this.itens = [...this.itens, criado];
      this.novoItem = this.criarItemVazio();
      await this.atualizarResumoEventos();
      this.sucesso = 'Item de carga/descarga cadastrado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel cadastrar o item.');
    }
  }

  async alterarStatusItem(item: ItemOperacaoNavio, status: StatusItem): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId || !item.id) {
      return;
    }
    try {
      this.limparMensagens();
      const atualizado = await this.api.alterarStatusItem(visitaId, item.id, status);
      this.itens = this.itens.map(atual => atual.id === atualizado.id ? atualizado : atual);
      await this.atualizarResumoEventos();
      this.sucesso = 'Status do item atualizado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel alterar o status do item.');
    }
  }

  async alterarBloqueio(item: ItemOperacaoNavio, bloquear: boolean): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId || !item.id) {
      return;
    }
    try {
      this.limparMensagens();
      const atualizado = await this.api.alterarBloqueioItem(visitaId, item.id, bloquear, bloquear ? this.motivoBloqueio : undefined);
      this.itens = this.itens.map(atual => atual.id === atualizado.id ? atualizado : atual);
      this.motivoBloqueio = '';
      await this.atualizarResumoEventos();
      this.sucesso = bloquear ? 'Item bloqueado.' : 'Bloqueio removido.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel alterar o bloqueio do item.');
    }
  }

  async criarPlanoEstiva(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) {
      return;
    }
    try {
      this.limparMensagens();
      this.plano = await this.api.criarPlanoEstiva(visitaId);
      this.validacaoPlano = undefined;
      await this.atualizarResumoEventos();
      this.sucesso = 'Plano de estiva criado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel criar o plano de estiva.');
    }
  }

  async adicionarPosicao(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) {
      return;
    }
    try {
      this.limparMensagens();
      if (!this.plano?.id) {
        this.plano = await this.api.criarPlanoEstiva(visitaId);
      }
      const planoAtual = this.plano;
      if (!planoAtual?.id) {
        throw new Error('Plano de estiva invalido.');
      }
      const posicao: PosicaoEstivaNavio = { ...this.novaPosicao, status: this.novaPosicao.status || 'PLANEJADO' };
      const posicoes = [...(planoAtual.posicoes || []), posicao];
      this.plano = await this.api.salvarPosicoesPlano(visitaId, planoAtual.id, posicoes);
      this.novaPosicao = this.criarPosicaoVazia();
      this.validacaoPlano = undefined;
      this.sucesso = 'Posicao adicionada ao plano.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel salvar a posicao de estiva.');
    }
  }

  async removerPosicao(indice: number): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    const planoAtual = this.plano;
    if (!visitaId || !planoAtual?.id) {
      return;
    }
    try {
      this.limparMensagens();
      const posicoes = (planoAtual.posicoes || []).filter((_, atual) => atual !== indice);
      this.plano = await this.api.salvarPosicoesPlano(visitaId, planoAtual.id, posicoes);
      this.validacaoPlano = undefined;
      this.sucesso = 'Posicao removida do plano.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel remover a posicao.');
    }
  }

  async validarPlano(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    const planoId = this.plano?.id;
    if (!visitaId || !planoId) {
      return;
    }
    try {
      this.limparMensagens();
      this.validacaoPlano = await this.api.validarPlano(visitaId, planoId);
      this.plano = this.validacaoPlano.plano;
      this.sucesso = this.validacaoPlano.erros.length === 0 ? 'Plano validado.' : 'Plano validado com erros bloqueantes.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel validar o plano.');
    }
  }

  async avancarFase(fase: FaseVisita): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) {
      return;
    }
    try {
      this.limparMensagens();
      const atualizada = await this.api.alterarFaseVisita(visitaId, fase);
      this.visitaSelecionada = atualizada;
      this.visitas = this.visitas.map(visita => visita.id === atualizada.id ? atualizada : visita);
      await this.atualizarResumoEventos();
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

  itensPlanejaveis(): ItemOperacaoNavio[] {
    return this.itens.filter(item => item.id && item.status !== 'CANCELADO');
  }

  private async carregarPlanoSelecionado(): Promise<void> {
    this.plano = undefined;
    this.validacaoPlano = undefined;
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) {
      return;
    }
    try {
      this.plano = await this.api.obterPlanoEstiva(visitaId);
    } catch (erro) {
      const status = (erro as { status?: number }).status;
      if (status !== 400 && status !== 404) {
        this.erro = this.extrairErro(erro, 'Nao foi possivel carregar o plano de estiva.');
      }
    }
  }

  private async atualizarResumoEventos(): Promise<void> {
    const visitaId = this.visitaSelecionada?.id;
    if (!visitaId) {
      return;
    }
    this.resumo = await this.api.obterResumo(visitaId);
    this.eventos = await this.api.listarEventos(visitaId);
  }

  private criarNavioVazio(): NavioSiderurgico {
    return {
      nome: '',
      codigoImo: '',
      paisBandeira: 'BRASIL',
      empresaArmadora: '',
      tipoNavio: 'GRANELEIRO',
      quantidadePoroes: 1,
      status: 'PLANEJADO'
    };
  }

  private criarVisitaVazia(): VisitaNavio {
    return {
      navioId: 0,
      codigoVisita: '',
      viagemEntrada: '',
      viagemSaida: '',
      linhaOperadora: '',
      terminalFacility: 'CLOUDPORT',
      bercoPrevisto: '',
      bercoAtual: '',
      eta: '',
      etb: '',
      etd: '',
      janelaRecebimentoInicio: '',
      janelaRecebimentoFim: '',
      cutoffOperacional: '',
      fase: 'PREVISTA',
      observacoes: ''
    };
  }

  private criarItemVazio(): ItemOperacaoNavio {
    return {
      tipoMovimento: 'EMBARQUE',
      codigoLote: '',
      tipoCarga: 'BOBINA',
      produto: '',
      quantidade: 1,
      pesoUnitarioToneladas: null,
      pesoTotalToneladas: 0,
      poraoPlanejado: null,
      poraoReal: null,
      posicaoPlanejada: '',
      posicaoReal: '',
      origemPatio: '',
      destinoPatio: '',
      sequenciaOperacional: null,
      status: 'PLANEJADO',
      motivoBloqueio: '',
      observacoes: ''
    };
  }

  private criarPosicaoVazia(): PosicaoEstivaNavio {
    return {
      itemOperacaoId: 0,
      porao: 1,
      camada: 1,
      coluna: 1,
      bordo: 'CENTRO',
      sequencia: 1,
      pesoToneladas: 0,
      status: 'PLANEJADO'
    };
  }

  private resumoVazio(): ResumoOperacionalNavio {
    return {
      totalItensPlanejados: 0,
      totalItensOperados: 0,
      pesoPlanejado: 0,
      pesoOperado: 0,
      percentualProgresso: 0,
      divergenciasPoraoPosicao: 0,
      itensBloqueados: 0,
      tempoOperacaoMinutos: null
    };
  }

  private normalizarData(valor?: string | null): string | null | undefined {
    return valor ? `${valor}:00` : valor;
  }

  private limparMensagens(): void {
    this.erro = '';
    this.sucesso = '';
  }

  private extrairErro(erro: unknown, fallback: string): string {
    return (erro as { error?: { erro?: string } })?.error?.erro || fallback;
  }
}
