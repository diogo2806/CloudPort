package br.com.cloudport.servicoyard.patio.dto;

import java.io.Serializable;

public class ResultadoSimulacaoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String cenarioDescricao;

    // Métricas atuais
    private Integer totalConteineresCenarioCurrent;
    private Integer ocupacaoPatioCurrent;
    private Double rehandleRatioCurrent;
    private Integer equipamentosDisponiveisCurrent;

    // Métricas após simulação
    private Integer totalConteineresSimulado;
    private Integer ocupacaoPatioSimulado;
    private Double rehandleRatioSimulado;
    private Integer equipamentosDisponiveisSimulado;

    // Impacto
    private Integer deltaConteineres;
    private Integer deltaOcupacao;
    private Double deltaRehandleRatio;
    private Integer deltaEquipamentos;

    // Alertas do cenário
    private String alertaPrincipal;
    private String recomendacao;
    private String impactoProducao;

    public ResultadoSimulacaoDto() {
    }

    public ResultadoSimulacaoDto(String cenarioDescricao,
                                Integer totalConteineresCenarioCurrent,
                                Integer ocupacaoPatioCurrent,
                                Double rehandleRatioCurrent,
                                Integer equipamentosDisponiveisCurrent,
                                Integer totalConteineresSimulado,
                                Integer ocupacaoPatioSimulado,
                                Double rehandleRatioSimulado,
                                Integer equipamentosDisponiveisSimulado,
                                String alertaPrincipal,
                                String recomendacao,
                                String impactoProducao) {
        this.cenarioDescricao = cenarioDescricao;
        this.totalConteineresCenarioCurrent = totalConteineresCenarioCurrent;
        this.ocupacaoPatioCurrent = ocupacaoPatioCurrent;
        this.rehandleRatioCurrent = rehandleRatioCurrent;
        this.equipamentosDisponiveisCurrent = equipamentosDisponiveisCurrent;
        this.totalConteineresSimulado = totalConteineresSimulado;
        this.ocupacaoPatioSimulado = ocupacaoPatioSimulado;
        this.rehandleRatioSimulado = rehandleRatioSimulado;
        this.equipamentosDisponiveisSimulado = equipamentosDisponiveisSimulado;
        this.alertaPrincipal = alertaPrincipal;
        this.recomendacao = recomendacao;
        this.impactoProducao = impactoProducao;

        calcularDeltas();
    }

    private void calcularDeltas() {
        this.deltaConteineres = totalConteineresSimulado - totalConteineresCenarioCurrent;
        this.deltaOcupacao = ocupacaoPatioSimulado - ocupacaoPatioCurrent;
        this.deltaRehandleRatio = rehandleRatioSimulado - rehandleRatioCurrent;
        this.deltaEquipamentos = equipamentosDisponiveisSimulado - equipamentosDisponiveisCurrent;
    }

    // Getters
    public String getCenarioDescricao() { return cenarioDescricao; }
    public Integer getTotalConteineresCenarioCurrent() { return totalConteineresCenarioCurrent; }
    public Integer getOcupacaoPatioCurrent() { return ocupacaoPatioCurrent; }
    public Double getRehandleRatioCurrent() { return rehandleRatioCurrent; }
    public Integer getEquipamentosDisponiveisCurrent() { return equipamentosDisponiveisCurrent; }
    public Integer getTotalConteineresSimulado() { return totalConteineresSimulado; }
    public Integer getOcupacaoPatioSimulado() { return ocupacaoPatioSimulado; }
    public Double getRehandleRatioSimulado() { return rehandleRatioSimulado; }
    public Integer getEquipamentosDisponiveisSimulado() { return equipamentosDisponiveisSimulado; }
    public Integer getDeltaConteineres() { return deltaConteineres; }
    public Integer getDeltaOcupacao() { return deltaOcupacao; }
    public Double getDeltaRehandleRatio() { return deltaRehandleRatio; }
    public Integer getDeltaEquipamentos() { return deltaEquipamentos; }
    public String getAlertaPrincipal() { return alertaPrincipal; }
    public String getRecomendacao() { return recomendacao; }
    public String getImpactoProducao() { return impactoProducao; }

    // Setters
    public void setCenarioDescricao(String cenarioDescricao) { this.cenarioDescricao = cenarioDescricao; }
    public void setTotalConteineresCenarioCurrent(Integer totalConteineresCenarioCurrent) { this.totalConteineresCenarioCurrent = totalConteineresCenarioCurrent; }
    public void setOcupacaoPatioCurrent(Integer ocupacaoPatioCurrent) { this.ocupacaoPatioCurrent = ocupacaoPatioCurrent; }
    public void setRehandleRatioCurrent(Double rehandleRatioCurrent) { this.rehandleRatioCurrent = rehandleRatioCurrent; }
    public void setEquipamentosDisponiveisCurrent(Integer equipamentosDisponiveisCurrent) { this.equipamentosDisponiveisCurrent = equipamentosDisponiveisCurrent; }
    public void setTotalConteineresSimulado(Integer totalConteineresSimulado) { this.totalConteineresSimulado = totalConteineresSimulado; }
    public void setOcupacaoPatioSimulado(Integer ocupacaoPatioSimulado) { this.ocupacaoPatioSimulado = ocupacaoPatioSimulado; }
    public void setRehandleRatioSimulado(Double rehandleRatioSimulado) { this.rehandleRatioSimulado = rehandleRatioSimulado; }
    public void setEquipamentosDisponiveisSimulado(Integer equipamentosDisponiveisSimulado) { this.equipamentosDisponiveisSimulado = equipamentosDisponiveisSimulado; }
    public void setAlertaPrincipal(String alertaPrincipal) { this.alertaPrincipal = alertaPrincipal; }
    public void setRecomendacao(String recomendacao) { this.recomendacao = recomendacao; }
    public void setImpactoProducao(String impactoProducao) { this.impactoProducao = impactoProducao; }
}
