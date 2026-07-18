package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LoteCargaFerroviaTest {

    @ParameterizedTest
    @EnumSource(NaturezaCarga.class)
    void deveExecutarFluxoFerroviarioComRastreabilidadePorTipoDeCarga(NaturezaCarga natureza) {
        LoteCarga lote = novoLote(natureza, "12000");
        lote.adicionarSaldo(decimal("10"), decimal("20"), decimal("10000"));

        lote.planejarFerrovia(
                "TREM-2026-001",
                "VAGAO-15",
                "PATIO-A-LINHA-2",
                3,
                decimal("15000"),
                "INCOMPATIVEL_COM_CLASSE_3",
                "OPERADOR_FERROVIARIO",
                "planejador.teste");
        lote.atualizarStatusFerroviario(
                StatusOrdemFerroviariaCarga.EM_EXECUCAO,
                "TERMINAL",
                "Vagão recebido na linha operacional.",
                "operador.teste");
        lote.atualizarStatusFerroviario(
                StatusOrdemFerroviariaCarga.CONCLUIDA,
                "CLIENTE",
                "Carga ferroviária concluída.",
                "operador.teste");

        assertEquals(natureza, lote.getNatureza());
        assertEquals("TREM-2026-001", lote.getVisitaTremId());
        assertEquals("VAGAO-15", lote.getVagaoId());
        assertEquals("PATIO-A-LINHA-2", lote.getPosicaoFerroviaria());
        assertEquals(3, lote.getSequenciaFerroviaria());
        assertEquals(decimal("15000"), lote.getCapacidadeVagaoPesoKg());
        assertEquals("INCOMPATIVEL_COM_CLASSE_3", lote.getIncompatibilidadesFerroviarias());
        assertEquals("CLIENTE", lote.getCustodiaFerroviaria());
        assertEquals(StatusOrdemFerroviariaCarga.CONCLUIDA, lote.getStatusOrdemFerroviaria());
        assertEquals(3, lote.getHistoricoCustodiaFerroviaria().size());
        assertEquals("ATUALIZACAO_STATUS", lote.getHistoricoCustodiaFerroviaria().get(2).getEvento());
    }

    @Test
    void deveBloquearPesoAcimaDaCapacidadeDoVagao() {
        LoteCarga lote = novoLote(NaturezaCarga.CARGA_PROJETO, "16000");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> lote.planejarFerrovia(
                        "TREM-2",
                        "VAGAO-1",
                        "LINHA-1",
                        1,
                        decimal("15000"),
                        null,
                        "OPERADOR_FERROVIARIO",
                        "planejador.teste"));

        assertEquals("O peso do cargo lot excede a capacidade do vagão.", exception.getMessage());
    }

    @Test
    void deveBloquearTransicaoRegressivaDeStatus() {
        LoteCarga lote = novoLote(NaturezaCarga.BREAK_BULK, "1000");
        lote.planejarFerrovia(
                "TREM-3",
                "VAGAO-2",
                null,
                1,
                decimal("2000"),
                null,
                "TERMINAL",
                "planejador.teste");
        lote.atualizarStatusFerroviario(
                StatusOrdemFerroviariaCarga.CONCLUIDA,
                "CLIENTE",
                null,
                "operador.teste");

        assertThrows(
                IllegalStateException.class,
                () -> lote.atualizarStatusFerroviario(
                        StatusOrdemFerroviariaCarga.EM_EXECUCAO,
                        "TERMINAL",
                        "Tentativa de retorno.",
                        "operador.teste"));
    }

    private LoteCarga novoLote(NaturezaCarga natureza, String pesoPrevistoKg) {
        LoteCarga lote = new LoteCarga();
        lote.setNatureza(natureza);
        lote.setPesoPrevistoKg(decimal(pesoPrevistoKg));
        return lote;
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
