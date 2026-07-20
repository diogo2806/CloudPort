package br.com.cloudport.servicoyard.vesselplanner.dto;

import br.com.cloudport.servicoyard.vesselplanner.modelo.NaturezaParalisacaoGuindaste;
import java.time.LocalDateTime;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class EventoOperacionalGuindasteDtos {

    private EventoOperacionalGuindasteDtos() {
    }

    public record RegistrarParalisacaoRequest(
            @NotNull @Min(1) Integer guindasteId,
            @NotNull NaturezaParalisacaoGuindaste natureza,
            LocalDateTime inicio,
            LocalDateTime fim,
            @NotBlank @Size(max = 1000) String motivo,
            @NotBlank @Size(max = 1000) String impacto,
            @NotBlank @Size(max = 120) String turno,
            @Size(max = 2000) String pendencias,
            @Size(max = 1000) String observacao) {
    }

    public record EncerrarParalisacaoRequest(
            LocalDateTime fim,
            @Size(max = 1000) String observacao) {
    }

    public record RegistrarHandoverRequest(
            @NotNull @Min(1) Integer guindasteId,
            LocalDateTime ocorridoEm,
            @NotBlank @Size(max = 120) String turnoOrigem,
            @NotBlank @Size(max = 120) String turnoDestino,
            @NotBlank @Size(max = 120) String responsavelDestino,
            @NotBlank @Size(max = 2000) String pendencias,
            @Size(max = 1000) String observacao) {
    }

    public record EventoOperacionalGuindasteResponse(
            Long id,
            Long execucaoId,
            Long planId,
            Long versao,
            Integer guindasteId,
            String tipo,
            String natureza,
            String estado,
            LocalDateTime inicio,
            LocalDateTime fim,
            String motivo,
            String impacto,
            String turnoOrigem,
            String turnoDestino,
            String responsavel,
            String responsavelDestino,
            String pendencias,
            String observacao,
            String encerradoPor,
            String observacaoEncerramento,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm) {
    }
}
