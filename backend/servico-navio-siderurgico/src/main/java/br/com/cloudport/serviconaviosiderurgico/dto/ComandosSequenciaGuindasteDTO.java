package br.com.cloudport.serviconaviosiderurgico.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public final class ComandosSequenciaGuindasteDTO {

    private ComandosSequenciaGuindasteDTO() {
    }

    public record Criar(
            @NotBlank(message = "movementId e obrigatorio.") String movementId,
            @NotBlank(message = "vesselVisitId e obrigatorio.") String vesselVisitId,
            @NotBlank(message = "craneId e obrigatorio.") String craneId,
            @NotBlank(message = "loadUnitId e obrigatorio.") String loadUnitId,
            @NotNull(message = "plannedStart e obrigatorio.") LocalDateTime plannedStart,
            String notes
    ) {
    }

    public record Transicao(
            @NotBlank(message = "operatorId e obrigatorio.") String operatorId,
            String reason
    ) {
    }
}
