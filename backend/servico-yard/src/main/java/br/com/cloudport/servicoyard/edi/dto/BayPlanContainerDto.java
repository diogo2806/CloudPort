package br.com.cloudport.servicoyard.edi.dto;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import java.time.LocalDateTime;

public class BayPlanContainerDto {

    private Long id;
    private String codigoContainer;
    private String isoCode;
    private Integer bay;
    private Integer row;
    private Integer tier;
    private String posicaoBayEdifact;
    private String portoCarga;
    private String portoDescarga;
    private Double pesoKg;
    private String referenciaBl;
    private TipoOperacaoBayPlan tipoOperacao;
    private String statusOperacao;
    private LocalDateTime horarioOperacao;
    private Integer linhaYard;
    private Integer colunaYard;

    public static BayPlanContainerDto deEntidade(BayPlanContainer e) {
        BayPlanContainerDto dto = new BayPlanContainerDto();
        dto.id = e.getId();
        dto.codigoContainer = e.getCodigoContainer();
        dto.isoCode = e.getIsoCode();
        if (e.getPosicaoBay() != null) {
            dto.bay = e.getPosicaoBay().getBay();
            dto.row = e.getPosicaoBay().getRow();
            dto.tier = e.getPosicaoBay().getTier();
            dto.posicaoBayEdifact = e.getPosicaoBay().toCodigoEdifact();
        }
        dto.portoCarga = e.getPortoCarga();
        dto.portoDescarga = e.getPortoDescarga();
        dto.pesoKg = e.getPesoKg();
        dto.referenciaBl = e.getReferenciaBl();
        dto.tipoOperacao = e.getTipoOperacao();
        dto.statusOperacao = e.getStatusOperacao();
        dto.horarioOperacao = e.getHorarioOperacao();
        dto.linhaYard = e.getLinhaYard();
        dto.colunaYard = e.getColunaYard();
        return dto;
    }

    public Long getId() { return id; }
    public String getCodigoContainer() { return codigoContainer; }
    public String getIsoCode() { return isoCode; }
    public Integer getBay() { return bay; }
    public Integer getRow() { return row; }
    public Integer getTier() { return tier; }
    public String getPosicaoBayEdifact() { return posicaoBayEdifact; }
    public String getPortoCarga() { return portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public Double getPesoKg() { return pesoKg; }
    public String getReferenciaBl() { return referenciaBl; }
    public TipoOperacaoBayPlan getTipoOperacao() { return tipoOperacao; }
    public String getStatusOperacao() { return statusOperacao; }
    public LocalDateTime getHorarioOperacao() { return horarioOperacao; }
    public Integer getLinhaYard() { return linhaYard; }
    public Integer getColunaYard() { return colunaYard; }
}
