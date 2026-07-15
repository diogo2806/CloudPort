import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';

interface ConfiguracaoRuntime {
  baseApiUrl: string;
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private baseApiUrl = '';

  constructor(private readonly http: HttpClient) {}

  async autenticar(login: string, senha: string): Promise<unknown> {
    await this.carregarConfiguracao();
    return firstValueFrom(this.http.post(
      `${this.baseApiUrl}/auth/login`,
      { login: this.sanitizarLogin(login), senha }
    ).pipe(timeout(10000)));
  }

  private async carregarConfiguracao(): Promise<void> {
    if (this.baseApiUrl) {
      return;
    }
    const configuracao = await firstValueFrom(
      this.http.get<ConfiguracaoRuntime>('assets/configuracao.json').pipe(timeout(5000))
    );
    this.baseApiUrl = (configuracao.baseApiUrl ?? '').replace(/\/+$/, '');
  }

  private sanitizarLogin(login: string): string {
    return (login ?? '')
      .normalize('NFKC')
      .replace(/[<>"'`\\]/g, '')
      .trim();
  }
}
