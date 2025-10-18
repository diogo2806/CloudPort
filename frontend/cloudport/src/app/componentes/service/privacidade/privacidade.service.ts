import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment as endpointConfig } from '../endpoint';

export interface OpcaoPrivacidade {
  id: string;
  descricao: string;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class PrivacidadeService {
  constructor(private readonly http: HttpClient) {}

  listarOpcoes(): Observable<OpcaoPrivacidade[]> {
    return this.http.get<OpcaoPrivacidade[]>(endpointConfig.configuracoes.privacidade);
  }
}
