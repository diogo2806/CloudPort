package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.cidadao.AgendamentoRealtimeService;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoRepository;
import br.com.cloudport.servicogate.app.gestor.dto.EmbarqueDiretoNavioRequest;
import br.com.cloudport.servicogate.app.gestor.dto.EmbarqueDiretoNavioResponse;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.porta.navio.EmbarqueDiretoNavioPorta;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbarqueDiretoNavioServiceTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private GatePassRepository gatePassRepository;
    @Mock private GateEventRepository gateEventRepository;
    @Mock private AgendamentoRealtimeService agendamentoRealtimeService;
    @Mock private GateOperadorRealtimeService gateOperadorRealtimeService;
    @Mock private EmbarqueDiretoNavioPorta embarqueDiretoNavioPorta;

    private EmbarqueDiretoNavioService service;
    private Agendamento agendamento;
    private GatePass gatePass;
    private EmbarqueDiretoNavioRequest request;
    private LocalDateTime embarque;

    @BeforeEach
    void setUp() {
        service = new EmbarqueDiretoNavioService(
                agendamentoRepository, gatePassRepository, gateEventRepository,
                agendamentoRealtimeService, gateOperadorRealtimeService, embarqueDiretoNavioPorta);
        LocalDateTime entrada = LocalDateTime.of(2026, 7, 17, 18, 0);
        embarque = LocalDateTime.of(2026, 7, 17, 19, 0);

        agendamento = new Agendamento();
        agendamento.setId(10L);
        agendamento.setCodigo("MSCU1234567");
        agendamento.setStatus(StatusAgendamento.EM_EXECUCAO);
        agendamento.setHorarioRealChegada(entrada);

        gatePass = new GatePass();
        gatePass.setId(20L);
        gatePass.setAgendamento(agendamento);
        gatePass.setDataEntrada(entrada);
        gatePass.setStatus(StatusGate.LIBERADO);
        agendamento.setGatePass(gatePass);

        request = new EmbarqueDiretoNavioRequest();
        request.setAgendamentoId(10L);
        request.setAtribuicaoEstivaId(30L);
        request.setHorarioEmbarque(embarque);
        request.setOperador("operador.gate");

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(gatePassRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(gatePass));
    }

    @Test
    void deveFecharGateSomenteDepoisDaConfirmacaoDoNavio() {
        when(gateEventRepository.existsByGatePassIdAndObservacaoStartingWith(eq(20L), any(String.class)))
                .thenReturn(false);
        when(gateEventRepository.save(any(GateEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(embarqueDiretoNavioPorta.embarcar(any(EmbarqueDiretoNavioPorta.Comando.class)))
                .thenReturn(new EmbarqueDiretoNavioPorta.Resultado(
                        30L, 40L, "MSCU1234567", 2, 4, 6, embarque));

        EmbarqueDiretoNavioResponse response = service.embarcar(request);

        assertFalse(response.isPassouPeloPatio());
        assertEquals(StatusGate.FINALIZADO.name(), response.getStatusGate());
        assertEquals(StatusAgendamento.COMPLETO.name(), response.getStatusAgendamento());
        assertEquals(embarque, response.getSaidaGateEm());
        verify(gatePassRepository).save(gatePass);
        verify(agendamentoRepository).save(agendamento);
        verify(gateEventRepository).save(any(GateEvent.class));
    }

    @Test
    void naoDeveFinalizarGateQuandoNavioRejeitarOperacao() {
        when(gateEventRepository.existsByGatePassIdAndObservacaoStartingWith(eq(20L), any(String.class)))
                .thenReturn(false);
        when(embarqueDiretoNavioPorta.embarcar(any(EmbarqueDiretoNavioPorta.Comando.class)))
                .thenThrow(new BusinessException("A atribuição possui origem no pátio"));

        assertThrows(BusinessException.class, () -> service.embarcar(request));
        assertEquals(StatusGate.LIBERADO, gatePass.getStatus());
        assertEquals(StatusAgendamento.EM_EXECUCAO, agendamento.getStatus());
        verify(gatePassRepository, never()).save(gatePass);
        verify(agendamentoRepository, never()).save(agendamento);
        verify(gateEventRepository, never()).save(any(GateEvent.class));
    }

    @Test
    void deveSerIdempotenteQuandoMesmoEmbarqueForRepetido() {
        gatePass.setStatus(StatusGate.FINALIZADO);
        gatePass.setDataSaida(embarque);
        agendamento.setStatus(StatusAgendamento.COMPLETO);
        agendamento.setHorarioRealSaida(embarque);
        when(gateEventRepository.existsByGatePassIdAndObservacaoStartingWith(eq(20L), any(String.class)))
                .thenReturn(true);
        when(embarqueDiretoNavioPorta.embarcar(any(EmbarqueDiretoNavioPorta.Comando.class)))
                .thenReturn(new EmbarqueDiretoNavioPorta.Resultado(
                        30L, 40L, "MSCU1234567", 2, 4, 6, embarque));

        EmbarqueDiretoNavioResponse response = service.embarcar(request);

        assertTrue(response.getMensagem().contains("sem duplicidade"));
        verify(gatePassRepository, never()).save(gatePass);
        verify(agendamentoRepository, never()).save(agendamento);
        verify(gateEventRepository, never()).save(any(GateEvent.class));
    }
}
