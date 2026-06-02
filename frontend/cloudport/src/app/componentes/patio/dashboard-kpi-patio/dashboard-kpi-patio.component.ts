import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { ServicoKpiPatio } from '../../service/servico-kpi-patio/servico-kpi-patio.service';

interface Kpi {
  yardDensity: number;
  rehandleRatio: number;
  equipmentUtilization: number;
  gateThroghput: number;
  atualizadoEm: string;
  statusYardDensity: string;
  statusRehandleRatio: string;
  statusEquipmentUtilization: string;
  statusGateThroghput: string;
}

@Component({
  selector: 'app-dashboard-kpi-patio',
  templateUrl: './dashboard-kpi-patio.component.html',
  styleUrls: ['./dashboard-kpi-patio.component.css'],
  standalone: false
})
export class DashboardKpiPatioComponent implements OnInit, OnDestroy {
  kpis: Kpi | null = null;
  carregando = false;
  erro?: string;
  inscricaoAtualizacao?: Subscription;
  intervaloAtualizacao = 30000;
  Math = Math;

  constructor(private readonly servicoKpi: ServicoKpiPatio) {}

  ngOnInit(): void {
    this.carregarKpis();
    this.inscricaoAtualizacao = interval(this.intervaloAtualizacao)
      .pipe(
        startWith(0),
        switchMap(() => this.servicoKpi.calcularKpis())
      )
      .subscribe({
        next: (kpis) => {
          this.kpis = kpis;
          this.carregando = false;
        },
        error: (erro) => {
          this.erro = `Erro ao carregar KPIs: ${erro?.error?.message || 'falha desconhecida'}`;
          this.carregando = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.inscricaoAtualizacao?.unsubscribe();
  }

  carregarKpis(): void {
    this.carregando = true;
    this.servicoKpi.calcularKpis().subscribe({
      next: (kpis) => {
        this.kpis = kpis;
        this.carregando = false;
      },
      error: (erro) => {
        this.erro = `Erro ao carregar KPIs: ${erro?.error?.message || 'falha desconhecida'}`;
        this.carregando = false;
      }
    });
  }

  obterCorStatus(status: string): string {
    switch (status) {
      case 'CRITICO':
        return '#dc2626';
      case 'ATENCAO':
        return '#f59e0b';
      case 'OK':
        return '#10b981';
      default:
        return '#6b7280';
    }
  }

  obterIconeStatus(status: string): string {
    switch (status) {
      case 'CRITICO':
        return '🔴';
      case 'ATENCAO':
        return '🟠';
      case 'OK':
        return '🟢';
      default:
        return '⚪';
    }
  }

  obterDescricaoStatus(status: string): string {
    switch (status) {
      case 'CRITICO':
        return 'Crítico - Ação Imediata';
      case 'ATENCAO':
        return 'Atenção - Monitorar';
      case 'OK':
        return 'OK - Operacional';
      default:
        return 'Desconhecido';
    }
  }

  recarregar(): void {
    this.carregarKpis();
  }
}
