package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.TipoReservaPatioNavio;

public record ComandoGeracaoReservasPatioDTO(
        TipoReservaPatioNavio tipoReserva,
        Boolean somentePendentes,
        String usuario
) {
    public TipoReservaPatioNavio tipoReservaEfetiva() {
        return tipoReserva == null ? TipoReservaPatioNavio.TENTATIVA : tipoReserva;
    }

    public boolean somentePendentesEfetivo() {
        return somentePendentes == null || somentePendentes;
    }
}
