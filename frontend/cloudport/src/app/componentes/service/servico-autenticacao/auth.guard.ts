import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, CanActivateChild, CanLoad, Route, UrlSegment } from '@angular/router';

import { ServicoAutenticacao } from './servico-autenticacao.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate, CanActivateChild, CanLoad {
    constructor(
        private router: Router,
        private servicoAutenticacao: ServicoAutenticacao
    ) { }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const currentUser = this.servicoAutenticacao.obterUsuarioAtual();
        if (currentUser) {
            // logged in so return true
            return true;
        }

        // not logged in so redirect to login page with the return url
        this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false;
    }

    canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        return this.canActivate(childRoute, state);
    }

    canLoad(route: Route, segments: UrlSegment[]): boolean {
        const currentUser = this.servicoAutenticacao.obterUsuarioAtual();
        if (currentUser) {
            return true;
        }
        const returnUrl = '/' + segments.map(segment => segment.path).join('/');
        this.router.navigate(['/login'], { queryParams: { returnUrl } });
        return false;
    }
}