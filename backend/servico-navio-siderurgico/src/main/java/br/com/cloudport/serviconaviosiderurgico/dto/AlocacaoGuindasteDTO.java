package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record AlocacaoGuindasteDTO(
        Long id,
        @NotBlank(message = "Codigo do guindaste e obrigatorio.") String codigoGuindaste,
        String recursoCais,
        @NotNull(message = "Porao e obrigatorio.") @Min(value = 1, message = "Porao deve ser maior que zero.") Integer porao,
        Long workQueueId,
        @NotNull(message = "Sequencia e obrigatoria.") @Min(value = 1, message = "Sequencia deve ser maior que zero.") Integer sequencia,
        @NotNull(message = "Movimentos planejados sao obrigatorios.") @Min(value = 1, message = "Movimentos planejados devem ser maiores que zero.") Integer movimentosPlanejados,
        @NotNull(message = "Produtividade planejada e obrigatoria.")
        @DecimalMin(value = "0.01", message = "Produtividade planejada deve ser maior que zero.")
        BigDecimal produtividadePlanejadaMovimentosHora,
        @NotNull(message = "Inicio planejado e obrigatorio.") LocalDateTime inicioPlanejado,
        @NotNull(message = "Fim planejado e obrigatorio.") LocalDateTime fimPlanejado,
        String observacao
) {
}
