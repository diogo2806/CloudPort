import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ListaVisitasComponent } from './lista-visitas/lista-visitas.component';
import { DetalheVisitaComponent } from './detalhe-visita/detalhe-visita.component';
import { PainelAtracacaoComponent } from './painel-atracacao/painel-atracacao.component';
import { ListaBercosComponent } from './lista-bercos/lista-bercos.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'visitas' },
  { path: 'visitas', component: ListaVisitasComponent },
  { path: 'visitas/:id', component: DetalheVisitaComponent },
  { path: 'painel', component: PainelAtracacaoComponent },
  { path: 'bercos', component: ListaBercosComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class NavioRoutingModule {}
