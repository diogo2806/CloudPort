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

    const correlationId = this.criarCorrelationId();
    const usuario = this.authSession.obterNomeUsuario();
    const body = this.enriquecerComando(request, usuario, correlationId);
    return next.handle(request.clone({
      body,
      setHeaders: {
        Authorization: `Bearer ${token}`,
        'X-Correlation-Id': correlationId
      }
    }));
  }

  private enriquecerComando(request: HttpRequest<unknown>, usuario: string, correlationId: string): unknown {
    const metodoComComando = ['POST', 'PUT', 'PATCH'].includes(request.method.toUpperCase());
    const rotaOperacional = request.url.includes('/visitas-navio/') || request.url.includes('/yard/patio/');
    const body = request.body;
    if (!metodoComComando || !rotaOperacional || !body || Array.isArray(body) || typeof body !== 'object') {
      return body;
    }
    const comando = { ...(body as Record<string, unknown>) };
    comando['usuario'] ??= usuario;
    comando['origemAcao'] ??= 'CONTROL_ROOM_NAVIO_PATIO';
    comando['correlationId'] ??= correlationId;
    if (request.url.endsWith('/dispatch')) {
      comando['operador'] = usuario;
    }
    return comando;
  }

  private criarCorrelationId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `cr-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
}
