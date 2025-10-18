import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ListaMovimentacoesComponent } from './lista-movimentacoes.component';
import { ServicoPatioService } from '../../service/servico-patio/servico-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

describe('ListaMovimentacoesComponent', () => {
  let component: ListaMovimentacoesComponent;
  let fixture: ComponentFixture<ListaMovimentacoesComponent>;
  let servicoPatioSpy: jasmine.SpyObj<ServicoPatioService>;

  beforeEach(async () => {
    servicoPatioSpy = jasmine.createSpyObj('ServicoPatioService', ['listarMovimentacoes']);

    await TestBed.configureTestingModule({
      declarations: [ListaMovimentacoesComponent],
      providers: [
        SanitizadorConteudoService,
        { provide: ServicoPatioService, useValue: servicoPatioSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListaMovimentacoesComponent);
    component = fixture.componentInstance;
  });

  it('deve carregar movimentações', () => {
    servicoPatioSpy.listarMovimentacoes.and.returnValue(of([
      {
        id: 10,
        codigoConteiner: 'CTN123',
        tipoMovimento: 'ALOCACAO',
        descricao: 'Teste',
        registradoEm: new Date().toISOString()
      }
    ]));

    component.carregarMovimentacoes();

    expect(component.movimentacoes.length).toBe(1);
    expect(component.erro).toBeUndefined();
    expect(component.carregando).toBeFalse();
  });

  it('deve exibir erro quando a busca falha', () => {
    servicoPatioSpy.listarMovimentacoes.and.returnValue(throwError(() => new Error('falha')));

    component.carregarMovimentacoes();

    expect(component.movimentacoes.length).toBe(0);
    expect(component.erro).toBeDefined();
    expect(component.carregando).toBeFalse();
  });
});
