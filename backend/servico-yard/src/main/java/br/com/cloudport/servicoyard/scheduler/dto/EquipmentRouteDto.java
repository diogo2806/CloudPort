package br.com.cloudport.servicoyard.scheduler.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class EquipmentRouteDto {

    @NotBlank
    private String equipamentoId;

    @NotNull
    private LocalDateTime tempoInicio;

    @NotNull
    private LocalDateTime tempoFim;

    private List<RouteStopDto> paradas;

    private Integer distanciaTotal;

    private Integer tempoTotalMinutos;

    private Boolean isDualCycle;

    private Integer economiaDistancia;

    private String status;

    public EquipmentRouteDto() {
        this.paradas = new ArrayList<>();
    }

    public EquipmentRouteDto(String equipamentoId, LocalDateTime tempoInicio, LocalDateTime tempoFim) {
        this.equipamentoId = equipamentoId;
        this.tempoInicio = tempoInicio;
        this.tempoFim = tempoFim;
        this.paradas = new ArrayList<>();
    }

    public String getEquipamentoId() {
        return equipamentoId;
    }

    public void setEquipamentoId(String equipamentoId) {
        this.equipamentoId = equipamentoId;
    }

    public LocalDateTime getTempoInicio() {
        return tempoInicio;
    }

    public void setTempoInicio(LocalDateTime tempoInicio) {
        this.tempoInicio = tempoInicio;
    }

    public LocalDateTime getTempoFim() {
        return tempoFim;
    }

    public void setTempoFim(LocalDateTime tempoFim) {
        this.tempoFim = tempoFim;
    }

    public List<RouteStopDto> getParadas() {
        return paradas;
    }

    public void setParadas(List<RouteStopDto> paradas) {
        this.paradas = paradas;
    }

    public void adicionarParada(RouteStopDto parada) {
        this.paradas.add(parada);
    }

    public Integer getDistanciaTotal() {
        return distanciaTotal;
    }

    public void setDistanciaTotal(Integer distanciaTotal) {
        this.distanciaTotal = distanciaTotal;
    }

    public Integer getTempoTotalMinutos() {
        return tempoTotalMinutos;
    }

    public void setTempoTotalMinutos(Integer tempoTotalMinutos) {
        this.tempoTotalMinutos = tempoTotalMinutos;
    }

    public Boolean getIsDualCycle() {
        return isDualCycle;
    }

    public void setIsDualCycle(Boolean isDualCycle) {
        this.isDualCycle = isDualCycle;
    }

    public Integer getEconomiaDistancia() {
        return economiaDistancia;
    }

    public void setEconomiaDistancia(Integer economiaDistancia) {
        this.economiaDistancia = economiaDistancia;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static class RouteStopDto {
        private Integer sequencia;
        private String tipoOperacao;
        private String codigoContainer;
        private Integer linha;
        private Integer coluna;
        private LocalDateTime tempoEsperado;

        public RouteStopDto(Integer sequencia, String tipoOperacao, String codigoContainer,
                          Integer linha, Integer coluna) {
            this.sequencia = sequencia;
            this.tipoOperacao = tipoOperacao;
            this.codigoContainer = codigoContainer;
            this.linha = linha;
            this.coluna = coluna;
        }

        public Integer getSequencia() {
            return sequencia;
        }

        public String getTipoOperacao() {
            return tipoOperacao;
        }

        public String getCodigoContainer() {
            return codigoContainer;
        }

        public Integer getLinha() {
            return linha;
        }

        public Integer getColuna() {
            return coluna;
        }

        public LocalDateTime getTempoEsperado() {
            return tempoEsperado;
        }

        public void setTempoEsperado(LocalDateTime tempoEsperado) {
            this.tempoEsperado = tempoEsperado;
        }

        public int getDistancia() {
            return linha + coluna;
        }
    }
}
