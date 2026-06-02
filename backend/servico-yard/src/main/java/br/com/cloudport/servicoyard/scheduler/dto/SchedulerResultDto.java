package br.com.cloudport.servicoyard.scheduler.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SchedulerResultDto {

    private String codigoNavio;

    private LocalDateTime tempoGeracaoPlano;

    private List<EquipmentRouteDto> rotasEquipamento;

    private List<DualCycleJobDto> jobsDualCycle;

    private Integer totalOperacoes;

    private Integer operacoesDualCycle;

    private Integer distanciaEconomizada;

    private Double eficienciaMedia;

    private String statusGeral;

    private String observacoes;

    public SchedulerResultDto() {
        this.rotasEquipamento = new ArrayList<>();
        this.jobsDualCycle = new ArrayList<>();
        this.tempoGeracaoPlano = LocalDateTime.now();
    }

    public SchedulerResultDto(String codigoNavio) {
        this();
        this.codigoNavio = codigoNavio;
    }

    public void adicionarRotaEquipamento(EquipmentRouteDto rota) {
        this.rotasEquipamento.add(rota);
    }

    public void adicionarDualCycleJob(DualCycleJobDto job) {
        this.jobsDualCycle.add(job);
    }

    public void calcularEstatisticas() {
        this.totalOperacoes = rotasEquipamento.stream()
            .mapToInt(r -> r.getParadas().size())
            .sum();

        this.operacoesDualCycle = jobsDualCycle.size();

        this.distanciaEconomizada = jobsDualCycle.stream()
            .mapToInt(job -> job.calcularEconomiaDistancia())
            .sum();

        if (!jobsDualCycle.isEmpty()) {
            this.eficienciaMedia = jobsDualCycle.stream()
                .mapToDouble(job -> job.calcularEficiencia())
                .average()
                .orElse(0.0);
        } else {
            this.eficienciaMedia = 0.0;
        }

        determinarStatusGeral();
    }

    private void determinarStatusGeral() {
        if (totalOperacoes == 0) {
            this.statusGeral = "SEM_OPERACOES";
        } else if (operacoesDualCycle >= totalOperacoes * 0.8) {
            this.statusGeral = "EXCELENTE";
        } else if (operacoesDualCycle >= totalOperacoes * 0.5) {
            this.statusGeral = "BOM";
        } else if (operacoesDualCycle > 0) {
            this.statusGeral = "REGULAR";
        } else {
            this.statusGeral = "PESSIMO";
        }
    }

    public String getCodigoNavio() {
        return codigoNavio;
    }

    public void setCodigoNavio(String codigoNavio) {
        this.codigoNavio = codigoNavio;
    }

    public LocalDateTime getTempoGeracaoPlano() {
        return tempoGeracaoPlano;
    }

    public void setTempoGeracaoPlano(LocalDateTime tempoGeracaoPlano) {
        this.tempoGeracaoPlano = tempoGeracaoPlano;
    }

    public List<EquipmentRouteDto> getRotasEquipamento() {
        return rotasEquipamento;
    }

    public void setRotasEquipamento(List<EquipmentRouteDto> rotasEquipamento) {
        this.rotasEquipamento = rotasEquipamento;
    }

    public List<DualCycleJobDto> getJobsDualCycle() {
        return jobsDualCycle;
    }

    public void setJobsDualCycle(List<DualCycleJobDto> jobsDualCycle) {
        this.jobsDualCycle = jobsDualCycle;
    }

    public Integer getTotalOperacoes() {
        return totalOperacoes;
    }

    public void setTotalOperacoes(Integer totalOperacoes) {
        this.totalOperacoes = totalOperacoes;
    }

    public Integer getOperacoesDualCycle() {
        return operacoesDualCycle;
    }

    public void setOperacoesDualCycle(Integer operacoesDualCycle) {
        this.operacoesDualCycle = operacoesDualCycle;
    }

    public Integer getDistanciaEconomizada() {
        return distanciaEconomizada;
    }

    public void setDistanciaEconomizada(Integer distanciaEconomizada) {
        this.distanciaEconomizada = distanciaEconomizada;
    }

    public Double getEficienciaMedia() {
        return eficienciaMedia;
    }

    public void setEficienciaMedia(Double eficienciaMedia) {
        this.eficienciaMedia = eficienciaMedia;
    }

    public String getStatusGeral() {
        return statusGeral;
    }

    public void setStatusGeral(String statusGeral) {
        this.statusGeral = statusGeral;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
