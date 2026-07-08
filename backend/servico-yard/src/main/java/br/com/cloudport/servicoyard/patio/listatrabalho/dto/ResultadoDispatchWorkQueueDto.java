package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.List;

public class ResultadoDispatchWorkQueueDto {

    private int totalDespachadas;
    private int totalIgnoradas;
    private String mensagem;
    private List<OrdemTrabalhoPatioRespostaDto> jobList;

    public ResultadoDispatchWorkQueueDto() {
    }

    public ResultadoDispatchWorkQueueDto(int totalDespachadas,
                                         int totalIgnoradas,
                                         String mensagem,
                                         List<OrdemTrabalhoPatioRespostaDto> jobList) {
        this.totalDespachadas = totalDespachadas;
        this.totalIgnoradas = totalIgnoradas;
        this.mensagem = mensagem;
        this.jobList = jobList;
    }

    public int getTotalDespachadas() { return totalDespachadas; }
    public void setTotalDespachadas(int totalDespachadas) { this.totalDespachadas = totalDespachadas; }
    public int getTotalIgnoradas() { return totalIgnoradas; }
    public void setTotalIgnoradas(int totalIgnoradas) { this.totalIgnoradas = totalIgnoradas; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public List<OrdemTrabalhoPatioRespostaDto> getJobList() { return jobList; }
    public void setJobList(List<OrdemTrabalhoPatioRespostaDto> jobList) { this.jobList = jobList; }
}
