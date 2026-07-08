package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import javax.validation.constraints.NotNull;

public record AtualizarStatusItemNavioDTO(
        @NotNull(message = "Status e obrigatorio.") StatusItemCarga status,
        String usuario,
        String observacao
) {}
