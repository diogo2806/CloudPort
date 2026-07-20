package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.app.gestor.dto.GatePassDTO;
import br.com.cloudport.servicogate.model.enums.GateQueueDirection;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreGateQueueServiceTest {

    @Mock
    private GateFlowService gateFlowService;

    @Mock
    private GateOperationsService gateOperationsService;

    private PreGateQueueService service;

    @BeforeEach
    void setUp() {
        service = new PreGateQueueService(gateFlowService, gateOperationsService);
    }

    @Test
    void deveIncluirGatePassNaFilaDeEntradaAposChegadaAntecipada() {
        AgendamentoDTO agendamento = criarAgendamento(42L, null);
        when(gateFlowService.confirmarChegadaAntecipada(10L)).thenReturn(agendamento);

        AgendamentoDTO resposta = service.confirmarChegadaAntecipada(10L);

        assertSame(agendamento, resposta);
        verify(gateOperationsService).adicionarNaFila(42L, GateQueueDirection.ENTRADA);
    }

    @Test
    void naoDeveReincluirNaFilaQuandoEntradaFisicaJaFoiRegistrada() {
        AgendamentoDTO agendamento = criarAgendamento(42L, LocalDateTime.now());
        when(gateFlowService.confirmarChegadaAntecipada(10L)).thenReturn(agendamento);

        service.confirmarChegadaAntecipada(10L);

        verify(gateOperationsService, never()).adicionarNaFila(42L, GateQueueDirection.ENTRADA);
    }

    @Test
    void naoDeveIncluirNaFilaQuandoNaoHaGatePass() {
        AgendamentoDTO agendamento = new AgendamentoDTO();
        when(gateFlowService.confirmarChegadaAntecipada(10L)).thenReturn(agendamento);

        service.confirmarChegadaAntecipada(10L);

        verify(gateOperationsService, never()).adicionarNaFila(42L, GateQueueDirection.ENTRADA);
    }

    private AgendamentoDTO criarAgendamento(Long gatePassId, LocalDateTime dataEntrada) {
        GatePassDTO gatePass = new GatePassDTO();
        gatePass.setId(gatePassId);
        gatePass.setDataEntrada(dataEntrada);

        AgendamentoDTO agendamento = new AgendamentoDTO();
        agendamento.setGatePass(gatePass);
        return agendamento;
    }
}
