import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ListaVisitasTremComponent } from './componentes/lista-visitas-trem/lista-visitas-trem.component';
import { DetalheVisitaTremComponent } from './componentes/detalhe-visita-trem/detalhe-visita-trem.component';
import { ListaTrabalhoTremComponent } from './componentes/lista-trabalho-trem/lista-trabalho-trem.component';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'visitas'
  },
  {
    path: 'visitas',
    component: ListaVisitasTremComponent
  },
  {
    path: 'visitas/:id/lista-trabalho',
    component: ListaTrabalhoTremComponent
  },
  {
    path: 'visitas/:id',
    component: DetalheVisitaTremComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FerroviaRoutingModule { }
