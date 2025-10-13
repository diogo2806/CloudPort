import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GateApiService } from './gate-api.service';

describe('GateApiService', () => {
  let service: GateApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GateApiService]
    });

    service = TestBed.inject(GateApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve enviar filtros na listagem de agendamentos', () => {
    service.listarAgendamentos({ codigo: 'AG01', pagina: 2 }).subscribe();

    const req = httpMock.expectOne((request) => request.url.includes('/gate/agendamentos'));
    expect(req.request.params.get('codigo')).toBe('AG01');
    expect(req.request.params.get('pagina')).toBe('2');
    req.flush({});
  });

  it('deve montar FormData com metadados ao enviar documentos', (done) => {
    const arquivo = new File(['conteudo'], 'comprovante.pdf', { type: 'application/pdf' });
    service.uploadDocumentoAgendamento(5, arquivo).subscribe();

    const req = httpMock.expectOne((request) => request.url.includes('/gate/agendamentos/5/documentos'));
    expect(req.request.method).toBe('POST');
    const formData = req.request.body as FormData;
    const metadata = formData.get('metadata') as Blob;
    expect(metadata).toBeTruthy();
    metadata.text().then((texto) => {
      const json = JSON.parse(texto);
      expect(json.tipoDocumento).toBe('application/pdf');
      done();
    });
    expect(formData.get('arquivo')).toBeTruthy();
    req.flush({});
  });
});

