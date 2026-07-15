import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthSessionService } from './auth-session.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private readonly authSession: AuthSessionService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authSession.obterToken();
    const recursoPublico = request.url.includes('assets/configuracao.json') || request.url.endsWith('/auth/login');
    if (!token || recursoPublico) {
      return next.handle(request);
    }
    return next.handle(request.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    }));
  }
}
