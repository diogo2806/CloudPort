import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './servico-autenticacao/login/login.component';
import { RouterModule, Routes } from '@angular/router';
import { NgxMaskModule, IConfig } from 'ngx-mask'
import { FormsModule } from '@angular/forms';
import { SolicitarAcessoComponent } from './servico-autenticacao/solicitar-acesso/solicitar-acesso.component'; // Nova importação

// Defina suas rotas
const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'solicitar-acesso', component: SolicitarAcessoComponent },
  { path: '', redirectTo: '/login', pathMatch: 'full' } // redireciona para a rota 'login' por padrão
];

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    SolicitarAcessoComponent
  ],
  imports: [

    NgxMaskModule.forRoot(),
    BrowserModule,
    AppRoutingModule,
    FormsModule,  // Adicione FormsModule aqui
    RouterModule.forRoot(routes) // Adicione as rotas aqui
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
