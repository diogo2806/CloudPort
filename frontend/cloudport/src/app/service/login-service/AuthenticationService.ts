import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment.prod';
//import { User } from '../../models/user.model';
import { User } from '../../servico-autenticacao/models/User';

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
    private currentUserSubject: BehaviorSubject<User | null>;
    public currentUser: Observable<User | null>;

    constructor(private http: HttpClient) {
        let currentUser = localStorage.getItem('currentUser');
        this.currentUserSubject = new BehaviorSubject<User | null>(currentUser ? JSON.parse(currentUser) : null);

        this.currentUser = this.currentUserSubject.asObservable();
    }

    public get currentUserValue(): User {
        return this.currentUserSubject.value!;
    }

    login(nome: string, senha: string) {
        return this.http.post<any>(`https://8080-diogo2806-cloudport-ggwot2o7imq.ws-us102.gitpod.io//auth`, { nome, senha })
            .pipe(map(user => {
                // store user details and jwt token in local storage to keep user logged in between page refreshes
                localStorage.setItem('currentUser', JSON.stringify(user));
                this.currentUserSubject.next(user);
                return user;
            }));
    }

    logout() {
        // remove user from local storage to log user out
        localStorage.removeItem('currentUser');
        localStorage.removeItem('username');
        this.currentUserSubject.next(null);
    }

    //set name user new in storage
    setUserName(username:string){
        localStorage.setItem('username', JSON.stringify(username));
    }

    getUserName() {
        let storedValue = localStorage.getItem('username');
        return JSON.parse(storedValue !== null ? storedValue : '""');
    }
    
}
