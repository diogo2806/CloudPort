import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MapaPatioComponent } from './mapa-patio/mapa-patio.component';
import { ListaPosicoesComponent } from './lista-posicoes/lista-posicoes.component';
import { ListaMovimentacoesComponent } from './lista-movimentacoes/lista-movimentacoes.component';
import { FormularioMovimentacaoComponent } from './formulario-movimentacao/formulario-movimentacao.component';
import { ListaTrabalhoPatioComponent } from './lista-trabalho-patio/lista-trabalho-patio.component';
import { RecursosPatioComponent } from './recursos-patio/recursos-patio.component';
import { SimuladorPatioComponent } from './simulador-patio/simulador-patio.component';
import { DashboardKpiPatioComponent } from './dashboard-kpi-patio/dashboard-kpi-patio.component';
import { AutomacaoTarefasPatioComponent } from './automacao-tarefas-patio/automacao-tarefas-patio.component';

const routes: Routes = [
  {
    path: 'lista-trabalho',
    component: ListaTrabalhoPatioComponent
  },
  {
    path: 'mapa',
    component: MapaPatioComponent
  },
  {
    path: 'posicoes',
    component: ListaPosicoesComponent
  },
  {
    path: 'movimentacoes',
    component: ListaMovimentacoesComponent
  },
  {
    path: 'movimentacao',
    component: FormularioMovimentacaoComponent
  },
  {
    path: 'recursos',
    component: RecursosPatioComponent
  },
  {
    path: 'simulador',
    component: SimuladorPatioComponent
  },
  {
    path: 'dashboard-kpi',
    component: DashboardKpiPatioComponent
  },
  {
    path: 'automacao',
    component: AutomacaoTarefasPatioComponent
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'recursos'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PatioRoutingModule { }
