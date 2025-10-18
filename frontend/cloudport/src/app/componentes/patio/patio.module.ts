import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MapaPatioComponent } from './mapa-patio/mapa-patio.component';
import { ListaPosicoesComponent } from './lista-posicoes/lista-posicoes.component';
import { ListaMovimentacoesComponent } from './lista-movimentacoes/lista-movimentacoes.component';
import { FormularioMovimentacaoComponent } from './formulario-movimentacao/formulario-movimentacao.component';
import { PatioRoutingModule } from './patio-routing.module';

@NgModule({
  declarations: [
    MapaPatioComponent,
    ListaPosicoesComponent,
    ListaMovimentacoesComponent,
    FormularioMovimentacaoComponent
  ],
  imports: [CommonModule, ReactiveFormsModule, PatioRoutingModule]
})
export class PatioModule { }
