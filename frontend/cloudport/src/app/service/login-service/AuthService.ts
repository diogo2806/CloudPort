import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
    constructor(private http: HttpClient) {}

    login(username: string, password: string) {
        return this.http.post<any>(`/authenticate`, { username, password })
            .pipe(map(user => {
                // se a autenticação for bem-sucedida, o token JWT será retornado na resposta
                if (user && user.jwt) {
                    // armazene os detalhes do usuário e o token jwt no localStorage para manter o usuário logado entre as atualizações da página
                    localStorage.setItem('currentUser', JSON.stringify(user));
                }

                return user;
            }));
    }

    logout() {
        // remove o usuário do local storage para fazer logout
        localStorage.removeItem('currentUser');
    }
}
