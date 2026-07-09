import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { SiderurgicoApiService, WorkQueuePatioDaVisita } from './siderurgico-api.service';

describe('SiderurgicoApiService', () => {
  let service: SiderurgicoApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SiderurgicoApiService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(SiderurgicoApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function carregarConfiguracao(): Promise<void> {
    const configuracao = service.carregarConfiguracao();
    const configuracaoRequest = http.expectOne('assets/configuracao.json');
    expect(configuracaoRequest.request.method).toBe('GET');
    configuracaoRequest.flush({ baseApiUrl: 'http://localhost:8081/api/' });
    await configuracao;
  }

  it('deve consultar work queues persistentes da visita pelo contrato do modulo Navio + Patio', async () => {
    await carregarConfiguracao();

    const resposta: WorkQueuePatioDaVisita[] = [criarWorkQueue()];
    const consulta = service.listarWorkQueuesPatio(42);
    const request = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/work-queues');

    expect(request.request.method).toBe('GET');
    request.flush(resposta);

    await expectAsync(consulta).toBeResolvedTo(resposta);
  });

  it('deve acionar contratos operacionais de work queue expostos pelo yard', async () => {
    await carregarConfiguracao();

    const ativar = service.ativarWorkQueuePatio(10);
    const ativarRequest = http.expectOne('http://localhost:8081/api/yard/patio/work-queues/10/ativar');
    expect(ativarRequest.request.method).toBe('PATCH');
    ativarRequest.flush({ ...criarWorkQueue(), status: 'ATIVA' });
    await expectAsync(ativar).toBeResolved();

    const desativar = service.desativarWorkQueuePatio(10);
    const desativarRequest = http.expectOne('http://localhost:8081/api/yard/patio/work-queues/10/desativar');
    expect(desativarRequest.request.method).toBe('PATCH');
    desativarRequest.flush({ ...criarWorkQueue(), status: 'INATIVA' });
    await expectAsync(desativar).toBeResolved();

    const atualizarPow = service.atualizarPowWorkQueuePatio(10, { pow: 'POW-02', poolOperacional: 'POOL-RS' });
    const powRequest = http.expectOne('http://localhost:8081/api/yard/patio/work-queues/10/pow');
    expect(powRequest.request.method).toBe('PATCH');
    expect(powRequest.request.body).toEqual({ pow: 'POW-02', poolOperacional: 'POOL-RS' });
    powRequest.flush({ ...criarWorkQueue(), pow: 'POW-02', poolOperacional: 'POOL-RS' });
    await expectAsync(atualizarPow).toBeResolved();

    const atualizarEquipamento = service.atualizarEquipamentoWorkQueuePatio(10, { equipamento: 'RTG-02' });
    const equipamentoRequest = http.expectOne('http://localhost:8081/api/yard/patio/work-queues/10/equipamento');
    expect(equipamentoRequest.request.method).toBe('PATCH');
    expect(equipamentoRequest.request.body).toEqual({ equipamento: 'RTG-02' });
    equipamentoRequest.flush({ ...criarWorkQueue(), equipamento: 'RTG-02' });
    await expectAsync(atualizarEquipamento).toBeResolved();

    const dispatch = service.despacharWorkQueuePatio(10, { limiteOrdens: 2, operador: 'CONTROL_ROOM' });
    const dispatchRequest = http.expectOne('http://localhost:8081/api/yard/patio/work-queues/10/dispatch');
    expect(dispatchRequest.request.method).toBe('POST');
    expect(dispatchRequest.request.body).toEqual({ limiteOrdens: 2, operador: 'CONTROL_ROOM' });
    dispatchRequest.flush({ workQueueId: 10, totalOrdensDespachadas: 1, ordens: [] });
    await expectAsync(dispatch).toBeResolved();
  });

  it('deve acionar contratos operacionais de work instruction expostos pelo yard', async () => {
    await carregarConfiguracao();

    const reset = service.resetarWorkInstructionPatio(99);
    const resetRequest = http.expectOne('http://localhost:8081/api/yard/patio/work-instructions/99/reset');
    expect(resetRequest.request.method).toBe('POST');
    resetRequest.flush(criarWorkQueue().jobList[0]);
    await expectAsync(reset).toBeResolved();

    const cancelar = service.cancelarWorkInstructionPatio(99);
    const cancelarRequest = http.expectOne('http://localhost:8081/api/yard/patio/work-instructions/99/cancelar');
    expect(cancelarRequest.request.method).toBe('POST');
    cancelarRequest.flush({ ...criarWorkQueue().jobList[0], statusOrdem: 'CANCELADA' });
    await expectAsync(cancelar).toBeResolved();
  });
});

function criarWorkQueue(): WorkQueuePatioDaVisita {
  return {
    id: 10,
    identificador: 'VISITA-42|B1|P2|POW-01',
    agrupamento: 'WORK_QUEUE_PATIO',
    visitaNavioId: 42,
    berco: 'B1',
    porao: 2,
    blocoZona: 'A',
    sequenciaInicial: 1,
    pow: 'POW-01',
    poolOperacional: 'POOL-RTG',
    equipamento: 'RTG-01',
    status: 'ATIVA',
    prioridadeOperacional: 1,
    totalOrdens: 1,
    jobList: [
      {
        id: 99,
        visitaNavioId: 42,
        itemOperacaoNavioId: 7,
        codigoLote: 'LOTE-001',
        tipoMovimento: 'DESCARGA',
        statusOrdem: 'PENDENTE',
        origem: 'NAVIO',
        destino: 'A-01-01',
        sequenciaNavio: 1,
        prioridadeOperacional: 1
      }
    ]
  };
}
