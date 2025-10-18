import { Component, OnInit } from '@angular/core';
import { PosicaoPatio, ServicoPatioService } from '../../service/servico-patio/servico-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

@Component({
  selector: 'app-lista-posicoes',
  templateUrl: './lista-posicoes.component.html',
  styleUrls: ['./lista-posicoes.component.css']
})
export class ListaPosicoesComponent implements OnInit {
  posicoes: PosicaoPatio[] = [];
  carregando = false;
  erro?: string;

  constructor(
    private readonly servicoPatio: ServicoPatioService,
    private readonly sanitizador: SanitizadorConteudoService
  ) { }

  ngOnInit(): void {
    this.carregarPosicoes();
  }

  carregarPosicoes(): void {
    this.carregando = true;
    this.erro = undefined;
    this.servicoPatio.listarPosicoes().subscribe({
      next: (posicoes) => {
        this.posicoes = posicoes;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar as posições do pátio. Tente novamente em instantes.';
        this.carregando = false;
      }
    });
  }

  descricaoOcupacao(posicao: PosicaoPatio): string {
    return posicao.ocupada ? 'Ocupada' : 'Livre';
  }

  sanitizar(valor: string | null | undefined): string {
    return this.sanitizador.sanitizar(valor ?? '');
  }

  identificarPosicao(index: number, posicao: PosicaoPatio): number | undefined {
    return posicao?.id ?? index;
  }
}
