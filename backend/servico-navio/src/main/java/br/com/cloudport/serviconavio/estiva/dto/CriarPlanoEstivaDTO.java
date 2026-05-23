package br.com.cloudport.serviconavio.estiva.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class CriarPlanoEstivaDTO {

    @NotNull(message = "Informe a quantidade de baias (bays).")
    @Min(value = 1, message = "O navio deve ter ao menos 1 baia.")
    @Max(value = 99, message = "O número de baias não pode exceder 99.")
    private Integer baias;

    @NotNull(message = "Informe a quantidade de fileiras (rows).")
    @Min(value = 1, message = "O navio deve ter ao menos 1 fileira.")
    @Max(value = 40, message = "O número de fileiras não pode exceder 40.")
    private Integer fileiras;

    @NotNull(message = "Informe a quantidade de camadas (tiers).")
    @Min(value = 1, message = "O navio deve ter ao menos 1 camada.")
    @Max(value = 20, message = "O número de camadas não pode exceder 20.")
    private Integer camadas;

    public CriarPlanoEstivaDTO() {
    }

    public Integer getBaias() {
        return baias;
    }

    public void setBaias(Integer baias) {
        this.baias = baias;
    }

    public Integer getFileiras() {
        return fileiras;
    }

    public void setFileiras(Integer fileiras) {
        this.fileiras = fileiras;
    }

    public Integer getCamadas() {
        return camadas;
    }

    public void setCamadas(Integer camadas) {
        this.camadas = camadas;
    }
}
