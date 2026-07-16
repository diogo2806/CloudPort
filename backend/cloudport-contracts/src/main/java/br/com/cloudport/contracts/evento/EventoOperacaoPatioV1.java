package br.com.cloudport.contracts.evento;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento interno versionado publicado após alterações operacionais do Yard.
 */
public record EventoOperacaoPatioV1(
        UUID eventId,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        Long visitaNavioId,
        Long workQueueId,
        Long workInstructionId,
        String tipoAlteracao,
        String statusAnterior,
        String statusAtual
) {

    public EventoOperacaoPatioV1 {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        if (eventVersion < 1) {
            throw new IllegalArgumentException("A versao do evento deve ser maior que zero.");
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        if (visitaNavioId == null) {
            throw new IllegalArgumentException("A visita do navio e obrigatoria no evento do patio.");
        }
        if (tipoAlteracao == null || tipoAlteracao.isBlank()) {
            throw new IllegalArgumentException("O tipo da alteracao do patio e obrigatorio.");
        }
    }

    public static EventoOperacaoPatioV1 criar(Long visitaNavioId,
                                               Long workQueueId,
                                               Long workInstructionId,
                                               String tipoAlteracao,
                                               String statusAnterior,
                                               String statusAtual,
                                               String correlationId) {
        return new EventoOperacaoPatioV1(
                UUID.randomUUID(),
                1,
                Instant.now(),
                correlationId,
                visitaNavioId,
                workQueueId,
                workInstructionId,
                tipoAlteracao,
                statusAnterior,
                statusAtual
        );
    }
}
