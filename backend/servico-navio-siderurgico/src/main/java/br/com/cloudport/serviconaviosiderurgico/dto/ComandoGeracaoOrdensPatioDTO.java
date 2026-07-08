package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.ModoGeracaoOrdensPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;

public record ComandoGeracaoOrdensPatioDTO(
        TipoMovimentoNavio tipoMovimento,
        ModoGeracaoOrdensPatio modo,
        String usuario,
        Boolean gerarReservasAutomaticas
) {
    public ModoGeracaoOrdensPatio modoEfetivo() {
        return modo == null ? ModoGeracaoOrdensPatio.SOMENTE_PENDENTES : modo;
    }

    public boolean gerarReservasAutomaticasEfetivo() {
        return Boolean.TRUE.equals(gerarReservasAutomaticas);
    }
}
