package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.time.LocalDateTime;

public class MovimentoPatioDto {

    private Long id;
    private String codigoConteiner;
    private TipoMovimentoPatio tipoMovimento;
    private String descricao;
    private String destino;
    private Integer linha;
    private Integer coluna;
    private String camadaOperacional;
    private LocalDateTime registradoEm;

    public MovimentoPatioDto() {
    }

    public MovimentoPatioDto(Long id, String codigoConteiner, TipoMovimentoPatio tipoMovimento,
                              String descricao, String destino, Integer linha, Integer coluna,
                              String camadaOperacional, LocalDateTime registradoEm) {
        this.id = id;
        this.codigoConteiner = codigoConteiner;
        this.tipoMovimento = tipoMovimento;
        this.descricao = descricao;
        this.destino = destino;
        this.linha = linha;
        this.coluna = coluna;
        this.camadaOperacional = camadaOperacional;
        this.registradoEm = registradoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public TipoMovimentoPatio getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) {
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
