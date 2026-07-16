package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.List;
import java.util.Map;

public class WorkInstructionDrillDownDto {

    private OrdemTrabalhoPatioRespostaDto workInstruction;
    private WorkQueuePatioRespostaDto workQueue;
    private Long equipamentoPatioId;
    private String equipamentoIdentificador;
    private String equipamentoTipo;
    private String equipamentoStatus;
    private List<String> proximosEstadosPermitidos;
    private Map<String, List<String>> matrizOficialEstados;
    private List<HistoricoOperacaoPatioRespostaDto> auditoria;

    public OrdemTrabalhoPatioRespostaDto getWorkInstruction() { return workInstruction; }
    public void setWorkInstruction(OrdemTrabalhoPatioRespostaDto workInstruction) { this.workInstruction = workInstruction; }
    public WorkQueuePatioRespostaDto getWorkQueue() { return workQueue; }
    public void setWorkQueue(WorkQueuePatioRespostaDto workQueue) { this.workQueue = workQueue; }
    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
    public String getEquipamentoIdentificador() { return equipamentoIdentificador; }
    public void setEquipamentoIdentificador(String equipamentoIdentificador) { this.equipamentoIdentificador = equipamentoIdentificador; }
    public String getEquipamentoTipo() { return equipamentoTipo; }
    public void setEquipamentoTipo(String equipamentoTipo) { this.equipamentoTipo = equipamentoTipo; }
    public String getEquipamentoStatus() { return equipamentoStatus; }
    public void setEquipamentoStatus(String equipamentoStatus) { this.equipamentoStatus = equipamentoStatus; }
    public List<String> getProximosEstadosPermitidos() { return proximosEstadosPermitidos; }
    public void setProximosEstadosPermitidos(List<String> proximosEstadosPermitidos) { this.proximosEstadosPermitidos = proximosEstadosPermitidos; }
    public Map<String, List<String>> getMatrizOficialEstados() { return matrizOficialEstados; }
    public void setMatrizOficialEstados(Map<String, List<String>> matrizOficialEstados) { this.matrizOficialEstados = matrizOficialEstados; }
    public List<HistoricoOperacaoPatioRespostaDto> getAuditoria() { return auditoria; }
    public void setAuditoria(List<HistoricoOperacaoPatioRespostaDto> auditoria) { this.auditoria = auditoria; }
}
