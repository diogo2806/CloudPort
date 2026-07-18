package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.StatusSequenciaGuindaste;
import java.time.LocalDateTime;

public record SequenciaGuindasteDTO(
        String movementId,
        String vesselVisitId,
        String craneId,
        String loadUnitId,
        LocalDateTime plannedStart,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        StatusSequenciaGuindaste status,
        String operatorId,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {
    public record Auditoria(
            String type,
            StatusSequenciaGuindaste statusBefore,
            StatusSequenciaGuindaste statusAfter,
            String operatorId,
            String reason,
            LocalDateTime occurredAt
    ) {
    }
}
