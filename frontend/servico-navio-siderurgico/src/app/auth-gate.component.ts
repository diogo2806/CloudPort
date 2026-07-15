import { Component, HostListener, OnInit } from '@angular/core';
import { AuthApiService } from './auth-api.service';
import { AuthSessionService } from './auth-session.service';

interface MensagemSessaoPortal {
  type: 'CLOUDPORT_AUTH_SESSION';
  session: {
    token: string;
    nome?: string;
    roles?: string[];
  };
}

@Component({
  selector: 'app-auth-gate',
  standalone: false,
  template: `
    <section class="auth-shell" *ngIf="!autenticado; else controlRoom">
      <form class="auth-card" (ngSubmit)="entrar()">
        <div class="auth-brand">CloudPort</div>
        <h1>Control Room Navio + Pátio</h1>
        <p>Entre com uma conta operacional autorizada.</p>
        <label>Login
          <input name="login" autocomplete="username" [(ngModel)]="login" required>
        </label>
        <label>Senha
          <input name="senha" type="password" autocomplete="current-password" [(ngModel)]="senha" required>
        </label>
        <p class="auth-error" *ngIf="erro">{{ erro }}</p>
        <button type="submit" [disabled]="carregando || !login || !senha">
          {{ carregando ? 'Autenticando...' : 'Entrar' }}
        </button>
      </form>
    </section>
    <ng-template #controlRoom>
      <button class="logout-button" type="button" (click)="sair()">
        Sair · {{ nomeUsuario }}
      </button>
      <app-root></app-root>
    </ng-template>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; }
    .auth-shell { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: #eef3f8; font-family: Inter, Arial, sans-serif; }
    .auth-card { width: min(420px, 100%); display: grid; gap: 14px; padding: 28px; border: 1px solid #d8e2ee; border-radius: 18px; background: #fff; box-shadow: 0 18px 45px rgba(15, 23, 42, .12); }
    .auth-brand { color: #0369a1; font-size: 13px; font-weight: 900; letter-spacing: .12em; text-transform: uppercase; }
    h1 { margin: 0; color: #172033; font-size: 24px; }
    p { margin: 0; color: #64748b; }
    label { display: grid; gap: 6px; color: #334155; font-size: 13px; font-weight: 800; }
    input { width: 100%; box-sizing: border-box; border: 1px solid #cbd5e1; border-radius: 10px; padding: 11px 12px; font: inherit; }
    button { border: 0; border-radius: 10px; padding: 11px 14px; background: #2563eb; color: #fff; font-weight: 800; cursor: pointer; }
    button:disabled { cursor: not-allowed; opacity: .55; }
    .auth-error { border-radius: 10px; padding: 10px; background: #fef2f2; color: #991b1b; font-weight: 700; }
    .logout-button { position: fixed; z-index: 50; right: 18px; bottom: 18px; box-shadow: 0 8px 24px rgba(15, 23, 42, .28); }
  `]
})
export class AuthGateComponent implements OnInit {
  login = '';
  senha = '';
  carregando = false;
  erro = '';
  autenticado = this.authSession.estaAutenticado();
  private trustedParentOrigins = new Set<string>();

  constructor(
    private readonly authApi: AuthApiService,
    private readonly authSession: AuthSessionService
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      const configuracao = await this.authApi.carregarConfiguracao();
      const origens = configuracao.trustedParentOrigins ?? [];
      this.trustedParentOrigins = new Set([
        window.location.origin,
        'http://localhost:4200',
        ...origens
      ].filter(Boolean));
      window.parent?.postMessage({ type: 'CLOUDPORT_CONTROL_ROOM_READY' }, '*');
    } catch (erro) {
      this.erro = this.extrairErro(erro);
    }
  }

  @HostListener('window:message', ['$event'])
  receberSessaoPortal(event: MessageEvent<MensagemSessaoPortal>): void {
    if (!this.trustedParentOrigins.has(event.origin)) {
      return;
    }
    const mensagem = event.data;
    if (mensagem?.type !== 'CLOUDPORT_AUTH_SESSION' || !mensagem.session?.token) {
      return;
    }
    try {
      this.authSession.iniciar({
        token: mensagem.session.token,
        nome: mensagem.session.nome,
        roles: mensagem.session.roles ?? []
      });
      this.validarPermissaoOperacional();
      this.erro = '';
      this.autenticado = true;
    } catch (erro) {
      this.authSession.encerrar();
      this.erro = this.extrairErro(erro);
    }
  }

  get nomeUsuario(): string {
    return this.authSession.obterNomeUsuario();
  }

  async entrar(): Promise<void> {
    if (!this.login || !this.senha || this.carregando) {
      return;
    }
    this.carregando = true;
    this.erro = '';
    try {
      const resposta = await this.authApi.autenticar(this.login, this.senha);
      this.authSession.iniciar(resposta);
      this.validarPermissaoOperacional();
      this.senha = '';
      this.autenticado = true;
    } catch (erro) {
      this.authSession.encerrar();
      this.erro = this.extrairErro(erro);
    } finally {
      this.carregando = false;
    }
  }

  sair(): void {
    this.authSession.encerrar();
    this.autenticado = false;
    this.senha = '';
  }

  private validarPermissaoOperacional(): void {
    if (!this.authSession.possuiAlgumaRole('ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE')) {
      throw new Error('A conta não possui permissão operacional para acessar o Control Room.');
    }
  }

  private extrairErro(erro: unknown): string {
    const resposta = erro as { error?: { mensagem?: string; message?: string; erro?: string }; message?: string };
    return resposta?.error?.mensagem
      ?? resposta?.error?.erro
      ?? resposta?.error?.message
      ?? resposta?.message
      ?? 'Não foi possível autenticar.';
  }
}
