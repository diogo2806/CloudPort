package br.com.cloudport.contracts.api;

import java.time.Instant;
import java.util.Map;

/**
 * Resposta de erro única para todos os contratos HTTP do CloudPort.
 */
public record ErroApi(
        String codigo,
        String mensagem,
        Map<String, Object> detalhes,
        String correlationId,
        Instant timestamp
) {

    public ErroApi {
        detalhes = detalhes == null ? Map.of() : Map.copyOf(detalhes);
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }
}
