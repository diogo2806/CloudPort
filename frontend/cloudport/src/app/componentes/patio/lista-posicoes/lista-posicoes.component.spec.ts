import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ListaPosicoesComponent } from './lista-posicoes.component';
import { ServicoPatioService } from '../../service/servico-patio/servico-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

describe('ListaPosicoesComponent', () => {
  let component: ListaPosicoesComponent;
  let fixture: ComponentFixture<ListaPosicoesComponent>;
  let servicoPatioSpy: jasmine.SpyObj<ServicoPatioService>;

  beforeEach(async () => {
    servicoPatioSpy = jasmine.createSpyObj('ServicoPatioService', ['listarPosicoes']);

    await TestBed.configureTestingModule({
      declarations: [ListaPosicoesComponent],
      providers: [
        SanitizadorConteudoService,
        { provide: ServicoPatioService, useValue: servicoPatioSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListaPosicoesComponent);
    component = fixture.componentInstance;
  });

  it('deve carregar posições com sucesso', () => {
    servicoPatioSpy.listarPosicoes.and.returnValue(of([
      { id: 1, linha: 1, coluna: 2, camadaOperacional: 'A', ocupada: true, codigoConteiner: 'CTN001', statusConteiner: 'RETIDO' }
    ]));

    component.carregarPosicoes();

    expect(component.posicoes.length).toBe(1);
    expect(component.carregando).toBeFalse();
    expect(component.erro).toBeUndefined();
  });

  it('deve informar erro ao falhar no carregamento', () => {
    servicoPatioSpy.listarPosicoes.and.returnValue(throwError(() => new Error('falha')));

    component.carregarPosicoes();

    expect(component.posicoes.length).toBe(0);
    expect(component.carregando).toBeFalse();
    expect(component.erro).toBeDefined();
  });
});
