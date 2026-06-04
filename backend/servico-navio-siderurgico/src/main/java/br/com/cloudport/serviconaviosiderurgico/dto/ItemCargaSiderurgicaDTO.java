package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemCargaSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoCargaSiderurgica;
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record ItemCargaSiderurgicaDTO(
        Long id,
        Long operacaoId,
        @NotBlank @Size(max = 60) String codigoLote,
        @NotNull TipoCargaSiderurgica tipoCarga,
        @NotBlank @Size(max = 120) String produto,
        @NotNull @Min(1) Integer quantidade,
        BigDecimal pesoUnitarioToneladas,
        @NotNull @DecimalMin("0.001") BigDecimal pesoTotalToneladas,
        @Min(1) Integer porao,
        @Size(max = 40) String posicaoBordo,
        @Size(max = 80) String origemPatio,
        @Size(max = 80) String destinoPatio,
        Integer sequenciaOperacional,
        StatusItemCarga status
) {
    public static ItemCargaSiderurgicaDTO de(ItemCargaSiderurgica item) {
        return new ItemCargaSiderurgicaDTO(
                item.getId(),
                item.getOperacao().getId(),
                item.getCodigoLote(),
                item.getTipoCarga(),
                item.getProduto(),
                item.getQuantidade(),
                item.getPesoUnitarioToneladas(),
                item.getPesoTotalToneladas(),
                item.getPorao(),
                item.getPosicaoBordo(),
                item.getOrigemPatio(),
                item.getDestinoPatio(),
                item.getSequenciaOperacional(),
                item.getStatus()
        );
    }
}
