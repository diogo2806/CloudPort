import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { GateAgendamentosComponent } from './agendamentos/gate-agendamentos.component';
import { GateJanelasComponent } from './janelas/gate-janelas.component';
import { GateDashboardComponent } from './dashboard/gate-dashboard.component';

const routes: Routes = [
  { path: '', redirectTo: 'agendamentos', pathMatch: 'full' },
  { path: 'agendamentos', component: GateAgendamentosComponent },
  { path: 'janelas', component: GateJanelasComponent },
  { path: 'dashboard', component: GateDashboardComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GateRoutingModule { }
