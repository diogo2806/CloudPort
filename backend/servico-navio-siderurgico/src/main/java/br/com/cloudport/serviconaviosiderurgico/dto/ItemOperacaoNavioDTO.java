package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoCargaSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record ItemOperacaoNavioDTO(
        Long id,
        Long visitaNavioId,
        @NotNull(message = "Tipo de movimento e obrigatorio.") TipoMovimentoNavio tipoMovimento,
        @NotBlank(message = "Codigo do lote e obrigatorio.") String codigoLote,
        @NotBlank(message = "Produto e obrigatorio.") String produto,
        @NotNull(message = "Tipo de carga e obrigatorio.") TipoCargaSiderurgica tipoCarga,
        @NotNull(message = "Quantidade e obrigatoria.") @Min(value = 1, message = "Quantidade deve ser maior que zero.") Integer quantidade,
        BigDecimal pesoUnitarioToneladas,
        @NotNull(message = "Peso total e obrigatorio.") @DecimalMin(value = "0.001", message = "Peso total deve ser maior que zero.") BigDecimal pesoTotalToneladas,
        @DecimalMin(value = "0.001", message = "Altura da carga deve ser maior que zero.") BigDecimal alturaCargaMetros,
        Integer poraoPlanejado,
        Integer poraoReal,
        String posicaoPlanejada,
        String posicaoReal,
        String origemPatio,
        String destinoPatio,
        Long conteinerPatioId,
        Long cargaPatioId,
        Long ordemTrabalhoPatioId,
        Long movimentoPatioId,
        String posicaoPatioPlanejada,
        String posicaoPatioReal,
        StatusIntegracaoPatio statusIntegracaoPatio,
        Integer sequenciaOperacional,
        StatusItemCarga status,
        String motivoBloqueio,
        String observacoes
) {
    public static ItemOperacaoNavioDTO de(ItemOperacaoNavio item) {
        return new ItemOperacaoNavioDTO(
                item.getId(),
                item.getVisitaNavio().getId(),
                item.getTipoMovimento(),
                item.getCodigoLote(),
                item.getProduto(),
                item.getTipoCarga(),
                item.getQuantidade(),
                item.getPesoUnitarioToneladas(),
                item.getPesoTotalToneladas(),
                item.getAlturaCargaMetros(),
                item.getPoraoPlanejado(),
                item.getPoraoReal(),
                item.getPosicaoPlanejada(),
                item.getPosicaoReal(),
                item.getOrigemPatio(),
                item.getDestinoPatio(),
                item.getConteinerPatioId(),
                item.getCargaPatioId(),
                item.getOrdemTrabalhoPatioId(),
                item.getMovimentoPatioId(),
                item.getPosicaoPatioPlanejada(),
                item.getPosicaoPatioReal(),
                item.getStatusIntegracaoPatio(),
                item.getSequenciaOperacional(),
                item.getStatus(),
                item.getMotivoBloqueio(),
                item.getObservacoes()
        );
    }
}
