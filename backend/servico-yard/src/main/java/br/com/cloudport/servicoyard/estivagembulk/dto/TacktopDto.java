package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.List;

public class TacktopDto {

    private int numeroBobinasTopLayer;
    private double anguloInclinacaoGraus;
    private List<MaterialLashingDto> materiaisNecessarios;
    private double pesoTotalLashingKg;
    private String observacoes;

    public TacktopDto() {
    }

    public TacktopDto(int numeroBobinasTopLayer, double anguloInclinacaoGraus,
            List<MaterialLashingDto> materiaisNecessarios, double pesoTotalLashingKg, String observacoes) {
        this.numeroBobinasTopLayer = numeroBobinasTopLayer;
        this.anguloInclinacaoGraus = anguloInclinacaoGraus;
        this.materiaisNecessarios = materiaisNecessarios;
        this.pesoTotalLashingKg = pesoTotalLashingKg;
        this.observacoes = observacoes;
    }

    public int getNumeroBobinasTopLayer() {
        return numeroBobinasTopLayer;
    }

    public void setNumeroBobinasTopLayer(int numeroBobinasTopLayer) {
        this.numeroBobinasTopLayer = numeroBobinasTopLayer;
    }

    public double getAnguloInclinacaoGraus() {
        return anguloInclinacaoGraus;
    }

    public void setAnguloInclinacaoGraus(double anguloInclinacaoGraus) {
        this.anguloInclinacaoGraus = anguloInclinacaoGraus;
    }

    public List<MaterialLashingDto> getMateriaisNecessarios() {
        return materiaisNecessarios;
    }

    public void setMateriaisNecessarios(List<MaterialLashingDto> materiaisNecessarios) {
        this.materiaisNecessarios = materiaisNecessarios;
    }

    public double getPesoTotalLashingKg() {
        return pesoTotalLashingKg;
    }

    public void setPesoTotalLashingKg(double pesoTotalLashingKg) {
        this.pesoTotalLashingKg = pesoTotalLashingKg;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
