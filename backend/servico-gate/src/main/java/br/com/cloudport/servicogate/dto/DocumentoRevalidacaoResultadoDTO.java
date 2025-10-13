package br.com.cloudport.servicogate.dto;

import java.time.LocalDateTime;

public class DocumentoRevalidacaoResultadoDTO {

    private Long documentoId;
    private String nomeArquivo;
    private boolean valido;
    private LocalDateTime verificadoEm;
    private String mensagem;

    public DocumentoRevalidacaoResultadoDTO() {
    }

    public DocumentoRevalidacaoResultadoDTO(Long documentoId, String nomeArquivo, boolean valido,
                                            LocalDateTime verificadoEm, String mensagem) {
        this.documentoId = documentoId;
        this.nomeArquivo = nomeArquivo;
        this.valido = valido;
        this.verificadoEm = verificadoEm;
        this.mensagem = mensagem;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public boolean isValido() {
        return valido;
    }

    public void setValido(boolean valido) {
        this.valido = valido;
    }

    public LocalDateTime getVerificadoEm() {
        return verificadoEm;
    }

    public void setVerificadoEm(LocalDateTime verificadoEm) {
        this.verificadoEm = verificadoEm;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
