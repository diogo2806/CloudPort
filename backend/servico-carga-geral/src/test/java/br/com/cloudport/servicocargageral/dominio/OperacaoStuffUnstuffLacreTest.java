package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLacreStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoLacreStuffUnstuff;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperacaoStuffUnstuffLacreTest {

    @Test
    void deveBloquearConclusaoQuandoExisteDivergenciaDeLacreAberta() {
        OperacaoStuffUnstuff operacao = novaOperacao();
        LacreOperacaoStuffUnstuff lacre = novoLacreDivergente();
        operacao.adicionarLacre(lacre);

        assertThrows(IllegalStateException.class,
                () -> operacao.concluir("LACRE-02", "conclusão", "operador", "corr-1"));
    }

    @Test
    void devePermitirConclusaoQuandoDivergenciaFoiAutorizada() {
        OperacaoStuffUnstuff operacao = novaOperacao();
        LacreOperacaoStuffUnstuff lacre = novoLacreDivergente();
        lacre.setOverrideAutorizado(true);
        operacao.adicionarLacre(lacre);

        assertDoesNotThrow(() -> operacao.concluir("LACRE-02", "conclusão", "admin", "corr-2"));
    }

    private OperacaoStuffUnstuff novaOperacao() {
        OperacaoStuffUnstuff operacao = new OperacaoStuffUnstuff();
        operacao.setTipo(CargaGeralTipos.TipoOperacaoStuffUnstuff.UNSTUFF);
        operacao.setConteinerId("CONT-01");
        return operacao;
    }

    private LacreOperacaoStuffUnstuff novoLacreDivergente() {
        LacreOperacaoStuffUnstuff lacre = new LacreOperacaoStuffUnstuff();
        lacre.setCommandId(UUID.randomUUID());
        lacre.setNumeroLacre("LACRE-01");
        lacre.setTipoEvento(TipoEventoLacreStuffUnstuff.CONFERIDO);
        lacre.setStatus(StatusLacreStuffUnstuff.DIVERGENTE);
        lacre.setOperador("operador");
        lacre.setDivergenciaAberta(true);
        return lacre;
    }
}
