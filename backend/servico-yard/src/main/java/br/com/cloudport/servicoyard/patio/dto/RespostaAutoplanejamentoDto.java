package br.com.cloudport.servicoyard.patio.dto;

import java.io.Serializable;
import java.util.List;

public class RespostaAutoplanejamentoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer totalConteineresPlanificados;
    private Integer totalConteineresSucesso;
    private Integer totalConteineresFalha;
    private Double percentualSucesso;
    private List<String> containersPlanificados;
    private List<String> containersException;
    private String mensagemResumo;
    private Boolean temExcecoes;

    public RespostaAutoplanejamentoDto() {
    }

    public RespostaAutoplanejamentoDto(Integer totalConteineresPlanificados,
                                       Integer totalConteineresSucesso,
                                       Integer totalConteineresFalha,
                                       List<String> containersPlanificados,
                                       List<String> containersException) {
        this.totalConteineresPlanificados = totalConteineresPlanificados;
        this.totalConteineresSucesso = totalConteineresSucesso;
        this.totalConteineresFalha = totalConteineresFalha;
        this.containersPlanificados = containersPlanificados;
        this.containersException = containersException;
        this.temExcecoes = !containersException.isEmpty();
        calcularMetricas();
    }

    private void calcularMetricas() {
        if (totalConteineresPlanificados > 0) {
            this.percentualSucesso = (totalConteineresSucesso * 100.0) / totalConteineresPlanificados;
        } else {
            this.percentualSucesso = 0.0;
        }

        if (temExcecoes) {
            this.mensagemResumo = String.format(
                "Auto-planejamento: %d de %d containers planificados com sucesso. %d exceptions requerem decisão manual.",
                totalConteineresSucesso, totalConteineresPlanificados, totalConteineresFalha);
        } else {
            this.mensagemResumo = String.format(
                "Auto-planejamento concluído com 100%% de sucesso! %d containers foram planificados automaticamente.",
                totalConteineresSucesso);
        }
    }

    // Getters e Setters
    public Integer getTotalConteineresPlanificados() { return totalConteineresPlanificados; }
    public void setTotalConteineresPlanificados(Integer totalConteineresPlanificados) { this.totalConteineresPlanificados = totalConteineresPlanificados; }

    public Integer getTotalConteineresSucesso() { return totalConteineresSucesso; }
    public void setTotalConteineresSucesso(Integer totalConteineresSucesso) { this.totalConteineresSucesso = totalConteineresSucesso; }

    public Integer getTotalConteineresFalha() { return totalConteineresFalha; }
    public void setTotalConteineresFalha(Integer totalConteineresFalha) { this.totalConteineresFalha = totalConteineresFalha; }

    public Double getPercentualSucesso() { return percentualSucesso; }
    public void setPercentualSucesso(Double percentualSucesso) { this.percentualSucesso = percentualSucesso; }

    public List<String> getContainersPlanificados() { return containersPlanificados; }
    public void setContainersPlanificados(List<String> containersPlanificados) { this.containersPlanificados = containersPlanificados; }

    public List<String> getContainersException() { return containersException; }
    public void setContainersException(List<String> containersException) { this.containersException = containersException; }

    public String getMensagemResumo() { return mensagemResumo; }
    public void setMensagemResumo(String mensagemResumo) { this.mensagemResumo = mensagemResumo; }

    public Boolean getTemExcecoes() { return temExcecoes; }
    public void setTemExcecoes(Boolean temExcecoes) { this.temExcecoes = temExcecoes; }
}
