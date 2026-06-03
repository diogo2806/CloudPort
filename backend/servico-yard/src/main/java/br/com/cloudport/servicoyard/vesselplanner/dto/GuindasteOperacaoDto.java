package br.com.cloudport.servicoyard.vesselplanner.dto;

public class GuindasteOperacaoDto {

    private int ordem;
    private String codigoContainer;
    private int bay;
    private int rowBay;
    private int tier;
    private String tipoOperacao;
    private int guindasteId;

    public GuindasteOperacaoDto() {
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public String getCodigoContainer() {
        return codigoContainer;
    }

    public void setCodigoContainer(String codigoContainer) {
        this.codigoContainer = codigoContainer;
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

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(String tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public int getGuindasteId() {
        return guindasteId;
    }

    public void setGuindasteId(int guindasteId) {
        this.guindasteId = guindasteId;
    }
}
