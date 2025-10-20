package br.com.cloudport.servicogate.integration.ocr;

public class ProcessamentoOcrMensagem {

    private Long documentoId;
    private Long agendamentoId;
    private String chaveArmazenamento;

    public ProcessamentoOcrMensagem() {
    }

    public ProcessamentoOcrMensagem(Long documentoId, Long agendamentoId, String chaveArmazenamento) {
        this.documentoId = documentoId;
        this.agendamentoId = agendamentoId;
        this.chaveArmazenamento = chaveArmazenamento;
    }

    public static ProcessamentoOcrMensagem from(br.com.cloudport.servicogate.model.DocumentoAgendamento documento) {
        Long agendamentoId = documento.getAgendamento() != null ? documento.getAgendamento().getId() : null;
        return new ProcessamentoOcrMensagem(documento.getId(), agendamentoId, documento.getUrlDocumento());
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }

    public Long getAgendamentoId() {
        return agendamentoId;
    }

    public void setAgendamentoId(Long agendamentoId) {
        this.agendamentoId = agendamentoId;
    }

    public String getChaveArmazenamento() {
        return chaveArmazenamento;
    }

    public void setChaveArmazenamento(String chaveArmazenamento) {
        this.chaveArmazenamento = chaveArmazenamento;
    }
}
