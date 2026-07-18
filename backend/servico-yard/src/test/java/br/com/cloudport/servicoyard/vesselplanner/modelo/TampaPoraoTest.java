package br.com.cloudport.servicoyard.vesselplanner.modelo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.EstadoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoOperacaoTampaPorao;
import org.junit.jupiter.api.Test;

class TampaPoraoTest {

    @Test
    void deveControlarAcessoAoPoraoESlotsSobreATampa() {
        TampaPorao tampa = new TampaPorao();
        tampa.setCodigo("HC-01");
        tampa.setEstado(EstadoTampaPorao.FECHADA);

        assertFalse(tampa.permiteMovimento(false));
        assertTrue(tampa.permiteMovimento(true));

        tampa.confirmar(TipoOperacaoTampaPorao.ABRIR);
        assertTrue(tampa.permiteMovimento(false));
        assertFalse(tampa.permiteMovimento(true));

        tampa.confirmar(TipoOperacaoTampaPorao.REMOVER);
        assertTrue(tampa.permiteMovimento(false));
        assertFalse(tampa.permiteMovimento(true));

        tampa.confirmar(TipoOperacaoTampaPorao.POSICIONAR);
        assertFalse(tampa.permiteMovimento(false));
        assertTrue(tampa.permiteMovimento(true));

        tampa.confirmar(TipoOperacaoTampaPorao.FECHAR);
        assertFalse(tampa.permiteMovimento(false));
        assertTrue(tampa.permiteMovimento(true));
    }

    @Test
    void deveRejeitarOperacaoForaDeSequencia() {
        TampaPorao tampa = new TampaPorao();
        tampa.setCodigo("HC-02");
        tampa.setEstado(EstadoTampaPorao.FECHADA);

        assertThrows(
                IllegalStateException.class,
                () -> tampa.validarInicio(TipoOperacaoTampaPorao.REMOVER));
    }
}
