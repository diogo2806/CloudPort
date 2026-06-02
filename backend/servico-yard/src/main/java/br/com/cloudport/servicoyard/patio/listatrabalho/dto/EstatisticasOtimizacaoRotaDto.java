package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.List;

public class EstatisticasOtimizacaoRotaDto {

    private Integer totalOrdens;
    private Double distanciaOriginal;
    private Double distanciaOtimizada;
    private Double percentualMejora;
    private List<OrdemTrabalhoPatioRespostaDto> ordensOtimizadas;

    public EstatisticasOtimizacaoRotaDto() {
    }

    public EstatisticasOtimizacaoRotaDto(Integer totalOrdens,
                                          Double distanciaOriginal,
                                          Double distanciaOtimizada,
                                          Double percentualMejora,
                                          List<OrdemTrabalhoPatioRespostaDto> ordensOtimizadas) {
        this.totalOrdens = totalOrdens;
        this.distanciaOriginal = distanciaOriginal;
        this.distanciaOtimizada = distanciaOtimizada;
        this.percentualMejora = percentualMejora;
        this.ordensOtimizadas = ordensOtimizadas;
    }

    public Integer getTotalOrdens() {
        return totalOrdens;
    }

    public void setTotalOrdens(Integer totalOrdens) {
        this.totalOrdens = totalOrdens;
    }

    public Double getDistanciaOriginal() {
        return distanciaOriginal;
    }

    public void setDistanciaOriginal(Double distanciaOriginal) {
        this.distanciaOriginal = distanciaOriginal;
    }

    public Double getDistanciaOtimizada() {
        return distanciaOtimizada;
    }

    public void setDistanciaOtimizada(Double distanciaOtimizada) {
        this.distanciaOtimizada = distanciaOtimizada;
    }

    public Double getPercentualMejora() {
        return percentualMejora;
    }

    public void setPercentualMejora(Double percentualMejora) {
        this.percentualMejora = percentualMejora;
    }

    public List<OrdemTrabalhoPatioRespostaDto> getOrdensOtimizadas() {
        return ordensOtimizadas;
    }

    public void setOrdensOtimizadas(List<OrdemTrabalhoPatioRespostaDto> ordensOtimizadas) {
        this.ordensOtimizadas = ordensOtimizadas;
    }
}
