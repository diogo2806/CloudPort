package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.List;

public class AnaliseEmpilhamentoDto {

    private int totalCamadas;
    private double alturaFinalM;
    private boolean corredorOperacaoLivre;
    private double larguraCorredorM;
    private List<ViolacaoEstivaDto> violacoes;
    private String descricaoEmpilhamento;

    public AnaliseEmpilhamentoDto() {
    }

    public AnaliseEmpilhamentoDto(int totalCamadas, double alturaFinalM, boolean corredorOperacaoLivre,
            double larguraCorredorM, List<ViolacaoEstivaDto> violacoes, String descricaoEmpilhamento) {
        this.totalCamadas = totalCamadas;
        this.alturaFinalM = alturaFinalM;
        this.corredorOperacaoLivre = corredorOperacaoLivre;
        this.larguraCorredorM = larguraCorredorM;
        this.violacoes = violacoes;
        this.descricaoEmpilhamento = descricaoEmpilhamento;
    }

    public int getTotalCamadas() {
        return totalCamadas;
    }

    public void setTotalCamadas(int totalCamadas) {
        this.totalCamadas = totalCamadas;
    }

    public double getAlturaFinalM() {
        return alturaFinalM;
    }

    public void setAlturaFinalM(double alturaFinalM) {
        this.alturaFinalM = alturaFinalM;
    }

    public boolean isCorredorOperacaoLivre() {
        return corredorOperacaoLivre;
    }

    public void setCorredorOperacaoLivre(boolean corredorOperacaoLivre) {
        this.corredorOperacaoLivre = corredorOperacaoLivre;
    }

    public double getLarguraCorredorM() {
        return larguraCorredorM;
    }

    public void setLarguraCorredorM(double larguraCorredorM) {
        this.larguraCorredorM = larguraCorredorM;
    }

    public List<ViolacaoEstivaDto> getViolacoes() {
        return violacoes;
    }

    public void setViolacoes(List<ViolacaoEstivaDto> violacoes) {
        this.violacoes = violacoes;
    }

    public String getDescricaoEmpilhamento() {
        return descricaoEmpilhamento;
    }

    public void setDescricaoEmpilhamento(String descricaoEmpilhamento) {
        this.descricaoEmpilhamento = descricaoEmpilhamento;
    }
}
