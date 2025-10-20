import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FerroviaRoutingModule } from './ferrovia-routing.module';
import { ListaVisitasTremComponent } from './componentes/lista-visitas-trem/lista-visitas-trem.component';
import { DetalheVisitaTremComponent } from './componentes/detalhe-visita-trem/detalhe-visita-trem.component';

@NgModule({
  declarations: [
    ListaVisitasTremComponent,
    DetalheVisitaTremComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    FerroviaRoutingModule
  ]
})
export class FerroviaModule { }
