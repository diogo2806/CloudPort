import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { GateAgendamentosComponent } from './agendamentos/gate-agendamentos.component';
import { GateJanelasComponent } from './janelas/gate-janelas.component';
import { GateDashboardComponent } from './analytics/gate-dashboard/gate-dashboard.component';
import { GateRelatoriosComponent } from './analytics/gate-relatorios/gate-relatorios.component';
import { GateOperadorConsoleComponent } from './operador/gate-operador-console/gate-operador-console.component';
import { GateOperadorEventosComponent } from './operador/gate-operador-eventos/gate-operador-eventos.component';

const routes: Routes = [
  { path: '', redirectTo: 'agendamentos', pathMatch: 'full' },
  { path: 'agendamentos', component: GateAgendamentosComponent },
  { path: 'janelas', component: GateJanelasComponent },
  { path: 'dashboard', redirectTo: 'agendamentos', pathMatch: 'full' },
  { path: 'relatorios', component: GateRelatoriosComponent },
  {
    path: 'operador',
    children: [
      { path: '', redirectTo: 'console', pathMatch: 'full' },
      { path: 'console', component: GateOperadorConsoleComponent },
      { path: 'eventos', component: GateOperadorEventosComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GateRoutingModule { }
