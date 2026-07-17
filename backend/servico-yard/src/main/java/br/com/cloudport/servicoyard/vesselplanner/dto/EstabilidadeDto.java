package br.com.cloudport.servicoyard.vesselplanner.dto;

import java.util.ArrayList;
import java.util.List;

public class EstabilidadeDto {

    private double trimMetros;
    private double listGraus;
    private double caladoMedioMetros;
    private double gmMetros;
    private double lcgMetros;
    private double tcgMetros;
    private double vcgMetros;
    private double sfMaxKn;
    private double bmMaxKnm;
    private double pesoTotalToneladas;
    private boolean operacional;
    private boolean aprovado;
    private String versaoDadosHidrostaticos;
    private String versaoDadosEstruturais;
    private String memoriaCalculo;
    private List<ViolacaoHardConstraintDto> violacoes;

    public EstabilidadeDto() {
    }

    public static EstabilidadeDto vazia() {
        EstabilidadeDto dto = new EstabilidadeDto();
        dto.operacional = false;
        dto.aprovado = false;
        dto.violacoes = new ArrayList<>();
        return dto;
    }

    public double getTrimMetros() { return trimMetros; }
    public void setTrimMetros(double trimMetros) { this.trimMetros = trimMetros; }
    public double getListGraus() { return listGraus; }
    public void setListGraus(double listGraus) { this.listGraus = listGraus; }
    public double getCaladoMedioMetros() { return caladoMedioMetros; }
    public void setCaladoMedioMetros(double caladoMedioMetros) { this.caladoMedioMetros = caladoMedioMetros; }
    public double getGmMetros() { return gmMetros; }
    public void setGmMetros(double gmMetros) { this.gmMetros = gmMetros; }
    public double getLcgMetros() { return lcgMetros; }
    public void setLcgMetros(double lcgMetros) { this.lcgMetros = lcgMetros; }
    public double getTcgMetros() { return tcgMetros; }
    public void setTcgMetros(double tcgMetros) { this.tcgMetros = tcgMetros; }
    public double getVcgMetros() { return vcgMetros; }
    public void setVcgMetros(double vcgMetros) { this.vcgMetros = vcgMetros; }
    public double getSfMaxKn() { return sfMaxKn; }
    public void setSfMaxKn(double sfMaxKn) { this.sfMaxKn = sfMaxKn; }
    public double getBmMaxKnm() { return bmMaxKnm; }
    public void setBmMaxKnm(double bmMaxKnm) { this.bmMaxKnm = bmMaxKnm; }
    public double getPesoTotalToneladas() { return pesoTotalToneladas; }
    public void setPesoTotalToneladas(double pesoTotalToneladas) { this.pesoTotalToneladas = pesoTotalToneladas; }
    public boolean isOperacional() { return operacional; }
    public void setOperacional(boolean operacional) { this.operacional = operacional; }
    public boolean isAprovado() { return aprovado; }
    public void setAprovado(boolean aprovado) { this.aprovado = aprovado; }
    public String getVersaoDadosHidrostaticos() { return versaoDadosHidrostaticos; }
    public void setVersaoDadosHidrostaticos(String versaoDadosHidrostaticos) { this.versaoDadosHidrostaticos = versaoDadosHidrostaticos; }
    public String getVersaoDadosEstruturais() { return versaoDadosEstruturais; }
    public void setVersaoDadosEstruturais(String versaoDadosEstruturais) { this.versaoDadosEstruturais = versaoDadosEstruturais; }
    public String getMemoriaCalculo() { return memoriaCalculo; }
    public void setMemoriaCalculo(String memoriaCalculo) { this.memoriaCalculo = memoriaCalculo; }
    public List<ViolacaoHardConstraintDto> getViolacoes() { return violacoes; }
    public void setViolacoes(List<ViolacaoHardConstraintDto> violacoes) { this.violacoes = violacoes; }
}
