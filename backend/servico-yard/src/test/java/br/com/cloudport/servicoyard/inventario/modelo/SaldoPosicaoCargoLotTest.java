package br.com.cloudport.servicoyard.inventario.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SaldoPosicaoCargoLotTest {

    @Test
    void deveCreditarEDebitarSaldoParcialDaPosicao() {
        SaldoPosicaoCargoLot saldo = new SaldoPosicaoCargoLot();

        saldo.creditar(
                new BigDecimal("10.000"),
                new BigDecimal("20.000"),
                new BigDecimal("30.000"));
        saldo.debitar(
                new BigDecimal("4.000"),
                new BigDecimal("8.000"),
                new BigDecimal("12.000"));

        assertEquals(new BigDecimal("6.000"), saldo.getQuantidade());
        assertEquals(new BigDecimal("12.000"), saldo.getVolumeM3());
        assertEquals(new BigDecimal("18.000"), saldo.getPesoKg());
    }

    @Test
    void deveRejeitarDebitoSuperiorAoSaldoDaOrigem() {
        SaldoPosicaoCargoLot saldo = new SaldoPosicaoCargoLot();
        saldo.creditar(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

        assertThrows(
                IllegalStateException.class,
                () -> saldo.debitar(new BigDecimal("2.000"), BigDecimal.ONE, BigDecimal.ONE));
    }

    @Test
    void deveIdentificarSaldoZeradoAposTransferenciaIntegral() {
        SaldoPosicaoCargoLot saldo = new SaldoPosicaoCargoLot();
        saldo.creditar(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        saldo.debitar(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

        assertTrue(saldo.estaZerado());
    }
}
