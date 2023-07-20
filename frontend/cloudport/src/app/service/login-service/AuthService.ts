import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map } from 'rxjs/operators';
import { throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  PATH_OF_API = 'https://8080-diogo2806-cloudport-p5osgdhffju.ws-us102.gitpod.io';

  login(username: string, password: string) {
    console.log('Realizando solicitação de login...');
    console.log('Dados de autenticação:', { username, password });

    return this.generateToken(username, password).pipe(
      map(token => this.callAuthenticateEndpoint(username, password, token))
    );
  }

  private generateToken(username: string, password: string) {
    const authRequest = {
      username: username,
      password: password
    };

    return this.http.post<string>(this.PATH_OF_API + '/authenticate', authRequest, { responseType: 'text' as 'json' });
  }

  private callAuthenticateEndpoint(username: string, password: string, token: string) {
    const httpOptions = {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`
      })
    };

    return this.http.post<any>(this.PATH_OF_API + '/authenticate', { username, password }, httpOptions)
      .pipe(
        map((user: any) => {
          console.log('Resposta de login recebida:', user);

          if (user && user.jwt) {
            localStorage.setItem('currentUser', JSON.stringify(user));
          }

          return user;
        }),
        catchError(error => {
          console.error('Ocorreu um erro na chamada HTTP:', error);
          return throwError(error);
        })
      );
  }

  logout() {
    localStorage.removeItem('currentUser');
    console.log('Usuário desconectado.');
  }
}
