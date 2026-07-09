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

  it('deve consultar work queues persistentes da visita pelo contrato do modulo Navio + Patio', async () => {
    const configuracao = service.carregarConfiguracao();
    const configuracaoRequest = http.expectOne('assets/configuracao.json');
    expect(configuracaoRequest.request.method).toBe('GET');
    configuracaoRequest.flush({ baseApiUrl: 'http://localhost:8081/api/' });
    await configuracao;

    const resposta: WorkQueuePatioDaVisita[] = [
      {
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
      }
    ];

    const consulta = service.listarWorkQueuesPatio(42);
    const request = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/work-queues');

    expect(request.request.method).toBe('GET');
    request.flush(resposta);

    await expectAsync(consulta).toBeResolvedTo(resposta);
  });
});
