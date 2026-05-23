import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {
  Berco,
  ServicoNavioService,
  VisitaNavioResumo
} from '../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-navio-painel-atracacao',
  templateUrl: './painel-atracacao.component.html',
  standalone: false
})
export class PainelAtracacaoComponent implements OnInit {
  agenda: VisitaNavioResumo[] = [];
  bercos: Berco[] = [];
  carregando = false;
  erro: string | null = null;

  constructor(
    private readonly servico: ServicoNavioService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.servico.listarBercos().subscribe({
      next: (dados) => (this.bercos = dados),
      error: () => undefined
    });
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.servico.listarAgendaAtracacao().subscribe({
      next: (dados) => {
        this.agenda = dados;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar a agenda de atracação.';
        this.carregando = false;
      }
    });
  }

  visitasDoBerco(berco: Berco): VisitaNavioResumo[] {
    return this.agenda.filter((visita) => visita.bercoNome === berco.nome);
  }

  abrir(visita: VisitaNavioResumo): void {
    this.router.navigate(['/home/navio/visitas', visita.identificador]);
  }
}
