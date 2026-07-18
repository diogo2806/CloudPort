package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StuffUnstuffDomainTest {

    @Test
    void deveExecutarParcialmenteEConcluirOperacao() {
        OperacaoStuffUnstuff operacao = novaOperacao();
        ItemOperacaoStuffUnstuff item = novoItem("10", "20", "1000");
        operacao.adicionarItem(item);

        operacao.iniciar();
        item.registrarExecucao(decimal("4"), decimal("8"), decimal("400"), null, null, "Diferença conferida");
        operacao.atualizarStatusExecucao();

        assertEquals(StatusOperacaoStuffUnstuff.PARCIAL, operacao.getStatus());
        assertFalse(item.estaCompleto());

        item.registrarExecucao(decimal("6"), decimal("12"), decimal("600"), "AMASSADO", "Embalagem avariada", null);
        operacao.atualizarStatusExecucao();

        assertEquals(StatusOperacaoStuffUnstuff.EM_EXECUCAO, operacao.getStatus());
        assertTrue(item.estaCompleto());

        operacao.concluir("LACRE-002", "Conferência encerrada", "operador", "corr-1");

        assertEquals(StatusOperacaoStuffUnstuff.CONCLUIDA, operacao.getStatus());
        assertEquals("AMASSADO", item.getCodigoAvaria());
        assertEquals("LACRE-002", operacao.getLacreFinal());
        assertEquals(TipoEventoStuffUnstuff.CONCLUIDA,
                operacao.getHistorico().get(operacao.getHistorico().size() - 1).getTipo());
    }

    @Test
    void deveRejeitarExecucaoAcimaDoPlanejado() {
        ItemOperacaoStuffUnstuff item = novoItem("10", "20", "1000");

        assertThrows(IllegalStateException.class,
                () -> item.registrarExecucao(decimal("11"), decimal("20"), decimal("1000"), null, null, null));
    }

    @Test
    void deveExigirExecucaoIntegralParaConcluir() {
        OperacaoStuffUnstuff operacao = novaOperacao();
        operacao.adicionarItem(novoItem("10", "20", "1000"));
        operacao.iniciar();

        assertThrows(IllegalStateException.class,
                () -> operacao.concluir(null, "Tentativa antecipada", "operador", null));
    }

    @Test
    void deveCancelarERegistrarMotivo() {
        OperacaoStuffUnstuff operacao = novaOperacao();
        operacao.adicionarItem(novoItem("1", "1", "1"));

        operacao.cancelar("Contêiner indisponível", "planejador", "corr-2");

        assertEquals(StatusOperacaoStuffUnstuff.CANCELADA, operacao.getStatus());
        assertEquals("Contêiner indisponível", operacao.getMotivoCancelamento());
        assertEquals(TipoEventoStuffUnstuff.CANCELADA, operacao.getHistorico().get(0).getTipo());
    }

    private OperacaoStuffUnstuff novaOperacao() {
        OperacaoStuffUnstuff operacao = new OperacaoStuffUnstuff();
        operacao.setTipo(TipoOperacaoStuffUnstuff.STUFF);
        operacao.setConteinerId("CONT-001");
        return operacao;
    }

    private ItemOperacaoStuffUnstuff novoItem(String quantidade, String volume, String peso) {
        ItemOperacaoStuffUnstuff item = new ItemOperacaoStuffUnstuff();
        item.setQuantidadePlanejada(decimal(quantidade));
        item.setVolumePlanejadoM3(decimal(volume));
        item.setPesoPlanejadoKg(decimal(peso));
        return item;
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
