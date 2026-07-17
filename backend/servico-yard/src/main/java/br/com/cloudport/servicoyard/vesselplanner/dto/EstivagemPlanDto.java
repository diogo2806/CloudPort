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
    public EstivagemPlanDto() { }
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getBayPlanId() { return bayPlanId; } public void setBayPlanId(Long v) { bayPlanId = v; }
    public Long getNavioCadastroId() { return navioCadastroId; } public void setNavioCadastroId(Long v) { navioCadastroId = v; }
    public Long getVisitaNavioId() { return visitaNavioId; } public void setVisitaNavioId(Long v) { visitaNavioId = v; }
    public String getCodigoVisita() { return codigoVisita; } public void setCodigoVisita(String v) { codigoVisita = v; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; } public void setVersaoNavioCanonico(Long v) { versaoNavioCanonico = v; }
    public Long getVersaoVisita() { return versaoVisita; } public void setVersaoVisita(Long v) { versaoVisita = v; }
    public String getCodigoNavio() { return codigoNavio; } public void setCodigoNavio(String v) { codigoNavio = v; }
    public String getCodigoViagem() { return codigoViagem; } public void setCodigoViagem(String v) { codigoViagem = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public EstabilidadeDto getEstabilidade() { return estabilidade; } public void setEstabilidade(EstabilidadeDto v) { estabilidade = v; }
    public List<SlotNavioDto> getSlots() { return slots; } public void setSlots(List<SlotNavioDto> v) { slots = v; }
    public int getTotalContainers() { return totalContainers; } public void setTotalContainers(int v) { totalContainers = v; }
    public int getTotalSlotsOcupados() { return totalSlotsOcupados; } public void setTotalSlotsOcupados(int v) { totalSlotsOcupados = v; }
}
