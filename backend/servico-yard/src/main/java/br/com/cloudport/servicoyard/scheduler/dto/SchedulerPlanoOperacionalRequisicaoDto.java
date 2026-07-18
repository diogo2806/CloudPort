package br.com.cloudport.servicoyard.scheduler.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class SchedulerPlanoOperacionalRequisicaoDto {

    @Valid
    @NotNull
    private VesselArrivalDto navio;

    @NotEmpty
    private List<String> equipamentosDisponiveis;

    @Valid
    @NotNull
    private List<SchedulerContainerDto> containersImportacao;

    @Valid
    @NotNull
    private List<SchedulerContainerDto> containersExportacao;

    @Valid
    private List<SchedulerPositionCandidateDto> posicoesCandidatas = List.of();

    @Valid
    private List<SchedulerEquipmentDto> equipamentosOperacionais = List.of();

    private LocalDateTime cutoffOperacional;
    private Map<String, BigDecimal> pesosCriterios = Map.of();

    public VesselArrivalDto getNavio() { return navio; }
    public void setNavio(VesselArrivalDto navio) { this.navio = navio; }
    public List<String> getEquipamentosDisponiveis() { return equipamentosDisponiveis; }
    public void setEquipamentosDisponiveis(List<String> equipamentosDisponiveis) { this.equipamentosDisponiveis = equipamentosDisponiveis; }
    public List<SchedulerContainerDto> getContainersImportacao() { return containersImportacao; }
    public void setContainersImportacao(List<SchedulerContainerDto> containersImportacao) { this.containersImportacao = containersImportacao; }
    public List<SchedulerContainerDto> getContainersExportacao() { return containersExportacao; }
    public void setContainersExportacao(List<SchedulerContainerDto> containersExportacao) { this.containersExportacao = containersExportacao; }
    public List<SchedulerPositionCandidateDto> getPosicoesCandidatas() { return posicoesCandidatas == null ? List.of() : posicoesCandidatas; }
    public void setPosicoesCandidatas(List<SchedulerPositionCandidateDto> posicoesCandidatas) { this.posicoesCandidatas = posicoesCandidatas; }
    public List<SchedulerEquipmentDto> getEquipamentosOperacionais() { return equipamentosOperacionais == null ? List.of() : equipamentosOperacionais; }
    public void setEquipamentosOperacionais(List<SchedulerEquipmentDto> equipamentosOperacionais) { this.equipamentosOperacionais = equipamentosOperacionais; }
    public LocalDateTime getCutoffOperacional() { return cutoffOperacional; }
    public void setCutoffOperacional(LocalDateTime cutoffOperacional) { this.cutoffOperacional = cutoffOperacional; }
    public Map<String, BigDecimal> getPesosCriterios() { return pesosCriterios == null ? Map.of() : pesosCriterios; }
    public void setPesosCriterios(Map<String, BigDecimal> pesosCriterios) { this.pesosCriterios = pesosCriterios; }
}
