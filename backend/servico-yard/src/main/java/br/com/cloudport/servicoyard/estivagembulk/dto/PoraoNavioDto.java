package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class PoraoNavioDto {

    private Long id;
    private int numero;
    private Double comprimento;
    private Double largura;
    private Double alturaUtil;
    private Double areaUtil;
    private Double anguloAntepara;
    private Double posLongInicio;
    private Double posLongFim;
    private List<SetorTanktopDto> setores = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public Double getComprimento() {
        return comprimento;
    }

    public void setComprimento(Double comprimento) {
        this.comprimento = comprimento;
    }

    public Double getLargura() {
        return largura;
    }

    public void setLargura(Double largura) {
        this.largura = largura;
    }

    public Double getAlturaUtil() {
        return alturaUtil;
    }

    public void setAlturaUtil(Double alturaUtil) {
        this.alturaUtil = alturaUtil;
    }

    public Double getAreaUtil() {
        return areaUtil;
    }

    public void setAreaUtil(Double areaUtil) {
        this.areaUtil = areaUtil;
    }

    public Double getAnguloAntepara() {
        return anguloAntepara;
    }

    public void setAnguloAntepara(Double anguloAntepara) {
        this.anguloAntepara = anguloAntepara;
    }

    public Double getPosLongInicio() {
        return posLongInicio;
    }

    public void setPosLongInicio(Double posLongInicio) {
        this.posLongInicio = posLongInicio;
    }

    public Double getPosLongFim() {
        return posLongFim;
    }

    public void setPosLongFim(Double posLongFim) {
        this.posLongFim = posLongFim;
    }

    public List<SetorTanktopDto> getSetores() {
        return setores;
    }

    public void setSetores(List<SetorTanktopDto> setores) {
        this.setores = setores;
    }
}
