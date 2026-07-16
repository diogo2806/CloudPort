package br.com.cloudport.visibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.entity.EventoProcessado;
import br.com.cloudport.visibilidade.exception.EventoEnvelopeInvalidoException;
import br.com.cloudport.visibilidade.exception.EventoIdentidadeColidenteException;
import br.com.cloudport.visibilidade.repository.EventoProcessadoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventoProcessadoServiceTest {

    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Test
    void devePersistirIdentidadeAntesDeAplicarEfeito() {
        when(eventoProcessadoRepository.findByIdentidadeEvento("evt-1"))
                .thenReturn(Optional.empty());
        EventoProcessadoService service = criarService();
        AtomicInteger efeitos = new AtomicInteger();

        boolean processado = service.processarUmaVez(
                "YARD", envelope("evt-1", "CONT001"), evento -> efeitos.incrementAndGet());

        assertTrue(processado);
        assertEquals(1, efeitos.get());
        ArgumentCaptor<EventoProcessado> captor = ArgumentCaptor.forClass(EventoProcessado.class);
        verify(eventoProcessadoRepository).saveAndFlush(captor.capture());
        assertEquals("evt-1", captor.getValue().getIdentidadeEvento());
        assertEquals("yard.container.stored", captor.getValue().getTipoEvento());
        assertEquals(1, captor.getValue().getVersaoEvento());
        assertEquals("YARD", captor.getValue().getConsumidor());
        assertEquals(64, captor.getValue().getHashPayload().length());
    }

    @Test
    void deveIgnorarRedeliveryComMesmoPayload() {
        EventoProcessadoService service = criarService();
        Map<String, Object> envelope = envelope("evt-2", "CONT002");
        when(eventoProcessadoRepository.findByIdentidadeEvento("evt-2"))
                .thenReturn(Optional.empty());

        assertTrue(service.processarUmaVez("YARD", envelope, evento -> { }));
        ArgumentCaptor<EventoProcessado> captor = ArgumentCaptor.forClass(EventoProcessado.class);
        verify(eventoProcessadoRepository).saveAndFlush(captor.capture());
        when(eventoProcessadoRepository.findByIdentidadeEvento("evt-2"))
                .thenReturn(Optional.of(captor.getValue()));
        AtomicInteger efeitos = new AtomicInteger();

        boolean processado = service.processarUmaVez(
                "YARD", envelope, evento -> efeitos.incrementAndGet());

        assertFalse(processado);
        assertEquals(0, efeitos.get());
        verify(eventoProcessadoRepository, times(1)).saveAndFlush(captor.getValue());
    }

    @Test
    void deveRejeitarMesmaIdentidadeComPayloadDivergente() {
        EventoProcessadoService service = criarService();
        when(eventoProcessadoRepository.findByIdentidadeEvento("evt-3"))
                .thenReturn(Optional.empty());

        service.processarUmaVez("GATE", envelope("evt-3", "CONT003"), evento -> { });
        ArgumentCaptor<EventoProcessado> captor = ArgumentCaptor.forClass(EventoProcessado.class);
        verify(eventoProcessadoRepository).saveAndFlush(captor.capture());
        when(eventoProcessadoRepository.findByIdentidadeEvento("evt-3"))
                .thenReturn(Optional.of(captor.getValue()));

        assertThrows(EventoIdentidadeColidenteException.class,
                () -> service.processarUmaVez(
                        "GATE", envelope("evt-3", "CONT999"), evento -> { }));
    }

    @Test
    void deveExigirIdentidadeEVersaoDoEnvelope() {
        EventoProcessadoService service = criarService();

        assertThrows(EventoEnvelopeInvalidoException.class,
                () -> service.processarUmaVez(
                        "NAVIO",
                        Map.of("eventType", "navio.arrived", "eventVersion", 1),
                        evento -> { }));

        assertThrows(EventoEnvelopeInvalidoException.class,
                () -> service.processarUmaVez(
                        "NAVIO",
                        Map.of("eventId", "evt-4", "eventType", "navio.arrived"),
                        evento -> { }));
    }

    @Test
    void deveCalcularMesmoHashIndependentementeDaOrdemDasChaves() {
        EventoProcessadoService service = criarService();
        Map<String, Object> primeiro = envelope("evt-5", "CONT005");
        when(eventoProcessadoRepository.findByIdentidadeEvento("evt-5"))
                .thenReturn(Optional.empty());

        service.processarUmaVez("YARD", primeiro, evento -> { });
        ArgumentCaptor<EventoProcessado> captor = ArgumentCaptor.forClass(EventoProcessado.class);
        verify(eventoProcessadoRepository).saveAndFlush(captor.capture());
        when(eventoProcessadoRepository.findByIdentidadeEvento("evt-5"))
                .thenReturn(Optional.of(captor.getValue()));

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("posicao", "01-02-03");
        dados.put("containerId", "CONT005");
        Map<String, Object> reordenado = new LinkedHashMap<>();
        reordenado.put("data", dados);
        reordenado.put("eventVersion", 1);
        reordenado.put("eventType", "yard.container.stored");
        reordenado.put("eventId", "evt-5");

        assertFalse(service.processarUmaVez("YARD", reordenado, evento -> { }));
    }

    private EventoProcessadoService criarService() {
        return new EventoProcessadoService(eventoProcessadoRepository, new ObjectMapper());
    }

    private Map<String, Object> envelope(String eventId, String containerId) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("containerId", containerId);
        dados.put("posicao", "01-02-03");

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", "yard.container.stored");
        envelope.put("eventVersion", 1);
        envelope.put("data", dados);
        return envelope;
    }
}
