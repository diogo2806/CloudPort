package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;

public class ConteinerBuscaDTO {

    private String containerId;
    private String statusAtual;
    private String zona;
    private String posicao;
    private Double latitude;
    private Double longitude;
    private String navioDestinoId;
    private LocalDateTime dataAtualizacao;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(String statusAtual) {
        this.statusAtual = statusAtual;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getPosicao() {
        return posicao;
    }

    public void setPosicao(String posicao) {
        this.posicao = posicao;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getNavioDestinoId() {
        return navioDestinoId;
    }

    public void setNavioDestinoId(String navioDestinoId) {
        this.navioDestinoId = navioDestinoId;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
}
