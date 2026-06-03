package br.com.cloudport.servicoyard.vesselplanner.dto;

public class RestowMovimentoDto {

    private String codigoContainer;
    private int bayAtual;
    private int rowAtual;
    private int tierAtual;
    private int bayDestino;
    private int rowDestino;
    private int tierDestino;
    private String motivoRestow;

    public RestowMovimentoDto() {
    }

    public String getCodigoContainer() {
        return codigoContainer;
    }

    public void setCodigoContainer(String codigoContainer) {
        this.codigoContainer = codigoContainer;
    }

    public int getBayAtual() {
        return bayAtual;
    }

    public void setBayAtual(int bayAtual) {
        this.bayAtual = bayAtual;
    }

    public int getRowAtual() {
        return rowAtual;
    }

    public void setRowAtual(int rowAtual) {
        this.rowAtual = rowAtual;
    }

    public int getTierAtual() {
        return tierAtual;
    }

    public void setTierAtual(int tierAtual) {
        this.tierAtual = tierAtual;
    }

    public int getBayDestino() {
        return bayDestino;
    }

    public void setBayDestino(int bayDestino) {
        this.bayDestino = bayDestino;
    }

    public int getRowDestino() {
        return rowDestino;
    }

    public void setRowDestino(int rowDestino) {
        this.rowDestino = rowDestino;
    }

    public int getTierDestino() {
        return tierDestino;
    }

    public void setTierDestino(int tierDestino) {
        this.tierDestino = tierDestino;
    }

    public String getMotivoRestow() {
        return motivoRestow;
    }

    public void setMotivoRestow(String motivoRestow) {
        this.motivoRestow = motivoRestow;
    }
}
