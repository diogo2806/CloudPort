package br.com.cloudport.servicoyard.vesselplanner.dto;

public class GuindasteOperacaoDto {

    private int ordem;
    private String codigoContainer;
    private int bay;
    private int rowBay;
    private int tier;
    private String tipoOperacao;
    private int guindasteId;
    private String codigoHatchCover;
    private boolean bloqueadoPorTampa;
    private String motivoBloqueioTampa;

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

    public String getCodigoHatchCover() {
        return codigoHatchCover;
    }

    public void setCodigoHatchCover(String codigoHatchCover) {
        this.codigoHatchCover = codigoHatchCover;
    }

    public boolean isBloqueadoPorTampa() {
        return bloqueadoPorTampa;
    }

    public void setBloqueadoPorTampa(boolean bloqueadoPorTampa) {
        this.bloqueadoPorTampa = bloqueadoPorTampa;
    }

    public String getMotivoBloqueioTampa() {
        return motivoBloqueioTampa;
    }

    public void setMotivoBloqueioTampa(String motivoBloqueioTampa) {
        this.motivoBloqueioTampa = motivoBloqueioTampa;
    }
}
