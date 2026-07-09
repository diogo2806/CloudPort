import { AppComponent } from './app.component';
import { SiderurgicoApiService, WorkQueuePatioDaVisita } from './siderurgico-api.service';

describe('AppComponent - Control Room work queues', () => {
  let component: AppComponent;

  beforeEach(() => {
    component = new AppComponent({} as SiderurgicoApiService);
  });

  it('deve filtrar work queues persistentes por status e berco/bloco/POW/pool/equipamento', () => {
    component.workQueuesPatio = criarWorkQueues();
    component.statusOrdemFiltro = 'ATIVA';
    component.blocoZonaFiltro = 'RTG-01';

    const filtradas = component.workQueuesPatioFiltradas();

    expect(filtradas.length).toBe(1);
    expect(filtradas[0].identificador).toBe('VISITA-42|B1|P2|POW-01');
    expect(filtradas[0].jobList.length).toBe(2);
  });

  it('deve totalizar job list real e usar totalOrdens quando a fila vier sem detalhe expandido', () => {
    component.workQueuesPatio = criarWorkQueues();

    expect(component.totalJobListWorkQueuesPatio()).toBe(5);
  });

  it('deve retornar todas as work queues quando os filtros estiverem vazios', () => {
    component.workQueuesPatio = criarWorkQueues();
    component.statusOrdemFiltro = '';
    component.blocoZonaFiltro = '';

    expect(component.workQueuesPatioFiltradas().length).toBe(2);
  });

  it('deve controlar expansao da job list e manter edicao operacional por work queue', () => {
    const workQueue = criarWorkQueues()[0];

    expect(component.workQueueExpandida(workQueue)).toBeFalse();

    component.alternarWorkQueue(workQueue);

    expect(component.workQueueExpandida(workQueue)).toBeTrue();
    expect(component.edicaoWorkQueue(workQueue)).toEqual({
      pow: 'POW-01',
      poolOperacional: 'POOL-RTG',
      equipamento: 'RTG-01',
      limiteDispatch: null
    });
  });

  it('deve chamar API de ativacao de work queue e atualizar o estado local', async () => {
    const workQueue = criarWorkQueues()[0];
    const api = {
      ativarWorkQueuePatio: jasmine.createSpy().and.resolveTo({ ...workQueue, status: 'ATIVA' })
    } as unknown as SiderurgicoApiService;
    component = new AppComponent(api);
    component.workQueuesPatio = [{ ...workQueue, status: 'INATIVA' }];

    await component.ativarWorkQueue(workQueue);

    expect(api.ativarWorkQueuePatio).toHaveBeenCalledWith(10);
    expect(component.workQueuesPatio[0].status).toBe('ATIVA');
    expect(component.sucesso).toBe('Work queue ativada.');
  });
});

function criarWorkQueues(): WorkQueuePatioDaVisita[] {
  return [
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
      totalOrdens: 2,
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
        },
        {
          id: 100,
          visitaNavioId: 42,
          itemOperacaoNavioId: 8,
          codigoLote: 'LOTE-002',
          tipoMovimento: 'DESCARGA',
          statusOrdem: 'EM_EXECUCAO',
          origem: 'NAVIO',
          destino: 'A-01-02',
          sequenciaNavio: 2,
          prioridadeOperacional: 2
        }
      ]
    },
    {
      id: 11,
      identificador: 'VISITA-42|B2|P1|POW-02',
      agrupamento: 'WORK_QUEUE_PATIO',
      visitaNavioId: 42,
      berco: 'B2',
      porao: 1,
      blocoZona: 'B',
      sequenciaInicial: 3,
      pow: 'POW-02',
      poolOperacional: 'POOL-RS',
      equipamento: 'RS-02',
      status: 'PLANEJADA',
      prioridadeOperacional: 3,
      totalOrdens: 3,
      jobList: []
    }
  ];
}
