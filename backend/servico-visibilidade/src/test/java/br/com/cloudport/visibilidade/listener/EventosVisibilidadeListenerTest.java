package br.com.cloudport.visibilidade.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.cloudport.visibilidade.service.AlertasService;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import br.com.cloudport.visibilidade.service.StatusNavioService;
import java.util.Map;
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

    @Test
    void deveAtualizarCapacidadeMesmoSemContainerId() {
        YardEventListener listener = new YardEventListener(
                movimentoConteinerService, capacidadeYardService);

        listener.handleYardEvent(Map.of(
                "eventType", "yard.capacity_updated",
                "zona", "A01",
                "ocupacaoAtual", 120L));

        verify(capacidadeYardService).atualizarOcupacao("A01", 120);
        verify(movimentoConteinerService, never()).registrarArmazenagemYard(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void devePersistirMovimentoFerroviario() {
        RailEventListener listener = new RailEventListener(movimentoConteinerService);

        listener.handleRailEvent(Map.of(
                "eventType", "rail.container.moved",
                "containerId", "CONT001",
                "origem", "LINHA-1",
                "destino", "PATIO-B",
                "locomotivaId", "LOCO-7"));

        verify(movimentoConteinerService).registrarMovimentoRail(
                "CONT001", "LINHA-1", "PATIO-B", "LOCO-7", null);
    }

    @Test
    void deveAtribuirBercoSemAlterarStatusOperacional() {
        NavioEventListener listener = new NavioEventListener(statusNavioService, alertasService);

        listener.handleNavioEvent(Map.of(
                "eventType", "navio.berth_assigned",
                "navioId", "NAV-10",
                "berco", "B-03"));

        verify(statusNavioService).atualizarStatusNavio("NAV-10", null, "B-03");
    }

    @Test
    void deveResolverAlertaDeAtrasoQuandoNavioChegar() {
        NavioEventListener listener = new NavioEventListener(statusNavioService, alertasService);

        listener.handleNavioEvent(Map.of(
                "eventType", "navio.arrived",
                "navioId", "NAV-11"));

        verify(statusNavioService).atualizarStatusNavio("NAV-11", "ancorando", null);
        verify(alertasService).resolverAlertasAtivos("NAV-11", "ATRASO_NAVIO");
    }
}
