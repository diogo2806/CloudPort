package br.com.cloudport.servicoyard.scheduler.dto;

import java.math.BigDecimal;

public class SchedulerEquipmentDto {

    private String equipamentoId;
    private String statusOperacional;
    private boolean disponivel = true;
    private boolean conflitoRecurso;
    private BigDecimal produtividadeMovimentosHora;
    private Integer linhaAtual;
    private Integer colunaAtual;
    private Integer prioridadeWorkQueue;
    private Integer totalOrdens;

    public String getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(String equipamentoId) { this.equipamentoId = equipamentoId; }
    public String getStatusOperacional() { return statusOperacional; }
    public void setStatusOperacional(String statusOperacional) { this.statusOperacional = statusOperacional; }
    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
    public boolean isConflitoRecurso() { return conflitoRecurso; }
    public void setConflitoRecurso(boolean conflitoRecurso) { this.conflitoRecurso = conflitoRecurso; }
    public BigDecimal getProdutividadeMovimentosHora() { return produtividadeMovimentosHora; }
    public void setProdutividadeMovimentosHora(BigDecimal produtividadeMovimentosHora) { this.produtividadeMovimentosHora = produtividadeMovimentosHora; }
    public Integer getLinhaAtual() { return linhaAtual; }
    public void setLinhaAtual(Integer linhaAtual) { this.linhaAtual = linhaAtual; }
    public Integer getColunaAtual() { return colunaAtual; }
    public void setColunaAtual(Integer colunaAtual) { this.colunaAtual = colunaAtual; }
    public Integer getPrioridadeWorkQueue() { return prioridadeWorkQueue; }
    public void setPrioridadeWorkQueue(Integer prioridadeWorkQueue) { this.prioridadeWorkQueue = prioridadeWorkQueue; }
    public Integer getTotalOrdens() { return totalOrdens; }
    public void setTotalOrdens(Integer totalOrdens) { this.totalOrdens = totalOrdens; }
}
