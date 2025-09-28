import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { User } from '../../model/user.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
    private currentUserSubject: BehaviorSubject<User | null>;
    public currentUser: Observable<User | null>;
    private menuStatus: BehaviorSubject<boolean>;
    public currentMenuStatus: Observable<boolean>;

    constructor(private http: HttpClient) {
        const storedData = localStorage.getItem('currentUser');
        const currentUser = storedData ? JSON.parse(storedData) : null;
        this.currentUserSubject = new BehaviorSubject<User | null>(currentUser);
        this.currentUser = this.currentUserSubject.asObservable();
        this.menuStatus = new BehaviorSubject<boolean>(!!currentUser);
        this.currentMenuStatus = this.menuStatus.asObservable();
    }

    public get currentUserValue(): User | null {
        return this.currentUserSubject.getValue();
    }

    login(login: string, password: string) {
        const url = `${environment.baseApiUrl}/auth/login`;
        return this.http.post<any>(url, { login, password })
            .pipe(map(user => {
                // store user details and jwt token in local storage to keep user logged in between page refreshes
                localStorage.setItem('currentUser', JSON.stringify(user));
                this.currentUserSubject.next(user);
                return user;
            }));
    }

    logout() {
        // remove user from local storage to log user out
        this.updateMenuStatus(false);
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

    updateMenuStatus(status: boolean) {
        this.menuStatus.next(status);
    }

    getMenuStatusValue(): boolean {
        return this.menuStatus.getValue();
    }
}
