package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import br.com.cloudport.servicocargageral.dominio.StatusOrdemFerroviariaCarga;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class FerroviaCargaGeralDTOs {

    private FerroviaCargaGeralDTOs() {
    }

    public record PlanejarOrdemFerroviariaRequest(
            @NotNull UUID loteId,
            @NotBlank @Size(max = 120) String vagaoId,
            @Size(max = 120) String posicao,
            @Min(1) int sequencia,
            @NotNull @DecimalMin("0.001") BigDecimal capacidadePesoKg,
            @Size(max = 1000) String incompatibilidades,
            @NotBlank @Size(max = 120) String custodia,
            @NotBlank @Size(max = 120) String responsavel) {
    }

    public record AtualizarStatusOrdemFerroviariaRequest(
            @NotNull StatusOrdemFerroviariaCarga status,
            @Size(max = 120) String custodia,
            @Size(max = 1000) String motivo,
            @NotBlank @Size(max = 120) String responsavel) {
    }

    public record OrdemFerroviariaCargaResposta(
            UUID loteId,
            String codigoLote,
            NaturezaCarga naturezaCarga,
            StatusOrdemFerroviariaCarga status,
            String visitaTremId,
            String vagaoId,
            String posicao,
            Integer sequencia,
            BigDecimal pesoPrevistoKg,
            BigDecimal pesoSaldoKg,
            BigDecimal capacidadePesoKg,
            String incompatibilidades,
            String custodia,
            List<HistoricoCustodiaResposta> historicoCustodia) {
    }

    public record HistoricoCustodiaResposta(
            StatusOrdemFerroviariaCarga statusAnterior,
            StatusOrdemFerroviariaCarga statusNovo,
            String custodiaAnterior,
            String custodiaNova,
            String evento,
            String motivo,
            String responsavel,
            OffsetDateTime ocorridoEm) {
    }
}
