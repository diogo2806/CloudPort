import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
    constructor(private http: HttpClient) {}


    requestHeader = new HttpHeaders({ 'No-Auth': 'True' });

    PATH_OF_API = 'https://8080-diogo2806-cloudport-5rk6q3wf87j.ws-us102.gitpod.io';
    
    login(username: string, password: string) {
        console.log('Realizando solicitação de login...');
        console.log('Dados de autenticação:', { username, password }); // Adicione este log
    
        return this.http.post<any>(this.PATH_OF_API + '/authenticate', { username, password })
            .pipe(map(user => {
                console.log('Resposta de login recebida:', user);
    
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
        console.log('Usuário desconectado.');
    }
}
