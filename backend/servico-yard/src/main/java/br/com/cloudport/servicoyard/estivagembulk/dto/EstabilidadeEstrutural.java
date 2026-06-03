package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class EstabilidadeEstrutural {

    private double bmMaxKnm;
    private double sfMaxKn;
    private double trimMetros;
    private double caladoSaidaMetros;
    private double pesoTotalToneladas;
    private boolean hogging;
    private boolean sagging;
    private boolean aprovado;
    private List<ViolacaoEstivaDto> violacoes;

    public EstabilidadeEstrutural() {
    }

    public EstabilidadeEstrutural(double bmMaxKnm, double sfMaxKn, double trimMetros,
            double caladoSaidaMetros, double pesoTotalToneladas, boolean hogging, boolean sagging,
            boolean aprovado, List<ViolacaoEstivaDto> violacoes) {
        this.bmMaxKnm = bmMaxKnm;
        this.sfMaxKn = sfMaxKn;
        this.trimMetros = trimMetros;
        this.caladoSaidaMetros = caladoSaidaMetros;
        this.pesoTotalToneladas = pesoTotalToneladas;
        this.hogging = hogging;
        this.sagging = sagging;
        this.aprovado = aprovado;
        this.violacoes = violacoes;
    }

    public static EstabilidadeEstrutural vazia() {
        return new EstabilidadeEstrutural(0.0, 0.0, 0.0, 0.0, 0.0, false, false, true, new ArrayList<>());
    }

    public double getBmMaxKnm() {
        return bmMaxKnm;
    }

    public void setBmMaxKnm(double bmMaxKnm) {
        this.bmMaxKnm = bmMaxKnm;
    }

    public double getSfMaxKn() {
        return sfMaxKn;
    }

    public void setSfMaxKn(double sfMaxKn) {
        this.sfMaxKn = sfMaxKn;
    }

    public double getTrimMetros() {
        return trimMetros;
    }

    public void setTrimMetros(double trimMetros) {
        this.trimMetros = trimMetros;
    }

    public double getCaladoSaidaMetros() {
        return caladoSaidaMetros;
    }

    public void setCaladoSaidaMetros(double caladoSaidaMetros) {
        this.caladoSaidaMetros = caladoSaidaMetros;
    }

    public double getPesoTotalToneladas() {
        return pesoTotalToneladas;
    }

    public void setPesoTotalToneladas(double pesoTotalToneladas) {
        this.pesoTotalToneladas = pesoTotalToneladas;
    }

    public boolean isHogging() {
        return hogging;
    }

    public void setHogging(boolean hogging) {
        this.hogging = hogging;
    }

    public boolean isSagging() {
        return sagging;
    }

    public void setSagging(boolean sagging) {
        this.sagging = sagging;
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
}
