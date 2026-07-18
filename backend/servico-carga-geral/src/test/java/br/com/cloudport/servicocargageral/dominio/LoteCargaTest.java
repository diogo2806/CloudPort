package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLoteCarga;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LoteCargaTest {

    @Test
    void deveControlarRecebimentoECargaParcial() {
        LoteCarga lote = new LoteCarga();

        lote.adicionarSaldo(decimal("10"), decimal("25.5"), decimal("12000"));
        lote.retirarSaldo(decimal("4"), decimal("10.2"), decimal("4800"));

        assertEquals(decimal("6"), lote.getQuantidadeSaldo());
        assertEquals(decimal("15.3"), lote.getVolumeSaldoM3());
        assertEquals(decimal("7200"), lote.getPesoSaldoKg());
        assertEquals(StatusLoteCarga.PARCIALMENTE_CARREGADO, lote.getStatus());
    }

    @Test
    void deveConcluirLoteQuandoTodoSaldoForRetirado() {
        LoteCarga lote = new LoteCarga();
        lote.adicionarSaldo(decimal("2"), decimal("3"), decimal("100"));

        lote.retirarSaldo(decimal("2"), decimal("3"), decimal("100"));

        assertEquals(BigDecimal.ZERO, lote.getQuantidadeSaldo());
        assertEquals(StatusLoteCarga.CONCLUIDO, lote.getStatus());
    }

    @Test
    void deveRejeitarMovimentacaoAcimaDoSaldo() {
        LoteCarga lote = new LoteCarga();
        lote.adicionarSaldo(decimal("1"), decimal("2"), decimal("10"));

        assertThrows(IllegalStateException.class,
                () -> lote.retirarSaldo(decimal("2"), decimal("2"), decimal("10")));
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
