import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  OrdemMovimentacao,
  ServicoListaTrabalhoTremService,
  StatusOrdemMovimentacao,
  TipoMovimentacaoOrdem
} from '../../../service/servico-lista-trabalho-trem/servico-lista-trabalho-trem.service';

interface OrdemVisivel extends OrdemMovimentacao {
  emAtualizacao?: boolean;
}

@Component({
  selector: 'app-lista-trabalho-trem',
  templateUrl: './lista-trabalho-trem.component.html',
  styleUrls: ['./lista-trabalho-trem.component.css']
})
export class ListaTrabalhoTremComponent implements OnInit {
  visitaId?: number;
  ordens: OrdemVisivel[] = [];
  estaCarregando = false;
  mensagemSucesso?: string;
  mensagemErro?: string;

  constructor(
    private readonly rotaAtiva: ActivatedRoute,
    private readonly router: Router,
    private readonly servicoListaTrabalho: ServicoListaTrabalhoTremService,
    private readonly sanitizador: SanitizadorConteudoService
  ) {}

  ngOnInit(): void {
    const parametroId = this.rotaAtiva.snapshot.paramMap.get('id');
    const id = parametroId ? Number(parametroId) : NaN;
    if (!Number.isFinite(id) || id <= 0) {
      this.mensagemErro = 'Identificador do trem inválido.';
      return;
    }
    this.visitaId = id;
    this.carregarOrdens();
  }

  carregarOrdens(): void {
    if (!this.visitaId) {
      return;
    }
    this.estaCarregando = true;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;
    this.servicoListaTrabalho.listarOrdens(this.visitaId)
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (ordens) => {
          this.ordens = this.organizarOrdens(ordens);
        },
        error: () => {
          this.ordens = [];
          this.mensagemErro = 'Não foi possível carregar as ordens de movimentação do trem.';
        }
      });
  }

  assumirOrdem(ordem: OrdemVisivel): void {
    this.atualizarStatus(ordem, 'EM_EXECUCAO');
  }

  concluirOrdem(ordem: OrdemVisivel): void {
    this.atualizarStatus(ordem, 'CONCLUIDA');
  }

  podeAssumir(ordem: OrdemMovimentacao): boolean {
    return ordem.statusMovimentacao === 'PENDENTE';
  }

  podeConcluir(ordem: OrdemMovimentacao): boolean {
    return ordem.statusMovimentacao === 'EM_EXECUCAO';
  }

  estaEmAtualizacao(ordem: OrdemVisivel): boolean {
    return !!ordem.emAtualizacao;
  }

  voltarParaDetalhes(): void {
    if (this.visitaId) {
      this.router.navigate(['/home', 'ferrovia', 'visitas', this.visitaId]);
    }
  }

  descricaoStatus(status: StatusOrdemMovimentacao): string {
    switch (status) {
      case 'EM_EXECUCAO':
        return 'Em execução';
      case 'CONCLUIDA':
        return 'Concluída';
      default:
        return 'Pendente';
    }
  }

  descricaoTipo(tipo: TipoMovimentacaoOrdem): string {
    return tipo === 'DESCARGA_TREM' ? 'Descarga do trem' : 'Carga no trem';
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizador.sanitizar(valor);
  }

  ordensVisiveis(): OrdemVisivel[] {
    return this.ordens.filter(ordem => ordem.statusMovimentacao !== 'CONCLUIDA');
  }

  private atualizarStatus(ordem: OrdemVisivel, status: StatusOrdemMovimentacao): void {
    if (!this.visitaId || !ordem || ordem.emAtualizacao) {
      return;
    }
    ordem.emAtualizacao = true;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;

    this.servicoListaTrabalho.atualizarStatus(this.visitaId, ordem.id, status)
      .pipe(finalize(() => {
        ordem.emAtualizacao = false;
      }))
      .subscribe({
        next: (ordemAtualizada) => {
          ordem.statusMovimentacao = ordemAtualizada.statusMovimentacao;
          ordem.atualizadoEm = ordemAtualizada.atualizadoEm;
          ordem.criadoEm = ordemAtualizada.criadoEm;
          if (ordem.statusMovimentacao === 'CONCLUIDA') {
            this.ordens = this.ordens.filter(item => item.id !== ordem.id);
            this.mensagemSucesso = 'Ordem concluída com sucesso.';
          } else {
            this.mensagemSucesso = 'Ordem assumida com sucesso.';
          }
        },
        error: () => {
          this.mensagemErro = status === 'CONCLUIDA'
            ? 'Não foi possível concluir a ordem. Tente novamente.'
            : 'Não foi possível assumir a ordem. Tente novamente.';
        }
      });
  }

  private organizarOrdens(ordens: OrdemMovimentacao[]): OrdemVisivel[] {
    return [...(ordens ?? [])]
      .sort((a, b) => {
        const dataA = Date.parse(a?.criadoEm ?? '') || 0;
        const dataB = Date.parse(b?.criadoEm ?? '') || 0;
        return dataA - dataB;
      })
      .map(ordem => ({ ...ordem, emAtualizacao: false }));
  }
}
