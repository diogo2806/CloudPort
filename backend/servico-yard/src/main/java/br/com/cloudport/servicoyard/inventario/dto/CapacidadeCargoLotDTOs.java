package br.com.cloudport.servicoyard.inventario.dto;

import java.math.BigDecimal;
import java.util.UUID;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class CapacidadeCargoLotDTOs {

    private CapacidadeCargoLotDTOs() {
    }

    public enum StatusReservaCapacidade {
        RESERVADA,
        CONFIRMADA,
        CANCELADA
    }

    public record ConfigurarCapacidadeRequest(
            @NotNull @DecimalMin("0.000") BigDecimal capacidadeQuantidade,
            @NotNull @DecimalMin("0.000") BigDecimal capacidadeVolumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal capacidadePesoKg,
            @Size(max = 1000) String restricoes,
            boolean ativa) {
    }

    public record ReservarCapacidadeRequest(
            @NotNull UUID commandId,
            @NotNull UUID loteId,
            @NotNull @DecimalMin("0.001") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record ComandoCapacidadeRequest(
            @NotBlank @Size(max = 120) String usuario,
            @NotBlank @Size(max = 1000) String motivo) {
    }

    public record ReservaCapacidadeResposta(
            UUID id,
            UUID commandId,
            UUID loteId,
            String posicao,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            StatusReservaCapacidade status,
            String restricoes) {
    }
}
