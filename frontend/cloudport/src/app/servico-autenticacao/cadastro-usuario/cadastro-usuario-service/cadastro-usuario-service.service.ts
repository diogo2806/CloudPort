import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UsuarioDTO } from '../../models/usuario-dto';

@Injectable({
  providedIn: 'root',
})
export class CadastroUsuarioService {
  private apiUrl = 'https://8080-diogo2806-cloudport-vd28lu2sfvn.ws-us101.gitpod.io/api/usuarios';

  constructor(private http: HttpClient) { }

  salvarUsuario(usuario: UsuarioDTO): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(this.apiUrl, usuario);
  }
}
