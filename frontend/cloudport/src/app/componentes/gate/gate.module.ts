import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GateRoutingModule } from './gate-routing.module';
import { GateAgendamentosComponent } from './agendamentos/gate-agendamentos.component';
import { GateJanelasComponent } from './janelas/gate-janelas.component';
import { GateDashboardComponent } from './dashboard/gate-dashboard.component';

@NgModule({
  declarations: [
    GateAgendamentosComponent,
    GateJanelasComponent,
    GateDashboardComponent
  ],
  imports: [
    CommonModule,
    GateRoutingModule
  ]
})
export class GateModule { }
