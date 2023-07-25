import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './componentes/login/login.component';
import { HomeComponent } from './componentes/home/home.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent }, // Adicione esta linha
  { path: 'home', component: HomeComponent }, // Adicione esta linha
  { path: '', component: HomeComponent } // Adicione esta linha
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
