package br.com.cloudport.serviconaviosiderurgico.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record EventoOperacionalVersionadoDTO(
        String schemaVersion,
        long sequencia,
        String eventId,
        Long visitaNavioId,
        Long itemOperacaoNavioId,
        String agregado,
        String tipoEvento,
        LocalDateTime ocorridoEm,
        String usuario,
        String correlationId,
        Map<String, Object> dados
) {
}
