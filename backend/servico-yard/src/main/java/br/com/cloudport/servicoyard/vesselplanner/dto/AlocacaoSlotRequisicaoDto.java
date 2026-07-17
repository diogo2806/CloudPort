package br.com.cloudport.servicoyard.vesselplanner.dto;

import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;

public class AlocacaoSlotRequisicaoDto {

    private String codigoContainer;
    private Long slotDestinoId;
    private String isoCode;
    private Double pesoKg;
    private Double pesoVgmKg;
    private EstadoCargaContainer estadoCarga;
    private String portoCarga;
    private String portoDescarga;
    private String classeImo;
    private String numeroOnu;
    private String grupoSegregacao;
    private boolean perigoso;
    private boolean reefer;
    private Double temperaturaRequeridaC;
    private Double temperaturaMinimaC;
    private Double temperaturaMaximaC;
    private boolean oog;
    private Double excessoFrontalCm;
    private Double excessoTraseiroCm;
    private Double excessoEsquerdoCm;
    private Double excessoDireitoCm;
    private Double excessoAlturaCm;

    public String getCodigoContainer() { return codigoContainer; }
    public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
    public Long getSlotDestinoId() { return slotDestinoId; }
    public void setSlotDestinoId(Long slotDestinoId) { this.slotDestinoId = slotDestinoId; }
    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }
    public Double getPesoVgmKg() { return pesoVgmKg; }
    public void setPesoVgmKg(Double pesoVgmKg) { this.pesoVgmKg = pesoVgmKg; }
    public EstadoCargaContainer getEstadoCarga() { return estadoCarga; }
    public void setEstadoCarga(EstadoCargaContainer estadoCarga) { this.estadoCarga = estadoCarga; }
    public String getPortoCarga() { return portoCarga; }
    public void setPortoCarga(String portoCarga) { this.portoCarga = portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
    public String getClasseImo() { return classeImo; }
    public void setClasseImo(String classeImo) { this.classeImo = classeImo; }
    public String getNumeroOnu() { return numeroOnu; }
    public void setNumeroOnu(String numeroOnu) { this.numeroOnu = numeroOnu; }
    public String getGrupoSegregacao() { return grupoSegregacao; }
    public void setGrupoSegregacao(String grupoSegregacao) { this.grupoSegregacao = grupoSegregacao; }
    public boolean isPerigoso() { return perigoso; }
    public void setPerigoso(boolean perigoso) { this.perigoso = perigoso; }
    public boolean isReefer() { return reefer; }
    public void setReefer(boolean reefer) { this.reefer = reefer; }
    public Double getTemperaturaRequeridaC() { return temperaturaRequeridaC; }
    public void setTemperaturaRequeridaC(Double temperaturaRequeridaC) { this.temperaturaRequeridaC = temperaturaRequeridaC; }
    public Double getTemperaturaMinimaC() { return temperaturaMinimaC; }
    public void setTemperaturaMinimaC(Double temperaturaMinimaC) { this.temperaturaMinimaC = temperaturaMinimaC; }
    public Double getTemperaturaMaximaC() { return temperaturaMaximaC; }
    public void setTemperaturaMaximaC(Double temperaturaMaximaC) { this.temperaturaMaximaC = temperaturaMaximaC; }
    public boolean isOog() { return oog; }
    public void setOog(boolean oog) { this.oog = oog; }
    public Double getExcessoFrontalCm() { return excessoFrontalCm; }
    public void setExcessoFrontalCm(Double excessoFrontalCm) { this.excessoFrontalCm = excessoFrontalCm; }
    public Double getExcessoTraseiroCm() { return excessoTraseiroCm; }
    public void setExcessoTraseiroCm(Double excessoTraseiroCm) { this.excessoTraseiroCm = excessoTraseiroCm; }
    public Double getExcessoEsquerdoCm() { return excessoEsquerdoCm; }
    public void setExcessoEsquerdoCm(Double excessoEsquerdoCm) { this.excessoEsquerdoCm = excessoEsquerdoCm; }
    public Double getExcessoDireitoCm() { return excessoDireitoCm; }
    public void setExcessoDireitoCm(Double excessoDireitoCm) { this.excessoDireitoCm = excessoDireitoCm; }
    public Double getExcessoAlturaCm() { return excessoAlturaCm; }
    public void setExcessoAlturaCm(Double excessoAlturaCm) { this.excessoAlturaCm = excessoAlturaCm; }
}
