package br.com.cloudport.servicoyard.vesselplanner.dto;

public class SlotNavioDto {

    private Long id;
    private int bay;
    private int rowBay;
    private int tier;
    private String tipoSlot;
    private Double maxPesoKg;
    private String codigoContainer;
    private String isoCode;
    private Double pesoKg;
    private String portoCarga;
    private String portoDescarga;
    private String classeImo;
    private boolean reefer;
    private String statusAlertas;

    public SlotNavioDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getBay() {
        return bay;
    }

    public void setBay(int bay) {
        this.bay = bay;
    }

    public int getRowBay() {
        return rowBay;
    }

    public void setRowBay(int rowBay) {
        this.rowBay = rowBay;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public String getTipoSlot() {
        return tipoSlot;
    }

    public void setTipoSlot(String tipoSlot) {
        this.tipoSlot = tipoSlot;
    }

    public Double getMaxPesoKg() {
        return maxPesoKg;
    }

    public void setMaxPesoKg(Double maxPesoKg) {
        this.maxPesoKg = maxPesoKg;
    }

    public String getCodigoContainer() {
        return codigoContainer;
    }

    public void setCodigoContainer(String codigoContainer) {
        this.codigoContainer = codigoContainer;
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

    public String getStatusAlertas() {
        return statusAlertas;
    }

    public void setStatusAlertas(String statusAlertas) {
        this.statusAlertas = statusAlertas;
    }
}
