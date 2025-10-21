import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  OrdemTrabalhoPatio,
  ServicoListaTrabalhoPatioService,
  StatusOrdemTrabalhoPatio
} from '../../service/servico-lista-trabalho-patio/servico-lista-trabalho-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

@Component({
  selector: 'app-lista-trabalho-patio',
  templateUrl: './lista-trabalho-patio.component.html',
  styleUrls: ['./lista-trabalho-patio.component.css']
})
export class ListaTrabalhoPatioComponent implements OnInit, OnDestroy {
  ordens: OrdemTrabalhoPatio[] = [];
  carregando = false;
  mensagemErro?: string;
  mensagemSucesso?: string;
  ordensEmAtualizacao = new Set<number>();
  filtroStatus?: StatusOrdemTrabalhoPatio;
  private inscricao?: Subscription;

  constructor(
    private readonly servicoListaTrabalho: ServicoListaTrabalhoPatioService,
    private readonly sanitizador: SanitizadorConteudoService
  ) {}

  ngOnInit(): void {
    this.carregarOrdens();
  }

  ngOnDestroy(): void {
    this.inscricao?.unsubscribe();
  }

  carregarOrdens(status?: StatusOrdemTrabalhoPatio): void {
    this.filtroStatus = status;
    this.carregando = true;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;
    this.inscricao?.unsubscribe();
    this.inscricao = this.servicoListaTrabalho.listarOrdens(status)
      .pipe(finalize(() => (this.carregando = false)))
      .subscribe({
        next: (ordens) => {
          this.ordens = ordens;
        },
        error: () => {
          this.mensagemErro = 'Não foi possível carregar a lista de trabalho do pátio.';
          this.ordens = [];
        }
      });
  }

  alterarFiltro(evento: Event): void {
    const alvo = evento.target as HTMLSelectElement;
    const valor = (alvo?.value ?? '').trim();
    const novoStatus = valor === '' ? undefined : (valor as StatusOrdemTrabalhoPatio);
    this.carregarOrdens(novoStatus);
  }

  iniciarOrdem(ordem: OrdemTrabalhoPatio): void {
    if (!this.podeIniciar(ordem)) {
      return;
    }
    this.atualizarStatus(ordem, 'EM_EXECUCAO');
  }

  concluirOrdem(ordem: OrdemTrabalhoPatio): void {
    if (!this.podeConcluir(ordem)) {
      return;
    }
    this.atualizarStatus(ordem, 'CONCLUIDA');
  }

  podeIniciar(ordem: OrdemTrabalhoPatio): boolean {
    return ordem.statusOrdem === 'PENDENTE';
  }

  podeConcluir(ordem: OrdemTrabalhoPatio): boolean {
    return ordem.statusOrdem === 'EM_EXECUCAO';
  }

  estaAtualizando(ordem: OrdemTrabalhoPatio): boolean {
    return this.ordensEmAtualizacao.has(ordem.id);
  }

  descricaoStatus(status: StatusOrdemTrabalhoPatio): string {
    switch (status) {
      case 'PENDENTE':
        return 'Pendente';
      case 'EM_EXECUCAO':
        return 'Em execução';
      case 'CONCLUIDA':
        return 'Concluída';
      default:
        return status;
    }
  }

  descricaoMovimento(tipo: string): string {
    const texto = tipo.replace(/_/g, ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  sanitizar(valor?: string): string {
    return this.sanitizador.sanitizar(valor ?? '');
  }

  private atualizarStatus(ordem: OrdemTrabalhoPatio, novoStatus: StatusOrdemTrabalhoPatio): void {
    this.ordensEmAtualizacao.add(ordem.id);
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;
    this.servicoListaTrabalho.atualizarStatus(ordem.id, novoStatus)
      .pipe(finalize(() => this.ordensEmAtualizacao.delete(ordem.id)))
      .subscribe({
        next: (atualizada) => {
          const indice = this.ordens.findIndex((item) => item.id === atualizada.id);
          if (indice >= 0) {
            this.ordens[indice] = atualizada;
          }
          this.mensagemSucesso = `Ordem do contêiner ${this.sanitizar(atualizada.codigoConteiner)} atualizada com sucesso.`;
        },
        error: () => {
          this.mensagemErro = 'Não foi possível atualizar o status da ordem.';
        }
      });
  }
}
