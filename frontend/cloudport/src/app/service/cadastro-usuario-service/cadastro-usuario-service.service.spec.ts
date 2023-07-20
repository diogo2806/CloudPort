import { TestBed } from '@angular/core/testing';

import { CadastroUsuarioService } from './cadastro-usuario-service.service';

describe('CadastroUsuarioServiceService', () => {
  let service: CadastroUsuarioService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CadastroUsuarioService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
