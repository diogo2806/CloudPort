import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ListaEscalasComponent } from './componentes/lista-escalas/lista-escalas.component';
import { FormularioEscalaComponent } from './componentes/formulario-escala/formulario-escala.component';
import { DetalheEscalaComponent } from './componentes/detalhe-escala/detalhe-escala.component';
import { ListaNaviosComponent } from './componentes/lista-navios/lista-navios.component';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'escalas'
  },
  {
    path: 'escalas',
    component: ListaEscalasComponent
  },
  {
    path: 'escalas/nova',
    component: FormularioEscalaComponent
  },
  {
    path: 'escalas/:id',
    component: DetalheEscalaComponent
  },
  {
    path: 'navios',
    component: ListaNaviosComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class NavioRoutingModule { }
