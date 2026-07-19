package br.com.cloudport.servicoyard.patio.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ExcluirGeometriaPatioDto {

    @NotBlank
    @Size(max = 500)
    private String motivo;

    @Size(max = 120)
    private String usuario;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
