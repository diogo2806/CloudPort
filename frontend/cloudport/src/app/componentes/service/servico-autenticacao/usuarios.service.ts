import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { environment as endpointConfig } from '../../service/endpoint';

export interface UsuarioResumo {
  id: string;
  nome: string;
  email: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  constructor(private http: HttpClient) {}

  listarUsuarios(): Observable<UsuarioResumo[]> {
    return this.http.get<UsuarioResumo[]>(endpointConfig.users.getAll).pipe(
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
