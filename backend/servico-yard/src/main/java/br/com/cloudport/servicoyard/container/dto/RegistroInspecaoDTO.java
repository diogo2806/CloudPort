package br.com.cloudport.servicoyard.container.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RegistroInspecaoDTO {
    @NotBlank(message = "Resultado da inspeção obrigatório")
    @Size(max = 120, message = "Resultado deve ter até 120 caracteres")
    private String resultado;

    @Size(max = 255, message = "Observações devem ter até 255 caracteres")
    private String observacoes;

    @NotBlank(message = "Responsável obrigatório")
    @Size(max = 80, message = "Responsável deve ter até 80 caracteres")
    private String responsavel;

    public RegistroInspecaoDTO() {
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }
}
