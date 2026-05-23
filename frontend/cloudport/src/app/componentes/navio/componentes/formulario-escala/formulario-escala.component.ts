import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  CadastroEscala,
  NavioResumo,
  ServicoNavioService
} from '../../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-formulario-escala',
  templateUrl: './formulario-escala.component.html',
  styleUrls: ['./formulario-escala.component.css'],
  standalone: false
})
export class FormularioEscalaComponent implements OnInit {
  navios: NavioResumo[] = [];
  carregandoNavios = false;
  salvando = false;
  erro?: string;

  navioId: number | null = null;
  viagemEntrada = '';
  viagemSaida = '';
  chegadaPrevista = '';
  atracacaoPrevista = '';
  partidaPrevista = '';
  bercoPrevisto = '';
  observacoes = '';

  constructor(
    private readonly servicoNavio: ServicoNavioService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.carregarNavios();
  }

  carregarNavios(): void {
    this.carregandoNavios = true;
    this.servicoNavio.listarNavios()
      .pipe(finalize(() => this.carregandoNavios = false))
      .subscribe({
        next: (navios) => {
          this.navios = navios ?? [];
        },
        error: () => {
          this.erro = 'Não foi possível carregar os navios cadastrados. Cadastre um navio antes de criar uma escala.';
          this.navios = [];
        }
      });
  }

  salvar(): void {
    this.erro = undefined;

    if (this.navioId === null || this.navioId === undefined) {
      this.erro = 'Selecione o navio da escala.';
      return;
    }
    if (!this.viagemEntrada.trim()) {
      this.erro = 'Informe a viagem de entrada.';
      return;
    }
    if (!this.chegadaPrevista) {
      this.erro = 'Informe a chegada prevista (ETA).';
      return;
    }

    const payload: CadastroEscala = {
      viagemEntrada: this.viagemEntrada.trim(),
      viagemSaida: this.normalizarOpcional(this.viagemSaida),
      chegadaPrevista: this.formatarDataHora(this.chegadaPrevista),
      atracacaoPrevista: this.formatarDataHoraOpcional(this.atracacaoPrevista),
      partidaPrevista: this.formatarDataHoraOpcional(this.partidaPrevista),
      bercoPrevisto: this.normalizarOpcional(this.bercoPrevisto),
      observacoes: this.normalizarOpcional(this.observacoes)
    };

    this.salvando = true;
    this.servicoNavio.registrarEscala(this.navioId, payload)
      .pipe(finalize(() => this.salvando = false))
      .subscribe({
        next: (escala) => {
          this.router.navigate(['/home', 'navio', 'escalas', escala.id]);
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro);
        }
      });
  }

  cancelar(): void {
    this.router.navigate(['/home', 'navio', 'escalas']);
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  private normalizarOpcional(valor: string): string | null {
    const limpo = (valor ?? '').trim();
    return limpo.length > 0 ? limpo : null;
  }

  private formatarDataHora(valor: string): string {
    return valor.length === 16 ? `${valor}:00` : valor;
  }

  private formatarDataHoraOpcional(valor: string): string | null {
    if (!valor) {
      return null;
    }
    return this.formatarDataHora(valor);
  }

  private extrairMensagemErro(erro: HttpErrorResponse): string {
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
    return 'Não foi possível salvar a escala. Verifique os dados e tente novamente.';
  }
}
