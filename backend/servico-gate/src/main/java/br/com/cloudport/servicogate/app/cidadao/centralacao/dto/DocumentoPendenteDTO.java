package br.com.cloudport.servicogate.app.cidadao.centralacao.dto;

public class DocumentoPendenteDTO {

    private Long id;
    private String nomeArquivo;
    private String tipoDocumento;
    private String mensagem;

    public DocumentoPendenteDTO() {
    }

    public DocumentoPendenteDTO(Long id, String nomeArquivo, String tipoDocumento, String mensagem) {
        this.id = id;
        this.nomeArquivo = nomeArquivo;
        this.tipoDocumento = tipoDocumento;
        this.mensagem = mensagem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
