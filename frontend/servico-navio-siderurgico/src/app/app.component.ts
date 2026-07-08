import { Component, OnInit } from '@angular/core';
import {
  ItemCargaSiderurgica,
  NavioSiderurgico,
  OperacaoSiderurgica,
  SiderurgicoApiService,
  TipoCarga,
  TipoOperacao
} from './siderurgico-api.service';

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  navios: NavioSiderurgico[] = [];
  operacoes: OperacaoSiderurgica[] = [];
  itens: ItemCargaSiderurgica[] = [];
  operacaoSelecionada?: OperacaoSiderurgica;
  carregando = false;
  erro = '';
  sucesso = '';

  tiposOperacao: TipoOperacao[] = ['EMBARQUE', 'DESCARGA'];
  tiposCarga: TipoCarga[] = ['BOBINA', 'CHAPA', 'TARUGO', 'PLACA', 'PERFIL', 'VERGALHAO', 'OUTROS'];

  novoNavio: NavioSiderurgico = this.criarNavioVazio();
  novaOperacao: OperacaoSiderurgica = this.criarOperacaoVazia();
  novoItem: ItemCargaSiderurgica = this.criarItemVazio();

  constructor(private readonly api: SiderurgicoApiService) {}

  ngOnInit(): void {
    this.carregarTudo();
  }

  async carregarTudo(): Promise<void> {
    this.carregando = true;
    try {
      this.erro = '';
      await this.api.carregarConfiguracao();
      this.navios = await this.api.listarNavios();
      this.operacoes = await this.api.listarOperacoes();
      if (this.operacoes.length > 0) {
        await this.selecionarOperacao(this.operacoes[0]);
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

  async cadastrarOperacao(): Promise<void> {
    try {
      this.limparMensagens();
      const payload = { ...this.novaOperacao, eta: this.normalizarData(this.novaOperacao.eta) };
      const criada = await this.api.criarOperacao(payload);
      this.operacoes = [criada, ...this.operacoes];
      this.novaOperacao = this.criarOperacaoVazia();
      await this.selecionarOperacao(criada);
      this.sucesso = 'Operacao/visita criada.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel cadastrar a operacao.');
    }
  }

  async selecionarOperacao(operacao: OperacaoSiderurgica): Promise<void> {
    this.operacaoSelecionada = operacao;
    this.itens = operacao.id ? await this.api.listarItens(operacao.id) : [];
    this.novoItem = this.criarItemVazio();
  }

  async cadastrarItem(): Promise<void> {
    if (!this.operacaoSelecionada?.id) {
      this.erro = 'Selecione uma operacao antes de cadastrar itens.';
      return;
    }
    try {
      this.limparMensagens();
      const criado = await this.api.criarItem(this.operacaoSelecionada.id, this.novoItem);
      this.itens = [...this.itens, criado];
      this.novoItem = this.criarItemVazio();
      this.sucesso = 'Item de carga cadastrado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel cadastrar o item.');
    }
  }

  progressoOperacao(): number {
    if (this.itens.length === 0) {
      return 0;
    }
    const operados = this.itens.filter(item => item.status === 'OPERADO').length;
    return Math.round((operados / this.itens.length) * 100);
  }

  pesoTotal(): number {
    return this.itens.reduce((total, item) => total + Number(item.pesoTotalToneladas || 0), 0);
  }

  itensBloqueados(): number {
    return this.itens.filter(item => item.status === 'BLOQUEADO').length;
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

  private criarOperacaoVazia(): OperacaoSiderurgica {
    return {
      navioId: 0,
      tipoOperacao: 'EMBARQUE',
      status: 'PLANEJADA',
      berco: '',
      viagem: '',
      eta: '',
      origem: '',
      destino: '',
      observacoes: ''
    };
  }

  private criarItemVazio(): ItemCargaSiderurgica {
    return {
      codigoLote: '',
      tipoCarga: 'BOBINA',
      produto: '',
      quantidade: 1,
      pesoUnitarioToneladas: null,
      pesoTotalToneladas: 0,
      porao: null,
      posicaoBordo: '',
      origemPatio: '',
      destinoPatio: '',
      sequenciaOperacional: null,
      status: 'PLANEJADO'
    };
  }

  private normalizarData(valor?: string): string | undefined {
    return valor ? `${valor}:00` : undefined;
  }

  private limparMensagens(): void {
    this.erro = '';
    this.sucesso = '';
  }

  private extrairErro(erro: unknown, fallback: string): string {
    return (erro as { error?: { erro?: string } })?.error?.erro || fallback;
  }
}
