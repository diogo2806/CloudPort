import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SolicitarAcessoServiceComponent } from './solicitar-acesso-service.component';

describe('SolicitarAcessoServiceComponent', () => {
  let component: SolicitarAcessoServiceComponent;
  let fixture: ComponentFixture<SolicitarAcessoServiceComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SolicitarAcessoServiceComponent]
    });
    fixture = TestBed.createComponent(SolicitarAcessoServiceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
