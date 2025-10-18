import { Component, OnInit } from '@angular/core';
import { MovimentoPatio, ServicoPatioService } from '../../service/servico-patio/servico-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

@Component({
  selector: 'app-lista-movimentacoes',
  templateUrl: './lista-movimentacoes.component.html',
  styleUrls: ['./lista-movimentacoes.component.css']
})
export class ListaMovimentacoesComponent implements OnInit {
  movimentacoes: MovimentoPatio[] = [];
  carregando = false;
  erro?: string;

  constructor(
    private readonly servicoPatio: ServicoPatioService,
    private readonly sanitizador: SanitizadorConteudoService
  ) { }

  ngOnInit(): void {
    this.carregarMovimentacoes();
  }

  carregarMovimentacoes(): void {
    this.carregando = true;
    this.erro = undefined;
    this.servicoPatio.listarMovimentacoes().subscribe({
      next: (movimentacoes) => {
        this.movimentacoes = movimentacoes;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível recuperar as movimentações recentes. Tente novamente em instantes.';
        this.carregando = false;
      }
    });
  }

  sanitizar(valor: string | null | undefined): string {
    return this.sanitizador.sanitizar(valor ?? '');
  }

  identificarMovimento(index: number, movimento: MovimentoPatio): number | undefined {
    return movimento?.id ?? index;
  }
}
