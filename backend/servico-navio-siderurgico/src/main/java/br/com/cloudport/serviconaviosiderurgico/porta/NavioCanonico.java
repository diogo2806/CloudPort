package br.com.cloudport.serviconaviosiderurgico.porta;

import java.math.BigDecimal;

public record NavioCanonico(
        Long identificador,
        String nome,
        String codigoImo,
        String paisBandeira,
        String empresaArmadora,
        Integer capacidadeTeu,
        BigDecimal loaMetros,
        BigDecimal caladoMaximoMetros,
        String callSign) {
}
