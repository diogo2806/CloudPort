package br.com.cloudport.servicogate.app.cidadao.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class DocumentoUploadRequest {

    @NotBlank
    @Size(max = 80)
    private String tipoDocumento;

    @Size(max = 80)
    private String numero;

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
}
