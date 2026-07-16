package br.com.cloudport.visibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.entity.EventoProcessado;
import br.com.cloudport.visibilidade.exception.ConflitoIdentidadeEventoException;
import br.com.cloudport.visibilidade.repository.EventoProcessadoInsercaoRepository;
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
class ProcessamentoEventoIdempotenteServiceTest {

    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Mock
    private EventoProcessadoInsercaoRepository eventoProcessadoInsercaoRepository;

    @Test
    void deveExecutarEfeitoSomenteNaPrimeiraEntrega() {
        when(eventoProcessadoInsercaoRepository.inserirSeAusente(
                eq("evt-1"), eq("yard.container.stored"), anyString()))
                .thenReturn(1);
        ProcessamentoEventoIdempotenteService service = criarService();
        AtomicInteger execucoes = new AtomicInteger();

        boolean processado = service.processarUmaVez(Map.of(
                "eventId", "evt-1",
                "eventType", "yard.container.stored",
                "containerId", "CONT001"),
                identidade -> {
                    assertEquals("evt-1", identidade);
                    execucoes.incrementAndGet();
                });

        assertTrue(processado);
        assertEquals(1, execucoes.get());
        verify(eventoProcessadoRepository, never()).findById("evt-1");
    }

    @Test
    void deveIgnorarRedeliveryComMesmoPayloadIndependentementeDaOrdemDosCampos() {
        when(eventoProcessadoInsercaoRepository.inserirSeAusente(
                eq("evt-2"), eq("gate.container.entered"), anyString()))
                .thenReturn(1, 0);
        ProcessamentoEventoIdempotenteService service = criarService();
        AtomicInteger execucoes = new AtomicInteger();

        Map<String, Object> primeiraEntrega = new LinkedHashMap<>();
        primeiraEntrega.put("eventId", "evt-2");
        primeiraEntrega.put("eventType", "gate.container.entered");
        primeiraEntrega.put("containerId", "CONT002");
        primeiraEntrega.put("operatorId", "operador-1");

        assertTrue(service.processarUmaVez(primeiraEntrega,
                identidade -> execucoes.incrementAndGet()));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventoProcessadoInsercaoRepository).inserirSeAusente(
                eq("evt-2"), eq("gate.container.entered"), hashCaptor.capture());

        EventoProcessado existente = new EventoProcessado();
        existente.setIdentidadeEvento("evt-2");
        existente.setTipoEvento("gate.container.entered");
        existente.setHashPayload(hashCaptor.getValue());
        when(eventoProcessadoRepository.findById("evt-2")).thenReturn(Optional.of(existente));

        Map<String, Object> redelivery = new LinkedHashMap<>();
        redelivery.put("operatorId", "operador-1");
        redelivery.put("containerId", "CONT002");
        redelivery.put("eventType", "gate.container.entered");
        redelivery.put("messageId", "evt-2");

        boolean processado = service.processarUmaVez(redelivery,
                identidade -> execucoes.incrementAndGet());

        assertFalse(processado);
        assertEquals(1, execucoes.get());
        verify(eventoProcessadoInsercaoRepository, times(2)).inserirSeAusente(
                eq("evt-2"), eq("gate.container.entered"), anyString());
    }

    @Test
    void deveRejeitarColisaoDeIdentidadeComPayloadDiferente() {
        when(eventoProcessadoInsercaoRepository.inserirSeAusente(
                eq("evt-3"), eq("navio.arrived"), anyString()))
                .thenReturn(0);
        EventoProcessado existente = new EventoProcessado();
        existente.setIdentidadeEvento("evt-3");
        existente.setTipoEvento("navio.arrived");
        existente.setHashPayload("hash-de-outro-payload");
        when(eventoProcessadoRepository.findById("evt-3")).thenReturn(Optional.of(existente));
        ProcessamentoEventoIdempotenteService service = criarService();

        assertThrows(ConflitoIdentidadeEventoException.class,
                () -> service.processarUmaVez(Map.of(
                        "eventId", "evt-3",
                        "eventType", "navio.arrived",
                        "navioId", "NAV-10"),
                        identidade -> {
                        }));
    }

    @Test
    void deveExigirIdentidadeNoEnvelope() {
        ProcessamentoEventoIdempotenteService service = criarService();

        assertThrows(IllegalArgumentException.class,
                () -> service.processarUmaVez(Map.of(
                        "eventType", "yard.capacity_updated",
                        "zona", "A01",
                        "ocupacaoAtual", 10),
                        identidade -> {
                        }));
    }

    @Test
    void deveRejeitarEventIdEMessageIdDivergentes() {
        ProcessamentoEventoIdempotenteService service = criarService();

        assertThrows(ConflitoIdentidadeEventoException.class,
                () -> service.processarUmaVez(Map.of(
                        "eventId", "evt-4",
                        "messageId", "msg-4",
                        "eventType", "rail.container.moved"),
                        identidade -> {
                        }));
    }

    @Test
    void devePropagarFalhaDoEfeitoParaPermitirRollbackDaIdentidade() {
        when(eventoProcessadoInsercaoRepository.inserirSeAusente(
                eq("evt-5"), eq("yard.container.stored"), anyString()))
                .thenReturn(1);
        ProcessamentoEventoIdempotenteService service = criarService();

        assertThrows(IllegalStateException.class,
                () -> service.processarUmaVez(Map.of(
                        "eventId", "evt-5",
                        "eventType", "yard.container.stored",
                        "containerId", "CONT005"),
                        identidade -> {
                            throw new IllegalStateException("falha ao persistir efeito");
                        }));
    }

    private ProcessamentoEventoIdempotenteService criarService() {
        return new ProcessamentoEventoIdempotenteService(
                eventoProcessadoRepository,
                eventoProcessadoInsercaoRepository,
                new ObjectMapper());
    }
}
