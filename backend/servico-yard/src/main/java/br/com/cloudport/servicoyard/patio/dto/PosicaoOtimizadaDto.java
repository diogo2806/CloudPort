package br.com.cloudport.servicoyard.patio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PosicaoOtimizadaDto {

    private Long containerId;

    private String codigoContainer;

    private Integer linha;

    private Integer coluna;

    private Integer nivel;

    private Integer sequenciaEmbarque;

    private Boolean otimizado;

    private String motivo;

    private Integer distanciaAoBerco;

    public PosicaoOtimizadaDto() {
    }

    public PosicaoOtimizadaDto(Long containerId, String codigoContainer, Integer linha,
                               Integer coluna, Integer nivel, Integer sequenciaEmbarque,
                               Boolean otimizado, String motivo) {
        this.containerId = containerId;
        this.codigoContainer = codigoContainer;
        this.linha = linha;
        this.coluna = coluna;
        this.nivel = nivel;
        this.sequenciaEmbarque = sequenciaEmbarque;
        this.otimizado = otimizado;
        this.motivo = motivo;
    }

    public Long getContainerId() {
        return containerId;
    }

    public void setContainerId(Long containerId) {
        this.containerId = containerId;
    }

    public String getCodigoContainer() {
        return codigoContainer;
    }

    public void setCodigoContainer(String codigoContainer) {
        this.codigoContainer = codigoContainer;
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

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public Integer getSequenciaEmbarque() {
        return sequenciaEmbarque;
    }

    public void setSequenciaEmbarque(Integer sequenciaEmbarque) {
        this.sequenciaEmbarque = sequenciaEmbarque;
    }

    public Boolean getOtimizado() {
        return otimizado;
    }

    public void setOtimizado(Boolean otimizado) {
        this.otimizado = otimizado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Integer getDistanciaAoBerco() {
        return distanciaAoBerco;
    }

    public void setDistanciaAoBerco(Integer distanciaAoBerco) {
        this.distanciaAoBerco = distanciaAoBerco;
    }

    @Override
    public String toString() {
        return "PosicaoOtimizadaDto{" +
                "codigo='" + codigoContainer + '\'' +
                ", posicao=(" + linha + "," + coluna + "," + nivel + ")" +
                ", sequencia=" + sequenciaEmbarque +
                ", otimizado=" + otimizado +
                '}';
    }
}
