import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { User } from '../model/user.model';

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
    private currentUserSubject: BehaviorSubject<User | null>;
    public currentUser: Observable<User | null>;

    constructor(private http: HttpClient) {
        const storedData = localStorage.getItem('currentUser');
        this.currentUserSubject = new BehaviorSubject<User | null>(storedData ? JSON.parse(storedData) : null);
        this.currentUser = this.currentUserSubject.asObservable();
    }

    public get currentUserValue(): User | null {
        return this.currentUserSubject.getValue();
    }

    login(login: string, password: string) {
        return this.http.post<any>(`https://8080-diogo2806-cloudport-mf5guiv054l.ws-us102.gitpod.io/auth/login`, { login, password })
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
    setUserName(username: string) {
        localStorage.setItem('username', JSON.stringify(username));
    }

    getUserName(): string | null {
        const storedData = localStorage.getItem('username');
        return storedData ? JSON.parse(storedData) : null;
    }
}
