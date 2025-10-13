import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ChartConfiguration, ChartOptions, ChartType } from 'chart.js';
import { Subject, EMPTY, Observable, of } from 'rxjs';
import { catchError, debounceTime, map, shareReplay, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { DashboardFiltro, DashboardResumo } from '../../../model/gate/dashboard.model';
import { GateEnumOption } from '../../../model/gate/agendamento.model';
import { GateDashboardService } from '../../../service/servico-gate/gate-dashboard.service';
import {
  GateAnalyticsPreferences,
  GateChartType,
  carregarPreferencias,
  salvarPreferencias
} from '../analytics-preferences';

@Component({
  selector: 'app-gate-dashboard',
  templateUrl: './gate-dashboard.component.html',
  styleUrls: ['./gate-dashboard.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GateDashboardComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  readonly filtrosForm = this.fb.nonNullable.group({
    inicio: '' as string,
    fim: '' as string,
    transportadoraId: null as number | null,
    tipoOperacao: '' as string,
    tipoGrafico: 'bar' as GateChartType
  });

  readonly tiposOperacao$: Observable<GateEnumOption[]> = this.dashboardService.listarTiposOperacao().pipe(
    catchError(() => of([])),
    shareReplay({ refCount: true, bufferSize: 1 })
  );

  readonly transportadoras$: Observable<GateEnumOption[]> = this.dashboardService.listarTransportadoras().pipe(
    catchError(() => of([])),
    shareReplay({ refCount: true, bufferSize: 1 })
  );

  readonly resumo$ = this.filtrosForm.valueChanges.pipe(
    startWith(this.filtrosForm.value),
    debounceTime(250),
    map(formValue => this.montarFiltro(formValue)),
    tap(filtro => this.persistirPreferencias(filtro)),
    tap(() => {
      this.carregando = true;
      this.erro = undefined;
    }),
    switchMap(filtro =>
      this.dashboardService.consultarResumo(filtro).pipe(
        tap(resumo => {
          this.resumo = resumo;
          this.atualizarGraficos(resumo, this.chartType);
          this.carregando = false;
          this.cdr.markForCheck();
        }),
        catchError(() => {
          this.erro = 'Não foi possível carregar os indicadores do período selecionado.';
          this.carregando = false;
          this.cdr.markForCheck();
          return EMPTY;
        })
      )
    ),
    takeUntil(this.destroy$),
    shareReplay({ refCount: false, bufferSize: 1 })
  );

  carregando = false;
  erro?: string;
  resumo?: DashboardResumo;

  chartType: ChartType = 'bar';

  ocupacaoChart: ChartConfiguration['data'] = { labels: [], datasets: [] };
  ocupacaoOptions: ChartOptions = this.criarOpcoesPadrao('Ocupação de slots por hora');

  turnaroundChart: ChartConfiguration['data'] = { labels: [], datasets: [] };
  turnaroundOptions: ChartOptions = this.criarOpcoesPadrao('Tempo médio de permanência (min)');

  pontualidadeChart: ChartConfiguration['data'] = { labels: ['Pontualidade', 'No-show', 'Ocupação'], datasets: [] };
  pontualidadeOptions: ChartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: this.obterCorTexto(),
          font: { size: 12 }
        }
      }
    }
  };

  constructor(
    private readonly fb: FormBuilder,
    private readonly dashboardService: GateDashboardService,
    private readonly cdr: ChangeDetectorRef
  ) {
    const preferencias = carregarPreferencias();
    if (preferencias) {
      this.reporPreferencias(preferencias);
    }

    this.filtrosForm
      .get('tipoGrafico')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe(tipo => {
        this.chartType = tipo ?? 'bar';
        if (this.resumo) {
          this.atualizarGraficos(this.resumo, this.chartType);
        }
        this.cdr.markForCheck();
      });
  }

  ngOnInit(): void {
    this.resumo$.subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  limparFiltros(): void {
    this.filtrosForm.patchValue({
      inicio: '',
      fim: '',
      transportadoraId: null,
      tipoOperacao: ''
    });
  }

  private montarFiltro(formValue: GateAnalyticsPreferences): DashboardFiltro {
    return {
      inicio: formValue.inicio || undefined,
      fim: formValue.fim || undefined,
      tipoOperacao: formValue.tipoOperacao || undefined,
      transportadoraId: formValue.transportadoraId ?? undefined
    };
  }

  private persistirPreferencias(filtro: DashboardFiltro): void {
    const preferencias: GateAnalyticsPreferences = {
      inicio: filtro.inicio ?? null,
      fim: filtro.fim ?? null,
      tipoOperacao: filtro.tipoOperacao ?? null,
      transportadoraId: filtro.transportadoraId ?? null,
      tipoGrafico: (this.filtrosForm.get('tipoGrafico')?.value as GateChartType) ?? 'bar'
    };
    salvarPreferencias(preferencias);
  }

  private reporPreferencias(preferencias: GateAnalyticsPreferences): void {
    this.chartType = preferencias.tipoGrafico ?? 'bar';
    this.filtrosForm.patchValue(
      {
        inicio: preferencias.inicio ?? '',
        fim: preferencias.fim ?? '',
        transportadoraId: preferencias.transportadoraId ?? null,
        tipoOperacao: preferencias.tipoOperacao ?? '',
        tipoGrafico: this.chartType as GateChartType
      },
      { emitEvent: false }
    );
  }

  private atualizarGraficos(resumo: DashboardResumo, tipo: ChartType): void {
    const texto = this.obterCorTexto();

    this.ocupacaoChart = {
      labels: resumo.ocupacaoPorHora.map(item => item.horaInicio),
      datasets: [
        {
          type: tipo,
          label: 'Agendamentos',
          data: resumo.ocupacaoPorHora.map(item => item.totalAgendamentos),
          backgroundColor: 'rgba(37, 99, 235, 0.6)',
          borderColor: 'rgba(37, 99, 235, 1)',
          borderWidth: 1,
          pointRadius: 4,
          pointHoverRadius: 6
        },
        {
          type: tipo,
          label: 'Capacidade',
          data: resumo.ocupacaoPorHora.map(item => item.capacidadeSlot),
          backgroundColor: 'rgba(16, 185, 129, 0.4)',
          borderColor: 'rgba(16, 185, 129, 1)',
          borderWidth: 1,
          pointRadius: 4,
          pointHoverRadius: 6
        }
      ]
    };

    this.turnaroundChart = {
      labels: resumo.turnaroundPorDia.map(item => item.dia),
      datasets: [
        {
          type: tipo,
          label: 'Tempo médio (minutos)',
          data: resumo.turnaroundPorDia.map(item => item.tempoMedioMinutos ?? 0),
          backgroundColor: 'rgba(249, 115, 22, 0.4)',
          borderColor: 'rgba(234, 88, 12, 1)',
          borderWidth: 2,
          tension: 0.3,
          pointRadius: 4,
          pointHoverRadius: 6
        }
      ]
    };

    this.pontualidadeChart = {
      labels: ['Pontualidade', 'No-show', 'Ocupação'],
      datasets: [
        {
          data: [
            resumo.percentualPontualidade,
            resumo.percentualNoShow,
            resumo.percentualOcupacaoSlots
          ],
          backgroundColor: [
            'rgba(16, 185, 129, 0.7)',
            'rgba(239, 68, 68, 0.7)',
            'rgba(59, 130, 246, 0.7)'
          ],
          borderColor: [
            'rgba(16, 185, 129, 1)',
            'rgba(239, 68, 68, 1)',
            'rgba(59, 130, 246, 1)'
          ],
          hoverOffset: 8
        }
      ]
    };

    this.ocupacaoOptions = this.criarOpcoesPadrao('Ocupação de slots por hora', texto);
    this.turnaroundOptions = this.criarOpcoesPadrao('Tempo médio de permanência (min)', texto);
    this.pontualidadeOptions = {
      responsive: true,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: texto,
            font: { size: 12 }
          }
        },
        tooltip: {
          callbacks: {
            label: context => `${context.label}: ${context.parsed.toFixed(1)}%`
          }
        }
      }
    };
  }

  private criarOpcoesPadrao(titulo: string, textoColor?: string): ChartOptions {
    const color = textoColor ?? this.obterCorTexto();
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          ticks: { color },
          grid: { color: 'rgba(148, 163, 184, 0.2)' }
        },
        y: {
          ticks: { color },
          grid: { color: 'rgba(148, 163, 184, 0.2)' }
        }
      },
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color,
            font: { size: 12 }
          }
        },
        title: {
          display: true,
          text: titulo,
          color,
          font: { size: 16, weight: '600' }
        },
        tooltip: {
          callbacks: {
            label: context => `${context.dataset.label}: ${context.parsed.y ?? context.parsed}`
          }
        }
      }
    };
  }

  private obterCorTexto(): string {
    if (typeof window === 'undefined') {
      return '#1f2937';
    }
    const estilo = getComputedStyle(document.documentElement);
    return (
      estilo.getPropertyValue('--cp-on-surface-color')?.trim() ||
      estilo.getPropertyValue('--on-surface')?.trim() ||
      estilo.getPropertyValue('--text-color')?.trim() ||
      '#1f2937'
    );
  }
}
