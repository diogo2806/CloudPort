package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import java.time.LocalDateTime;

public class ProcessamentoOcrMensagem {

    private Long documentoId;
    private Long agendamentoId;
    private String chaveArmazenamento;
    private LocalDateTime reivindicadoEm;
    private Integer tentativa;

    public ProcessamentoOcrMensagem() {
    }

    public ProcessamentoOcrMensagem(Long documentoId, Long agendamentoId, String chaveArmazenamento) {
        this(documentoId, agendamentoId, chaveArmazenamento, null, null);
    }

    public ProcessamentoOcrMensagem(Long documentoId,
                                    Long agendamentoId,
                                    String chaveArmazenamento,
                                    LocalDateTime reivindicadoEm,
                                    Integer tentativa) {
        this.documentoId = documentoId;
        this.agendamentoId = agendamentoId;
        this.chaveArmazenamento = chaveArmazenamento;
        this.reivindicadoEm = reivindicadoEm;
        this.tentativa = tentativa;
    }

    public static ProcessamentoOcrMensagem from(DocumentoAgendamento documento) {
        Long agendamentoId = documento.getAgendamento() != null ? documento.getAgendamento().getId() : null;
        return new ProcessamentoOcrMensagem(
                documento.getId(),
                agendamentoId,
                documento.getUrlDocumento(),
                documento.getProcessamentoOcrIniciadoEm(),
                documento.getTentativasOcr());
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

    public LocalDateTime getReivindicadoEm() {
        return reivindicadoEm;
    }

    public void setReivindicadoEm(LocalDateTime reivindicadoEm) {
        this.reivindicadoEm = reivindicadoEm;
    }

    public Integer getTentativa() {
        return tentativa;
    }

    public void setTentativa(Integer tentativa) {
        this.tentativa = tentativa;
    }
}
