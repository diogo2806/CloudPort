package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import javax.validation.constraints.NotNull;

public class WorkQueuePatioRequisicaoDto {

    @NotNull
    private Long visitaNavioId;
    private String identificador;
    private String berco;
    private Integer porao;
    private String blocoZona;
    private Integer sequenciaInicial;
    private String pow;
    private String poolOperacional;
    private String equipamento;
    private Integer prioridadeOperacional;

    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getBerco() { return berco; }
    public void setBerco(String berco) { this.berco = berco; }
    public Integer getPorao() { return porao; }
    public void setPorao(Integer porao) { this.porao = porao; }
    public String getBlocoZona() { return blocoZona; }
    public void setBlocoZona(String blocoZona) { this.blocoZona = blocoZona; }
    public Integer getSequenciaInicial() { return sequenciaInicial; }
    public void setSequenciaInicial(Integer sequenciaInicial) { this.sequenciaInicial = sequenciaInicial; }
    public String getPow() { return pow; }
    public void setPow(String pow) { this.pow = pow; }
    public String getPoolOperacional() { return poolOperacional; }
    public void setPoolOperacional(String poolOperacional) { this.poolOperacional = poolOperacional; }
    public String getEquipamento() { return equipamento; }
    public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
    public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
    public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
}
