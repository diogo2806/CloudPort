import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  EscalaResumo,
  FaseEscala,
  ROTULOS_FASE,
  ServicoNavioService
} from '../../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-lista-escalas',
  templateUrl: './lista-escalas.component.html',
  styleUrls: ['./lista-escalas.component.css'],
  standalone: false
})
export class ListaEscalasComponent implements OnInit {
  escalas: EscalaResumo[] = [];
  estaCarregando = false;
  erroCarregamento?: string;
  ordenacaoAscendente = true;
  diasFiltro = 7;

  constructor(
    private readonly servicoNavio: ServicoNavioService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.carregarEscalas();
  }

  carregarEscalas(): void {
    this.estaCarregando = true;
    this.erroCarregamento = undefined;
    const dias = this.normalizarDiasFiltro(this.diasFiltro);
    this.servicoNavio.listarCronograma(dias)
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (escalas) => {
          this.escalas = this.ordenarPorEta(escalas, this.ordenacaoAscendente);
        },
        error: () => {
          this.erroCarregamento = 'Não foi possível carregar o cronograma de escalas. Atualize a página ou tente novamente mais tarde.';
          this.escalas = [];
        }
      });
  }

  alternarOrdenacao(): void {
    this.ordenacaoAscendente = !this.ordenacaoAscendente;
    this.escalas = this.ordenarPorEta(this.escalas, this.ordenacaoAscendente);
  }

  aoAlterarDias(): void {
    this.diasFiltro = this.normalizarDiasFiltro(this.diasFiltro);
    this.carregarEscalas();
  }

  novaEscala(): void {
    this.router.navigate(['/home', 'navio', 'escalas', 'nova']);
  }

  abrirNavios(): void {
    this.router.navigate(['/home', 'navio', 'navios']);
  }

  verDetalhes(escala: EscalaResumo): void {
    if (!escala || escala.id === undefined || escala.id === null) {
      return;
    }
    this.router.navigate(['/home', 'navio', 'escalas', escala.id]);
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

  private ordenarPorEta(escalas: EscalaResumo[], ascendente: boolean): EscalaResumo[] {
    const ordenadas = [...(escalas ?? [])].sort((a, b) => {
      const tempoA = Date.parse(a?.chegadaPrevista ?? '') || 0;
      const tempoB = Date.parse(b?.chegadaPrevista ?? '') || 0;
      return tempoA - tempoB;
    });
    return ascendente ? ordenadas : ordenadas.reverse();
  }

  private normalizarDiasFiltro(valor: number): number {
    if (!Number.isFinite(valor)) {
      return 7;
    }
    const inteiro = Math.floor(Math.abs(valor));
    if (inteiro < 1) {
      return 1;
    }
    if (inteiro > 60) {
      return 60;
    }
    return inteiro;
  }
}
