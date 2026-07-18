package br.com.cloudport.servicoyard.vesselplanner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class ExecucaoSequenciaGuindasteDtos {

    private ExecucaoSequenciaGuindasteDtos() {
    }

    @Schema(name = "CriarExecucaoSequenciaGuindasteRequest")
    public record CriarExecucaoRequest(
            @Min(1) @Max(8) Integer numGuindastes,
            LocalDateTime janelaInicio,
            @Min(1) @Max(240) Integer duracaoMovimentoMinutos) {
    }

    @Schema(name = "IniciarMovimentoGuindasteRequest")
    public record IniciarMovimentoRequest(
            @NotNull Long versao,
            LocalDateTime ocorridoEm) {
    }

    @Schema(name = "ConcluirMovimentoGuindasteRequest")
    public record ConcluirMovimentoRequest(
            @NotNull Long versao,
            @NotNull @DecimalMin("0.0") BigDecimal quantidadeRealizada,
            LocalDateTime concluidoEm) {
    }

    @Schema(name = "FalharMovimentoGuindasteRequest")
    public record FalharMovimentoRequest(
            @NotNull Long versao,
            @NotBlank @Size(max = 1000) String excecao,
            @DecimalMin("0.0") BigDecimal quantidadeRealizada,
            LocalDateTime ocorridoEm) {
    }

    @Schema(name = "ReplanejarMovimentoGuindasteRequest")
    public record ReplanejarMovimentoRequest(
            @NotNull Long versao,
            @NotNull @Min(1) Integer guindasteId,
            @NotNull @Min(1) Integer ordemPlanejada,
            @NotNull LocalDateTime janelaInicio,
            @NotNull LocalDateTime janelaFim,
            @NotBlank @Size(max = 1000) String motivo) {
    }

    @Schema(name = "ReconciliarExecucaoGuindasteRequest")
    public record ReconciliarExecucaoRequest(
            @NotNull Long versao,
            @Size(max = 1000) String observacao) {
    }

    @Schema(name = "MovimentoExecucaoGuindasteResponse")
    public record MovimentoResponse(
            Long id,
            Long versao,
            Integer ordemPlanejada,
            Integer guindasteId,
            String codigoContainer,
            Integer bay,
            Integer rowBay,
            Integer tier,
            String tipoOperacao,
            String codigoHatchCover,
            boolean sobreHatchCover,
            boolean bloqueadoPorTampa,
            String motivoBloqueio,
            LocalDateTime janelaInicioPlanejada,
            LocalDateTime janelaFimPlanejada,
            BigDecimal quantidadePlanejada,
            BigDecimal quantidadeRealizada,
            BigDecimal divergenciaQuantidade,
            String status,
            LocalDateTime iniciadoEm,
            String iniciadoPor,
            LocalDateTime concluidoEm,
            String concluidoPor,
            String excecao,
            String motivoReplanejamento,
            LocalDateTime replanejadoEm,
            String replanejadoPor,
            boolean atrasado) {
    }

    @Schema(name = "ExecucaoSequenciaGuindasteResponse")
    public record ExecucaoResponse(
            Long id,
            Long planId,
            Long versao,
            String status,
            Integer numeroGuindastes,
            LocalDateTime janelaBaseInicio,
            Integer duracaoMovimentoMinutos,
            BigDecimal quantidadePlanejada,
            BigDecimal quantidadeRealizada,
            BigDecimal divergenciaQuantidade,
            Integer totalMovimentos,
            Integer movimentosConcluidos,
            Integer movimentosComFalha,
            Integer movimentosEmExecucao,
            BigDecimal percentualConcluido,
            LocalDateTime reconciliadoEm,
            String reconciliadoPor,
            String observacaoReconciliacao,
            List<MovimentoResponse> movimentos) {
    }
}
