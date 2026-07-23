package br.com.cloudport.servicoyard.patio.dto;

import java.util.List;

public record RespostaAlocacaoIntegradaYardDto(
        int totalRecebido,
        int totalAlocado,
        int totalRejeitado,
        List<ResultadoAlocacaoYardDto> resultados) {
}
