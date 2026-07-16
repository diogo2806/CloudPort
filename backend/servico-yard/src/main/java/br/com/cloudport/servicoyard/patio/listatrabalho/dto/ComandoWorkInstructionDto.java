package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import javax.validation.constraints.NotBlank;

public class ComandoWorkInstructionDto {

    @NotBlank
    private String motivo;
    private String usuario;
    private String origemAcao;
    private String correlationId;

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getOrigemAcao() { return origemAcao; }
    public void setOrigemAcao(String origemAcao) { this.origemAcao = origemAcao; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
