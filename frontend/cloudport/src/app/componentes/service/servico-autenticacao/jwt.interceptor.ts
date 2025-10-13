
import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AuthenticationService } from './authentication.service';
import { User } from '../../model/user.model';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
    constructor(private authenticationService: AuthenticationService) { }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const token = this.resolveToken();

        if (token) {
            request = request.clone({
                setHeaders: {
                    Authorization: `Bearer ${token}`
                }
            });
        }

        return next.handle(request);
    }

    private resolveToken(): string | null {
        const currentUser = this.authenticationService.currentUserValue as (User & { [key: string]: any }) | null;

        if (!currentUser) {
            return null;
        }

        if (currentUser.token) {
            return currentUser.token;
        }

        const dynamicToken = currentUser['data']?.token
            ?? currentUser['accessToken']
            ?? currentUser['jwt'];

        return dynamicToken ?? null;
    }
}
