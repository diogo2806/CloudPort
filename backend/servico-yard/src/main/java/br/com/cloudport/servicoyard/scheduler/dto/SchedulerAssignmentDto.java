package br.com.cloudport.servicoyard.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

public class SchedulerAssignmentDto {

    private String codigoContainer;
    private String movimento;
    private Integer linhaOriginal;
    private Integer colunaOriginal;
    private String camadaOriginal;
    private Integer linhaProposta;
    private Integer colunaProposta;
    private String camadaProposta;
    private String blocoProposto;
    private String equipamentoId;
    private Integer sequenciaPlano;
    private Double scoreTotal;
    private Integer distancia;
    private Integer rehandlesEstimados;
    private Double penalidadeOcupacao;
    private Double penalidadeDestino;
    private Double penalidadeEquipamento;
    private List<String> justificativas = new ArrayList<>();

    public String getCodigoContainer() { return codigoContainer; }
    public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
    public String getMovimento() { return movimento; }
    public void setMovimento(String movimento) { this.movimento = movimento; }
    public Integer getLinhaOriginal() { return linhaOriginal; }
    public void setLinhaOriginal(Integer linhaOriginal) { this.linhaOriginal = linhaOriginal; }
    public Integer getColunaOriginal() { return colunaOriginal; }
    public void setColunaOriginal(Integer colunaOriginal) { this.colunaOriginal = colunaOriginal; }
    public String getCamadaOriginal() { return camadaOriginal; }
    public void setCamadaOriginal(String camadaOriginal) { this.camadaOriginal = camadaOriginal; }
    public Integer getLinhaProposta() { return linhaProposta; }
    public void setLinhaProposta(Integer linhaProposta) { this.linhaProposta = linhaProposta; }
    public Integer getColunaProposta() { return colunaProposta; }
    public void setColunaProposta(Integer colunaProposta) { this.colunaProposta = colunaProposta; }
    public String getCamadaProposta() { return camadaProposta; }
    public void setCamadaProposta(String camadaProposta) { this.camadaProposta = camadaProposta; }
    public String getBlocoProposto() { return blocoProposto; }
    public void setBlocoProposto(String blocoProposto) { this.blocoProposto = blocoProposto; }
    public String getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(String equipamentoId) { this.equipamentoId = equipamentoId; }
    public Integer getSequenciaPlano() { return sequenciaPlano; }
    public void setSequenciaPlano(Integer sequenciaPlano) { this.sequenciaPlano = sequenciaPlano; }
    public Double getScoreTotal() { return scoreTotal; }
    public void setScoreTotal(Double scoreTotal) { this.scoreTotal = scoreTotal; }
    public Integer getDistancia() { return distancia; }
    public void setDistancia(Integer distancia) { this.distancia = distancia; }
    public Integer getRehandlesEstimados() { return rehandlesEstimados; }
    public void setRehandlesEstimados(Integer rehandlesEstimados) { this.rehandlesEstimados = rehandlesEstimados; }
    public Double getPenalidadeOcupacao() { return penalidadeOcupacao; }
    public void setPenalidadeOcupacao(Double penalidadeOcupacao) { this.penalidadeOcupacao = penalidadeOcupacao; }
    public Double getPenalidadeDestino() { return penalidadeDestino; }
    public void setPenalidadeDestino(Double penalidadeDestino) { this.penalidadeDestino = penalidadeDestino; }
    public Double getPenalidadeEquipamento() { return penalidadeEquipamento; }
    public void setPenalidadeEquipamento(Double penalidadeEquipamento) { this.penalidadeEquipamento = penalidadeEquipamento; }
    public List<String> getJustificativas() { return justificativas; }
    public void setJustificativas(List<String> justificativas) { this.justificativas = justificativas == null ? new ArrayList<>() : new ArrayList<>(justificativas); }
}
