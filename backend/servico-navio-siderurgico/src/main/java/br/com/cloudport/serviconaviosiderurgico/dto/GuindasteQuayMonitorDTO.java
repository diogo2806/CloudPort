package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GuindasteQuayMonitorDTO(
        Long planoId,
        String codigoGuindaste,
        String recursoCais,
        Integer porao,
        Long workQueueId,
        Integer sequencia,
        Integer movimentosPlanejados,
        Integer movimentosRealizados,
        Integer movimentosPendentes,
        BigDecimal produtividadePlanejadaMovimentosHora,
        BigDecimal produtividadeAtualMovimentosHora,
        BigDecimal percentualConclusao,
        LocalDateTime inicioPlanejado,
        LocalDateTime fimPlanejado,
        LocalDateTime previsaoTermino,
        String statusOperacional
) {
}
