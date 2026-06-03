package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.util.ArrayList;
import java.util.List;

public class EstabilidadeDto {

    private double trimMetros;
    private double listGraus;
    private double lcgMetros;
    private double tcgMetros;
    private double vcgMetros;
    private double pesoTotalToneladas;
    private boolean aprovado;
    private List<ViolacaoHardConstraintDto> violacoes;

    public EstabilidadeDto() {
    }

    public static EstabilidadeDto vazia() {
        EstabilidadeDto dto = new EstabilidadeDto();
        dto.trimMetros = 0.0;
        dto.listGraus = 0.0;
        dto.lcgMetros = 0.0;
        dto.tcgMetros = 0.0;
        dto.vcgMetros = 0.0;
        dto.pesoTotalToneladas = 0.0;
        dto.aprovado = true;
        dto.violacoes = new ArrayList<>();
        return dto;
    }

    public double getTrimMetros() {
        return trimMetros;
    }

    public void setTrimMetros(double trimMetros) {
        this.trimMetros = trimMetros;
    }

    public double getListGraus() {
        return listGraus;
    }

    public void setListGraus(double listGraus) {
        this.listGraus = listGraus;
    }

    public double getLcgMetros() {
        return lcgMetros;
    }

    public void setLcgMetros(double lcgMetros) {
        this.lcgMetros = lcgMetros;
    }

    public double getTcgMetros() {
        return tcgMetros;
    }

    public void setTcgMetros(double tcgMetros) {
        this.tcgMetros = tcgMetros;
    }

    public double getVcgMetros() {
        return vcgMetros;
    }

    public void setVcgMetros(double vcgMetros) {
        this.vcgMetros = vcgMetros;
    }

    public double getPesoTotalToneladas() {
        return pesoTotalToneladas;
    }

    public void setPesoTotalToneladas(double pesoTotalToneladas) {
        this.pesoTotalToneladas = pesoTotalToneladas;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public void setAprovado(boolean aprovado) {
        this.aprovado = aprovado;
    }

    public List<ViolacaoHardConstraintDto> getViolacoes() {
        return violacoes;
    }

    public void setViolacoes(List<ViolacaoHardConstraintDto> violacoes) {
        this.violacoes = violacoes;
    }
}
