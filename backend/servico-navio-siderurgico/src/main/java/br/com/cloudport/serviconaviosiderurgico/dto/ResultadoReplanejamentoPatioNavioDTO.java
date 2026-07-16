package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoReplanejamentoPatioNavioDTO(
        List<ReservaPatioNavioDTO> reservasSugeridas,
        List<OrdemPatioDaVisitaDTO> ordensReordenadas,
        BigDecimal economiaEstimadaDistanciaPercentual,
        String riscoRehandle,
        List<String> alertasImpeditivos,
        List<Long> itensNaoReplanejados,
        String planoOtimizacaoId,
        Integer versaoPlano,
        Integer distanciaOriginal,
        Integer distanciaOtimizada
) {

    public ResultadoReplanejamentoPatioNavioDTO(
            List<ReservaPatioNavioDTO> reservasSugeridas,
            List<OrdemPatioDaVisitaDTO> ordensReordenadas,
            BigDecimal economiaEstimadaDistanciaPercentual,
            String riscoRehandle,
            List<String> alertasImpeditivos,
            List<Long> itensNaoReplanejados
    ) {
        this(
                reservasSugeridas,
                ordensReordenadas,
                economiaEstimadaDistanciaPercentual,
                riscoRehandle,
                alertasImpeditivos,
                itensNaoReplanejados,
                null,
                null,
                null,
                null);
    }
}
