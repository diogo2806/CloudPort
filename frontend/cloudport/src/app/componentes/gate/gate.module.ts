import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GateRoutingModule } from './gate-routing.module';
import { GateAgendamentosComponent } from './agendamentos/gate-agendamentos.component';
import { GateJanelasComponent } from './janelas/gate-janelas.component';
import { GateDashboardComponent } from './analytics/gate-dashboard/gate-dashboard.component';
import { GateRelatoriosComponent } from './analytics/gate-relatorios/gate-relatorios.component';
import { AgendamentosListComponent } from './portal/agendamentos-list/agendamentos-list.component';
import { AgendamentoFormComponent } from './portal/agendamento-form/agendamento-form.component';
import { AgendamentoDetalheComponent } from './portal/agendamento-detalhe/agendamento-detalhe.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DynamicTableModule } from '../../dynamic-table/dynamic-table.module';
import { NgChartsModule } from 'ng2-charts';
import { GateOperadorConsoleComponent } from './operador/gate-operador-console/gate-operador-console.component';
import { GateOperadorEventosComponent } from './operador/gate-operador-eventos/gate-operador-eventos.component';

@NgModule({
  declarations: [
    GateAgendamentosComponent,
    GateJanelasComponent,
    GateDashboardComponent,
    GateRelatoriosComponent,
    AgendamentosListComponent,
    AgendamentoFormComponent,
    AgendamentoDetalheComponent,
    GateOperadorConsoleComponent,
    GateOperadorEventosComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DynamicTableModule,
    NgChartsModule,
    GateRoutingModule
  ]
})
export class GateModule { }
