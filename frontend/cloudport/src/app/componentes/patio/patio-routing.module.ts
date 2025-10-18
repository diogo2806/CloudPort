import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MapaPatioComponent } from './mapa-patio/mapa-patio.component';
import { ListaPosicoesComponent } from './lista-posicoes/lista-posicoes.component';
import { ListaMovimentacoesComponent } from './lista-movimentacoes/lista-movimentacoes.component';
import { FormularioMovimentacaoComponent } from './formulario-movimentacao/formulario-movimentacao.component';

const routes: Routes = [
  {
    path: 'mapa',
    component: MapaPatioComponent
  },
  {
    path: 'posicoes',
    component: ListaPosicoesComponent
  },
  {
    path: 'movimentacoes',
    component: ListaMovimentacoesComponent
  },
  {
    path: 'movimentacao',
    component: FormularioMovimentacaoComponent
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'mapa'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PatioRoutingModule { }
