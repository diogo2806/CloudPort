package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import javax.validation.constraints.NotNull;

public record AtualizarFaseVisitaNavioDTO(
        @NotNull(message = "Fase e obrigatoria.") FaseVisitaNavio fase,
        String usuario,
        String observacao
) {}
