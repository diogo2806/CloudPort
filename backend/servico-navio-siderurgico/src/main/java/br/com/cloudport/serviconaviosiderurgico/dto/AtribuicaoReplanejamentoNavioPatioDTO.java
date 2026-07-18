package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.util.List;

public record AtribuicaoReplanejamentoNavioPatioDTO(
        String codigoCarga,
        String movimento,
        Integer linhaOriginal,
        Integer colunaOriginal,
        String camadaOriginal,
        Integer linhaProposta,
        Integer colunaProposta,
        String camadaProposta,
        String blocoProposto,
        String equipamento,
        Integer sequenciaPlano,
        BigDecimal scoreTotal,
        Integer distancia,
        Integer rehandlesEstimados,
        List<String> justificativas
) {
}
