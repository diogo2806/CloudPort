package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ValidacaoPlanoBulkDto {

    private boolean aprovado;
    private Long versaoPlano;
    private String versaoEspecificacao;
    private String referenciaRegra;
    private String responsavelValidacao;
    private LocalDateTime validadoEm;
    private EstabilidadeEstrutural estabilidade;
    private List<PressaoTanktopDto> analisesTanktop = new ArrayList<>();
    private List<AnaliseEmpilhamentoDto> analisesEmpilhamento = new ArrayList<>();
    private TacktopDto analiseSecuring;
    private List<ViolacaoEstivaDto> violacoes = new ArrayList<>();

    public boolean isAprovado() {
        return aprovado;
    }

    public void setAprovado(boolean aprovado) {
        this.aprovado = aprovado;
    }

    public Long getVersaoPlano() {
        return versaoPlano;
    }

    public void setVersaoPlano(Long versaoPlano) {
        this.versaoPlano = versaoPlano;
    }

    public String getVersaoEspecificacao() {
        return versaoEspecificacao;
    }

    public void setVersaoEspecificacao(String versaoEspecificacao) {
        this.versaoEspecificacao = versaoEspecificacao;
    }

    public String getReferenciaRegra() {
        return referenciaRegra;
    }

    public void setReferenciaRegra(String referenciaRegra) {
        this.referenciaRegra = referenciaRegra;
    }

    public String getResponsavelValidacao() {
        return responsavelValidacao;
    }

    public void setResponsavelValidacao(String responsavelValidacao) {
        this.responsavelValidacao = responsavelValidacao;
    }

    public LocalDateTime getValidadoEm() {
        return validadoEm;
    }

    public void setValidadoEm(LocalDateTime validadoEm) {
        this.validadoEm = validadoEm;
    }

    public EstabilidadeEstrutural getEstabilidade() {
        return estabilidade;
    }

    public void setEstabilidade(EstabilidadeEstrutural estabilidade) {
        this.estabilidade = estabilidade;
    }

    public List<PressaoTanktopDto> getAnalisesTanktop() {
        return analisesTanktop;
    }

    public void setAnalisesTanktop(List<PressaoTanktopDto> analisesTanktop) {
        this.analisesTanktop = analisesTanktop;
    }

    public List<AnaliseEmpilhamentoDto> getAnalisesEmpilhamento() {
        return analisesEmpilhamento;
    }

    public void setAnalisesEmpilhamento(List<AnaliseEmpilhamentoDto> analisesEmpilhamento) {
        this.analisesEmpilhamento = analisesEmpilhamento;
    }

    public TacktopDto getAnaliseSecuring() {
        return analiseSecuring;
    }

    public void setAnaliseSecuring(TacktopDto analiseSecuring) {
        this.analiseSecuring = analiseSecuring;
    }

    public List<ViolacaoEstivaDto> getViolacoes() {
        return violacoes;
    }

    public void setViolacoes(List<ViolacaoEstivaDto> violacoes) {
        this.violacoes = violacoes;
    }
}
