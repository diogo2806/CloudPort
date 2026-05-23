package br.com.cloudport.servicoyard.dispatch.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DespachoInstrucaoDTO {

    @NotNull(message = "Informe o equipamento (CHE) para o dispatch.")
    private Long equipamentoId;

    private Integer sequencia;
}
