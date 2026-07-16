package br.com.cloudport.visibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.dto.ThroughputGateDTO;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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

    private HistoricoMovimento movimento(String containerId, String tipo, LocalDateTime timestamp) {
        HistoricoMovimento movimento = new HistoricoMovimento();
        movimento.setContainerId(containerId);
        movimento.setTipo(tipo);
        movimento.setTimestamp(timestamp);
        return movimento;
    }
}
