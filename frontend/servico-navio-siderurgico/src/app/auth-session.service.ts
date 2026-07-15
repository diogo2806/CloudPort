import { Injectable } from '@angular/core';

interface SessaoControlRoom {
  token: string;
  nome: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly storageKey = 'cloudportControlRoomSession';

  estaAutenticado(): boolean {
    const token = this.obterToken();
    if (!token) {
      return false;
    }
    const payload = this.decodificarToken(token);
    if (typeof payload?.exp === 'number' && payload.exp * 1000 <= Date.now()) {
      this.encerrar();
      return false;
    }
    return true;
  }

  iniciar(resposta: any): void {
    const origem = resposta?.data ?? resposta ?? {};
    const token = origem.token ?? origem.accessToken ?? resposta?.token ?? resposta?.accessToken ?? '';
    if (!token) {
      throw new Error('O serviço de autenticação não retornou um token JWT.');
    }
    const payload = this.decodificarToken(token) ?? {};
    const rolesResposta = Array.isArray(origem.roles) ? origem.roles : (origem.roles ? [origem.roles] : []);
    const rolesToken = Array.isArray(payload.roles) ? payload.roles : (payload.role ? [payload.role] : []);
    const sessao: SessaoControlRoom = {
      token,
      nome: payload.nome ?? origem.nome ?? origem.name ?? origem.login ?? payload.sub ?? 'operador',
      roles: Array.from(new Set([...rolesResposta, ...rolesToken].map((role: string) => this.normalizarRole(role))))
    };
    sessionStorage.setItem(this.storageKey, JSON.stringify(sessao));
  }

  encerrar(): void {
    sessionStorage.removeItem(this.storageKey);
  }

  obterToken(): string | null {
    return this.obterSessao()?.token ?? null;
  }

  obterNomeUsuario(): string {
    return this.obterSessao()?.nome ?? 'operador';
  }

  obterRoles(): string[] {
    return this.obterSessao()?.roles ?? [];
  }

  possuiAlgumaRole(...roles: string[]): boolean {
    const atuais = this.obterRoles();
    return roles.some(role => atuais.includes(this.normalizarRole(role)));
  }

  private obterSessao(): SessaoControlRoom | null {
    const armazenada = sessionStorage.getItem(this.storageKey);
    if (!armazenada) {
      return null;
    }
    try {
      const sessao = JSON.parse(armazenada) as SessaoControlRoom;
      return sessao?.token ? sessao : null;
    } catch {
      this.encerrar();
      return null;
    }
  }

  private decodificarToken(token: string): any | null {
    const partes = token.split('.');
    if (partes.length < 2) {
      return null;
    }
    try {
      const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
      const padding = '='.repeat((4 - base64.length % 4) % 4);
      const json = decodeURIComponent(atob(base64 + padding)
        .split('')
        .map(caractere => `%${('00' + caractere.charCodeAt(0).toString(16)).slice(-2)}`)
        .join(''));
      return JSON.parse(json);
    } catch {
      return null;
    }
  }

  private normalizarRole(role: string): string {
    const normalizada = (role ?? '').trim().toUpperCase();
    return normalizada.startsWith('ROLE_') ? normalizada : `ROLE_${normalizada}`;
  }
}
