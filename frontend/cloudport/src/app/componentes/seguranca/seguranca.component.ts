import { Component, OnInit } from '@angular/core';
import { DiretrizSeguranca, PoliticaSegurancaService } from '../service/seguranca/politica-seguranca.service';

@Component({
  selector: 'app-seguranca',
  templateUrl: './seguranca.component.html',
  styleUrls: ['./seguranca.component.css']
})
export class SegurancaComponent implements OnInit {
  diretrizes: DiretrizSeguranca[] = [];
  carregando = false;
  mensagemErro: string | null = null;

  constructor(private readonly politicaSegurancaService: PoliticaSegurancaService) {}

  ngOnInit(): void {
    this.carregarDiretrizes();
  }

  private carregarDiretrizes(): void {
    this.carregando = true;
    this.mensagemErro = null;
    this.politicaSegurancaService.listarDiretrizes().subscribe({
      next: (diretrizes) => {
        this.diretrizes = diretrizes;
        this.carregando = false;
      },
      error: (erro) => {
        console.error('Erro ao carregar políticas de segurança', erro);
        this.mensagemErro = 'Não foi possível carregar as políticas de segurança no momento. Tente novamente mais tarde.';
        this.carregando = false;
      }
    });
  }
}
