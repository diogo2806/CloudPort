package br.com.cloudport.servicoyard.container.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RegistroLiberacaoDTO {
    @NotBlank(message = "Destino final obrigatório")
    @Size(max = 120, message = "Destino final deve ter até 120 caracteres")
    private String destinoFinal;

    @Size(max = 255, message = "Observações devem ter até 255 caracteres")
    private String observacoes;

    @NotBlank(message = "Responsável obrigatório")
    @Size(max = 80, message = "Responsável deve ter até 80 caracteres")
    private String responsavel;

    public RegistroLiberacaoDTO() {
    }

    public String getDestinoFinal() {
        return destinoFinal;
    }

    public void setDestinoFinal(String destinoFinal) {
        this.destinoFinal = destinoFinal;
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
