package br.com.cloudport.servicogate.dto;

import java.util.List;

public class DocumentoRevalidacaoResponse {

    private AgendamentoDTO agendamento;
    private List<DocumentoRevalidacaoResultadoDTO> resultados;

    public DocumentoRevalidacaoResponse() {
    }

    public DocumentoRevalidacaoResponse(AgendamentoDTO agendamento,
                                        List<DocumentoRevalidacaoResultadoDTO> resultados) {
        this.agendamento = agendamento;
        this.resultados = resultados;
    }

    public AgendamentoDTO getAgendamento() {
        return agendamento;
    }

    public void setAgendamento(AgendamentoDTO agendamento) {
        this.agendamento = agendamento;
    }

    public List<DocumentoRevalidacaoResultadoDTO> getResultados() {
        return resultados;
    }

    public void setResultados(List<DocumentoRevalidacaoResultadoDTO> resultados) {
        this.resultados = resultados;
    }
}
