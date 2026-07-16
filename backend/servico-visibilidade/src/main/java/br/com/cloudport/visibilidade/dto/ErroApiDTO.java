package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;

public class ErroApiDTO {

    private String codigo;
    private String mensagem;
    private String detalhes;
    private String correlationId;
    private LocalDateTime timestamp;

    public ErroApiDTO(String codigo,
                      String mensagem,
                      String detalhes,
                      String correlationId,
                      LocalDateTime timestamp) {
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.detalhes = detalhes;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
