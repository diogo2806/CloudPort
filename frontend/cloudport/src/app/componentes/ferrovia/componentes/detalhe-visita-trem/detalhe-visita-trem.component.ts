import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import { ServicoFerroviaService, VisitaTrem } from '../../../service/servico-ferrovia/servico-ferrovia.service';

@Component({
  selector: 'app-detalhe-visita-trem',
  templateUrl: './detalhe-visita-trem.component.html',
  styleUrls: ['./detalhe-visita-trem.component.css']
})
export class DetalheVisitaTremComponent implements OnInit {
  visita?: VisitaTrem;
  estaCarregando = false;
  erroCarregamento?: string;

  constructor(
    private readonly rotaAtiva: ActivatedRoute,
    private readonly router: Router,
    private readonly servicoFerrovia: ServicoFerroviaService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService
  ) {}

  ngOnInit(): void {
    this.carregarVisita();
  }

  voltarParaLista(): void {
    this.router.navigate(['/home', 'ferrovia', 'visitas']);
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  private carregarVisita(): void {
    const parametroId = this.rotaAtiva.snapshot.paramMap.get('id');
    const id = parametroId ? Number(parametroId) : NaN;

    if (!Number.isFinite(id) || id <= 0) {
      this.erroCarregamento = 'Identificador da visita inválido.';
      return;
    }

    this.estaCarregando = true;
    this.erroCarregamento = undefined;

    this.servicoFerrovia.obterVisita(id)
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (visita) => {
          this.visita = visita;
        },
        error: () => {
          this.erroCarregamento = 'Não foi possível localizar a visita do trem solicitada.';
          this.visita = undefined;
        }
      });
  }
}
