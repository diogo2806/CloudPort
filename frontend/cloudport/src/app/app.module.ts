import { NgModule, NO_ERRORS_SCHEMA  } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { MDBBootstrapModule } from 'angular-bootstrap-md';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './servico-autenticacao/login/login.component';
import { RouterModule, Routes } from '@angular/router';
import { NgxMaskModule, IConfig } from 'ngx-mask'
import { FormsModule } from '@angular/forms';
import { SolicitarAcessoComponent } from './servico-autenticacao/solicitar-acesso/solicitar-acesso.component';
import { CadastroUsuarioComponent } from './servico-autenticacao/cadastro-usuario/cadastro-usuario.component';
import { NotificacaoComponent } from './notificacao/notificacao.component';
import { SolicitarAcessoServiceComponent } from './service/solicitar-acesso-service/solicitar-acesso-service/solicitar-acesso-service.component';
//import { SolicitarAcessoServiceComponent } from './servico-autenticacao/solicitar-acesso/solicitar-acesso-service/solicitar-acesso-service/solicitar-acesso-service.component';

// Defina suas rotas
const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'solicitar-acesso', component: SolicitarAcessoComponent },
  { path: 'cadastro-usuario', component: CadastroUsuarioComponent },
  { path: '', redirectTo: '/login', pathMatch: 'full' } // redireciona para a rota 'login' por padrão
];

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    SolicitarAcessoComponent,
    CadastroUsuarioComponent,
    NotificacaoComponent,
    SolicitarAcessoServiceComponent
  ],
  imports: [
    HttpClientModule,
    MDBBootstrapModule.forRoot(),
    NgxMaskModule.forRoot(),
    BrowserModule,
    AppRoutingModule,
    FormsModule,  // Adicione FormsModule aqui
    RouterModule.forRoot(routes) // Adicione as rotas aqui
  ],
  schemas: [ NO_ERRORS_SCHEMA ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
