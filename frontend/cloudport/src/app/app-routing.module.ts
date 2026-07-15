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
import { NavioControlRoomComponent } from './componentes/navio/navio-control-room.component';

const homeChildRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'role'
  },
  {
    path: 'role',
    component: RoleTabelaComponent
  },
  {
    path: 'seguranca',
    component: SegurancaComponent
  },
  {
    path: 'notificacoes',
    component: NotificacoesComponent
  },
  {
    path: 'privacidade',
    component: PrivacidadeComponent
  },
  {
    path: 'lista-de-usuarios',
    component: UsuariosListaComponent
  },
  {
    path: 'navio',
    pathMatch: 'full',
    redirectTo: 'navio/control-room'
  },
  {
    path: 'navio/control-room',
    component: NavioControlRoomComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'gate',
    canActivate: [AuthGuard],
    canLoad: [AuthGuard],
    loadChildren: () => import('./componentes/gate/gate.module').then(m => m.GateModule)
  },
  {
    path: 'ferrovia',
    canActivate: [AuthGuard],
    canLoad: [AuthGuard],
    loadChildren: () => import('./componentes/ferrovia/ferrovia.module').then(m => m.FerroviaModule)
  },
  {
    path: 'patio',
    canActivate: [AuthGuard],
    canLoad: [AuthGuard],
    loadChildren: () => import('./componentes/patio/patio.module').then(m => m.PatioModule)
  },
  {
    path: 'embarque',
    canActivate: [AuthGuard],
    canLoad: [AuthGuard],
    loadChildren: () => import('./componentes/embarque/embarque.module').then(m => m.EmbarqueModule)
  }
];

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    children: homeChildRoutes
  },
  { path: '', redirectTo: 'home', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
