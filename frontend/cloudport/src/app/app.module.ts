/* app.module.ts */
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './componentes/login/login.component';
import { TranslateModule } from '@ngx-translate/core';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HomeComponent } from './componentes/home/home.component';
import { HeaderComponent } from './componentes/header/header.component';
import { FooterComponent } from './componentes/footer/footer.component';
import { NavbarComponent } from './componentes/navbar/navbar.component';
import { RoleComponent } from './componentes/role/role.component';
import { ContextMenuComponent } from './componentes/context-menu/context-menu.component';
import { DynamicTableComponent } from './componentes/dynamic-table/dynamic-table.component';
import { AgGridModule } from 'ag-grid-angular';
import { CustomReuseStrategy } from './componentes/tab-content/customreusestrategy';
import { RouteReuseStrategy } from '@angular/router';
import { ModalComponent } from './componentes/modal/modal.component';
import { RoleCadastroComponent } from './componentes/role-cadastro/role-cadastro.component';



@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HomeComponent,
    HeaderComponent,
    FooterComponent,
    NavbarComponent,
    RoleComponent,
    ContextMenuComponent,
    DynamicTableComponent,
    ModalComponent,
    RoleCadastroComponent,
  

  ],
  imports: [
    BrowserModule,
    AppRoutingModule, 
    BrowserAnimationsModule,
    ReactiveFormsModule,
    FormsModule,
    HttpClientModule,
    TranslateModule.forRoot(),
    AgGridModule
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: CustomReuseStrategy }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
