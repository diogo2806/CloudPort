import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, RouteReuseStrategy, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { LoginComponent } from './login.component';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';

class MockServicoAutenticacao {
  obterUsuarioAtual() {
    return null;
  }

  autenticar() {
    return of({ token: 'fake-token', login: 'usuario', roles: [] });
  }

  atualizarStatusMenu(_status: boolean) {}

  definirNomeUsuario(_nome: string) {}
}

const activatedRouteStub = {
  snapshot: {
    queryParams: {}
  }
};

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LoginComponent],
      imports: [RouterTestingModule],
      providers: [
        FormBuilder,
        { provide: ServicoAutenticacao, useClass: MockServicoAutenticacao },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: RouteReuseStrategy, useClass: CustomReuseStrategy }
      ]
    });
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default returnUrl to /home/role when query param is missing', () => {
    expect(component.rotaRetorno).toBe('/home/role');
  });

  it('should redirect to /home/role when login succeeds without returnUrl', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.formularioLogin.setValue({ login: 'joao', senha: 'segredo' });
    component.aoEnviar();

    expect(navigateSpy).toHaveBeenCalledWith('/home/role');
  });
});
