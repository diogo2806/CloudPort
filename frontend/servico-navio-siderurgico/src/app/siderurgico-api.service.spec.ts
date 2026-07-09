import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import {
  AlertaIntegracaoNavioPatio,
  OrdemPatioDaVisita,
  RelatorioOperacionalIntegrado,
  ReservaPatioNavio,
  ResultadoReplanejamentoPatioNavio,
  SiderurgicoApiService,
  WorkQueuePatioDaVisita
} from './siderurgico-api.service';

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

  it('deve validar contratos frontend de reservas, ordens, excecoes e alertas da integracao Patio', async () => {
    await carregarConfiguracao();

    const reserva = criarReserva();
    const criarReservas = service.gerarReservasPatio(42, 'DEFINITIVA');
    const criarReservasRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/reservas');
    expect(criarReservasRequest.request.method).toBe('POST');
    expect(criarReservasRequest.request.body).toEqual({ tipoReserva: 'DEFINITIVA', somentePendentes: true });
    criarReservasRequest.flush([reserva]);
    await expectAsync(criarReservas).toBeResolvedTo([reserva]);

    const listarReservas = service.listarReservasPatio(42);
    const listarReservasRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/reservas');
    expect(listarReservasRequest.request.method).toBe('GET');
    listarReservasRequest.flush([reserva]);
    await expectAsync(listarReservas).toBeResolvedTo([reserva]);

    const ordem = criarOrdem();
    const listarOrdens = service.listarOrdensPatio(42);
    const listarOrdensRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/ordens');
    expect(listarOrdensRequest.request.method).toBe('GET');
    listarOrdensRequest.flush([ordem]);
    await expectAsync(listarOrdens).toBeResolvedTo([ordem]);

    const semCobertura = service.listarOrdensSemCoberturaPatio(42);
    const semCoberturaRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/sem-cobertura');
    expect(semCoberturaRequest.request.method).toBe('GET');
    semCoberturaRequest.flush([ordem]);
    await expectAsync(semCobertura).toBeResolvedTo([ordem]);

    const alerta = criarAlerta();
    const alertas = service.listarAlertasIntegracaoPatio(42);
    const alertasRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/alertas');
    expect(alertasRequest.request.method).toBe('GET');
    alertasRequest.flush([alerta]);
    await expectAsync(alertas).toBeResolvedTo([alerta]);
  });

  it('deve validar contratos frontend de geracao de ordens, sincronizacao, replanejamento e relatorio integrado', async () => {
    await carregarConfiguracao();

    const geracao = service.gerarOrdensPatio(42, 'DESCARGA');
    const geracaoRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/gerar-ordens');
    expect(geracaoRequest.request.method).toBe('POST');
    expect(geracaoRequest.request.body).toEqual({ tipoMovimento: 'DESCARGA', modo: 'SOMENTE_PENDENTES', gerarReservasAutomaticas: true });
    geracaoRequest.flush({ totalOrdensCriadas: 1, totalItensIgnorados: 0, totalItensComErro: 0, errosPorItem: [], alertas: [] });
    await expectAsync(geracao).toBeResolved();

    const sincronizacao = service.sincronizarStatusPatio(42);
    const sincronizacaoRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/sincronizar-status');
    expect(sincronizacaoRequest.request.method).toBe('POST');
    expect(sincronizacaoRequest.request.body).toEqual({});
    sincronizacaoRequest.flush({ visitaNavioId: 42, totalItens: 1, itensComReserva: 1, itensComOrdem: 1, itensSemReserva: 0, itensSemOrdem: 0, ordensEmExecucao: 1, ordensConcluidas: 0, totalAlertas: 0, statusPredominante: 'EM_EXECUCAO' });
    await expectAsync(sincronizacao).toBeResolved();

    const replanejamentoResposta = criarResultadoReplanejamento();
    const replanejamento = service.replanejarPatioVisita(42, true);
    const replanejamentoRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/integracao-patio/replanejar');
    expect(replanejamentoRequest.request.method).toBe('POST');
    expect(replanejamentoRequest.request.body).toEqual({ aplicar: true });
    replanejamentoRequest.flush(replanejamentoResposta);
    await expectAsync(replanejamento).toBeResolvedTo(replanejamentoResposta);

    const relatorio = criarRelatorioIntegrado();
    const consultaRelatorio = service.obterRelatorioOperacionalIntegrado(42);
    const relatorioRequest = http.expectOne('http://localhost:8081/api/visitas-navio/42/relatorio-operacional-integrado');
    expect(relatorioRequest.request.method).toBe('GET');
    relatorioRequest.flush(relatorio);
    await expectAsync(consultaRelatorio).toBeResolvedTo(relatorio);
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
    jobList: [criarOrdem()]
  };
}

function criarOrdem(): OrdemPatioDaVisita {
  return {
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
  };
}

function criarReserva(): ReservaPatioNavio {
  return {
    id: 20,
    visitaNavioId: 42,
    itemOperacaoNavioId: 7,
    posicaoPatioId: 'A-01-01',
    bloco: 'A',
    linha: 1,
    coluna: 1,
    camada: '1',
    tipoReserva: 'DEFINITIVA',
    status: 'ATIVA'
  };
}

function criarAlerta(): AlertaIntegracaoNavioPatio {
  return {
    tipo: 'SEM_EQUIPAMENTO',
    severidade: 'ALTA',
    visitaNavioId: 42,
    itemOperacaoNavioId: 7,
    ordemTrabalhoPatioId: 99,
    mensagem: 'Work instruction sem CHE associado.'
  };
}

function criarResultadoReplanejamento(): ResultadoReplanejamentoPatioNavio {
  return {
    reservasSugeridas: [criarReserva()],
    ordensReordenadas: [criarOrdem()],
    economiaEstimadaDistanciaPercentual: 12,
    riscoRehandle: 'BAIXO',
    alertasImpeditivos: [],
    itensNaoReplanejados: []
  };
}

function criarRelatorioIntegrado(): RelatorioOperacionalIntegrado {
  return {
    visita: {
      id: 42,
      navioId: 1,
      navioNome: 'MV CloudPort',
      codigoVisita: 'VIS-42',
      fase: 'OPERANDO'
    },
    resumoOperacional: {
      totalItensPlanejados: 1,
      totalItensOperados: 0,
      pesoPlanejado: 10,
      pesoOperado: 0,
      percentualProgresso: 0,
      divergenciasPoraoPosicao: 0,
      itensBloqueados: 0,
      tempoOperacaoMinutos: null
    },
    resumoIntegracao: {
      visitaNavioId: 42,
      totalItens: 1,
      itensComReserva: 1,
      itensComOrdem: 1,
      itensSemReserva: 0,
      itensSemOrdem: 0,
      ordensEmExecucao: 0,
      ordensConcluidas: 0,
      totalAlertas: 1,
      statusPredominante: 'ORDEM_GERADA'
    },
    planoEstiva: null,
    itens: [],
    reservasPatio: [criarReserva()],
    ordensPatio: [criarOrdem()],
    divergenciasAlertas: [criarAlerta()],
    eventosRelevantes: []
  };
}
