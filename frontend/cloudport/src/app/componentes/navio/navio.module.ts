import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavioRoutingModule } from './navio-routing.module';
import { ListaVisitasComponent } from './lista-visitas/lista-visitas.component';
import { DetalheVisitaComponent } from './detalhe-visita/detalhe-visita.component';
import { PainelAtracacaoComponent } from './painel-atracacao/painel-atracacao.component';
import { ListaBercosComponent } from './lista-bercos/lista-bercos.component';

@NgModule({
  declarations: [
    ListaVisitasComponent,
    DetalheVisitaComponent,
    PainelAtracacaoComponent,
    ListaBercosComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    NavioRoutingModule
  ]
})
export class NavioModule {}
