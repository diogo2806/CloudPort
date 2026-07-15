import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { AppComponent } from './app.component';
import { AuthGateComponent } from './auth-gate.component';
import { JwtInterceptor } from './jwt.interceptor';

@NgModule({
  declarations: [AppComponent, AuthGateComponent],
  imports: [BrowserModule, HttpClientModule, FormsModule],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AuthGateComponent]
})
export class AppModule {}
