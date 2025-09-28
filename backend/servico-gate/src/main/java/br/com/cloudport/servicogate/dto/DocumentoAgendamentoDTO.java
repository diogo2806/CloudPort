package br.com.cloudport.servicogate.dto;

public class DocumentoAgendamentoDTO {

    private Long id;
    private String tipoDocumento;
    private String numero;
    private String urlDocumento;

    public DocumentoAgendamentoDTO() {
    }

    public DocumentoAgendamentoDTO(Long id, String tipoDocumento, String numero, String urlDocumento) {
        this.id = id;
        this.tipoDocumento = tipoDocumento;
        this.numero = numero;
        this.urlDocumento = urlDocumento;
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
}
