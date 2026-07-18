package br.com.cloudport.visibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.dto.AlertaDTO;
import br.com.cloudport.visibilidade.dto.ThroughputGateDTO;
import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class VisibilidadeDashboardServiceTest {

    @Mock
    private StatusNavioRepository statusNavioRepository;

    @Mock
    private CapacidadeYardRepository capacidadeYardRepository;

    @Mock
    private ConteinerLocalizacaoRepository conteinerLocalizacaoRepository;

    @Mock
    private HistoricoMovimentoRepository historicoMovimentoRepository;

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RastreamentoConteinerService rastreamentoConteinerService;

    @Mock
    private AlertasService alertasService;

    @AfterEach
    void limparContextoTransacional() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void deveCalcularThroughputSomenteComMovimentosReaisDoGate() {
        LocalDateTime referencia = LocalDateTime.now().withSecond(0).withNano(0);
        HistoricoMovimento entradaUm = movimento(
                "CONT-1", MovimentoConteinerService.TIPO_ENTRADA_GATE, referencia.minusMinutes(60));
        HistoricoMovimento saidaUm = movimento(
                "CONT-1", MovimentoConteinerService.TIPO_SAIDA_GATE, referencia.minusMinutes(30));
        HistoricoMovimento entradaDois = movimento(
                "CONT-2", MovimentoConteinerService.TIPO_ENTRADA_GATE, referencia.minusMinutes(10));
        HistoricoMovimento patio = movimento(
                "CONT-3", MovimentoConteinerService.TIPO_ARMAZENAGEM_YARD, referencia.minusMinutes(5));
        when(historicoMovimentoRepository.findAll())
                .thenReturn(List.of(entradaUm, saidaUm, entradaDois, patio));

        VisibilidadeDashboardService service = criarService(4);

        ThroughputGateDTO resultado = service.obterThroughputGate();

        assertEquals(2, resultado.getEntradasHoje());
        assertEquals(1, resultado.getSaidasHoje());
        assertEquals(3, resultado.getMovimentosHoje());
        assertEquals(75d, resultado.getDesempenhoPercentual());
        assertEquals(30d, resultado.getTempoMedioProcessamentoMinutos());
        assertEquals("ABAIXO_DA_META", resultado.getStatus());
        assertEquals(entradaDois.getTimestamp(), resultado.getDataAtualizacao());
    }

    @Test
    void naoDeveInventarTempoMedioQuandoNaoHaCicloCompleto() {
        HistoricoMovimento entrada = movimento(
                "CONT-4", MovimentoConteinerService.TIPO_ENTRADA_GATE, LocalDateTime.now());
        when(historicoMovimentoRepository.findAll()).thenReturn(List.of(entrada));

        ThroughputGateDTO resultado = criarService(10).obterThroughputGate();

        assertNull(resultado.getTempoMedioProcessamentoMinutos());
    }

    @Test
    void devePublicarDiretamenteQuandoNaoHaTransacaoAtiva() {
        VisibilidadeDashboardService service = spy(criarService(10));
        doNothing().when(service).publicarDashboard();

        service.publicarDashboardAposCommit();

        verify(service).publicarDashboard();
    }

    @Test
    void resolverAlertaDevePublicarSomenteAposCommit() {
        Alerta alerta = alertaAtivo(15L);
        when(alertaRepository.findById(15L)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(alerta)).thenReturn(alerta);
        VisibilidadeDashboardService service = spy(criarService(10));
        doNothing().when(service).publicarDashboard();
        iniciarTransacaoSimulada();

        AlertaDTO resultado = service.resolverAlerta(15L, "Operacao normalizada");

        assertEquals("resolvido", resultado.getStatus());
        verify(service, never()).publicarDashboard();

        confirmarTransacaoSimulada();

        verify(service).publicarDashboard();
    }

    @Test
    void detectarAlertasAutomaticosNaoDevePublicarQuandoHouverRollback() {
        VisibilidadeDashboardService service = spy(criarService(10));
        iniciarTransacaoSimulada();

        service.detectarAlertasAutomaticos();

        verify(alertasService).detectarAtrasos();
        verify(alertasService).detectarGargalos();
        verify(service, never()).publicarDashboard();

        reverterTransacaoSimulada();

        verify(service, never()).publicarDashboard();
    }

    @Test
    void naoDeveUsarFallbackDiretoQuandoHaTransacaoSemSincronizacao() {
        VisibilidadeDashboardService service = spy(criarService(10));
        TransactionSynchronizationManager.setActualTransactionActive(true);

        assertThrows(IllegalStateException.class, service::publicarDashboardAposCommit);
        verify(service, never()).publicarDashboard();
    }

    private VisibilidadeDashboardService criarService(int metaDiaria) {
        return new VisibilidadeDashboardService(
                statusNavioRepository,
                capacidadeYardRepository,
                conteinerLocalizacaoRepository,
                historicoMovimentoRepository,
                alertaRepository,
                messagingTemplate,
                rastreamentoConteinerService,
                alertasService,
                metaDiaria);
    }

    private void iniciarTransacaoSimulada() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void confirmarTransacaoSimulada() {
        List<TransactionSynchronization> sincronizacoes = TransactionSynchronizationManager.getSynchronizations();
        sincronizacoes.forEach(TransactionSynchronization::afterCommit);
        sincronizacoes.forEach(sincronizacao ->
                sincronizacao.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
    }

    private void reverterTransacaoSimulada() {
        TransactionSynchronizationManager.getSynchronizations().forEach(sincronizacao ->
                sincronizacao.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private Alerta alertaAtivo(Long id) {
        Alerta alerta = new Alerta();
        alerta.setId(id);
        alerta.setTipo("ATRASO_NAVIO");
        alerta.setSeveridade("alta");
        alerta.setEntidadeId("NAVIO-1");
        alerta.setDescricao("Atraso operacional");
        alerta.setDataGerada(LocalDateTime.now().minusHours(1));
        alerta.setStatus("ativo");
        alerta.setAcaoSugerida("Revisar sequencia");
        return alerta;
    }

    private HistoricoMovimento movimento(String containerId, String tipo, LocalDateTime timestamp) {
        HistoricoMovimento movimento = new HistoricoMovimento();
        movimento.setContainerId(containerId);
        movimento.setTipo(tipo);
        movimento.setTimestamp(timestamp);
        return movimento;
    }
}
