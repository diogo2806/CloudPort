package br.com.cloudport.visibilidade.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.cloudport.visibilidade.dto.evento.EventoRecebido;
import br.com.cloudport.visibilidade.service.AlertasService;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import br.com.cloudport.visibilidade.service.EventoProcessadoService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
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
    private EventoProcessadoService eventoProcessadoService;

    @BeforeEach
    void configurarProcessamentoIdempotente() {
        doAnswer(invocation -> {
            Map<String, Object> envelope = invocation.getArgument(1);
            Consumer<EventoRecebido> processador = invocation.getArgument(2);
            processador.accept(EventoRecebido.de(envelope));
            return true;
        }).when(eventoProcessadoService).processarUmaVez(anyString(), anyMap(), any());
    }

    @Test
    void deveAtualizarCapacidadeMesmoSemContainerId() {
        YardEventListener listener = new YardEventListener(
                movimentoConteinerService, capacidadeYardService, eventoProcessadoService);

        listener.handleYardEvent(Map.of(
                "eventId", "evt-yard-capacidade-1",
                "eventType", "yard.capacity_updated",
                "eventVersion", 1,
                "data", Map.of(
                        "zona", "A01",
                        "ocupacaoAtual", 120L)));

        verify(capacidadeYardService).atualizarOcupacao("A01", 120);
        verify(movimentoConteinerService, never()).registrarArmazenagemYard(
                anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    void devePersistirMovimentoFerroviario() {
        RailEventListener listener = new RailEventListener(
                movimentoConteinerService, eventoProcessadoService);

        listener.handleRailEvent(Map.of(
                "eventId", "evt-rail-1",
                "eventType", "rail.container.moved",
                "eventVersion", 1,
                "containerId", "CONT001",
                "origem", "LINHA-1",
                "destino", "PATIO-B",
                "locomotivaId", "LOCO-7"));

        verify(movimentoConteinerService).registrarMovimentoRail(
                "evt-rail-1", "CONT001", "LINHA-1", "PATIO-B", "LOCO-7", null);
    }

    @Test
    void deveAtribuirBercoSemAlterarStatusOperacional() {
        NavioEventListener listener = new NavioEventListener(
                statusNavioService, alertasService, eventoProcessadoService);

        listener.handleNavioEvent(Map.of(
                "messageId", "msg-navio-berco-1",
                "eventType", "navio.berth_assigned",
                "eventVersion", 1,
                "navioId", "NAV-10",
                "berco", "B-03"));

        verify(statusNavioService).atualizarStatusNavio("NAV-10", null, "B-03");
    }

    @Test
    void deveResolverAlertaDeAtrasoQuandoNavioChegar() {
        NavioEventListener listener = new NavioEventListener(
                statusNavioService, alertasService, eventoProcessadoService);

        listener.handleNavioEvent(Map.of(
                "eventId", "evt-navio-chegada-1",
                "eventType", "navio.arrived",
                "eventVersion", 1,
                "navioId", "NAV-11"));

        verify(statusNavioService).atualizarStatusNavio("NAV-11", "ancorando", null);
        verify(alertasService).resolverAlertasAtivos("NAV-11", "ATRASO_NAVIO");
    }
}
