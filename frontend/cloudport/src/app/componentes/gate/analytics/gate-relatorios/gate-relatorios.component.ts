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
  selector: 'app-gate-relatorios',
  templateUrl: './gate-relatorios.component.html',
  styleUrls: ['./gate-relatorios.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GateRelatoriosComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  readonly filtrosForm = this.fb.nonNullable.group({
    inicio: '' as string,
    fim: '' as string,
    transportadoraId: null as number | null,
    tipoOperacao: '' as string,
    tipoGrafico: 'line' as GateChartType
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
          this.erro = 'Não foi possível carregar os dados para o relatório.';
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
  exportando = false;
  erro?: string;
  mensagem?: string;
  resumo?: DashboardResumo;
  chartType: ChartType = 'line';

  ocupacaoChart: ChartConfiguration['data'] = { labels: [], datasets: [] };
  ocupacaoOptions: ChartOptions = this.criarOpcoesPadrao('Ocupação média dos slots');

  turnaroundChart: ChartConfiguration['data'] = { labels: [], datasets: [] };
  turnaroundOptions: ChartOptions = this.criarOpcoesPadrao('Tempo médio diário (minutos)');

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
        this.chartType = tipo ?? 'line';
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

  exportar(formato: 'csv' | 'xlsx'): void {
    if (this.exportando) {
      return;
    }

    const filtro = this.montarFiltro(this.filtrosForm.value);
    this.exportando = true;
    this.mensagem = undefined;
    this.cdr.markForCheck();

    this.dashboardService.exportarResumo(formato, filtro).pipe(takeUntil(this.destroy$)).subscribe({
      next: blob => {
        const nomeArquivo = this.gerarNomeArquivo(formato);
        this.realizarDownload(blob, nomeArquivo);
        this.exportando = false;
        this.mensagem = `Relatório ${formato.toUpperCase()} exportado com sucesso.`;
        this.cdr.markForCheck();
      },
      error: () => {
        this.exportando = false;
        this.mensagem = 'Não foi possível exportar o relatório. Tente novamente.';
        this.cdr.markForCheck();
      }
    });
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
      tipoGrafico: (this.filtrosForm.get('tipoGrafico')?.value as GateChartType) ?? 'line'
    };
    salvarPreferencias(preferencias);
  }

  private reporPreferencias(preferencias: GateAnalyticsPreferences): void {
    this.chartType = preferencias.tipoGrafico ?? 'line';
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
    this.ocupacaoChart = {
      labels: resumo.ocupacaoPorHora.map(item => item.horaInicio),
      datasets: [
        {
          type: tipo,
          label: 'Agendamentos confirmados',
          data: resumo.ocupacaoPorHora.map(item => item.totalAgendamentos),
          backgroundColor: 'rgba(59, 130, 246, 0.5)',
          borderColor: 'rgba(59, 130, 246, 1)',
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
          label: 'Tempo médio (min)',
          data: resumo.turnaroundPorDia.map(item => item.tempoMedioMinutos ?? 0),
          backgroundColor: 'rgba(16, 185, 129, 0.45)',
          borderColor: 'rgba(16, 185, 129, 1)',
          borderWidth: 2,
          tension: 0.3,
          pointRadius: 4,
          pointHoverRadius: 6
        }
      ]
    };

    this.ocupacaoOptions = this.criarOpcoesPadrao('Ocupação média dos slots');
    this.turnaroundOptions = this.criarOpcoesPadrao('Tempo médio diário (minutos)');
  }

  private criarOpcoesPadrao(titulo: string): ChartOptions {
    const corTexto = this.obterCorTexto();
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          ticks: { color: corTexto },
          grid: { color: 'rgba(148, 163, 184, 0.2)' }
        },
        y: {
          ticks: { color: corTexto },
          grid: { color: 'rgba(148, 163, 184, 0.2)' }
        }
      },
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: corTexto, font: { size: 12 } }
        },
        title: {
          display: true,
          text: titulo,
          color: corTexto,
          font: { size: 16, weight: '600' }
        }
      }
    };
  }

  private gerarNomeArquivo(formato: 'csv' | 'xlsx'): string {
    const data = new Date().toISOString().slice(0, 10);
    return `relatorio-gate-${data}.${formato}`;
  }

  private realizarDownload(blob: Blob, nomeArquivo: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nomeArquivo;
    link.click();
    URL.revokeObjectURL(url);
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
