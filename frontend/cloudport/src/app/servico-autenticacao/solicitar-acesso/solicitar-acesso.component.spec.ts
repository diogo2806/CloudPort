import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SolicitarAcessoComponent } from './solicitar-acesso.component';

describe('SolicitarAcessoComponent', () => {
  let component: SolicitarAcessoComponent;
  let fixture: ComponentFixture<SolicitarAcessoComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SolicitarAcessoComponent]
    });
    fixture = TestBed.createComponent(SolicitarAcessoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
