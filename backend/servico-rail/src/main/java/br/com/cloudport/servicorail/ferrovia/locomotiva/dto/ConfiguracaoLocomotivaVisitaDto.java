package br.com.cloudport.servicorail.ferrovia.locomotiva.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ConfiguracaoLocomotivaVisitaDto {

    @Size(max = 80)
    private String fabricante;

    @Size(max = 80)
    private String modelo;

    @Size(max = 80)
    private String numeroSerie;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal pesoToneladas;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal comprimentoMetros;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal larguraMetros;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal alturaMetros;

    @Size(max = 1000)
    private String observacoes;

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public BigDecimal getPesoToneladas() { return pesoToneladas; }
    public void setPesoToneladas(BigDecimal pesoToneladas) { this.pesoToneladas = pesoToneladas; }
    public BigDecimal getComprimentoMetros() { return comprimentoMetros; }
    public void setComprimentoMetros(BigDecimal comprimentoMetros) { this.comprimentoMetros = comprimentoMetros; }
    public BigDecimal getLarguraMetros() { return larguraMetros; }
    public void setLarguraMetros(BigDecimal larguraMetros) { this.larguraMetros = larguraMetros; }
    public BigDecimal getAlturaMetros() { return alturaMetros; }
    public void setAlturaMetros(BigDecimal alturaMetros) { this.alturaMetros = alturaMetros; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
