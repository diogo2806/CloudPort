import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  OrdemMovimentacaoNavio,
  ROTULOS_STATUS_ORDEM,
  ROTULOS_TIPO_MOVIMENTACAO,
  ServicoNavioService,
  StatusOrdemMovimentacaoNavio,
  TipoMovimentacaoOrdemNavio
} from '../../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-lista-trabalho-escala',
  templateUrl: './lista-trabalho-escala.component.html',
  styleUrls: ['./lista-trabalho-escala.component.css'],
  standalone: false
})
export class ListaTrabalhoEscalaComponent implements OnInit {
  ordens: OrdemMovimentacaoNavio[] = [];
  estaCarregando = false;
  processando = false;
  erro?: string;

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
    this.carregarOrdens();
  }

  carregarOrdens(): void {
    this.estaCarregando = true;
    this.erro = undefined;
    this.servicoNavio.listarOrdens(this.escalaId)
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (ordens) => {
          this.ordens = ordens ?? [];
        },
        error: () => {
          this.erro = 'Não foi possível carregar a lista de trabalho.';
          this.ordens = [];
        }
      });
  }

  proximoStatus(ordem: OrdemMovimentacaoNavio): StatusOrdemMovimentacaoNavio | null {
    if (ordem.statusMovimentacao === 'PENDENTE') {
      return 'EM_EXECUCAO';
    }
    if (ordem.statusMovimentacao === 'EM_EXECUCAO') {
      return 'CONCLUIDA';
    }
    return null;
  }

  avancar(ordem: OrdemMovimentacaoNavio): void {
    const proximo = this.proximoStatus(ordem);
    if (!proximo || this.processando) {
      return;
    }
    this.processando = true;
    this.erro = undefined;
    this.servicoNavio.atualizarStatusOrdem(this.escalaId, ordem.id, proximo)
      .pipe(finalize(() => this.processando = false))
      .subscribe({
        next: () => this.carregarOrdens(),
        error: (erro: HttpErrorResponse) => {
          this.erro = this.extrairMensagemErro(erro, 'Não foi possível atualizar a ordem.');
        }
      });
  }

  voltar(): void {
    this.router.navigate(['/home', 'navio', 'escalas', this.escalaId]);
  }

  rotuloTipo(tipo: TipoMovimentacaoOrdemNavio): string {
    return ROTULOS_TIPO_MOVIMENTACAO[tipo] ?? tipo;
  }

  rotuloStatus(status: StatusOrdemMovimentacaoNavio): string {
    return ROTULOS_STATUS_ORDEM[status] ?? status;
  }

  rotuloProximo(ordem: OrdemMovimentacaoNavio): string {
    const proximo = this.proximoStatus(ordem);
    return proximo ? this.rotuloStatus(proximo) : '';
  }

  classeStatus(status: StatusOrdemMovimentacaoNavio): string {
    return `status-tag status-${status.toLowerCase()}`;
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  private extrairMensagemErro(erro: HttpErrorResponse, padrao: string): string {
    const corpo = erro?.error;
    if (corpo && typeof corpo === 'object' && typeof corpo.mensagem === 'string' && corpo.mensagem.trim()) {
      return this.sanitizadorConteudo.sanitizar(corpo.mensagem);
    }
    return padrao;
  }
}
