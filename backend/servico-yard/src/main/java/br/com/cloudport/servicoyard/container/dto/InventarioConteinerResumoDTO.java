package br.com.cloudport.servicoyard.container.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventarioConteinerResumoDTO {

    private final long totalConteiners;
    private final long totalOperacionais;
    private final long totalLiberados;
    private final long totalRetidos;
    private final long totalDanificados;
    private final long totalRefrigerados;
    private final long totalPerigosos;
    private final long totalSemPosicao;
    private final BigDecimal pesoTotalToneladas;
    private final LocalDateTime atualizadoEm;

    public InventarioConteinerResumoDTO(long totalConteiners,
                                        long totalOperacionais,
                                        long totalLiberados,
                                        long totalRetidos,
                                        long totalDanificados,
                                        long totalRefrigerados,
                                        long totalPerigosos,
                                        long totalSemPosicao,
                                        BigDecimal pesoTotalToneladas,
                                        LocalDateTime atualizadoEm) {
        this.totalConteiners = totalConteiners;
        this.totalOperacionais = totalOperacionais;
        this.totalLiberados = totalLiberados;
        this.totalRetidos = totalRetidos;
        this.totalDanificados = totalDanificados;
        this.totalRefrigerados = totalRefrigerados;
        this.totalPerigosos = totalPerigosos;
        this.totalSemPosicao = totalSemPosicao;
        this.pesoTotalToneladas = pesoTotalToneladas;
        this.atualizadoEm = atualizadoEm;
    }

    public long getTotalConteiners() {
        return totalConteiners;
    }

    public long getTotalOperacionais() {
        return totalOperacionais;
    }

    public long getTotalLiberados() {
        return totalLiberados;
    }

    public long getTotalRetidos() {
        return totalRetidos;
    }

    public long getTotalDanificados() {
        return totalDanificados;
    }

    public long getTotalRefrigerados() {
        return totalRefrigerados;
    }

    public long getTotalPerigosos() {
        return totalPerigosos;
    }

    public long getTotalSemPosicao() {
        return totalSemPosicao;
    }

    public BigDecimal getPesoTotalToneladas() {
        return pesoTotalToneladas;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
