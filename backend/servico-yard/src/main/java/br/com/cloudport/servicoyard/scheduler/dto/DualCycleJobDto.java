package br.com.cloudport.servicoyard.scheduler.dto;

import java.time.LocalDateTime;

public class DualCycleJobDto {

    private Long id;

    private String equipamentoId;

    private String containerPickup;

    private Integer linhaPickup;

    private Integer colunaPickup;

    private String tipoOperacaoPickup;

    private String containerDropoff;

    private Integer linhaDropoff;

    private Integer colunaDropoff;

    private String tipoOperacaoDropoff;

    private LocalDateTime tempoInicioEstimado;

    private LocalDateTime tempoFimEstimado;

    private Integer distanciaTotal;

    private Integer tempoMinutos;

    private Integer economiaDistancia;

    private Double eficiencia;

    private String status;

    private String observacoes;

    public DualCycleJobDto() {
    }

    public DualCycleJobDto(String equipamentoId, String containerPickup,
                          Integer linhaPickup, Integer colunaPickup,
                          String containerDropoff, Integer linhaDropoff,
                          Integer colunaDropoff) {
        this.equipamentoId = equipamentoId;
        this.containerPickup = containerPickup;
        this.linhaPickup = linhaPickup;
        this.colunaPickup = colunaPickup;
        this.containerDropoff = containerDropoff;
        this.linhaDropoff = linhaDropoff;
        this.colunaDropoff = colunaDropoff;
        this.status = "PLANEJADO";
    }

    public Integer calcularDistanciaTotal() {
        if (linhaPickup == null || colunaPickup == null ||
            linhaDropoff == null || colunaDropoff == null) {
            return 0;
        }

        int distPickup = linhaPickup + colunaPickup;
        int distDropoff = linhaDropoff + colunaDropoff;
        int distPickupToDropoff = Math.abs(linhaPickup - linhaDropoff) +
                                 Math.abs(colunaPickup - colunaDropoff);

        return distPickup + distPickupToDropoff + distDropoff;
    }

    public Integer calcularEconomiaDistancia() {
        int separado = (linhaPickup + colunaPickup) +
                      (linhaDropoff + colunaDropoff);
        int junto = calcularDistanciaTotal();
        return separado - junto;
    }

    public Double calcularEficiencia() {
        Integer economia = calcularEconomiaDistancia();
        Integer distancia = calcularDistanciaTotal();

        if (distancia == 0) {
            return 0.0;
        }

        return (economia.doubleValue() / distancia) * 100;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEquipamentoId() {
        return equipamentoId;
    }

    public void setEquipamentoId(String equipamentoId) {
        this.equipamentoId = equipamentoId;
    }

    public String getContainerPickup() {
        return containerPickup;
    }

    public void setContainerPickup(String containerPickup) {
        this.containerPickup = containerPickup;
    }

    public Integer getLinhaPickup() {
        return linhaPickup;
    }

    public void setLinhaPickup(Integer linhaPickup) {
        this.linhaPickup = linhaPickup;
    }

    public Integer getColunaPickup() {
        return colunaPickup;
    }

    public void setColunaPickup(Integer colunaPickup) {
        this.colunaPickup = colunaPickup;
    }

    public String getTipoOperacaoPickup() {
        return tipoOperacaoPickup;
    }

    public void setTipoOperacaoPickup(String tipoOperacaoPickup) {
        this.tipoOperacaoPickup = tipoOperacaoPickup;
    }

    public String getContainerDropoff() {
        return containerDropoff;
    }

    public void setContainerDropoff(String containerDropoff) {
        this.containerDropoff = containerDropoff;
    }

    public Integer getLinhaDropoff() {
        return linhaDropoff;
    }

    public void setLinhaDropoff(Integer linhaDropoff) {
        this.linhaDropoff = linhaDropoff;
    }

    public Integer getColunaDropoff() {
        return colunaDropoff;
    }

    public void setColunaDropoff(Integer colunaDropoff) {
        this.colunaDropoff = colunaDropoff;
    }

    public String getTipoOperacaoDropoff() {
        return tipoOperacaoDropoff;
    }

    public void setTipoOperacaoDropoff(String tipoOperacaoDropoff) {
        this.tipoOperacaoDropoff = tipoOperacaoDropoff;
    }

    public LocalDateTime getTempoInicioEstimado() {
        return tempoInicioEstimado;
    }

    public void setTempoInicioEstimado(LocalDateTime tempoInicioEstimado) {
        this.tempoInicioEstimado = tempoInicioEstimado;
    }

    public LocalDateTime getTempoFimEstimado() {
        return tempoFimEstimado;
    }

    public void setTempoFimEstimado(LocalDateTime tempoFimEstimado) {
        this.tempoFimEstimado = tempoFimEstimado;
    }

    public Integer getDistanciaTotal() {
        return distanciaTotal;
    }

    public void setDistanciaTotal(Integer distanciaTotal) {
        this.distanciaTotal = distanciaTotal;
    }

    public Integer getTempoMinutos() {
        return tempoMinutos;
    }

    public void setTempoMinutos(Integer tempoMinutos) {
        this.tempoMinutos = tempoMinutos;
    }

    public Integer getEconomiaDistancia() {
        return economiaDistancia;
    }

    public void setEconomiaDistancia(Integer economiaDistancia) {
        this.economiaDistancia = economiaDistancia;
    }

    public Double getEficiencia() {
        return eficiencia;
    }

    public void setEficiencia(Double eficiencia) {
        this.eficiencia = eficiencia;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
