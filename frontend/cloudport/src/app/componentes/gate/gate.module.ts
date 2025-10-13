import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GateRoutingModule } from './gate-routing.module';
import { GateAgendamentosComponent } from './agendamentos/gate-agendamentos.component';
import { GateJanelasComponent } from './janelas/gate-janelas.component';
import { GateDashboardComponent } from './dashboard/gate-dashboard.component';
import { AgendamentosListComponent } from './portal/agendamentos-list/agendamentos-list.component';
import { AgendamentoFormComponent } from './portal/agendamento-form/agendamento-form.component';
import { AgendamentoDetalheComponent } from './portal/agendamento-detalhe/agendamento-detalhe.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DynamicTableModule } from '../../dynamic-table/dynamic-table.module';

@NgModule({
  declarations: [
    GateAgendamentosComponent,
    GateJanelasComponent,
    GateDashboardComponent,
    AgendamentosListComponent,
    AgendamentoFormComponent,
    AgendamentoDetalheComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DynamicTableModule,
    GateRoutingModule
  ]
})
export class GateModule { }
