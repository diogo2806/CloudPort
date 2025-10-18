import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { FormularioMovimentacaoComponent } from './formulario-movimentacao.component';
import { ServicoPatioService } from '../../service/servico-patio/servico-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

describe('FormularioMovimentacaoComponent', () => {
  let component: FormularioMovimentacaoComponent;
  let fixture: ComponentFixture<FormularioMovimentacaoComponent>;
  let servicoPatioSpy: jasmine.SpyObj<ServicoPatioService>;

  beforeEach(async () => {
    servicoPatioSpy = jasmine.createSpyObj('ServicoPatioService', [
      'obterOpcoesCadastro',
      'salvarConteiner',
      'salvarEquipamento'
    ]);

    servicoPatioSpy.obterOpcoesCadastro.and.returnValue(of({
      statusConteiner: ['AGUARDANDO_RETIRADA'],
      tiposEquipamento: ['RTG'],
      statusEquipamento: ['OPERACIONAL']
    }));

    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      declarations: [FormularioMovimentacaoComponent],
      providers: [
        SanitizadorConteudoService,
        { provide: ServicoPatioService, useValue: servicoPatioSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FormularioMovimentacaoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve enviar formulário de contêiner com sucesso', () => {
    servicoPatioSpy.salvarConteiner.and.returnValue(of({ id: 1 } as any));

    component.formularioConteiner.setValue({
      id: null,
      codigo: 'CTN123',
      linha: 1,
      coluna: 2,
      status: 'AGUARDANDO_RETIRADA',
      tipoCarga: 'Granel',
      destino: 'Santos',
      camadaOperacional: 'A1'
    });

    component.submeterConteiner();

    expect(servicoPatioSpy.salvarConteiner).toHaveBeenCalled();
    expect(component.sucessoConteiner).toBeTruthy();
  });

  it('não deve enviar contêiner quando formulário é inválido', () => {
    component.formularioConteiner.patchValue({ codigo: '' });

    component.submeterConteiner();

    expect(servicoPatioSpy.salvarConteiner).not.toHaveBeenCalled();
  });

  it('deve informar erro ao falhar no envio do equipamento', () => {
    servicoPatioSpy.salvarEquipamento.and.returnValue(throwError(() => new Error('falha')));

    component.formularioEquipamento.setValue({
      id: null,
      identificador: 'EQP',
      tipoEquipamento: 'RTG',
      linha: 1,
      coluna: 1,
      statusOperacional: 'OPERACIONAL'
    });

    component.submeterEquipamento();

    expect(servicoPatioSpy.salvarEquipamento).toHaveBeenCalled();
    expect(component.erroEquipamento).toBeTruthy();
  });
});
