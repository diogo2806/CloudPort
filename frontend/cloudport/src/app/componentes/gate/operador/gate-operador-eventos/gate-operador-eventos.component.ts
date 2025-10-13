import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { combineLatest, map, Observable, startWith, Subject, takeUntil } from 'rxjs';
import { GateOperadorService } from '../../../service/servico-gate/gate-operador.service';
import { GateOperadorEvento } from '../../../model/gate/operador.model';
import { GateEnumOption } from '../../../model/gate/agendamento.model';

@Component({
  selector: 'app-gate-operador-eventos',
  templateUrl: './gate-operador-eventos.component.html',
  styleUrls: ['./gate-operador-eventos.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GateOperadorEventosComponent implements OnInit, OnDestroy {
  readonly statusConexao$ = this.gateOperadorService.statusConexao$;
  readonly niveisEvento$: Observable<GateEnumOption[]> = this.gateOperadorService.listarNiveisEvento().pipe(
    map((niveis) => [{ codigo: 'TODOS', descricao: 'Todos os níveis' }, ...niveis])
  );
  readonly eventos$: Observable<GateOperadorEvento[]> = this.gateOperadorService.eventos$;

  readonly filtros = this.fb.group({
    nivel: ['TODOS'],
    busca: ['']
  });

  readonly eventosFiltrados$: Observable<GateOperadorEvento[]> = combineLatest([
    this.eventos$,
    this.filtros.get('nivel')!.valueChanges.pipe(startWith(this.filtros.get('nivel')!.value ?? 'TODOS')),
    this.filtros.get('busca')!.valueChanges.pipe(startWith(this.filtros.get('busca')!.value ?? ''))
  ]).pipe(
    map(([eventos, nivelSelecionado, busca]) => {
      const buscaNormalizada = (busca ?? '').toString().toLowerCase();
      return eventos.filter((evento) => {
        const nivelMatch = nivelSelecionado === 'TODOS' || (evento.nivel ?? '').toUpperCase() === (nivelSelecionado ?? '').toUpperCase();
        const buscaMatch = !buscaNormalizada ||
          `${evento.tipo} ${evento.descricao} ${evento.placaVeiculo ?? ''} ${evento.transportadora ?? ''}`
            .toLowerCase()
            .includes(buscaNormalizada);
        return nivelMatch && buscaMatch;
      });
    })
  );

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly gateOperadorService: GateOperadorService, private readonly fb: FormBuilder) {}

  ngOnInit(): void {
    this.gateOperadorService.carregarPainel().subscribe({
      error: (erro) => console.warn('Não foi possível atualizar o painel do Gate.', erro)
    });
    this.gateOperadorService.atualizarHistorico().subscribe({
      error: (erro) => console.warn('Não foi possível carregar o histórico do Gate.', erro)
    });
    this.gateOperadorService.conectarEventos();

    this.filtros.valueChanges.pipe(takeUntil(this.destroy$)).subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  classeNivel(nivel: string | null | undefined): string {
    const nivelUpper = (nivel ?? '').toUpperCase();
    if (nivelUpper === 'CRITICA') {
      return 'evento-critico';
    }
    if (nivelUpper === 'ALERTA') {
      return 'evento-alerta';
    }
    return 'evento-normal';
  }
}
