package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProdutividadeCaisDTO(
        Long visitaNavioId,
        String codigoVisita,
        String berco,
        LocalDateTime inicioOperacao,
        LocalDateTime fimReferencia,
        Long minutosOperacao,
        Integer movimentosPlanejados,
        Integer movimentosRealizados,
        Integer movimentosPendentes,
        BigDecimal produtividadePlanejadaMovimentosHora,
        BigDecimal produtividadeAtualMovimentosHora,
        BigDecimal percentualConclusao,
        LocalDateTime previsaoTermino,
        List<GuindasteQuayMonitorDTO> guindastes
) {
}
