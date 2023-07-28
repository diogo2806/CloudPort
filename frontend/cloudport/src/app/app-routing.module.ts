import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './componentes/login/login.component';
import { HomeComponent } from './componentes/home/home.component';
import { RoleComponent } from './componentes/role/role.component';


const routes: Routes = [
  { path: 'login', component: LoginComponent }, 
  { path: 'home', component: HomeComponent },
  { path: '', component: HomeComponent },
  { path: 'role', component: RoleComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
