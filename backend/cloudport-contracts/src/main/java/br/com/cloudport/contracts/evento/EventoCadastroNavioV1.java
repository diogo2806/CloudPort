package br.com.cloudport.contracts.evento;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Evento interno versionado publicado após alteração do cadastro canônico de navios. */
public record EventoCadastroNavioV1(
        UUID eventId,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        Long navioCadastroId,
        String codigoImo,
        String tipoAlteracao,
        Set<String> camposAlterados
) {
    public EventoCadastroNavioV1 {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        if (eventVersion < 1) {
            throw new IllegalArgumentException("A versao do evento deve ser maior que zero.");
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        if (navioCadastroId == null) {
            throw new IllegalArgumentException("O identificador canônico do navio e obrigatorio.");
        }
        if (tipoAlteracao == null || tipoAlteracao.isBlank()) {
            throw new IllegalArgumentException("O tipo da alteracao do navio e obrigatorio.");
        }
        camposAlterados = camposAlterados == null ? Set.of() : Set.copyOf(camposAlterados);
    }

    public static EventoCadastroNavioV1 criar(Long navioCadastroId,
                                               String codigoImo,
                                               String tipoAlteracao,
                                               Set<String> camposAlterados,
                                               String correlationId) {
        return new EventoCadastroNavioV1(
                UUID.randomUUID(), 1, Instant.now(), correlationId, navioCadastroId,
                codigoImo, tipoAlteracao, camposAlterados);
    }
}
