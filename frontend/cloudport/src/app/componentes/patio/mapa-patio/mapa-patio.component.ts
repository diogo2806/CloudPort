import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  EventoTempoRealMapa,
  FiltrosMapaPatio,
  MapaPatioResposta,
  ServicoPatioService
} from '../../service/servico-patio/servico-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

interface FiltroFormulario {
  status: FormControl<string[]>;
  tiposCarga: FormControl<string[]>;
  destinos: FormControl<string[]>;
  camadas: FormControl<string[]>;
  tiposEquipamento: FormControl<string[]>;
}

interface ConteinerEmMovimento {
  codigo: string;
  linhaOrigem: number;
  colunaOrigem: number;
  linhaDestino?: number;
  colunaDestino?: number;
}

@Component({
    selector: 'app-mapa-patio',
    templateUrl: './mapa-patio.component.html',
    styleUrls: ['./mapa-patio.component.css'],
    standalone: false
})
export class MapaPatioComponent implements OnInit, OnDestroy {
  filtrosDisponiveis?: FiltrosMapaPatio;
  formularioFiltros: FormGroup;
  mapaCompleto?: MapaPatioResposta;
  mapaFiltrado?: MapaPatioResposta;
  carregando = false;
  carregandoFiltros = false;
  erro?: string;
  inscricaoTempoReal?: Subscription;
  inscricaoFormulario?: Subscription;
  conteinerEmDragueio?: ConteinerEmMovimento;
  celulaDestineAlvo?: { linha: number; coluna: number };
  salvandoModo = false;
  mensagemSucesso?: string;

  constructor(
    private readonly servicoPatio: ServicoPatioService,
    private readonly formBuilder: FormBuilder,
    private readonly sanitizador: SanitizadorConteudoService
  ) {
    this.formularioFiltros = this.formBuilder.group({
      status: this.formBuilder.control<string[]>([]),
      tiposCarga: this.formBuilder.control<string[]>([]),
      destinos: this.formBuilder.control<string[]>([]),
      camadas: this.formBuilder.control<string[]>([]),
      tiposEquipamento: this.formBuilder.control<string[]>([])
    });
  }

  ngOnInit(): void {
    this.carregarFiltros();
    this.carregarMapaCompleto();
    this.inscricaoTempoReal = this.servicoPatio.iniciarMonitoramentoTempoReal().subscribe({
      next: (evento) => this.processarEventoTempoReal(evento),
      error: (erro) => {
        console.error('Erro ao receber atualizações de pátio', erro);
        this.erro = 'Falha na atualização em tempo real. Atualize a página para reconectar.';
      }
    });
    this.inscricaoFormulario = this.formularioFiltros.valueChanges.subscribe(() => this.aplicarFiltrosLocais());
  }

  ngOnDestroy(): void {
    this.inscricaoTempoReal?.unsubscribe();
    this.inscricaoFormulario?.unsubscribe();
  }

  obterClasseStatus(status: string): string {
    switch ((status ?? '').toUpperCase()) {
      case 'AGUARDANDO_RETIRADA':
        return 'status-aguardando';
      case 'INSPECIONANDO':
        return 'status-inspecionando';
      case 'DESPACHADO':
        return 'status-despachado';
      case 'RETIDO':
        return 'status-retido';
      case 'DANIFICADO':
        return 'status-danificado';
      default:
        return 'status-desconhecido';
    }
  }

  obterCelulas(): number[] {
    const linhas = this.mapaFiltrado?.totalLinhas ?? 0;
    const colunas = this.mapaFiltrado?.totalColunas ?? 0;
    return Array.from({ length: linhas * colunas }, (_, indice) => indice);
  }

  obterLinha(indice: number): number {
    const colunas = this.mapaFiltrado?.totalColunas ?? 1;
    return Math.floor(indice / colunas);
  }

  obterColuna(indice: number): number {
    const colunas = this.mapaFiltrado?.totalColunas ?? 1;
    return indice % colunas;
  }

  obterTemplateColunas(): string {
    const colunas = this.mapaFiltrado?.totalColunas ?? 0;
    return `repeat(${colunas}, minmax(70px, 1fr))`;
  }

  obterTemplateLinhas(): string {
    const linhas = this.mapaFiltrado?.totalLinhas ?? 0;
    return `repeat(${linhas}, minmax(70px, 1fr))`;
  }

  sanitizarTexto(texto: string | null | undefined): string {
    return this.sanitizador.sanitizar(texto ?? '');
  }

  iniciarDrag(evento: DragEvent, conteiner: any): void {
    if (evento.dataTransfer) {
      evento.dataTransfer.effectAllowed = 'move';
      evento.dataTransfer.setData('application/json', JSON.stringify(conteiner));
    }
    this.conteinerEmDragueio = {
      codigo: conteiner.codigo,
      linhaOrigem: conteiner.linha,
      colunaOrigem: conteiner.coluna
    };
  }

  terminarDrag(): void {
    this.conteinerEmDragueio = undefined;
    this.celulaDestineAlvo = undefined;
  }

  sobrecelula(evento: DragEvent, linha: number, coluna: number): void {
    evento.preventDefault();
    if (evento.dataTransfer) {
      evento.dataTransfer.dropEffect = 'move';
    }
    if (this.conteinerEmDragueio) {
      this.celulaDestineAlvo = { linha, coluna };
    }
  }

  saiuDaCelula(): void {
    this.celulaDestineAlvo = undefined;
  }

  soltarEmCelula(evento: DragEvent, linha: number, coluna: number): void {
    evento.preventDefault();
    if (!this.conteinerEmDragueio) {
      return;
    }

    const posicaoAtual = `(${this.conteinerEmDragueio.linhaOrigem}, ${this.conteinerEmDragueio.colunaOrigem})`;
    const posicaoNova = `(${linha}, ${coluna})`;

    if (posicaoAtual === posicaoNova) {
      this.celulaDestineAlvo = undefined;
      this.conteinerEmDragueio = undefined;
      return;
    }

    if (!this.mapaFiltrado) {
      return;
    }

    const conteinerOriginal = this.mapaFiltrado.conteineres.find(
      (c) => c.linha === this.conteinerEmDragueio?.linhaOrigem &&
             c.coluna === this.conteinerEmDragueio?.colunaOrigem
    );

    if (!conteinerOriginal) {
      this.erro = 'Contêiner não encontrado.';
      this.celulaDestineAlvo = undefined;
      this.conteinerEmDragueio = undefined;
      return;
    }

    const validacao = this.validarMovimentacao(linha, coluna, conteinerOriginal);
    if (!validacao.valido) {
      this.erro = `Movimento não permitido: ${validacao.motivo}`;
      this.celulaDestineAlvo = undefined;
      this.conteinerEmDragueio = undefined;
      return;
    }

    this.executarMovimentacao(conteinerOriginal, linha, coluna);
  }

  private validarMovimentacao(linha: number, coluna: number, conteiner: any): { valido: boolean; motivo?: string } {
    if (!this.mapaCompleto) {
      return { valido: false, motivo: 'Mapa não carregado' };
    }

    if (linha < 0 || coluna < 0 || linha >= (this.mapaCompleto.totalLinhas || 0) || coluna >= (this.mapaCompleto.totalColunas || 0)) {
      return { valido: false, motivo: 'Posição fora dos limites do pátio' };
    }

    const jaOcupada = this.mapaCompleto.conteineres.some(
      (c) => c.linha === linha && c.coluna === coluna && c.codigo !== conteiner.codigo
    );

    if (jaOcupada) {
      return { valido: false, motivo: 'Posição já ocupada por outro contêiner' };
    }

    const equipamentoNo = this.mapaCompleto.equipamentos.some(
      (e) => e.linha === linha && e.coluna === coluna
    );

    if (equipamentoNo) {
      return { valido: false, motivo: 'Posição ocupada por equipamento' };
    }

    return { valido: true };
  }

  private executarMovimentacao(conteiner: any, novaLinha: number, novaColuna: number): void {
    this.salvandoModo = true;
    this.erro = undefined;
    this.mensagemSucesso = undefined;

    const conteinerAtualizado = {
      ...conteiner,
      linha: novaLinha,
      coluna: novaColuna
    };

    this.servicoPatio.salvarConteiner(conteinerAtualizado).subscribe({
      next: () => {
        this.mensagemSucesso = `Contêiner ${conteiner.codigo} movido com sucesso.`;
        this.carregarMapaCompleto();
        this.salvandoModo = false;
      },
      error: (erro) => {
        this.erro = `Erro ao mover contêiner: ${erro?.error?.message || 'operação falhou'}`;
        this.salvandoModo = false;
      }
    });

    this.celulaDestineAlvo = undefined;
    this.conteinerEmDragueio = undefined;
  }

  private carregarFiltros(): void {
    this.carregandoFiltros = true;
    this.servicoPatio.obterFiltros().subscribe({
      next: (filtros) => {
        this.filtrosDisponiveis = filtros;
        this.carregandoFiltros = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar os filtros do pátio.';
        this.carregandoFiltros = false;
      }
    });
  }

  private carregarMapaCompleto(): void {
    this.carregando = true;
    this.servicoPatio.obterMapa({}).subscribe({
      next: (mapa) => {
        this.mapaCompleto = mapa;
        this.aplicarFiltrosLocais();
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar o mapa do pátio.';
        this.carregando = false;
      }
    });
  }

  private processarEventoTempoReal(evento: EventoTempoRealMapa): void {
    if (!evento || !evento.mapa) {
      return;
    }
    this.mapaCompleto = evento.mapa;
    this.aplicarFiltrosLocais();
  }

  private aplicarFiltrosLocais(): void {
    if (!this.mapaCompleto) {
      return;
    }
    const valores = this.formularioFiltros.getRawValue();
    const filtrosAtivos = {
      status: new Set((valores.status ?? []).map((valor: string) => valor.toUpperCase())),
      tiposCarga: new Set((valores.tiposCarga ?? []).map((valor: string) => valor.toUpperCase())),
      destinos: new Set((valores.destinos ?? []).map((valor: string) => valor.toUpperCase())),
      camadas: new Set((valores.camadas ?? []).map((valor: string) => valor.toUpperCase())),
      tiposEquipamento: new Set((valores.tiposEquipamento ?? []).map((valor: string) => valor.toUpperCase()))
    };

    const conteineresFiltrados = this.mapaCompleto.conteineres.filter((conteiner) => {
      const statusValido = filtrosAtivos.status.size === 0 || filtrosAtivos.status.has((conteiner.status ?? '').toUpperCase());
      const tipoCargaValido = filtrosAtivos.tiposCarga.size === 0 || filtrosAtivos.tiposCarga.has((conteiner.tipoCarga ?? '').toUpperCase());
      const destinoValido = filtrosAtivos.destinos.size === 0 || filtrosAtivos.destinos.has((conteiner.destino ?? '').toUpperCase());
      const camadaValida = filtrosAtivos.camadas.size === 0 || filtrosAtivos.camadas.has((conteiner.camadaOperacional ?? '').toUpperCase());
      return statusValido && tipoCargaValido && destinoValido && camadaValida;
    });

    const equipamentosFiltrados = this.mapaCompleto.equipamentos.filter((equipamento) => {
      return filtrosAtivos.tiposEquipamento.size === 0 || filtrosAtivos.tiposEquipamento.has((equipamento.tipoEquipamento ?? '').toUpperCase());
    });

    this.mapaFiltrado = {
      ...this.mapaCompleto,
      conteineres: conteineresFiltrados,
      equipamentos: equipamentosFiltrados
    };
  }
}
