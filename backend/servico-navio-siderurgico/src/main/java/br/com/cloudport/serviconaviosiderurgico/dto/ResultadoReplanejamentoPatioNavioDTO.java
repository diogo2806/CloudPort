package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        Integer distanciaOtimizada,
        List<AtribuicaoReplanejamentoNavioPatioDTO> atribuicoesPropostas,
        Map<String, BigDecimal> memoriaCalculo,
        List<String> justificativas,
        String assinaturaEntrada,
        BigDecimal pontuacaoTotal
) {

    public ResultadoReplanejamentoPatioNavioDTO(
            List<ReservaPatioNavioDTO> reservasSugeridas,
            List<OrdemPatioDaVisitaDTO> ordensReordenadas,
            BigDecimal economiaEstimadaDistanciaPercentual,
            String riscoRehandle,
            List<String> alertasImpeditivos,
            List<Long> itensNaoReplanejados,
            String planoOtimizacaoId,
            Integer versaoPlano,
            Integer distanciaOriginal,
            Integer distanciaOtimizada) {
        this(
                reservasSugeridas,
                ordensReordenadas,
                economiaEstimadaDistanciaPercentual,
                riscoRehandle,
                alertasImpeditivos,
                itensNaoReplanejados,
                planoOtimizacaoId,
                versaoPlano,
                distanciaOriginal,
                distanciaOtimizada,
                List.of(),
                Map.of(),
                List.of(),
                null,
                null);
    }

    public ResultadoReplanejamentoPatioNavioDTO(
            List<ReservaPatioNavioDTO> reservasSugeridas,
            List<OrdemPatioDaVisitaDTO> ordensReordenadas,
            BigDecimal economiaEstimadaDistanciaPercentual,
            String riscoRehandle,
            List<String> alertasImpeditivos,
            List<Long> itensNaoReplanejados) {
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
