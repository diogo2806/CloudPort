import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, RouteReuseStrategy, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';

class MockAuthenticationService {
  currentUserValue = null;

  login() {
    return of({ token: 'fake-token', login: 'user', roles: [] });
  }

  updateMenuStatus(_status: boolean) {}

  setUserName(_username: string) {}
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
        { provide: AuthenticationService, useClass: MockAuthenticationService },
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

  it('should default returnUrl to /home when query param is missing', () => {
    expect(component.returnUrl).toBe('/home');
  });

  it('should redirect to /home when login succeeds without returnUrl', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigateByUrl');

    component.loginForm.setValue({ username: 'john', password: 'secret' });
    component.onSubmit();

    expect(navigateSpy).toHaveBeenCalledWith('/home');
  });
});
