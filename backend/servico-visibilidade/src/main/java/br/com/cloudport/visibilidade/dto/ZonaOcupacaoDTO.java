package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;

public class ZonaOcupacaoDTO {

    private String zona;
    private Integer capacidadeTotal;
    private Integer ocupacaoAtual;
    private Double percentualOcupacao;
    private Integer equipamentosDisponiveis;
    private String status;
    private LocalDateTime dataAtualizacao;

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public Integer getCapacidadeTotal() {
        return capacidadeTotal;
    }

    public void setCapacidadeTotal(Integer capacidadeTotal) {
        this.capacidadeTotal = capacidadeTotal;
    }

    public Integer getOcupacaoAtual() {
        return ocupacaoAtual;
    }

    public void setOcupacaoAtual(Integer ocupacaoAtual) {
        this.ocupacaoAtual = ocupacaoAtual;
    }

    public Double getPercentualOcupacao() {
        return percentualOcupacao;
    }

    public void setPercentualOcupacao(Double percentualOcupacao) {
        this.percentualOcupacao = percentualOcupacao;
    }

    public Integer getEquipamentosDisponiveis() {
        return equipamentosDisponiveis;
    }

    public void setEquipamentosDisponiveis(Integer equipamentosDisponiveis) {
        this.equipamentosDisponiveis = equipamentosDisponiveis;
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
