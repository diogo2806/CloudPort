package br.com.cloudport.serviconaviosiderurgico.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public record ComandoPrioridadeOrdemPatioDTO(
        @NotNull @Min(0) Integer prioridadeOperacional,
        Boolean prioridadeBusca,
        String usuario
) {
    public boolean prioridadeBuscaEfetiva() {
        return Boolean.TRUE.equals(prioridadeBusca);
    }

    public String usuarioEfetivo() {
        return usuario == null || usuario.isBlank() ? "sistema" : usuario.trim();
    }
}
