import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  CadastroNavio,
  NavioResumo,
  ServicoNavioService
} from '../../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-lista-navios',
  templateUrl: './lista-navios.component.html',
  styleUrls: ['./lista-navios.component.css'],
  standalone: false
})
export class ListaNaviosComponent implements OnInit {
  navios: NavioResumo[] = [];
  estaCarregando = false;
  salvando = false;
  erro?: string;
  mensagem?: string;
  mostrarFormulario = false;

  novo: {
    nome: string;
    codigoImo: string;
    paisBandeira: string;
    empresaArmadora: string;
    capacidadeTeu: number | null;
    loaMetros: number | null;
    caladoMaximoMetros: number | null;
    callSign: string;
  } = this.formularioVazio();

  constructor(
    private readonly servicoNavio: ServicoNavioService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.carregarNavios();
  }

  carregarNavios(): void {
    this.estaCarregando = true;
    this.erro = undefined;
    this.servicoNavio.listarNavios()
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (navios) => {
          this.navios = navios ?? [];
        },
        error: () => {
          this.erro = 'Não foi possível carregar os navios cadastrados.';
          this.navios = [];
        }
      });
  }

  alternarFormulario(): void {
    this.mostrarFormulario = !this.mostrarFormulario;
    if (this.mostrarFormulario) {
      this.novo = this.formularioVazio();
      this.erro = undefined;
    }
  }

  voltarCronograma(): void {
    this.router.navigate(['/home', 'navio', 'escalas']);
  }

  salvar(): void {
    this.erro = undefined;
    this.mensagem = undefined;

    if (!this.novo.nome.trim()) {
      this.erro = 'Informe o nome do navio.';
      return;
    }
    if (!/^IMO[0-9]{7}$/.test(this.novo.codigoImo.trim().toUpperCase())) {
      this.erro = 'O código IMO deve seguir o padrão IMO9999999.';
      return;
    }
    if (!this.novo.paisBandeira.trim()) {
      this.erro = 'Informe o país da bandeira.';
      return;
    }
    if (!this.novo.empresaArmadora.trim()) {
      this.erro = 'Informe a empresa armadora.';
      return;
    }
    if (this.novo.capacidadeTeu === null || this.novo.capacidadeTeu <= 0) {
      this.erro = 'Informe a capacidade em TEU (maior que zero).';
      return;
    }

    const payload: CadastroNavio = {
      nome: this.novo.nome.trim(),
      codigoImo: this.novo.codigoImo.trim().toUpperCase(),
      paisBandeira: this.novo.paisBandeira.trim(),
      empresaArmadora: this.novo.empresaArmadora.trim(),
      capacidadeTeu: this.novo.capacidadeTeu,
      loaMetros: this.novo.loaMetros ?? null,
      caladoMaximoMetros: this.novo.caladoMaximoMetros ?? null,
      callSign: this.novo.callSign.trim() ? this.novo.callSign.trim() : null
    };

    this.salvando = true;
    this.servicoNavio.registrarNavio(payload)
      .pipe(finalize(() => this.salvando = false))
      .subscribe({
        next: () => {
          this.mensagem = 'Navio cadastrado com sucesso.';
          this.mostrarFormulario = false;
          this.novo = this.formularioVazio();
          this.carregarNavios();
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível cadastrar o navio.');
        }
      });
  }

  remover(navio: NavioResumo): void {
    if (this.salvando) {
      return;
    }
    if (!window.confirm(`Remover o navio "${navio.nome}"? Escalas vinculadas impedem a remoção.`)) {
      return;
    }
    this.erro = undefined;
    this.mensagem = undefined;
    this.servicoNavio.removerNavio(navio.identificador)
      .subscribe({
        next: () => {
          this.mensagem = 'Navio removido com sucesso.';
          this.carregarNavios();
        },
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível remover o navio. Verifique se há escalas vinculadas.');
        }
      });
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  private formularioVazio(): typeof this.novo {
    return {
      nome: '',
      codigoImo: '',
      paisBandeira: '',
      empresaArmadora: '',
      capacidadeTeu: null,
      loaMetros: null,
      caladoMaximoMetros: null,
      callSign: ''
    };
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
