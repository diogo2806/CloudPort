import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  OperacaoConteinerVisita,
  ServicoFerroviaService,
  StatusOperacaoConteinerVisita,
  VisitaTrem
} from '../../../service/servico-ferrovia/servico-ferrovia.service';

@Component({
  selector: 'app-detalhe-visita-trem',
  templateUrl: './detalhe-visita-trem.component.html',
  styleUrls: ['./detalhe-visita-trem.component.css']
})
export class DetalheVisitaTremComponent implements OnInit {
  visita?: VisitaTrem;
  estaCarregando = false;
  erroCarregamento?: string;
  abaAtiva: 'DESCARGA' | 'CARGA' = 'DESCARGA';
  mensagemOperacao?: string;
  erroOperacao?: string;
  operacaoEmAndamento = false;

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

  selecionarAba(aba: 'DESCARGA' | 'CARGA'): void {
    this.abaAtiva = aba;
    this.mensagemOperacao = undefined;
    this.erroOperacao = undefined;
  }

  obterItensAba(aba: 'DESCARGA' | 'CARGA'): OperacaoConteinerVisita[] {
    if (!this.visita) {
      return [];
    }
    return aba === 'DESCARGA' ? this.visita.listaDescarga : this.visita.listaCarga;
  }

  descricaoStatus(status: StatusOperacaoConteinerVisita): string {
    return status === 'CONCLUIDO' ? 'Concluído' : 'Pendente';
  }

  estaConcluido(status: StatusOperacaoConteinerVisita): boolean {
    return status === 'CONCLUIDO';
  }

  marcarComoConcluido(aba: 'DESCARGA' | 'CARGA', codigoConteiner: string): void {
    if (!this.visita || this.operacaoEmAndamento) {
      return;
    }

    this.operacaoEmAndamento = true;
    this.erroOperacao = undefined;
    this.mensagemOperacao = undefined;

    const comando = aba === 'DESCARGA'
      ? this.servicoFerrovia.atualizarStatusDescarga(this.visita.id, codigoConteiner, { statusOperacao: 'CONCLUIDO' })
      : this.servicoFerrovia.atualizarStatusCarga(this.visita.id, codigoConteiner, { statusOperacao: 'CONCLUIDO' });

    comando
      .pipe(finalize(() => this.operacaoEmAndamento = false))
      .subscribe({
        next: (visitaAtualizada) => {
          this.visita = visitaAtualizada;
          this.mensagemOperacao = 'Status atualizado com sucesso.';
        },
        error: () => {
          this.erroOperacao = 'Não foi possível atualizar o status do contêiner.';
        }
      });
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
    this.mensagemOperacao = undefined;
    this.erroOperacao = undefined;

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
