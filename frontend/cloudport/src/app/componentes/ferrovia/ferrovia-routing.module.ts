import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ListaVisitasTremComponent } from './componentes/lista-visitas-trem/lista-visitas-trem.component';
import { DetalheVisitaTremComponent } from './componentes/detalhe-visita-trem/detalhe-visita-trem.component';

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
    path: 'visitas/:id',
    component: DetalheVisitaTremComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FerroviaRoutingModule { }
