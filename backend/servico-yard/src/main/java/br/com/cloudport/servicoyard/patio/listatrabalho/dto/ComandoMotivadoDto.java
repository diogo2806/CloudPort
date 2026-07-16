package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ComandoMotivadoDto {

    @NotBlank(message = "O motivo e obrigatorio.")
    @Size(max = 500, message = "O motivo deve ter no maximo 500 caracteres.")
    private String motivo;

    @Size(max = 150)
    private String usuario;

    @Size(max = 100)
    private String origemAcao;

    @Size(max = 100)
    private String correlationId;

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

    public String getOrigemAcao() {
        return origemAcao;
    }

    public void setOrigemAcao(String origemAcao) {
        this.origemAcao = origemAcao;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
