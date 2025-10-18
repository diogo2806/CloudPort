import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface OpcaoPrivacidade {
  id: string;
  descricao: string;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class PrivacidadeService {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarOpcoes(): Observable<OpcaoPrivacidade[]> {
    const url = this.configuracaoAplicacao.construirUrlApi('/api/configuracoes/privacidade');
    return this.http.get<OpcaoPrivacidade[]>(url);
  }
}
