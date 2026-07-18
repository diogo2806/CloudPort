package br.com.cloudport.servicoyard.patio.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

public class ReeferTelemetriaPatioRequisicaoDto {

    @NotNull
    @DecimalMin("-80.00")
    @DecimalMax("80.00")
    private BigDecimal temperaturaAtualCelsius;

    @NotNull
    @DecimalMin("-80.00")
    @DecimalMax("80.00")
    private BigDecimal temperaturaMinimaCelsius;

    @NotNull
    @DecimalMin("-80.00")
    @DecimalMax("80.00")
    private BigDecimal temperaturaMaximaCelsius;

    @NotNull
    private Boolean ligado;

    public BigDecimal getTemperaturaAtualCelsius() { return temperaturaAtualCelsius; }
    public void setTemperaturaAtualCelsius(BigDecimal temperaturaAtualCelsius) { this.temperaturaAtualCelsius = temperaturaAtualCelsius; }
    public BigDecimal getTemperaturaMinimaCelsius() { return temperaturaMinimaCelsius; }
    public void setTemperaturaMinimaCelsius(BigDecimal temperaturaMinimaCelsius) { this.temperaturaMinimaCelsius = temperaturaMinimaCelsius; }
    public BigDecimal getTemperaturaMaximaCelsius() { return temperaturaMaximaCelsius; }
    public void setTemperaturaMaximaCelsius(BigDecimal temperaturaMaximaCelsius) { this.temperaturaMaximaCelsius = temperaturaMaximaCelsius; }
    public Boolean getLigado() { return ligado; }
    public void setLigado(Boolean ligado) { this.ligado = ligado; }
}