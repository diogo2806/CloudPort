package br.com.cloudport.servicoyard.vesselplanner.dto;

import javax.validation.constraints.NotNull;

public class CriarEstivagemPlanRequisicaoDto {
    @NotNull private Long bayPlanId;
    @NotNull private Long visitaNavioId;
    public Long getBayPlanId() { return bayPlanId; }
    public void setBayPlanId(Long bayPlanId) { this.bayPlanId = bayPlanId; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
}
