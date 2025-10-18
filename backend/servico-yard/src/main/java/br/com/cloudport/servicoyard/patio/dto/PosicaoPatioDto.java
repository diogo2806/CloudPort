package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;

public class PosicaoPatioDto {

    private Long id;
    private Integer linha;
    private Integer coluna;
    private String camadaOperacional;
    private boolean ocupada;
    private String codigoConteiner;
    private StatusConteiner statusConteiner;

    public PosicaoPatioDto() {
    }

    public PosicaoPatioDto(Long id, Integer linha, Integer coluna, String camadaOperacional,
                           boolean ocupada, String codigoConteiner, StatusConteiner statusConteiner) {
        this.id = id;
        this.linha = linha;
        this.coluna = coluna;
        this.camadaOperacional = camadaOperacional;
        this.ocupada = ocupada;
        this.codigoConteiner = codigoConteiner;
        this.statusConteiner = statusConteiner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public String getCamadaOperacional() {
        return camadaOperacional;
    }

    public void setCamadaOperacional(String camadaOperacional) {
        this.camadaOperacional = camadaOperacional;
    }

    public boolean isOcupada() {
        return ocupada;
    }

    public void setOcupada(boolean ocupada) {
        this.ocupada = ocupada;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public StatusConteiner getStatusConteiner() {
        return statusConteiner;
    }

    public void setStatusConteiner(StatusConteiner statusConteiner) {
        this.statusConteiner = statusConteiner;
    }
}
