import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MapaPatioComponent } from './mapa-patio/mapa-patio.component';

const routes: Routes = [
  {
    path: 'mapa',
    component: MapaPatioComponent
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
