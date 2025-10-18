package br.com.cloudport.servicoyard.patio.dto;

import java.time.LocalDateTime;

public class EventoMovimentoPatioDto {

    private String codigoConteiner;
    private String tipoMovimento;
    private String descricao;
    private String destino;
    private Integer linha;
    private Integer coluna;
    private String camadaOperacional;
    private LocalDateTime registradoEm;

    public EventoMovimentoPatioDto() {
    }

    public EventoMovimentoPatioDto(String codigoConteiner, String tipoMovimento, String descricao,
                                   String destino, Integer linha, Integer coluna,
                                   String camadaOperacional, LocalDateTime registradoEm) {
        this.codigoConteiner = codigoConteiner;
        this.tipoMovimento = tipoMovimento;
        this.descricao = descricao;
        this.destino = destino;
        this.linha = linha;
        this.coluna = coluna;
        this.camadaOperacional = camadaOperacional;
        this.registradoEm = registradoEm;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public String getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(String tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
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

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }
}
