package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;

public class ThroughputGateDTO {

    private Integer entradasHoje;
    private Integer saidasHoje;
    private Integer movimentosHoje;
    private Double desempenhoPercentual;
    private Double tempoMedioProcessamentoMinutos;
    private String status;
    private LocalDateTime dataAtualizacao;

    public Integer getEntradasHoje() {
        return entradasHoje;
    }

    public void setEntradasHoje(Integer entradasHoje) {
        this.entradasHoje = entradasHoje;
    }

    public Integer getSaidasHoje() {
        return saidasHoje;
    }

    public void setSaidasHoje(Integer saidasHoje) {
        this.saidasHoje = saidasHoje;
    }

    public Integer getMovimentosHoje() {
        return movimentosHoje;
    }

    public void setMovimentosHoje(Integer movimentosHoje) {
        this.movimentosHoje = movimentosHoje;
    }

    public Double getDesempenhoPercentual() {
        return desempenhoPercentual;
    }

    public void setDesempenhoPercentual(Double desempenhoPercentual) {
        this.desempenhoPercentual = desempenhoPercentual;
    }

    public Double getTempoMedioProcessamentoMinutos() {
        return tempoMedioProcessamentoMinutos;
    }

    public void setTempoMedioProcessamentoMinutos(Double tempoMedioProcessamentoMinutos) {
        this.tempoMedioProcessamentoMinutos = tempoMedioProcessamentoMinutos;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
}
