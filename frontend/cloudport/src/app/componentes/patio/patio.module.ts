import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MapaPatioComponent } from './mapa-patio/mapa-patio.component';
import { ListaPosicoesComponent } from './lista-posicoes/lista-posicoes.component';
import { ListaMovimentacoesComponent } from './lista-movimentacoes/lista-movimentacoes.component';
import { FormularioMovimentacaoComponent } from './formulario-movimentacao/formulario-movimentacao.component';
import { ListaTrabalhoPatioComponent } from './lista-trabalho-patio/lista-trabalho-patio.component';
import { PatioRoutingModule } from './patio-routing.module';

@NgModule({
  declarations: [
    MapaPatioComponent,
    ListaPosicoesComponent,
    ListaMovimentacoesComponent,
    FormularioMovimentacaoComponent,
    ListaTrabalhoPatioComponent
  ],
  imports: [CommonModule, ReactiveFormsModule, PatioRoutingModule]
})
export class PatioModule { }
