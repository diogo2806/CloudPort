package br.com.cloudport.serviconaviosiderurgico.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record ComandoPrioridadeOrdemPatioDTO(
        @NotNull @Min(0) Integer prioridadeOperacional,
        Boolean prioridadeBusca,
        @NotBlank @Size(max = 500) String motivo,
        String usuario,
        String origemAcao,
        String correlationId
) {
    public boolean prioridadeBuscaEfetiva() {
        return Boolean.TRUE.equals(prioridadeBusca);
    }

    public String usuarioEfetivo() {
        return usuario == null || usuario.isBlank() ? "sistema" : usuario.trim();
    }
}
