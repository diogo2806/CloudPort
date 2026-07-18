package br.com.cloudport.visibilidade.dto;

import javax.validation.constraints.Size;

public class AcaoAlertaRequest {

    @Size(max = 120)
    private String usuario;

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
