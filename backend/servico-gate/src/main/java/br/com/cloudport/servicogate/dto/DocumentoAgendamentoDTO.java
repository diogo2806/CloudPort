package br.com.cloudport.servicogate.dto;

import java.time.LocalDateTime;

public class DocumentoAgendamentoDTO {

    private Long id;
    private String tipoDocumento;
    private String numero;
    private String urlDocumento;
    private String nomeArquivo;
    private String contentType;
    private Long tamanhoBytes;
    private LocalDateTime ultimaRevalidacao;

    public DocumentoAgendamentoDTO() {
    }

    public DocumentoAgendamentoDTO(Long id, String tipoDocumento, String numero, String urlDocumento,
                                   String nomeArquivo, String contentType, Long tamanhoBytes,
                                   LocalDateTime ultimaRevalidacao) {
        this.id = id;
        this.tipoDocumento = tipoDocumento;
        this.numero = numero;
        this.urlDocumento = urlDocumento;
        this.nomeArquivo = nomeArquivo;
        this.contentType = contentType;
        this.tamanhoBytes = tamanhoBytes;
        this.ultimaRevalidacao = ultimaRevalidacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getUrlDocumento() {
        return urlDocumento;
    }

    public void setUrlDocumento(String urlDocumento) {
        this.urlDocumento = urlDocumento;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public LocalDateTime getUltimaRevalidacao() {
        return ultimaRevalidacao;
    }

    public void setUltimaRevalidacao(LocalDateTime ultimaRevalidacao) {
        this.ultimaRevalidacao = ultimaRevalidacao;
    }
}
