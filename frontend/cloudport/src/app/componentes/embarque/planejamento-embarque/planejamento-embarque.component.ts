import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AtribuicaoEstiva,
  EscalaResumo,
  NovaAtribuicaoEstiva,
  NovoPlanoEstiva,
  PlanoEstivaDetalhe,
  ServicoEstivaService,
  TipoCargaConteiner,
  TipoOperacaoEstiva
} from '../../service/servico-estiva/servico-estiva.service';

interface OpcaoTipoCarga {
  valor: TipoCargaConteiner;
  rotulo: string;
}

@Component({
  selector: 'app-planejamento-embarque',
  templateUrl: './planejamento-embarque.component.html',
  styleUrls: ['./planejamento-embarque.component.css'],
  standalone: false
})
export class PlanejamentoEmbarqueComponent implements OnInit {

  readonly tiposCarga: OpcaoTipoCarga[] = [
    { valor: 'SECO', rotulo: 'Seco' },
    { valor: 'REFRIGERADO', rotulo: 'Refrigerado (reefer)' },
    { valor: 'PERIGOSO', rotulo: 'Perigoso (IMO)' },
    { valor: 'GRANELEIRO', rotulo: 'Granel' },
    { valor: 'OUTRO', rotulo: 'Outro' }
  ];

  escalas: EscalaResumo[] = [];
  escalaSelecionadaId: number | null = null;
  plano: PlanoEstivaDetalhe | null = null;

  modoOperacao: TipoOperacaoEstiva = 'EMBARQUE';

  carregando = false;
  salvando = false;
  erro: string | null = null;
  aviso: string | null = null;

  baiaSelecionada: number | null = null;

  novoPlano: NovoPlanoEstiva = { baias: 10, fileiras: 6, camadas: 4 };

  novaAtribuicao: NovaAtribuicaoEstiva = this.criarAtribuicaoVazia();

  private celulasOcupadas = new Map<string, AtribuicaoEstiva>();

  constructor(private readonly servicoEstiva: ServicoEstivaService) {}

  ngOnInit(): void {
    this.carregarEscalas();
  }

  carregarEscalas(): void {
    this.carregando = true;
    this.erro = null;
    this.servicoEstiva.listarEscalas(30).subscribe({
      next: (escalas) => {
        this.escalas = escalas;
        this.carregando = false;
      },
      error: (erro) => {
        this.carregando = false;
        this.erro = this.extrairMensagem(erro, 'Não foi possível carregar as escalas.');
      }
    });
  }

  aoSelecionarEscala(): void {
    this.plano = null;
    this.baiaSelecionada = null;
    this.aviso = null;
    this.erro = null;
    if (this.escalaSelecionadaId == null) {
      return;
    }
    this.carregarPlano(this.escalaSelecionadaId);
  }

  carregarPlano(escalaId: number): void {
    this.carregando = true;
    this.servicoEstiva.obterPlano(escalaId).subscribe({
      next: (plano) => {
        this.aplicarPlano(plano);
        this.carregando = false;
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando = false;
        if (erro.status === 404) {
          this.plano = null;
          this.aviso = 'Esta escala ainda não possui um plano de estiva. Defina as dimensões do navio para iniciar.';
        } else {
          this.erro = this.extrairMensagem(erro, 'Não foi possível carregar o plano de estiva.');
        }
      }
    });
  }

  criarPlano(): void {
    if (this.escalaSelecionadaId == null) {
      return;
    }
    this.salvando = true;
    this.erro = null;
    this.servicoEstiva.criarPlano(this.escalaSelecionadaId, this.novoPlano).subscribe({
      next: (plano) => {
        this.aplicarPlano(plano);
        this.aviso = null;
        this.salvando = false;
      },
      error: (erro) => {
        this.salvando = false;
        this.erro = this.extrairMensagem(erro, 'Não foi possível criar o plano de estiva.');
      }
    });
  }

  alternarModo(modo: TipoOperacaoEstiva): void {
    if (this.modoOperacao === modo) {
      return;
    }
    this.modoOperacao = modo;
    this.novaAtribuicao = { ...this.criarAtribuicaoVazia(), baia: this.baiaSelecionada ?? 1 };
  }

  selecionarBaia(baia: number): void {
    this.baiaSelecionada = baia;
    this.novaAtribuicao.baia = baia;
  }

  selecionarCelula(fileira: number, camada: number): void {
    if (this.baiaSelecionada == null) {
      return;
    }
    if (this.atribuicaoEm(this.baiaSelecionada, fileira, camada)) {
      return;
    }
    this.novaAtribuicao.baia = this.baiaSelecionada;
    this.novaAtribuicao.fileira = fileira;
    this.novaAtribuicao.camada = camada;
  }

  adicionarAtribuicao(): void {
    if (this.escalaSelecionadaId == null) {
      return;
    }
    this.salvando = true;
    this.erro = null;
    const payload: NovaAtribuicaoEstiva = {
      ...this.novaAtribuicao,
      tipoOperacao: this.modoOperacao,
      pesoToneladas: this.novaAtribuicao.pesoToneladas || null,
      posicaoPatioOrigem: this.novaAtribuicao.posicaoPatioOrigem || null,
      posicaoPatioDestino: this.novaAtribuicao.posicaoPatioDestino || null,
      sequenciaEmbarque: this.novaAtribuicao.sequenciaEmbarque || null
    };
    this.servicoEstiva.adicionarAtribuicao(this.escalaSelecionadaId, payload).subscribe({
      next: (plano) => {
        this.aplicarPlano(plano);
        this.reiniciarFormularioAtribuicao();
        this.salvando = false;
      },
      error: (erro) => {
        this.salvando = false;
        this.erro = this.extrairMensagem(erro, 'Não foi possível atribuir o contêiner.');
      }
    });
  }

  operar(atribuicao: AtribuicaoEstiva): void {
    this.salvando = true;
    this.erro = null;
    this.servicoEstiva.operar(atribuicao.id).subscribe({
      next: (plano) => {
        this.aplicarPlano(plano);
        this.salvando = false;
      },
      error: (erro) => {
        this.salvando = false;
        this.erro = this.extrairMensagem(erro, 'Não foi possível registrar a operação.');
      }
    });
  }

  remover(atribuicao: AtribuicaoEstiva): void {
    this.salvando = true;
    this.erro = null;
    this.servicoEstiva.removerAtribuicao(atribuicao.id).subscribe({
      next: (plano) => {
        this.aplicarPlano(plano);
        this.salvando = false;
      },
      error: (erro) => {
        this.salvando = false;
        this.erro = this.extrairMensagem(erro, 'Não foi possível remover a atribuição.');
      }
    });
  }

  atribuicaoEm(baia: number, fileira: number, camada: number): AtribuicaoEstiva | undefined {
    return this.celulasOcupadas.get(this.chaveCelula(this.modoOperacao, baia, fileira, camada));
  }

  contarNaBaia(baia: number): number {
    if (!this.plano) {
      return 0;
    }
    return this.plano.atribuicoes
      .filter((a) => a.tipoOperacao === this.modoOperacao && a.baia === baia).length;
  }

  atribuicoesDaBaia(baia: number): AtribuicaoEstiva[] {
    if (!this.plano) {
      return [];
    }
    return this.plano.atribuicoes
      .filter((a) => a.tipoOperacao === this.modoOperacao && a.baia === baia)
      .sort((a, b) => (a.sequenciaEmbarque ?? 0) - (b.sequenciaEmbarque ?? 0));
  }

  get baiasArray(): number[] {
    return this.intervalo(this.plano?.baias ?? 0);
  }

  get fileirasArray(): number[] {
    return this.intervalo(this.plano?.fileiras ?? 0);
  }

  get camadasArrayDescendente(): number[] {
    return this.intervalo(this.plano?.camadas ?? 0).reverse();
  }

  get ehDescarga(): boolean {
    return this.modoOperacao === 'DESCARGA';
  }

  get rotuloAcao(): string {
    return this.ehDescarga ? 'Descarregar' : 'Embarcar';
  }

  get totalPlanejado(): number {
    if (!this.plano) {
      return 0;
    }
    return this.ehDescarga ? this.plano.descargaPlanejada : this.plano.embarquePlanejado;
  }

  get totalExecutado(): number {
    if (!this.plano) {
      return 0;
    }
    return this.ehDescarga ? this.plano.descargaExecutada : this.plano.embarqueExecutado;
  }

  get totalPendente(): number {
    if (!this.plano) {
      return 0;
    }
    return this.ehDescarga ? this.plano.descargaPendente : this.plano.embarquePendente;
  }

  rotuloTipo(tipo: TipoCargaConteiner): string {
    return this.tiposCarga.find((t) => t.valor === tipo)?.rotulo ?? tipo;
  }

  classeCelula(baia: number, fileira: number, camada: number): string {
    const ocupante = this.atribuicaoEm(baia, fileira, camada);
    if (!ocupante) {
      return 'celula-vazia';
    }
    const base = `celula-ocupada tipo-${ocupante.tipoCarga.toLowerCase()}`;
    return ocupante.embarcado ? `${base} embarcado` : base;
  }

  celulaSelecionadaNoForm(fileira: number, camada: number): boolean {
    return this.baiaSelecionada != null
      && this.novaAtribuicao.baia === this.baiaSelecionada
      && this.novaAtribuicao.fileira === fileira
      && this.novaAtribuicao.camada === camada;
  }

  private aplicarPlano(plano: PlanoEstivaDetalhe): void {
    this.plano = plano;
    this.celulasOcupadas = new Map<string, AtribuicaoEstiva>();
    for (const atribuicao of plano.atribuicoes) {
      this.celulasOcupadas.set(
        this.chaveCelula(atribuicao.tipoOperacao, atribuicao.baia, atribuicao.fileira, atribuicao.camada),
        atribuicao
      );
    }
    if (this.baiaSelecionada == null || this.baiaSelecionada > plano.baias) {
      this.baiaSelecionada = plano.baias > 0 ? 1 : null;
    }
    if (this.baiaSelecionada != null) {
      this.novaAtribuicao.baia = this.baiaSelecionada;
    }
  }

  private reiniciarFormularioAtribuicao(): void {
    const baia = this.baiaSelecionada ?? 1;
    this.novaAtribuicao = { ...this.criarAtribuicaoVazia(), baia };
  }

  private criarAtribuicaoVazia(): NovaAtribuicaoEstiva {
    return {
      tipoOperacao: this.modoOperacao,
      codigoConteiner: '',
      tipoCarga: 'SECO',
      pesoToneladas: null,
      baia: 1,
      fileira: 1,
      camada: 1,
      posicaoPatioOrigem: null,
      posicaoPatioDestino: null,
      sequenciaEmbarque: null
    };
  }

  private intervalo(tamanho: number): number[] {
    return Array.from({ length: Math.max(0, tamanho) }, (_, indice) => indice + 1);
  }

  private chaveCelula(operacao: TipoOperacaoEstiva, baia: number, fileira: number, camada: number): string {
    return `${operacao}-${baia}-${fileira}-${camada}`;
  }

  private extrairMensagem(erro: unknown, padrao: string): string {
    const httpErro = erro as HttpErrorResponse;
    const mensagem = httpErro?.error?.mensagem;
    return typeof mensagem === 'string' && mensagem.length > 0 ? mensagem : padrao;
  }
}
