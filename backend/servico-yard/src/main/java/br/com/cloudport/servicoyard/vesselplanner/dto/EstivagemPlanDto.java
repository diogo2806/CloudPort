package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.util.List;

public class EstivagemPlanDto {

    private Long id;
    private Long bayPlanId;
    private String codigoNavio;
    private String codigoViagem;
    private String status;
    private EstabilidadeDto estabilidade;
    private List<SlotNavioDto> slots;
    private int totalContainers;
    private int totalSlotsOcupados;

    public EstivagemPlanDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBayPlanId() {
        return bayPlanId;
    }

    public void setBayPlanId(Long bayPlanId) {
        this.bayPlanId = bayPlanId;
    }

    public String getCodigoNavio() {
        return codigoNavio;
    }

    public void setCodigoNavio(String codigoNavio) {
        this.codigoNavio = codigoNavio;
    }

    public String getCodigoViagem() {
        return codigoViagem;
    }

    public void setCodigoViagem(String codigoViagem) {
        this.codigoViagem = codigoViagem;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public EstabilidadeDto getEstabilidade() {
        return estabilidade;
    }

    public void setEstabilidade(EstabilidadeDto estabilidade) {
        this.estabilidade = estabilidade;
    }

    public List<SlotNavioDto> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotNavioDto> slots) {
        this.slots = slots;
    }

    public int getTotalContainers() {
        return totalContainers;
    }

    public void setTotalContainers(int totalContainers) {
        this.totalContainers = totalContainers;
    }

    public int getTotalSlotsOcupados() {
        return totalSlotsOcupados;
    }

    public void setTotalSlotsOcupados(int totalSlotsOcupados) {
        this.totalSlotsOcupados = totalSlotsOcupados;
    }
}
