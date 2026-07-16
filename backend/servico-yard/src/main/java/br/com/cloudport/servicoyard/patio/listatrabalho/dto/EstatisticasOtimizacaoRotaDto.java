package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EstatisticasOtimizacaoRotaDto {

    private Integer totalOrdens;
    private Double distanciaOriginal;
    private Double distanciaOtimizada;
    private Double percentualMelhoria;
    private List<OrdemTrabalhoPatioRespostaDto> ordensOtimizadas;

    public EstatisticasOtimizacaoRotaDto() {
    }

    public EstatisticasOtimizacaoRotaDto(Integer totalOrdens,
                                           Double distanciaOriginal,
                                           Double distanciaOtimizada,
                                           Double percentualMelhoria,
                                           List<OrdemTrabalhoPatioRespostaDto> ordensOtimizadas) {
        this.totalOrdens = totalOrdens;
        this.distanciaOriginal = distanciaOriginal;
        this.distanciaOtimizada = distanciaOtimizada;
        this.percentualMelhoria = percentualMelhoria;
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

    public Double getPercentualMelhoria() {
        return percentualMelhoria;
    }

    public void setPercentualMelhoria(Double percentualMelhoria) {
        this.percentualMelhoria = percentualMelhoria;
    }

    @Deprecated
    @JsonProperty("percentualMejora")
    public Double getPercentualMejora() {
        return percentualMelhoria;
    }

    @Deprecated
    @JsonProperty("percentualMejora")
    public void setPercentualMejora(Double percentualMejora) {
        this.percentualMelhoria = percentualMejora;
    }

    public List<OrdemTrabalhoPatioRespostaDto> getOrdensOtimizadas() {
        return ordensOtimizadas;
    }

    public void setOrdensOtimizadas(List<OrdemTrabalhoPatioRespostaDto> ordensOtimizadas) {
        this.ordensOtimizadas = ordensOtimizadas;
    }
}
