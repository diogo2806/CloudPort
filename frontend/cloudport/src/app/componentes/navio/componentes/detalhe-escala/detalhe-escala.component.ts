import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  AtualizacaoEscala,
  EscalaDetalhe,
  FaseEscala,
  ROTULOS_FASE,
  ServicoNavioService,
  TRANSICOES_FASE
} from '../../../service/servico-navio/servico-navio.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-detalhe-escala',
  templateUrl: './detalhe-escala.component.html',
  styleUrls: ['./detalhe-escala.component.css'],
  standalone: false
})
export class DetalheEscalaComponent implements OnInit {
  escala?: EscalaDetalhe;
  estaCarregando = false;
  processando = false;
  erro?: string;
  mensagem?: string;
  modoEdicao = false;
  novoConteinerDescarga = '';
  novoConteinerCarga = '';

  edicao: {
    viagemEntrada: string;
    viagemSaida: string;
    chegadaPrevista: string;
    atracacaoPrevista: string;
    partidaPrevista: string;
    bercoPrevisto: string;
    bercoAtual: string;
    observacoes: string;
  } = this.edicaoVazia();

  private escalaId!: number;

  constructor(
    private readonly servicoNavio: ServicoNavioService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService,
    private readonly rota: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const idParam = this.rota.snapshot.paramMap.get('id');
    const id = Number(idParam);
    if (!idParam || !Number.isInteger(id) || id <= 0) {
      this.erro = 'Escala inválida.';
      return;
    }
    this.escalaId = id;
    this.carregarEscala();
  }

  carregarEscala(): void {
    this.estaCarregando = true;
    this.erro = undefined;
    this.servicoNavio.obterEscala(this.escalaId)
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (escala) => {
          this.escala = escala;
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = erro.status === 404
            ? 'Escala não encontrada.'
            : 'Não foi possível carregar a escala.';
          this.escala = undefined;
        }
      });
  }

  get transicoesDisponiveis(): FaseEscala[] {
    if (!this.escala) {
      return [];
    }
    return TRANSICOES_FASE[this.escala.fase] ?? [];
  }

  avancarFase(destino: FaseEscala): void {
    if (!this.escala || this.processando) {
      return;
    }
    this.processando = true;
    this.erro = undefined;
    this.mensagem = undefined;
    this.servicoNavio.avancarFase(this.escala.id, destino)
      .pipe(finalize(() => this.processando = false))
      .subscribe({
        next: (escala) => {
          this.escala = escala;
          this.mensagem = `Escala avançada para "${this.rotuloFase(escala.fase)}".`;
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível avançar a fase da escala.');
        }
      });
  }

  iniciarEdicao(): void {
    if (!this.escala) {
      return;
    }
    this.edicao = {
      viagemEntrada: this.escala.viagemEntrada ?? '',
      viagemSaida: this.escala.viagemSaida ?? '',
      chegadaPrevista: this.paraInputDataHora(this.escala.chegadaPrevista),
      atracacaoPrevista: this.paraInputDataHora(this.escala.atracacaoPrevista),
      partidaPrevista: this.paraInputDataHora(this.escala.partidaPrevista),
      bercoPrevisto: this.escala.bercoPrevisto ?? '',
      bercoAtual: this.escala.bercoAtual ?? '',
      observacoes: this.escala.observacoes ?? ''
    };
    this.modoEdicao = true;
    this.mensagem = undefined;
    this.erro = undefined;
  }

  cancelarEdicao(): void {
    this.modoEdicao = false;
    this.edicao = this.edicaoVazia();
  }

  salvarEdicao(): void {
    if (!this.escala || this.processando) {
      return;
    }
    const payload: AtualizacaoEscala = {
      viagemEntrada: this.normalizarOpcional(this.edicao.viagemEntrada),
      viagemSaida: this.normalizarOpcional(this.edicao.viagemSaida),
      chegadaPrevista: this.formatarDataHoraOpcional(this.edicao.chegadaPrevista),
      atracacaoPrevista: this.formatarDataHoraOpcional(this.edicao.atracacaoPrevista),
      partidaPrevista: this.formatarDataHoraOpcional(this.edicao.partidaPrevista),
      bercoPrevisto: this.normalizarOpcional(this.edicao.bercoPrevisto),
      bercoAtual: this.normalizarOpcional(this.edicao.bercoAtual),
      observacoes: this.normalizarOpcional(this.edicao.observacoes)
    };

    this.processando = true;
    this.erro = undefined;
    this.servicoNavio.atualizarEscala(this.escala.id, payload)
      .pipe(finalize(() => this.processando = false))
      .subscribe({
        next: (escala) => {
          this.escala = escala;
          this.modoEdicao = false;
          this.mensagem = 'Escala atualizada com sucesso.';
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível atualizar a escala.');
        }
      });
  }

  remover(): void {
    if (!this.escala || this.processando) {
      return;
    }
    if (!window.confirm('Deseja realmente remover esta escala?')) {
      return;
    }
    this.processando = true;
    this.servicoNavio.removerEscala(this.escala.id)
      .pipe(finalize(() => this.processando = false))
      .subscribe({
        next: () => {
          this.router.navigate(['/home', 'navio', 'escalas']);
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível remover a escala.');
        }
      });
  }

  voltar(): void {
    this.router.navigate(['/home', 'navio', 'escalas']);
  }

  irParaListaTrabalho(): void {
    if (this.escala) {
      this.router.navigate(['/home', 'navio', 'escalas', this.escala.id, 'lista-trabalho']);
    }
  }

  get listasEditaveis(): boolean {
    return !!this.escala && this.escala.fase !== 'ENCERRADA' && this.escala.fase !== 'CANCELADA';
  }

  adicionarDescarga(): void {
    this.adicionarConteiner(this.novoConteinerDescarga,
      (id, codigo) => this.servicoNavio.adicionarConteinerDescarga(id, codigo),
      () => this.novoConteinerDescarga = '');
  }

  adicionarCarga(): void {
    this.adicionarConteiner(this.novoConteinerCarga,
      (id, codigo) => this.servicoNavio.adicionarConteinerCarga(id, codigo),
      () => this.novoConteinerCarga = '');
  }

  removerDescarga(codigoConteiner: string): void {
    this.executarOperacaoLista(
      (id) => this.servicoNavio.removerConteinerDescarga(id, codigoConteiner));
  }

  removerCarga(codigoConteiner: string): void {
    this.executarOperacaoLista(
      (id) => this.servicoNavio.removerConteinerCarga(id, codigoConteiner));
  }

  private adicionarConteiner(codigo: string,
                             acao: (id: number, codigo: string) => Observable<EscalaDetalhe>,
                             aoConcluir: () => void): void {
    if (!this.escala || this.processando) {
      return;
    }
    const codigoLimpo = (codigo ?? '').trim();
    if (!codigoLimpo) {
      this.erro = 'Informe o código do contêiner.';
      return;
    }
    this.processando = true;
    this.erro = undefined;
    this.mensagem = undefined;
    acao(this.escala.id, codigoLimpo)
      .pipe(finalize(() => this.processando = false))
      .subscribe({
        next: (escala) => {
          this.escala = escala;
          aoConcluir();
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível adicionar o contêiner.');
        }
      });
  }

  private executarOperacaoLista(acao: (id: number) => Observable<EscalaDetalhe>): void {
    if (!this.escala || this.processando) {
      return;
    }
    this.processando = true;
    this.erro = undefined;
    this.mensagem = undefined;
    acao(this.escala.id)
      .pipe(finalize(() => this.processando = false))
      .subscribe({
        next: (escala) => {
          this.escala = escala;
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível atualizar a lista.');
        }
      });
  }

  rotuloFase(fase: FaseEscala): string {
    return ROTULOS_FASE[fase] ?? fase;
  }

  classeFase(fase: FaseEscala): string {
    return `fase-tag fase-${fase.toLowerCase()}`;
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  private edicaoVazia(): typeof this.edicao {
    return {
      viagemEntrada: '',
      viagemSaida: '',
      chegadaPrevista: '',
      atracacaoPrevista: '',
      partidaPrevista: '',
      bercoPrevisto: '',
      bercoAtual: '',
      observacoes: ''
    };
  }

  private normalizarOpcional(valor: string): string | null {
    const limpo = (valor ?? '').trim();
    return limpo.length > 0 ? limpo : null;
  }

  private paraInputDataHora(valor: string | null | undefined): string {
    if (!valor) {
      return '';
    }
    return valor.length >= 16 ? valor.substring(0, 16) : valor;
  }

  private formatarDataHoraOpcional(valor: string): string | null {
    if (!valor) {
      return null;
    }
    return valor.length === 16 ? `${valor}:00` : valor;
  }

  private extrairMensagemErro(erro: HttpErrorResponse, padrao: string): string {
    const corpo = erro?.error;
    if (corpo && typeof corpo === 'object') {
      if (corpo.erros && typeof corpo.erros === 'object') {
        const mensagens = Object.values(corpo.erros).filter((m) => typeof m === 'string');
        if (mensagens.length > 0) {
          return this.sanitizadorConteudo.sanitizar(mensagens.join(' '));
        }
      }
      if (typeof corpo.mensagem === 'string' && corpo.mensagem.trim().length > 0) {
        return this.sanitizadorConteudo.sanitizar(corpo.mensagem);
      }
    }
    return padrao;
  }
}
