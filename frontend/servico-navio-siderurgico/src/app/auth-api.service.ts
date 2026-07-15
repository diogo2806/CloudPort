import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';

export interface ConfiguracaoRuntimeControlRoom {
  baseApiUrl: string;
  trustedParentOrigins?: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private configuracao?: ConfiguracaoRuntimeControlRoom;

  constructor(private readonly http: HttpClient) {}

  async autenticar(login: string, senha: string): Promise<unknown> {
    const configuracao = await this.carregarConfiguracao();
    return firstValueFrom(this.http.post(
      `${this.normalizarBase(configuracao.baseApiUrl)}/auth/login`,
      { login: this.sanitizarLogin(login), senha }
    ).pipe(timeout(10000)));
  }

  async carregarConfiguracao(): Promise<ConfiguracaoRuntimeControlRoom> {
    if (this.configuracao) {
      return this.configuracao;
    }
    this.configuracao = await firstValueFrom(
      this.http.get<ConfiguracaoRuntimeControlRoom>('assets/configuracao.json').pipe(timeout(5000))
    );
    return this.configuracao;
  }

  private normalizarBase(baseApiUrl: string): string {
    return (baseApiUrl ?? '').replace(/\/+$/, '');
  }

  private sanitizarLogin(login: string): string {
    return (login ?? '')
      .normalize('NFKC')
      .replace(/[<>"'`\\]/g, '')
      .trim();
  }
}
