package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;

public class ConteinerMapaDto {

    private Long id;
    private String codigo;
    private Integer linha;
    private Integer coluna;
    private StatusConteiner status;
    private String tipoCarga;
    private String destino;
    private String camadaOperacional;

    public ConteinerMapaDto() {
    }

    public ConteinerMapaDto(Long id, String codigo, Integer linha, Integer coluna, StatusConteiner status,
                             String tipoCarga, String destino, String camadaOperacional) {
        this.id = id;
        this.codigo = codigo;
        this.linha = linha;
        this.coluna = coluna;
        this.status = status;
        this.tipoCarga = tipoCarga;
        this.destino = destino;
        this.camadaOperacional = camadaOperacional;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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

    public StatusConteiner getStatus() {
        return status;
    }

    public void setStatus(StatusConteiner status) {
        this.status = status;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getCamadaOperacional() {
        return camadaOperacional;
    }

    public void setCamadaOperacional(String camadaOperacional) {
        this.camadaOperacional = camadaOperacional;
    }
}
