import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GateDashboardService } from './gate-dashboard.service';

describe('GateDashboardService', () => {
  let service: GateDashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GateDashboardService]
    });

    service = TestBed.inject(GateDashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve enviar filtros como parâmetros de consulta ao buscar o resumo', () => {
    const filtro = {
      inicio: '2024-01-01',
      fim: '2024-01-02',
      transportadoraId: 10,
      tipoOperacao: 'ENTRADA'
    };

    service.consultarResumo(filtro).subscribe();

    const req = httpMock.expectOne((request) => request.url.includes('/gate/dashboard'));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('inicio')).toBe('2024-01-01');
    expect(req.request.params.get('fim')).toBe('2024-01-02');
    expect(req.request.params.get('transportadoraId')).toBe('10');
    expect(req.request.params.get('tipoOperacao')).toBe('ENTRADA');
    req.flush({});
  });

  it('deve abrir stream com query string construída a partir do filtro', () => {
    const originalEventSource = (window as any).EventSource;
    const instances: any[] = [];
    (window as any).EventSource = function(url: string) {
      this.url = url;
      instances.push(this);
    } as any;

    const filtro = { tipoOperacao: 'SAIDA', transportadoraId: 12 };
    const eventSource = service.registrarStream(filtro);

    expect((eventSource as any).url).toContain('tipoOperacao=SAIDA');
    expect((eventSource as any).url).toContain('transportadoraId=12');

    (window as any).EventSource = originalEventSource;
  });
});

