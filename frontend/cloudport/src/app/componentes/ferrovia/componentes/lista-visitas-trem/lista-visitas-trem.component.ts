import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import { ServicoFerroviaService, VisitaTrem } from '../../../service/servico-ferrovia/servico-ferrovia.service';

@Component({
  selector: 'app-lista-visitas-trem',
  templateUrl: './lista-visitas-trem.component.html',
  styleUrls: ['./lista-visitas-trem.component.css']
})
export class ListaVisitasTremComponent implements OnInit {
  visitas: VisitaTrem[] = [];
  estaCarregando = false;
  erroCarregamento?: string;
  ordenacaoAscendente = true;
  diasFiltro = 7;

  constructor(
    private readonly servicoFerrovia: ServicoFerroviaService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.carregarVisitas();
  }

  carregarVisitas(): void {
    this.estaCarregando = true;
    this.erroCarregamento = undefined;
    const diasConsulta = this.normalizarDiasFiltro(this.diasFiltro);
    this.servicoFerrovia.listarVisitasProximosDias(diasConsulta)
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (visitas) => {
          this.visitas = this.ordenarPorEta(visitas, this.ordenacaoAscendente);
        },
        error: () => {
          this.erroCarregamento = 'Não foi possível carregar as visitas de trem. Atualize a página ou tente novamente mais tarde.';
          this.visitas = [];
        }
      });
  }

  alternarOrdenacao(): void {
    this.ordenacaoAscendente = !this.ordenacaoAscendente;
    this.visitas = this.ordenarPorEta(this.visitas, this.ordenacaoAscendente);
  }

  aoAlterarDias(): void {
    this.diasFiltro = this.normalizarDiasFiltro(this.diasFiltro);
    this.carregarVisitas();
  }

  abrirImportacao(): void {
    this.router.navigate(['/home', 'ferrovia', 'visitas', 'importar']);
  }

  verDetalhes(visita: VisitaTrem): void {
    if (!visita || visita.id === undefined || visita.id === null) {
      return;
    }
    this.router.navigate(['/home', 'ferrovia', 'visitas', visita.id]);
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  private ordenarPorEta(visitas: VisitaTrem[], ascendente: boolean): VisitaTrem[] {
    const ordenadas = [...(visitas ?? [])].sort((a, b) => {
      const tempoA = Date.parse(a?.horaChegadaPrevista ?? '') || 0;
      const tempoB = Date.parse(b?.horaChegadaPrevista ?? '') || 0;
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
    if (inteiro > 30) {
      return 30;
    }
    return inteiro;
  }
}
