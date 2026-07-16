package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.List;

public class JobListEquipamentoDto {

    private Long equipamentoPatioId;
    private String equipamentoIdentificador;
    private String equipamentoTipo;
    private String equipamentoStatus;
    private int totalFilas;
    private int totalInstrucoes;
    private List<WorkQueuePatioRespostaDto> workQueues;

    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
    public String getEquipamentoIdentificador() { return equipamentoIdentificador; }
    public void setEquipamentoIdentificador(String equipamentoIdentificador) { this.equipamentoIdentificador = equipamentoIdentificador; }
    public String getEquipamentoTipo() { return equipamentoTipo; }
    public void setEquipamentoTipo(String equipamentoTipo) { this.equipamentoTipo = equipamentoTipo; }
    public String getEquipamentoStatus() { return equipamentoStatus; }
    public void setEquipamentoStatus(String equipamentoStatus) { this.equipamentoStatus = equipamentoStatus; }
    public int getTotalFilas() { return totalFilas; }
    public void setTotalFilas(int totalFilas) { this.totalFilas = totalFilas; }
    public int getTotalInstrucoes() { return totalInstrucoes; }
    public void setTotalInstrucoes(int totalInstrucoes) { this.totalInstrucoes = totalInstrucoes; }
    public List<WorkQueuePatioRespostaDto> getWorkQueues() { return workQueues; }
    public void setWorkQueues(List<WorkQueuePatioRespostaDto> workQueues) { this.workQueues = workQueues; }
}
