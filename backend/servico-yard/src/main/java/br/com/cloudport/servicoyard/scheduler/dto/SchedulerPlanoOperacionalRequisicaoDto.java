package br.com.cloudport.servicoyard.scheduler.dto;

import java.util.List;
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

    public VesselArrivalDto getNavio() { return navio; }
    public void setNavio(VesselArrivalDto navio) { this.navio = navio; }
    public List<String> getEquipamentosDisponiveis() { return equipamentosDisponiveis; }
    public void setEquipamentosDisponiveis(List<String> equipamentosDisponiveis) { this.equipamentosDisponiveis = equipamentosDisponiveis; }
    public List<SchedulerContainerDto> getContainersImportacao() { return containersImportacao; }
    public void setContainersImportacao(List<SchedulerContainerDto> containersImportacao) { this.containersImportacao = containersImportacao; }
    public List<SchedulerContainerDto> getContainersExportacao() { return containersExportacao; }
    public void setContainersExportacao(List<SchedulerContainerDto> containersExportacao) { this.containersExportacao = containersExportacao; }
}
