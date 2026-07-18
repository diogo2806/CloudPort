package br.com.cloudport.servicoyard.scheduler.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private List<SchedulerAssignmentDto> atribuicoesReplanejamento;
    private Map<String, Double> memoriaCalculo;
    private List<String> justificativas;
    private String assinaturaEntrada;
    private Integer rehandlesEstimados;
    private Integer distanciaOriginal;
    private Integer distanciaOtimizada;
    private Double pontuacaoTotal;

    public SchedulerResultDto() {
        this.rotasEquipamento = new ArrayList<>();
        this.jobsDualCycle = new ArrayList<>();
        this.atribuicoesReplanejamento = new ArrayList<>();
        this.memoriaCalculo = new LinkedHashMap<>();
        this.justificativas = new ArrayList<>();
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
                .mapToInt(rota -> rota.getParadas().size())
                .sum();
        this.operacoesDualCycle = jobsDualCycle.size();
        this.distanciaEconomizada = jobsDualCycle.stream()
                .mapToInt(DualCycleJobDto::calcularEconomiaDistancia)
                .sum();
        if (!jobsDualCycle.isEmpty()) {
            this.eficienciaMedia = jobsDualCycle.stream()
                    .mapToDouble(DualCycleJobDto::calcularEficiencia)
                    .average()
                    .orElse(0.0);
        } else {
            this.eficienciaMedia = 0.0;
        }
        determinarStatusGeral();
    }

    private void determinarStatusGeral() {
        if (totalOperacoes == 0 && atribuicoesReplanejamento.isEmpty()) {
            this.statusGeral = "SEM_OPERACOES";
        } else if (!atribuicoesReplanejamento.isEmpty()
                && justificativas.stream().noneMatch(texto -> texto.startsWith("Nenhuma posicao"))) {
            this.statusGeral = "OTIMIZADO_REAL";
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

    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public LocalDateTime getTempoGeracaoPlano() { return tempoGeracaoPlano; }
    public void setTempoGeracaoPlano(LocalDateTime tempoGeracaoPlano) { this.tempoGeracaoPlano = tempoGeracaoPlano; }
    public List<EquipmentRouteDto> getRotasEquipamento() { return rotasEquipamento; }
    public void setRotasEquipamento(List<EquipmentRouteDto> rotasEquipamento) { this.rotasEquipamento = rotasEquipamento == null ? new ArrayList<>() : rotasEquipamento; }
    public List<DualCycleJobDto> getJobsDualCycle() { return jobsDualCycle; }
    public void setJobsDualCycle(List<DualCycleJobDto> jobsDualCycle) { this.jobsDualCycle = jobsDualCycle == null ? new ArrayList<>() : jobsDualCycle; }
    public Integer getTotalOperacoes() { return totalOperacoes; }
    public void setTotalOperacoes(Integer totalOperacoes) { this.totalOperacoes = totalOperacoes; }
    public Integer getOperacoesDualCycle() { return operacoesDualCycle; }
    public void setOperacoesDualCycle(Integer operacoesDualCycle) { this.operacoesDualCycle = operacoesDualCycle; }
    public Integer getDistanciaEconomizada() { return distanciaEconomizada; }
    public void setDistanciaEconomizada(Integer distanciaEconomizada) { this.distanciaEconomizada = distanciaEconomizada; }
    public Double getEficienciaMedia() { return eficienciaMedia; }
    public void setEficienciaMedia(Double eficienciaMedia) { this.eficienciaMedia = eficienciaMedia; }
    public String getStatusGeral() { return statusGeral; }
    public void setStatusGeral(String statusGeral) { this.statusGeral = statusGeral; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public List<SchedulerAssignmentDto> getAtribuicoesReplanejamento() { return atribuicoesReplanejamento; }
    public void setAtribuicoesReplanejamento(List<SchedulerAssignmentDto> atribuicoesReplanejamento) { this.atribuicoesReplanejamento = atribuicoesReplanejamento == null ? new ArrayList<>() : new ArrayList<>(atribuicoesReplanejamento); }
    public Map<String, Double> getMemoriaCalculo() { return memoriaCalculo; }
    public void setMemoriaCalculo(Map<String, Double> memoriaCalculo) { this.memoriaCalculo = memoriaCalculo == null ? new LinkedHashMap<>() : new LinkedHashMap<>(memoriaCalculo); }
    public List<String> getJustificativas() { return justificativas; }
    public void setJustificativas(List<String> justificativas) { this.justificativas = justificativas == null ? new ArrayList<>() : new ArrayList<>(justificativas); }
    public String getAssinaturaEntrada() { return assinaturaEntrada; }
    public void setAssinaturaEntrada(String assinaturaEntrada) { this.assinaturaEntrada = assinaturaEntrada; }
    public Integer getRehandlesEstimados() { return rehandlesEstimados; }
    public void setRehandlesEstimados(Integer rehandlesEstimados) { this.rehandlesEstimados = rehandlesEstimados; }
    public Integer getDistanciaOriginal() { return distanciaOriginal; }
    public void setDistanciaOriginal(Integer distanciaOriginal) { this.distanciaOriginal = distanciaOriginal; }
    public Integer getDistanciaOtimizada() { return distanciaOtimizada; }
    public void setDistanciaOtimizada(Integer distanciaOtimizada) { this.distanciaOtimizada = distanciaOtimizada; }
    public Double getPontuacaoTotal() { return pontuacaoTotal; }
    public void setPontuacaoTotal(Double pontuacaoTotal) { this.pontuacaoTotal = pontuacaoTotal; }
}
