package br.com.cloudport.servicogate.dto;

public class GatePassQrCodeDTO {

    private String mimeType;
    private String base64;
    private String conteudo;

    public GatePassQrCodeDTO() {
    }

    public GatePassQrCodeDTO(String mimeType, String base64, String conteudo) {
        this.mimeType = mimeType;
        this.base64 = base64;
        this.conteudo = conteudo;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
}
