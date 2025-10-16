import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, RouteReuseStrategy } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { first } from 'rxjs/operators';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';

// Importações para animações
import { trigger, state, style, animate, transition } from '@angular/animations';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    animations: [
        trigger('routerTransition', [
            // Defina os estados e transições aqui
            state('void', style({ opacity: 0 })),
            state('*', style({ opacity: 1 })),
            transition('void => *', animate('0.5s ease-in')),
            transition('* => void', animate('0.5s ease-out'))
        ])
    ]
})
const ROTA_PROTEGIDA_PADRAO = '/home/role';

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
        private reuseStrategy: RouteReuseStrategy // Injete a estratégia de reutilização de rota aqui

    ) {
        // redirect to home if already logged in
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

        // get return url from route parameters or default to '/'
        const requestedReturnUrl = this.route.snapshot.queryParams['returnUrl'];
        const hasCustomReturnUrl = typeof requestedReturnUrl === 'string' && requestedReturnUrl.trim().length > 0;
        this.rotaRetorno = hasCustomReturnUrl ? requestedReturnUrl : ROTA_PROTEGIDA_PADRAO;
        (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
    }

    // convenience getter for easy access to form fields
    get f() {
        return this.formularioLogin.controls;
    }

    aoEnviar() {
        this.enviado = true;

        // stop here if form is invalid
        if (this.formularioLogin.invalid) {
            return;
        }

        const loginSanitizado = this.sanitizarCampo(this.f['login'].value);
        const senhaSanitizada = this.sanitizarCampo(this.f['senha'].value);
        this.formularioLogin.patchValue({ login: loginSanitizado, senha: senhaSanitizada }, { emitEvent: false, onlySelf: true });

        this.mensagemErro = '';
        this.carregando = true;
        this.servicoAutenticacao.autenticar(loginSanitizado, senhaSanitizada)
            .pipe(first())
            .subscribe(
                data => {

                    this.router.navigateByUrl(this.rotaRetorno);
                    this.servicoAutenticacao.definirNomeUsuario(loginSanitizado);
                    this.servicoAutenticacao.atualizarStatusMenu(true); // set mostrarMenu to true after successful login
                    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
                    this.carregando = false;
                },
                error => {
                    this.mensagemErro = error;
                    this.carregando = false;
                });
    }

    private sanitizarCampo(valor: string): string {
        if (!valor) {
            return '';
        }
        return valor
            .normalize('NFKC')
            .replace(/[<>"'`\\]/g, '')
            .trim();
    }
}
