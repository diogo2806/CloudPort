import { AppComponent } from './app.component';
import { OrdemPatioDaVisita, SiderurgicoApiService, WorkQueuePatioDaVisita } from './siderurgico-api.service';

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

  it('deve chamar API de desativacao de work queue e atualizar o estado local', async () => {
    const workQueue = criarWorkQueues()[0];
    const api = {
      desativarWorkQueuePatio: jasmine.createSpy().and.resolveTo({ ...workQueue, status: 'INATIVA' })
    } as unknown as SiderurgicoApiService;
    component = new AppComponent(api);
    component.workQueuesPatio = [{ ...workQueue, status: 'ATIVA' }];
    await component.desativarWorkQueue(workQueue);
    expect(api.desativarWorkQueuePatio).toHaveBeenCalledWith(10);
    expect(component.workQueuesPatio[0].status).toBe('INATIVA');
    expect(component.sucesso).toBe('Work queue desativada.');
  });

  it('deve enviar POW e pool operacional editados para a work queue', async () => {
    const workQueue = criarWorkQueues()[0];
    const api = {
      atualizarPowWorkQueuePatio: jasmine.createSpy().and.resolveTo({ ...workQueue, pow: 'POW-09', poolOperacional: 'POOL-RS' })
    } as unknown as SiderurgicoApiService;
    component = new AppComponent(api);
    component.workQueuesPatio = [workQueue];
    component.edicaoWorkQueue(workQueue).pow = 'POW-09';
    component.edicaoWorkQueue(workQueue).poolOperacional = 'POOL-RS';
    await component.salvarPowWorkQueue(workQueue);
    expect(api.atualizarPowWorkQueuePatio).toHaveBeenCalledWith(10, { pow: 'POW-09', poolOperacional: 'POOL-RS' });
    expect(component.workQueuesPatio[0].pow).toBe('POW-09');
    expect(component.workQueuesPatio[0].poolOperacional).toBe('POOL-RS');
  });

  it('deve enviar equipamento editado para a work queue', async () => {
    const workQueue = criarWorkQueues()[0];
    const api = {
      atualizarEquipamentoWorkQueuePatio: jasmine.createSpy().and.resolveTo({ ...workQueue, equipamento: 'RTG-09' })
    } as unknown as SiderurgicoApiService;
    component = new AppComponent(api);
    component.workQueuesPatio = [workQueue];
    component.edicaoWorkQueue(workQueue).equipamento = 'RTG-09';
    await component.salvarEquipamentoWorkQueue(workQueue);
    expect(api.atualizarEquipamentoWorkQueuePatio).toHaveBeenCalledWith(10, { equipamento: 'RTG-09' });
    expect(component.workQueuesPatio[0].equipamento).toBe('RTG-09');
  });

  it('deve despachar work queue com limite operacional e recarregar integracao', async () => {
    const workQueue = criarWorkQueues()[0];
    const carregarIntegracaoPatio = jasmine.createSpy().and.resolveTo(undefined);
    const api = {
      despacharWorkQueuePatio: jasmine.createSpy().and.resolveTo({ workQueueId: 10, totalOrdensDespachadas: 2, ordens: workQueue.jobList })
    } as unknown as SiderurgicoApiService;
    component = new AppComponent(api);
    (component as unknown as { carregarIntegracaoPatio: () => Promise<void> }).carregarIntegracaoPatio = carregarIntegracaoPatio;
    component.workQueuesPatio = [workQueue];
    component.edicaoWorkQueue(workQueue).limiteDispatch = 2;
    await component.despacharWorkQueue(workQueue);
    expect(api.despacharWorkQueuePatio).toHaveBeenCalledWith(10, {
      limiteOrdens: 2,
      observacao: 'Dispatch acionado pela tela Control Room'
    });
    expect(carregarIntegracaoPatio).toHaveBeenCalled();
    expect(component.sucesso).toBe('2 ordem(ns) despachada(s) na work queue.');
  });

  it('deve resetar work instruction e substituir a ordem no estado local', async () => {
    const workQueue = criarWorkQueues()[0];
    const ordemResetada = { ...workQueue.jobList[0], statusOrdem: 'PENDENTE', prioridadeOperacional: 5 } as OrdemPatioDaVisita;
    const carregarIntegracaoPatio = jasmine.createSpy().and.resolveTo(undefined);
    const api = {
      resetarWorkInstructionPatio: jasmine.createSpy().and.resolveTo(ordemResetada)
    } as unknown as SiderurgicoApiService;
    component = new AppComponent(api);
    (component as unknown as { carregarIntegracaoPatio: () => Promise<void> }).carregarIntegracaoPatio = carregarIntegracaoPatio;
    component.workQueuesPatio = [workQueue];
    component.ordensPatio = [...workQueue.jobList];
    await component.resetarWorkInstruction(workQueue.jobList[0]);
    expect(api.resetarWorkInstructionPatio).toHaveBeenCalledWith(99);
    expect(component.ordensPatio[0].prioridadeOperacional).toBe(5);
    expect(component.workQueuesPatio[0].jobList[0].prioridadeOperacional).toBe(5);
    expect(component.sucesso).toBe('Work instruction resetada.');
  });

  it('deve cancelar work instruction e substituir a ordem no estado local', async () => {
    const workQueue = criarWorkQueues()[0];
    const ordemCancelada = { ...workQueue.jobList[1], statusOrdem: 'CANCELADA' } as OrdemPatioDaVisita;
    const carregarIntegracaoPatio = jasmine.createSpy().and.resolveTo(undefined);
    const api = {
      cancelarWorkInstructionPatio: jasmine.createSpy().and.resolveTo(ordemCancelada)
    } as unknown as SiderurgicoApiService;
    component = new AppComponent(api);
    (component as unknown as { carregarIntegracaoPatio: () => Promise<void> }).carregarIntegracaoPatio = carregarIntegracaoPatio;
    component.workQueuesPatio = [workQueue];
    component.ordensPatio = [...workQueue.jobList];
    await component.cancelarWorkInstruction(workQueue.jobList[1]);
    expect(api.cancelarWorkInstructionPatio).toHaveBeenCalledWith(100);
    expect(component.ordensPatio[1].statusOrdem).toBe('CANCELADA');
    expect(component.workQueuesPatio[0].jobList[1].statusOrdem).toBe('CANCELADA');
    expect(component.sucesso).toBe('Work instruction cancelada.');
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
