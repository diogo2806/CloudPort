package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.util.List;

public class EstivagemPlanDto {

    private Long id;
    private Long bayPlanId;
    private Long navioCadastroId;
    private Long visitaNavioId;
    private String codigoVisita;
    private Long versaoNavioCanonico;
    private Long versaoVisita;
    private String codigoNavio;
    private String codigoViagem;
    private String status;
    private EstabilidadeDto estabilidade;
    private List<SlotNavioDto> slots;
    private int totalContainers;
    private int totalSlotsOcupados;

    public EstivagemPlanDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBayPlanId() { return bayPlanId; }
    public void setBayPlanId(Long bayPlanId) { this.bayPlanId = bayPlanId; }
    public Long getNavioCadastroId() { return navioCadastroId; }
    public void setNavioCadastroId(Long navioCadastroId) { this.navioCadastroId = navioCadastroId; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getCodigoVisita() { return codigoVisita; }
    public void setCodigoVisita(String codigoVisita) { this.codigoVisita = codigoVisita; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; }
    public void setVersaoNavioCanonico(Long versaoNavioCanonico) { this.versaoNavioCanonico = versaoNavioCanonico; }
    public Long getVersaoVisita() { return versaoVisita; }
    public void setVersaoVisita(Long versaoVisita) { this.versaoVisita = versaoVisita; }
    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public EstabilidadeDto getEstabilidade() { return estabilidade; }
    public void setEstabilidade(EstabilidadeDto estabilidade) { this.estabilidade = estabilidade; }
    public List<SlotNavioDto> getSlots() { return slots; }
    public void setSlots(List<SlotNavioDto> slots) { this.slots = slots; }
    public int getTotalContainers() { return totalContainers; }
    public void setTotalContainers(int totalContainers) { this.totalContainers = totalContainers; }
    public int getTotalSlotsOcupados() { return totalSlotsOcupados; }
    public void setTotalSlotsOcupados(int totalSlotsOcupados) { this.totalSlotsOcupados = totalSlotsOcupados; }
}
