package br.com.cloudport.servicoyard.patio.dto;

import java.util.Map;

public record ResumoAvisosEstivagemPatioDto(
        long ativos,
        long criticos,
        Map<String, Long> porBloco,
        Map<String, Long> porPilha,
        Map<String, Long> porPosicao,
        Map<String, Long> porUnidade) {
}
