import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, HostListener, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';

interface ConfiguracaoPortal {
  baseApiUrl: string;
  navioControlRoomUrl?: string;
}

@Component({
  selector: 'app-navio-control-room',
  standalone: false,
  template: `
    <section class="control-room-host">
      <header>
        <div>
          <span>CloudPort</span>
          <strong>Control Room Navio + Pátio</strong>
        </div>
        <button type="button" (click)="recarregar()">Recarregar módulo</button>
      </header>
      <p class="erro" *ngIf="erro">{{ erro }}</p>
      <iframe
        #frame
        *ngIf="urlSegura"
        [src]="urlSegura"
        title="Control Room Navio e Pátio"
        (load)="enviarSessao()"
        referrerpolicy="strict-origin"
      ></iframe>
    </section>
  `,
  styles: [`
    :host { display: block; height: calc(100vh - 150px); min-height: 650px; }
    .control-room-host { display: grid; grid-template-rows: auto auto 1fr; height: 100%; background: #eef3f8; }
    header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 16px; background: #0d1b2a; color: #fff; }
    header div { display: grid; }
    header span { color: #7dd3fc; font-size: 11px; font-weight: 900; letter-spacing: .08em; text-transform: uppercase; }
    header strong { font-size: 16px; }
    button { border: 1px solid rgba(255,255,255,.25); border-radius: 999px; padding: 8px 12px; background: transparent; color: #fff; font-weight: 800; cursor: pointer; }
    iframe { width: 100%; height: 100%; border: 0; background: #fff; }
    .erro { margin: 12px; border-radius: 10px; padding: 10px; background: #fef2f2; color: #991b1b; font-weight: 700; }
  `]
})
export class NavioControlRoomComponent implements OnInit {
  @ViewChild('frame') frame?: ElementRef<HTMLIFrameElement>;
  urlSegura?: SafeResourceUrl;
  erro = '';
  private controlRoomUrl = '';
  private targetOrigin = '';

  constructor(
    private readonly http: HttpClient,
    private readonly sanitizer: DomSanitizer,
    private readonly autenticacao: ServicoAutenticacao
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      const configuracao = await firstValueFrom(this.http.get<ConfiguracaoPortal>('assets/configuracao.json'));
      this.controlRoomUrl = configuracao.navioControlRoomUrl || 'http://localhost:4201';
      const url = new URL(this.controlRoomUrl, window.location.origin);
      this.targetOrigin = url.origin;
      this.urlSegura = this.sanitizer.bypassSecurityTrustResourceUrl(url.toString());
    } catch {
      this.erro = 'Não foi possível carregar a configuração do Control Room de navios.';
    }
  }

  @HostListener('window:message', ['$event'])
  receberMensagem(event: MessageEvent): void {
    if (event.origin !== this.targetOrigin || event.data?.type !== 'CLOUDPORT_CONTROL_ROOM_READY') {
      return;
    }
    this.enviarSessao();
  }

  enviarSessao(): void {
    const usuario = this.autenticacao.obterUsuarioAtual();
    const destino = this.frame?.nativeElement.contentWindow;
    if (!usuario?.token || !destino || !this.targetOrigin) {
      return;
    }
    destino.postMessage({
      type: 'CLOUDPORT_AUTH_SESSION',
      session: {
        token: usuario.token,
        nome: usuario.nome,
        roles: usuario.roles
      }
    }, this.targetOrigin);
  }

  recarregar(): void {
    const frame = this.frame?.nativeElement;
    if (frame) {
      frame.src = frame.src;
    }
  }
}
