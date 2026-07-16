package br.com.cloudport.contracts.evento;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Envelope público versionado para SSE, WebSocket e integrações externas.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventoIntegracaoV1<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        String source,
        T data
) {

    public EventoIntegracaoV1 {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("O tipo do evento e obrigatorio.");
        }
        if (eventVersion < 1) {
            throw new IllegalArgumentException("A versao do evento deve ser maior que zero.");
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        source = source == null || source.isBlank() ? "cloudport" : source.trim();
    }

    public static <T> EventoIntegracaoV1<T> criar(String tipo,
                                                   String correlationId,
                                                   String origem,
                                                   T dados) {
        return new EventoIntegracaoV1<>(UUID.randomUUID(), tipo, 1, Instant.now(), correlationId, origem, dados);
    }
}
