import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, RouteReuseStrategy } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { first } from 'rxjs/operators';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';

import { trigger, state, style, animate, transition } from '@angular/animations';

const ROTA_PROTEGIDA_PADRAO = '/home/role';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    animations: [
        trigger('routerTransition', [
            state('void', style({ opacity: 0 })),
            state('*', style({ opacity: 1 })),
            transition('void => *', animate('0.5s ease-in')),
            transition('* => void', animate('0.5s ease-out'))
        ])
    ],
    standalone: false
})
export class LoginComponent implements OnInit {
    formularioLogin: FormGroup = this.formBuilder.group({});
    carregando = false;
    enviado = false;
    rotaRetorno: string = ROTA_PROTEGIDA_PADRAO;
    mensagemErro = '';

    constructor(
        private formBuilder: FormBuilder,
        private route: ActivatedRoute,
        private router: Router,
        private servicoAutenticacao: ServicoAutenticacao,
        private reuseStrategy: RouteReuseStrategy
    ) {
        if (this.servicoAutenticacao.obterUsuarioAtual()) {
            this.router.navigateByUrl(this.rotaRetorno);
        }
    }

    ngOnInit() {
        this.servicoAutenticacao.atualizarStatusMenu(false);
        this.formularioLogin = this.formBuilder.group({
            login: ['', [Validators.required, Validators.pattern(/^[\p{L}\p{N}@._-]+$/u)]],
            senha: ['', [Validators.required, Validators.minLength(6)]]
        });

        const requestedReturnUrl = this.route.snapshot.queryParams['returnUrl'];
        const hasCustomReturnUrl = typeof requestedReturnUrl === 'string' && requestedReturnUrl.trim().length > 0;
        this.rotaRetorno = hasCustomReturnUrl ? requestedReturnUrl : ROTA_PROTEGIDA_PADRAO;
        (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
    }

    get f() {
        return this.formularioLogin.controls;
    }

    aoEnviar() {
        this.enviado = true;

        if (this.formularioLogin.invalid) {
            return;
        }

        const loginSanitizado = this.sanitizarLogin(this.f['login'].value);
        const senha = typeof this.f['senha'].value === 'string' ? this.f['senha'].value : '';
        this.formularioLogin.patchValue({ login: loginSanitizado }, { emitEvent: false, onlySelf: true });

        this.mensagemErro = '';
        this.carregando = true;
        this.servicoAutenticacao.autenticar(loginSanitizado, senha)
            .pipe(first())
            .subscribe({
                next: () => {
                    this.router.navigateByUrl(this.rotaRetorno);
                    this.servicoAutenticacao.definirNomeUsuario(loginSanitizado);
                    this.servicoAutenticacao.atualizarStatusMenu(true);
                    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
                    this.carregando = false;
                },
                error: (error) => {
                    this.mensagemErro = error;
                    this.carregando = false;
                }
            });
    }

    private sanitizarLogin(valor: string): string {
        if (!valor) {
            return '';
        }
        return valor
            .normalize('NFKC')
            .replace(/[<>"'`\\]/g, '')
            .trim();
    }
}
