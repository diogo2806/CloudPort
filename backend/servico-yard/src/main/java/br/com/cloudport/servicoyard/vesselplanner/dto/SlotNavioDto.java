package br.com.cloudport.servicoyard.vesselplanner.dto;

import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;

public class SlotNavioDto {

    private Long id;
    private int bay;
    private int rowBay;
    private int tier;
    private String tipoSlot;
    private String codigoHatchCover;
    private boolean sobreHatchCover;
    private boolean restrito;
    private String motivoRestricao;
    private boolean tomadaReefer;
    private boolean aceita20Pes;
    private boolean aceita40Pes;
    private boolean aceita45Pes;
    private Double maxPesoKg;
    private Double maxPesoPilhaKg;
    private String codigoContainer;
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
    private String statusAlertas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getBay() { return bay; }
    public void setBay(int bay) { this.bay = bay; }
    public int getRowBay() { return rowBay; }
    public void setRowBay(int rowBay) { this.rowBay = rowBay; }
    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }
    public String getTipoSlot() { return tipoSlot; }
    public void setTipoSlot(String tipoSlot) { this.tipoSlot = tipoSlot; }
    public String getCodigoHatchCover() { return codigoHatchCover; }
    public void setCodigoHatchCover(String codigoHatchCover) { this.codigoHatchCover = codigoHatchCover; }
    public boolean isSobreHatchCover() { return sobreHatchCover; }
    public void setSobreHatchCover(boolean sobreHatchCover) { this.sobreHatchCover = sobreHatchCover; }
    public boolean isRestrito() { return restrito; }
    public void setRestrito(boolean restrito) { this.restrito = restrito; }
    public String getMotivoRestricao() { return motivoRestricao; }
    public void setMotivoRestricao(String motivoRestricao) { this.motivoRestricao = motivoRestricao; }
    public boolean isTomadaReefer() { return tomadaReefer; }
    public void setTomadaReefer(boolean tomadaReefer) { this.tomadaReefer = tomadaReefer; }
    public boolean isAceita20Pes() { return aceita20Pes; }
    public void setAceita20Pes(boolean aceita20Pes) { this.aceita20Pes = aceita20Pes; }
    public boolean isAceita40Pes() { return aceita40Pes; }
    public void setAceita40Pes(boolean aceita40Pes) { this.aceita40Pes = aceita40Pes; }
    public boolean isAceita45Pes() { return aceita45Pes; }
    public void setAceita45Pes(boolean aceita45Pes) { this.aceita45Pes = aceita45Pes; }
    public Double getMaxPesoKg() { return maxPesoKg; }
    public void setMaxPesoKg(Double maxPesoKg) { this.maxPesoKg = maxPesoKg; }
    public Double getMaxPesoPilhaKg() { return maxPesoPilhaKg; }
    public void setMaxPesoPilhaKg(Double maxPesoPilhaKg) { this.maxPesoPilhaKg = maxPesoPilhaKg; }
    public String getCodigoContainer() { return codigoContainer; }
    public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
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
    public String getStatusAlertas() { return statusAlertas; }
    public void setStatusAlertas(String statusAlertas) { this.statusAlertas = statusAlertas; }
}
