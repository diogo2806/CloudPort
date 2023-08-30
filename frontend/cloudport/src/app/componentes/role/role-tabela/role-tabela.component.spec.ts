import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoleTabelaComponent } from './role-tabela.component';

describe('RoleComponent', () => {
  let component: RoleTabelaComponent;
  let fixture: ComponentFixture<RoleTabelaComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RoleTabelaComponent]
    });
    fixture = TestBed.createComponent(RoleTabelaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
