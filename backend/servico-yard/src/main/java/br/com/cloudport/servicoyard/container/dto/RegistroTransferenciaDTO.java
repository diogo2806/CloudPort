package br.com.cloudport.servicoyard.container.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RegistroTransferenciaDTO {
    @NotBlank(message = "Posição de destino obrigatória")
    @Size(max = 60, message = "Posição de destino deve ter até 60 caracteres")
    private String posicaoDestino;

    @NotBlank(message = "Motivo obrigatório")
    @Size(max = 120, message = "Motivo deve ter até 120 caracteres")
    private String motivo;

    @NotBlank(message = "Responsável obrigatório")
    @Size(max = 80, message = "Responsável deve ter até 80 caracteres")
    private String responsavel;

    public RegistroTransferenciaDTO() {
    }

    public String getPosicaoDestino() {
        return posicaoDestino;
    }

    public void setPosicaoDestino(String posicaoDestino) {
        this.posicaoDestino = posicaoDestino;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }
}
