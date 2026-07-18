package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ComandoReplanejamentoPatioNavioDTO(
        Boolean aplicar,
        String usuario,
        Integer limiteRehandleAceitavel,
        String motivo,
        String correlationId,
        Map<String, BigDecimal> pesosCriterios
) {
    public ComandoReplanejamentoPatioNavioDTO(
            Boolean aplicar,
            String usuario,
            Integer limiteRehandleAceitavel) {
        this(aplicar, usuario, limiteRehandleAceitavel, null, null, Map.of());
    }

    public boolean aplicarEfetivo() {
        return Boolean.TRUE.equals(aplicar);
    }

    public Map<String, BigDecimal> pesosEfetivos() {
        return pesosCriterios == null ? Map.of() : Map.copyOf(pesosCriterios);
    }

    public String motivoEfetivo() {
        return motivo == null || motivo.isBlank()
                ? (aplicarEfetivo() ? "Aplicacao do plano otimizado real" : "Simulacao do plano otimizado real")
                : motivo.trim();
    }
}
