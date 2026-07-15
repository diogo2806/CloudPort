package br.com.cloudport.servicoyard.scheduler.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

public class SchedulerContainerDto {

    @NotBlank
    private String codigoContainer;

    @NotNull
    @PositiveOrZero
    private Integer linha;

    @NotNull
    @PositiveOrZero
    private Integer coluna;

    public SchedulerContainerDto() {
    }

    public SchedulerContainerDto(String codigoContainer, Integer linha, Integer coluna) {
        this.codigoContainer = codigoContainer;
        this.linha = linha;
        this.coluna = coluna;
    }

    public String getCodigoContainer() { return codigoContainer; }
    public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
    public Integer getLinha() { return linha; }
    public void setLinha(Integer linha) { this.linha = linha; }
    public Integer getColuna() { return coluna; }
    public void setColuna(Integer coluna) { this.coluna = coluna; }
}
