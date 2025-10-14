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
        const currentUserData = storedData ? JSON.parse(storedData) : null;
        const currentUser = currentUserData ? this.mapToUser(currentUserData) : null;
        this.currentUserSubject = new BehaviorSubject<User | null>(currentUser);
        this.currentUser = this.currentUserSubject.asObservable();
        this.menuStatus = new BehaviorSubject<boolean>(this.shouldDisplayMenu(currentUser?.roles ?? []));
        this.currentMenuStatus = this.menuStatus.asObservable();
    }

    public get currentUserValue(): User | null {
        return this.currentUserSubject.getValue();
    }

    login(login: string, password: string) {
        const url = `${environment.baseApiUrl}/auth/login`;
        return this.http.post<any>(url, { login, password })
            .pipe(map(response => {
                const user = this.mapToUser(response);
                // store user details and jwt token in local storage to keep user logged in between page refreshes
                localStorage.setItem('currentUser', JSON.stringify(user));
                this.currentUserSubject.next(user);
                this.updateMenuStatus(this.shouldDisplayMenu(user.roles));
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

    hasRole(role: string): boolean {
        const normalized = role?.startsWith('ROLE_') ? role : `ROLE_${(role ?? '').toUpperCase()}`;
        return this.getCurrentRoles().includes(normalized);
    }

    hasAnyRole(...roles: string[]): boolean {
        if (!roles || roles.length === 0) {
            return false;
        }
        return roles.some(role => this.hasRole(role));
    }

    getCurrentRoles(): string[] {
        return this.currentUserSubject.getValue()?.roles ?? [];
    }

    private shouldDisplayMenu(roles: string[]): boolean {
        const normalizedRoles = this.normalizeRoles(roles);
        const allowedRoles = [
            'ROLE_ADMIN_PORTO',
            'ROLE_PLANEJADOR',
            'ROLE_OPERADOR_GATE',
            'ROLE_TRANSPORTADORA'
        ];
        return normalizedRoles.some(role => allowedRoles.includes(role));
    }

    private mapToUser(data: any): User {
        if (!data) {
            return new User();
        }

        const source = data.data ?? data;
        const token = source.token
            ?? data.token
            ?? source.accessToken
            ?? data.accessToken
            ?? '';
        const decoded = this.decodeToken(token);
        const responseRoles = Array.isArray(source.roles)
            ? source.roles
            : (source.roles ? [source.roles] : []);
        const tokenRoles = Array.isArray(decoded?.roles)
            ? decoded.roles
            : (decoded?.role ? [decoded.role] : []);
        const roles = this.normalizeRoles([...(responseRoles || []), ...(tokenRoles || [])]);
        const perfil = decoded?.perfil ?? source.perfil ?? data.perfil ?? (roles.length > 0 ? roles[0] : '');
        const nome = decoded?.nome ?? source.nome ?? source.name ?? source.login ?? data.nome ?? data.login ?? '';
        const id = decoded?.userId ?? source.id ?? data.id ?? source.userId ?? data.userId ?? '';
        const transportadoraDocumento = decoded?.transportadoraDocumento ?? source.transportadoraDocumento ?? null;
        const transportadoraNome = decoded?.transportadoraNome ?? source.transportadoraNome ?? null;

        return new User(
            id,
            nome,
            token,
            source.email ?? data.email ?? '',
            source.senha ?? data.senha ?? '',
            perfil,
            roles,
            transportadoraDocumento,
            transportadoraNome
        );
    }

    private decodeToken(token: string | undefined): any | null {
        if (!token) {
            return null;
        }
        const segments = token.split('.');
        if (segments.length < 2) {
            return null;
        }
        try {
            const payload = segments[1]
                .replace(/-/g, '+')
                .replace(/_/g, '/');
            const decodedPayload = decodeURIComponent(atob(payload)
                .split('')
                .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join(''));
            return JSON.parse(decodedPayload);
        } catch (error) {
            console.warn('Falha ao decodificar token JWT', error);
            return null;
        }
    }

    private normalizeRoles(roles: string[] | undefined): string[] {
        if (!roles) {
            return [];
        }
        const normalized = roles
            .filter(role => !!role)
            .map(role => role.startsWith('ROLE_') ? role : `ROLE_${role.toUpperCase()}`);
        return Array.from(new Set(normalized));
    }
}
