import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './componentes/login/login.component';
import { HomeComponent } from './componentes/home/home.component';
import { RoleComponent } from './componentes/role/role.component';
import { AuthGuard } from './componentes/service/servico-autenticacao/auth.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'home', component: HomeComponent, canActivate: [AuthGuard], children: [
    { path: 'role', component: RoleComponent },
    { path: 'home', component: HomeComponent },
    { path: 'login', component: LoginComponent }, 
  ]},
  { path: '', redirectTo: 'home', pathMatch: 'full' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
