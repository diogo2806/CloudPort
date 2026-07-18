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

    @Test
    void deveSegregarSomenteSaldoAfetadoPelaAvaria() {
        LoteCarga lote = new LoteCarga();
        lote.adicionarSaldo(decimal("10"), decimal("20"), decimal("1000"));

        lote.bloquearSaldo(decimal("3"), decimal("5"), decimal("200"));

        assertEquals(decimal("10"), lote.getQuantidadeSaldo());
        assertEquals(decimal("3"), lote.getQuantidadeBloqueada());
        assertEquals(decimal("7"), lote.getQuantidadeDisponivel());
        assertEquals(decimal("15"), lote.getVolumeDisponivelM3());
        assertEquals(decimal("800"), lote.getPesoDisponivelKg());
        assertEquals(StatusLoteCarga.AVARIADO, lote.getStatus());
    }

    @Test
    void deveReintegrarSaldoDepoisDoReparo() {
        LoteCarga lote = new LoteCarga();
        lote.adicionarSaldo(decimal("10"), decimal("20"), decimal("1000"));
        lote.bloquearSaldo(decimal("3"), decimal("5"), decimal("200"));

        lote.liberarSaldoBloqueado(decimal("3"), decimal("5"), decimal("200"));

        assertEquals(BigDecimal.ZERO, lote.getQuantidadeBloqueada());
        assertEquals(decimal("10"), lote.getQuantidadeDisponivel());
        assertEquals(StatusLoteCarga.NO_TERMINAL, lote.getStatus());
    }

    @Test
    void deveBaixarSaldoAvariadoDoEstoque() {
        LoteCarga lote = new LoteCarga();
        lote.adicionarSaldo(decimal("10"), decimal("20"), decimal("1000"));
        lote.bloquearSaldo(decimal("3"), decimal("5"), decimal("200"));

        lote.baixarSaldoBloqueado(decimal("3"), decimal("5"), decimal("200"));

        assertEquals(decimal("7"), lote.getQuantidadeSaldo());
        assertEquals(decimal("15"), lote.getVolumeSaldoM3());
        assertEquals(decimal("800"), lote.getPesoSaldoKg());
        assertEquals(BigDecimal.ZERO, lote.getQuantidadeBloqueada());
        assertEquals(StatusLoteCarga.NO_TERMINAL, lote.getStatus());
    }

    @Test
    void deveRejeitarAvariaAcimaDoSaldoDisponivel() {
        LoteCarga lote = new LoteCarga();
        lote.adicionarSaldo(decimal("5"), decimal("10"), decimal("500"));
        lote.bloquearSaldo(decimal("3"), decimal("5"), decimal("200"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> lote.bloquearSaldo(decimal("3"), decimal("5"), decimal("200")));

        assertEquals("Avaria excede o saldo disponível do lote.", exception.getMessage());
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
