import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoleCadastroComponent } from './role-cadastro.component';

describe('RoleCadastroComponent', () => {
  let component: RoleCadastroComponent;
  let fixture: ComponentFixture<RoleCadastroComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RoleCadastroComponent]
    });
    fixture = TestBed.createComponent(RoleCadastroComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
