package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;

import javax.validation.constraints.NotNull;

public class AvancarFaseEscalaDTO {

    @NotNull(message = "Informe a fase de destino da escala.")
    private FaseEscala fase;

    public AvancarFaseEscalaDTO() {
    }

    public FaseEscala getFase() {
        return fase;
    }

    public void setFase(FaseEscala fase) {
        this.fase = fase;
    }
}
