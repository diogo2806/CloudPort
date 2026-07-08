package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.BordoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.PosicaoEstivaNavio;
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public record PosicaoEstivaNavioDTO(
        Long id,
        Long planoEstivaId,
        @NotNull(message = "Item de operacao e obrigatorio.") Long itemOperacaoId,
        String codigoLote,
        @NotNull(message = "Porao e obrigatorio.") @Min(value = 1, message = "Porao deve ser maior que zero.") Integer porao,
        @NotNull(message = "Camada e obrigatoria.") @Min(value = 1, message = "Camada deve ser maior que zero.") Integer camada,
        @NotNull(message = "Coluna e obrigatoria.") @Min(value = 1, message = "Coluna deve ser maior que zero.") Integer coluna,
        @NotNull(message = "Bordo e obrigatorio.") BordoEstivaNavio bordo,
        @NotNull(message = "Sequencia e obrigatoria.") @Min(value = 1, message = "Sequencia deve ser maior que zero.") Integer sequencia,
        @NotNull(message = "Peso e obrigatorio.") @DecimalMin(value = "0.001", message = "Peso deve ser maior que zero.") BigDecimal pesoToneladas,
        String status
) {
    public static PosicaoEstivaNavioDTO de(PosicaoEstivaNavio posicao) {
        return new PosicaoEstivaNavioDTO(
                posicao.getId(),
                posicao.getPlanoEstiva().getId(),
                posicao.getItemOperacao().getId(),
                posicao.getItemOperacao().getCodigoLote(),
                posicao.getPorao(),
                posicao.getCamada(),
                posicao.getColuna(),
                posicao.getBordo(),
                posicao.getSequencia(),
                posicao.getPesoToneladas(),
                posicao.getStatus()
        );
    }
}
