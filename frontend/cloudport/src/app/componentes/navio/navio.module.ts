import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavioRoutingModule } from './navio-routing.module';
import { ListaEscalasComponent } from './componentes/lista-escalas/lista-escalas.component';
import { FormularioEscalaComponent } from './componentes/formulario-escala/formulario-escala.component';
import { DetalheEscalaComponent } from './componentes/detalhe-escala/detalhe-escala.component';
import { ListaNaviosComponent } from './componentes/lista-navios/lista-navios.component';

@NgModule({
  declarations: [
    ListaEscalasComponent,
    FormularioEscalaComponent,
    DetalheEscalaComponent,
    ListaNaviosComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    NavioRoutingModule
  ]
})
export class NavioModule { }
