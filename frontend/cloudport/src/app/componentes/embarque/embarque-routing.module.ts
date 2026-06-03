import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PlanejamentoEmbarqueComponent } from './planejamento-embarque/planejamento-embarque.component';
import { SteelCoilPlannerComponent } from './steel-coil-planner/steel-coil-planner.component';

const routes: Routes = [
  {
    path: 'planejamento',
    component: PlanejamentoEmbarqueComponent
  },
  {
    path: 'steel-coils',
    component: SteelCoilPlannerComponent
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'planejamento'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class EmbarqueRoutingModule { }
