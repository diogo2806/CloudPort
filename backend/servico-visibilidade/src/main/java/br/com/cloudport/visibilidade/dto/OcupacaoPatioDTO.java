package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OcupacaoPatioDTO {

    private Integer capacidadeTotal;
    private Integer ocupacaoAtual;
    private Double percentualOcupacao;
    private String status;
    private String bloqueioAutomaticoEm;
    private LocalDateTime dataAtualizacao;
    private List<ZonaOcupacaoDTO> zonas;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBloqueioAutomaticoEm() {
        return bloqueioAutomaticoEm;
    }

    public void setBloqueioAutomaticoEm(String bloqueioAutomaticoEm) {
        this.bloqueioAutomaticoEm = bloqueioAutomaticoEm;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public List<ZonaOcupacaoDTO> getZonas() {
        return zonas;
    }

    public void setZonas(List<ZonaOcupacaoDTO> zonas) {
        this.zonas = zonas;
    }
}
