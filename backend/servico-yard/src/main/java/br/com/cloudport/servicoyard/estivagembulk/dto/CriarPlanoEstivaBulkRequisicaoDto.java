package br.com.cloudport.servicoyard.estivagembulk.dto;

import javax.validation.constraints.NotNull;

public class CriarPlanoEstivaBulkRequisicaoDto {
    @NotNull private Long navioId;
    @NotNull private Long visitaNavioId;
    private String codigoViagem;
    private String portoCarga;
    private String portoDescarga;
    public Long getNavioId() { return navioId; }
    public void setNavioId(Long navioId) { this.navioId = navioId; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public String getPortoCarga() { return portoCarga; }
    public void setPortoCarga(String portoCarga) { this.portoCarga = portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
}
