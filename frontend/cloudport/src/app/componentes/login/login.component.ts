import { Component, EventEmitter, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { first } from 'rxjs/operators';
import { HomeComponent } from '../home/home.component';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';

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


export class LoginComponent implements OnInit {
    loginForm: FormGroup = this.formBuilder.group({}); // Initialized here
    loading = false;
    submitted = false;
    returnUrl: string = 'home'; // Initialized here
    error = '';

    

    constructor(
        private formBuilder: FormBuilder,
        private route: ActivatedRoute,
        private router: Router,
        private authenticationService: AuthenticationService
    ) {
        // redirect to home if already logged in
        if (this.authenticationService.currentUserValue) {
    
            this.router.navigate([this.returnUrl]);

        } else {
        
        }
    }

    ngOnInit() {
        this.authenticationService.updateMenuStatus(false);
        this.loginForm = this.formBuilder.group({
            username: ['', Validators.required],
            password: ['', Validators.required]
        });

        // get return url from route parameters or default to '/'
        this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '';
    }

    // convenience getter for easy access to form fields
    get f() {
        return this.loginForm.controls;
    }

    onSubmit() {
        this.submitted = true;

        // stop here if form is invalid
        if (this.loginForm.invalid) {
            return;
        }

        this.loading = true;
        this.authenticationService.login(this.f['username'].value, this.f['password'].value)
            .pipe(first())
            .subscribe(
                data => {
                    this.router.navigate([this.returnUrl]);
                    this.authenticationService.setUserName(this.f['username'].value);
                    this.authenticationService.updateMenuStatus(true); // set mostrarMenu to true after successful login
                },
                error => {
                    this.error = error;
                    this.loading = false;
                });
    }
}
