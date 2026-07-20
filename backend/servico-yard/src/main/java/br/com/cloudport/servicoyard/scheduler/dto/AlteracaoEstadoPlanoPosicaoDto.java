package br.com.cloudport.servicoyard.scheduler.dto;

import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AlteracaoEstadoPlanoPosicaoDto {

    @NotNull
    private EstadoPlanoPosicaoOperacional estadoDestino;

    @NotBlank
    @Size(max = 1000)
    private String motivo;

    @NotBlank
    @Size(max = 120)
    private String operador;

    public EstadoPlanoPosicaoOperacional getEstadoDestino() { return estadoDestino; }
    public void setEstadoDestino(EstadoPlanoPosicaoOperacional estadoDestino) { this.estadoDestino = estadoDestino; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
}
