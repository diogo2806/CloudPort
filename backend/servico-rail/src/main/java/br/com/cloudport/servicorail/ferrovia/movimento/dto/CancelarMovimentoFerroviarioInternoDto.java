package br.com.cloudport.servicorail.ferrovia.movimento.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CancelarMovimentoFerroviarioInternoDto {

    @NotBlank(message = "O motivo do cancelamento deve ser informado.")
    @Size(max = 500, message = "O motivo do cancelamento deve ter no máximo 500 caracteres.")
    private String motivo;

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
