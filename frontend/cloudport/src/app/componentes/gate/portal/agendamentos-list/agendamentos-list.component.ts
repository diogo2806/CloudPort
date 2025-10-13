import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Agendamento } from '../../../model/gate/agendamento.model';

interface AgendamentoRow {
  __id: number;
  Código: string;
  Operação: string;
  Status: string;
  Transportadora: string;
  Janela: string;
  'Chegada Prevista': string;
  'Saída Prevista': string;
}

@Component({
  selector: 'app-agendamentos-list',
  templateUrl: './agendamentos-list.component.html',
  styleUrls: ['./agendamentos-list.component.css']
})
export class AgendamentosListComponent implements OnChanges {
  @Input() agendamentos: Agendamento[] | null = [];
  @Input() loading = false;
  @Input() selectedId: number | null = null;

  @Output() selecionar = new EventEmitter<number>();
  @Output() editar = new EventEmitter<number>();
  @Output() cancelar = new EventEmitter<number>();
  @Output() criar = new EventEmitter<void>();
  @Output() atualizar = new EventEmitter<void>();

  readonly colunas = ['Código', 'Operação', 'Status', 'Transportadora', 'Janela', 'Chegada Prevista', 'Saída Prevista'];

  dadosTabela: AgendamentoRow[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['agendamentos']) {
      this.dadosTabela = (this.agendamentos ?? []).map((item) => this.mapearParaLinha(item));
    }
  }

  onRowClicked(evento: any): void {
    const idSelecionado = evento?.data?.__id as number | undefined;
    if (typeof idSelecionado !== 'number') {
      return;
    }
    this.selectedId = idSelecionado;
    this.selecionar.emit(idSelecionado);
  }

  onRowDoubleClicked(evento: any): void {
    const idSelecionado = evento?.data?.__id as number | undefined;
    if (typeof idSelecionado !== 'number') {
      return;
    }
    this.editar.emit(idSelecionado);
  }

  novoAgendamento(): void {
    this.criar.emit();
  }

  editarAgendamento(): void {
    if (this.selectedId === null) {
      return;
    }
    this.editar.emit(this.selectedId);
  }

  cancelarAgendamento(): void {
    if (this.selectedId === null) {
      return;
    }
    this.cancelar.emit(this.selectedId);
  }

  atualizarLista(): void {
    this.atualizar.emit();
  }

  private mapearParaLinha(agendamento: Agendamento): AgendamentoRow {
    return {
      __id: agendamento.id,
      Código: agendamento.codigo,
      Operação: agendamento.tipoOperacaoDescricao || agendamento.tipoOperacao,
      Status: agendamento.statusDescricao || agendamento.status,
      Transportadora: agendamento.transportadoraNome || '—',
      Janela: this.formatarJanela(agendamento),
      'Chegada Prevista': this.formatarHora(agendamento.horarioPrevistoChegada),
      'Saída Prevista': this.formatarHora(agendamento.horarioPrevistoSaida)
    };
  }

  private formatarJanela(agendamento: Agendamento): string {
    if (!agendamento.dataJanela) {
      return '—';
    }
    const inicio = this.formatarHora(agendamento.horaInicioJanela);
    const fim = this.formatarHora(agendamento.horaFimJanela);
    const data = new Date(agendamento.dataJanela);
    const dataFormatada = data.toLocaleDateString('pt-BR');
    return `${dataFormatada} ${inicio} - ${fim}`.trim();
  }

  private formatarHora(valor: string | null): string {
    return valor ? valor.substring(0, 5) : '—';
  }
}

