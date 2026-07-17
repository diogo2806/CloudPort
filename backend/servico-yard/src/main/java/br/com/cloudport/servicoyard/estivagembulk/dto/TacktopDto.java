package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class TacktopDto {

    private int numeroBobinasTopLayer;
    private Double anguloInclinacaoGraus;
    private List<MaterialLashingDto> materiaisNecessarios = new ArrayList<>();
    private double pesoTotalLashingKg;
    private double forcaRequeridaTotalKn;
    private double capacidadeDisponivelTotalKn;
    private boolean aprovado;
    private List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
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

    public Double getAnguloInclinacaoGraus() {
        return anguloInclinacaoGraus;
    }

    public void setAnguloInclinacaoGraus(Double anguloInclinacaoGraus) {
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

    public double getForcaRequeridaTotalKn() {
        return forcaRequeridaTotalKn;
    }

    public void setForcaRequeridaTotalKn(double forcaRequeridaTotalKn) {
        this.forcaRequeridaTotalKn = forcaRequeridaTotalKn;
    }

    public double getCapacidadeDisponivelTotalKn() {
        return capacidadeDisponivelTotalKn;
    }

    public void setCapacidadeDisponivelTotalKn(double capacidadeDisponivelTotalKn) {
        this.capacidadeDisponivelTotalKn = capacidadeDisponivelTotalKn;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public void setAprovado(boolean aprovado) {
        this.aprovado = aprovado;
    }

    public List<ViolacaoEstivaDto> getViolacoes() {
        return violacoes;
    }

    public void setViolacoes(List<ViolacaoEstivaDto> violacoes) {
        this.violacoes = violacoes;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
