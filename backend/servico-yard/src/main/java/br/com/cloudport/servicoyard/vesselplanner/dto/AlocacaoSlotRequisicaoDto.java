package br.com.cloudport.servicoyard.vesselplanner.dto;

public class AlocacaoSlotRequisicaoDto {

    private String codigoContainer;
    private Long slotDestinoId;
    private String isoCode;
    private Double pesoKg;
    private String portoCarga;
    private String portoDescarga;
    private String classeImo;
    private boolean reefer;

    public AlocacaoSlotRequisicaoDto() {
    }

    public String getCodigoContainer() {
        return codigoContainer;
    }

    public void setCodigoContainer(String codigoContainer) {
        this.codigoContainer = codigoContainer;
    }

    public Long getSlotDestinoId() {
        return slotDestinoId;
    }

    public void setSlotDestinoId(Long slotDestinoId) {
        this.slotDestinoId = slotDestinoId;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public String getPortoCarga() {
        return portoCarga;
    }

    public void setPortoCarga(String portoCarga) {
        this.portoCarga = portoCarga;
    }

    public String getPortoDescarga() {
        return portoDescarga;
    }

    public void setPortoDescarga(String portoDescarga) {
        this.portoDescarga = portoDescarga;
    }

    public String getClasseImo() {
        return classeImo;
    }

    public void setClasseImo(String classeImo) {
        this.classeImo = classeImo;
    }

    public boolean isReefer() {
        return reefer;
    }

    public void setReefer(boolean reefer) {
        this.reefer = reefer;
    }
}
