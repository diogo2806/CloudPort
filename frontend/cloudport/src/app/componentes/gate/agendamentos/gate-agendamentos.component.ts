import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { GateApiService } from '../../service/servico-gate/gate-api.service';
import {
  CentralAcaoAgendamentoResposta,
  VisaoCompletaAgendamento
} from '../../model/gate/agendamento.model';

@Component({
  selector: 'app-gate-agendamentos',
  templateUrl: './gate-agendamentos.component.html',
  styleUrls: ['./gate-agendamentos.component.css']
})
export class GateAgendamentosComponent implements OnInit, OnDestroy {
  readonly titulo = 'Central de Ação do Gate';

  carregando = false;
  erroCarregamento: string | null = null;
  mensagemSucesso: string | null = null;
  resposta: CentralAcaoAgendamentoResposta | null = null;
  acaoEmExecucao: number | null = null;

  private readonly destruir$ = new Subject<void>();

  constructor(private readonly gateApi: GateApiService) {}

  ngOnInit(): void {
    this.carregarVisao();
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }

  carregarVisao(): void {
    if (this.carregando) {
      return;
    }
    this.carregando = true;
    this.erroCarregamento = null;
    this.mensagemSucesso = null;
    this.gateApi
      .obterCentralAcaoAgendamentos()
      .pipe(
        finalize(() => (this.carregando = false)),
        takeUntil(this.destruir$)
      )
      .subscribe({
        next: (resposta) => {
          this.resposta = {
            ...resposta,
            agendamentos: resposta?.agendamentos ?? []
          };
        },
        error: () => {
          this.erroCarregamento = 'Não foi possível carregar seus agendamentos. Tente novamente em instantes.';
          this.resposta = null;
        }
      });
  }

  executarAcao(card: VisaoCompletaAgendamento): void {
    const acao = card.acaoPrincipal;
    if (!acao || !acao.habilitada) {
      return;
    }
    this.acaoEmExecucao = card.agendamentoId;
    this.erroCarregamento = null;
    this.mensagemSucesso = null;
    this.gateApi
      .executarAcaoCentral(acao)
      .pipe(
        finalize(() => (this.acaoEmExecucao = null)),
        takeUntil(this.destruir$)
      )
      .subscribe({
        next: () => {
          this.mensagemSucesso = 'Ação enviada com sucesso.';
          this.carregarVisao();
        },
        error: () => {
          this.erroCarregamento = 'Não foi possível concluir a ação. Confirme sua conexão e tente novamente.';
        }
      });
  }

  formatarJanela(card: VisaoCompletaAgendamento): string {
    if (!card.janelaData) {
      return 'Janela não definida';
    }
    const data = new Date(card.janelaData);
    const dataFormatada = Number.isNaN(data.getTime()) ? card.janelaData : data.toLocaleDateString('pt-BR');
    const inicio = this.formatarHorario(card.janelaHoraInicio);
    const fim = this.formatarHorario(card.janelaHoraFim);
    return `${dataFormatada} ${inicio} - ${fim}`.trim();
  }

  formatarHorario(valor: string | null): string {
    if (!valor) {
      return '—';
    }
    const data = new Date(valor);
    if (!Number.isNaN(data.getTime())) {
      return data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    }
    if (valor.includes('T')) {
      const [, horario] = valor.split('T');
      if (horario && horario.length >= 5) {
        return horario.substring(0, 5);
      }
    }
    return valor.length > 5 ? valor.substring(0, 5) : valor;
  }

  trackPorId(_: number, card: VisaoCompletaAgendamento): number {
    return card.agendamentoId;
  }
}
