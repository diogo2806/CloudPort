import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './componentes/login/login.component';
import { HomeComponent } from './componentes/home/home.component';
import { RoleTabelaComponent } from './componentes/role/role-tabela/role-tabela.component';
import { AuthGuard } from './componentes/service/servico-autenticacao/auth.guard';
import { SegurancaComponent } from './componentes/seguranca/seguranca.component';
import { NotificacoesComponent } from './componentes/notificacoes/notificacoes.component';
import { PrivacidadeComponent } from './componentes/privacidade/privacidade.component';
import { UsuariosListaComponent } from './componentes/usuarios-lista/usuarios-lista.component';

const homeChildRoutes: Routes = [
  { path: '', redirectTo: 'role', pathMatch: 'full' },
  { path: 'role', component: RoleTabelaComponent },
  { path: 'seguranca', component: SegurancaComponent },
  { path: 'notificacoes', component: NotificacoesComponent },
  { path: 'privacidade', component: PrivacidadeComponent },
  { path: 'lista-de-usuarios', component: UsuariosListaComponent }
];

const homeChildRoutes: Routes = [
  { path: '', redirectTo: 'role', pathMatch: 'full' },
  { path: 'role', component: RoleTabelaComponent },
];

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [AuthGuard],
    children: homeChildRoutes
  },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
