package br.com.cloudport.visibilidade.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.cloudport.visibilidade.service.AlertasService;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import br.com.cloudport.visibilidade.service.ProcessamentoEventoIdempotenteService;
import br.com.cloudport.visibilidade.service.StatusNavioService;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventosVisibilidadeListenerTest {

    @Mock
    private MovimentoConteinerService movimentoConteinerService;

    @Mock
    private CapacidadeYardService capacidadeYardService;

    @Mock
    private StatusNavioService statusNavioService;

    @Mock
    private AlertasService alertasService;

    @Mock
    private ProcessamentoEventoIdempotenteService processamentoEventoIdempotenteService;

    @BeforeEach
    void executarProcessamentoIdempotenteNosTestesDosListeners() {
        doAnswer(invocation -> {
            Map<String, Object> evento = invocation.getArgument(0);
            Consumer<String> processamento = invocation.getArgument(1);
            Object identidade = evento.containsKey("eventId")
                    ? evento.get("eventId")
                    : evento.get("messageId");
            processamento.accept(String.valueOf(identidade));
            return true;
        }).when(processamentoEventoIdempotenteService).processarUmaVez(anyMap(), any());
    }

    @Test
    void deveAtualizarCapacidadeMesmoSemContainerId() {
        YardEventListener listener = new YardEventListener(
                movimentoConteinerService, capacidadeYardService,
                processamentoEventoIdempotenteService);

        listener.handleYardEvent(Map.of(
                "eventId", "evt-yard-capacidade-1",
                "eventType", "yard.capacity_updated",
                "zona", "A01",
                "ocupacaoAtual", 120L));

        verify(capacidadeYardService).atualizarOcupacao("A01", 120);
        verify(movimentoConteinerService, never()).registrarArmazenagemYard(
                anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    void devePersistirMovimentoFerroviario() {
        RailEventListener listener = new RailEventListener(
                movimentoConteinerService, processamentoEventoIdempotenteService);

        listener.handleRailEvent(Map.of(
                "messageId", "msg-rail-1",
                "eventType", "rail.container.moved",
                "containerId", "CONT001",
                "origem", "LINHA-1",
                "destino", "PATIO-B",
                "locomotivaId", "LOCO-7"));

        verify(movimentoConteinerService).registrarMovimentoRail(
                "msg-rail-1", "CONT001", "LINHA-1", "PATIO-B", "LOCO-7", null);
    }

    @Test
    void deveRegistrarEntradaDoGateComIdentidadeDoEvento() {
        GateEventListener listener = new GateEventListener(
                movimentoConteinerService, processamentoEventoIdempotenteService);

        listener.handleGateEvent(Map.of(
                "eventId", "evt-gate-1",
                "eventType", "gate.container.entered",
                "containerId", "CONT002",
                "operatorId", "operador-1"));

        verify(movimentoConteinerService).registrarEntradaGate(
                "evt-gate-1", "CONT002", "operador-1");
    }

    @Test
    void deveAtribuirBercoSemAlterarStatusOperacional() {
        NavioEventListener listener = new NavioEventListener(
                statusNavioService, alertasService, processamentoEventoIdempotenteService);

        listener.handleNavioEvent(Map.of(
                "eventId", "evt-navio-berco-1",
                "eventType", "navio.berth_assigned",
                "navioId", "NAV-10",
                "berco", "B-03"));

        verify(statusNavioService).atualizarStatusNavio("NAV-10", null, "B-03");
    }

    @Test
    void deveResolverAlertaDeAtrasoQuandoNavioChegar() {
        NavioEventListener listener = new NavioEventListener(
                statusNavioService, alertasService, processamentoEventoIdempotenteService);

        listener.handleNavioEvent(Map.of(
                "eventId", "evt-navio-chegada-1",
                "eventType", "navio.arrived",
                "navioId", "NAV-11"));

        verify(statusNavioService).atualizarStatusNavio("NAV-11", "ancorando", null);
        verify(alertasService).resolverAlertasAtivos("NAV-11", "ATRASO_NAVIO");
    }
}
