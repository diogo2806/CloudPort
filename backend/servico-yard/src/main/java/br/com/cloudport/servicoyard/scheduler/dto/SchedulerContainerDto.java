package br.com.cloudport.servicoyard.scheduler.dto;

import java.math.BigDecimal;
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

    private String camadaAtual;
    private String tipoCarga;
    private String movimento;
    private String destino;
    private String operador;
    private BigDecimal pesoToneladas;
    private BigDecimal alturaMetros;
    private Boolean imo;
    private Boolean reefer;
    private Boolean oog;
    private Integer sequenciaOperacional;
    private Integer prioridadeWorkQueue;
    private Integer dwellTimeHoras;

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
    public String getCamadaAtual() { return camadaAtual; }
    public void setCamadaAtual(String camadaAtual) { this.camadaAtual = camadaAtual; }
    public String getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }
    public String getMovimento() { return movimento; }
    public void setMovimento(String movimento) { this.movimento = movimento; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public BigDecimal getPesoToneladas() { return pesoToneladas; }
    public void setPesoToneladas(BigDecimal pesoToneladas) { this.pesoToneladas = pesoToneladas; }
    public BigDecimal getAlturaMetros() { return alturaMetros; }
    public void setAlturaMetros(BigDecimal alturaMetros) { this.alturaMetros = alturaMetros; }
    public Boolean getImo() { return imo; }
    public void setImo(Boolean imo) { this.imo = imo; }
    public Boolean getReefer() { return reefer; }
    public void setReefer(Boolean reefer) { this.reefer = reefer; }
    public Boolean getOog() { return oog; }
    public void setOog(Boolean oog) { this.oog = oog; }
    public Integer getSequenciaOperacional() { return sequenciaOperacional; }
    public void setSequenciaOperacional(Integer sequenciaOperacional) { this.sequenciaOperacional = sequenciaOperacional; }
    public Integer getPrioridadeWorkQueue() { return prioridadeWorkQueue; }
    public void setPrioridadeWorkQueue(Integer prioridadeWorkQueue) { this.prioridadeWorkQueue = prioridadeWorkQueue; }
    public Integer getDwellTimeHoras() { return dwellTimeHoras; }
    public void setDwellTimeHoras(Integer dwellTimeHoras) { this.dwellTimeHoras = dwellTimeHoras; }
}
