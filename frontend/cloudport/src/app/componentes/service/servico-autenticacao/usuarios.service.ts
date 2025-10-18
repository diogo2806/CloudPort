import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface UsuarioResumo {
  id: string;
  nome: string;
  email: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  constructor(
    private http: HttpClient,
    private configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarUsuarios(): Observable<UsuarioResumo[]> {
    const url = this.configuracaoAplicacao.construirUrlApi('/api/usuarios');
    return this.http.get<UsuarioResumo[]>(url).pipe(
      map((usuarios) =>
        (usuarios ?? []).map((usuario) => ({
          ...usuario,
          status: usuario.status ?? 'Ativo'
        }))
        .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR', { sensitivity: 'base' }))
      )
    );
  }
}
