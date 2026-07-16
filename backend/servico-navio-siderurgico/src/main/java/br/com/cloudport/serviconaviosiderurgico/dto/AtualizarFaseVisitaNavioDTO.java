package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record AtualizarFaseVisitaNavioDTO(
        @NotNull(message = "Fase e obrigatoria.") FaseVisitaNavio fase,
        @Size(max = 150) String usuario,
        @NotBlank(message = "O motivo da alteracao de fase e obrigatorio.")
        @Size(max = 500) String observacao
) {
}
