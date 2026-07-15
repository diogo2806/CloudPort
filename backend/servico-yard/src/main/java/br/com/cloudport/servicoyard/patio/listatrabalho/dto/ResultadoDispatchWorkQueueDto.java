package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public class ResultadoDispatchWorkQueueDto {

    private Long workQueueId;
    private int totalOrdensDespachadas;
    private int totalOrdensIgnoradas;
    private String mensagem;
    private List<OrdemTrabalhoPatioRespostaDto> ordens;

    public ResultadoDispatchWorkQueueDto() {
    }

    public ResultadoDispatchWorkQueueDto(Long workQueueId,
                                          int totalOrdensDespachadas,
                                          int totalOrdensIgnoradas,
                                          String mensagem,
                                          List<OrdemTrabalhoPatioRespostaDto> ordens) {
        this.workQueueId = workQueueId;
        this.totalOrdensDespachadas = totalOrdensDespachadas;
        this.totalOrdensIgnoradas = totalOrdensIgnoradas;
        this.mensagem = mensagem;
        this.ordens = ordens;
    }

    public Long getWorkQueueId() { return workQueueId; }
    public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    public int getTotalOrdensDespachadas() { return totalOrdensDespachadas; }
    public void setTotalOrdensDespachadas(int totalOrdensDespachadas) { this.totalOrdensDespachadas = totalOrdensDespachadas; }
    public int getTotalOrdensIgnoradas() { return totalOrdensIgnoradas; }
    public void setTotalOrdensIgnoradas(int totalOrdensIgnoradas) { this.totalOrdensIgnoradas = totalOrdensIgnoradas; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public List<OrdemTrabalhoPatioRespostaDto> getOrdens() { return ordens; }
    public void setOrdens(List<OrdemTrabalhoPatioRespostaDto> ordens) { this.ordens = ordens; }

    @Deprecated
    @JsonIgnore
    public int getTotalDespachadas() { return totalOrdensDespachadas; }

    @Deprecated
    @JsonIgnore
    public int getTotalIgnoradas() { return totalOrdensIgnoradas; }

    @Deprecated
    @JsonIgnore
    public List<OrdemTrabalhoPatioRespostaDto> getJobList() { return ordens; }
}
