package br.com.cloudport.serviconavio.escala.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class ProntidaoBercoDtos {

    private ProntidaoBercoDtos() {
    }

    public record ConfirmarProntidaoBercoRequest(
            @NotBlank @Size(max = 40) String berco,
            @NotNull @DecimalMin("0.0") BigDecimal caladoMetros,
            @NotNull Boolean bercoConfirmado,
            @NotNull Boolean caladoConfirmado,
            @NotNull Boolean defensasConfirmadas,
            @NotNull Boolean amarracaoConfirmada,
            @NotNull Boolean acessoConfirmado,
            @NotNull Boolean recursosConfirmados,
            @NotNull Boolean restricoesAvaliadas,
            @NotNull Boolean liberacoesConfirmadas,
            @Size(max = 1000) String recursos,
            @Size(max = 1000) String restricoes,
            @Size(max = 1000) String liberacoes,
            @Size(max = 1000) String observacoes) {
    }

    public record ProntidaoBercoResponse(
            Long id,
            Long escalaId,
            Integer versaoChecklist,
            String berco,
            BigDecimal caladoMetros,
            Boolean bercoConfirmado,
            Boolean caladoConfirmado,
            Boolean defensasConfirmadas,
            Boolean amarracaoConfirmada,
            Boolean acessoConfirmado,
            Boolean recursosConfirmados,
            Boolean restricoesAvaliadas,
            Boolean liberacoesConfirmadas,
            String recursos,
            String restricoes,
            String liberacoes,
            String observacoes,
            String responsavel,
            LocalDateTime confirmadoEm,
            boolean pronto,
            List<String> motivosBloqueio) {
    }
}
