package br.com.cloudport.servicoyard.patio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlanoRecebimentoPatioDto {

    private Integer totalConteineres;
    private Integer totalGrupos;
    private Integer totalTeus;
    private BigDecimal pesoTotalToneladas;
    private LocalDateTime primeiraChegada;
    private LocalDateTime ultimaChegada;
    private List<String> alertas = new ArrayList<>();
    private List<GrupoRecebimentoPatioDto> grupos = new ArrayList<>();

    public Integer getTotalConteineres() {
        return totalConteineres;
    }

    public void setTotalConteineres(Integer totalConteineres) {
        this.totalConteineres = totalConteineres;
    }

    public Integer getTotalGrupos() {
        return totalGrupos;
    }

    public void setTotalGrupos(Integer totalGrupos) {
        this.totalGrupos = totalGrupos;
    }

    public Integer getTotalTeus() {
        return totalTeus;
    }

    public void setTotalTeus(Integer totalTeus) {
        this.totalTeus = totalTeus;
    }

    public BigDecimal getPesoTotalToneladas() {
        return pesoTotalToneladas;
    }

    public void setPesoTotalToneladas(BigDecimal pesoTotalToneladas) {
        this.pesoTotalToneladas = pesoTotalToneladas;
    }

    public LocalDateTime getPrimeiraChegada() {
        return primeiraChegada;
    }

    public void setPrimeiraChegada(LocalDateTime primeiraChegada) {
        this.primeiraChegada = primeiraChegada;
    }

    public LocalDateTime getUltimaChegada() {
        return ultimaChegada;
    }

    public void setUltimaChegada(LocalDateTime ultimaChegada) {
        this.ultimaChegada = ultimaChegada;
    }

    public List<String> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<String> alertas) {
        this.alertas = alertas == null ? new ArrayList<>() : new ArrayList<>(alertas);
    }

    public List<GrupoRecebimentoPatioDto> getGrupos() {
        return grupos;
    }

    public void setGrupos(List<GrupoRecebimentoPatioDto> grupos) {
        this.grupos = grupos == null ? new ArrayList<>() : new ArrayList<>(grupos);
    }
}
